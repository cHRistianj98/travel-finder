package com.travelfinder.trip.train;

import com.travelfinder.trip.domain.TripOption;
import com.travelfinder.trip.domain.TripSearchRequest;
import java.util.List;

@FunctionalInterface
public interface TrainSearchProvider {
    List<TripOption> search(TripSearchRequest request);
}
