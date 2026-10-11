# Travel Finder — Slice 01

**React + TypeScript + Vite → REST → Spring Boot → mockowane `TripOption[]`**

Pierwszy działający wycinek multimodalnej wyszukiwarki weekendowych wyjazdów. UI jest po polsku, responsywne i pokazuje pełny flow od formularza do kart wyników.

## Uruchomienie w VS Code (Windows / macOS / Linux)

Wymagania:
- **JDK 21** (`java -version`)
- **Maven 3.6.3+** (`mvn -version`)
- **Node.js 22.12+** (`node -v`)
- **npm** (`npm -v`)
- **VS Code**, najlepiej z rozszerzeniami rekomendowanymi przy otwarciu repozytorium

### 1. Otwórz projekt

W VS Code: **File → Open Folder → `travel-finder`** (folder główny, nie samo `frontend`).

### 2. Backend (terminal 1)

```bash
cd backend
mvn spring-boot:run
```

Backend działa na **http://localhost:8080**.

### 3. Frontend (terminal 2)

```bash
cd frontend
npm install
npm run dev
```

Otwórz **http://localhost:5173**. Vite automatycznie przekierowuje `/api/*` do backendu na porcie 8080; osobna konfiguracja CORS nie jest wymagana.

Alternatywnie w VS Code: **Terminal → Run Task** → `Backend: Spring Boot` i `Frontend: Vite` (po wcześniejszym `npm install`).

**PostgreSQL nie jest potrzebny do uruchomienia tego etapu.** Jeśli chcesz przygotować bazę na przyszłość, uruchom osobno:

```bash
cd infra
docker compose up -d
```

To uruchomi pustą bazę `travel_finder` na porcie 5432. Backend celowo nie ma sterownika JDBC, JPA ani tabel.

## Co działa

- Wybór miasta startu i dat, godzin, budżetu, transportu oraz bezpośrednich połączeń.
- Walidacja podstawowych pól formularza.
- `POST /api/trips/search` z React do Spring Boot.
- Mockowane kierunki: Rzym, Praga, Berlin, Karpacz, Mediolan, Drezno.
- Podstawowe dopasowanie mocków do: dat, godziny najwcześniejszego wyjazdu, godziny najpóźniejszego powrotu, budżetu, wybranych typów transportu i `directOnly`.
- Liczenie `totalTravelDurationMinutes` i `timeAtDestinationMinutes` po stronie backendu.
- Interfejs desktop/mobile, loading skeletons, empty state i ekran błędu (np. backend nie działa).
- Rozwijane szczegóły trasy w każdej karcie.
- Przykładowe zdjęcia pobierane z Unsplash; gdy są niedostępne, pozostają kolorowe tła gradientowe. Fonty mają lokalne fallbacki.

## Czego na razie NIE ma

- Prawdziwych połączeń, cen ani linków rezerwacyjnych. `provider = MOCK`.
- `origin → anywhere` z prawdziwego zewnętrznego API.
- Wielu okien godzinowych dla różnych dni, przesiadek modelowanych jako osobne odcinki, lotnisk, geo-lokalizacji, kierunków spoza mocków.
- Zaawansowanych filtrów, Weekend Score, rankingu, historii, logowania, bazy danych i zapisywania wyników.

**Ważne:** przykładowe ceny nie są aktualnymi ofertami; także cena samochodu to wyłącznie fikcyjna wartość do prezentacji UI.

## Kontrakt REST

`POST http://localhost:8080/api/trips/search`

```json
{
  "origin": "Wrocław",
  "outboundDate": "2026-10-16",
  "returnDate": "2026-10-18",
  "earliestOutboundDeparture": "15:00",
  "latestReturnArrival": "23:00",
  "maxTransportBudget": 500,
  "currency": "PLN",
  "transportTypes": ["FLIGHT", "TRAIN", "BUS", "CAR"],
  "directOnly": true
}
```

Odpowiedź: tablica `TripOption` z czasami w ISO-8601 z offsetem, ceną w PLN, długością podróży, czasem na miejscu i przesiadkami. Przy błędnym żądaniu: HTTP 400 + `{ "message": "..." }`.

Przykładowy test w terminalu z `curl`:

```bash
curl -X POST http://localhost:8080/api/trips/search \
  -H "Content-Type: application/json" \
  -d '{"origin":"Wrocław","outboundDate":"2026-10-16","returnDate":"2026-10-18","earliestOutboundDeparture":"15:00","latestReturnArrival":"23:00","maxTransportBudget":500,"currency":"PLN","transportTypes":["FLIGHT","TRAIN","BUS","CAR"],"directOnly":true}'
```

## Struktura

```text
travel-finder/
├── frontend/
│   ├── src/api/trips.ts                  # fetch → backend
│   ├── src/features/trip-search/
│   │   ├── types.ts                      # kontrakty TS
│   │   ├── TripSearchForm.tsx            # wejście użytkownika
│   │   ├── TripSearchPage.tsx            # stan + przepływ
│   │   ├── TripResults.tsx               # loading/empty/error/wyniki
│   │   └── TripResultCard.tsx            # karta i szczegóły
│   ├── src/components/Icon.tsx           # małe ikony SVG
│   └── src/styles/                       # spójny design system MVP
├── backend/
│   ├── src/main/java/com/travelfinder/trip/
│   │   ├── api/                          # kontroler + obsługa 400
│   │   ├── application/                  # TripSearchService
│   │   ├── domain/                       # DTO niezależne od API
│   │   └── mock/                         # 6 tras demonstracyjnych
│   └── src/test/                         # testy podstawowych reguł
├── infra/docker-compose.yml             # opcjonalny PostgreSQL
└── .vscode/                              # rekomendacje i zadania
```

## Dlaczego bez `TransportProvider`?

To dopiero **Etap 1**. W Etapie 2, po sprawdzeniu aktualnej dokumentacji i realnych możliwości API `origin → anywhere`, zastąpimy `MockTripData` adapterem integracyjnym i dopiero wtedy wybierzemy interfejs providerów.

## Testy

```bash
cd backend
mvn test
```

```bash
cd frontend
npm run build
```

## Decyzje projektowe

- Daty wyjazdu/powrotu: `LocalDate`. Godziny graniczne: `LocalTime`.
- Konkretne odjazdy/przyloty: `OffsetDateTime`, nie `LocalDateTime`.
- Wspólny `TripOption`, bez `SkyscannerFlightOffer` ani innych DTO providerów.
- `bookingUrl = null` — UI nie sugeruje rezerwacji fikcyjnych ofert.
- `origin` to w pierwszym slice zwykły `String`. Identyfikację lokalizacji zaprojektujemy przy pierwszym prawdziwym API.

## Uwaga o środowisku generowania

Kod przygotowano jako kompletny szkielet, ale środowisko, w którym wygenerowano archiwum, nie miało dostępu do pobrania zależności z npm/Maven Central. W związku z tym **nie było możliwe pełne uruchomienie aplikacji React + Spring Boot** przed przekazaniem. Uruchom lokalnie powyższe komendy — możemy wspólnie poprawić każdy pojawiający się problem.
