package com.travelfinder.trip.domain;

import java.math.BigDecimal;
import com.travelfinder.trip.car.CarTripDetails;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Wspólny model podróży. Brak klas/identyfikatorów zewnętrznych API. */
public record TripOption(
        UUID id,
        String origin,
        String destination,
        OffsetDateTime outboundDeparture,
        OffsetDateTime outboundArrival,
        OffsetDateTime returnDeparture,
        OffsetDateTime returnArrival,
        TransportType transportType,
        BigDecimal price,
        String currency,
        long totalTravelDurationMinutes,
        long timeAtDestinationMinutes,
        int outboundTransfers,
        int returnTransfers,
        String provider,
        String bookingUrl,
        CarTripDetails carDetails
) {
    // Dotychczasowe providery wciąż mogą tworzyć TripOption bez danych samochodowych.
    public TripOption(UUID id, String origin, String destination, OffsetDateTime outboundDeparture,
            OffsetDateTime outboundArrival, OffsetDateTime returnDeparture, OffsetDateTime returnArrival,
            TransportType transportType, BigDecimal price, String currency,
            long totalTravelDurationMinutes, long timeAtDestinationMinutes,
            int outboundTransfers, int returnTransfers, String provider, String bookingUrl) {
        this(id, origin, destination, outboundDeparture, outboundArrival, returnDeparture, returnArrival,
                transportType, price, currency, totalTravelDurationMinutes, timeAtDestinationMinutes,
                outboundTransfers, returnTransfers, provider, bookingUrl, null);
    }
}
