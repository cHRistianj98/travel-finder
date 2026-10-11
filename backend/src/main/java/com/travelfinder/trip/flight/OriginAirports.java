package com.travelfinder.trip.flight;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** W MVP wybór miasta daje pojedyncze lotnisko główne, bez autosuggestu. */
public final class OriginAirports {
    private static final Map<String, String> AIRPORTS = Map.ofEntries(
            Map.entry("wroclaw", "WRO"), Map.entry("warszawa", "WAW"),
            Map.entry("krakow", "KRK"), Map.entry("gdansk", "GDN"),
            Map.entry("poznan", "POZ"), Map.entry("lodz", "LCJ"),
            Map.entry("katowice", "KTW"), Map.entry("szczecin", "SZZ"),
            Map.entry("lublin", "LUZ"), Map.entry("bydgoszcz", "BZG"),
            Map.entry("rzeszow", "RZE")
    );

    private OriginAirports() {
    }

    public static Optional<String> airportFor(String city) {
        String normalized = Normalizer.normalize(city.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('ł', 'l');
        return Optional.ofNullable(AIRPORTS.get(normalized));
    }
}
