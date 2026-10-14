package com.travelfinder.trip.car;

import java.math.BigDecimal;

/** Jawnie szacunkowe parametry podróży samochodem, nie oferta na konkretny kurs. */
public record CarTripDetails(String landmark, String country, int nearbyAttractions,
        int oneWayKm, int roundTripKm, long drivingMinutesEachWay,
        int passengers, double litresPer100Km, String fuelType, BigDecimal fuelPricePlnPerLitre,
        String fuelPriceSource, String fuelPriceDate, BigDecimal fuelCostPln,
        BigDecimal vignettePln, BigDecimal parkingPln, BigDecimal extraTollsPln,
        BigDecimal totalCarCostPln, boolean roadEstimate, String tollNotice) { }
