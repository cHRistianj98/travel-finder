package com.travelfinder.trip.bus;

import java.math.BigDecimal;
import java.time.Instant;

/** Wyłącznie potwierdzona cena sumy przejazdów w obie strony dla konkretnej pary kursów. */
public record FlixBusFareQuote(BigDecimal roundTripTotal, String currency, Instant checkedAt) {}
