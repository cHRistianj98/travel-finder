package com.travelfinder.trip.car;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Publiczne, bezkluczowe notowania fuel-prices.eu (EU Weekly Oil Bulletin; dla CZ także ČSÚ)
 * + średni kurs NBP. W razie awarii jawnie oznaczony scenariusz cenowy, a NIE udawane ceny live.
 */
@Service
public class CarFuelPriceService {
    public record Quote(BigDecimal plnPerLitre, String source, String date, boolean verified) { }
    private record Cached(Quote quote, Instant expiresAt) { }
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final Map<String,Cached> cache = new ConcurrentHashMap<>();
    // Awaria NBP / notowań nie może oznaczać 3 sekund oczekiwania za każdym razem
    // dla 12–15 krajów widocznych w rozszerzonym katalogu atrakcji.
    private volatile Instant remoteRetryAfter = Instant.EPOCH;
    @Autowired
    public CarFuelPriceService(ObjectMapper mapper) {
        this(mapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
    }
    CarFuelPriceService(ObjectMapper mapper, HttpClient client) { this.mapper = mapper; this.client = client; }

    public synchronized Quote get(String country, CarPreferences.Fuel fuel) {
        String key = country + ":" + fuel;
        Cached cached = cache.get(key);
        if (cached != null && Instant.now().isBefore(cached.expiresAt())) return cached.quote();
        Quote quote = load(country, fuel);
        // Notowania tygodniowe/średnie; jedna próba maksymalnie na 12h, również po błędzie.
        cache.put(key, new Cached(quote, Instant.now().plus(Duration.ofHours(12))));
        return quote;
    }
    private Quote load(String country, CarPreferences.Fuel fuel) {
        if (fuel == CarPreferences.Fuel.LPG && !"CZ".equals(country)) return fallback(country, fuel);
        if (Instant.now().isBefore(remoteRetryAfter)) return fallback(country, fuel);
        try {
            BigDecimal euroPln = euroPln();
            if ("CZ".equals(country)) {
                JsonNode summary = fetch("https://www.fuel-prices.eu/live/api.php?action=summary");
                JsonNode countries = summary.path("data").path("countries");
                for (JsonNode c : countries) {
                    if (!"CZ".equals(c.path("country_code").asText())) continue;
                    String type = switch(fuel) { case PETROL -> "sp95"; case DIESEL -> "diesel"; case LPG -> "gpl"; };
                    double avg = c.path("fuels").path(type).path("avg").asDouble(-1);
                    String date = c.path("price_date").asText("");
                    if (avg > 0 && validDate(date)) {
                        return new Quote(euroPln.multiply(BigDecimal.valueOf(avg)).setScale(2,RoundingMode.HALF_UP),
                                "ČSÚ / fuel-prices.eu (tygodniowa średnia, EUR→PLN NBP)", date, true);
                    }
                }
            }
            if (fuel != CarPreferences.Fuel.LPG) {
                JsonNode history = fetch("https://www.fuel-prices.eu/lang/api/country-history.php?cc=" + country);
                JsonNode labels = history.path("labels");
                JsonNode values = history.path(fuel == CarPreferences.Fuel.PETROL ? "e95" : "diesel");
                if (labels.isArray() && values.isArray()) {
                    for (int i = Math.min(labels.size(), values.size()) - 1; i >= 0; i--) {
                        double price = values.get(i).asDouble(-1);
                        String date = labels.get(i).asText("");
                        if (price > 0 && validDate(date)) {
                            return new Quote(euroPln.multiply(BigDecimal.valueOf(price)).setScale(2,RoundingMode.HALF_UP),
                                    "Komisja Europejska WOB / fuel-prices.eu (tygodniowa średnia, EUR→PLN NBP)", date, true);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            remoteRetryAfter = Instant.now().plus(Duration.ofMinutes(10));
        }
        return fallback(country, fuel);
    }
    private boolean validDate(String date) {
        try { LocalDate day = LocalDate.parse(date); return !day.isAfter(LocalDate.now().plusDays(1)) && !day.isBefore(LocalDate.now().minusDays(31)); }
        catch (Exception e) { return false; }
    }
    private BigDecimal euroPln() throws Exception {
        String key="fx:EUR"; Cached cached=cache.get(key);
        if (cached!=null && Instant.now().isBefore(cached.expiresAt())) return cached.quote().plnPerLitre();
        JsonNode json=fetch("https://api.nbp.pl/api/exchangerates/rates/a/eur/?format=json");
        double rate=json.path("rates").path(0).path("mid").asDouble(-1);
        if (rate<2 || rate>8) throw new IllegalStateException("Nieprawidłowy kurs walutowy");
        BigDecimal value=BigDecimal.valueOf(rate);
        cache.put(key,new Cached(new Quote(value,"NBP",json.path("rates").path(0).path("effectiveDate").asText(),true),Instant.now().plus(Duration.ofHours(12))));
        return value;
    }
    private JsonNode fetch(String url) throws Exception {
        HttpRequest request=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(3))
                .header("Accept","application/json")
                .header("User-Agent","TravelFinder/1.0 (personal travel comparison app; cached averages)")
                .GET().build();
        HttpResponse<String> response=client.send(request,HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()!=200 || response.body().length()>1_000_000) throw new IllegalStateException("Źródło paliwa niedostępne");
        return mapper.readTree(response.body());
    }
    public static Quote fallback(String country, CarPreferences.Fuel fuel) {
        // Kwoty PLANISTYCZNE, nie przypisywać im źródła live ani daty notowania.
        double value = switch (country) {
            case "DE" -> switch (fuel) { case PETROL -> 8.20; case DIESEL -> 7.70; case LPG -> 4.10; };
            case "CZ" -> switch (fuel) { case PETROL -> 7.90; case DIESEL -> 7.40; case LPG -> 3.90; };
            case "AT", "SI", "SK", "HU" -> switch (fuel) { case PETROL -> 7.40; case DIESEL -> 7.20; case LPG -> 4.10; };
            case "IT", "FR", "CH", "NL", "BE", "DK" -> switch (fuel) { case PETROL -> 8.70; case DIESEL -> 8.10; case LPG -> 4.50; };
            case "HR" -> switch (fuel) { case PETROL -> 7.40; case DIESEL -> 7.20; case LPG -> 4.10; };
            case "LT" -> switch (fuel) { case PETROL -> 7.00; case DIESEL -> 7.20; case LPG -> 3.90; };
            default -> switch (fuel) { case PETROL -> 6.80; case DIESEL -> 6.90; case LPG -> 3.40; };
        };
        return new Quote(BigDecimal.valueOf(value).setScale(2),"ZAŁOŻENIE PLANISTYCZNE — zmień cenę w formularzu", "brak aktualnego odczytu",false);
    }
}
