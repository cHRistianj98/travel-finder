package com.travelfinder.trip.bus;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Publiczne strony relacji FlixBus (nie linki do opłaconego kursu).
 * Nie dodajemy nieudokumentowanych parametrów dat i identyfikatorów sesji.
 * Dla nierozpoznanej relacji kierujemy do bezpiecznej strony głównej.
 */
public final class FlixBusBookingLinks {
    private static final String ROOT = "https://www.flixbus.pl/";
    private static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry("wroclaw", "wroclaw"),
            Map.entry("poznan", "poznan"),
            Map.entry("berlin", "berlin"),
            Map.entry("prague", "praga"), Map.entry("praga", "praga"),
            Map.entry("krakow", "krakow"),
            Map.entry("lodz", "lodz"), Map.entry("warszawa", "warszawa"),
            Map.entry("katowice", "katowice"), Map.entry("drezno", "drezno"),
            Map.entry("dresden", "drezno"), Map.entry("wieden", "wieden"),
            Map.entry("szklarska poreba", "szklarska-poreba")
    );
    private static final Set<String> KNOWN_WROCLAW_ROUTES = Set.of(
            "poznan", "berlin", "praga", "krakow", "lodz", "warszawa", "katowice",
            "drezno", "wieden", "szklarska-poreba", "port-lotniczy-berlin");

    private FlixBusBookingLinks() {}

    public static String route(String from, String to) {
        String fromName = city(from);
        String toName = city(to);
        if ("wroclaw".equals(fromName) && KNOWN_WROCLAW_ROUTES.contains(toName)) {
            return ROOT + "polaczenia-autobusowe/wroclaw-" + toName;
        }
        if ("wroclaw".equals(toName) && KNOWN_WROCLAW_ROUTES.contains(fromName)) {
            return ROOT + "polaczenia-autobusowe/" + fromName + "-wroclaw";
        }
        return ROOT;
    }

    private static String city(String raw) {
        if (raw == null) return "";
        String normalized = Normalizer.normalize(raw.toLowerCase(Locale.ROOT)
                        .replace('ł', 'l'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]+", " ").trim();
        if (normalized.startsWith("szklarska poreba")) return "szklarska-poreba";
        if (normalized.startsWith("berlin airport") || normalized.startsWith("berlin brandenburg airport")
                || normalized.startsWith("port lotniczy berlin")) return "port-lotniczy-berlin";
        // Z GTFS: "Berlin central bus station", "Poznan bus station", "Prague Florenc".
        for (var entry : NAMES.entrySet()) {
            if (normalized.equals(entry.getKey()) || normalized.startsWith(entry.getKey() + " ")) {
                return entry.getValue();
            }
        }
        return "";
    }
}
