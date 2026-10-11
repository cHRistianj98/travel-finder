package com.travelfinder.trip.api;

import com.travelfinder.trip.application.TripSearchService;
import com.travelfinder.trip.domain.TripOption;
import com.travelfinder.trip.domain.TripSearchRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripSearchController {

    private final TripSearchService service;

    public TripSearchController(TripSearchService service) {
        this.service = service;
    }

    @PostMapping("/search")
    public List<TripOption> search(@Valid @RequestBody TripSearchRequest request) {
        return service.search(request);
    }
}
