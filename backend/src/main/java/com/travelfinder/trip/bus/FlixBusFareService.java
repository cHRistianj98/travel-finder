package com.travelfinder.trip.bus;

import com.travelfinder.trip.domain.TripOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Opcjonalne wzbogacanie GTFS o rzeczywiste ceny partnerskie.
 * Cache izoluje parę kursów po dokładnych odjazdach TAM + POWRÓT; nie wolno
 * zastępować go średnią/minimum z ogólnych stron reklamujących trasy.
 */
@Component
public final class FlixBusFareService {
    private static final Logger log = LoggerFactory.getLogger(FlixBusFareService.class);
    private final FlixBusFareGateway gateway;
    private final Duration ttl;
    private final Clock clock;
    private final ConcurrentHashMap<Key, CacheEntry> cache = new ConcurrentHashMap<>();

    @Autowired
    public FlixBusFareService(FlixBusFareGateway gateway,
                             @Value("${bus.fares.cache-minutes:30}") long cacheMinutes) {
        this(gateway, Duration.ofMinutes(Math.max(1, Math.min(120, cacheMinutes))), Clock.systemUTC());
    }

    public FlixBusFareService(FlixBusFareGateway gateway, Duration ttl, Clock clock) {
        this.gateway = gateway;
        this.ttl = ttl;
        this.clock = clock;
    }

    public TripOption enrich(TripOption trip) {
        Key key = new Key(trip.origin(), trip.destination(), trip.outboundDeparture(),
                trip.returnDeparture(), trip.returnArrival());
        Instant now = clock.instant();
        CacheEntry cached = cache.compute(key, (ignored, current) -> {
            if (current != null && now.isBefore(current.expiresAt())) return current;
            Optional<FlixBusFareQuote> quote;
            try {
                quote = Optional.ofNullable(gateway.quoteFor(trip)).orElseGet(Optional::empty);
            } catch (RuntimeException ex) {
                log.warn("Nie udało się zweryfikować ceny FlixBus dla {} → {}: {}",
                        trip.origin(), trip.destination(), ex.getMessage());
                quote = Optional.empty();
            }
            Optional<FlixBusFareQuote> accepted = quote.filter(q -> valid(q, now));
            // Cena nie może być pokazywana >30 min od czasu, kiedy partner ją faktycznie sprawdził.
            Instant expiry = accepted.map(q -> q.checkedAt().plus(Duration.ofMinutes(30)))
                    .filter(before -> before.isBefore(now.plus(ttl)))
                    .orElse(now.plus(ttl));
            return new CacheEntry(accepted, expiry);
        });
        // Nie przechowujemy z czasem dowolnej liczby starych par dat.
        if (cache.size() > 10_000) cache.entrySet().removeIf(e -> !now.isBefore(e.getValue().expiresAt()));
        if (cached.quote().isEmpty()) return trip;
        BigDecimal total = cached.quote().get().roundTripTotal();
        return new TripOption(trip.id(), trip.origin(), trip.destination(), trip.outboundDeparture(),
                trip.outboundArrival(), trip.returnDeparture(), trip.returnArrival(),
                trip.transportType(), total, "PLN", trip.totalTravelDurationMinutes(),
                trip.timeAtDestinationMinutes(), trip.outboundTransfers(), trip.returnTransfers(),
                "FLIXBUS_PARTNER_FARE", trip.bookingUrl());
    }

    private static boolean valid(FlixBusFareQuote q, Instant now) {
        return q.roundTripTotal() != null && q.roundTripTotal().signum() > 0
                && "PLN".equalsIgnoreCase(q.currency())
                && q.checkedAt() != null && !q.checkedAt().isAfter(now.plusSeconds(60))
                && !q.checkedAt().isBefore(now.minus(Duration.ofMinutes(30)));
    }

    private record Key(String origin, String destination, OffsetDateTime outbound,
                       OffsetDateTime inbound, OffsetDateTime returnArrival) {}
    private record CacheEntry(Optional<FlixBusFareQuote> quote, Instant expiresAt) {}
}
