package com.travelfinder.trip.car;

import java.text.Normalizer;
import java.util.*;

/** Redakcyjny katalog regionów/celów. Współrzędne orientacyjne punktu atrakcji, nie wjazdu na parking. */
public final class CarAttractionCatalog {
    private CarAttractionCatalog() {}
    public record Attraction(String country, String city, String landmark, double lat, double lon, int interest) {
        public String destination() { return city + " — " + landmark; }
    }
    // Dane punktowe orientacyjne. Aktualna dostępność oraz dojazdy do wejścia wymagają sprawdzenia na miejscu.
    private static final String DATA = """
PL|Karpacz|Świątynia Wang|50.776|15.723|5
PL|Karpacz|Śnieżka|50.736|15.740|5
PL|Szklarska Poręba|Wodospad Kamieńczyka|50.820|15.497|5
PL|Szklarska Poręba|Wodospad Szklarki|50.829|15.555|4
PL|Jelenia Góra|Zamek Chojnik|50.838|15.643|4
PL|Wałbrzych|Zamek Książ|50.843|16.292|5
PL|Świdnica|Kościół Pokoju|50.843|16.488|5
PL|Dzierżoniów|Arboretum Wojsławice|50.748|16.865|4
PL|Ząbkowice Śląskie|Krzywa Wieża|50.588|16.812|3
PL|Kłodzko|Twierdza Kłodzko|50.439|16.655|4
PL|Międzygórze|Wodospad Wilczki|50.232|16.765|4
PL|Kudowa-Zdrój|Błędne Skały|50.482|16.286|5
PL|Radków|Szczeliniec Wielki|50.484|16.341|5
PL|Kletno|Jaskinia Niedźwiedzia|50.247|16.853|5
PL|Polanica-Zdrój|Park Zdrojowy|50.407|16.512|3
PL|Wrocław|Hala Stulecia|51.107|17.077|4
PL|Bolków|Zamek Bolków|50.921|16.101|4
PL|Legnica|Zamek Piastowski|51.208|16.164|3
PL|Oleśnica|Zamek Książęcy|51.210|17.382|3
PL|Opole|Wieża Piastowska|50.668|17.924|3
PL|Moszna|Zamek Moszna|50.440|17.767|5
PL|Olsztyn koło Częstochowy|Zamek Olsztyn|50.751|19.275|4
PL|Częstochowa|Jasna Góra|50.813|19.098|5
PL|Kraków|Wawel|50.054|19.935|5
PL|Kraków|Rynek Główny|50.062|19.937|5
PL|Wieliczka|Kopalnia Soli|49.984|20.056|5
PL|Oświęcim|Auschwitz-Birkenau|50.027|19.203|5
PL|Zakopane|Morskie Oko|49.198|20.071|5
PL|Zakopane|Gubałówka|49.307|19.933|4
PL|Kościelisko|Dolina Kościeliska|49.251|19.858|4
PL|Białka Tatrzańska|Termy Bania|49.389|20.112|4
PL|Zator|Energylandia|49.999|19.411|5
PL|Ojców|Zamek w Pieskowej Skale|50.244|19.780|4
PL|Sandomierz|Stare Miasto|50.679|21.750|5
PL|Kazimierz Dolny|Rynek|51.322|21.948|5
PL|Lublin|Stare Miasto|51.247|22.568|4
PL|Warszawa|Łazienki Królewskie|52.214|21.032|5
PL|Warszawa|Stare Miasto|52.249|21.013|5
PL|Łódź|Ulica Piotrkowska|51.768|19.457|4
PL|Poznań|Stary Rynek|52.409|16.934|5
PL|Gniezno|Katedra|52.535|17.595|4
PL|Toruń|Stare Miasto|53.010|18.605|5
PL|Bydgoszcz|Wyspa Młyńska|53.122|18.000|4
PL|Gdańsk|Długi Targ|54.348|18.652|5
PL|Gdańsk|Westerplatte|54.407|18.671|4
PL|Sopot|Molo|54.446|18.569|4
PL|Malbork|Zamek Krzyżacki|54.039|19.028|5
PL|Frombork|Wzgórze Katedralne|54.359|19.680|4
PL|Gdynia|Klif Orłowski|54.473|18.565|4
PL|Hel|Fokarium|54.608|18.801|4
PL|Łeba|Wydmy Ruchome|54.755|17.496|5
PL|Międzyzdroje|Molo|53.928|14.451|4
PL|Świnoujście|Latarnia Morska|53.915|14.285|4
PL|Szczecin|Wały Chrobrego|53.431|14.564|4
PL|Świebodzin|Figura Chrystusa Króla|52.247|15.532|4
PL|Zielona Góra|Palmiarnia|51.934|15.508|3
PL|Białowieża|Puszcza Białowieska|52.705|23.840|5
PL|Białystok|Pałac Branickich|53.133|23.164|4
PL|Augustów|Jezioro Necko|53.851|22.977|3
PL|Mikołajki|Jezioro Mikołajskie|53.800|21.567|4
PL|Giżycko|Twierdza Boyen|54.027|21.773|4
PL|Solina|Zapora i jezioro|49.395|22.448|5
PL|Sanok|Skansen|49.562|22.204|4
PL|Rzeszów|Podziemna Trasa Turystyczna|50.038|22.004|4
CZ|Praga|Most Karola|50.086|14.412|5
CZ|Praga|Zamek na Hradczanach|50.091|14.400|5
CZ|Kutná Hora|Kaplica Czaszek|49.960|15.288|5
CZ|Kutná Hora|Katedra św. Barbary|49.945|15.263|5
CZ|Adršpach|Skalne Miasto|50.615|16.119|5
CZ|Teplice nad Metují|Teplickie Skały|50.594|16.145|5
CZ|Pec pod Sněžkou|Śnieżka od strony Czech|50.738|15.740|5
CZ|Harrachov|Wodospad Mumlawy|50.773|15.414|4
CZ|Liberec|Ještěd|50.733|14.985|5
CZ|Děčín|Pravčická Brána|50.884|14.281|5
CZ|Hřensko|Wąwóz Edmunda|50.872|14.243|4
CZ|Czeski Krumlov|Zamek|48.812|14.315|5
CZ|Hluboká nad Vltavou|Zamek Hluboká|49.053|14.435|5
CZ|Karlowe Wary|Kolumnada Młyńska|50.226|12.882|5
CZ|Mariańskie Łaźnie|Kolumnada|49.978|12.704|4
CZ|Pilzno|Katedra św. Bartłomieja|49.748|13.379|4
CZ|Brno|Zamek Špilberk|49.195|16.599|4
CZ|Brno|Willa Tugendhat|49.207|16.616|5
CZ|Ołomuniec|Kolumna Trójcy Świętej|49.594|17.251|5
CZ|Ostrawa|Dolne Vitkovice|49.824|18.279|4
CZ|Telč|Renesansowy Rynek|49.184|15.451|5
CZ|Třebíč|Dzielnica Żydowska|49.215|15.878|4
CZ|Mikulov|Zamek Mikulov|48.807|16.636|4
CZ|Morawski Kras|Przepaść Macocha|49.373|16.730|5
CZ|Jičín|Prachowskie Skały|50.471|15.289|5
CZ|Boublík|Šumava|48.981|13.612|4
CZ|Znojmo|Rotunda św. Katarzyny|48.856|16.052|4
CZ|Pardubice|Zamek Pardubice|50.038|15.782|3
CZ|Kromieryż|Ogrody Kwiatowe|49.298|17.388|5
CZ|Litomyšl|Zamek Litomyšl|49.873|16.312|5
DE|Berlin|Brama Brandenburska|52.516|13.378|5
DE|Berlin|Wyspa Muzeów|52.521|13.400|5
DE|Drezno|Frauenkirche|51.051|13.741|5
DE|Drezno|Zwinger|51.054|13.734|5
DE|Görlitz|Stare Miasto|51.157|14.987|5
DE|Budziszyn|Stare Miasto|51.181|14.430|4
DE|Saksonia Szwajcarska|Bastei|50.962|14.073|5
DE|Bad Schandau|Twierdza Königstein|50.919|14.057|5
DE|Miśnia|Albrechtsburg|51.165|13.470|5
DE|Lipsk|Pomnik Bitwy Narodów|51.312|12.413|5
DE|Lipsk|Zoo|51.349|12.374|4
DE|Chemnitz|Pomnik Karola Marksa|50.833|12.920|4
DE|Berlin|Pałac Charlottenburg|52.520|13.295|4
DE|Poczdam|Sanssouci|52.404|13.038|5
DE|Hamburg|Miniatur Wunderland|53.544|9.989|5
DE|Hamburg|Elbphilharmonie|53.541|9.984|5
DE|Monachium|Marienplatz|48.137|11.576|5
DE|Monachium|Englischer Garten|48.153|11.589|4
DE|Füssen|Neuschwanstein|47.557|10.749|5
DE|Kolonia|Katedra|50.941|6.958|5
DE|Frankfurt|Römerberg|50.110|8.682|4
DE|Heidelberg|Zamek|49.412|8.716|5
DE|Norymberga|Zamek Cesarski|49.457|11.075|5
DE|Bamberg|Stare Miasto|49.894|10.889|5
DE|Regensburg|Kamienny Most|49.022|12.098|4
DE|Rothenburg ob der Tauber|Stare Miasto|49.378|10.179|5
DE|Trewir|Porta Nigra|49.760|6.644|5
DE|Bonn|Dom Beethovena|50.736|7.100|4
DE|Wyspa Rugia|Königsstuhl|54.573|13.663|5
DE|Lubeka|Brama Holsztyńska|53.865|10.686|5
DE|Schwerin|Zamek|53.624|11.418|5
DE|Hanower|Herrenhäuser Gärten|52.392|9.696|4
DE|Hohenzollern|Zamek Hohenzollern|48.323|8.967|5
DE|Stuttgart|Muzeum Mercedes-Benz|48.789|9.232|4
DE|Düsseldorf|Promenada nad Renem|51.229|6.770|4
DE|Brema|Rynek i ratusz|53.077|8.806|4
""";
    private static final List<Attraction> ATTRACTIONS = parse(DATA);
    private static List<Attraction> parse(String data) {
        List<Attraction> output = new ArrayList<>();
        for (String line : data.split("\\R")) {
            if (line.isBlank()) continue;
            String[] cols = line.trim().split("\\|", -1);
            if (cols.length != 6) throw new IllegalStateException("Invalid attraction: " + line);
            output.add(new Attraction(cols[0],cols[1],cols[2],Double.parseDouble(cols[3]),Double.parseDouble(cols[4]),Integer.parseInt(cols[5])));
        }
        return List.copyOf(output);
    }
    public static List<Attraction> all() { return ATTRACTIONS; }
    public static String normalize(String s) {
        return Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('ł', 'l').replaceAll("[^a-z0-9]+", " ").trim();
    }
    public static int nearbyCount(Attraction attraction, int radiusKm) {
        return (int) ATTRACTIONS.stream().filter(candidate -> distanceKm(attraction.lat(),attraction.lon(),candidate.lat(),candidate.lon()) <= radiusKm).count();
    }
    public static double distanceKm(double aLat, double aLon, double bLat, double bLon) {
        double lat = Math.toRadians(bLat - aLat), lon = Math.toRadians(bLon - aLon);
        double x = Math.pow(Math.sin(lat/2),2) + Math.cos(Math.toRadians(aLat))*Math.cos(Math.toRadians(bLat))*Math.pow(Math.sin(lon/2),2);
        return 12742d * Math.asin(Math.sqrt(Math.min(1,x)));
    }
}
