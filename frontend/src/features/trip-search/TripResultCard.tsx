import { useState } from 'react';
import { Icon, type IconName } from '../../components/Icon';
import {
  DESTINATION_META,
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
  const meta = DESTINATION_META[trip.destination] ?? {
    subtitle: 'Odkryj nowe miejsce',
    country: 'NOWY KIERUNEK',
    image: '',
    palette: 'green',
  };
  const stayHours = Math.max(1, Math.round(trip.timeAtDestinationMinutes / 60));
  const allDirect = trip.outboundTransfers === 0 && trip.returnTransfers === 0;
  const themeClass = `transport-tone-${TRANSPORT_THEMES[trip.transportType]}`;


  return (
    <article className={`trip-card palette-${meta.palette} ${themeClass}`} style={{ animationDelay: `${index * 70}ms` }}>
      <div className="trip-cover">
        {meta.image ? <img src={meta.image} alt={trip.destination} loading="lazy" /> : <div className="trip-cover-placeholder" aria-hidden="true"><Icon name="plane" size={74} strokeWidth={1} /></div>}
        <div className="trip-cover-overlay" />
        <div className="trip-cover-meta"><span className={`transport-pill pill-${TRANSPORT_THEMES[trip.transportType]}`}><Icon name={TRANSPORT_ICONS[trip.transportType]} size={14} /> {TRANSPORT_LABELS[trip.transportType]}</span><span className="trip-badge">Aviasales · cache</span></div>
        <div className="trip-cover-content"><small>{meta.country || 'Europa'}</small><h3>{trip.destination}</h3><p>{meta.subtitle}</p></div>
      </div>

      <div className="trip-card-body">
        <div className="trip-price-row"><div><span className="micro-label">Cena orientacyjna</span><strong>{formatPrice(trip.price, trip.currency)}</strong><small>transport w obie strony</small></div></div>
        <div className="trip-route"><div className="route-stop"><span className="route-stop-top">WYJAZD</span><strong>{formatTime(trip.outboundDeparture)}</strong><span>{formatWeekday(trip.outboundDeparture)}, {formatDate(trip.outboundDeparture)}</span></div><div className="route-mid"><div className={`route-line route-line-${TRANSPORT_THEMES[trip.transportType]}`}><span /><Icon name={TRANSPORT_ICONS[trip.transportType]} size={17} /><span /></div><small>{trip.origin}</small></div><div className="route-stop route-stop-end"><span className="route-stop-top">POWRÓT</span><strong>{formatTime(trip.returnArrival)}</strong><span>{formatWeekday(trip.returnArrival)}, {formatDate(trip.returnArrival)}</span></div></div>
        <div className="trip-facts"><span className="stay-highlight"><Icon name="hourglass" size={15} /> <strong>{stayHours} godz.</strong> na miejscu</span><span className={`trip-fact trip-fact-${TRANSPORT_THEMES[trip.transportType]}`}><Icon name={allDirect ? 'check' : 'route'} size={15} /> {allDirect ? 'Bez przesiadek' : 'Z przesiadką'}</span></div>
        {expanded && <div className="trip-details" id={`details-${trip.id}`}>
          <div className="detail-line"><span>Podróż tam</span><strong>{formatTime(trip.outboundDeparture)} – {formatTime(trip.outboundArrival)}</strong></div>
          <div className="detail-line"><span>Podróż z powrotem</span><strong>{formatTime(trip.returnDeparture)} – {formatTime(trip.returnArrival)}</strong></div>
          <div className="detail-line"><span>Łącznie w podróży</span><strong>{durationLabel(trip.totalTravelDurationMinutes)}</strong></div>
          <div className="detail-line"><span>Przesiadki tam / powrót</span><strong>{trip.outboundTransfers} / {trip.returnTransfers}</strong></div>
          <div className="live-detail-note"><Icon name="info" size={14} /> Historyczna oferta znaleziona przez użytkowników Aviasales. Cena, dostępność i zasady bagażowe mogą się zmienić. Kliknij poniżej, aby sprawdzić bieżącą ofertę przed zakupem.</div>
        </div>}
        {trip.bookingUrl && <a className="trip-booking-link" href={trip.bookingUrl} target="_blank" rel="noopener noreferrer"><span>Sprawdź aktualną cenę</span><Icon name="arrow-up-right" size={17} /></a>}
        <button className="detail-button" type="button" onClick={() => setExpanded(!expanded)} aria-expanded={expanded} aria-controls={`details-${trip.id}`}><span>{expanded ? 'Ukryj szczegóły' : 'Zobacz szczegóły podróży'}</span><Icon name={expanded ? 'chevron-down' : 'arrow-up-right'} size={17} /></button>
      </div>
    </article>
  );
}
