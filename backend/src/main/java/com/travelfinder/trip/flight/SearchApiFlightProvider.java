package com.travelfinder.trip.flight;

import com.travelfinder.trip.domain.TransportType;
import com.travelfinder.trip.domain.TripOption;
import com.travelfinder.trip.domain.TripSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Etap 1: Explore odkrywa kierunki. Etap 2: Calendar zwraca ceny wielu dat
 * JEDNYM zapytaniem na kierunek. Etap 3: Google Flights weryfikuje wybrane
 * kombinacje (wylot -> departure_token -> powrót -> cena obu lotów).
 * Calendar służy WYŁĄCZNIE preselekcji: szczegółowe godziny i ceny zawsze
 * pochodzą z Google Flights. Nie generujemy żadnych danych demonstracyjnych.
 */
@Component
@ConditionalOnProperty(name = "flights.provider", havingValue = "searchapi")
public class SearchApiFlightProvider implements FlightSearchProvider {
    private static final Logger log = LoggerFactory.getLogger(SearchApiFlightProvider.class);
    private static final ZoneId POLAND = ZoneId.of("Europe/Warsaw");
    private static final String SOURCE = "SEARCHAPI_GOOGLE_FLIGHTS";

    private final SearchApiGateway gateway;
    private final TravelTimeZones timeZones;
    private final int maxDestinations;
    private final int maxDatePairs;
    private final int maxOutbounds;

    public SearchApiFlightProvider(SearchApiGateway gateway, TravelTimeZones timeZones,
            @Value("${searchapi.max-destinations:3}") int maxDestinations,
            @Value("${searchapi.max-calendar-pairs-per-destination:2}") int maxDatePairs,
            @Value("${searchapi.max-outbounds-per-pair:1}") int maxOutbounds) {
        this.gateway = gateway;
        this.timeZones = timeZones;
        this.maxDestinations = Math.max(1, Math.min(maxDestinations, 5));
        this.maxDatePairs = Math.max(1, Math.min(maxDatePairs, 4));
        this.maxOutbounds = Math.max(1, Math.min(maxOutbounds, 2));
    }

    @Override
    public List<TripOption> search(TripSearchRequest request) {
        String originAirport = OriginAirports.airportFor(request.origin())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Na razie nie obsługujemy wylotów z miasta " + request.origin() + "."));

        // Walidacja długości okna ZANIM wydamy jakiekolwiek zapytanie do API.
        List<AvailableDatePairs.DatePair> allPairs = AvailableDatePairs.inWindow(
                request.outboundDate(), request.returnDate());

        Map<String, String> exploreParameters = new LinkedHashMap<>();
        exploreParameters.put("engine", "google_travel_explore");
        exploreParameters.put("departure_id", originAirport);
        exploreParameters.put("time_period", request.outboundDate() + ".." + request.returnDate());
        exploreParameters.put("travel_mode", "flights_only");
        exploreParameters.put("stops", request.directOnly() ? "nonstop" : "any");
        exploreParameters.put("currency", "PLN");
        exploreParameters.put("hl", "pl");
        exploreParameters.put("gl", "pl");

        List<Destination> discovered = destinations(gateway.search(exploreParameters));
        log.info("SearchApi Explore: znaleziono {} wybranych kierunków; okno {}–{} ({} możliwych par dni)",
                discovered.size(), request.outboundDate(), request.returnDate(), allPairs.size());

        List<TripOption> result = new ArrayList<>();
        int successfulCalendars = 0;
        int verifiedPairs = 0;
        FlightProviderException firstFailure = null;
        for (Destination destination : discovered) {
            List<CalendarDateSelector.PricedDatePair> shortlisted;
            try {
                JsonNode calendar = gateway.search(calendarParameters(request, originAirport, destination.airport()));
                successfulCalendars++;
                shortlisted = CalendarDateSelector.select(calendar, request.outboundDate(),
                        request.returnDate(), request.maxTransportBudget(), maxDatePairs);
                log.info("SearchApi Calendar: {} ({}), do weryfikacji {} z {} możliwych par dni",
                        destination.name(), destination.airport(), shortlisted.size(), allPairs.size());
            } catch (FlightProviderException exception) {
                if (firstFailure == null) firstFailure = exception;
                log.warn("SearchApi Calendar: {} ({}) nieudane: {}",
                        destination.name(), destination.airport(), exception.getMessage());
                continue;
            }
            if (shortlisted.isEmpty()) continue;

            Optional<ZoneId> destinationZone = timeZones.forLocation(destination.latitude(), destination.longitude());
            if (destinationZone.isEmpty()) {
                log.warn("Brak strefy czasowej dla {} ({}, {})", destination.name(),
                        destination.latitude(), destination.longitude());
                continue;
            }
            for (CalendarDateSelector.PricedDatePair candidate : shortlisted) {
                var pair = candidate.pair();
                try {
                    List<TripOption> trips = searchDatePair(request, pair, originAirport,
                            destination, destinationZone.get());
                    verifiedPairs++;
                    result.addAll(trips);
                    log.info("Google Flights {}: {} → {}, cena Calendar {} PLN, zweryfikowanych wariantów {}",
                            destination.name(), pair.outbound(), pair.returnDeparture(),
                            candidate.indicativePrice(), trips.size());
                } catch (FlightProviderException exception) {
                    if (firstFailure == null) firstFailure = exception;
                    log.warn("Google Flights {}: {} → {}: {}", destination.name(),
                            pair.outbound(), pair.returnDeparture(), exception.getMessage());
                }
            }
        }

        // Jeśli SearchApi nie przetworzyło ani jednego kalendarza, zgłaszamy błąd,
        // zamiast fałszywie sugerować, że rzeczywiście brak lotów.
        if (!discovered.isEmpty() && successfulCalendars == 0 && firstFailure != null) {
            throw firstFailure;
        }
        if (successfulCalendars > 0 && verifiedPairs == 0 && firstFailure != null) {
            log.warn("Nie udało się zweryfikować żadnej pary dat; pierwsza przyczyna: {}",
                    firstFailure.getMessage());
        }
        Map<UUID, TripOption> unique = new LinkedHashMap<>();
        result.forEach(option -> unique.putIfAbsent(option.id(), option));
        return unique.values().stream()
                .sorted(Comparator.comparing(TripOption::price)
                        .thenComparing(TripOption::timeAtDestinationMinutes, Comparator.reverseOrder()))
                .toList();
    }

