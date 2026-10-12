import { Icon } from '../../components/Icon';
import { TripResultCard } from './TripResultCard';
import { formatDate, type TripOption, type TripSearchRequest } from './types';

export type SearchStatus = 'idle' | 'loading' | 'success' | 'error';

interface Props {
  results: TripOption[];
  status: SearchStatus;
  error: string | null;
  searchedRequest: TripSearchRequest | null;
  onRetry: () => void;
  trainAccess: 'checking' | 'pending' | 'active' | 'unavailable';
  busAccess: boolean;
}

export function TripResults({ results, status, error, searchedRequest, onRetry, trainAccess, busAccess }: Props) {
  return (
    <section className="results-panel" aria-live="polite" aria-busy={status === 'loading'}>
      <div className="results-header"><div><span className="section-overline">TWOJE MOŻLIWOŚCI</span><h2>Odkryte kierunki<span className="header-period">.</span></h2><p>{searchedRequest ? `${searchedRequest.origin} · ${searchedRequest.availableDates?.length ?? 0} dni wolnych · ${formatDate(searchedRequest.outboundDate)} – ${formatDate(searchedRequest.returnDate)}` : 'Podróże dopasowane do Twojego terminu'}</p></div><div className="results-count">{status === 'success' ? <><strong>{results.length}</strong><span>wariantów</span></> : <><Icon name="sparkles" size={18} /><span>Odkrywanie</span></>}</div></div>
      {status === 'idle' && <div className="empty-state">
        <div className="empty-art" aria-hidden="true"><div className="empty-art-circle"><Icon name="compass" size={65} strokeWidth={1} /></div><span className="empty-art-dot one" /><span className="empty-art-dot two" /><span className="empty-route-a" /><span className="empty-route-b" /><span className="empty-floating one"><Icon name="plane" size={18} /></span><span className="empty-floating two"><Icon name="train" size={18} /></span></div>
        <span className="empty-eyebrow"><Icon name="sparkles" size={14} /> ZACZNIJ OD SWOJEGO TERMINU</span><h3>Nie musisz wiedzieć,<br /><em>dokąd chcesz jechać.</em></h3><p>Wybierz dni wolne i godziny podróży. Pokażemy dostępne oferty lotów z Aviasales. {trainAccess === 'active' ? 'Kolej korzysta z rozkładów PKP PLK.' : 'Kolej czeka na dostęp do danych PKP PLK.'} {busAccess ? ' Autobusy FlixBus: planowe rozkłady GTFS.' : ''} Bez mocków.</p><div className="empty-instruction"><span>01. Ustaw termin</span><Icon name="arrow-right" size={15} /><span>02. Kliknij „Odkryj kierunki”</span></div>
      </div>}
      {status === 'loading' && <div className="skeleton-grid" aria-label="Trwa wyszukiwanie kierunków">{[0, 1, 2, 3].map(i => <div className="skeleton-card" key={i}><div className="skeleton-image skeleton-wave" /><div className="skeleton-body"><div className="skeleton-line skeleton-wave short"/><div className="skeleton-line skeleton-wave"/><div className="skeleton-line skeleton-wave medium"/></div></div>)}</div>}
      {status === 'error' && <div className="feedback-state"><div className="feedback-icon error"><Icon name="alert" size={30} /></div><h3>Nie udało się pobrać kierunków</h3><p>{error}</p><button type="button" className="retry-button" onClick={onRetry}><Icon name="refresh" size={17} /> Spróbuj ponownie</button></div>}
      {status === 'success' && results.length === 0 && <div className="feedback-state"><div className="feedback-icon"><Icon name="search" size={30} /></div><h3>Nic nie pasuje do tych kryteriów</h3><p>Nie znaleziono podróży spełniających te daty i godziny. Loty pochodzą z niepełnego cache Aviasales. {trainAccess === 'active' ? 'Pociągi korzystają z rozkładów PLK.' : 'Kolej czeka na aktywację dostępu do PLK.'} GTFS FlixBus nie obejmuje wszystkich przewoźników i przystanków. Spróbuj szerszych godzin lub innych wolnych dni.</p></div>}
      {status === 'success' && results.length > 0 && <><div className="results-grid">{results.map((trip, index) => <TripResultCard key={trip.id} trip={trip} index={index} />)}</div><div className="results-footnote"><Icon name="info" size={15} /><span>Loty: orientacyjne ceny z cache Aviasales, nie wszystkie dostępne loty. {trainAccess === 'active' ? 'Pociągi: planowy rozkład PKP PLK bez cen biletów i przesiadek.' : 'Pociągi: oczekiwanie na dostęp do PKP PLK.'} Autobusy: planowy rozkład <a href="https://transport.data.gouv.fr/datasets/flixbus-horaires-theoriques-du-reseau-europeen-1" target="_blank" rel="noopener noreferrer">FlixBus Europe GTFS</a> (ODbL 1.0), bez cen. Budżet filtruje autobusy tylko po uzyskaniu potwierdzonej ceny dla obu kursów, a pozostałych nie odrzuca. Zawsze potwierdź rozkład, cenę i dostępność przed podróżą.</span></div></>}
    </section>
  );
}
