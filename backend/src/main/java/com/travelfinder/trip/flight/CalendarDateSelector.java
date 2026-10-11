package com.travelfinder.trip.flight;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calendar API zwraca orientacyjne ceny dla WIELU kombinacji dat w jednej odpowiedzi.
 * Ten selektor nie uznaje tych cen za oferty: wybiera tylko pary do weryfikacji
 * przez Google Flights, które dopiero zwraca konkretne godziny i cenę round-trip.
 */
public final class CalendarDateSelector {
    private CalendarDateSelector() { }

    public static List<PricedDatePair> select(JsonNode response, LocalDate firstFreeDay,
            LocalDate lastFreeDay, BigDecimal budget, int limit) {
        List<AvailableDatePairs.DatePair> permitted = AvailableDatePairs.inWindow(firstFreeDay, lastFreeDay);
        Set<AvailableDatePairs.DatePair> allowedPairs = Set.copyOf(permitted);
        Map<AvailableDatePairs.DatePair, BigDecimal> prices = new HashMap<>();

        JsonNode rows = response.path("calendar");
        if (!rows.isArray()) return List.of();

        for (JsonNode row : rows) {
            if (row.path("has_no_flights").asBoolean(false)) continue;
            try {
                String departure = row.path("departure").asText();
                String returning = row.path("return").asText();
                var pair = new AvailableDatePairs.DatePair(LocalDate.parse(departure), LocalDate.parse(returning));
                if (!allowedPairs.contains(pair) || !row.path("price").isNumber()) continue;
                BigDecimal price = new BigDecimal(row.path("price").asText());
                if (price.signum() <= 0) continue;
                prices.merge(pair, price, BigDecimal::min);
            } catch (RuntimeException ignored) {
                // Pojedynczy błędny wiersz nie powinien przekreślać całego kalendarza.
            }
        }

        // Najpierw potencjalne wyjazdy w budżecie, później pozostałe.
        // Cena / liczba nocy pozwala nie preferować automatycznie wyłącznie jednej nocy.
        // Cena Calendar to wskazówka, NIE filtr odrzucający oferty; ostateczne
        // filtrowanie ceny następuje na szczegółowej ofercie round-trip.
        Comparator<PricedDatePair> preference = Comparator
                .comparing((PricedDatePair p) -> p.indicativePrice().compareTo(budget) > 0)
                .thenComparing(PricedDatePair::estimatedCostPerNight)
                .thenComparing(PricedDatePair::indicativePrice)
                .thenComparing(Comparator.comparingLong(PricedDatePair::nights).reversed())
                .thenComparing(p -> p.pair().outbound());

        return prices.entrySet().stream()
                .map(entry -> new PricedDatePair(entry.getKey(), entry.getValue()))
                .sorted(preference)
                .limit(Math.max(0, limit))
                .toList();
    }

    public record PricedDatePair(AvailableDatePairs.DatePair pair, BigDecimal indicativePrice) {
        public long nights() {
            return ChronoUnit.DAYS.between(pair.outbound(), pair.returnDeparture());
        }

        BigDecimal estimatedCostPerNight() {
            return indicativePrice.divide(BigDecimal.valueOf(nights()), 2, RoundingMode.HALF_UP);
        }
    }
}
