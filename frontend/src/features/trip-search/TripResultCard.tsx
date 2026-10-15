import { useEffect, useMemo, useRef, useState } from 'react';
import { Icon, type IconName } from '../../components/Icon';
import { resolveDestinationMeta } from './destinationVisuals';
import { CarAttractionCover } from './CarAttractionCover';
import { flixBusDateSummary, flixBusSearchLink } from './flixBusBooking';
import { fetchFlixBusPreview, type FlixBusPublicPreview } from './flixBusPreview';
import type { TravelScore } from './travelScore';
import {
  TRANSPORT_LABELS,
  durationLabel,
  formatDate,
  formatPrice,
  formatTime,
  formatWeekday,
  type TripOption,
  type TransportType,
} from './types';

const TRANSPORT_ICONS: Record<TransportType, IconName> = {
  FLIGHT: 'plane',
  TRAIN: 'train',
  BUS: 'bus',
  CAR: 'car',
};

const TRANSPORT_THEMES: Record<TransportType, string> = {
  FLIGHT: 'flight',
  TRAIN: 'train',
  BUS: 'bus',
  CAR: 'car',
};

export function TripResultCard({ trip, index, score }: { trip: TripOption; index: number; score?: TravelScore }) {
  const [expanded, setExpanded] = useState(false);
  const cardRef = useRef<HTMLElement>(null);
  const [preview, setPreview] = useState<FlixBusPublicPreview | null>(null);
  const [previewStarted, setPreviewStarted] = useState(false);
  const [previewFailed, setPreviewFailed] = useState(false);
  const [copyState, setCopyState] = useState<'idle' | 'copied' | 'failed'>('idle');
  const meta = useMemo(
    () => resolveDestinationMeta(trip.transportType === 'CAR' ? trip.destination.split(' — ')[0] : trip.destination, trip.transportType),
    [trip.destination, trip.transportType],
  );
  const isTrain = trip.transportType === 'TRAIN';
  const isBus = trip.transportType === 'BUS';
  const isCar = trip.transportType === 'CAR' && Boolean(trip.carDetails);
  const car = trip.carDetails;
  const isSchedule = isTrain || isBus;
  const stayHours = Math.max(1, Math.round(trip.timeAtDestinationMinutes / 60));
  const allDirect = trip.outboundTransfers === 0 && trip.returnTransfers === 0;
  const themeClass = `transport-tone-${TRANSPORT_THEMES[trip.transportType]}`;
  const displayDestination = isCar ? trip.destination.split(' — ')[0] : meta.displayName || trip.destination;
  const landmark = isCar ? car?.landmark : meta.landmark;
  const busOutboundUrl = isBus ? flixBusSearchLink(trip.origin, trip.destination, trip.outboundDeparture) : '';
  const busReturnUrl = isBus ? flixBusSearchLink(trip.destination, trip.origin, trip.returnDeparture) : '';
  useEffect(() => {
    if (!isBus || !cardRef.current) return;
    let active = true;
    let launched = false;
    const load = () => {
      if (launched) return;
      launched = true;
      setPreviewStarted(true);
      void fetchFlixBusPreview(trip.origin, trip.destination, trip.outboundDeparture, trip.returnDeparture)
        .then(result => { if (active) setPreview(result); })
        .catch(() => { if (active) { setPreview(null); setPreviewFailed(true); } });
    };
    if (typeof IntersectionObserver === 'undefined') { load(); return () => { active = false; }; }
    const observer = new IntersectionObserver(items => {
      if (items.some(item => item.isIntersecting)) { load(); observer.disconnect(); }
    }, { rootMargin: '450px' });
    observer.observe(cardRef.current);
    return () => { active = false; observer.disconnect(); };
  }, [isBus, trip.origin, trip.destination, trip.outboundDeparture, trip.returnDeparture]);
  const copyBusDates = async () => {
    try {
      await navigator.clipboard.writeText(flixBusDateSummary(trip.origin, displayDestination, trip.outboundDeparture, trip.returnDeparture));
      setCopyState('copied');
    } catch {
      setCopyState('failed');
    }
  };

  return (
    <article ref={cardRef} className={`trip-card palette-${meta.palette} ${themeClass}`} style={{ animationDelay: `${index * 70}ms` }}>
      <div className="trip-cover">
        {isCar && car ? (
          <CarAttractionCover key={trip.destination} destination={trip.destination} landmark={car.landmark} />
        ) : meta.image ? (
          <img src={meta.image} alt={`${displayDestination} – wizualizacja AI kierunku`} loading="lazy" />
        ) : (
          <div className="trip-cover-placeholder" aria-hidden="true">
            <Icon name={isTrain ? 'train' : isBus ? 'bus' : 'plane'} size={74} strokeWidth={1} />
          </div>
        )}
        <div className="trip-cover-overlay" />
        <div className="trip-cover-meta">
          <span className={`transport-pill pill-${TRANSPORT_THEMES[trip.transportType]}`}>
            <Icon name={TRANSPORT_ICONS[trip.transportType]} size={14} /> {TRANSPORT_LABELS[trip.transportType]}
          </span>
          <span className="trip-badge">{isTrain ? 'PKP PLK · rozkład' : isBus ? 'FlixBus · GTFS' : isCar ? 'Własne auto · szacunek' : 'Aviasales · cache'}</span>
        </div>
        {score && <div className="travel-score-cover" title={score.partial ? `Travel Score: ${score.value}/100; dostępne kryteria ${score.availablePoints}/100 pkt` : `Travel Score: ${score.value}/100`}>
          <Icon name="sparkles" size={13} /> <strong>{score.value}</strong><span>/100</span>
        </div>}
        <div className="trip-cover-content">
          <small>{isCar ? car?.country === 'CZ' ? 'Czechy' : car?.country === 'DE' ? 'Niemcy' : 'Polska' : meta.country || 'Europa'}</small>
          <h3>{displayDestination}</h3>
          <p>{isCar ? 'Wycieczka samochodowa · atrakcja turystyczna' : meta.subtitle}</p>
          {landmark && <span className="trip-landmark-badge">
            <Icon name="sparkles" size={13} /> {landmark}
          </span>}
        </div>
      </div>

      <div className="trip-card-body">
        {score && <div className="travel-score-summary">
          <div className="travel-score-summary-line">
            <strong><Icon name="sparkles" size={14}/> Travel Score <span>{score.value}/100</span></strong>
            <span className={`travel-score-rating ${score.value >= 75 ? 'high' : score.value >= 50 ? 'mid' : 'low'}`}>
              {isCar ? 'Szacunkowy wynik' : score.partial ? 'Ocena częściowa' : score.value >= 75 ? 'Bardzo dobry' : score.value >= 50 ? 'Warto rozważyć' : 'Słabsze dopasowanie'}
            </span>
          </div>
          <div className="travel-score-meter" role="meter" aria-label="Ocena atrakcyjności podróży" aria-valuemin={0} aria-valuemax={100} aria-valuenow={score.value}>
            <span style={{ width: `${score.value}%` }} />
          </div>
          <div className="travel-score-summary-sub">
            {(isCar && score.cautions.find(text => text.startsWith('Miejsce raczej na krótką wycieczkę'))) || score.strengths[0] || 'Ocena na podstawie dostępnych parametrów'}
            {score.partial && <span>· Dane: {score.availablePoints}/100 pkt</span>}
          </div>
        </div>}
        <div className="trip-price-row">
          <div>
            <span className="micro-label">{isCar ? 'SZACUNEK · ZA OSOBĘ' : isBus && trip.price === null ? 'Ceny publikowane na stronach tras' : isSchedule ? 'Cena biletu' : 'Cena orientacyjna'}</span>
            {isBus && trip.price === null ? (
              <strong className={preview?.roundTripFromPln != null ? 'bus-indicative-price' : 'train-unknown-price'}>
                {preview?.roundTripFromPln != null ? `od ${formatPrice(preview.roundTripFromPln)}` : previewStarted && !preview && !previewFailed ? 'Sprawdzam…' : 'Do sprawdzenia'}
              </strong>
            ) : <strong className={isSchedule ? 'train-unknown-price' : ''}>{trip.price === null ? 'Do sprawdzenia' : formatPrice(trip.price, trip.currency)}</strong>}
            <small>{isCar && car ? `Cały samochód: ${formatPrice(car.totalCarCostPln)} / ${car.passengers} os. · tam i powrót` : isBus ? (trip.price !== null ? 'Cena partnerska za podróż w obie strony' : preview?.roundTripFromPln != null ? 'Suma reklamowanych minimów tam + powrót; NIE cena tych kursów' : 'Brak potwierdzonej ceny dla tych terminów') : isTrain ? 'u przewoźnika · brak ceny w PLK' : 'transport w obie strony'}</small>
          </div>
        </div>
        <div className="trip-route">
          <div className="route-stop">
            <span className="route-stop-top">{isCar ? 'START PODRÓŻY' : isSchedule ? 'ODJAZD Z DOMU' : 'WYLOT Z DOMU'}</span>
            <strong>{formatTime(trip.outboundDeparture)}</strong>
            <span>{formatWeekday(trip.outboundDeparture)}, {formatDate(trip.outboundDeparture)}</span>
          </div>
          <div className="route-mid">
            <div className={`route-line route-line-${TRANSPORT_THEMES[trip.transportType]}`}>
              <span /><Icon name={TRANSPORT_ICONS[trip.transportType]} size={17} /><span />
            </div>
            <small>{trip.origin}</small>
          </div>
          <div className="route-stop route-stop-end">
            <span className="route-stop-top">{isCar ? 'START POWROTU' : isSchedule ? 'ODJAZD POWROTNY' : 'WYLOT POWROTNY'}</span>
            <strong>{formatTime(trip.returnDeparture)}</strong>
            <span>{formatWeekday(trip.returnDeparture)}, {formatDate(trip.returnDeparture)}</span>
          </div>
        </div>
        <div className="trip-return-arrival">
          <Icon name="clock" size={13} />
          <span>{isSchedule || isCar ? 'Przyjazd' : 'Przylot'} do {trip.origin}: <strong>{formatTime(trip.returnArrival)}</strong> · {formatWeekday(trip.returnArrival)}, {formatDate(trip.returnArrival)}</span>
        </div>
        <div className="trip-facts">
          <span className="stay-highlight"><Icon name="hourglass" size={15} /> <strong>{stayHours} godz.</strong> na miejscu</span>
          <span className={`trip-fact trip-fact-${TRANSPORT_THEMES[trip.transportType]}`}><Icon name={allDirect ? 'check' : 'route'} size={15} /> {isCar ? 'Elastyczne godziny' : allDirect ? 'Bez przesiadek' : 'Z przesiadką'}</span>
        </div>
        {isCar && car && <div className="car-summary">
          <div><Icon name="route" size={15}/><strong>~{car.oneWayKm} km</strong><small>{car.roadEstimate ? 'szacunek / strona' : 'trasa OSRM / strona'}</small></div>
          <div><Icon name="clock" size={15}/><strong>~{durationLabel(car.drivingMinutesEachWay)}</strong><small>{car.roadEstimate ? 'czas szacunkowy' : 'czas z rezerwą / strona'}</small></div>
          <div><Icon name="compass" size={15}/><strong>{car.nearbyAttractions}</strong><small>atrakcji w okolicy</small></div>
        </div>}
        {isBus && <div className="bus-search-actions" aria-label="Wyszukaj autobusy we FlixBusie">
          <a className="bus-search-action" href={busOutboundUrl} target="_blank" rel="noopener noreferrer"
            aria-label={`Wyszukaj wyjazd ${trip.origin} do ${displayDestination} dnia ${trip.outboundDeparture.slice(0, 10)}`}>
            <span className="bus-search-action-label"><Icon name="bus" size={14} /> WYSZUKAJ WYJAZD <Icon name="arrow-up-right" size={15} /></span>
            <strong>{trip.origin} → {displayDestination}</strong>
            <span className="bus-search-action-time">{formatWeekday(trip.outboundDeparture)}, {formatDate(trip.outboundDeparture)} · {formatTime(trip.outboundDeparture)}</span>
          </a>
          <a className="bus-search-action bus-search-action-return" href={busReturnUrl} target="_blank" rel="noopener noreferrer"
            aria-label={`Wyszukaj powrót ${displayDestination} do ${trip.origin} dnia ${trip.returnDeparture.slice(0, 10)}`}>
            <span className="bus-search-action-label"><Icon name="refresh" size={14} /> WYSZUKAJ POWRÓT <Icon name="arrow-up-right" size={15} /></span>
            <strong>{displayDestination} → {trip.origin}</strong>
            <span className="bus-search-action-time">{formatWeekday(trip.returnDeparture)}, {formatDate(trip.returnDeparture)} · {formatTime(trip.returnDeparture)}</span>
          </a>
          <p className="bus-search-action-note">Otwieramy wyszukiwanie na wybrany dzień, 1 pasażer. W wynikach FlixBusa sprawdź godzinę, przystanek i cenę.</p>
        </div>}
        {expanded && <div className="trip-details" id={`details-${trip.id}`}>
          <div className="detail-line"><span>Podróż tam</span><strong>{formatTime(trip.outboundDeparture)} – {formatTime(trip.outboundArrival)}</strong></div>
          <div className="detail-line"><span>Powrót ({displayDestination} → {trip.origin})</span><strong>{formatTime(trip.returnDeparture)} – {formatTime(trip.returnArrival)}</strong></div>
          <div className="detail-line"><span>Łącznie w podróży</span><strong>{durationLabel(trip.totalTravelDurationMinutes)}</strong></div>
          {landmark && <div className="detail-line"><span>Cel wycieczki</span><strong>{landmark}</strong></div>}
          {!isCar && trip.destination !== displayDestination && <div className="detail-line"><span>Przystanek docelowy w źródle</span><strong>{trip.destination}</strong></div>}
          {isCar ? <div className="live-detail-note"><Icon name="info" size={14} /> Obraz atrakcji: lokalna wizualizacja AI albo fotografia Wikimedia Commons z podanym autorem i licencją. Ujęcia AI są poglądowe i mogą różnić się od rzeczywistego obiektu.</div>
            : meta.image && <div className="live-detail-note"><Icon name="info" size={14} /> Obraz na karcie to wizualizacja AI i może pokazywać inny punkt miejscowości niż cel wycieczki.</div>}
          <div className="detail-line"><span>Przesiadki tam / powrót</span><strong>{trip.outboundTransfers} / {trip.returnTransfers}</strong></div>
          {isCar && car && <div className="car-price-details">
            <strong><Icon name="car" size={17}/> Kalkulacja kosztu samochodu (szacunkowa)</strong>
            <div className="detail-line"><span>Dystans tam i powrót</span><strong>~{car.roundTripKm} km</strong></div>
            <div className="detail-line"><span>Spalanie i paliwo</span><strong>{car.litresPer100Km.toFixed(1)} l/100 km · {car.fuelType}</strong></div>
            <div className="detail-line"><span>Przyjęta cena paliwa</span><strong>{car.fuelPricePlnPerLitre.toFixed(2)} zł/l</strong></div>
            <div className="detail-line"><span>Koszt paliwa</span><strong>{formatPrice(car.fuelCostPln)}</strong></div>
            <div className="detail-line"><span>Parking (rezerwa)</span><strong>{formatPrice(car.parkingPln)}</strong></div>
            <div className="detail-line"><span>Winieta CZ (jeśli zakładana)</span><strong>{formatPrice(car.vignettePln)}</strong></div>
            <div className="detail-line"><span>Dodatkowe opłaty (rezerwa)</span><strong>{formatPrice(car.extraTollsPln)}</strong></div>
            <div className="detail-line car-total"><span>Całe auto / na osobę</span><strong>{formatPrice(car.totalCarCostPln)} / {formatPrice(trip.price ?? 0)}</strong></div>
            <p>Źródło paliwa: {car.fuelPriceSource}. Data: {car.fuelPriceDate}. <a href="https://www.fuel-prices.eu/" target="_blank" rel="noopener noreferrer">Dane fuel-prices.eu (CC BY 4.0)</a>.</p>
            <p>{car.tollNotice}</p>
            <p>{car.roadEstimate ? 'Nie udało się sprawdzić przebiegu trasy — dystans i czas są szacowane geometrycznie. Zweryfikuj je w Google Maps.' : <>Trasa liczona po drogach przez <a href="https://routing.openstreetmap.de/about.html" target="_blank" rel="noopener noreferrer">OSRM</a> na danych <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">© OpenStreetMap (ODbL)</a>. Czas obejmuje rezerwę na dojazd i krótkie postoje; nie jest prognozą korków na wybraną godzinę. Przy szlakach górskich dojazd dotyczy okolicy punktu startowego, nie samego szczytu.</>}</p>
          </div>}
          {score && <div className="travel-score-detail">
            <div className="travel-score-detail-heading"><Icon name="sparkles" size={15}/> Skąd wynik {score.value}/100?
              <span>{score.partial ? `${score.unknownPoints} pkt — brak danych` : 'Pełne dane'}</span>
            </div>
            <div className="travel-score-criteria">
              {score.criteria.map(criterion => <div key={criterion.key} className="travel-score-criterion">
                <div className="travel-score-criterion-heading"><strong>{criterion.label}</strong><span>{criterion.points === null ? 'brak danych' : `${criterion.points}/${criterion.maxPoints} pkt`}</span></div>
                <div className={`travel-score-criterion-bar${criterion.points === null ? ' missing' : ''}`}>
                  {criterion.points !== null && <span style={{ width: `${criterion.maxPoints ? criterion.points / criterion.maxPoints * 100 : 0}%` }} />}
                </div>
                <p>{criterion.explanation}</p>
              </div>)}
            </div>
            {score.strengths.length > 0 && <div className="travel-score-explanations">
              {score.strengths.map((reason, i) => <span key={`plus-${i}`} className="positive"><Icon name="check" size={13}/>{reason}</span>)}
            </div>}
            {score.cautions.length > 0 && <div className="travel-score-explanations">
              {score.cautions.map((reason, i) => <span key={`warning-${i}`} className="caution"><Icon name="alert" size={13}/>{reason}</span>)}
            </div>}
            <p className="travel-score-clarification">Brak danych oznacza brak przyznanych punktów. Dla auta odległość i koszty są wyłącznie szacunkowe; dla lotów dystans jest w linii prostej. Katalog obejmuje wybrane atrakcje.</p>
          </div>}
          {isBus && <div className="bus-check-panel">
            <div className="bus-check-heading"><Icon name="ticket" size={15} /> Sprawdź obie ceny przed zakupem</div>
            <div className="bus-check-leg"><span>Wyjazd: {formatDate(trip.outboundDeparture)} o {formatTime(trip.outboundDeparture)} · {trip.origin} → {displayDestination}</span></div>
            <div className="bus-check-leg"><span>Powrót: {formatDate(trip.returnDeparture)} o {formatTime(trip.returnDeparture)} · {displayDestination} → {trip.origin}</span></div>
            <button className="bus-check-copy" type="button" onClick={copyBusDates}><Icon name={copyState === 'copied' ? 'check' : 'calendar'} size={14} /> {copyState === 'copied' ? 'Daty i godziny skopiowane' : copyState === 'failed' ? 'Kopiowanie niedostępne' : 'Kopiuj daty i godziny'}</button>
            <small>Przyciski wyżej próbują automatycznie otworzyć wyszukiwanie z miastami, datą i jednym pasażerem. Jeśli FlixBus nie udostępni identyfikatorów miast, przekierujemy do strony relacji. Wybór konkretnej godziny i przystanku wymaga potwierdzenia w wynikach FlixBusa.</small>
            {preview?.roundTripFromPln != null && <small>Reklamowe minima: tam od {formatPrice(preview.outboundFromPln!)} · powrót od {formatPrice(preview.returnFromPln!)}. Nie obowiązują automatycznie dla wskazanych dat.</small>}
          </div>}
          <div className="live-detail-note"><Icon name="info" size={14} /> {isCar ? 'Samochód: daty i godziny to propozycja. Koszty, dystans i czas są szacunkowe; sprawdź trasę, parking oraz opłaty przed wyjazdem.' : isTrain ? 'Planowy rozkład PKP PLK, nie potwierdzona rezerwacja. API nie zawiera cen biletów. Sprawdź aktualny rozkład, taryfę i dostępność u przewoźnika.' : isBus ? 'Planowy rozkład FlixBus Europe GTFS (źródło: FlixBus France, licencja ODbL 1.0). Cena i dostępność miejsc nie są podane. Przed wyjazdem potwierdź kurs i cenę na stronie przewoźnika.' : 'Historyczna oferta znaleziona przez użytkowników Aviasales. Cena, dostępność i zasady bagażowe mogą się zmienić. Sprawdź ofertę przed zakupem.'}</div>
        </div>}
        {!isBus && trip.bookingUrl && <a className="trip-booking-link" href={trip.bookingUrl} target="_blank" rel="noopener noreferrer"><span>{isCar ? 'Otwórz trasę w Google Maps' : isTrain ? 'Otwórz wyszukiwarkę PLK' : 'Sprawdź aktualną cenę'}</span><Icon name="arrow-up-right" size={17} /></a>}
        <button className="detail-button" type="button" onClick={() => setExpanded(!expanded)} aria-expanded={expanded} aria-controls={`details-${trip.id}`}><span>{expanded ? 'Ukryj szczegóły' : 'Zobacz szczegóły podróży'}</span><Icon name={expanded ? 'chevron-down' : 'arrow-up-right'} size={17} /></button>
      </div>
    </article>
  );
}
