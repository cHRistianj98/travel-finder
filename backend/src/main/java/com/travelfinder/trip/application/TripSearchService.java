package com.travelfinder.trip.application;

import com.travelfinder.trip.domain.TripOption;
import com.travelfinder.trip.domain.TripSearchRequest;
import com.travelfinder.trip.mock.MockTripData;
import com.travelfinder.trip.flight.FlightSearchProvider;
import com.travelfinder.trip.domain.TransportType;
import com.travelfinder.trip.flight.OriginAirports;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TripSearchService {

    private final MockTripData mockTripData;
    private final FlightSearchProvider flightProvider;

    public TripSearchService(MockTripData mockTripData, FlightSearchProvider flightProvider) {
        this.mockTripData = mockTripData;
        this.flightProvider = flightProvider;
    }

    public List<TripOption> search(TripSearchRequest request) {
        if (!request.returnDate().isAfter(request.outboundDate())) {
            throw new IllegalArgumentException("Data powrotu musi być późniejsza niż data wyjazdu.");
        }
        if (!"PLN".equalsIgnoreCase(request.currency())) {
            throw new IllegalArgumentException("Na razie obsługujemy tylko walutę PLN.");
        }
        if (request.earliestOutboundDeparture().isAfter(request.latestOutboundDeparture())) {
            throw new IllegalArgumentException("Zakres godzin wyjazdu jest niepoprawny.");
        }
        if (request.earliestReturnArrival().isAfter(request.latestReturnArrival())) {
            throw new IllegalArgumentException("Zakres godzin powrotu jest niepoprawny.");
        }

        List<TripOption> candidates = new java.util.ArrayList<>();
        if (request.transportTypes().contains(TransportType.FLIGHT)) {
            // Przykład: Białystok nie ma obsługiwanego lotniska, ale autobus/auto nadal działa.
            if (OriginAirports.airportFor(request.origin()).isPresent()) {
                candidates.addAll(flightProvider.search(request));
            } else if (request.transportTypes().size() == 1) {
                throw new IllegalArgumentException("Dla tego miasta nie mamy jeszcze lotniska w wyszukiwarce lotów.");
            }
        }
        // Pozostałe środki transportu zachowują demonstracyjne mocki.
        candidates.addAll(mockTripData.forDates(request).stream()
                .filter(option -> option.transportType() != TransportType.FLIGHT)
                .toList());
        return candidates.stream()
                .filter(option -> request.transportTypes().contains(option.transportType()))
                .filter(option -> option.price().compareTo(request.maxTransportBudget()) <= 0)
                .filter(option -> !request.directOnly()
                        || (option.outboundTransfers() == 0 && option.returnTransfers() == 0))
                .filter(option -> !option.outboundDeparture().toLocalTime()
                        .isBefore(request.earliestOutboundDeparture()))
                .filter(option -> !option.outboundDeparture().toLocalTime()
                        .isAfter(request.latestOutboundDeparture()))
                .filter(option -> !option.returnArrival().toLocalTime()
                        .isBefore(request.earliestReturnArrival()))
                .filter(option -> !option.returnArrival().toLocalTime()
                        .isAfter(request.latestReturnArrival()))
                .filter(option -> option.timeAtDestinationMinutes() > 0)
                .toList();
    }
}
