package com.travelfinder.trip.car;

/** Parametry samochodu to ustawienia użytkownika, nie dane od przewoźnika. */
public record CarPreferences(Fuel fuel, Double litresPer100Km, Integer passengers,
        Double parkingPlnPerDay, Double additionalTollsPln, Boolean useCzechMotorways,
        Double fuelPriceOverridePln, Integer maxOneWayKm) {
    public enum Fuel { PETROL, DIESEL, LPG }
    public Fuel selectedFuel() { return fuel == null ? Fuel.PETROL : fuel; }
    public double consumption() { return litresPer100Km == null ? switch(selectedFuel()) {
        case PETROL -> 7.2; case DIESEL -> 6.1; case LPG -> 9.0;
    } : litresPer100Km; }
    public int people() { return passengers == null ? 1 : passengers; }
    public double parkingDaily() { return parkingPlnPerDay == null ? 30 : parkingPlnPerDay; }
    public double tolls() { return additionalTollsPln == null ? 0 : additionalTollsPln; }
    public boolean vignette() { return useCzechMotorways == null || useCzechMotorways; }
    public int maxDistance() { return maxOneWayKm == null ? 750 : maxOneWayKm; }
    public void validate() {
        if (!(consumption() >= 2 && consumption() <= 30) || !Double.isFinite(consumption())) throw new IllegalArgumentException("Spalanie musi wynosić 2–30 l/100 km.");
        if (people() < 1 || people() > 9) throw new IllegalArgumentException("Podaj od 1 do 9 osób w samochodzie.");
        if (!Double.isFinite(parkingDaily()) || parkingDaily() < 0 || parkingDaily() > 500) throw new IllegalArgumentException("Rezerwa parkingowa: 0–500 PLN/dzień.");
        if (!Double.isFinite(tolls()) || tolls() < 0 || tolls() > 2000) throw new IllegalArgumentException("Dodatkowe opłaty drogowe: 0–2000 PLN.");
        if (fuelPriceOverridePln != null && (!Double.isFinite(fuelPriceOverridePln) || fuelPriceOverridePln < 0.5 || fuelPriceOverridePln > 20)) throw new IllegalArgumentException("Cena paliwa: 0,5–20 PLN/l.");
        if (maxDistance() < 30 || maxDistance() > 1500) throw new IllegalArgumentException("Maksymalny dystans samochodem: 30–1500 km w jedną stronę.");
    }
}
