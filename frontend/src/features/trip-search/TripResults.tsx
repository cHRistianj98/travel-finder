import { Icon } from '../../components/Icon';
import { TripResultCard } from './TripResultCard';
import { formatDate, type TripOption, type TripSearchRequest } from './types';
import { SCORE_PROFILES, sortTrips, type ScoredTrip, type TravelScoreProfile, type TripSort } from './travelScore';

export type SearchStatus = 'idle' | 'loading' | 'success' | 'error';

interface Props {
  results: TripOption[];
  scores: ScoredTrip[];
  status: SearchStatus;
  error: string | null;
  searchedRequest: TripSearchRequest | null;
  onRetry: () => void;
  profile: TravelScoreProfile;
  onProfileChange: (profile: TravelScoreProfile) => void;
  sort: TripSort;
  onSortChange: (sort: TripSort) => void;
  scoreBusy: boolean;
  scoreError: string | null;
  trainAccess: 'checking' | 'pending' | 'active' | 'unavailable';
  busAccess: boolean;
}

export function TripResults({ results, scores, status, error, searchedRequest, onRetry,
  trainAccess, busAccess, profile, onProfileChange, sort, onSortChange, scoreBusy, scoreError }: Props) {
  const ranked = sortTrips(results, scores, sort);
  return (
    <section className="results-panel" aria-live="polite" aria-busy={status === 'loading'}>
      <div className="results-header"><div><span className="section-overline">TWOJE MOŻLIWOŚCI</span><h2>Odkryte kierunki<span className="header-period">.</span></h2><p>{searchedRequest ? `${searchedRequest.origin} · ${searchedRequest.availableDates?.length ?? 0} dni wolnych · ${formatDate(searchedRequest.outboundDate)} – ${formatDate(searchedRequest.returnDate)}` : 'Podróże dopasowane do Twojego terminu'}</p></div><div className="results-count">{status === 'success' ? <><strong>{results.length}</strong><span>wariantów</span></> : <><Icon name="sparkles" size={18} /><span>Odkrywanie</span></>}</div></div>
      {status === 'idle' && <div className="empty-state">
        <div className="empty-art" aria-hidden="true"><div className="empty-art-circle"><Icon name="compass" size={65} strokeWidth={1} /></div><span className="empty-art-dot one" /><span className="empty-art-dot two" /><span className="empty-route-a" /><span className="empty-route-b" /><span className="empty-floating one"><Icon name="plane" size={18} /></span><span className="empty-floating two"><Icon name="train" size={18} /></span></div>
        <span className="empty-eyebrow"><Icon name="sparkles" size={14} /> ZACZNIJ OD SWOJEGO TERMINU</span><h3>Nie musisz wiedzieć,<br /><em>dokąd chcesz jechać.</em></h3><p>Wybierz dni wolne i godziny podróży. Pokażemy dostępne oferty lotów z Aviasales. {trainAccess === 'active' ? 'Kolej korzysta z rozkładów PKP PLK.' : 'Kolej czeka na dostęp do danych PKP PLK.'} {busAccess ? ' Autobusy FlixBus: planowe rozkłady GTFS.' : ''} Ranking Travel Score pozwoli wybrać najlepszy wyjazd.</p><div className="empty-instruction"><span>01. Ustaw termin</span><Icon name="arrow-right" size={15} /><span>02. Odkryj kierunki</span></div>
      </div>}
      {status === 'loading' && <div className="skeleton-grid" aria-label="Trwa wyszukiwanie kierunków">{[0, 1, 2, 3].map(i => <div className="skeleton-card" key={i}><div className="skeleton-image skeleton-wave" /><div className="skeleton-body"><div className="skeleton-line skeleton-wave short"/><div className="skeleton-line skeleton-wave"/><div className="skeleton-line skeleton-wave medium"/></div></div>)}</div>}
      {status === 'error' && <div className="feedback-state"><div className="feedback-icon error"><Icon name="alert" size={30} /></div><h3>Nie udało się pobrać kierunków</h3><p>{error}</p><button type="button" className="retry-button" onClick={onRetry}><Icon name="refresh" size={17} /> Spróbuj ponownie</button></div>}
      {status === 'success' && results.length === 0 && <div className="feedback-state"><div className="feedback-icon"><Icon name="search" size={30} /></div><h3>Nic nie pasuje do tych kryteriów</h3><p>Nie znaleziono podróży spełniających daty i godziny. Loty pochodzą z niepełnego cache Aviasales. {trainAccess === 'active' ? 'Pociągi korzystają z rozkładów PLK.' : 'Kolej czeka na dostęp do PLK.'} GTFS FlixBus nie obejmuje wszystkich przewoźników. Spróbuj szerszych godzin lub innych wolnych dni.</p></div>}
      {status === 'success' && results.length > 0 && <>
        <div className="travel-score-toolbar">
          <div className="travel-score-toolbar-heading">
            <span className="travel-score-kicker"><Icon name="sparkles" size={14}/> INTELIGENTNY RANKING</span>
            <h3>Travel Score <span>0–100</span></h3>
            <p>Wybierz, na czym najbardziej Ci zależy. Punkty zostaną przeliczone bez ponownego wyszukiwania.</p>
          </div>
          <div className="travel-score-profiles" role="group" aria-label="Priorytet oceniania podróży">
            {SCORE_PROFILES.map(item => <button key={item.id} type="button" className={`travel-score-profile${profile === item.id ? ' selected' : ''}`} title={item.description} aria-pressed={profile === item.id} onClick={() => onProfileChange(item.id)}>
              <Icon name={item.icon} size={16}/><span>{item.label}</span>
            </button>)}
          </div>
          <div className="travel-score-sortbar">
            <label htmlFor="trip-results-sort"><Icon name="sliders" size={15} /> Sortuj</label>
            <select id="trip-results-sort" value={sort} onChange={event => onSortChange(event.target.value as TripSort)}>
              <option value="SCORE">Najlepszy Travel Score</option>
              <option value="PRICE">Najniższa znana cena</option>
              <option value="STAY">Najwięcej czasu na miejscu</option>
              <option value="DURATION">Najkrótsza podróż</option>
            </select>
            {scoreBusy && <span className="travel-score-calculating"><Icon name="refresh" size={13}/> Przeliczam…</span>}
          </div>
          {scoreError && <p className="travel-score-error" role="status"><Icon name="alert" size={14}/>{scoreError}</p>}
          <p className="travel-score-method">Brak ceny konkretnego kursu lub brak miejsca w katalogu atrakcji = brak punktów za tę kategorię, a nie wymyślona ocena. Zobacz szczegóły punktacji na karcie.</p>
        </div>
        <div className="results-grid">{ranked.map(({trip, score}, index) => <TripResultCard key={trip.id} trip={trip} index={index} score={score} />)}</div>
        <div className="results-footnote"><Icon name="info" size={15} /><span>Travel Score to pomocniczy ranking oparty na rozkładzie, parametrach wyszukiwania i lokalnym katalogu atrakcji. Odległość mierzona jest między centrami miejscowości, bez transferów lotniskowych. Cena lotu pochodzi z historycznego cache; reklamowana cena minimalna FlixBusa NIE jest ceną konkretnego kursu i nie jest punktowana. {trainAccess === 'active' ? 'Pociągi: rozkład PLK bez cen.' : 'Pociągi: oczekiwanie na PLK.'} Autobusy: planowy rozkład <a href="https://transport.data.gouv.fr/datasets/flixbus-horaires-theoriques-du-reseau-europeen-1" target="_blank" rel="noopener noreferrer">FlixBus Europe GTFS</a> (ODbL 1.0). Potwierdź taryfę, przystanek i rozkład przed zakupem.</span></div>
      </>}
    </section>
  );
}
