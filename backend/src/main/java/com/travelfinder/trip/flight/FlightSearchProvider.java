package com.travelfinder.trip.flight;

import com.travelfinder.trip.domain.TripOption;
import com.travelfinder.trip.domain.TripSearchRequest;
import java.util.List;

/** Zewnętrzne źródło lotów; reszta domeny nie zna jego formatu JSON. */
@FunctionalInterface
public interface FlightSearchProvider {
    List<TripOption> search(TripSearchRequest request);
}
