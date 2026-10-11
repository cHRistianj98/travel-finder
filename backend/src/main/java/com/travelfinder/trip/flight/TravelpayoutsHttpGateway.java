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

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Jeden szeroki odczyt na miesiąc (plus opcjonalna paginacja), bez destination,
 * bez filtrów godzinowych i cenowych. Token nigdy nie pojawia się w URL ani logach.
 */
@Component
public class TravelpayoutsHttpGateway implements TravelpayoutsGateway {
    private static final Logger log = LoggerFactory.getLogger(TravelpayoutsHttpGateway.class);
    private static final int PAGE_SIZE = 1000; // limit udokumentowany przez Aviasales Data API

    private final RestClient client;
    private final boolean configured;
    private final int maxPages;

    public TravelpayoutsHttpGateway(
            @Value("${travelpayouts.api-token:}") String token,
            @Value("${travelpayouts.max-pages-per-month:2}") int maxPages) {
        configured = token != null && !token.isBlank() && !token.startsWith("TU_WKLEJ_");
        this.maxPages = Math.max(1, Math.min(maxPages, 5));
        client = RestClient.builder()
                .baseUrl("https://api.travelpayouts.com")
                .defaultHeader("X-Access-Token", token == null ? "" : token)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Override
    public List<JsonNode> fetchPeriod(String airport, YearMonth outboundMonth, YearMonth returnMonth) {
        if (!configured) {
            throw new FlightProviderException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Ustaw TRAVELPAYOUTS_TOKEN w pliku .env.local i uruchom backend ponownie.");
        }
        List<JsonNode> offers = new ArrayList<>();
        for (int page = 1; page <= maxPages; page++) {
            try {
                final int requestedPage = page;
                JsonNode root = client.get().uri(builder -> builder
                        .path("/aviasales/v3/prices_for_dates")
                        .queryParam("origin", airport)
                        .queryParam("departure_at", outboundMonth.toString())
                        .queryParam("return_at", returnMonth.toString())
                        .queryParam("one_way", "false")
                        .queryParam("currency", "pln")
                        .queryParam("limit", PAGE_SIZE)
                        .queryParam("page", requestedPage)
                        .queryParam("sorting", "price")
                        .queryParam("unique", "false")
                        .build()).retrieve().body(JsonNode.class);

                if (root == null || !root.path("success").asBoolean(false) || !root.path("data").isArray()) {
                    log.warn("Travelpayouts: niepoprawna odpowiedź, {} {}, strona {}", airport, outboundMonth + "→" + returnMonth, page);
                    throw new FlightProviderException(HttpStatus.BAD_GATEWAY,
                            "Travelpayouts zwróciło niepoprawne dane. Sprawdź dostęp do programu Aviasales.");
                }
                JsonNode data = root.path("data");
                int count = 0;
                for (JsonNode entry : data) {
                    offers.add(entry);
                    count++;
                }
                log.info("Travelpayouts: {} {}, strona {}, {} ofert", airport, outboundMonth + "→" + returnMonth, page, count);
                if (count < PAGE_SIZE) break;
            } catch (RestClientResponseException error) {
                int status = error.getStatusCode().value();
                log.warn("Travelpayouts: HTTP {} dla {} {} (strona {})", status, airport, outboundMonth + "→" + returnMonth, page);
                if (status == 401 || status == 403) {
                    throw new FlightProviderException(HttpStatus.BAD_GATEWAY,
                            "Travelpayouts odrzuciło token (HTTP " + status + "). Sprawdź TRAVELPAYOUTS_TOKEN i program Aviasales.");
                }
                if (status == 429) {
                    throw new FlightProviderException(HttpStatus.TOO_MANY_REQUESTS,
                            "Travelpayouts: limit zapytań (429). Poczekaj chwilę.");
                }
                throw new FlightProviderException(HttpStatus.BAD_GATEWAY,
                        "Travelpayouts jest chwilowo niedostępne (HTTP " + status + ").");
            } catch (RestClientException error) {
                log.warn("Travelpayouts: błąd połączenia {} {} ({})", airport, outboundMonth + "→" + returnMonth,
                        error.getClass().getSimpleName());
                throw new FlightProviderException(HttpStatus.BAD_GATEWAY,
                        "Nie udało się połączyć z Travelpayouts. Spróbuj ponownie później.");
            }
        }
        return List.copyOf(offers);
    }
}
