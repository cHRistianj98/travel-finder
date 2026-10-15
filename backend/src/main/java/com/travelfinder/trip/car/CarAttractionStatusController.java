package com.travelfinder.trip.car;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Czytelny status pobierania danych: bez sugerowania, że pobrano >1000 przed synchronizacją. */
@RestController
@RequestMapping("/api/car-attractions")
public class CarAttractionStatusController {
    private final CarAttractionSyncService service;
    public CarAttractionStatusController(CarAttractionSyncService service) { this.service = service; }
    @GetMapping("/status")
    public CarAttractionSyncService.Status status() { return service.status(); }
}
