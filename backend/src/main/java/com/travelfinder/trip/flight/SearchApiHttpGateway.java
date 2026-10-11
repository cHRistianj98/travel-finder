package com.travelfinder.trip.flight;

import tools.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SearchApiHttpGateway implements SearchApiGateway {
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
                    "Brak klucza SearchApi. Utwórz .env.local w katalogu projektu z SEARCHAPI_KEY=... i uruchom backend ponownie.");
        }
        Map<String, String> key = Map.copyOf(parameters);
        CacheEntry entry = cache.get(key);
        if (entry != null && Instant.now().isBefore(entry.expiresAt())) {
            return entry.json();
        }
        try {
            JsonNode result = restClient.get().uri(builder -> {
                builder.path("/api/v1/search");
                // Używamy template variables, by poprawnie zakodować + / = w departure_token.
                Map<String, Object> values = new LinkedHashMap<>();
                parameters.forEach((name, value) -> {
                    builder.queryParam(name, "{" + name + "}");
                    values.put(name, value);
                });
                return builder.build(values);
            }).retrieve().body(JsonNode.class);
            if (result == null || result.isNull()) {
                throw new FlightProviderException(HttpStatus.BAD_GATEWAY, "SearchApi zwróciło pustą odpowiedź.");
            }
            if (result.hasNonNull("error")) {
                throw new FlightProviderException(HttpStatus.BAD_GATEWAY, "SearchApi zwróciło błąd. Sprawdź logi backendu i parametry wyszukiwania.");
            }
            if (cache.size() > 150) {
                cache.clear();
            }
            cache.put(key, new CacheEntry(result, Instant.now().plus(TTL)));
            return result;
        } catch (RestClientResponseException exception) {
            HttpStatusCode status = exception.getStatusCode();
            if (status.value() == 401 || status.value() == 403) {
                throw new FlightProviderException(HttpStatus.BAD_GATEWAY,
                        "SearchApi odrzuciło klucz API (HTTP " + status.value() + ").");
            }
            if (status.value() == 429) {
                throw new FlightProviderException(HttpStatus.TOO_MANY_REQUESTS,
                        "Wyczerpano limit lub tempo zapytań SearchApi (429). Spróbuj później.");
            }
            throw new FlightProviderException(HttpStatus.BAD_GATEWAY,
                    "SearchApi nie zwróciło danych (HTTP " + status.value() + ").");
        } catch (RestClientException exception) {
            throw new FlightProviderException(HttpStatus.BAD_GATEWAY,
                    "Nie udało się połączyć z SearchApi. Sprawdź internet i spróbuj ponownie.");
        }
    }

    private record CacheEntry(JsonNode json, Instant expiresAt) { }
}
