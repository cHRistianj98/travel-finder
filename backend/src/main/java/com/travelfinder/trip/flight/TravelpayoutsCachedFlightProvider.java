package com.travelfinder.trip.flight;

import com.travelfinder.trip.domain.TransportType;
import com.travelfinder.trip.domain.AvailabilityCalendar;
import com.travelfinder.trip.domain.TripOption;
import com.travelfinder.trip.domain.TripSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Jedno pobranie pary miesięcy wylotu i powrotu na lotnisko; filtry użytkownika działają WYŁĄCZNIE
 * w Javie na pamięci podręcznej. Oferty mają orientacyjną cenę z Aviasales Data API.
 */
@Component
@ConditionalOnProperty(name = "flights.provider", havingValue = "travelpayouts", matchIfMissing = true)
public class TravelpayoutsCachedFlightProvider implements FlightSearchProvider {
    private static final Logger log = LoggerFactory.getLogger(TravelpayoutsCachedFlightProvider.class);
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final int MAX_RESULTS = 150;
    private static final String PROVIDER = "TRAVELPAYOUTS_CACHED";

    private final TravelpayoutsGateway gateway;
    private final Duration ttl;
    private final Clock clock;
    private final Map<MonthKey, Snapshot> cache = new HashMap<>();

    @Autowired
    public TravelpayoutsCachedFlightProvider(TravelpayoutsGateway gateway,
            @Value("${travelpayouts.cache-hours:6}") int cacheHours) {
        this(gateway, cacheHours, Clock.systemUTC());
    }

    // Umożliwia deterministyczny test TTL bez sleep() i bez żadnych HTTP.
    TravelpayoutsCachedFlightProvider(TravelpayoutsGateway gateway, int cacheHours, Clock clock) {
        this.gateway = gateway;
        this.ttl = Duration.ofHours(Math.max(1, Math.min(cacheHours, 24)));
        this.clock = clock;
    }

    @Override
    public List<TripOption> search(TripSearchRequest request) {
        String airport = OriginAirports.airportFor(request.origin())
                .orElseThrow(() -> new IllegalArgumentException("Nieznane lotnisko: " + request.origin()));

        AvailabilityCalendar availability = AvailabilityCalendar.from(request);
        List<TripOption> matches = new ArrayList<>();
        YearMonth begin = YearMonth.from(request.outboundDate());
        YearMonth end = YearMonth.from(request.returnDate());
        for (YearMonth outboundMonth = begin; !outboundMonth.isAfter(end); outboundMonth = outboundMonth.plusMonths(1)) {
            for (YearMonth returnMonth = outboundMonth; !returnMonth.isAfter(end); returnMonth = returnMonth.plusMonths(1)) {
                for (JsonNode row : periodOffers(airport, outboundMonth, returnMonth)) {
                    parse(row, request, availability, airport).ifPresent(matches::add);
                }
            }
        }

        // Wielu sprzedawców potrafi zwrócić ten sam lot z inną ceną.
        // Wybieramy najtańszą zapisaną ofertę dla identycznych godzin + lotnisk.
        matches.sort(Comparator.comparing(TripOption::price)
                .thenComparing(Comparator.comparingLong(TripOption::timeAtDestinationMinutes).reversed()));
        Map<String, TripOption> unique = new LinkedHashMap<>();
        for (TripOption trip : matches) {
            String key = trip.destination() + '|' + trip.outboundDeparture().toInstant() + '|'
                    + trip.returnDeparture().toInstant();
            unique.putIfAbsent(key, trip);
        }
        log.info("Travelpayouts: lokalne filtrowanie {} {}..{} → {} dopasowań ({} unikalnych)",
                airport, request.outboundDate(), request.returnDate(), matches.size(), unique.size());
        return unique.values().stream().limit(MAX_RESULTS).toList();
    }

    /** Cache współdzielony między wszystkimi wyszukiwaniami, także równoległymi.
     * synchronized zapobiega podwójnemu pobraniu przy dwóch jednoczesnych kliknięciach. */
    private synchronized List<JsonNode> periodOffers(String airport, YearMonth outboundMonth, YearMonth returnMonth) {
        MonthKey key = new MonthKey(airport, outboundMonth, returnMonth);
        Snapshot old = cache.get(key);
        Instant now = clock.instant();
        if (old != null && now.isBefore(old.expiresAt())) {
            log.info("Travelpayouts CACHE HIT: {} {}, {} ofert (0 żądań HTTP)",
                    airport, outboundMonth + "→" + returnMonth, old.offers().size());
            return old.offers();
        }
        List<JsonNode> fresh = gateway.fetchPeriod(airport, outboundMonth, returnMonth);
        // Usuwamy wpisy historyczne, żeby cache nie rósł bez końca.
        cache.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
        cache.put(key, new Snapshot(List.copyOf(fresh), now.plus(ttl)));
        log.info("Travelpayouts CACHE MISS: {} {}, pobrano {} ofert", airport, outboundMonth + "→" + returnMonth, fresh.size());
        return fresh;
    }

