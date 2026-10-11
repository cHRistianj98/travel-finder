package com.travelfinder.trip.flight;

import net.iakovlev.timeshape.TimeZoneEngine;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Optional;

@Component
public class TravelTimeZones {
    // Ładujemy bazę dopiero przy pierwszym prawdziwym wyszukiwaniu lotów.
    private static final class Holder {
        private static final TimeZoneEngine ENGINE = TimeZoneEngine.initialize();
    }

    public Optional<ZoneId> forLocation(double latitude, double longitude) {
        return Holder.ENGINE.query(latitude, longitude);
    }
}
