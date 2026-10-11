package com.travelfinder.trip.flight;

import com.travelfinder.trip.domain.TransportType;
import com.travelfinder.trip.domain.TripOption;
import com.travelfinder.trip.domain.TripSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
 * Explore -> Google Flights (wylot) -> Google Flights (powrót dla departure_token).
 * Nie prezentujemy ceny ani czasu pobytu, dopóki nie mamy obu etapów trasy.
 */
@Component
public class SearchApiFlightProvider implements FlightSearchProvider {
    private static final Logger log = LoggerFactory.getLogger(SearchApiFlightProvider.class);
    private static final ZoneId POLAND = ZoneId.of("Europe/Warsaw");
    private static final String SOURCE = "SEARCHAPI_GOOGLE_FLIGHTS";

    private final SearchApiGateway gateway;
    private final TravelTimeZones timeZones;
    private final int maxDestinations;
    private final int maxOutbounds;

    public SearchApiFlightProvider(SearchApiGateway gateway, TravelTimeZones timeZones,
            @Value("${searchapi.max-destinations:3}") int maxDestinations,
            @Value("${searchapi.max-outbounds-per-destination:2}") int maxOutbounds) {
        this.gateway = gateway;
        this.timeZones = timeZones;
        this.maxDestinations = Math.max(1, Math.min(maxDestinations, 10));
        this.maxOutbounds = Math.max(1, Math.min(maxOutbounds, 4));
    }

    @Override
    public List<TripOption> search(TripSearchRequest request) {
        String originAirport = OriginAirports.airportFor(request.origin())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Na razie nie obsługujemy wylotów z miasta " + request.origin() + "."));

        Map<String, String> exploreParameters = new LinkedHashMap<>();
        exploreParameters.put("engine", "google_travel_explore");
        exploreParameters.put("departure_id", originAirport);
        exploreParameters.put("time_period", request.outboundDate() + ".." + request.returnDate());
        exploreParameters.put("travel_mode", "flights_only");
        exploreParameters.put("stops", request.directOnly() ? "nonstop" : "any");
        exploreParameters.put("max_price", request.maxTransportBudget().toPlainString());
        exploreParameters.put("currency", "PLN");
        exploreParameters.put("hl", "pl");
        exploreParameters.put("gl", "pl");

        JsonNode explore = gateway.search(exploreParameters);
        List<Destination> destinations = destinations(explore, request);
        List<TripOption> result = new ArrayList<>();
        int successfulDetails = 0;
        FlightProviderException firstFailure = null;

        for (Destination destination : destinations) {
            Optional<ZoneId> destinationZone = timeZones.forLocation(destination.latitude(), destination.longitude());
            if (destinationZone.isEmpty()) {
                log.warn("Brak strefy czasowej dla {} ({}, {})", destination.name(), destination.latitude(), destination.longitude());
                continue;
            }
            try {
                successfulDetails++;
                searchDestination(request, originAirport, destination, destinationZone.get()).ifPresent(result::add);
            } catch (FlightProviderException exception) {
                successfulDetails--;
                if (firstFailure == null) firstFailure = exception;
                log.warn("SearchApi: wyszukiwanie kierunku {} nieudane: {}", destination.name(), exception.getMessage());
            }
        }
        // Nie maskujemy awarii API jako prawidłowego „brak wyników”.
        if (!destinations.isEmpty() && successfulDetails == 0 && firstFailure != null) {
            throw firstFailure;
        }
        return result.stream().sorted(Comparator.comparing(TripOption::price)).toList();
    }

    private List<Destination> destinations(JsonNode root, TripSearchRequest request) {
        List<Destination> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (JsonNode item : items(root.path("destinations"))) {
            String airport = firstText(item.path("primary_airport"), item.path("flight").path("airport_code"));
            String name = text(item.path("name"));
            JsonNode coords = item.path("coordinates");
            if (name == null || airport == null || !airport.matches("[A-Z]{3}")
                    || !coords.isArray() || coords.size() < 2 || !coords.get(0).isNumber() || !coords.get(1).isNumber()) {
                continue;
            }
            if (!visited.add(airport)) continue;
            BigDecimal indication = decimal(item.path("flight").path("price"));
            if (indication != null && indication.compareTo(request.maxTransportBudget()) > 0) continue;
            result.add(new Destination(name, airport, coords.get(0).asDouble(), coords.get(1).asDouble(), indication));
        }
        result.sort(Comparator.comparing(Destination::indication, Comparator.nullsLast(Comparator.naturalOrder())));
        return result.stream().limit(maxDestinations).toList();
    }

