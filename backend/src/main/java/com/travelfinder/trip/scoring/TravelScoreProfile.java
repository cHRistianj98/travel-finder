package com.travelfinder.trip.scoring;

import java.util.Map;

/** Wagi profili. Każdy profil sumuje się dokładnie do 100 punktów. */
public enum TravelScoreProfile {
    BALANCED(22, 20, 12, 10, 10, 8, 8, 6, 4),
    BUDGET(40, 17, 10, 8, 7, 6, 4, 5, 3),
    WEEKEND(12, 29, 20, 8, 12, 8, 5, 4, 2),
    COMFORT(12, 14, 8, 24, 20, 12, 3, 4, 3);

    private final int[] weights;

    TravelScoreProfile(int... weights) {
        this.weights = weights;
        if (java.util.Arrays.stream(weights).sum() != 100) throw new IllegalStateException("Wagi nie sumują się do 100");
    }

    public int weight(Criterion criterion) { return weights[criterion.ordinal()]; }

    public enum Criterion {
        PRICE, STAY, DAYS_OFF, COMFORT, SCHEDULE, DISTANCE, ATTRACTIONS, MATCH, CONFIDENCE
    }
}