    /** Parsowanie oddzielone od HTTP — wadliwy rekord pomijamy, nie zgadujemy danych. */
    private java.util.Optional<TripOption> parse(JsonNode row, TripSearchRequest req,
            AvailabilityCalendar availability, String airport) {
        try {
            if (!airport.equalsIgnoreCase(text(row, "origin_airport",
                    text(row, "origin", "")))) {
                return java.util.Optional.empty();
            }
            String destinationCode = text(row, "destination", "");
            String destAirport = text(row, "destination_airport", destinationCode);
            if (destinationCode.isBlank() || destAirport.isBlank()) return java.util.Optional.empty();

            OffsetDateTime rawDepart = OffsetDateTime.parse(text(row, "departure_at", ""));
            OffsetDateTime rawReturnDepart = OffsetDateTime.parse(text(row, "return_at", ""));
            int durationTo = integer(row, "duration_to", -1);
            int durationBack = integer(row, "duration_back", -1);
            int stopsTo = integer(row, "transfers", -1);
            int stopsBack = integer(row, "return_transfers", -1);
            int price = integer(row, "price", -1);
            if (durationTo < 1 || durationBack < 1 || stopsTo < 0 || stopsBack < 0 || price < 1) {
                return java.util.Optional.empty();
            }

            // Offset daty wylotu i powrotu pochodzi bezpośrednio z API.
            // Przyloty wyliczamy z rzeczywistego czasu podróży (instant), z DST w strefie celu.
            ZoneId destinationZone = FlightDestinationCatalog.zone(destinationCode, destAirport,
                    rawReturnDepart.getOffset());
            OffsetDateTime depart = rawDepart.atZoneSameInstant(WARSAW).toOffsetDateTime();
            OffsetDateTime returnDepart = rawReturnDepart.atZoneSameInstant(destinationZone).toOffsetDateTime();
            OffsetDateTime arrival = depart.toInstant().plus(Duration.ofMinutes(durationTo))
                    .atZone(destinationZone).toOffsetDateTime();
            OffsetDateTime returnArrival = returnDepart.toInstant().plus(Duration.ofMinutes(durationBack))
                    .atZone(WARSAW).toOffsetDateTime();
            long stay = Duration.between(arrival.toInstant(), returnDepart.toInstant()).toMinutes();

            // Okno wolnego: start lotu z Polski i PRZYLOT do domu muszą mieścić się w dniach wolnych.
            // Natomiast filtr godzin powrotu dotyczy STARTU z miasta docelowego.
            LocalDate startDay = depart.atZoneSameInstant(WARSAW).toLocalDate();
            LocalDate lastDay = returnArrival.toLocalDate();
            if (!availability.allowsTrip(startDay, lastDay) || stay <= 0) return java.util.Optional.empty();
            if (req.directOnly() && (stopsTo > 0 || stopsBack > 0)) return java.util.Optional.empty();
            if (BigDecimal.valueOf(price).compareTo(req.maxTransportBudget()) > 0) return java.util.Optional.empty();
            if (depart.toLocalTime().isBefore(req.earliestOutboundDeparture())
                    || depart.toLocalTime().isAfter(req.latestOutboundDeparture())
                    || returnDepart.toLocalTime().isBefore(req.earliestReturnDeparture())
                    || returnDepart.toLocalTime().isAfter(req.latestReturnDeparture())) {
                return java.util.Optional.empty();
            }

            String link = safeBookingUrl(text(row, "link", ""));
            String identity = airport + '|' + destAirport + '|' + depart.toInstant() + '|'
                    + returnDepart.toInstant() + '|' + price + '|' + link;
            return java.util.Optional.of(new TripOption(
                    UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)),
                    req.origin(), FlightDestinationCatalog.name(destinationCode, destAirport),
                    depart, arrival, returnDepart, returnArrival,
                    TransportType.FLIGHT, BigDecimal.valueOf(price), "PLN",
                    (long) durationTo + durationBack, stay,
                    stopsTo, stopsBack, PROVIDER, link));
        } catch (RuntimeException malformed) {
            // Puste pola, niepoprawny offset lub brak czasu: nigdy nie tworzymy fikcyjnej oferty.
            return java.util.Optional.empty();
        }
    }

    static String safeBookingUrl(String raw) {
        if (raw == null || !raw.startsWith("/search/") || raw.indexOf('\\') >= 0
                || raw.chars().anyMatch(c -> c < 32 || c == 127)) return null;
        return "https://www.aviasales.com" + raw;
    }

    private static String text(JsonNode row, String property, String fallback) {
        JsonNode node = row.path(property);
        return node.isTextual() ? node.asText() : fallback;
    }

    private static int integer(JsonNode row, String property, int fallback) {
        JsonNode node = row.path(property);
        return node.isIntegralNumber() ? node.intValue() : fallback;
    }

    private record MonthKey(String airport, YearMonth outboundMonth, YearMonth returnMonth) { }
    private record Snapshot(List<JsonNode> offers, Instant expiresAt) { }
}
