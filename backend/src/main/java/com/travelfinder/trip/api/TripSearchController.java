package com.travelfinder.trip.api;

import com.travelfinder.trip.application.TripSearchService;
import com.travelfinder.trip.scoring.TravelScoringService;
import com.travelfinder.trip.bus.FlixBusPublicPreviewService;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import java.time.LocalDate;
import com.travelfinder.trip.domain.TripOption;
import com.travelfinder.trip.domain.TripSearchRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripSearchController {

    private final TripSearchService service;
    private final FlixBusPublicPreviewService busPreviewService;
    private final TravelScoringService scoringService;

    public TripSearchController(TripSearchService service, FlixBusPublicPreviewService busPreviewService,
            TravelScoringService scoringService) {
        this.service = service;
        this.busPreviewService = busPreviewService;
        this.scoringService = scoringService;
    }

    /** Publiczny status funkcji bez udostępniania klucza API ani kontaktu z PLK. */
    @GetMapping("/providers")
    public java.util.Map<String, Boolean> providers() {
        return java.util.Map.of("trainAvailable", service.isTrainAvailable(),
                "busAvailable", service.isBusAvailable());
    }

    /** Podgląd reklamowego minimum cenowego i linków do wyszukiwania. Nie jest to cena kursu. */
    @GetMapping("/bus/preview")
    public FlixBusPublicPreviewService.Preview busPreview(
            @RequestParam String from, @RequestParam String to,
            @RequestParam LocalDate departureDate, @RequestParam LocalDate returnDate) {
        return busPreviewService.preview(from, to, departureDate, returnDate);
    }

    /** Jednoklikowe wyszukiwanie wybranego dnia: tam i powrót to osobne linki. */
    @GetMapping("/bus/search-link")
    public ResponseEntity<Void> busSearchLink(
            @RequestParam String from, @RequestParam String to, @RequestParam LocalDate date) {
        String destination = busPreviewService.searchLink(from, to, date);
        // URL pochodzi tylko z dwóch zaufanych generatorów (flixbus.com), a nie z requestu.
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(destination)).build();
    }

    /** Przelicz profil rankingu bez ponownego odpytywania zewnętrznych providerów. */
    @PostMapping("/score")
    public List<TravelScoringService.ScoredTrip> score(@RequestBody TravelScoringService.ScoreRequest payload) {
        return scoringService.scoreAll(payload);
    }

    @PostMapping("/search")
    public List<TripOption> search(@Valid @RequestBody TripSearchRequest request) {
        return service.search(request);
    }
}