    private Optional<TripOption> searchDestination(TripSearchRequest request, String airport,
            Destination destination, ZoneId destinationZone) {
        Map<String, String> params = flightParameters(request, airport, destination.airport());
        JsonNode outboundResult = gateway.search(params);
        List<LegCandidate> departures = options(outboundResult).stream()
                .map(node -> leg(node, POLAND, destinationZone))
                .flatMap(Optional::stream)
                .filter(leg -> leg.departure().toLocalDate().equals(request.outboundDate()))
                .filter(leg -> within(leg.departure().toLocalTime(), request.earliestOutboundDeparture(),
                        request.latestOutboundDeparture()))
                .filter(leg -> leg.stops() == 0 || !request.directOnly())
                .filter(leg -> leg.departureToken() != null)
                .sorted(Comparator.comparing(LegCandidate::price, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(maxOutbounds)
                .toList();

        List<TripOption> combinations = new ArrayList<>();
        for (LegCandidate outbound : departures) {
            Map<String, String> returnParams = new LinkedHashMap<>(params);
            returnParams.put("departure_token", outbound.departureToken());
            JsonNode returnResult = gateway.search(returnParams);
            for (JsonNode option : options(returnResult)) {
                Optional<LegCandidate> maybeReturn = leg(option, destinationZone, POLAND);
                if (maybeReturn.isEmpty()) continue;
                LegCandidate back = maybeReturn.get();
                BigDecimal finalPrice = back.price(); // Cena CAŁEJ pary z odpowiedzi po departure_token.
                if (finalPrice == null || finalPrice.compareTo(request.maxTransportBudget()) > 0) continue;
                if (request.directOnly() && back.stops() != 0) continue;
                if (!back.departure().toLocalDate().equals(request.returnDate())
                        || !back.arrival().toLocalDate().equals(request.returnDate())) continue;
                if (!within(back.arrival().toLocalTime(), request.earliestReturnArrival(),
                        request.latestReturnArrival())) continue;
                long stay = Duration.between(outbound.arrival().toInstant(), back.departure().toInstant()).toMinutes();
                if (stay <= 0) continue;
                String fingerprint = airport + destination.airport() + outbound.departure() + back.departure()
                        + outbound.arrival() + back.arrival() + finalPrice;
                combinations.add(new TripOption(
                        UUID.nameUUIDFromBytes(fingerprint.getBytes(StandardCharsets.UTF_8)),
                        request.origin().strip(), destination.name(),
                        outbound.departure(), outbound.arrival(), back.departure(), back.arrival(),
                        TransportType.FLIGHT, finalPrice, "PLN",
                        (long) outbound.durationMinutes() + back.durationMinutes(), stay,
                        outbound.stops(), back.stops(), SOURCE, null));
            }
        }
        return combinations.stream().min(Comparator.comparing(TripOption::price)
                .thenComparing(TripOption::timeAtDestinationMinutes, Comparator.reverseOrder()));
    }

    private Map<String, String> flightParameters(TripSearchRequest request, String origin, String arrival) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("engine", "google_flights");
        params.put("flight_type", "round_trip");
        params.put("departure_id", origin);
        params.put("arrival_id", arrival);
        params.put("outbound_date", request.outboundDate().toString());
        params.put("return_date", request.returnDate().toString());
        params.put("currency", "PLN");
        params.put("hl", "pl");
        params.put("gl", "pl");
        params.put("stops", request.directOnly() ? "nonstop" : "any");
        params.put("max_price", request.maxTransportBudget().toPlainString());
        // Filtry dostawcy są godzinowe; backend sprawdza finalnie dokładne HH:mm.
        params.put("outbound_times", request.earliestOutboundDeparture().getHour() + ","
                + request.latestOutboundDeparture().getHour() + ",0,23");
        params.put("return_times", "0,23," + request.earliestReturnArrival().getHour() + ","
                + request.latestReturnArrival().getHour());
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
            // Brak poprawnego czasu lokalnego - nie produkujemy niepewnego TripOption.
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

    private static boolean within(LocalTime time, LocalTime start, LocalTime end) {
        return !time.isBefore(start) && !time.isAfter(end);
    }

    private record Destination(String name, String airport, double latitude, double longitude,
                               BigDecimal indication) { }

    private record LegCandidate(OffsetDateTime departure, OffsetDateTime arrival, int durationMinutes,
                                int stops, BigDecimal price, String departureToken) { }
}
