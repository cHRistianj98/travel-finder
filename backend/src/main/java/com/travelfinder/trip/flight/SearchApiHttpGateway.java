package com.travelfinder.trip.flight;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SearchApiHttpGateway implements SearchApiGateway {
    private static final Logger log = LoggerFactory.getLogger(SearchApiHttpGateway.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Duration TTL = Duration.ofMinutes(5);

    private final RestClient restClient;
    private final boolean configured;
    private final ConcurrentHashMap<Map<String, String>, CacheEntry> cache = new ConcurrentHashMap<>();

    public SearchApiHttpGateway(@Value("${searchapi.api-key:}") String key) {
        configured = !key.isBlank() && !key.startsWith("TU_WKLEJ_");
        restClient = RestClient.builder()
                .baseUrl("https://www.searchapi.io")
                .defaultHeader("Authorization", "Bearer " + key)
                .build();
    }

    @Override
    public JsonNode search(Map<String, String> parameters) {
        if (!configured) {
            throw new FlightProviderException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Brak klucza SearchApi. Ustaw SEARCHAPI_KEY w .env.local i uruchom backend ponownie.");
        }
        Map<String, String> cacheKey = Map.copyOf(parameters);
        CacheEntry entry = cache.get(cacheKey);
        if (entry != null && Instant.now().isBefore(entry.expiresAt())) {
            return entry.json();
        }

        String context = describeRequest(parameters);
        try {
            JsonNode result = restClient.get().uri(builder -> {
                builder.path("/api/v1/search");
                Map<String, Object> values = new LinkedHashMap<>();
                parameters.forEach((name, value) -> {
                    builder.queryParam(name, "{" + name + "}");
                    values.put(name, value);
                });
                // Token jest kodowany przez UriBuilder. Nie zapisujemy go w logach.
                return builder.build(values);
            }).retrieve().body(JsonNode.class);

            if (result == null || result.isNull()) {
                log.warn("SearchApi: pusta odpowiedź dla {}", context);
                throw new FlightProviderException(HttpStatus.BAD_GATEWAY,
                        "SearchApi zwróciło pustą odpowiedź (" + context + ").");
            }
            if (result.hasNonNull("error")) {
                // Flights i Calendar potrafią zwrócić HTTP 200 i status=Success razem z
                // error="Google Flights didn't return any results.".
                // To brak ofert, nie awaria API: provider zobaczy puste best_flights/other_flights.
                if (isEmptyFlightResult(parameters, result)) {
                    log.info("SearchApi: brak lotów dla {} (prawidłowa pusta odpowiedź)", context);
                } else {
                    String reason = safeReason(result);
                    log.warn("SearchApi: błąd w odpowiedzi dla {}: {}", context, reason);
                    throw new FlightProviderException(HttpStatus.BAD_GATEWAY,
                            "SearchApi: " + reason + " [" + context + "]");
                }
            }

            if (cache.size() > 150) cache.clear();
            cache.put(cacheKey, new CacheEntry(result, Instant.now().plus(TTL)));
            return result;
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            String reason = safeReason(parseJson(exception.getResponseBodyAsString()));
            log.warn("SearchApi: HTTP {} dla {}: {}", status, context, reason);
            if (status == 401 || status == 403) {
                throw new FlightProviderException(HttpStatus.BAD_GATEWAY,
                        "SearchApi odrzuciło klucz albo uprawnienia (HTTP " + status + ").");
            }
            if (status == 429) {
                throw new FlightProviderException(HttpStatus.TOO_MANY_REQUESTS,
                        "SearchApi: przekroczony limit zapytań (429). Sprawdź konto i spróbuj później.");
            }
            throw new FlightProviderException(HttpStatus.BAD_GATEWAY,
                    "SearchApi HTTP " + status + ": " + reason + " [" + context + "]");
        } catch (RestClientException exception) {
            log.warn("SearchApi: nie udało się połączyć dla {} ({})", context,
                    exception.getClass().getSimpleName());
            throw new FlightProviderException(HttpStatus.BAD_GATEWAY,
                    "Problem z połączeniem do SearchApi (" + context + ").");
        }
    }

    /** Rozpoznajemy tylko udokumentowany przez rzeczywistą odpowiedź przypadek pustych wyników,
     *  a nie dowolne błędy HTTP 200 (np. złe parametry/limit API). */
    static boolean isEmptyFlightResult(Map<String, String> parameters, JsonNode root) {
        String engine = parameters.get("engine");
        if (!("google_flights".equals(engine) || "google_flights_calendar".equals(engine))
                || root == null || root.isNull()) {
            return false;
        }
        JsonNode error = root.path("error");
        String message = asText(error);
        if (message == null) message = asText(error.path("message"));
        if (message == null) return false;
        return "google flights didn't return any results.".equalsIgnoreCase(message.trim())
                || "google flights didn't return any results".equalsIgnoreCase(message.trim());
    }

    /** Nie logujemy sekretów ani departure_token / booking_token. */
    static String describeRequest(Map<String, String> parameters) {
        String engine = parameters.getOrDefault("engine", "unknown");
        String stage = parameters.containsKey("departure_token") ? "wybór powrotu" :
                "google_flights".equals(engine) ? "wyszukiwanie wylotu" :
                "google_flights_calendar".equals(engine) ? "kalendarz cen" : "odkrywanie kierunków";
        String origin = parameters.getOrDefault("departure_id", "?");
        String destination = parameters.get("arrival_id");
        String route = destination == null ? origin + " → ANYWHERE" : origin + " → " + destination;
        String from = parameters.get("outbound_date");
        String to = parameters.get("return_date");
        return engine + "/" + stage + ", " + route +
                (from == null ? "" : ", " + from + (to == null ? "" : "–" + to));
    }

    private static JsonNode parseJson(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            return JSON.readTree(body);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Odczytujemy wyłącznie pola błędu, bez wypisywania całego JSON/URL/tokenu. */
    static String safeReason(JsonNode root) {
        if (root == null || root.isNull()) return "brak szczegółów odpowiedzi";
        JsonNode error = root.path("error");
        String reason = asText(error.path("message"));
        if (reason == null) reason = asText(error.path("description"));
        if (reason == null) reason = asText(root.path("message"));
        if (reason == null) reason = asText(error);
        if (reason == null) reason = asText(error.path("type"));
        if (reason == null) reason = "nieznany błąd dostawcy";
        // Odrzucamy potencjalne dane uwierzytelniające, nawet jeśli API umieści je w komunikacie.
        reason = reason.replaceAll("(?i)(api_key|departure_token|booking_token|authorization|bearer)\\s*[:=]\\s*[^\\s,;&\\\"}]+", "$1=[ukryte]");
        reason = reason.replaceAll("(?i)(https?://[^\\s]+)", "[adres ukryty]");
        reason = reason.replaceAll("[\\r\\n\\t]", " ").trim();
        return reason.substring(0, Math.min(reason.length(), 220));
    }

    private static String asText(JsonNode node) {
        return node != null && node.isTextual() && !node.asText().isBlank() ? node.asText() : null;
    }

    private record CacheEntry(JsonNode json, Instant expiresAt) { }
}
