package com.travelfinder.trip.car;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OSRM: rzeczywista odległość i czas po sieci drogowej, NIE Haversine × umowna prędkość.
 * Publiczny serwer FOSSGIS jest zasobem społecznościowym: małe tablice 1→N,
 * globalnie maksymalnie 1 żądanie/sek., cache na dysku, circuit breaker po błędzie.
 * W produkcji/większym ruchu wskaż własny serwer OSRM przez CAR_ROUTING_URL.
 */
@Service
public class CarRoadRoutingService {
    public record Road(int km, long minutes) { }
    private record Saved(Road route, Instant expires) { }
    private static final int BATCH_SIZE = 35;
    private static final Duration CACHE_LIFETIME = Duration.ofDays(21);
    private static final Duration ERROR_COOLDOWN = Duration.ofMinutes(15);
    private final ObjectMapper mapper;
    private final HttpClient client;
    private final String baseUrl;
    private final boolean enabled;
    private final Path cacheFile;
    private final Map<String,Saved> routes = new ConcurrentHashMap<>();
    private volatile Instant retryAfter = Instant.EPOCH;
    private long lastRequestAt = 0L;

    @Autowired
    public CarRoadRoutingService(ObjectMapper mapper,
            @Value("${CAR_ROUTING_URL:https://routing.openstreetmap.de/routed-car}") String baseUrl,
            @Value("${CAR_ROUTING_ENABLED:true}") boolean enabled) {
        this(mapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
                baseUrl, enabled, Paths.get(".cache", "travel-finder-road-routes.tsv"));
    }

    CarRoadRoutingService(ObjectMapper mapper, HttpClient client, String baseUrl, boolean enabled, Path cacheFile) {
        this.mapper = Objects.requireNonNull(mapper);
        this.client = Objects.requireNonNull(client);
        if (!baseUrl.startsWith("https://") && !baseUrl.startsWith("http://localhost")
                && !baseUrl.startsWith("http://127.0.0.1"))
            throw new IllegalArgumentException("CAR_ROUTING_URL wymaga HTTPS lub lokalnego serwera OSRM");
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.enabled = enabled;
        this.cacheFile = cacheFile;
        restore();
    }

    /** Do jednej odpowiedzi zbiorczej: maksymalnie 35 destynacji i 1 źródło. */
    public Map<CarAttractionCatalog.Attraction,Road> roads(double fromLat, double fromLon,
            Collection<CarAttractionCatalog.Attraction> destinations) {
        if (!enabled || destinations.isEmpty()) return Map.of();
        Map<CarAttractionCatalog.Attraction,Road> found = new HashMap<>();
        Map<String,List<CarAttractionCatalog.Attraction>> missing = new LinkedHashMap<>();
        Instant now = Instant.now();
        for (var place : destinations) {
            String key = key(fromLat, fromLon, place);
            Saved cached = routes.get(key);
            if (cached != null && now.isBefore(cached.expires())) {
                found.put(place, cached.route());
            } else {
                // Te same współrzędne (np. dwa zabytki w obrębie placu) = tylko jeden punkt zapytania.
                missing.computeIfAbsent(key, ignored -> new ArrayList<>()).add(place);
            }
        }
        if (missing.isEmpty() || Instant.now().isBefore(retryAfter)) return found;
        List<List<CarAttractionCatalog.Attraction>> groups = new ArrayList<>(missing.values());
        boolean changed = false;
        for (int i=0;i<groups.size();i+=BATCH_SIZE) {
            if (Instant.now().isBefore(retryAfter)) break;
            var batch = groups.subList(i,Math.min(i+BATCH_SIZE,groups.size()));
            try {
                var fetched = fetch(fromLat,fromLon,batch.stream().map(group -> group.get(0)).toList());
                for (int n=0;n<batch.size();n++) {
                    Road road = fetched.get(n);
                    if (road == null) continue;
                    for (var place : batch.get(n)) {
                        found.put(place,road);
                        routes.put(key(fromLat,fromLon,place),new Saved(road,Instant.now().plus(CACHE_LIFETIME)));
                    }
                    changed = true;
                }
            } catch (Exception ex) {
                // Fail-open: wyniki nadal powstają z jawnie oznaczonym szacunkiem (nie znikają).
                retryAfter = Instant.now().plus(ERROR_COOLDOWN);
                break;
            }
        }
        if (changed) persist();
        return found;
    }

