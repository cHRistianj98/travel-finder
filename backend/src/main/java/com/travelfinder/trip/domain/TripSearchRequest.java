package com.travelfinder.trip.domain;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record TripSearchRequest(
        @NotBlank(message = "Podaj miasto początkowe") String origin,
        @NotNull(message = "Podaj datę wyjazdu") LocalDate outboundDate,
        @NotNull(message = "Podaj datę powrotu") LocalDate returnDate,
        @NotNull(message = "Podaj najwcześniejszą godzinę wyjazdu") LocalTime earliestOutboundDeparture,
        @NotNull(message = "Podaj najpóźniejszą godzinę wyjazdu") LocalTime latestOutboundDeparture,
        @NotNull(message = "Podaj najwcześniejszą godzinę powrotu") LocalTime earliestReturnArrival,
        @NotNull(message = "Podaj najpóźniejszą godzinę powrotu") LocalTime latestReturnArrival,
        @NotNull @DecimalMin(value = "0.01", message = "Budżet musi być większy od zera")
        BigDecimal maxTransportBudget,
        @NotBlank String currency,
        @NotEmpty(message = "Wybierz przynajmniej jeden środek transportu") Set<TransportType> transportTypes,
        boolean directOnly
) {
}
