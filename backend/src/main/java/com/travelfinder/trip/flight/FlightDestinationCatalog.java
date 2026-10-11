package com.travelfinder.trip.flight;

import java.time.ZoneId;
import java.util.Map;

/** Nazwy miast i strefy IANA dla popularnych kierunków z Polski.
 * Nie przypisujemy miast ani zdjęć na podstawie przypadkowych zgadywanek. */
public final class FlightDestinationCatalog {
    private record City(String name, String zone) { }
    private static final Map<String, City> CITIES = Map.ofEntries(
            city("LON", "Londyn", "Europe/London"), city("STN", "Londyn", "Europe/London"),
            city("LTN", "Londyn", "Europe/London"), city("LGW", "Londyn", "Europe/London"),
            city("MAN", "Manchester", "Europe/London"), city("EDI", "Edynburg", "Europe/London"),
            city("DUB", "Dublin", "Europe/Dublin"), city("ORK", "Cork", "Europe/Dublin"),
            city("ROM", "Rzym", "Europe/Rome"), city("FCO", "Rzym", "Europe/Rome"),
            city("CIA", "Rzym", "Europe/Rome"), city("MIL", "Mediolan", "Europe/Rome"),
            city("BGY", "Mediolan", "Europe/Rome"), city("MXP", "Mediolan", "Europe/Rome"),
            city("LIN", "Mediolan", "Europe/Rome"), city("NAP", "Neapol", "Europe/Rome"),
            city("BLQ", "Bolonia", "Europe/Rome"), city("VCE", "Wenecja", "Europe/Rome"),
            city("TSF", "Wenecja", "Europe/Rome"), city("PSA", "Piza", "Europe/Rome"),
            city("BRI", "Bari", "Europe/Rome"), city("PMO", "Palermo", "Europe/Rome"),
            city("CTA", "Katania", "Europe/Rome"), city("TRN", "Turyn", "Europe/Rome"),
            city("BCN", "Barcelona", "Europe/Madrid"), city("MAD", "Madryt", "Europe/Madrid"),
            city("AGP", "Malaga", "Europe/Madrid"), city("ALC", "Alicante", "Europe/Madrid"),
            city("VLC", "Walencja", "Europe/Madrid"), city("SVQ", "Sewilla", "Europe/Madrid"),
            city("PMI", "Palma", "Europe/Madrid"), city("IBZ", "Ibiza", "Europe/Madrid"),
            city("TFS", "Teneryfa", "Atlantic/Canary"), city("TFN", "Teneryfa", "Atlantic/Canary"),
            city("LPA", "Gran Canaria", "Atlantic/Canary"),
            city("LIS", "Lizbona", "Europe/Lisbon"), city("OPO", "Porto", "Europe/Lisbon"),
            city("PAR", "Paryż", "Europe/Paris"), city("BVA", "Paryż", "Europe/Paris"),
            city("CDG", "Paryż", "Europe/Paris"), city("ORY", "Paryż", "Europe/Paris"),
            city("MRS", "Marsylia", "Europe/Paris"), city("NCE", "Nicea", "Europe/Paris"),
            city("BER", "Berlin", "Europe/Berlin"), city("FRA", "Frankfurt", "Europe/Berlin"),
            city("HHN", "Frankfurt", "Europe/Berlin"), city("HAM", "Hamburg", "Europe/Berlin"),
            city("MUC", "Monachium", "Europe/Berlin"), city("DTM", "Dortmund", "Europe/Berlin"),
            city("DUS", "Düsseldorf", "Europe/Berlin"), city("CGN", "Kolonia", "Europe/Berlin"),
            city("AMS", "Amsterdam", "Europe/Amsterdam"), city("EIN", "Eindhoven", "Europe/Amsterdam"),
            city("BRU", "Bruksela", "Europe/Brussels"), city("CRL", "Bruksela", "Europe/Brussels"),
            city("CPH", "Kopenhaga", "Europe/Copenhagen"), city("BLL", "Billund", "Europe/Copenhagen"),
            city("OSL", "Oslo", "Europe/Oslo"), city("TRF", "Oslo", "Europe/Oslo"),
            city("BGO", "Bergen", "Europe/Oslo"), city("SVG", "Stavanger", "Europe/Oslo"),
            city("ARN", "Sztokholm", "Europe/Stockholm"), city("NYO", "Sztokholm", "Europe/Stockholm"),
            city("GOT", "Göteborg", "Europe/Stockholm"), city("HEL", "Helsinki", "Europe/Helsinki"),
            city("RIX", "Ryga", "Europe/Riga"), city("VNO", "Wilno", "Europe/Vilnius"),
            city("TLL", "Tallinn", "Europe/Tallinn"), city("KEF", "Reykjavík", "Atlantic/Reykjavik"),
            city("PRG", "Praga", "Europe/Prague"), city("VIE", "Wiedeń", "Europe/Vienna"),
            city("BUD", "Budapeszt", "Europe/Budapest"), city("BTS", "Bratysława", "Europe/Bratislava"),
            city("ZAG", "Zagrzeb", "Europe/Zagreb"), city("SPU", "Split", "Europe/Zagreb"),
            city("DBV", "Dubrownik", "Europe/Zagreb"), city("ZAD", "Zadar", "Europe/Zagreb"),
            city("LJU", "Lublana", "Europe/Ljubljana"), city("SJJ", "Sarajewo", "Europe/Sarajevo"),
            city("BEG", "Belgrad", "Europe/Belgrade"), city("TGD", "Podgorica", "Europe/Podgorica"),
            city("TIA", "Tirana", "Europe/Tirane"), city("SKP", "Skopje", "Europe/Skopje"),
            city("SOF", "Sofia", "Europe/Sofia"), city("OTP", "Bukareszt", "Europe/Bucharest"),
            city("CLJ", "Kluż-Napoka", "Europe/Bucharest"),
            city("ATH", "Ateny", "Europe/Athens"), city("SKG", "Saloniki", "Europe/Athens"),
            city("HER", "Heraklion", "Europe/Athens"), city("RHO", "Rodos", "Europe/Athens"),
            city("CFU", "Korfu", "Europe/Athens"), city("CHQ", "Chania", "Europe/Athens"),
            city("MLA", "Malta", "Europe/Malta"), city("LCA", "Larnaka", "Asia/Nicosia"),
            city("PFO", "Pafos", "Asia/Nicosia"), city("IST", "Stambuł", "Europe/Istanbul"),
            city("SAW", "Stambuł", "Europe/Istanbul"), city("AYT", "Antalya", "Europe/Istanbul"),
            city("DXB", "Dubaj", "Asia/Dubai"), city("AUH", "Abu Zabi", "Asia/Dubai"),
            city("GDN", "Gdańsk", "Europe/Warsaw"), city("KRK", "Kraków", "Europe/Warsaw"),
            city("WAW", "Warszawa", "Europe/Warsaw"), city("WMI", "Warszawa", "Europe/Warsaw"),
            city("WRO", "Wrocław", "Europe/Warsaw"), city("POZ", "Poznań", "Europe/Warsaw"),
            city("KTW", "Katowice", "Europe/Warsaw"), city("SZZ", "Szczecin", "Europe/Warsaw"),
            city("RZE", "Rzeszów", "Europe/Warsaw"), city("LUZ", "Lublin", "Europe/Warsaw"),
            city("LCJ", "Łódź", "Europe/Warsaw"), city("BZG", "Bydgoszcz", "Europe/Warsaw")
    );

    private static Map.Entry<String, City> city(String iata, String name, String zone) {
        return Map.entry(iata, new City(name, zone));
    }

    private FlightDestinationCatalog() { }

    public static String name(String city, String airport) {
        City value = CITIES.get(city);
        if (value == null) value = CITIES.get(airport);
        return value == null ? (city == null || city.isBlank() ? airport : city) : value.name();
    }

    public static ZoneId zone(String city, String airport, java.time.ZoneOffset fallback) {
        City value = CITIES.get(city);
        if (value == null) value = CITIES.get(airport);
        return value == null ? fallback : ZoneId.of(value.zone());
    }
}
