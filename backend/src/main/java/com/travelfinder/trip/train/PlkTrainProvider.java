package com.travelfinder.trip.train;

import com.travelfinder.trip.domain.TripOption;
import com.travelfinder.trip.domain.TripSearchRequest;
import com.travelfinder.trip.domain.TransportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import com.travelfinder.trip.flight.FlightProviderException;
import tools.jackson.databind.JsonNode;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Wyszukiwanie dowolnego kierunku na podstawie oficjalnych planowanych tras PKP PLK.
 * Pierwsza iteracja: wyłącznie bezpośrednie pociągi tam/powrót, bez cen i przesiadek.
 * Zbiorcze odczyty per stacja+miesiąc; ponowne filtrowanie nie odpytuje API.
 */
@Component
public class PlkTrainProvider implements TrainSearchProvider {
    private static final Logger log = LoggerFactory.getLogger(PlkTrainProvider.class);
    private static final ZoneId POLAND = ZoneId.of("Europe/Warsaw");
    private static final String SCHEDULE_SEARCH = "https://rozkladq.plk-sa.pl/";
    private static final Map<String, String> HUBS = Map.ofEntries(
            Map.entry("Wrocław", "Wrocław Główny"), Map.entry("Warszawa", "Warszawa Centralna"),
            Map.entry("Kraków", "Kraków Główny"), Map.entry("Gdańsk", "Gdańsk Główny"),
            Map.entry("Poznań", "Poznań Główny"), Map.entry("Łódź", "Łódź Fabryczna"),
            Map.entry("Katowice", "Katowice"), Map.entry("Szczecin", "Szczecin Główny"),
            Map.entry("Lublin", "Lublin Główny"), Map.entry("Bydgoszcz", "Bydgoszcz Główna"),
            Map.entry("Białystok", "Białystok"), Map.entry("Rzeszów", "Rzeszów Główny"));

    private final PlkGateway gateway;
    private final int cacheHours;
    private final int maxTripDays;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Autowired
    public PlkTrainProvider(PlkGateway gateway,
            @Value("${plk.cache-hours:12}") int cacheHours,
            @Value("${plk.max-trip-days:5}") int maxTripDays) {
        this.gateway = gateway;
        this.cacheHours = Math.max(1, cacheHours);
        this.maxTripDays = Math.max(2, Math.min(maxTripDays, 14));
    }

    public PlkTrainProvider(PlkGateway gateway) {
        this(gateway, 12, 5);
    }

