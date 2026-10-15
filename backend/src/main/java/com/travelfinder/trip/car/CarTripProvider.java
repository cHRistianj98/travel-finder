package com.travelfinder.trip.car;

import com.travelfinder.trip.domain.*;
import com.travelfinder.trip.scoring.TravelDestinationCatalog;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

/** Szacunkowe weekendowe wypady własnym samochodem; bez pozorowania dostępności parkingu. */
@Service
public class CarTripProvider {
    private static final ZoneId ZONE=ZoneId.of("Europe/Warsaw");
    private final CarFuelPriceService prices;
    public CarTripProvider(CarFuelPriceService prices) { this.prices=prices; }
    public List<TripOption> search(TripSearchRequest request) {
        CarPreferences preferences=request.car()==null ? new CarPreferences(null,null,null,null,null,null,null,null) : request.car();
        preferences.validate();
        TravelDestinationCatalog.Place origin = TravelDestinationCatalog.find(request.origin());
        if (origin==null) throw new IllegalArgumentException("Samochód: nie znamy współrzędnych miasta wyjazdu " + request.origin());
        AvailabilityCalendar calendar=AvailabilityCalendar.from(request);
        List<LocalDate[]> windows = windows(request);
        if (windows.isEmpty()) return List.of();
        // Cache: raz na region i paliwo przy jednym wyszukiwaniu. Stary provider lotów nie zostaje odpytywany.
        Map<String, CarFuelPriceService.Quote> quotes = new HashMap<>();
        List<TripOption> results=new ArrayList<>();
        List<Destination> destinations=new ArrayList<>();
        for (CarAttractionCatalog.Attraction attraction : CarAttractionCatalog.all()) {
            double straight = CarAttractionCatalog.distanceKm(origin.lat(),origin.lon(),attraction.lat(),attraction.lon());
            // Haversine × faktor dróg (szacunek, nie API nawigacji). Górskie cele mają zwykle krętsze drogi.
            int roadKm=(int)Math.ceil(straight * (straight<140 ? 1.38 : straight<380 ? 1.30 : 1.25) + 8);
            if(roadKm<30 || roadKm>preferences.maxDistance()) continue;
            destinations.add(new Destination(attraction,roadKm));
        }
        // Nie wybieraj najpierw 90 najbliższych! W przeciwnym razie po rozbudowie katalogu
        // miejscowości 600–1500 km NIGDY nie trafiłyby do wyników.
        destinations.sort(Comparator.comparingDouble((Destination d) -> appealValue(d)).reversed()
                .thenComparingInt(Destination::km));
        int capPerWindow = Math.max(10, 120 / Math.max(1, windows.size()));
        for (LocalDate[] window : windows) {
            List<List<Destination>> bands = new ArrayList<>();
            for (int i=0;i<4;i++) bands.add(new ArrayList<>());
            for (Destination destination : destinations) {
                int km = destination.km();
                int band = km <= 220 ? 0 : km <= 500 ? 1 : km <= 900 ? 2 : 3;
                bands.get(band).add(destination);
            }
            List<TripOption> selected = new ArrayList<>();
            Set<String> used = new HashSet<>();
            Map<String,Integer> perCity = new HashMap<>();
            int quota = Math.max(2, capPerWindow / 4);
            for (List<Destination> band : bands) {
                addCandidates(band, Math.min(capPerWindow, selected.size() + quota), request, preferences, calendar, window,
                        quotes, selected, used, perCity);
            }
            // Krótszy maks. dystans albo zbyt drogie dalekie cele: wypełniamy
            // wolne miejsca z innych przedziałów, nie pokazujemy pustej listy.
            addCandidates(destinations, capPerWindow, request, preferences, calendar, window,
                    quotes, selected, used, perCity);
            results.addAll(selected);
        }
        return results.stream().limit(120).toList();
    }
    private static double appealValue(Destination destination) {
        var a = CarDestinationAppeal.forDestination(destination.attraction().destination());
        double attractiveness = a == null ? (destination.attraction().interest() / 5d) : a.factor();
        return attractiveness - .09 * destination.km() / 1500d;
    }
    private void addCandidates(List<Destination> choices, int wanted, TripSearchRequest request,
            CarPreferences pref, AvailabilityCalendar calendar, LocalDate[] window,
            Map<String,CarFuelPriceService.Quote> quotes, List<TripOption> selected,
            Set<String> used, Map<String,Integer> perCity) {
        for (Destination destination : choices) {
            if (selected.size() >= wanted) break;
            var place = destination.attraction();
            String placeKey = CarAttractionCatalog.normalize(place.destination());
            String cityKey = CarAttractionCatalog.normalize(place.country() + " " + place.city());
            if (used.contains(placeKey) || perCity.getOrDefault(cityKey,0) >= 3) continue;
            TripOption option = build(request,pref,calendar,window,destination,quotes);
            if (option != null && option.price().compareTo(request.maxTransportBudget())<=0) {
                selected.add(option);
                used.add(placeKey);
                perCity.merge(cityKey,1,Integer::sum);
            }
        }
    }

