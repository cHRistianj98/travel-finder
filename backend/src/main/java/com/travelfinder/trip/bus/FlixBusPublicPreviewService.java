package com.travelfinder.trip.bus;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Darmowy podgląd PUBLICZNIE REKLAMOWANYCH cen minimalnych na stronach relacji,
 * nigdy ceny wybranego kursu. GTFS i ceny partnerskie pozostają bez zmian.
 * Zewnętrzne żądania następują wyłącznie po zapytaniu o podgląd karty, z cache 24h.
 * Nie omijamy zabezpieczeń serwisu, nie korzystamy z kluczy ani sesji klienta.
 */
@Component
public class FlixBusPublicPreviewService {
    private static final Logger log = LoggerFactory.getLogger(FlixBusPublicPreviewService.class);
    private static final Pattern FROM_PRICE = Pattern.compile(
            "(?iu)\\bod\\s*([0-9][0-9\\s\\u00a0]*[,.][0-9]{2})\\s*zł");
    private static final DateTimeFormatter SHOP_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Duration FARE_TTL = Duration.ofHours(24);
    private static final Duration NEGATIVE_TTL = Duration.ofHours(1);
    private static final Duration CITY_TTL = Duration.ofDays(14);
    private final HttpClient http;
    private final ObjectMapper json;
    private final Clock clock;
    private final boolean enabled;
    private final Semaphore concurrency = new Semaphore(3);
    private final ConcurrentHashMap<String, TimedValue<BigDecimal>> fares = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TimedValue<String>> cities = new ConcurrentHashMap<>();

