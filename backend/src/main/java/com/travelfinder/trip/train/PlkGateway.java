package com.travelfinder.trip.train;

import tools.jackson.databind.JsonNode;
import java.time.LocalDate;

/** Port do oficjalnego PKP PLK Open Railway Data API; łatwy do zastąpienia w testach. */
public interface PlkGateway {
    JsonNode stations(String city);
    JsonNode schedules(int stationId, LocalDate from, LocalDate to);
}
