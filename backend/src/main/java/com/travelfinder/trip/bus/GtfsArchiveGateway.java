package com.travelfinder.trip.bus;

import java.io.IOException;
import java.nio.file.Path;

@FunctionalInterface
public interface GtfsArchiveGateway {
    Path archive() throws IOException, InterruptedException;
}
