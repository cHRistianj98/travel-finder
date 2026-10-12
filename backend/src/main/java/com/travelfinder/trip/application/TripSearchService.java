package com.travelfinder.trip.application;

import com.travelfinder.trip.domain.TransportType;
import com.travelfinder.trip.domain.TripOption;
import com.travelfinder.trip.domain.TripSearchRequest;
import com.travelfinder.trip.domain.AvailabilityCalendar;
import com.travelfinder.trip.flight.FlightSearchProvider;
import com.travelfinder.trip.flight.OriginAirports;
import com.travelfinder.trip.train.TrainSearchProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loty z cache Travelpayouts oraz bezpośrednie pociągi z rozkładu PKP PLK.
 * Żadnych mocków i żadnych wymyślonych cen; BUS/CAR pozostają nieaktywne.
 */
@Service
public class TripSearchService {
    private final FlightSearchProvider flightProvider;
    private final TrainSearchProvider trainProvider;
    private final boolean trainAvailable;

    @Autowired
    public TripSearchService(FlightSearchProvider flightProvider, TrainSearchProvider trainProvider,
            @Value("${plk.enabled:false}") boolean plkEnabled,
            @Value("${plk.api-key:}") String plkApiKey) {
        this.flightProvider = flightProvider;
        this.trainProvider = trainProvider;
        this.trainAvailable = plkEnabled && trainProvider != null
                && plkApiKey != null && !plkApiKey.isBlank() && !plkApiKey.startsWith("TU_WKLEJ_");
    }

    // Konstruktory dla testów jednostkowych z fałszywym gatewayem (bez Springa).
    public TripSearchService(FlightSearchProvider flightProvider, TrainSearchProvider trainProvider) {
        this(flightProvider, trainProvider, true, "test-key");
    }

    public TripSearchService(FlightSearchProvider flightProvider) {
        this(flightProvider, null, false, "");
    }

    public boolean isTrainAvailable() {
        return trainAvailable;
    }

    public List<TripOption> search(TripSearchRequest request) {
        AvailabilityCalendar availability = AvailabilityCalendar.from(request);
        if (!"PLN".equalsIgnoreCase(request.currency())) {
            throw new IllegalArgumentException("Na razie obsługujemy tylko walutę PLN.");
        }
        if (request.earliestOutboundDeparture().isAfter(request.latestOutboundDeparture())) {
            throw new IllegalArgumentException("Zakres godzin wyjazdu jest niepoprawny.");
        }
        if (request.earliestReturnDeparture().isAfter(request.latestReturnDeparture())) {
            throw new IllegalArgumentException("Zakres godzin wylotu powrotnego jest niepoprawny.");
        }
        boolean flight = request.transportTypes().contains(TransportType.FLIGHT);
        boolean train = request.transportTypes().contains(TransportType.TRAIN);
        if (!flight && !train) {
            throw new IllegalArgumentException("Wybierz samolot lub pociąg. Autobusy i samochody dołączą później.");
        }
        if (flight && OriginAirports.airportFor(request.origin()).isEmpty()) {
            throw new IllegalArgumentException("Nie mamy jeszcze obsługiwanego lotniska dla miasta: " + request.origin());
        }
        if (train && !trainAvailable && !flight) {
            throw new IllegalArgumentException("Kolej: oczekiwanie na dostęp do danych PKP PLK. "
                    + "Klucz musi zostać aktywowany, a integracja włączona w konfiguracji.");
        }

        java.util.List<TripOption> options = new java.util.ArrayList<>();
        if (flight) options.addAll(flightProvider.search(request).stream()
                .filter(option -> option.transportType() == TransportType.FLIGHT).toList());
        if (train && trainAvailable) options.addAll(trainProvider.search(request).stream()
                .filter(option -> option.transportType() == TransportType.TRAIN).toList());

        // Bez cen pociągów z PLK nie odrzucamy ich według budżetu ani nie wymyślamy kwot.
        return options.stream()
                .filter(option -> availability.allowsTrip(option.outboundDeparture().toLocalDate(),
                        option.returnArrival().toLocalDate()))
                .filter(option -> !option.returnArrival().toInstant().isBefore(option.outboundArrival().toInstant()))
                .filter(option -> option.price() == null || option.price().compareTo(request.maxTransportBudget()) <= 0)
                .filter(option -> !request.directOnly()
                        || (option.outboundTransfers() == 0 && option.returnTransfers() == 0))
                .filter(option -> !option.outboundDeparture().toLocalTime()
                        .isBefore(request.earliestOutboundDeparture()))
                .filter(option -> !option.outboundDeparture().toLocalTime()
                        .isAfter(request.latestOutboundDeparture()))
                .filter(option -> !option.returnDeparture().toLocalTime()
                        .isBefore(request.earliestReturnDeparture()))
                .filter(option -> !option.returnDeparture().toLocalTime()
                        .isAfter(request.latestReturnDeparture()))
                .filter(option -> option.timeAtDestinationMinutes() > 0)
                .toList();
    }
}