    /** Jedno zapytanie dla całego okna dostępności i pojedynczego kierunku. */
    private Map<String, String> calendarParameters(TripSearchRequest request, String origin, String arrival) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("engine", "google_flights_calendar");
        params.put("flight_type", "round_trip");
        params.put("departure_id", origin);
        params.put("arrival_id", arrival);
        params.put("outbound_date", request.outboundDate().toString());
        params.put("return_date", request.returnDate().toString());
        params.put("outbound_date_start", request.outboundDate().toString());
        params.put("outbound_date_end", request.returnDate().minusDays(1).toString());
        params.put("return_date_start", request.outboundDate().plusDays(1).toString());
        params.put("return_date_end", request.returnDate().toString());
        params.put("stops", request.directOnly() ? "nonstop" : "any");
        params.put("currency", "PLN");
        params.put("hl", "pl");
        params.put("gl", "pl");
        // Cena i godziny z formularza są kontrolowane na potwierdzonym round-trip.
        // Calendar nie zna godzin, więc nie służy do ostatecznego filtrowania.
        return params;
    }

    private List<Destination> destinations(JsonNode root) {
        List<Destination> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (JsonNode item : items(root.path("destinations"))) {
            String airport = firstText(item.path("primary_airport"), item.path("flight").path("airport_code"));
            String kgmid = text(item.path("kgmid"));
            String arrivalId = airport != null && airport.matches("[A-Z]{3}") ? airport
                    : kgmid != null && kgmid.matches("/m/[A-Za-z0-9_]+") ? kgmid : null;
            String name = text(item.path("name"));
            JsonNode coords = item.path("coordinates");
            if (name == null || arrivalId == null || !coords.isArray() || coords.size() < 2
                    || !coords.get(0).isNumber() || !coords.get(1).isNumber()) {
                continue;
            }
            if (!visited.add(arrivalId)) continue;
            BigDecimal indication = decimal(item.path("flight").path("price"));
            result.add(new Destination(name, arrivalId, coords.get(0).asDouble(),
                    coords.get(1).asDouble(), indication));
        }
        // Destinations are only recommendations; do NOT discard them by indicative price.
        result.sort(Comparator.comparing(Destination::indication,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return result.stream().limit(maxDestinations).toList();
    }

    private List<TripOption> searchDatePair(TripSearchRequest request,
            AvailableDatePairs.DatePair pair, String originAirport,
            Destination destination, ZoneId destinationZone) {
        Map<String, String> params = flightParameters(request, pair, originAirport, destination.airport());
        JsonNode outboundResult = gateway.search(params);
        List<JsonNode> rawDepartures = options(outboundResult);
        List<LegCandidate> parsedDepartures = rawDepartures.stream()
                .map(node -> leg(node, POLAND, destinationZone))
                .flatMap(Optional::stream).toList();
        List<LegCandidate> validDepartures = parsedDepartures.stream()
                .filter(leg -> leg.departure().toLocalDate().equals(pair.outbound()))
                .filter(leg -> !leg.arrival().toLocalDate().isAfter(request.returnDate()))
                .filter(leg -> within(leg.departure().toLocalTime(), request.earliestOutboundDeparture(),
                        request.latestOutboundDeparture()))
                .filter(leg -> !request.directOnly() || leg.stops() == 0)
                .toList();
        List<LegCandidate> selectedDepartures = validDepartures.stream()
                .filter(leg -> leg.departureToken() != null)
                .sorted(Comparator.comparing(LegCandidate::price,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(maxOutbounds).toList();

        log.info("{} {}→{}: API wylotów {}, odczytano {}, pasujących {}, tokenów wybrano {}",
                destination.name(), pair.outbound(), pair.returnDeparture(), rawDepartures.size(),
                parsedDepartures.size(), validDepartures.size(), selectedDepartures.size());
        for (LegCandidate candidate : parsedDepartures.stream().limit(4).toList()) {
            log.debug("Wylot {} {}: {}, przesiadki {}, token {}, decyzja {}",
                    destination.name(), pair.outbound(), candidate.departure(), candidate.stops(),
                    candidate.departureToken() != null, outboundDecision(candidate, pair, request));
        }

        List<TripOption> combinations = new ArrayList<>();
        for (LegCandidate outbound : selectedDepartures) {
            Map<String, String> returnParams = new LinkedHashMap<>(params);
            returnParams.put("departure_token", outbound.departureToken());
            JsonNode returnResult = gateway.search(returnParams);
            for (JsonNode option : options(returnResult)) {
                Optional<LegCandidate> maybeReturn = leg(option, destinationZone, POLAND);
                if (maybeReturn.isEmpty()) continue;
                LegCandidate back = maybeReturn.get();
                if (request.directOnly() && back.stops() != 0) continue;
                // SearchApi return_date to dzień startu lotu powrotnego, niekoniecznie dzień
                // dotarcia do domu. Powrót może wylądować następnego dnia.
                if (!back.departure().toLocalDate().equals(pair.returnDeparture())) continue;
                if (back.arrival().toLocalDate().isAfter(request.returnDate())) continue;
                if (!within(back.arrival().toLocalTime(), request.earliestReturnArrival(),
                        request.latestReturnArrival())) continue;
                BigDecimal finalPrice = back.price(); // Cena całego round_trip po departure_token.
                if (finalPrice == null || finalPrice.compareTo(request.maxTransportBudget()) > 0) continue;
                long stay = Duration.between(outbound.arrival().toInstant(), back.departure().toInstant()).toMinutes();
                if (stay <= 0) continue;

                String fingerprint = originAirport + destination.airport() + outbound.departure()
                        + outbound.arrival() + back.departure() + back.arrival() + finalPrice;
                combinations.add(new TripOption(
                        UUID.nameUUIDFromBytes(fingerprint.getBytes(StandardCharsets.UTF_8)),
                        request.origin().strip(), destination.name(), outbound.departure(), outbound.arrival(),
                        back.departure(), back.arrival(), TransportType.FLIGHT, finalPrice, "PLN",
                        (long) outbound.durationMinutes() + back.durationMinutes(), stay,
                        outbound.stops(), back.stops(), SOURCE, null));
            }
        }
        // Każdy wariant dat może dać inną cenę; zachowujemy najlepszy dla tej konkretnej pary.
        return combinations.stream().min(Comparator.comparing(TripOption::price)
                .thenComparing(TripOption::timeAtDestinationMinutes, Comparator.reverseOrder()))
                .map(List::of).orElseGet(List::of);
    }

    private Map<String, String> flightParameters(TripSearchRequest request,
            AvailableDatePairs.DatePair pair, String origin, String arrival) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("engine", "google_flights");
        params.put("flight_type", "round_trip");
        params.put("departure_id", origin);
        params.put("arrival_id", arrival);
        params.put("outbound_date", pair.outbound().toString());
        params.put("return_date", pair.returnDeparture().toString());
        params.put("currency", "PLN");
        params.put("hl", "pl");
        params.put("gl", "pl");
        params.put("stops", request.directOnly() ? "nonstop" : "any");
        // Ceny/godziny filtrujemy dopiero po odczytaniu faktycznych odcinków.
        return params;
    }

    private Optional<LegCandidate> leg(JsonNode option, ZoneId fromZone, ZoneId toZone) {
        List<JsonNode> segments = items(option.path("flights"));
        if (segments.isEmpty()) return Optional.empty();
        JsonNode first = segments.getFirst().path("departure_airport");
        JsonNode last = segments.getLast().path("arrival_airport");
        String firstDate = text(first.path("date"));
        String firstTime = text(first.path("time"));
        String lastDate = text(last.path("date"));
        String lastTime = text(last.path("time"));
        if (firstDate == null || firstTime == null || lastDate == null || lastTime == null) return Optional.empty();
        try {
            OffsetDateTime departure = LocalDateTime.of(LocalDate.parse(firstDate), LocalTime.parse(firstTime))
                    .atZone(fromZone).toOffsetDateTime();
            OffsetDateTime arrival = LocalDateTime.of(LocalDate.parse(lastDate), LocalTime.parse(lastTime))
                    .atZone(toZone).toOffsetDateTime();
            long actualMinutes = Duration.between(departure.toInstant(), arrival.toInstant()).toMinutes();
            if (actualMinutes <= 0 || actualMinutes > 48 * 60) return Optional.empty();
            int minutes = option.path("total_duration").asInt((int) actualMinutes);
            if (minutes <= 0 || Math.abs(minutes - actualMinutes) > 180) return Optional.empty();
            return Optional.of(new LegCandidate(departure, arrival, minutes, segments.size() - 1,
                    decimal(option.path("price")), text(option.path("departure_token"))));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private List<JsonNode> options(JsonNode json) {
        List<JsonNode> result = new ArrayList<>(items(json.path("best_flights")));
        result.addAll(items(json.path("other_flights")));
        return result;
    }

    private static List<JsonNode> items(JsonNode node) {
        if (!node.isArray()) return List.of();
        List<JsonNode> list = new ArrayList<>();
        node.forEach(list::add);
        return list;
    }

    private static String text(JsonNode node) {
        return node.isTextual() && !node.asText().isBlank() ? node.asText() : null;
    }

    private static String firstText(JsonNode a, JsonNode b) {
        return Optional.ofNullable(text(a)).orElseGet(() -> text(b));
    }

    private static BigDecimal decimal(JsonNode node) {
        if (!node.isNumber()) return null;
        try { return new BigDecimal(node.asText()); }
        catch (NumberFormatException ignored) { return null; }
    }

    static String outboundDecision(LocalDate date, LocalTime time, int stops, boolean hasToken,
            AvailableDatePairs.DatePair pair, TripSearchRequest request) {
        if (!date.equals(pair.outbound())) return "inna data wyjazdu";
        if (!within(time, request.earliestOutboundDeparture(), request.latestOutboundDeparture()))
            return "godzina wyjazdu poza przedziałem";
        if (request.directOnly() && stops > 0) return "przesiadki przy włączonym directOnly";
        if (!hasToken) return "brak departure_token — nie można pobrać powrotu";
        return "pasuje — sprawdzamy powrót";
    }

    private static String outboundDecision(LegCandidate candidate,
            AvailableDatePairs.DatePair pair, TripSearchRequest request) {
        return outboundDecision(candidate.departure().toLocalDate(), candidate.departure().toLocalTime(),
                candidate.stops(), candidate.departureToken() != null, pair, request);
    }

    private static boolean within(LocalTime time, LocalTime start, LocalTime end) {
        return !time.isBefore(start) && !time.isAfter(end);
    }

    private record Destination(String name, String airport, double latitude, double longitude,
                               BigDecimal indication) { }

    private record LegCandidate(OffsetDateTime departure, OffsetDateTime arrival, int durationMinutes,
                                int stops, BigDecimal price, String departureToken) { }
}
