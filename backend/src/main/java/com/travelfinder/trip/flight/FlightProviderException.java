package com.travelfinder.trip.flight;

import org.springframework.http.HttpStatus;

public class FlightProviderException extends RuntimeException {
    private final HttpStatus status;

    public FlightProviderException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
