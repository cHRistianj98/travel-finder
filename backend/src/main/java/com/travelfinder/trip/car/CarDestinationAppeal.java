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
    private static final Map<String, Assessment> BY_DESTINATION = buildAssessments();

    public static Assessment forDestination(String destination) {
        if (destination == null) return null;
        return BY_DESTINATION.get(CarAttractionCatalog.normalize(destination));
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
        for (CarAttractionCatalog.Attraction place : all) {
            int importance = Math.max(1, Math.min(5, place.interest()));
            double localDensity = 0;
            int nearby = 0;
            for (CarAttractionCatalog.Attraction other : all) {
                if (place == other) continue;
                double km = CarAttractionCatalog.distanceKm(place.lat(), place.lon(), other.lat(), other.lon());
                // Maksymalnie 25 km; punkt 5 km od celu waży więcej niż punkt 22 km dalej.
                if (km > 25) continue;
                nearby++;
                double quality = .35 + .65 * (Math.max(1, Math.min(5, other.interest())) - 1) / 4d;
                localDensity += quality * Math.pow(.5, km / 9d);
            }
            double richness = Math.min(1, localDensity / 2.7);
            int weekendTier = TIERS.getOrDefault(CarAttractionCatalog.normalize(place.city()),
                    Math.max(1, Math.min(5, (int) Math.round(2 + localDensity * 1.15))));
            // Wybitny punkt nie wystarcza sam w sobie na 3-dniowy pobyt; oceniamy też region.
            double landmark = (importance - 1) / 4d;
            double region = (weekendTier - 1) / 4d;
            double factor = Math.min(1, .08 + .40 * landmark + .34 * region + .18 * richness);
            String explanation = "Ranga głównej atrakcji " + importance + "/5 (katalog redakcyjny), "
                    + "potencjał weekendowy regionu " + weekendTier + "/5, "
                    + nearby + " innych punktów katalogu do ~25 km, z premią za bliskość i znaczenie. "
                    + "To heurystyka turystyczna, a nie oceny odwiedzających; nie uwzględnia pogody, "
                    + "godzin otwarcia ani wszystkich atrakcji danego regionu.";
            Assessment assessment = new Assessment(factor, importance, weekendTier, nearby, explanation);
            String key = CarAttractionCatalog.normalize(place.destination());
            if (result.putIfAbsent(key, assessment) != null) {
                throw new IllegalStateException("Powtórzona nazwa celu samochodowego: " + place.destination());
            }
        }
        return Map.copyOf(result);
    }
}
