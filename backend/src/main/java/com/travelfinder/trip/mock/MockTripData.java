package com.travelfinder.trip.mock;

import com.travelfinder.trip.domain.TransportType;
import com.travelfinder.trip.domain.TripOption;
import com.travelfinder.trip.domain.TripSearchRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/** Dane demonstracyjne. Nie pochodzą z systemów rezerwacyjnych. */
@Component
public class MockTripData {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    public List<TripOption> forDates(TripSearchRequest request) {
        LocalDate from = request.outboundDate();
        LocalDate to = request.returnDate();
        String origin = request.origin().trim();

        return List.of(
            option("11111111-1111-4111-8111-111111111111", origin, "Rzym", TransportType.FLIGHT,
                    "189", from, "18:10", "20:05", to, "18:30", "20:25", 0, 0),
            option("22222222-2222-4222-8222-222222222222", origin, "Praga", TransportType.TRAIN,
                    "149", from, "16:30", "20:35", to, "17:10", "21:15", 0, 0),
            option("33333333-3333-4333-8333-333333333333", origin, "Berlin", TransportType.BUS,
                    "118", from, "17:20", "21:30", to, "17:20", "21:40", 0, 0),
            option("44444444-4444-4444-8444-444444444444", origin, "Karpacz", TransportType.CAR,
                    "95", from, "17:30", "19:25", to, "18:05", "20:00", 0, 0),
            option("55555555-5555-4555-8555-555555555555", origin, "Mediolan", TransportType.FLIGHT,
                    "239", from, "21:05", "22:55", to, "19:00", "20:50", 0, 0),
            option("66666666-6666-4666-8666-666666666666", origin, "Drezno", TransportType.TRAIN,
                    "169", from, "15:35", "19:25", to, "18:10", "22:00", 1, 1)
        );
    }

    private TripOption option(
            String id, String origin, String destination, TransportType type, String price,
            LocalDate from, String outboundStart, String outboundEnd,
            LocalDate to, String returnStart, String returnEnd,
            int outboundTransfers, int returnTransfers
    ) {
        OffsetDateTime departure = at(from, outboundStart);
        OffsetDateTime arrival = at(from, outboundEnd);
        OffsetDateTime backDeparture = at(to, returnStart);
        OffsetDateTime backArrival = at(to, returnEnd);

        long travelMinutes = Duration.between(departure, arrival).toMinutes()
                + Duration.between(backDeparture, backArrival).toMinutes();
        long stayMinutes = Duration.between(arrival, backDeparture).toMinutes();

        return new TripOption(
                UUID.fromString(id), origin, destination,
                departure, arrival, backDeparture, backArrival,
                type, new BigDecimal(price), "PLN",
                travelMinutes, stayMinutes,
                outboundTransfers, returnTransfers,
                "MOCK", null
        );
    }

    private OffsetDateTime at(LocalDate date, String time) {
        return date.atTime(LocalTime.parse(time)).atZone(ZONE).toOffsetDateTime();
    }
}
