package com.travelfinder.trip.flight;

import tools.jackson.databind.JsonNode;
import java.time.YearMonth;
import java.util.List;

/** Surowe, cache'owane oferty Aviasales dla miesiąca; nigdy live fares. */
@FunctionalInterface
public interface TravelpayoutsGateway {
    List<JsonNode> fetchPeriod(String airport, YearMonth outboundMonth, YearMonth returnMonth);
}
