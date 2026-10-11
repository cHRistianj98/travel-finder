# Travel Finder — prawdziwe loty (SearchApi)

## 1. Sekret lokalny

W folderze głównym `C:\projekty\travel-finder` utwórz plik `.env.local`
(zwróć uwagę na kropkę i brak rozszerzenia `.txt`):

```properties
SEARCHAPI_KEY=twoj_wlasny_klucz_searchapi
```

Możesz skopiować `.env.local.example` i zmienić nazwę. Nigdy nie przesyłaj tego pliku ani nie dodawaj go do Git. `.gitignore` już wyklucza `.env.local`.

Backend Spring Boot importuje plik `../.env.local` **przy uruchomieniu z folderu `backend/`**, czyli tak, jak działa dotychczasowy skrypt START. Nie trzeba edytować BAT ani frontendu. Alternatywnie użyj zmiennej środowiskowej `SEARCHAPI_KEY` bez pliku.

## 2. Uruchomienie

Wykonaj dotychczasowy `STOP-Travel-Finder.bat` i ponownie `START-Travel-Finder.bat`, aby przeładować klucz w Spring Boot. Porty: backend `8081`, Vite `5173`, PostgreSQL `5434` (nadal opcjonalny dla lotów).

W formularzu wybierz np. **Wrocław, 16–18.10.2026, samolot, budżet 500 PLN**. Dopasowanie dotyczy wybranych dat oraz *godziny rozpoczęcia wylotu* i *godziny dotarcia z powrotem*. Lotów innych niż zaprezentowane przez API nie dopisujemy automatycznie.

Jeśli wybierzesz też pociąg/autobus/samochód, te trzy środki transportu **nadal korzystają z danych demonstracyjnych** — ich karty są tak oznaczone.

## 3. Limit zapytań

Jedno kliknięcie to maksymalnie:
- 1 × `google_travel_explore` (kierunki origin → anywhere),
- 3 × `google_flights` (szczegółowe loty),
- 6 × `google_flights` z `departure_token` (pasujące powroty).

Łącznie maks. **10 zapytań** do SearchApi; mniej, jeśli nie ma pasujących lotów.
Powtarzane *identyczne* zapytania do API są pamiętane przez backend przez 5 minut (cache w pamięci procesu). Limity można zmniejszyć przez `searchapi.max-destinations` i `searchapi.max-outbounds-per-destination` w `application.properties`.

## 4. Techniczny przebieg

`POST /api/trips/search` → `TripSearchService` → `SearchApiFlightProvider` →
1. Google Travel Explore (wymagane lotnisko wylotu, daty i budżet),
2. Google Flights round_trip (początkowe oferty wylotu),
3. Google Flights z `departure_token` (pasujące loty powrotne i łączna cena),
4. konwersja lokalnych czasów do `OffsetDateTime` z realną strefą IANA (na podstawie współrzędnych z Explore),
5. filtr dokładnych godzin HH:mm, cen i liczby przesiadek.

`TripOption` pozostaje niezależny od SearchApi. `provider = SEARCHAPI_GOOGLE_FLIGHTS`.
`bookingUrl` pozostaje `null`, ponieważ bez dodatkowego wyboru `booking_token` nie mamy bezpośredniego linku do **tej samej taryfy**. Nie należy przedstawiać ceny jako gwarantowanej do zakupu.

## 5. Możliwe błędy

- `503 Brak klucza SearchApi` → sprawdź `.env.local`, uruchom ponownie backend w katalogu `backend/`.
- `502 ... 401/403` → zły klucz lub problem autoryzacji po stronie SearchApi.
- `429` → limit zapytań / planu SearchApi; poczekaj lub sprawdź konto.
- Pusta lista → Explore lub rozpatrywane oferty nie dały prawidłowego zestawienia lotów spełniających godziny/budżet.
- Białystok + tylko samolot → świadomie komunikujemy brak obsługiwanego portu lotniczego.

## 6. Testy

```powershell
cd C:\projekty\travel-finder\backend
mvn test
```

Dołączone testy używają sztucznych odpowiedzi JSON, dlatego nie wykorzystują klucza i nie zużywają limitu. Nie było możliwości wykonania testu end-to-end z prawdziwym SearchApi w środowisku generowania.

Oficjalna dokumentacja:
- https://www.searchapi.io/docs/google-travel-explore-api
- https://www.searchapi.io/docs/google-flights-api
- https://github.com/RomanIakovlev/timeshape
