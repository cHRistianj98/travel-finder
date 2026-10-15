package com.travelfinder.trip.car;

import java.util.*;

/**
 * Redakcyjny szacunek atrakcyjności CELU samochodowego, nie ocena użytkowników.
 * Łączy znaczenie obiektu (interest 1..5 z katalogu), potencjał regionu
 * do spędzenia weekendu i pobliskie punkty z malejącą wagą odległości.
 * Brak wpisu => brak oceny, a nie 0 lub automatyczna premia.
 * Nie jest to liczba WSZYSTKICH atrakcji ani informacja o ich dostępności.
 */
public final class CarDestinationAppeal {
    private CarDestinationAppeal() { }

    public record Assessment(double factor, int landmarkImportance, int regionWeekendPotential,
            int localAttractions, String explanation) { }

    // 1 = miejsce raczej na krótki przystanek, 5 = bogaty potencjał na cały weekend.
    // Oceny redakcyjne, jawne i możliwe do skorygowania wraz z rozwojem bazy.
    // Dla pozostałych celów bazą jest zagęszczenie innych punktów z katalogu.
    private static final String REGION_TIERS = """
            Oleśnica|2
            Dzierżoniów|3
            Świdnica|3
            Wałbrzych|4
            Ząbkowice Śląskie|2
            Legnica|2
            Bolków|2
            Opole|3
            Kłodzko|4
            Polanica-Zdrój|4
            Karpacz|5
            Szklarska Poręba|5
            Jelenia Góra|4
            Kudowa-Zdrój|5
            Radków|5
            Międzygórze|4
            Kletno|4
            Moszna|3
            Adršpach|5
            Teplice nad Metují|5
            Pec pod Sněžkou|5
            Harrachov|4
            Liberec|4
            Děčín|5
            Hřensko|5
            Saksonia Szwajcarska|5
            Bad Schandau|5
            Praga|5
            Drezno|5
            Berlin|5
            Kraków|5
            Zakopane|5
            Kościelisko|5
            Ojców|4
            Wieliczka|3
            Zator|4
            Oświęcim|3
            Warszawa|5
            Poznań|4
            Wrocław|5
            Gdańsk|5
            Gdynia|4
            Sopot|4
            Toruń|4
            Malbork|4
            Łeba|5
            Świnoujście|4
            Czeski Krumlov|5
            Kutná Hora|4
            Brno|4
            Ostrawa|3
            Morawski Kras|5
            Jičín|5
            Litomyšl|3
            Görlitz|4
            Budziszyn|3
            Miśnia|3
            Lipsk|4
            Poczdam|5
            Hamburg|5
            Monachium|5
            Füssen|5
            Heidelberg|5
            Norymberga|4
            Rothenburg ob der Tauber|5
            Wyspa Rugia|5
            Hohenzollern|4
            """;

    private static final Map<String, Integer> TIERS = parseRegionTiers();
    private static volatile Map<String, Assessment> assessments = Map.of();
    private static volatile int assessedGeneration = -1;

    public static Assessment forDestination(String destination) {
        if (destination == null) return null;
        if (assessedGeneration != CarAttractionCatalog.generation()) refresh();
        return assessments.get(CarAttractionCatalog.normalize(destination));
    }

    public static synchronized void refresh() {
        if (assessedGeneration == CarAttractionCatalog.generation()) return;
        assessments = buildAssessments();
        assessedGeneration = CarAttractionCatalog.generation();
    }

    private static Map<String, Integer> parseRegionTiers() {
        Map<String, Integer> result = new HashMap<>();
        for (String line : REGION_TIERS.split("\\R")) {
            if (line.isBlank()) continue;
            String[] parts = line.trim().split("\\|", -1);
            if (parts.length != 2) throw new IllegalStateException("Niepoprawny potencjał regionu: " + line);
            int tier = Integer.parseInt(parts[1]);
            if (tier < 1 || tier > 5) throw new IllegalStateException("Ocena regionu poza skalą 1–5");
            result.put(CarAttractionCatalog.normalize(parts[0]), tier);
        }
        return Map.copyOf(result);
    }

