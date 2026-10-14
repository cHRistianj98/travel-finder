package com.travelfinder.trip.scoring;

import com.travelfinder.trip.domain.TripOption;
import com.travelfinder.trip.domain.TripSearchRequest;
import com.travelfinder.trip.scoring.TravelScoreProfile.Criterion;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/**
 * Travel Score 0–100: suma jawnych punktów, BEZ skalowania brakujących kryteriów.
 * Nieznana cena / brak danych ma points=null i 0 punktów do sumy, nigdy domyślne 50%.
 * Ocena prezentuje dataCoverage, bo częściowych wyników nie należy traktować jak pełnych.
 */
@Service
public class TravelScoringService {
    public record CriterionResult(String key, String label, int maxPoints, Integer points, String explanation) { }
    public record Score(int value, int availablePoints, int unknownPoints, boolean partial,
            String profile, List<CriterionResult> criteria, List<String> strengths,
            List<String> cautions, Integer distanceKm, int weekendOrHolidayDays, int usefulDaylightHours,
            List<String> attractionExamples) { }
    public record ScoredTrip(TripOption trip, Score score) { }
    public record ScoreRequest(TripSearchRequest request, List<TripOption> trips, TravelScoreProfile profile) { }

    public List<ScoredTrip> scoreAll(ScoreRequest payload) {
        if (payload == null || payload.request() == null || payload.trips() == null) {
            throw new IllegalArgumentException("Brakuje kryteriów lub listy podróży do ocenienia.");
        }
        if (payload.trips().size() > 500) throw new IllegalArgumentException("Można ocenić maksymalnie 500 podróży naraz.");
        TravelScoreProfile profile = payload.profile() == null ? TravelScoreProfile.BALANCED : payload.profile();
        return payload.trips().stream().filter(Objects::nonNull)
                .map(trip -> new ScoredTrip(trip, score(payload.request(), trip, profile)))
                .sorted(Comparator.comparingInt((ScoredTrip trip) -> trip.score().value()).reversed()
                        .thenComparing(Comparator.comparingLong((ScoredTrip trip) -> trip.trip().timeAtDestinationMinutes()).reversed())
                        .thenComparing(trip -> trip.trip().price(), Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(trip -> trip.trip().destination()))
                .toList();
    }

    public Score score(TripSearchRequest request, TripOption trip, TravelScoreProfile profile) {
        if (request == null || trip == null) throw new IllegalArgumentException("Brakuje danych podróży do oceny.");
        profile = profile == null ? TravelScoreProfile.BALANCED : profile;
        List<CriterionResult> criteria = new ArrayList<>(9);
        List<String> strengths = new ArrayList<>(), cautions = new ArrayList<>();
        int stayDaylight = daylightHours(trip.outboundArrival(), trip.returnDeparture());
        int daysOff = 0, tripDays = 0;
        long availableDaytimeMinutes = 0, freeDaytimeMinutes = 0;
        for (LocalDate day = trip.outboundArrival().toLocalDate(); !day.isAfter(trip.returnDeparture().toLocalDate()) && tripDays < 366; day = day.plusDays(1)) {
            tripDays++;
            long minutes = overlapWithTouringDay(trip.outboundArrival(), trip.returnDeparture(), day);
            availableDaytimeMinutes += minutes;
            if (isPolishDayOff(day)) {
                daysOff++;
                freeDaytimeMinutes += minutes;
            }
        }
        TravelDestinationCatalog.Place place = TravelDestinationCatalog.find(trip.destination());
        Integer distanceKm = TravelDestinationCatalog.distanceKm(trip.origin(), trip.destination());

        BigDecimal budget = request.maxTransportBudget();
        if (trip.price() == null || budget == null || budget.signum() <= 0 || !"PLN".equalsIgnoreCase(trip.currency())) {
            add(criteria, profile, Criterion.PRICE, null,
                    "Brak wiarygodnej ceny za konkretną podróż. Minimum «od» FlixBusa nie jest ceną tych kursów.");
            cautions.add("Cena wybranych kursów jest nieznana");
        } else {
            double ratio = trip.price().doubleValue() / budget.doubleValue();
            double priceScore = clamp(0.24 + 0.76 * (1 - Math.min(1, Math.max(0, ratio))), 0, 1);
            add(criteria, profile, Criterion.PRICE, priceScore,
                    "Cena z oferty: " + money(trip.price()) + " / limit " + money(budget) + ". Dla lotów może być historyczna.");
            if (ratio <= 0.45) strengths.add("Tani transport względem budżetu");
            if (ratio >= 0.90) cautions.add("Cena blisko limitu budżetu");
        }

        double actualStayHours = Math.max(0, trip.timeAtDestinationMinutes() / 60d);
        // Dodatkowe dni po przekroczeniu ~3 dób nie powinny nieograniczenie dominować rankingu.
        double stayScore = clamp(0.65 * Math.min(stayDaylight / 28d, 1) + 0.35 * Math.min(actualStayHours / 66, 1), 0, 1);
        add(criteria, profile, Criterion.STAY, stayScore,
                formatHours(trip.timeAtDestinationMinutes()) + " na miejscu, w tym około " + stayDaylight + " godz. między 9:00 a 21:00 (szacunek czasu dziennego).");
        if (stayDaylight >= 20) strengths.add("Dużo czasu dziennego na miejscu");
        if (stayDaylight < 8) cautions.add("Niewiele godzin dziennych na miejscu");

        double daysScore = availableDaytimeMinutes > 0
                ? (double) freeDaytimeMinutes / availableDaytimeMinutes
                : (tripDays > 0 ? (double) daysOff / tripDays : 0);
        add(criteria, profile, Criterion.DAYS_OFF, daysScore,
                daysOff + " z " + tripDays + " dni zahacza o weekend lub polskie święto; "
                        + Math.round(daysScore * 100) + "% dostępnych godzin 9:00–21:00 przypada na takie dni. Zakładamy standardowy tydzień pracy.");
        if (tripDays > 0 && daysOff >= 2) strengths.add("Wykorzystuje weekend lub dni wolne");

        int transfers = Math.max(0, trip.outboundTransfers()) + Math.max(0, trip.returnTransfers());
        double changeFactor = switch (Math.min(transfers, 3)) { case 0 -> 1; case 1 -> .67; case 2 -> .40; default -> .18; };
        double transportHours = Math.max(0, trip.totalTravelDurationMinutes() / 60d);
        double timeFactor = clamp(1 - Math.max(0, transportHours - 3) / 20, .15, 1);
        add(criteria, profile, Criterion.COMFORT, .70 * changeFactor + .30 * timeFactor,
                transfers + " przesiadek łącznie; " + formatHours(trip.totalTravelDurationMinutes()) + " w trasie. Komfort to heurystyka, nie ocena siedzeń i bagażu.");
        if (transfers == 0) strengths.add("Podróż bez przesiadek");
        if (transfers >= 2) cautions.add("Przesiadki mogą zwiększyć ryzyko opóźnień");

        // Oceniamy rzeczywistą porę przyjazdu i powrotu; nie samo «odlot rano».
        double arrival = hourOfDay(trip.outboundArrival());
        double returnDeparture = hourOfDay(trip.returnDeparture());
        double arrivalHome = hourOfDay(trip.returnArrival());
        double outboundHome = hourOfDay(trip.outboundDeparture());
        double timing = .40 * comfortHour(arrival, 9, 20) + .25 * comfortHour(returnDeparture, 8, 22)
                + .20 * comfortHour(arrivalHome, 7, 23) + .15 * comfortHour(outboundHome, 6, 23);
        add(criteria, profile, Criterion.SCHEDULE, timing,
                "Przyjazd na miejsce " + trip.outboundArrival().toLocalTime().withSecond(0)
                        + ", wyjazd powrotny " + trip.returnDeparture().toLocalTime().withSecond(0)
                        + ", powrót do domu " + trip.returnArrival().toLocalTime().withSecond(0) + ".");
        if (arrival < 6 || arrival >= 23) cautions.add("Przyjazd nocą utrudnia logistykę");
        if (arrivalHome >= 23 || arrivalHome < 5) cautions.add("Późny powrót do domu");

        if (distanceKm == null) {
            add(criteria, profile, Criterion.DISTANCE, null, "Brak wiarygodnego położenia miasta w lokalnym katalogu.");
        } else {
            // Preferencja relatywnie bliskich wypadów, nie twierdzenie o dojeździe z lotniska.
            double closeness = clamp(1 - Math.max(0, distanceKm - 100) / 3100d, .12, 1);
            add(criteria, profile, Criterion.DISTANCE, closeness,
                    "Około " + distanceKm + " km w linii prostej między centrami miast. NIE uwzględnia transferu z lotniska/dworca.");
        }

        List<String> attractions = place == null ? List.of() : place.highlights();
        if (attractions.isEmpty()) {
            add(criteria, profile, Criterion.ATTRACTIONS, null, "Brak tego miasta w redakcyjnym katalogu atrakcji.");
        } else {
            double value = Math.min(1, attractions.size() / 4d);
            add(criteria, profile, Criterion.ATTRACTIONS, value,
                    attractions.size() + " przykłady atrakcji w naszym katalogu, nie całkowita liczba atrakcji miasta: "
                            + String.join(", ", attractions.stream().limit(4).toList()) + ".");
            if (attractions.size() >= 4) strengths.add("Bogaty wybór atrakcji w katalogu");
        }

        boolean transportMatches = request.transportTypes() != null && request.transportTypes().contains(trip.transportType());
        boolean directMatches = !request.directOnly() || transfers == 0;
        boolean budgetMatches = trip.price() == null || (budget != null && trip.price().compareTo(budget) <= 0);
        boolean departureInWindow = inRange(trip.outboundDeparture().toLocalTime(), request.earliestOutboundDeparture(), request.latestOutboundDeparture());
        boolean returnInWindow = inRange(trip.returnDeparture().toLocalTime(), request.earliestReturnDeparture(), request.latestReturnDeparture());
        double match = (transportMatches ? .30 : 0) + (directMatches ? .25 : 0) + (budgetMatches ? .15 : 0)
                + (departureInWindow ? .15 : 0) + (returnInWindow ? .15 : 0);
        add(criteria, profile, Criterion.MATCH, match,
                "Transport, budżet (gdy cena znana), bezpośredniość i oba przedziały godzin. Wymagania obowiązkowe nadal filtruje wyszukiwarka.");

        double confidence = trip.price() == null ? .30 : .52;
        String confidenceDetail = "Planowe GTFS/PLK lub historyczne cache nie potwierdzają dostępności miejsca na wybrany kurs.";
        if (trip.price() == null) confidenceDetail += " Brak ceny dokładnej podróży.";
        add(criteria, profile, Criterion.CONFIDENCE, confidence, confidenceDetail);

        int value = 0, available = 0;
        for (CriterionResult item : criteria) {
            if (item.points() != null) { value += item.points(); available += item.maxPoints(); }
        }
        return new Score(value, available, 100 - available, available < 100, profile.name(),
                List.copyOf(criteria), List.copyOf(strengths.stream().limit(4).toList()),
                List.copyOf(cautions.stream().limit(4).toList()), distanceKm, daysOff,
                stayDaylight, attractions.stream().limit(4).toList());
    }

    private static void add(List<CriterionResult> target, TravelScoreProfile profile,
            Criterion criterion, Double factor, String detail) {
        String[] names = {"Cena", "Czas na miejscu", "Dni wolne", "Komfort", "Godziny transportu", "Odległość", "Atrakcje", "Dopasowanie", "Pewność danych"};
        int max = profile.weight(criterion);
        target.add(new CriterionResult(criterion.name(), names[criterion.ordinal()], max,
                factor == null ? null : (int) Math.round(max * clamp(factor, 0, 1)), detail));
    }

    private static String money(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString() + " PLN";
    }
    private static String formatHours(long minutes) {
        return String.format(Locale.ROOT, "%.1f godz.", minutes / 60d).replace('.', ',');
    }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    private static double hourOfDay(OffsetDateTime timestamp) { return timestamp.getHour() + timestamp.getMinute() / 60d; }
    private static boolean inRange(LocalTime hour, LocalTime min, LocalTime max) {
        return min != null && max != null && !hour.isBefore(min) && !hour.isAfter(max);
    }
    private static double comfortHour(double hour, double idealFrom, double idealUntil) {
        if (hour >= idealFrom && hour <= idealUntil) return 1;
        double distance = Math.min(Math.abs(hour - idealFrom), Math.abs(hour - idealUntil));
        return clamp(1 - distance / 7, .12, 1);
    }

    /** Godziny części wspólnej pobytu z umownym turystycznym 09:00–21:00. */
    static int daylightHours(OffsetDateTime arrival, OffsetDateTime departure) {
        if (arrival == null || departure == null || !arrival.toInstant().isBefore(departure.toInstant())) return 0;
        long minutes = 0;
        LocalDate end = departure.withOffsetSameInstant(arrival.getOffset()).toLocalDate();
        for (LocalDate day = arrival.toLocalDate(); !day.isAfter(end) && !day.isAfter(arrival.toLocalDate().plusDays(365)); day = day.plusDays(1)) {
            minutes += overlapWithTouringDay(arrival, departure, day);
        }
        return (int) Math.round(minutes / 60d);
    }

    private static long overlapWithTouringDay(OffsetDateTime arrival, OffsetDateTime departure, LocalDate day) {
        if (!arrival.toInstant().isBefore(departure.toInstant())) return 0;
        ZoneOffset offset = arrival.getOffset();
        OffsetDateTime endLocal = departure.withOffsetSameInstant(offset);
        OffsetDateTime from = day.atTime(9, 0).atOffset(offset), to = day.atTime(21, 0).atOffset(offset);
        OffsetDateTime overlapFrom = from.isAfter(arrival) ? from : arrival;
        OffsetDateTime overlapTo = to.isBefore(endLocal) ? to : endLocal;
        return overlapTo.isAfter(overlapFrom) ? Duration.between(overlapFrom, overlapTo).toMinutes() : 0;
    }

    /** Weekendy i święta ustawowe w Polsce; nie rozstrzyga indywidualnego grafiku pracy. */
    static boolean isPolishDayOff(LocalDate day) {
        DayOfWeek dow = day.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) return true;
        int month = day.getMonthValue(), d = day.getDayOfMonth();
        if ((month == 1 && (d == 1 || d == 6)) || (month == 5 && (d == 1 || d == 3))
                || (month == 8 && d == 15) || (month == 11 && (d == 1 || d == 11))
                || (month == 12 && (d == 24 || d == 25 || d == 26))) return true;
        LocalDate easter = easterSunday(day.getYear());
        return day.equals(easter.plusDays(1)) || day.equals(easter.plusDays(49)) || day.equals(easter.plusDays(60));
    }

    /** Meeus/Jones/Butcher, kalendarz gregoriański. */
    private static LocalDate easterSunday(int year) {
        int a = year % 19, b = year / 100, c = year % 100, d = b / 4, e = b % 4;
        int f = (b + 8) / 25, g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4, k = c % 4, l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = (h + l - 7 * m + 114) % 31 + 1;
        return LocalDate.of(year, month, day);
    }
}