    @Autowired
    public FlixBusPublicPreviewService(ObjectMapper json,
            @Value("${bus.public-preview.enabled:true}") boolean enabled) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build(), json, Clock.systemUTC(), enabled);
    }

    // W testach można zastąpić klienta HTTP serwerem lub przetestować sam parser.
    FlixBusPublicPreviewService(HttpClient http, ObjectMapper json, Clock clock, boolean enabled) {
        this.http = http;
        this.json = json;
        this.clock = clock;
        this.enabled = enabled;
    }

    public Preview preview(String from, String to, LocalDate departureDate, LocalDate returnDate) {
        if (from == null || to == null || from.length() > 120 || to.length() > 120
                || departureDate == null || returnDate == null
                || returnDate.isBefore(departureDate)) {
            throw new IllegalArgumentException("Niepoprawna relacja lub daty FlixBus.");
        }
        String fromCity = normalizedCity(from);
        String toCity = normalizedCity(to);
        String outboundPage = FlixBusBookingLinks.route(from, to);
        String inboundPage = FlixBusBookingLinks.route(to, from);
        if (!enabled) return new Preview(null, null, null, outboundPage, inboundPage,
                false, "DISABLED", null);

        BigDecimal outbound = minimumFor(outboundPage);
        BigDecimal inbound = minimumFor(inboundPage);
        BigDecimal total = outbound != null && inbound != null ? outbound.add(inbound) : null;

        String outId = cityId(fromCity);
        String inId = cityId(toCity);
        boolean prefilled = outId != null && inId != null;
        String outboundLink = prefilled ? shop(outId, inId, departureDate) : outboundPage;
        String inboundLink = prefilled ? shop(inId, outId, returnDate) : inboundPage;
        return new Preview(outbound, inbound, total, outboundLink, inboundLink,
                prefilled, total != null ? "PUBLIC_ROUTE_MINIMUM" : "UNAVAILABLE", clock.instant());
    }

    private String shop(String originId, String destinationId, LocalDate date) {
        return "https://shop.flixbus.com/search?departureCity=" + originId
                + "&arrivalCity=" + destinationId
                + "&rideDate=" + SHOP_DATE.format(date)
                + "&adult=1&currency=PLN&_locale=pl";
    }

    private BigDecimal minimumFor(String routeUrl) {
        if (!routeUrl.startsWith("https://www.flixbus.pl/polaczenia-autobusowe/")) return null;
        var now = clock.instant();
        var value = fares.compute(routeUrl, (key, old) -> {
            if (old != null && old.expiresAt().isAfter(now)) return old;
            BigDecimal amount = null;
            try {
                String html = fetch(key);
                amount = advertisedMinimum(html);
            } catch (Exception ex) {
                log.debug("Brak publicznej ceny minimalnej dla {}: {}", key, ex.getMessage());
            }
            return new TimedValue<>(amount, now.plus(amount == null ? NEGATIVE_TTL : FARE_TTL));
        });
        trimExpired(fares);
        return value.value();
    }

    private String cityId(String city) {
        if (city.isBlank()) return null;
        var now = clock.instant();
        var value = cities.compute(city, (key, old) -> {
            if (old != null && old.expiresAt().isAfter(now)) return old;
            String id = null;
            try {
                String url = "https://global.api.flixbus.com/search/autocomplete/cities?q="
                        + URLEncoder.encode(key, StandardCharsets.UTF_8)
                        + "&lang=en&country=pl&flixbus_cities_only=true&stations=false";
                id = resolveExactCityId(json.readTree(fetch(url)), key);
            } catch (Exception ex) {
                log.debug("Nie można rozpoznać ID miasta FlixBus {}: {}", key, ex.getMessage());
            }
            return new TimedValue<>(id, now.plus(id == null ? NEGATIVE_TTL : CITY_TTL));
        });
        trimExpired(cities);
        return value.value();
    }

    private String fetch(String url) throws Exception {
        if (!concurrency.tryAcquire(5, TimeUnit.SECONDS)) throw new IllegalStateException("Limit równoległych pobrań");
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(7))
                    .header("Accept", "text/html,application/json")
                    .header("Accept-Language", "pl-PL,pl;q=0.9,en;q=0.5")
                    .header("User-Agent", "Mozilla/5.0 (compatible; TravelFinder/1.0)")
                    .GET().build();
            HttpResponse<String> result = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (result.statusCode() != 200) throw new IllegalStateException("HTTP " + result.statusCode());
            if (result.body().length() > 1_500_000) throw new IllegalStateException("Zbyt duża odpowiedź");
            return result.body();
        } finally {
            concurrency.release();
        }
    }

    /** Tekst i proste tagi HTML; nie pobieramy ceny z cenników innych niż PLN. */
    public static BigDecimal advertisedMinimum(String html) {
        if (html == null || html.isBlank()) return null;
        String text = html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ")
                .replaceAll("(?s)<[^>]+>", " ")
                .replace("&nbsp;", " ").replace("&#160;", " ").replace("&zwnj;", " ")
                .replace("\\u00a0", " ");
        Matcher m = FROM_PRICE.matcher(text);
        if (!m.find()) return null;
        try {
            BigDecimal price = new BigDecimal(m.group(1).replaceAll("[\\s\\u00a0]", "").replace(',', '.'));
            return price.signum() > 0 && price.compareTo(new BigDecimal("5000")) <= 0 ? price : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static String resolveExactCityId(JsonNode root, String query) {
        if (root == null || !root.isArray()) return null;
        String wanted = normalizedCity(query);
        for (JsonNode city : root) {
            String found = normalizedCity(city.path("name").asText(""));
            String id = city.path("id").asText("");
            if (found.equals(wanted) && id.matches("[0-9a-fA-F-]{32,36}")) return id;
        }
        return null;
    }

    private static String normalizedCity(String name) {
        String normalized = Normalizer.normalize(name.toLowerCase(Locale.ROOT).replace('ł', 'l'),
                Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]+", " ").trim();
        // Nazwy GTFS zawierają dokładny przystanek, nie miasto.
        if (normalized.startsWith("szklarska poreba")) return "szklarska poreba";
        if (normalized.startsWith("gorzow wielkopolski")) return "gorzow wielkopolski";
        if (normalized.startsWith("zielona gora")) return "zielona gora";
        if (normalized.startsWith("jelenia gora")) return "jelenia gora";
        if (normalized.startsWith("berlin airport") || normalized.startsWith("berlin brandenburg airport")) return "berlin airport";
        if (normalized.startsWith("prague") || normalized.startsWith("praga")) return "prague";
        if (normalized.startsWith("poznan")) return "poznan";
        if (normalized.startsWith("wroclaw")) return "wroclaw";
        if (normalized.startsWith("dresden") || normalized.startsWith("drezno")) return "dresden";
        if (normalized.startsWith("krakow")) return "krakow";
        if (normalized.startsWith("warsaw") || normalized.startsWith("warszawa")) return "warsaw";
        if (normalized.startsWith("berlin")) return "berlin";
        if (normalized.startsWith("katowice")) return "katowice";
        if (normalized.startsWith("chemnitz")) return "chemnitz";
        if (normalized.startsWith("szczecin")) return "szczecin";
        if (normalized.startsWith("lubin")) return "lubin";
        if (normalized.startsWith("swiebodzin")) return "swiebodzin";
        if (normalized.startsWith("gliwice")) return "gliwice";
        if (normalized.startsWith("miedzyzdroje")) return "miedzyzdroje";
        if (normalized.startsWith("lodz")) return "lodz";
        if (normalized.startsWith("brno")) return "brno";
        if (normalized.startsWith("vienna") || normalized.startsWith("wieden")) return "vienna";
        return normalized;
    }

    private <T> void trimExpired(ConcurrentHashMap<String, TimedValue<T>> cache) {
        if (cache.size() > 1000) {
            Instant now = clock.instant();
            cache.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        }
    }

    public record Preview(BigDecimal outboundFromPln, BigDecimal returnFromPln,
            BigDecimal roundTripFromPln, String outboundSearchUrl, String returnSearchUrl,
            boolean searchPrefilled, String priceKind, Instant checkedAt) { }
    private record TimedValue<T>(T value, Instant expiresAt) { }
}
