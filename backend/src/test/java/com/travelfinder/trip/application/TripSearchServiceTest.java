package com.travelfinder.trip.application;

import com.travelfinder.trip.domain.TransportType;
import com.travelfinder.trip.domain.TripSearchRequest;
import com.travelfinder.trip.mock.MockTripData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripSearchServiceTest {
    private TripSearchService service;

    @BeforeEach
    void setUp() {
        service = new TripSearchService(new MockTripData(), request -> java.util.List.of());
    }

    private TripSearchRequest request(EnumSet<TransportType> modes, boolean direct, int budget) {
        return new TripSearchRequest("Wrocław", LocalDate.of(2026, 10, 16),
                LocalDate.of(2026, 10, 18), LocalTime.of(15, 0),
                LocalTime.of(23, 0), LocalTime.of(15, 0), LocalTime.of(23, 0),
                BigDecimal.valueOf(budget), "PLN", modes, direct);
    }

    @Test
    void returnsWeekendOffersForAllModes() {
        var results = service.search(request(EnumSet.allOf(TransportType.class), false, 500));
        assertEquals(4, results.size());
        assertTrue(results.stream().allMatch(option -> option.timeAtDestinationMinutes() > 0));
        assertTrue(results.stream().allMatch(option -> option.bookingUrl() == null));
    }

    @Test
    void directOnlyRemovesTransferOffers() {
        var results = service.search(request(EnumSet.allOf(TransportType.class), true, 500));
        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(option -> option.outboundTransfers() == 0));
    }

    @Test
    void respectsModesAndBudget() {
        var results = service.search(request(EnumSet.of(TransportType.FLIGHT), true, 200));
        assertEquals(0, results.size()); // w realnym trybie pusta odpowiedź providera = zero lotów
    }

    @Test
    void otherOriginStillReturnsDemoTrips() {
        var base = request(EnumSet.allOf(TransportType.class), true, 500);
        var alternative = new TripSearchRequest("Warszawa", base.outboundDate(), base.returnDate(),
                base.earliestOutboundDeparture(), base.latestOutboundDeparture(),
                base.earliestReturnArrival(), base.latestReturnArrival(), base.maxTransportBudget(),
                base.currency(), base.transportTypes(), base.directOnly());
        var results = service.search(alternative);
        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(option -> option.origin().equals("Warszawa")));
    }
}
