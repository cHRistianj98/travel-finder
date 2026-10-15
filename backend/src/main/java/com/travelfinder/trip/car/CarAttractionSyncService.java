package com.travelfinder.trip.car;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Import atrakcji OpenStreetMap (ODbL) przez Overpass. Wczytuje cache bez sieci,
 * a uzupełnianie uruchamia w tle — nigdy nie blokuje startu Springa i wyszukiwania.
 * Status pozwala odróżnić 130 wpisów redakcyjnych od faktycznie pobranych danych.
 */
@Service
public class CarAttractionSyncService {
    public record Status(int editorial, int imported, int total, int target, boolean syncing,
                         String lastSuccess, String message, String attribution) { }
    record Hub(String country, String label, double lat, double lon) { }
    private final ObjectMapper mapper;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(12)).build();
    private final AtomicBoolean updating = new AtomicBoolean(false);
    private final Path cacheFile = Paths.get(".cache", "travel-finder-attractions-osm.json");
    private static final int TARGET = 1300; // próg >1000 z buforem na odrzucane duplikaty
    private static final int MAX_OSM = 2400;
    private final boolean enabled;
    private volatile String message = "Katalog redakcyjny gotowy; czekam na synchronizację OSM";
    private volatile String lastSuccess = "brak";
    private volatile List<CarAttractionCatalog.Attraction> imported = List.of();

    // Węzły sieci miejskiej i krajobrazowej. Każdy obszar ma ograniczony promień,
    // zamiast jednego bardzo kosztownego zapytania na większość Europy.
    private static final String HUB_DATA = """
        PL|Karkonosze|50.77|15.73
        PL|Wrocław|51.108|17.038
        CZ|Praga|50.075|14.438
        DE|Drezno|51.050|13.738
        PL|Kraków|50.064|19.945
        CZ|Brno|49.195|16.609
        DE|Berlin|52.520|13.405
        PL|Warszawa|52.230|21.012
        PL|Gdańsk|54.352|18.646
        AT|Wiedeń|48.208|16.373
        HU|Budapeszt|47.498|19.040
        SK|Bratysława|48.148|17.107
        DE|Monachium|48.137|11.576
        AT|Salzburg|47.800|13.045
        SI|Lublana|46.056|14.505
        HR|Zagrzeb|45.815|15.982
        IT|Wenecja|45.440|12.316
        PL|Poznań|52.407|16.925
        CZ|Ostrawa|49.820|18.263
        DE|Lipsk|51.340|12.374
        DE|Hamburg|53.550|10.000
        PL|Toruń|53.013|18.598
        PL|Zakopane|49.299|19.949
        SK|Tatry Wysokie|49.147|20.220
        AT|Hallstatt|47.563|13.650
        IT|Bolzano|46.498|11.354
        SI|Bled|46.368|14.114
        HR|Rijeka|45.328|14.442
        IT|Triest|45.649|13.777
        CZ|Český Krumlov|48.812|14.317
        DE|Norymberga|49.453|11.077
        PL|Łódź|51.759|19.457
        DE|Poczdam|52.400|13.067
        PL|Lublin|51.247|22.568
        PL|Szczecin|53.428|14.553
        PL|Malbork|54.039|19.029
        DE|Rugia|54.414|13.400
        DE|Kolonia|50.938|6.960
        DE|Heidelberg|49.398|8.672
        AT|Innsbruck|47.268|11.393
        HU|Balaton|46.914|17.890
        SK|Koszyce|48.716|21.261
        HR|Plitvice|44.883|15.622
        IT|Werona|45.439|10.993
        IT|Garda|45.615|10.563
        CH|Zurych|47.377|8.541
        CH|Lucerna|47.050|8.310
        DK|Kopenhaga|55.676|12.568
        LT|Wilno|54.687|25.280
        PL|Białystok|53.132|23.168
        PL|Olsztyn|53.778|20.480
        DE|Frankfurt|50.111|8.682
        DE|Stuttgart|48.776|9.177
        AT|Graz|47.070|15.439
        AT|Wachau|48.397|15.575
        HU|Eger|47.902|20.377
        SK|Bańska Bystrzyca|48.736|19.146
        SI|Maribor|46.555|15.647
        HR|Pula|44.868|13.848
        IT|Treviso|45.666|12.245
        CZ|Karlowe Wary|50.233|12.872
        DE|Görlitz|51.156|14.990
        PL|Kłodzko|50.434|16.653
        PL|Bieszczady|49.370|22.460
        PL|Sandomierz|50.680|21.749
        CZ|Liberec|50.766|15.056
        DE|Saksonia Szwajcarska|50.960|14.071
        SI|Postojna|45.775|14.214
        HU|Pecz|46.072|18.232
        """;

    public CarAttractionSyncService(ObjectMapper mapper,
                                   @Value("${travel.car.osm.enabled:true}") boolean enabled) {
        this.mapper = mapper;
        this.enabled = enabled;
        loadCache();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void afterStartup() {
        if (enabled && (!isCacheFresh() || CarAttractionCatalog.importedCount() < 1001)) {
            Thread.startVirtualThread(this::sync);
        } else if (!enabled) {
            message = "Synchronizacja wyłączona; działa katalog lokalny";
        }
    }

    public Status status() {
        return new Status(CarAttractionCatalog.editorialCount(), CarAttractionCatalog.importedCount(),
                CarAttractionCatalog.all().size(), TARGET, updating.get(), lastSuccess, message,
                "© OpenStreetMap contributors, ODbL 1.0 · www.openstreetmap.org/copyright");
    }

    private boolean isCacheFresh() {
        try { return Files.exists(cacheFile) &&
                Files.getLastModifiedTime(cacheFile).toInstant().isAfter(Instant.now().minus(Duration.ofDays(14))); }
        catch (Exception ex) { return false; }
    }

    private void loadCache() {
        if (!Files.exists(cacheFile)) return;
        try {
            String raw = Files.readString(cacheFile);
            if (raw.length() > 8_000_000) throw new IllegalStateException("Cache za duży");
            imported = parseCache(mapper.readTree(raw));
            CarAttractionCatalog.useImported(imported);
            message = "Wczytano " + CarAttractionCatalog.importedCount() + " atrakcji OSM z lokalnego cache";
            lastSuccess = Files.getLastModifiedTime(cacheFile).toInstant().toString();
        } catch (Exception e) {
            message = "Nie mogę odczytać cache OSM, pozostaje katalog redakcyjny: " + e.getClass().getSimpleName();
        }
    }

    public static List<CarAttractionCatalog.Attraction> parseCache(JsonNode root) {
        if (!root.isArray()) throw new IllegalArgumentException("Nieprawidłowy cache atrakcji");
        List<CarAttractionCatalog.Attraction> places = new ArrayList<>();
        for (JsonNode p : root) {
            String country = p.path("country").asText("");
            String city = p.path("city").asText("");
            String landmark = p.path("landmark").asText("");
            if (city.isBlank() || landmark.isBlank() || country.isBlank()) continue;
            places.add(new CarAttractionCatalog.Attraction(country, city, landmark,
                    p.path("lat").asDouble(Double.NaN), p.path("lon").asDouble(Double.NaN),
                    p.path("interest").asInt(1), p.path("category").asText("inne"),
                    p.path("source").asText("OSM"), p.path("sourceQuality").asInt(0)));
        }
        return places;
    }

    private synchronized void sync() {
        if (!updating.compareAndSet(false, true)) return;
        try {
            message = "Pobieram atrakcje z OpenStreetMap w tle (brak opłat/klucza API)";
            Map<String, CarAttractionCatalog.Attraction> byId = new LinkedHashMap<>();
            for (var place : imported) byId.put(place.source(), place);
            List<Hub> hubs = parseHubs();
            // Interleaving regionów: wyniki nie ograniczają się do okolic Wrocławia.
            for (int offset = 0; offset < hubs.size() && byId.size() < MAX_OSM; offset += 4) {
                List<Hub> batch = hubs.subList(offset, Math.min(hubs.size(), offset + 4));
                try {
                    List<CarAttractionCatalog.Attraction> found = fetchBatch(batch);
                    for (var place : found) byId.putIfAbsent(place.source(), place);
                    imported = List.copyOf(byId.values());
                    CarAttractionCatalog.useImported(imported);
                    saveCache(imported);
                    message = "Zebrano " + CarAttractionCatalog.importedCount() + " nowych atrakcji OSM; pobieram kolejne regiony";
                    // Po osiągnięciu celu kończymy pełną synchronizację, żeby oszczędzać darmowy serwer.
                    if (CarAttractionCatalog.importedCount() >= TARGET) break;
                } catch (Exception e) {
                    message = "Część danych OSM jest niedostępna (" + e.getClass().getSimpleName()
                            + "); zapisano " + CarAttractionCatalog.importedCount() + " atrakcji";
                }
                Thread.sleep(1300); // nie przeciążamy publicznego Overpass
            }
            int total = CarAttractionCatalog.importedCount();
            message = total >= 1001 ? "Katalog OSM gotowy: " + total + " zaimportowanych atrakcji"
                    : "Zebrano " + total + " atrakcji; do 1000 potrzebny jest ponowny import/dostęp do Overpass";
            lastSuccess = Instant.now().toString();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            message = "Import atrakcji przerwany; dotychczasowy katalog działa";
        } finally {
            updating.set(false);
        }
    }

    private List<CarAttractionCatalog.Attraction> fetchBatch(List<Hub> hubs) throws Exception {
        StringBuilder q = new StringBuilder("[out:json][timeout:75];(");
        for (Hub hub : hubs) {
            String around = "(around:35000," + hub.lat() + "," + hub.lon() + ")";
            q.append("nwr").append(around).append("[name][tourism~\"^(attraction|museum|viewpoint|zoo|gallery|theme_park|aquarium)$\"];");
            q.append("nwr").append(around).append("[name][historic~\"^(castle|fort|monument|archaeological_site)$\"];");
            q.append("nwr").append(around).append("[name][natural~\"^(peak|waterfall|cave_entrance)$\"];");
        }
        q.append(");out center qt;");
        JsonNode root = null;
        Exception latest = null;
        String form = "data=" + URLEncoder.encode(q.toString(), StandardCharsets.UTF_8);
        for (String endpoint : List.of("https://overpass-api.de/api/interpreter",
                                       "https://overpass.kumi.systems/api/interpreter")) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(86))
                        .header("User-Agent", "TravelFinder/1.0 (personal travel catalog; OSM ODbL)")
                        .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(form)).build();
                HttpResponse<String> result = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (result.statusCode() != 200 || result.body().length() > 14_000_000)
                    throw new IllegalStateException("Overpass HTTP " + result.statusCode());
                root = mapper.readTree(result.body());
                break;
            } catch (Exception ex) { latest = ex; }
        }
        if (root == null) throw latest == null ? new IllegalStateException("Brak odpowiedzi OSM") : latest;
        List<CarAttractionCatalog.Attraction> found = new ArrayList<>();
        for (JsonNode obj : root.path("elements")) {
            CarAttractionCatalog.Attraction place = fromOsm(obj, hubs);
            if (place != null) found.add(place);
        }
        // Nie wysyłamy wielkich list do klienta bez sortowania.
        found.sort(Comparator.comparingInt(CarAttractionCatalog.Attraction::sourceQuality).reversed());
        return found.stream().limit(1500).toList();
    }

    /** Czysta funkcja: testowalna bez sieci na realistycznym obiekcie OSM. */
    static CarAttractionCatalog.Attraction fromOsm(JsonNode obj, List<Hub> hubs) {
        JsonNode tags = obj.path("tags");
        String name = tags.path("name:pl").asText(tags.path("name").asText("")).trim();
        if (name.length() < 3 || name.length() > 95 || "yes".equalsIgnoreCase(name)
                || tags.path("access").asText("").matches("private|no|customers")
                || tags.path("disused").asText("").equals("yes")) return null;
        double lat = obj.path("lat").asDouble(obj.path("center").path("lat").asDouble(Double.NaN));
        double lon = obj.path("lon").asDouble(obj.path("center").path("lon").asDouble(Double.NaN));
        if (!Double.isFinite(lat) || !Double.isFinite(lon)) return null;
        Hub nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (Hub hub : hubs) {
            double km = CarAttractionCatalog.distanceKm(lat, lon, hub.lat(), hub.lon());
            if (km < distance) {distance = km; nearest = hub;}
        }
        if (nearest == null || distance > 40) return null;
        String tourism = tags.path("tourism").asText("");
        String historic = tags.path("historic").asText("");
        String natural = tags.path("natural").asText("");
        String cat = !natural.isBlank() ? natural : !historic.isBlank() ? historic : tourism;
        int base = switch (cat) {
            case "castle", "peak", "waterfall", "cave_entrance", "theme_park", "zoo" -> 19;
            case "museum", "monument", "fort", "archaeological_site", "aquarium" -> 17;
            case "viewpoint", "gallery" -> 12;
            default -> 10;
        };
        boolean wiki = !tags.path("wikipedia").asText("").isBlank();
        boolean wd = !tags.path("wikidata").asText("").isBlank();
        boolean heritage = !tags.path("heritage").asText("").isBlank()
                || !tags.path("heritage:operator").asText("").isBlank();
        int quality = Math.min(32, base + (wiki ? 6 : 0) + (wd ? 3 : 0) + (heritage ? 4 : 0));
        int interest = Math.max(1, Math.min(5, (int)Math.ceil(quality / 6.5)));
        String city = tags.path("addr:city").asText("").trim();
        if (city.isBlank()) city = "okolice " + nearest.label();
        if (CarAttractionCatalog.normalize(city).equals(CarAttractionCatalog.normalize(name))) city = "okolice " + nearest.label();
        String source = "OSM:" + obj.path("type").asText("node") + "/" + obj.path("id").asText("");
        if (source.endsWith("/")) return null;
        String taggedCountry = tags.path("addr:country").asText(nearest.country()).toUpperCase(Locale.ROOT);
        String country = Set.of("PL", "CZ", "DE", "AT", "SK", "HU", "SI", "HR", "IT", "CH", "DK", "LT").contains(taggedCountry)
                ? taggedCountry : nearest.country();
        var attraction = new CarAttractionCatalog.Attraction(country, city, name,
                lat, lon, interest, cat, source, quality);
        return CarAttractionCatalog.estimatedRoadKmFromWroclaw(attraction) <= 1500 ? attraction : null;
    }

    private static List<Hub> parseHubs() {
        List<Hub> hubs = new ArrayList<>();
        for (String line : HUB_DATA.split("\\R")) {
            if (line.isBlank()) continue;
            String[] p = line.trim().split("\\|");
            hubs.add(new Hub(p[0], p[1], Double.parseDouble(p[2]), Double.parseDouble(p[3])));
        }
        return hubs;
    }

    private void saveCache(List<CarAttractionCatalog.Attraction> list) throws Exception {
        Files.createDirectories(cacheFile.getParent());
        Path temp = cacheFile.resolveSibling(cacheFile.getFileName().toString() + ".tmp");
        Files.writeString(temp, mapper.writeValueAsString(list), StandardCharsets.UTF_8);
        try { Files.move(temp, cacheFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException e) { Files.move(temp, cacheFile, StandardCopyOption.REPLACE_EXISTING); }
    }
}
