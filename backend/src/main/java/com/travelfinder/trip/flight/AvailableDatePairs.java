package com.travelfinder.trip.flight;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Generuje pary dni wylotu i rozpoczecia powrotu wewnatrz wolnego przedzialu. */
public final class AvailableDatePairs {
    public static final int MAX_WINDOW_DAYS = 4;

    private AvailableDatePairs() { }

    public static List<DatePair> inWindow(LocalDate firstFreeDay, LocalDate lastFreeDay) {
        if (firstFreeDay == null || lastFreeDay == null) {
            throw new IllegalArgumentException("Wybierz początek i koniec wolnego terminu.");
        }
        long numberOfDays = ChronoUnit.DAYS.between(firstFreeDay, lastFreeDay) + 1;
        if (numberOfDays < 2) {
            throw new IllegalArgumentException("Wybierz przynajmniej dwa dni dostępności.");
        }
        if (numberOfDays > MAX_WINDOW_DAYS) {
            throw new IllegalArgumentException("W tej wersji można przeszukać maksymalnie 4 kolejne dni. Wybierz krótszy termin.");
        }
        List<DatePair> pairs = new ArrayList<>();
        for (LocalDate departure = firstFreeDay; departure.isBefore(lastFreeDay); departure = departure.plusDays(1)) {
            for (LocalDate returning = departure.plusDays(1); !returning.isAfter(lastFreeDay); returning = returning.plusDays(1)) {
                pairs.add(new DatePair(departure, returning));
            }
        }
        // Dłuższy pobyt ma pierwszeństwo przy ograniczonym budżecie zewnętrznego API.
        pairs.sort(Comparator.<DatePair>comparingLong(pair -> ChronoUnit.DAYS.between(pair.outbound(), pair.returnDeparture()))
                .reversed().thenComparing(DatePair::outbound));
        return List.copyOf(pairs);
    }

    public record DatePair(LocalDate outbound, LocalDate returnDeparture) { }
}
