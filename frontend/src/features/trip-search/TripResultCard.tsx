import { useEffect, useMemo, useRef, useState } from 'react';
import { Icon, type IconName } from '../../components/Icon';
import { resolveDestinationMeta } from './destinationVisuals';
import { flixBusRoutePage, flixBusDateSummary } from './flixBusBooking';
import { fetchFlixBusPreview, type FlixBusPublicPreview } from './flixBusPreview';
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

export function TripResultCard({ trip, index }: { trip: TripOption; index: number }) {
  const [expanded, setExpanded] = useState(false);
  const cardRef = useRef<HTMLElement>(null);
  const [preview, setPreview] = useState<FlixBusPublicPreview | null>(null);
  const [previewStarted, setPreviewStarted] = useState(false);
  const [previewFailed, setPreviewFailed] = useState(false);
  const [copyState, setCopyState] = useState<'idle' | 'copied' | 'failed'>('idle');
  const meta = useMemo(
    () => resolveDestinationMeta(trip.destination, trip.transportType),
    [trip.destination, trip.transportType],
  );
  const isTrain = trip.transportType === 'TRAIN';
  const isBus = trip.transportType === 'BUS';
  const isSchedule = isTrain || isBus;
  const stayHours = Math.max(1, Math.round(trip.timeAtDestinationMinutes / 60));
  const allDirect = trip.outboundTransfers === 0 && trip.returnTransfers === 0;
  const themeClass = `transport-tone-${TRANSPORT_THEMES[trip.transportType]}`;
  const displayDestination = meta.displayName || trip.destination;
  const busOutboundUrl = isBus ? flixBusRoutePage(trip.origin, trip.destination) : '';
  const busReturnUrl = isBus ? (preview?.returnSearchUrl ?? flixBusRoutePage(trip.destination, trip.origin)) : '';
  const busOutboundDeepUrl = isBus ? (preview?.outboundSearchUrl ?? busOutboundUrl) : '';
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
        {meta.image ? (
          <img src={meta.image} alt={`${displayDestination} – ${meta.landmark}, wizualizacja fotograficzna`} loading="lazy" />
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
          <span className="trip-badge">{isTrain ? 'PKP PLK · rozkład' : isBus ? 'FlixBus · GTFS' : 'Aviasales · cache'}</span>
        </div>
        <div className="trip-cover-content">
          <small>{meta.country || 'Europa'}</small>
          <h3>{displayDestination}</h3>
          <p>{meta.subtitle}</p>
          {meta.landmark && <span className="trip-landmark-badge">
            <Icon name="sparkles" size={13} /> {meta.landmark}
          </span>}
        </div>
      </div>

      <div className="trip-card-body">
        <div className="trip-price-row">
          <div>
            <span className="micro-label">{isBus && trip.price === null ? 'Ceny publikowane na stronach tras' : isSchedule ? 'Cena biletu' : 'Cena orientacyjna'}</span>
            {isBus && trip.price === null ? (
              <strong className={preview?.roundTripFromPln != null ? 'bus-indicative-price' : 'train-unknown-price'}>
                {preview?.roundTripFromPln != null ? `od ${formatPrice(preview.roundTripFromPln)}` : previewStarted && !preview && !previewFailed ? 'Sprawdzam…' : 'Do sprawdzenia'}
              </strong>
            ) : <strong className={isSchedule ? 'train-unknown-price' : ''}>{trip.price === null ? 'Do sprawdzenia' : formatPrice(trip.price, trip.currency)}</strong>}
            <small>{isBus ? (trip.price !== null ? 'Cena partnerska za podróż w obie strony' : preview?.roundTripFromPln != null ? 'Suma reklamowanych minimów tam + powrót; NIE cena tych kursów' : 'Brak potwierdzonej ceny dla tych terminów') : isTrain ? 'u przewoźnika · brak ceny w PLK' : 'transport w obie strony'}</small>
          </div>
        </div>
        <div className="trip-route">
          <div className="route-stop">
            <span className="route-stop-top">{isSchedule ? 'ODJAZD Z DOMU' : 'WYLOT Z DOMU'}</span>
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
            <span className="route-stop-top">{isSchedule ? 'ODJAZD POWROTNY' : 'WYLOT POWROTNY'}</span>
            <strong>{formatTime(trip.returnDeparture)}</strong>
            <span>{formatWeekday(trip.returnDeparture)}, {formatDate(trip.returnDeparture)}</span>
          </div>
        </div>
        <div className="trip-return-arrival">
          <Icon name="clock" size={13} />
          <span>{isSchedule ? 'Przyjazd' : 'Przylot'} do {trip.origin}: <strong>{formatTime(trip.returnArrival)}</strong> · {formatWeekday(trip.returnArrival)}, {formatDate(trip.returnArrival)}</span>
        </div>
        <div className="trip-facts">
          <span className="stay-highlight"><Icon name="hourglass" size={15} /> <strong>{stayHours} godz.</strong> na miejscu</span>
          <span className={`trip-fact trip-fact-${TRANSPORT_THEMES[trip.transportType]}`}><Icon name={allDirect ? 'check' : 'route'} size={15} /> {allDirect ? 'Bez przesiadek' : 'Z przesiadką'}</span>
        </div>
        {expanded && <div className="trip-details" id={`details-${trip.id}`}>
          <div className="detail-line"><span>Podróż tam</span><strong>{formatTime(trip.outboundDeparture)} – {formatTime(trip.outboundArrival)}</strong></div>
          <div className="detail-line"><span>Powrót ({displayDestination} → {trip.origin})</span><strong>{formatTime(trip.returnDeparture)} – {formatTime(trip.returnArrival)}</strong></div>
          <div className="detail-line"><span>Łącznie w podróży</span><strong>{durationLabel(trip.totalTravelDurationMinutes)}</strong></div>
          {meta.landmark && <div className="detail-line"><span>Znane miejsce na karcie</span><strong>{meta.landmark}</strong></div>}
          {trip.destination !== displayDestination && <div className="detail-line"><span>Przystanek docelowy w źródle</span><strong>{trip.destination}</strong></div>}
          {meta.image && <div className="live-detail-note"><Icon name="info" size={14} /> Obraz na karcie jest fotorealistyczną wizualizacją AI inspirowaną miejscem, a nie dokumentalną fotografią.</div>}
          <div className="detail-line"><span>Przesiadki tam / powrót</span><strong>{trip.outboundTransfers} / {trip.returnTransfers}</strong></div>
          {isBus && <div className="bus-check-panel">
            <div className="bus-check-heading"><Icon name="ticket" size={15} /> Sprawdź obie ceny przed zakupem</div>
            <div className="bus-check-leg"><span>Tam: {formatDate(trip.outboundDeparture)} o {formatTime(trip.outboundDeparture)}</span><a href={busOutboundDeepUrl} target="_blank" rel="noopener noreferrer">{trip.origin} → {displayDestination} <Icon name="arrow-up-right" size={13} /></a></div>
            <div className="bus-check-leg"><span>Powrót: {formatDate(trip.returnDeparture)} o {formatTime(trip.returnDeparture)}</span><a href={busReturnUrl} target="_blank" rel="noopener noreferrer">{displayDestination} → {trip.origin} <Icon name="arrow-up-right" size={13} /></a></div>
            <button className="bus-check-copy" type="button" onClick={copyBusDates}><Icon name={copyState === 'copied' ? 'check' : 'calendar'} size={14} /> {copyState === 'copied' ? 'Daty i godziny skopiowane' : copyState === 'failed' ? 'Kopiowanie niedostępne' : 'Kopiuj daty i godziny'}</button>
            <small>{preview?.searchPrefilled
              ? 'Linki wyszukiwania zawierają miasta, datę i 1 pasażera. FlixBus może zmienić sposób obsługi linków; przed zakupem sprawdź przystanki, godzinę i końcową cenę.'
              : 'Nie udało się potwierdzić identyfikatorów miast do automatycznego wyszukiwania. Link prowadzi na stronę relacji; daty pozostają dostępne do skopiowania.'}</small>
            {preview?.roundTripFromPln != null && <small>Reklamowe minima: tam od {formatPrice(preview.outboundFromPln!)} · powrót od {formatPrice(preview.returnFromPln!)}. Nie obowiązują automatycznie dla wskazanych dat.</small>}
          </div>}
          <div className="live-detail-note"><Icon name="info" size={14} /> {isTrain ? 'Planowy rozkład PKP PLK, nie potwierdzona rezerwacja. API nie zawiera cen biletów. Sprawdź aktualny rozkład, taryfę i dostępność u przewoźnika.' : isBus ? 'Planowy rozkład FlixBus Europe GTFS (źródło: FlixBus France, licencja ODbL 1.0). Cena i dostępność miejsc nie są podane. Przed wyjazdem potwierdź kurs i cenę na stronie przewoźnika.' : 'Historyczna oferta znaleziona przez użytkowników Aviasales. Cena, dostępność i zasady bagażowe mogą się zmienić. Sprawdź ofertę przed zakupem.'}</div>
        </div>}
        {(trip.bookingUrl || isBus) && <a className="trip-booking-link" href={isBus ? busOutboundDeepUrl : trip.bookingUrl!} target="_blank" rel="noopener noreferrer"><span>{isTrain ? 'Otwórz wyszukiwarkę PLK' : isBus ? (preview?.searchPrefilled ? 'Sprawdź kurs — trasa i data wypełnione' : 'Sprawdź we FlixBusie') : 'Sprawdź aktualną cenę'}</span><Icon name="arrow-up-right" size={17} /></a>}
        <button className="detail-button" type="button" onClick={() => setExpanded(!expanded)} aria-expanded={expanded} aria-controls={`details-${trip.id}`}><span>{expanded ? 'Ukryj szczegóły' : 'Zobacz szczegóły podróży'}</span><Icon name={expanded ? 'chevron-down' : 'arrow-up-right'} size={17} /></button>
      </div>
    </article>
  );
}
