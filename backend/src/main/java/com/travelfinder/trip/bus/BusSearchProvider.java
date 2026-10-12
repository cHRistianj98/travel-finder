package com.travelfinder.trip.bus;

import com.travelfinder.trip.domain.TripOption;
import com.travelfinder.trip.domain.TripSearchRequest;
import java.util.List;

@FunctionalInterface
public interface BusSearchProvider {
    List<TripOption> search(TripSearchRequest request);
}
