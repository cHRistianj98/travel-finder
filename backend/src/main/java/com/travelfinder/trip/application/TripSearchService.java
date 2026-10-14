package com.travelfinder.trip.application;

import com.travelfinder.trip.domain.TransportType;
import com.travelfinder.trip.domain.TripOption;
import com.travelfinder.trip.domain.TripSearchRequest;
import com.travelfinder.trip.domain.AvailabilityCalendar;
import com.travelfinder.trip.flight.FlightSearchProvider;
import com.travelfinder.trip.flight.OriginAirports;
import com.travelfinder.trip.train.TrainSearchProvider;
import com.travelfinder.trip.bus.BusSearchProvider;
import com.travelfinder.trip.car.CarTripProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loty z cache Travelpayouts oraz bezpośrednie pociągi z rozkładu PKP PLK.
 * Żadnych mocków ani wymyślonych cen; BUS korzysta z rozkładu FlixBus GTFS.
 */
@Service
public class TripSearchService {
    private final FlightSearchProvider flightProvider;
    private final TrainSearchProvider trainProvider;
    private final BusSearchProvider busProvider;
    private final CarTripProvider carProvider;
    private final boolean trainAvailable;
    private final boolean busAvailable;

    @Autowired
    public TripSearchService(FlightSearchProvider flightProvider, TrainSearchProvider trainProvider,
            BusSearchProvider busProvider, CarTripProvider carProvider,
            @Value("${plk.enabled:false}") boolean plkEnabled,
            @Value("${plk.api-key:}") String plkApiKey,
            @Value("${bus.enabled:true}") boolean busEnabled) {
        this.flightProvider = flightProvider;
        this.trainProvider = trainProvider;
        this.busProvider = busProvider;
        this.carProvider = carProvider;
        this.busAvailable = busEnabled && busProvider != null;
        this.trainAvailable = plkEnabled && trainProvider != null
                && plkApiKey != null && !plkApiKey.isBlank() && !plkApiKey.startsWith("TU_WKLEJ_");
    }

    // Dotychczasowy konstruktor testowy (bez zależności samochodowej).
    public TripSearchService(FlightSearchProvider flightProvider, TrainSearchProvider trainProvider,
            BusSearchProvider busProvider, boolean plkEnabled, String plkApiKey, boolean busEnabled) {
        this(flightProvider, trainProvider, busProvider, null, plkEnabled, plkApiKey, busEnabled);
    }

    // Zachowujemy dotychczasowe konstruktory w testach.
    public TripSearchService(FlightSearchProvider flightProvider, TrainSearchProvider trainProvider,
            boolean plkEnabled, String plkApiKey) {
        this(flightProvider, trainProvider, null, plkEnabled, plkApiKey, false);
    }

    public TripSearchService(FlightSearchProvider flightProvider, TrainSearchProvider trainProvider,
            BusSearchProvider busProvider) {
        this(flightProvider, trainProvider, busProvider, false, "", true);
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

    public boolean isBusAvailable() {
        return busAvailable;
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
        boolean bus = request.transportTypes().contains(TransportType.BUS);
        boolean car = request.transportTypes().contains(TransportType.CAR);
        if (!flight && !train && !bus && !car) {
            throw new IllegalArgumentException("Wybierz co najmniej jeden środek transportu.");
        }
        if (flight && OriginAirports.airportFor(request.origin()).isEmpty()) {
            throw new IllegalArgumentException("Nie mamy jeszcze obsługiwanego lotniska dla miasta: " + request.origin());
        }
        if (train && !trainAvailable && !flight && !car && !(bus && busAvailable)) {
            throw new IllegalArgumentException("Kolej: oczekiwanie na dostęp do danych PKP PLK. "
                    + "Klucz musi zostać aktywowany, a integracja włączona w konfiguracji.");
        }

        if (bus && !busAvailable && !flight && !car && !(train && trainAvailable)) {
            throw new IllegalArgumentException("Autobusy: źródło rozkładów GTFS jest wyłączone w konfiguracji.");
        }

        java.util.List<TripOption> options = new java.util.ArrayList<>();
        if (flight) options.addAll(flightProvider.search(request).stream()
                .filter(option -> option.transportType() == TransportType.FLIGHT).toList());
        if (train && trainAvailable) options.addAll(trainProvider.search(request).stream()
                .filter(option -> option.transportType() == TransportType.TRAIN).toList());
        if (car && carProvider != null) options.addAll(carProvider.search(request));
        if (bus && busAvailable) options.addAll(busProvider.search(request).stream()
                .filter(option -> option.transportType() == TransportType.BUS).toList());

        if (car && carProvider == null && !flight && !(train && trainAvailable) && !(bus && busAvailable)) {
            throw new IllegalArgumentException("Wyszukiwanie samochodem jest chwilowo niedostępne.");
        }

        // Bez cen pociągów i autobusów nie odrzucamy ich według budżetu ani nie wymyślamy kwot.
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
