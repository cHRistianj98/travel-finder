package com.travelfinder.trip.bus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;

/** Jedno pobranie oficjalnego europejskiego GTFS FlixBus, z kopią na dysku między restartami. */
@Component
public class HttpGtfsArchiveGateway implements GtfsArchiveGateway {
    private static final Logger log = LoggerFactory.getLogger(HttpGtfsArchiveGateway.class);
    private static final long MAX_BYTES = 100L * 1024L * 1024L;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private final URI url;
    private final Path path;
    private final int cacheHours;

    public HttpGtfsArchiveGateway(
            @Value("${bus.gtfs-url:https://gtfs.gis.flix.tech/gtfs_generic_eu.zip}") String url,
            @Value("${bus.cache-path:.cache/flixbus_europe_gtfs.zip}") String path,
            @Value("${bus.cache-hours:24}") int cacheHours) {
        this.url = URI.create(url);
        if (!"https".equalsIgnoreCase(this.url.getScheme())) {
            throw new IllegalArgumentException("Źródło GTFS autobusów musi korzystać z HTTPS.");
        }
        this.path = Path.of(path).toAbsolutePath();
        this.cacheHours = Math.max(1, cacheHours);
    }

    @Override
    public synchronized Path archive() throws IOException, InterruptedException {
        if (Files.isRegularFile(path) && Files.size(path) > 0
                && Files.getLastModifiedTime(path).toInstant().plus(Duration.ofHours(cacheHours)).isAfter(Instant.now())) {
            log.info("FlixBus GTFS CACHE HIT: {}", path);
            return path;
        }
        log.info("FlixBus GTFS CACHE MISS: pobieram zbiorczy rozkład z {}", url.getHost());
        Files.createDirectories(path.getParent());
        Path temp = Files.createTempFile(path.getParent(), "flixbus-download-", ".zip");
        try {
            HttpRequest request = HttpRequest.newBuilder(url).GET()
                    .timeout(Duration.ofMinutes(3)).header("Accept", "application/zip").build();
            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(temp));
            if (response.statusCode() != 200) throw new IOException("GTFS HTTP " + response.statusCode());
            long size = Files.size(temp);
            if (size < 100 || size > MAX_BYTES) throw new IOException("Nieprawidłowy rozmiar GTFS: " + size);
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(temp.toFile())) {
                if (zip.getEntry("stops.txt") == null || zip.getEntry("stop_times.txt") == null
                        || zip.getEntry("trips.txt") == null) {
                    throw new IOException("Źródło nie zwróciło poprawnego archiwum GTFS.");
                }
            }
            try { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
            log.info("FlixBus GTFS: pobrano {} MB rozkładu", size / 1024 / 1024);
        } catch (IOException | InterruptedException error) {
            if (Files.isRegularFile(path) && Files.size(path) > 0) {
                log.warn("FlixBus GTFS: nie udało się odświeżyć rozkładu, używam starszej kopii: {}", error.getMessage());
                return path;
            }
            throw error;
        } finally {
            Files.deleteIfExists(temp);
        }
        return path;
    }
}
