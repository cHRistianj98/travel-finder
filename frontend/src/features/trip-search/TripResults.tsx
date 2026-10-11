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
}

export function TripResults({ results, status, error, searchedRequest, onRetry }: Props) {
  return (
    <section className="results-panel" aria-live="polite" aria-busy={status === 'loading'}>
      <div className="results-header"><div><span className="section-overline">TWOJE MOŻLIWOŚCI</span><h2>Odkryte kierunki<span className="header-period">.</span></h2><p>{searchedRequest ? `${searchedRequest.origin} · ${searchedRequest.availableDates?.length ?? 0} dni wolnych · ${formatDate(searchedRequest.outboundDate)} – ${formatDate(searchedRequest.returnDate)}` : 'Podróże dopasowane do Twojego terminu'}</p></div><div className="results-count">{status === 'success' ? <><strong>{results.length}</strong><span>wariantów</span></> : <><Icon name="sparkles" size={18} /><span>Odkrywanie</span></>}</div></div>
      {status === 'idle' && <div className="empty-state">
        <div className="empty-art" aria-hidden="true"><div className="empty-art-circle"><Icon name="compass" size={65} strokeWidth={1} /></div><span className="empty-art-dot one" /><span className="empty-art-dot two" /><span className="empty-route-a" /><span className="empty-route-b" /><span className="empty-floating one"><Icon name="plane" size={18} /></span><span className="empty-floating two"><Icon name="train" size={18} /></span></div>
        <span className="empty-eyebrow"><Icon name="sparkles" size={14} /> ZACZNIJ OD SWOJEGO TERMINU</span><h3>Nie musisz wiedzieć,<br /><em>dokąd chcesz jechać.</em></h3><p>Wybierz daty, godziny i budżet. Pokażemy pasujące loty z cache Aviasales — z rzeczywistymi godzinami i orientacyjnymi cenami, bez mocków.</p><div className="empty-instruction"><span>01. Ustaw termin</span><Icon name="arrow-right" size={15} /><span>02. Kliknij „Odkryj kierunki”</span></div>
      </div>}
      {status === 'loading' && <div className="skeleton-grid" aria-label="Trwa wyszukiwanie kierunków">{[0, 1, 2, 3].map(i => <div className="skeleton-card" key={i}><div className="skeleton-image skeleton-wave" /><div className="skeleton-body"><div className="skeleton-line skeleton-wave short"/><div className="skeleton-line skeleton-wave"/><div className="skeleton-line skeleton-wave medium"/></div></div>)}</div>}
      {status === 'error' && <div className="feedback-state"><div className="feedback-icon error"><Icon name="alert" size={30} /></div><h3>Nie udało się pobrać kierunków</h3><p>{error}</p><button type="button" className="retry-button" onClick={onRetry}><Icon name="refresh" size={17} /> Spróbuj ponownie</button></div>}
      {status === 'success' && results.length === 0 && <div className="feedback-state"><div className="feedback-icon"><Icon name="search" size={30} /></div><h3>Nic nie pasuje do tych kryteriów</h3><p>W cache Aviasales nie ma oferty spełniającej podane daty, godziny i budżet. To nie znaczy, że takich lotów nie ma — baza obejmuje tylko wcześniej wyszukiwane oferty. Spróbuj innych godzin, większego budżetu albo innego terminu.</p></div>}
      {status === 'success' && results.length > 0 && <><div className="results-grid">{results.map((trip, index) => <TripResultCard key={trip.id} trip={trip} index={index} />)}</div><div className="results-footnote"><Icon name="info" size={15} /><span>Oferty pochodzą z pamięci podręcznej Aviasales/Travelpayouts i mogą być nieaktualne. Podane ceny są wyłącznie orientacyjne. Ostateczną cenę, bagaż i dostępność sprawdzaj przed zakupem. Wyniki nie obejmują wszystkich istniejących lotów. Filtry działają na danych zapisanych po stronie backendu.</span></div></>}
    </section>
  );
}