    @Override
    public List<TripOption> search(TripSearchRequest request) {
        String hub = HUBS.get(request.origin());
        if (hub == null) throw new IllegalArgumentException("Kolej: miasto wyjazdu nie jest jeszcze obsługiwane: " + request.origin());
        JsonNode stations = cached("stations:" + request.origin(), () -> gateway.stations(request.origin()));
        int stationId = stationId(stations, hub);
        if (stationId < 0) throw new FlightProviderException(HttpStatus.BAD_GATEWAY,
                "PKP PLK: nie znaleziono stacji " + hub + " w słowniku.");

        List<Leg> outbound = new ArrayList<>();
        List<Leg> inbound = new ArrayList<>();
        // Stabilny klucz cache: pełne miesiące. API pozwala na maks. 31 dni na pobranie.
        YearMonth first = YearMonth.from(request.outboundDate());
        YearMonth last = YearMonth.from(request.returnDate());
        for (YearMonth month = first; !month.isAfter(last); month = month.plusMonths(1)) {
            final YearMonth bucket = month;
            JsonNode root = cached("schedule:" + stationId + ":" + bucket,
                    () -> gateway.schedules(stationId, bucket.atDay(1), bucket.atEndOfMonth()));
            if (!root.path("routes").isArray()) throw new FlightProviderException(HttpStatus.BAD_GATEWAY,
                    "PKP PLK: niepoprawna struktura danych rozkładowych (brak routes).");
            Map<Integer, String> names = stationNames(root.path("dictionaries").path("stations"));
            collectLegs(root, stationId, names, request, outbound, inbound);
        }
        log.info("PLK {}: {} odjazdów, {} powrotów (przed łączeniem)", hub, outbound.size(), inbound.size());

        Map<Integer, List<Leg>> returns = inbound.stream()
                .collect(Collectors.groupingBy(Leg::otherStationId));
        Map<String, List<TripOption>> byDestination = new HashMap<>();
        for (Leg there : outbound) {
            List<Leg> backwards = returns.getOrDefault(there.otherStationId(), List.of());
            for (Leg back : backwards) {
                if (!back.departure().toInstant().isAfter(there.arrival().toInstant())) continue;
                long stay = Duration.between(there.arrival().toInstant(), back.departure().toInstant()).toMinutes();
                if (stay < 180 || stay > maxTripDays * 24L * 60) continue;
                if (back.arrival().toLocalDate().isAfter(request.returnDate())) continue;
                if (!isAvailable(request, there.departure().toLocalDate(), back.arrival().toLocalDate())) continue;
                if (there.otherStationId() == stationId) continue;
                String destination = cityName(there.otherStation());
                long total = Duration.between(there.departure().toInstant(), there.arrival().toInstant()).toMinutes()
                        + Duration.between(back.departure().toInstant(), back.arrival().toInstant()).toMinutes();
                TripOption option = new TripOption(UUID.nameUUIDFromBytes((there.id() + ":" + back.id()).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                        request.origin(), destination, there.departure(), there.arrival(), back.departure(), back.arrival(),
                        TransportType.TRAIN, null, "PLN", total, stay, 0, 0,
                        "PKP_PLK_TIMETABLE", SCHEDULE_SEARCH);
                byDestination.computeIfAbsent(destination, ignored -> new ArrayList<>()).add(option);
            }
        }
        // Bez dominacji jednego miasta: max 2 warianty/destynację, maks. 40 kart.
        Comparator<TripOption> score = Comparator
                .comparingLong(TripOption::timeAtDestinationMinutes).reversed()
                .thenComparingLong(TripOption::totalTravelDurationMinutes)
                .thenComparing(TripOption::outboundDeparture);
        return byDestination.values().stream()
                .flatMap(options -> options.stream().distinct().sorted(score).limit(2))
                .sorted(score).limit(40).toList();
    }

    private static boolean isAvailable(TripSearchRequest request, LocalDate from, LocalDate to) {
        if (!to.isAfter(from) || from.isBefore(request.outboundDate()) || to.isAfter(request.returnDate())) return false;
        Set<LocalDate> dates = request.availableDates();
        if (dates.isEmpty()) return true;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (!dates.contains(date)) return false;
        }
        return true;
    }

    private void collectLegs(JsonNode root, int hubId, Map<Integer, String> names, TripSearchRequest request,
                             List<Leg> outbound, List<Leg> inbound) {
        for (JsonNode route : root.path("routes")) {
            if (!route.path("stations").isArray() || !route.path("operatingDates").isArray()) continue;
            List<JsonNode> stops = new ArrayList<>();
            route.path("stations").forEach(stops::add);
            stops.sort(Comparator.comparingInt(stop -> stop.path("orderNumber").asInt(0)));
            for (int i = 0; i < stops.size(); i++) {
                if (stops.get(i).path("stationId").asInt(-1) != hubId) continue;
                for (JsonNode baseDay : route.path("operatingDates")) {
                    LocalDate operatingDate;
                    try { operatingDate = LocalDate.parse(baseDay.asText()); }
                    catch (RuntimeException ignored) { continue; }
                    for (int j = 0; j < stops.size(); j++) {
                        if (j == i) continue;
                        JsonNode other = stops.get(j);
                        int otherId = other.path("stationId").asInt(-1);
                        String stationName = names.get(otherId);
                        if (stationName == null || stationName.isBlank() || otherId == hubId) continue;
                        if (j > i) {
                            OffsetDateTime dep = timeAt(operatingDate, stops.get(i), "departure");
                            OffsetDateTime arr = timeAt(operatingDate, other, "arrival");
                            if (valid(dep, arr) && !dep.toLocalDate().isBefore(request.outboundDate())
                                    && !dep.toLocalDate().isAfter(request.returnDate())
                                    && within(dep.toLocalTime(), request.earliestOutboundDeparture(), request.latestOutboundDeparture())) {
                                outbound.add(new Leg(key(route, operatingDate, i, j), otherId, stationName, dep, arr));
                            }
                        } else {
                            OffsetDateTime dep = timeAt(operatingDate, other, "departure");
                            OffsetDateTime arr = timeAt(operatingDate, stops.get(i), "arrival");
                            if (valid(dep, arr) && !dep.toLocalDate().isBefore(request.outboundDate())
                                    && !dep.toLocalDate().isAfter(request.returnDate())
                                    && within(dep.toLocalTime(), request.earliestReturnDeparture(), request.latestReturnDeparture())) {
                                inbound.add(new Leg(key(route, operatingDate, j, i), otherId, stationName, dep, arr));
                            }
                        }
                    }
                }
            }
        }
    }

