package com.travelfinder.trip.bus;

import com.travelfinder.trip.domain.TripOption;
import java.util.Optional;

/**
 * Port dla autoryzowanego źródła cen. Adapter partnerskiego API można dodać po
 * otrzymaniu dokumentacji. GTFS nie zawiera taryf — stąd pusty wynik domyślny.
 */
@FunctionalInterface
public interface FlixBusFareGateway {
    Optional<FlixBusFareQuote> quoteFor(TripOption roundTrip);
}