    private TripOption build(TripSearchRequest request, CarPreferences pref, AvailabilityCalendar availability,
            LocalDate[] window, Destination place, Map<String,CarFuelPriceService.Quote> quotes) {
        long eachMinutes=(long)Math.ceil(place.km()/(place.km()<260 ? 62d : 72d)*60 + 20);
        LocalDate departureDay=window[0], returnDay=window[1];
        // Własnym autem wyjazd o dowolnej godzinie W WYBRANYM oknie, ale unikamy niepotrzebnie późnego startu.
        LocalTime dep=clampTime(LocalTime.of(7,0),request.earliestOutboundDeparture(),request.latestOutboundDeparture());
        ZonedDateTime departure=ZonedDateTime.of(departureDay,dep,ZONE);
        ZonedDateTime arrival=departure.plusMinutes(eachMinutes);
        // Najpóźniejszy możliwy powrót w dozwolonym przedziale, tak by dotrzeć przed północą końca wolnych dni.
        ZonedDateTime deadline=ZonedDateTime.of(returnDay,LocalTime.of(23,55),ZONE).minusMinutes(eachMinutes);
        ZonedDateTime ideal=ZonedDateTime.of(returnDay,request.latestReturnDeparture(),ZONE);
        ZonedDateTime back=ideal.isBefore(deadline)?ideal:deadline;
        ZonedDateTime earliestBack=ZonedDateTime.of(returnDay,request.earliestReturnDeparture(),ZONE);
        if (back.isBefore(earliestBack) || back.isBefore(arrival.plusHours(5))) return null;
        ZonedDateTime home=back.plusMinutes(eachMinutes);
        if (!availability.allowsTrip(departure.toLocalDate(),home.toLocalDate()) || !home.isAfter(arrival)) return null;
        int roundKm=place.km()*2;
        double fuelLitres=roundKm*pref.consumption()/100d;
        String country=place.attraction().country();
        CarFuelPriceService.Quote polish=quote(quotes,"PL",pref);
        CarFuelPriceService.Quote foreign=country.equals("PL")?polish:quote(quotes,country,pref);
        // Model tankowania: 65% zużycia wg ceny PL, 35% wg średniej docelowego kraju.
        BigDecimal litre=pref.fuelPriceOverridePln()!=null ? money(pref.fuelPriceOverridePln())
                : country.equals("PL") ? polish.plnPerLitre() : polish.plnPerLitre().multiply(BigDecimal.valueOf(.65))
                    .add(foreign.plnPerLitre().multiply(BigDecimal.valueOf(.35))).setScale(2,RoundingMode.HALF_UP);
        BigDecimal fuelCost=money(fuelLitres*litre.doubleValue());
        int dayCount=(int)(java.time.temporal.ChronoUnit.DAYS.between(departure.toLocalDate(),home.toLocalDate())+1);
        BigDecimal parking=money(dayCount*pref.parkingDaily());
        // Czeskie autostrady: zakup dziesięciodniowej winiety jeśli jedziemy drogami płatnymi.
        // 2026: 300 CZK (10d) / 480 CZK (30d). Kurs na potrzeby szacunku 0,18 PLN/CZK.
        BigDecimal vignette=country.equals("CZ") && pref.vignette() ? money(dayCount<=10?54:86.40) : BigDecimal.ZERO.setScale(2);
        BigDecimal extra=money(pref.tolls());
        BigDecimal total=fuelCost.add(parking).add(vignette).add(extra);
        BigDecimal perPerson=total.divide(BigDecimal.valueOf(pref.people()),2,RoundingMode.HALF_UP);
        String priceSource=pref.fuelPriceOverridePln()!=null ? "RĘCZNA CENA PALIWA UŻYTKOWNIKA"
                : (country.equals("PL")?polish.source():"65% PL: " +polish.source()+"; 35% "+country+": "+foreign.source());
        String priceDate=pref.fuelPriceOverridePln()!=null ? "własne ustawienie" : (country.equals("PL")?polish.date():polish.date()+" / "+foreign.date());
        String tollNotice=country.equals("CZ") ? (pref.vignette()
                ? "Uwzględniono ORIENTACYJNIE czeską winietę, choć trasa może omijać drogi płatne. LPG bez ulgi. Kurs CZK/PLN orientacyjny."
                : "Bez winiety: trasa MUSI omijać czeskie płatne autostrady. Przed wyjazdem sprawdź trasę.")
                : country.equals("DE") ? "W Niemczech brak ogólnej opłaty autostradowej dla standardowego auta osobowego; możliwe opłaty za parking/strefy/tunele."
                : country.equals("PL") ? "Nie doliczono automatycznie płatnych odcinków A2/A4 ani parkingu konkretnego obiektu; można ustawić rezerwę."
                : "UWAGA: zagraniczna trasa może wymagać winiety, opłat autostradowych, tunelowych i miejskich. NIE są obliczone automatycznie; ustaw rezerwę w opłatach dodatkowych.";
        String fuelLabel=switch(pref.selectedFuel()){case PETROL->"Benzyna 95";case DIESEL->"Diesel";case LPG->"LPG";};
        CarTripDetails details=new CarTripDetails(place.attraction().landmark(),country,
                CarAttractionCatalog.nearbyCount(place.attraction(),45),place.km(),roundKm,eachMinutes,
                pref.people(),pref.consumption(),fuelLabel,litre,priceSource,priceDate,fuelCost,
                vignette,parking,extra,total,true,tollNotice);
        String maps="https://www.google.com/maps/dir/?api=1&origin="+enc(request.origin()+", Polska")
                +"&destination="+enc(place.attraction().lat()+","+place.attraction().lon())+"&travelmode=driving";
        UUID id=UUID.nameUUIDFromBytes((request.origin()+"|"+place.attraction().destination()+"|"+departure+"|"+home+"|"+pref.selectedFuel()).getBytes(StandardCharsets.UTF_8));
        long stay=Duration.between(arrival,back).toMinutes();
        return new TripOption(id,request.origin(),place.attraction().destination(),
                departure.toOffsetDateTime(),arrival.toOffsetDateTime(),back.toOffsetDateTime(),home.toOffsetDateTime(),
                TransportType.CAR,perPerson,"PLN",eachMinutes*2,stay,0,0,"CAR_ESTIMATE",maps,details);
    }
    private CarFuelPriceService.Quote quote(Map<String,CarFuelPriceService.Quote> cache, String country, CarPreferences pref) {
        if (pref.fuelPriceOverridePln()!=null) return new CarFuelPriceService.Quote(money(pref.fuelPriceOverridePln()),"Ustawione ręcznie","własne",false);
        return cache.computeIfAbsent(country, key -> prices.get(key,pref.selectedFuel()));
    }
    private static List<LocalDate[]> windows(TripSearchRequest request) {
        List<LocalDate> selected = new ArrayList<>();
        if (request.availableDates().isEmpty()) {
            for(LocalDate d=request.outboundDate();!d.isAfter(request.returnDate());d=d.plusDays(1))selected.add(d);
        } else selected.addAll(request.availableDates());
        selected.sort(Comparator.naturalOrder());
        List<LocalDate[]> result=new ArrayList<>();
        if(selected.isEmpty())return result;
        LocalDate start=selected.get(0),last=start;
        for (int i=1;i<=selected.size();i++) {
            if(i<selected.size() && selected.get(i).equals(last.plusDays(1))) {last=selected.get(i);continue;}
            if(last.isAfter(start)) {
                // Konkretny weekend do 5 dni; dłuższego okresu nie uważamy za automatyczne 62 dni parkingu.
                LocalDate end=last.isAfter(start.plusDays(4))?start.plusDays(4):last;
                result.add(new LocalDate[]{start,end});
            }
            if(i<selected.size()) start=last=selected.get(i);
            if(result.size()==8)break;
        }
        return result;
    }
    private static LocalTime clampTime(LocalTime ideal,LocalTime min,LocalTime max) {
        return ideal.isBefore(min)?min:ideal.isAfter(max)?max:ideal;
    }
    private static String enc(String input){return URLEncoder.encode(input,StandardCharsets.UTF_8);}
    private static BigDecimal money(double value){return BigDecimal.valueOf(value).setScale(2,RoundingMode.HALF_UP);}
    private record Destination(CarAttractionCatalog.Attraction attraction,int km){}
}