    private static String key(JsonNode route, LocalDate day, int from, int to) {
        return route.path("scheduleId").asText() + ":" + route.path("orderId").asText() + ":" + day + ":" + from + ":" + to;
    }

    private static boolean within(LocalTime time, LocalTime min, LocalTime max) {
        return !time.isBefore(min) && !time.isAfter(max);
    }

    private static boolean valid(OffsetDateTime departure, OffsetDateTime arrival) {
        if (departure == null || arrival == null) return false;
        long minutes = Duration.between(departure.toInstant(), arrival.toInstant()).toMinutes();
        return minutes >= 40 && minutes <= 12 * 60;
    }

    private static OffsetDateTime timeAt(LocalDate day, JsonNode stop, String which) {
        String value = stop.path(which + "Time").asText("");
        if (value.isBlank()) return null;
        int offset = stop.path(which + "Day").asInt(0);
        try {
            // PLK OpenAPI: string format duration (np. HH:mm:ss albo ISO-8601 PT...)
            long minutes;
            if (value.startsWith("P")) minutes = Duration.parse(value).toMinutes();
            else {
                String[] chunks = value.split(":");
                if (chunks.length < 2 || chunks.length > 3) return null;
                minutes = Long.parseLong(chunks[0]) * 60 + Long.parseLong(chunks[1]);
            }
            LocalDateTime local = day.plusDays(offset).atStartOfDay().plusMinutes(minutes);
            return local.atZone(POLAND).toOffsetDateTime();
        } catch (RuntimeException ignored) { return null; }
    }

    private static int stationId(JsonNode response, String preferred) {
        int best = -1;
        for (JsonNode station : response.path("stations")) {
            String name = station.path("name").asText("");
            if (name.equalsIgnoreCase(preferred)) return station.path("id").asInt(-1);
            if (best < 0 && name.toLowerCase(Locale.ROOT).startsWith(preferred.toLowerCase(Locale.ROOT)))
                best = station.path("id").asInt(-1);
        }
        return best;
    }

    private static Map<Integer, String> stationNames(JsonNode dictionary) {
        Map<Integer, String> names = new HashMap<>();
        if (dictionary == null || !dictionary.isObject()) return names;
        for (Map.Entry<String, JsonNode> entry : dictionary.properties()) {
            try {
                names.put(Integer.parseInt(entry.getKey()), entry.getValue().path("name").asText(""));
            } catch (NumberFormatException ignored) {
                // Pomijamy klucze niebedace identyfikatorami stacji.
            }
        }
        return names;
    }

    private static String cityName(String station) {
        String result = station.replaceAll("(?iu)\\s+(Główny|Główna|Centralna|Centralny|Fabryczna|Zachodnia|Zachodni|Wschodnia|Wschodni)$", "");
        return result.isBlank() ? station : result;
    }

    private JsonNode cached(String key, java.util.function.Supplier<JsonNode> fetcher) {
        CacheEntry cached = cache.get(key);
        if (cached != null && Instant.now().isBefore(cached.expiresAt())) {
            log.info("PKP PLK CACHE HIT: {}", key);
            return cached.body();
        }
        synchronized (cache) {
            cached = cache.get(key);
            if (cached != null && Instant.now().isBefore(cached.expiresAt())) return cached.body();
            log.info("PKP PLK CACHE MISS: {}", key);
            JsonNode body = fetcher.get();
            cache.put(key, new CacheEntry(body, Instant.now().plus(Duration.ofHours(cacheHours))));
            return body;
        }
    }

    private record CacheEntry(JsonNode body, Instant expiresAt) { }
    private record Leg(String id, int otherStationId, String otherStation,
                       OffsetDateTime departure, OffsetDateTime arrival) { }
}