    private synchronized List<Road> fetch(double fromLat,double fromLon,
            List<CarAttractionCatalog.Attraction> attractions) throws Exception {
        // Limit max. 1 req/s, także przy równoczesnych wyszukiwaniach różnych użytkowników.
        long elapsed = System.currentTimeMillis() - lastRequestAt;
        if (elapsed < 1100 && lastRequestAt > 0) Thread.sleep(1100-elapsed);
        StringBuilder coordinates = new StringBuilder(coord(fromLon,fromLat));
        for (var place : attractions) {
            var access = roadAccess(place);
            coordinates.append(';').append(coord(access[1],access[0]));
        }
        URI uri = URI.create(baseUrl + "/table/v1/driving/" + coordinates
                + "?sources=0&annotations=duration%2Cdistance");
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json")
                .header("User-Agent", "TravelFinder/1.0 (personal travel planner; cached OSRM routes)")
                .GET().build();
        lastRequestAt = System.currentTimeMillis();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 || response.body().length() > 600_000)
            throw new IOException("OSRM HTTP " + response.statusCode());
        JsonNode json = mapper.readTree(response.body());
        if (!"Ok".equals(json.path("code").asText())) throw new IOException("OSRM table: " + json.path("code").asText());
        JsonNode times = json.path("durations").path(0);
        JsonNode metres = json.path("distances").path(0);
        JsonNode snapped = json.path("destinations");
        if (!times.isArray() || !metres.isArray() || times.size() != attractions.size()+1
                || metres.size() != attractions.size()+1) throw new IOException("Niekompletna tabela OSRM");
        List<Road> result = new ArrayList<>();
        for (int n=0;n<attractions.size();n++) {
            int col=n+1;
            double d=metres.path(col).asDouble(-1);
            double t=times.path(col).asDouble(-1);
            var access = roadAccess(attractions.get(n));
            double straight=CarAttractionCatalog.distanceKm(fromLat,fromLon,access[0],access[1]);
            double snapDistance=snapped.path(col).path("distance").asDouble(100000);
            // Nie podpisuj odległości do najbliższej drogi kilka km dalej jako dojazdu pod zabytek.
            if (d<1000 || t<=0 || d>4_000_000 || t>200_000 || snapDistance>2500
                    || d/1000d < straight*.92) {
                result.add(null); continue;
            }
            int km=(int)Math.ceil(d/1000d);
            // OSRM zwykle nie uwzględnia bieżących korków ani postoju/parkingu.
            // Rezerwa 12% + 8 minut + 15 minut postoju co ~2.5h jazdy.
            long raw=(long)Math.ceil(t/60d);
            long minutes=(long)Math.ceil(raw*1.12+8+Math.max(0,raw/150)*15);
            result.add(new Road(km,minutes));
        }
        return result;
    }

    /** Dla szczytów i atrakcji pieszych punkt startu jazdy, nie niedostępny samochodem sam zabytek. */
    static double[] roadAccess(CarAttractionCatalog.Attraction a) {
        String name = CarAttractionCatalog.normalize(a.destination());
        if (name.contains("pec pod snezkou") && name.contains("sniezka"))
            return new double[]{50.69357,15.73356}; // czeska strona: centrum Pec pod Sněžkou
        if (name.contains("sniezka")) return new double[]{50.776,15.723}; // polska strona: Karpacz/Wang
        if (name.contains("morskie oko")) return new double[]{49.254,20.102}; // okolice Palenicy Białczańskiej
        if (name.contains("szczeliniec wielki")) return new double[]{50.476,16.336}; // Karłów
        if (name.contains("dolina koscieliska")) return new double[]{49.276,19.870}; // Kiry
        if (name.contains("jaskinia niedzwiedzia")) return new double[]{50.250,16.853}; // Kletno: parking/dojście piesze
        return new double[]{a.lat(),a.lon()};
    }

    private static String coord(double lon,double lat) {
        return String.format(Locale.ROOT,"%.5f,%.5f",lon,lat);
    }
    private static String key(double lat,double lon,CarAttractionCatalog.Attraction a) {
        double[] access=roadAccess(a);
        return coord(lon,lat) + ":" + coord(access[1],access[0]);
    }
    private synchronized void restore() {
        try {
            if (!Files.isRegularFile(cacheFile)) return;
            for (String line : Files.readAllLines(cacheFile,StandardCharsets.UTF_8)) {
                String[] fields=line.split("\\t",-1);
                if (fields.length!=4) continue;
                Instant expire=Instant.ofEpochSecond(Long.parseLong(fields[1]));
                int km=Integer.parseInt(fields[2]); long minutes=Long.parseLong(fields[3]);
                if (Instant.now().isBefore(expire) && km>0 && minutes>0 && fields[0].length()<200)
                    routes.put(fields[0],new Saved(new Road(km,minutes),expire));
            }
        } catch (Exception ignored) { /* skasowany/uszkodzony cache nie blokuje aplikacji */ }
    }
    private synchronized void persist() {
        try {
            Files.createDirectories(cacheFile.getParent());
            Path temp=cacheFile.resolveSibling(cacheFile.getFileName()+".tmp");
            List<String> lines=new ArrayList<>();
            Instant now=Instant.now();
            for (var pair:routes.entrySet()) {
                Saved v=pair.getValue();
                if (v.expires().isAfter(now))
                    lines.add(pair.getKey()+"\t"+v.expires().getEpochSecond()+"\t"+v.route().km()+"\t"+v.route().minutes());
            }
            Files.write(temp,lines,StandardCharsets.UTF_8);
            try { Files.move(temp,cacheFile,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch (IOException e) { Files.move(temp,cacheFile,StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException ignored) { /* nadal działa cache w pamięci */ }
    }
}
