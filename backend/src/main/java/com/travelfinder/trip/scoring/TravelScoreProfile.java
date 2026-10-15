package com.travelfinder.trip.scoring;

import com.travelfinder.trip.domain.TransportType;
import java.util.Arrays;

/** Wagi profili. Każdy profil sumuje się dokładnie do 100 punktów. */
public enum TravelScoreProfile {
    BALANCED(22, 20, 12, 10, 10, 8, 8, 6, 4),
    BUDGET(40, 17, 10, 8, 7, 6, 4, 5, 3),
    WEEKEND(12, 29, 20, 8, 12, 8, 5, 4, 2),
    COMFORT(12, 14, 8, 24, 20, 12, 3, 4, 3);

    // Samochód oceniamy przede wszystkim jako CEL wyjazdu, nie jako najtańszy środek transportu.
    // Kolejność jak w enum Criterion: PRICE, STAY, DAYS_OFF, COMFORT, SCHEDULE,
    // DISTANCE, ATTRACTIONS, MATCH, CONFIDENCE.
    private static final int[][] CAR_WEIGHTS = {
        {10, 6, 4, 5, 4, 6, 55, 5, 5}, // BALANCED
        {25, 7, 4, 5, 3, 6, 40, 5, 5}, // BUDGET
        {7, 8, 8, 4, 4, 4, 58, 4, 3},  // WEEKEND
        {9, 6, 4, 12, 7, 5, 47, 5, 5} // COMFORT
    };
    static {
        for (int[] weights : CAR_WEIGHTS) {
            if (Arrays.stream(weights).sum() != 100) throw new IllegalStateException("Wagi CAR nie sumują się do 100");
        }
    }
    private final int[] weights;

    TravelScoreProfile(int... weights) {
        this.weights = weights;
        if (java.util.Arrays.stream(weights).sum() != 100) throw new IllegalStateException("Wagi nie sumują się do 100");
    }

    public int weight(Criterion criterion) { return weights[criterion.ordinal()]; }
    public int weight(Criterion criterion, TransportType transport) {
        return transport == TransportType.CAR ? CAR_WEIGHTS[ordinal()][criterion.ordinal()] : weight(criterion);
    }

    public enum Criterion {
        PRICE, STAY, DAYS_OFF, COMFORT, SCHEDULE, DISTANCE, ATTRACTIONS, MATCH, CONFIDENCE
    }
}
