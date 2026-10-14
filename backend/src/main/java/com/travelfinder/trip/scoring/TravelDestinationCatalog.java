package com.travelfinder.trip.scoring;

import java.text.Normalizer;
import java.util.*;

/**
 * Ograniczony katalog redakcyjny (NIE kompletna liczba atrakcji).
 * Współrzędne są przybliżonymi środkami miejscowości, nie adresami terminali.
 * Brak rekordu -> brak punktów za atrakcje/odległość; nigdy wymyślona wartość.
 */
public final class TravelDestinationCatalog {
    private TravelDestinationCatalog() {}
    public record Place(String city, double lat, double lon, List<String> highlights) {}
    private static final Map<String, Place> PLACES = new HashMap<>();

    private static void register(String aliases, String city, double lat, double lon, String... attractions) {
        Place p = new Place(city, lat, lon, List.of(attractions));
        for (String alias : aliases.split("\\|")) PLACES.put(normalize(alias), p);
    }

    static {
        register("wroclaw", "Wrocław",51.108,17.039,"Rynek", "Ostrów Tumski", "Hala Stulecia", "Panorama Racławicka");
        register("warszawa|warsaw", "Warszawa",52.230,21.012,"Stare Miasto", "Łazienki Królewskie", "Muzeum Powstania Warszawskiego", "Pałac Kultury");
        register("poznan", "Poznań",52.406,16.925,"Stary Rynek", "Ostrów Tumski", "Palmiarnia", "Zamek Cesarski");
        register("krakow|cracow|krakow glowny", "Kraków",50.061,19.937,"Wawel", "Rynek Główny", "Kazimierz", "Sukiennice");
        register("gdansk", "Gdańsk",54.352,18.646,"Długi Targ", "Żuraw", "Europejskie Centrum Solidarności");
        register("katowice", "Katowice",50.265,19.024,"Spodek", "Nikiszowiec", "Strefa Kultury");
        register("szklarska poreba", "Szklarska Poręba",50.829,15.522,"Wodospad Kamieńczyka", "Wodospad Szklarki", "Zakręt Śmierci");
        register("jelenia gora", "Jelenia Góra",50.904,15.735,"Rynek", "Cieplice", "Zamek Chojnik");
        register("zielona gora", "Zielona Góra",51.936,15.506,"Palmiarnia", "Wzgórza Winne", "Deptak");
        register("lubin", "Lubin",51.399,16.201,"Rynek", "Park Wrocławski");
        register("lodz", "Łódź",51.759,19.458,"Ulica Piotrkowska", "Manufaktura", "EC1");
        register("szczecin", "Szczecin",53.429,14.553,"Wały Chrobrego", "Zamek Książąt Pomorskich", "Filharmonia");
        register("swiebodzin", "Świebodzin",52.247,15.533,"Figura Chrystusa Króla", "Rynek");
        register("gorzow wielkopolski", "Gorzów Wielkopolski",52.737,15.229,"Bulwar nad Wartą", "Katedra");
        register("gliwice", "Gliwice",50.295,18.669,"Radiostacja", "Rynek", "Palmiarnia");
        register("miedzyzdroje", "Międzyzdroje",53.928,14.451,"Molo", "Kawcza Góra", "Woliński Park Narodowy");
        register("harrachov", "Harrachov",50.773,15.431,"Skocznie narciarskie", "Wodospad Mumlawy");
        register("prague|praha|praga", "Praga",50.088,14.421,"Most Karola", "Zamek na Hradczanach", "Rynek Staromiejski", "Josefov");
        register("berlin", "Berlin",52.520,13.405,"Brama Brandenburska", "Wyspa Muzeów", "East Side Gallery", "Reichstag");
        register("dresden|drezno", "Drezno",51.050,13.738,"Frauenkirche", "Zwinger", "Tarasy Brühla");
        register("chemnitz", "Chemnitz",50.835,12.921,"Pomnik Karola Marksa", "Schlossberg");
        register("wieden|vienna|wien", "Wiedeń",48.208,16.373,"Schönbrunn", "Katedra św. Szczepana", "Belweder", "Hofburg");
        register("bratyslawa|bratislava", "Bratysława",48.148,17.107,"Zamek Bratysławski", "Stare Miasto");
        register("brno", "Brno",49.195,16.606,"Špilberk", "Katedra św. Piotra i Pawła");
        register("budapeszt|budapest", "Budapeszt",47.497,19.040,"Parlament", "Baszta Rybacka", "Wzgórze Zamkowe", "Łaźnie Széchenyiego");
        register("londyn|london", "Londyn",51.508,-0.128,"Big Ben", "Tower Bridge", "British Museum", "Hyde Park", "Buckingham Palace");
        register("paryz|paris", "Paryż",48.856,2.352,"Wieża Eiffla", "Luwr", "Montmartre", "Notre-Dame", "Łuk Triumfalny");
        register("rzym|rome|roma", "Rzym",41.903,12.496,"Koloseum", "Forum Romanum", "Panteon", "Watykan");
        register("mediolan|milan|milano", "Mediolan",45.464,9.190,"Duomo", "Galeria Wiktora Emanuela II", "Zamek Sforzów");
        register("barcelona", "Barcelona",41.387,2.170,"Sagrada Família", "Park Güell", "Dzielnica Gotycka", "Casa Batlló");
        register("amsterdam", "Amsterdam",52.368,4.904,"Kanały", "Rijksmuseum", "Dom Anny Frank");
        register("kopenhaga|copenhagen", "Kopenhaga",55.676,12.569,"Nyhavn", "Tivoli", "Mała Syrenka");
        register("sztokholm|stockholm", "Sztokholm",59.329,18.069,"Gamla Stan", "Muzeum Vasa", "Skansen");
        register("oslo", "Oslo",59.914,10.752,"Opera", "Park Vigelanda", "Twierdza Akershus");
        register("dublin", "Dublin",53.349,-6.260,"Trinity College", "Temple Bar", "Zamek Dubliński");
        register("palermo", "Palermo",38.116,13.361,"Katedra", "Teatro Massimo", "Quattro Canti");
        register("malaga|malaga", "Malaga",36.721,-4.420,"Alcazaba", "Teatr Rzymski", "Zamek Gibralfaro");
        register("alicante", "Alicante",38.345,-0.481,"Zamek Santa Bárbara", "Explanada de España", "Postiguet");
        register("larnaka|larnaca", "Larnaka",34.918,33.624,"Cerkiew św. Łazarza", "Słone Jezioro", "Finikoudes");
        register("bolonia|bologna", "Bolonia",44.495,11.343,"Piazza Maggiore", "Dwie Wieże", "Portyki");
        register("fue|fuerteventura", "Fuerteventura",28.501,-13.862,"Dunas de Corralejo", "Plaża Cofete", "Betancuria");
        register("teneryfa|tenerife|tci", "Teneryfa",28.292,-16.630,"Teide", "Anaga", "La Laguna");
        register("gran canaria|lpa", "Gran Canaria",28.100,-15.413,"Wydmy Maspalomas", "Roque Nublo", "Vegueta");
        register("ateny|athens", "Ateny",37.984,23.728,"Akropol", "Plaka", "Agora");
        register("nicea|nice", "Nicea",43.700,7.268,"Promenada Anglików", "Wzgórze Zamkowe", "Stare Miasto");
        register("belgrad|belgrade", "Belgrad",44.813,20.463,"Kalemegdan", "Knez Mihailova", "Cerkiew św. Sawy");
        register("reykjavik|reykjavik", "Reykjavík",64.146,-21.942,"Hallgrímskirkja", "Harpa", "Perlan");
        register("sewilla|seville", "Sewilla",37.389,-5.984,"Alcázar", "Katedra", "Plaza de España");
        register("walencja|valencia", "Walencja",39.470,-0.376,"Miasto Sztuki i Nauki", "Mercado Central", "Ogrody Turii");
        register("madryt|madrid", "Madryt",40.417,-3.703,"Prado", "Pałac Królewski", "Park Retiro");
        register("monachium|munich|munchen", "Monachium",48.137,11.575,"Marienplatz", "Englischer Garten", "Residenz");
        register("frankfurt", "Frankfurt",50.110,8.682,"Römerberg", "Muzea nad Menem", "Wieża Main Tower");
        register("antalya", "Antalya",36.895,30.707,"Kaleiçi", "Wodospady Düden", "Brama Hadriana");
        register("kut|kutaisi", "Kutaisi",42.268,42.705,"Katedra Bagrati", "Klasztor Gelati");
        register("tbs|tbilisi", "Tbilisi",41.693,44.801,"Twierdza Narikala", "Stare Miasto", "Łaźnie siarkowe");
        register("rak|marrakesz|marrakech", "Marrakesz",31.630,-8.008,"Dżamaa al-Fina", "Ogród Majorelle", "Pałac Bahia");
        register("rmo|kiszyniow|chisinau", "Kiszyniów",47.011,28.864,"Łuk Triumfalny", "Park Ștefan cel Mare");
        register("scv|suczawa|suceava", "Suczawa",47.651,26.255,"Twierdza Tronowa", "Monastyr św. Jana");
        register("ovd|oviedo", "Oviedo",43.362,-5.848,"Katedra San Salvador", "Stare Miasto", "Campo de San Francisco");
        register("evn|erywan|yerevan", "Erywań",40.181,44.513,"Plac Republiki", "Kaskady", "Matenadaran");
        register("zrh|zurych|zurich", "Zurych",47.376,8.542,"Jezioro Zuryskie", "Grossmünster", "Stare Miasto");
        register("bak|baku", "Baku",40.409,49.868,"Stare Miasto", "Flame Towers", "Bulwar nadkaspijski");
        register("hrg|hurghada", "Hurghada",27.257,33.812,"Marina", "Wyspy Giftun");
        register("ssh|szarm el szejk|sharm el sheikh", "Szarm el-Szejk",27.916,34.330,"Ras Mohammed", "Zatoka Naama");
        register("korfu|corfu", "Korfu",39.624,19.921,"Stare Miasto", "Stara Forteca", "Kanoni");
    }

    public static Place find(String name) {
        if (name == null) return null;
        String key = normalize(name);
        Place exact = PLACES.get(key);
        if (exact != null) return exact;
        // Prefiksy dopuszczamy dla opisów przystanków (np. Berlin central bus station).
        // Najdłuższy alias ma pierwszeństwo. Unikamy dopasowania krótkich kodów lotnisk w środku słów.
        return PLACES.entrySet().stream()
                .filter(e -> e.getKey().length() >= 4 && key.startsWith(e.getKey() + " "))
                .max(Comparator.comparingInt(e -> e.getKey().length()))
                .map(Map.Entry::getValue).orElse(null);
    }

    private static String normalize(String s) {
        return Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('ł','l')
                .replaceAll("[^a-z0-9]+", " ").trim();
    }

    public static Integer distanceKm(String origin, String destination) {
        Place a = find(origin), b = find(destination);
        if (a == null || b == null) return null;
        double lat1 = Math.toRadians(a.lat()), lat2 = Math.toRadians(b.lat());
        double dlat = lat2 - lat1, dlon = Math.toRadians(b.lon() - a.lon());
        double x = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);
        return (int) Math.round(6371 * 2 * Math.asin(Math.sqrt(Math.min(1, x))));
    }
}
