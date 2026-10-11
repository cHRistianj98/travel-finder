package com.travelfinder.trip.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

/**
 * Dni, w których użytkownik faktycznie może podróżować. Przerwy pomiędzy
 * weekendami NIE oznaczają urlopu: każda data od wylotu do przylotu musi być wolna.
 * Brak availableDates w starych requestach oznacza ciągły zakres.
 */
public final class AvailabilityCalendar {
    public static final int MAX_SPAN_DAYS = 62;

    private final Set<LocalDate> freeDays;

    private AvailabilityCalendar(Set<LocalDate> freeDays) {
        this.freeDays = Set.copyOf(freeDays);
    }

    public static AvailabilityCalendar from(TripSearchRequest request) {
        LocalDate from = request.outboundDate();
        LocalDate to = request.returnDate();
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("Wybierz poprawny termin dostępności.");
        }
        long span = ChronoUnit.DAYS.between(from, to) + 1;
        if (span > MAX_SPAN_DAYS) {
            throw new IllegalArgumentException("Możesz zaznaczyć dni w obrębie maksymalnie 62 dni kalendarzowych.");
        }
        Set<LocalDate> dates;
        if (request.availableDates().isEmpty()) {
            dates = new HashSet<>();
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) dates.add(d);
        } else {
            dates = request.availableDates();
            if (dates.stream().anyMatch(day -> day == null || day.isBefore(from) || day.isAfter(to))) {
                throw new IllegalArgumentException("Wybrane wolne dni muszą należeć do podanego przedziału.");
            }
            if (!dates.contains(from) || !dates.contains(to)) {
                throw new IllegalArgumentException("Początek i koniec terminu muszą być zaznaczonymi dniami.");
            }
        }
        if (dates.stream().noneMatch(day -> dates.contains(day.plusDays(1)))) {
            throw new IllegalArgumentException("Zaznacz co najmniej dwa kolejne wolne dni na jeden wyjazd.");
        }
        return new AvailabilityCalendar(dates);
    }

    public boolean allowsTrip(LocalDate departureDay, LocalDate arrivalHomeDay) {
        if (departureDay == null || arrivalHomeDay == null || !arrivalHomeDay.isAfter(departureDay)) {
            return false; // Minimum jedna noc, bez wyjazdów jednodniowych.
        }
        for (LocalDate day = departureDay; !day.isAfter(arrivalHomeDay); day = day.plusDays(1)) {
            if (!freeDays.contains(day)) return false;
        }
        return true;
    }
}