    private static Map<String, Assessment> buildAssessments() {
        List<CarAttractionCatalog.Attraction> all = CarAttractionCatalog.all();
        Map<String, Assessment> result = new HashMap<>();
        // Siatka 0,2 stopnia ogranicza liczbę porównań dla kilku tysięcy miejsc.
        Map<Long, List<CarAttractionCatalog.Attraction>> grid = new HashMap<>();
        for (var a : all) grid.computeIfAbsent(cell(a.lat(), a.lon()), k -> new ArrayList<>()).add(a);
        for (CarAttractionCatalog.Attraction place : all) {
            int importance = Math.max(1, Math.min(5, place.interest()));
            int latCell = (int) Math.floor(place.lat() * 5), lonCell = (int) Math.floor(place.lon() * 5);
            double density = 0;
            int nearby = 0;
            Set<String> categories = new HashSet<>();
            for (int di = -2; di <= 2; di++) for (int dj = -3; dj <= 3; dj++) {
                for (var other : grid.getOrDefault(cellKey(latCell + di, lonCell + dj), List.of())) {
                    if (place == other) continue;
                    double km = CarAttractionCatalog.distanceKm(place.lat(), place.lon(), other.lat(), other.lon());
                    if (km > 28 || km < .05) continue;
                    nearby++;
                    if (!other.category().isBlank()) categories.add(other.category());
                    // Duże miasta nie mogą zgarniać 55 pkt przez 100 podobnych muzeów.
                    double quality = .25 + .75 * Math.max(1, other.interest()) / 5d;
                    density += quality * Math.pow(.5, km / 7d);
                }
            }
            int weekendTier = TIERS.getOrDefault(CarAttractionCatalog.normalize(place.city()),
                    Math.max(1, Math.min(5, (int) Math.round(1.7 + Math.log1p(density) * 1.25))));
            // 0–55 pkt: ranga obiektu 0–32, region 0–14, różnorodność i bliskość 0–9.
            // Obiekty importowane: ich znaczenie zależy od typu i jawnych tagów OSM
            // (Wikidata/Wikipedia/zabytek), a nie od bezpodstawnie założonego 5/5.
            int qualityPoints = place.source().equals("EDITORIAL")
                    ? Math.min(32, switch (importance) { case 1 -> 6; case 2 -> 10; case 3 -> 16; case 4 -> 24; default -> 31; })
                    : Math.min(32, Math.max(0, place.sourceQuality()));
            int regionPoints = (int) Math.round((weekendTier - 1) * 3.5);
            int nearbyPoints = (int) Math.round(9 * Math.min(1,
                    .75 * Math.min(1, density / 5d) + .25 * Math.min(1, categories.size() / 4d)));
            int attraction55 = Math.max(0, Math.min(55, qualityPoints + regionPoints + nearbyPoints));
            String explanation = "Atrakcyjność turystyczna " + attraction55 + "/55 pkt: ranga obiektu "
                    + qualityPoints + "/32, potencjał weekendowy regionu " + regionPoints
                    + "/14, pobliskie miejsca i różnorodność " + nearbyPoints + "/9 ("
                    + nearby + " innych punktów w promieniu ~28 km). Źródło: "
                    + (place.source().equals("EDITORIAL") ? "katalog redakcyjny" : "OpenStreetMap, ocena heurystyczna na podstawie tagów")
                    + ". Nie są to oceny odwiedzających; brak gwarancji dostępności i godzin otwarcia.";
            Assessment assessment = new Assessment(attraction55 / 55d, importance, weekendTier, nearby, explanation);
            result.putIfAbsent(CarAttractionCatalog.normalize(place.destination()), assessment);
        }
        return Map.copyOf(result);
    }

    private static long cell(double lat, double lon) {
        return cellKey((int) Math.floor(lat * 5), (int) Math.floor(lon * 5));
    }
    private static long cellKey(int a, int b) { return ((long) a << 32) | (b & 0xffffffffL); }

}
