package com.travelfinder.trip.application;

import com.travelfinder.trip.domain.TransportType;
import com.travelfinder.trip.domain.TripOption;
import com.travelfinder.trip.domain.TripSearchRequest;
import com.travelfinder.trip.domain.AvailabilityCalendar;
import com.travelfinder.trip.flight.FlightSearchProvider;
import com.travelfinder.trip.flight.OriginAirports;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Wyniki tylko od aktywnego providera; domyślnie cache Travelpayouts.
 * Pociąg/autobus/samochód pozostają nieaktywne, dopóki nie dodamy prawdziwych providerów.
 */
@Service
public class TripSearchService {
    private final FlightSearchProvider flightProvider;

    public TripSearchService(FlightSearchProvider flightProvider) {
        this.flightProvider = flightProvider;
    }

    public List<TripOption> search(TripSearchRequest request) {
        AvailabilityCalendar availability = AvailabilityCalendar.from(request);
        if (!"PLN".equalsIgnoreCase(request.currency())) {
            throw new IllegalArgumentException("Na razie obsługujemy tylko walutę PLN.");
        }
        if (request.earliestOutboundDeparture().isAfter(request.latestOutboundDeparture())) {
            throw new IllegalArgumentException("Zakres godzin wyjazdu jest niepoprawny.");
        }
        if (request.earliestReturnArrival().isAfter(request.latestReturnArrival())) {
            throw new IllegalArgumentException("Zakres godzin powrotu jest niepoprawny.");
        }
        if (!request.transportTypes().contains(TransportType.FLIGHT)) {
            throw new IllegalArgumentException("Na razie dostępne jest tylko wyszukiwanie prawdziwych lotów. Pociągi, autobusy i samochody dodamy później.");
        }
        if (OriginAirports.airportFor(request.origin()).isEmpty()) {
            throw new IllegalArgumentException("Nie mamy jeszcze obsługiwanego lotniska dla miasta: " + request.origin());
        }

        // Żadnego mockowania ani fallbacku do przykładowych podróży.
        // Brak oferty = pusta lista, błąd dostawcy = wyjątek.
        return flightProvider.search(request).stream()
                .filter(option -> option.transportType() == TransportType.FLIGHT)
                .filter(option -> availability.allowsTrip(option.outboundDeparture().toLocalDate(),
                        option.returnArrival().toLocalDate()))
                .filter(option -> !option.returnArrival().toInstant().isBefore(option.outboundArrival().toInstant()))
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
