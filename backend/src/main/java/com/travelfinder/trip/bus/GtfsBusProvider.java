package com.travelfinder.trip.bus;

import com.travelfinder.trip.domain.AvailabilityCalendar;
import com.travelfinder.trip.domain.TransportType;
import com.travelfinder.trip.domain.TripOption;
import com.travelfinder.trip.domain.TripSearchRequest;
import com.travelfinder.trip.flight.FlightProviderException;
import com.travelfinder.trip.flight.TravelTimeZones;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.Normalizer;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Odkrywanie bezpośrednich kursów autobusowych tam i z powrotem z rzeczywistego GTFS.
 * Rozkład pochodzi z FlixBus Europe; nie ma tu cen ani potwierdzonej dostępności biletów.
 * Jedno archiwum, lokalne filtrowanie kolejnych dat/budżetów, obsługa dni >24:00 i calendar_dates.
 */
@Component
public class GtfsBusProvider implements BusSearchProvider {
    private static final Logger log = LoggerFactory.getLogger(GtfsBusProvider.class);
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Warsaw");
    private static final String BOOKING = "https://www.flixbus.pl/";
    private static final DateTimeFormatter GTFS_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final List<String> ORIGINS = List.of("Wrocław", "Warszawa", "Kraków", "Gdańsk", "Poznań", "Łódź",
            "Katowice", "Szczecin", "Lublin", "Bydgoszcz", "Białystok", "Rzeszów");

    private final GtfsArchiveGateway gateway;
    private final TravelTimeZones timeZones;
    private final int maxTripDays;
    private final int maxResults;
    private final Map<String, ZoneId> zoneCache = new ConcurrentHashMap<>();
    private volatile Loaded loaded;

    @Autowired
    public GtfsBusProvider(GtfsArchiveGateway gateway, TravelTimeZones timeZones,
            @Value("${bus.max-trip-days:5}") int maxTripDays,
            @Value("${bus.max-results:40}") int maxResults) {
        this.gateway = gateway;
        this.timeZones = timeZones;
        this.maxTripDays = Math.max(2, Math.min(14, maxTripDays));
        this.maxResults = Math.max(1, Math.min(200, maxResults));
    }

    public GtfsBusProvider(GtfsArchiveGateway gateway) {
        this(gateway, new TravelTimeZones(), 5, 40);
    }

