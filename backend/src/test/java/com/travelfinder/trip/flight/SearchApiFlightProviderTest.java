package com.travelfinder.trip.flight;

import com.travelfinder.trip.domain.TransportType;
import com.travelfinder.trip.domain.TripSearchRequest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchApiFlightProviderTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private TripSearchRequest request(boolean direct, String departFrom, String departTo,
                                      String returnFrom, String returnTo) {
        return new TripSearchRequest("Wrocław", LocalDate.of(2026, 10, 16),
                LocalDate.of(2026, 10, 18), LocalTime.parse(departFrom), LocalTime.parse(departTo),
                LocalTime.parse(returnFrom), LocalTime.parse(returnTo), BigDecimal.valueOf(500),
                "PLN", EnumSet.of(TransportType.FLIGHT), direct);
    }

    private JsonNode json(String text) {
        return JSON.readTree(text);
    }

    private SearchApiFlightProvider provider(AtomicInteger calls) {
        SearchApiGateway gateway = params -> {
            calls.incrementAndGet();
            if ("google_travel_explore".equals(params.get("engine"))) {
                assertEquals("WRO", params.get("departure_id"));
                assertEquals("2026-10-16..2026-10-18", params.get("time_period"));
                return json("""
                        {"destinations":[{"name":"Praga","primary_airport":"PRG",
                          "coordinates":[50.08,14.42],"flight":{"price":180}}]}
                        """);
            }
            assertEquals("google_flights", params.get("engine"));
            assertEquals("round_trip", params.get("flight_type"));
            if (params.containsKey("departure_token")) {
                assertEquals("selected-outbound", params.get("departure_token"));
                return json("""
                        {"best_flights":[
                          {"price":279,"total_duration":90,"booking_token":"this-is-not-a-booking-url",
                           "flights":[{"departure_airport":{"id":"PRG","date":"2026-10-18","time":"19:10"},
                                        "arrival_airport":{"id":"WRO","date":"2026-10-18","time":"20:40"}}]},
                          {"price":189,"total_duration":90,
                           "flights":[{"departure_airport":{"id":"PRG","date":"2026-10-18","time":"23:00"},
                                        "arrival_airport":{"id":"WRO","date":"2026-10-19","time":"00:30"}}]}
                        ]}
                        """);
            }
            return json("""
                    {"best_flights":[{"price":160,"total_duration":85,"departure_token":"selected-outbound",
                      "flights":[{"departure_airport":{"id":"WRO","date":"2026-10-16","time":"18:00"},
                                   "arrival_airport":{"id":"PRG","date":"2026-10-16","time":"19:25"}}]}]}
                    """);
        };
        TravelTimeZones tz = new TravelTimeZones() {
            @Override
            public Optional<ZoneId> forLocation(double latitude, double longitude) {
                return Optional.of(ZoneId.of("Europe/Prague"));
            }
        };
        return new SearchApiFlightProvider(gateway, tz, 3, 2);
    }

    @Test
    void combinesSelectedOutboundAndReturnPriceWithoutSumming() {
        AtomicInteger calls = new AtomicInteger();
        var trips = provider(calls).search(request(true, "15:00", "23:00", "15:00", "23:00"));
        assertEquals(1, trips.size());
        var trip = trips.getFirst();
        assertEquals(new BigDecimal("279"), trip.price());
        assertEquals(0, trip.outboundTransfers());
        assertEquals(0, trip.returnTransfers());
        assertEquals(2865, trip.timeAtDestinationMinutes());
        assertEquals("SEARCHAPI_GOOGLE_FLIGHTS", trip.provider());
        assertEquals(null, trip.bookingUrl());
        assertEquals(3, calls.get());
    }

    @Test
    void rejectsReturnArrivalOutsideAllowedHours() {
        var trips = provider(new AtomicInteger()).search(request(true, "15:00", "23:00", "15:00", "19:00"));
        assertTrue(trips.isEmpty());
    }

    @Test
    void rejectsOutboundOutsideAllowedHoursBeforeTokenRequest() {
        AtomicInteger calls = new AtomicInteger();
        var trips = provider(calls).search(request(true, "20:00", "23:00", "15:00", "23:00"));
        assertTrue(trips.isEmpty());
        assertEquals(2, calls.get());
    }
}
