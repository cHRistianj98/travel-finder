package com.travelfinder.trip.train;

import com.travelfinder.trip.flight.FlightProviderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import java.time.LocalDate;

/** Darmowe oficjalne dane rozkładowe. API wymaga przydzielonego klucza X-API-Key. */
@Component
public class PlkHttpGateway implements PlkGateway {
    private final boolean configured;
    private final RestClient client;

    public PlkHttpGateway(@Value("${plk.api-key:}") String apiKey) {
        configured = apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("TU_WKLEJ_");
        client = RestClient.builder().baseUrl("https://pdp-api.plk-sa.pl")
                .defaultHeader("X-API-Key", apiKey == null ? "" : apiKey)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Override
    public JsonNode stations(String city) {
        requireKey();
        return fetch(() -> client.get().uri(builder -> builder.path("/api/v1/dictionaries/stations")
                .queryParam("search", city).queryParam("pageSize", 500).build())
                .retrieve().body(JsonNode.class));
    }

    @Override
    public JsonNode schedules(int stationId, LocalDate from, LocalDate to) {
        requireKey();
        if (from.isAfter(to) || java.time.temporal.ChronoUnit.DAYS.between(from, to) > 30) {
            throw new IllegalArgumentException("PLK: zakres jednego pobrania może wynosić maksymalnie 31 dni.");
        }
        return fetch(() -> client.get().uri(builder -> builder.path("/api/v1/schedules")
                .queryParam("dateFrom", from).queryParam("dateTo", to)
                .queryParam("stations", stationId).queryParam("fullRoute", true)
                .queryParam("dictionaries", true).build())
                .retrieve().body(JsonNode.class));
    }

    private void requireKey() {
        if (!configured) throw new FlightProviderException(HttpStatus.SERVICE_UNAVAILABLE,
                "Pociągi: dodaj PLK_API_KEY do .env.local (bezpłatny klucz: pdp-api.plk-sa.pl) i uruchom backend ponownie.");
    }

    private JsonNode fetch(java.util.function.Supplier<JsonNode> call) {
        try {
            JsonNode body = call.get();
            if (body == null || !body.isObject()) throw new FlightProviderException(HttpStatus.BAD_GATEWAY,
                    "PKP PLK zwróciło niepoprawną odpowiedź JSON.");
            return body;
        } catch (RestClientResponseException error) {
            int status = error.getStatusCode().value();
            if (status == 401 || status == 403) throw new FlightProviderException(HttpStatus.BAD_GATEWAY,
                    "PKP PLK odrzuciło klucz API (HTTP " + status + "). Sprawdź PLK_API_KEY i aktywację konta.");
            if (status == 429) throw new FlightProviderException(HttpStatus.TOO_MANY_REQUESTS,
                    "PKP PLK: przekroczony limit zapytań. Spróbuj później.");
            throw new FlightProviderException(HttpStatus.BAD_GATEWAY,
                    "PKP PLK: błąd serwera rozkładów (HTTP " + status + ").");
        } catch (RestClientException error) {
            throw new FlightProviderException(HttpStatus.BAD_GATEWAY,
                    "Brak połączenia z oficjalnym API PKP PLK. Sprawdź sieć i spróbuj ponownie.");
        }
    }
}