    @Override
    public List<TripOption> search(TripSearchRequest request) {
        AvailabilityCalendar availability = AvailabilityCalendar.from(request);
        String origin = normalize(request.origin());
        if (ORIGINS.stream().map(GtfsBusProvider::normalize).noneMatch(origin::equals)) {
            throw new IllegalArgumentException("Autobusy: nieobsługiwane miasto wyjazdu: " + request.origin());
        }
        Gtfs gtfs = snapshot();
        List<Leg> outbound = new ArrayList<>();
        List<Leg> inbound = new ArrayList<>();
        LocalDate from = request.outboundDate();
        LocalDate to = request.returnDate();
        // W GTFS godzina 26:00 oznacza kurs kolejnego dnia kalendarzowego.
        LocalDate earliestService = from.minusDays(3);
        for (Schedule schedule : gtfs.schedules()) {
            List<StopTime> times = schedule.times();
            for (int i = 0; i < times.size(); i++) {
                StopTime atOrigin = times.get(i);
                if (!normalize(atOrigin.stop().city()).equals(origin)) continue;
                for (LocalDate serviceDay = earliestService; !serviceDay.isAfter(to); serviceDay = serviceDay.plusDays(1)) {
                    if (!gtfs.active(schedule.serviceId(), serviceDay)) continue;
                    OffsetDateTime depFromOrigin = actual(serviceDay, atOrigin.departure(), schedule.zone(), atOrigin.stop());
                    OffsetDateTime arrAtOrigin = actual(serviceDay, atOrigin.arrival(), schedule.zone(), atOrigin.stop());
                    for (int j = 0; j < times.size(); j++) {
                        if (i == j) continue;
                        StopTime other = times.get(j);
                        if (normalize(other.stop().city()).equals(origin)) continue;
                        String destination = other.stop().city();
                        if (j > i && atOrigin.pickup() && other.dropoff() && depFromOrigin != null) {
                            OffsetDateTime arr = actual(serviceDay, other.arrival(), schedule.zone(), other.stop());
                            if (validLeg(depFromOrigin, arr) && between(depFromOrigin.toLocalDate(), from, to)
                                    && within(depFromOrigin.toLocalTime(), request.earliestOutboundDeparture(), request.latestOutboundDeparture())) {
                                outbound.add(new Leg(schedule.tripId() + ':' + serviceDay + ':' + i + ':' + j,
                                        destination, depFromOrigin, arr));
                            }
                        } else if (j < i && other.pickup() && atOrigin.dropoff() && arrAtOrigin != null) {
                            OffsetDateTime dep = actual(serviceDay, other.departure(), schedule.zone(), other.stop());
                            if (validLeg(dep, arrAtOrigin) && between(dep.toLocalDate(), from, to)
                                    && within(dep.toLocalTime(), request.earliestReturnDeparture(), request.latestReturnDeparture())
                                    && !arrAtOrigin.toLocalDate().isAfter(to)) {
                                inbound.add(new Leg(schedule.tripId() + ':' + serviceDay + ':' + j + ':' + i,
                                        destination, dep, arrAtOrigin));
                            }
                        }
                    }
                }
            }
        }
        log.info("FlixBus GTFS: {} odjazdów i {} powrotów z {} przed łączeniem", outbound.size(), inbound.size(), request.origin());
        Map<String, List<Leg>> returns = inbound.stream().collect(Collectors.groupingBy(leg -> normalize(leg.city())));
        // Dla każdego kierunku zachowujemy najwyżej dwa dobre warianty, nie mnożymy kart.
        Map<String, List<TripOption>> options = new HashMap<>();
        for (Leg go : outbound) {
            for (Leg back : returns.getOrDefault(normalize(go.city()), List.of())) {
                long stay = Duration.between(go.arrival().toInstant(), back.departure().toInstant()).toMinutes();
                if (stay < 180 || stay > maxTripDays * 1440L) continue;
                if (!availability.allowsTrip(go.departure().toLocalDate(), back.arrival().toLocalDate())) continue;
                long total = Duration.between(go.departure().toInstant(), go.arrival().toInstant()).toMinutes()
                        + Duration.between(back.departure().toInstant(), back.arrival().toInstant()).toMinutes();
                if (total <= 0) continue;
                String key = go.id() + "::" + back.id();
                TripOption option = new TripOption(UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)),
                        request.origin(), go.city(), go.departure(), go.arrival(), back.departure(), back.arrival(),
                        TransportType.BUS, null, "PLN", total, stay, 0, 0, "FLIXBUS_GTFS_TIMETABLE", BOOKING);
                options.computeIfAbsent(normalize(go.city()), ignored -> new ArrayList<>()).add(option);
            }
        }
        Comparator<TripOption> ranking = Comparator.comparingLong(TripOption::timeAtDestinationMinutes).reversed()
                .thenComparingLong(TripOption::totalTravelDurationMinutes).thenComparing(TripOption::outboundDeparture);
        return options.values().stream().flatMap(list -> list.stream().sorted(ranking).limit(2))
                .sorted(ranking).limit(maxResults).toList();
    }

    private Gtfs snapshot() {
        try {
            Path path = gateway.archive();
            long stamp = Files.getLastModifiedTime(path).toMillis();
            Loaded current = loaded;
            if (current != null && current.stamp() == stamp && current.path().equals(path)) return current.gtfs();
            synchronized (this) {
                current = loaded;
                if (current != null && current.stamp() == stamp && current.path().equals(path)) return current.gtfs();
                Gtfs parsed = parse(path);
                loaded = new Loaded(path, stamp, parsed);
                log.info("FlixBus GTFS: {} kursów obejmuje dostępne polskie miasta", parsed.schedules().size());
                return parsed;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FlightProviderException(HttpStatus.BAD_GATEWAY, "Przerwano pobieranie rozkładu autobusów.");
        } catch (IOException e) {
            log.warn("FlixBus GTFS: {}", e.getMessage());
            throw new FlightProviderException(HttpStatus.BAD_GATEWAY,
                    "Nie udało się pobrać rozkładu FlixBus GTFS. Sprawdź połączenie internetowe lub spróbuj ponownie.");
        }
    }

    private Gtfs parse(Path archive) throws IOException {
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            Map<String, Stop> stops = new HashMap<>();
            rows(zip, "stops.txt", row -> {
                String id = row.get("stop_id"), name = row.get("stop_name");
                if (id.isBlank() || name.isBlank()) return;
                String city = cityName(name);
                stops.put(id, new Stop(id, city, number(row.get("stop_lat")), number(row.get("stop_lon")), row.get("stop_timezone")));
            });
            Map<String, ZoneId> agencies = new HashMap<>();
            rows(zip, "agency.txt", row -> {
                String zone = row.get("agency_timezone");
                if (zone.isBlank()) return;
                try { agencies.put(row.get("agency_id"), ZoneId.of(zone)); }
                catch (DateTimeException ignored) { /* nieprawidłowe strefy ignorujemy */ }
            });
            ZoneId fallback = agencies.values().stream().findFirst().orElse(DEFAULT_ZONE);
            Map<String, Route> busRoutes = new HashMap<>();
            rows(zip, "routes.txt", row -> {
                String kind = row.get("route_type");
                int code = integer(kind, -1);
                if (code == 3 || (code >= 700 && code < 800))
                    busRoutes.put(row.get("route_id"), new Route(agencies.getOrDefault(row.get("agency_id"), fallback)));
            });
            // Pierwsze przejście: zapamiętujemy tylko kursy obsługujące miasta startowe.
            Set<String> originStops = stops.values().stream().filter(s -> ORIGINS.stream()
                    .anyMatch(origin -> normalize(origin).equals(normalize(s.city()))))
                    .map(Stop::id).collect(Collectors.toSet());
            Set<String> relevantTrips = new HashSet<>();
            rows(zip, "stop_times.txt", row -> {
                if (originStops.contains(row.get("stop_id"))) relevantTrips.add(row.get("trip_id"));
            });
            Map<String, Trip> trips = new HashMap<>();
            rows(zip, "trips.txt", row -> {
                String id = row.get("trip_id");
                Route route = busRoutes.get(row.get("route_id"));
                if (route != null && relevantTrips.contains(id))
                    trips.put(id, new Trip(id, row.get("service_id"), route.zone()));
            });
            Map<String, List<StopTime>> times = new HashMap<>();
            rows(zip, "stop_times.txt", row -> {
                String id = row.get("trip_id");
                if (!trips.containsKey(id)) return;
                Stop stop = stops.get(row.get("stop_id"));
                if (stop == null) return;
                int arrival = seconds(row.get("arrival_time"));
                int departure = seconds(row.get("departure_time"));
                if (arrival < 0 || departure < 0) return;
                times.computeIfAbsent(id, ignored -> new ArrayList<>()).add(new StopTime(stop, arrival, departure,
                        integer(row.get("stop_sequence"), 0), (row.get("pickup_type").isBlank() || "0".equals(row.get("pickup_type"))),
                        (row.get("drop_off_type").isBlank() || "0".equals(row.get("drop_off_type")))));
            });
            Set<String> neededServices = trips.values().stream().map(Trip::serviceId).collect(Collectors.toSet());
            Map<String, CalendarRule> calendar = new HashMap<>();
            rows(zip, "calendar.txt", row -> {
                String id = row.get("service_id");
                if (!neededServices.contains(id)) return;
                try {
                    boolean[] weekly = new boolean[7];
                    String[] cols = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
                    for (int i = 0; i < cols.length; i++) weekly[i] = "1".equals(row.get(cols[i]));
                    calendar.put(id, new CalendarRule(LocalDate.parse(row.get("start_date"), GTFS_DATE),
                            LocalDate.parse(row.get("end_date"), GTFS_DATE), weekly));
                } catch (RuntimeException ignored) { /* wiersz niepoprawny */ }
            });
            Map<String, Map<LocalDate, Boolean>> exceptions = new HashMap<>();
            rows(zip, "calendar_dates.txt", row -> {
                String id = row.get("service_id");
                if (!neededServices.contains(id)) return;
                try {
                    LocalDate day = LocalDate.parse(row.get("date"), GTFS_DATE);
                    String kind = row.get("exception_type");
                    if ("1".equals(kind) || "2".equals(kind))
                        exceptions.computeIfAbsent(id, ignored -> new HashMap<>()).put(day, "1".equals(kind));
                } catch (RuntimeException ignored) { /* wiersz niepoprawny */ }
            });
            List<Schedule> schedules = new ArrayList<>();
            for (Trip trip : trips.values()) {
                List<StopTime> events = times.getOrDefault(trip.id(), List.of());
                if (events.size() < 2) continue;
                events.sort(Comparator.comparingInt(StopTime::sequence));
                schedules.add(new Schedule(trip.id(), trip.serviceId(), trip.zone(), List.copyOf(events)));
            }
            if (stops.isEmpty() || busRoutes.isEmpty()) throw new IOException("Archiwum GTFS nie zawiera przystanków lub linii autobusowych.");
            return new Gtfs(List.copyOf(schedules), Map.copyOf(calendar), Map.copyOf(exceptions));
        }
    }

    // GTFS jest archiwum CSV. Czytamy wiersze strumieniowo, bez rozpakowywania dużych plików na dysk.
    private static void rows(ZipFile zip, String filename, Consumer<CsvRow> consumer) throws IOException {
        ZipEntry entry = zip.getEntry(filename);
        if (entry == null) return; // calendar.txt bywa pominięty, jeśli użyto calendar_dates.txt
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8), 128 * 1024)) {
            String first = reader.readLine();
            if (first == null) return;
            List<String> names = csv(first.replace("\uFEFF", ""));
            Map<String, Integer> columns = new HashMap<>();
            for (int i = 0; i < names.size(); i++) columns.put(names.get(i), i);
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) consumer.accept(new CsvRow(columns, csv(line)));
            }
        }
    }

    private static List<String> csv(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { cell.append('"'); i++; }
                else quoted = !quoted;
            } else if (c == ',' && !quoted) { result.add(cell.toString()); cell.setLength(0); }
            else cell.append(c);
        }
        result.add(cell.toString());
        return result;
    }

    private OffsetDateTime actual(LocalDate day, int seconds, ZoneId agency, Stop stop) {
        if (seconds < 0) return null;
        LocalDateTime local = day.atStartOfDay().plusSeconds(seconds);
        ZoneId destination = zoneCache.computeIfAbsent(stop.id(), id -> {
            if (Double.isFinite(stop.lat()) && Double.isFinite(stop.lon())) {
                return timeZones.forLocation(stop.lat(), stop.lon()).orElse(agency);
            }
            try { return stop.timezone().isBlank() ? agency : ZoneId.of(stop.timezone()); }
            catch (DateTimeException ignored) { return agency; }
        });
        return local.atZone(agency).withZoneSameInstant(destination).toOffsetDateTime();
    }

    private static boolean validLeg(OffsetDateTime start, OffsetDateTime end) {
        if (start == null || end == null) return false;
        long minutes = Duration.between(start.toInstant(), end.toInstant()).toMinutes();
        return minutes >= 30 && minutes <= 18 * 60;
    }

    private static boolean between(LocalDate day, LocalDate from, LocalDate to) {
        return !day.isBefore(from) && !day.isAfter(to);
    }

    private static boolean within(LocalTime value, LocalTime from, LocalTime to) {
        return !value.isBefore(from) && !value.isAfter(to);
    }

    private static int seconds(String value) {
        try {
            String[] parts = value.split(":");
            if (parts.length != 3) return -1;
            int h = Integer.parseInt(parts[0]), m = Integer.parseInt(parts[1]), s = Integer.parseInt(parts[2]);
            return h < 0 || h > 72 || m > 59 || m < 0 || s < 0 || s > 59 ? -1 : h * 3600 + m * 60 + s;
        } catch (RuntimeException ignored) { return -1; }
    }

    private static int integer(String text, int fallback) {
        try { return Integer.parseInt(text); } catch (RuntimeException ignored) { return fallback; }
    }
    private static double number(String text) {
        try { return Double.parseDouble(text); } catch (RuntimeException ignored) { return Double.NaN; }
    }
    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('ł', 'l').replace('Ł', 'L').toLowerCase(Locale.ROOT).trim();
    }
    private static String cityName(String stop) {
        // Nazwy FlixBus: "Wrocław (Dworzec Autobusowy)", "Berlin, Zentraler Omnibusbahnhof".
        String name = stop.split("\\s*\\(|,| - ", 2)[0].trim();
        return name.isBlank() ? stop : name;
    }

    private record CsvRow(Map<String, Integer> columns, List<String> cells) {
        String get(String column) {
            Integer index = columns.get(column);
            return index == null || index >= cells.size() ? "" : cells.get(index);
        }
    }
    private record Stop(String id, String city, double lat, double lon, String timezone) {}
    private record Route(ZoneId zone) {}
    private record Trip(String id, String serviceId, ZoneId zone) {}
    private record StopTime(Stop stop, int arrival, int departure, int sequence, boolean pickup, boolean dropoff) {}
    private record Schedule(String tripId, String serviceId, ZoneId zone, List<StopTime> times) {}
    private record CalendarRule(LocalDate start, LocalDate end, boolean[] week) {
        boolean active(LocalDate date) {
            return !date.isBefore(start) && !date.isAfter(end) && week[date.getDayOfWeek().getValue() - 1];
        }
    }
    private record Gtfs(List<Schedule> schedules, Map<String, CalendarRule> rules,
                        Map<String, Map<LocalDate, Boolean>> exceptions) {
        boolean active(String serviceId, LocalDate day) {
            Boolean override = exceptions.getOrDefault(serviceId, Map.of()).get(day);
            if (override != null) return override;
            CalendarRule rule = rules.get(serviceId);
            return rule != null && rule.active(day);
        }
    }
    private record Leg(String id, String city, OffsetDateTime departure, OffsetDateTime arrival) {}
    private record Loaded(Path path, long stamp, Gtfs gtfs) {}
}
