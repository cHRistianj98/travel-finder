package com.travelfinder.trip.domain;

import java.math.BigDecimal;
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
        String bookingUrl
) {
}
