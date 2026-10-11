package com.travelfinder.trip.flight;

import tools.jackson.databind.JsonNode;
import java.util.Map;

/** Pozwala testować adapter z fixture JSON, bez wykonywania płatnych requestów. */
@FunctionalInterface
public interface SearchApiGateway {
    JsonNode search(Map<String, String> parameters);
}
