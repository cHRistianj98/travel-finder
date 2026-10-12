import { useEffect, useId, useRef, useState, type FormEvent } from 'react';
import { DayPicker } from 'react-day-picker';
import { Icon, type IconName } from '../../components/Icon';
import {
  POLISH_CITIES,
  formatDateLong,
  nextWeekend,
  type TransportType,
  type TripSearchRequest,
} from './types';
import 'react-day-picker/style.css';

const TRANSPORTS: { type: TransportType; icon: IconName; title: string; subtitle: string; accent: string }[] = [
  { type: 'FLIGHT', icon: 'plane', title: 'Samolot', subtitle: 'Szybko i daleko', accent: 'flight' },
  { type: 'TRAIN', icon: 'train', title: 'Pociąg', subtitle: 'Rozkład PKP PLK', accent: 'train' },
  { type: 'BUS', icon: 'bus', title: 'Autobus', subtitle: 'Wkrótce', accent: 'bus' },
  { type: 'CAR', icon: 'car', title: 'Samochód', subtitle: 'Wkrótce', accent: 'car' },
];

const TIME_OPTIONS = createTimeOptions();
const DEFAULT_WEEKEND = nextWeekend();

interface Props {
  onSearch: (request: TripSearchRequest) => void;
  loading: boolean;
  trainAccess: 'checking' | 'pending' | 'active' | 'unavailable';
}

export function TripSearchForm({ onSearch, loading, trainAccess }: Props) {
  const [form, setForm] = useState<TripSearchRequest>(() => ({
    origin: 'Wrocław',
    outboundDate: DEFAULT_WEEKEND.outboundDate,
    returnDate: DEFAULT_WEEKEND.returnDate,
    availableDates: datesBetween(DEFAULT_WEEKEND.outboundDate, DEFAULT_WEEKEND.returnDate),
    earliestOutboundDeparture: '15:00',
    latestOutboundDeparture: '23:00',
    earliestReturnDeparture: '15:00',
    latestReturnDeparture: '23:00',
    maxTransportBudget: 500,
    currency: 'PLN',
    transportTypes: ['FLIGHT'],
    directOnly: true,
  }));
  const [validationError, setValidationError] = useState<string | null>(null);
  const freeDays = form.availableDates.length;
  const datePairs = countPossiblePairs(form.availableDates);
  const spanDays = daysInclusive(form.outboundDate, form.returnDate);

  function update<K extends keyof TripSearchRequest>(key: K, value: TripSearchRequest[K]) {
    setForm(previous => ({ ...previous, [key]: value }));
    setValidationError(null);
  }

  function setWideSearch() {
    setForm(previous => ({
      ...previous,
      earliestOutboundDeparture: '00:00',
      latestOutboundDeparture: '23:30',
      earliestReturnDeparture: '00:00',
      latestReturnDeparture: '23:30',
      maxTransportBudget: 2000,
      directOnly: false,
      transportTypes: previous.transportTypes,
    }));
    setValidationError(null);
  }

  function handleAvailabilityChange(days: string[]) {
    const selected = [...new Set(days)].sort();
    setForm(previous => ({
      ...previous,
      availableDates: selected,
      outboundDate: selected[0] ?? previous.outboundDate,
      returnDate: selected[selected.length - 1] ?? previous.returnDate,
    }));
    setValidationError(null);
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!form.origin.trim()) {
      setValidationError('Wybierz miasto, z którego chcesz wyjechać.');
      return;
    }
    if (datePairs === 0) {
      setValidationError('Zaznacz co najmniej dwa następujące po sobie wolne dni — bez łączenia osobnych weekendów.');
      return;
    }
    if (spanDays > 62) {
      setValidationError('W jednym wyszukiwaniu możesz wybrać dni w obrębie maksymalnie 62 dni kalendarzowych.');
      return;
    }
    if (!form.transportTypes.length) {
      setValidationError('Wybierz przynajmniej jeden środek transportu.');
      return;
    }
    if (!Number.isFinite(form.maxTransportBudget) || form.maxTransportBudget <= 0) {
      setValidationError('Podaj budżet większy od zera.');
      return;
    }
    if (form.earliestOutboundDeparture > form.latestOutboundDeparture) {
      setValidationError('Godzina końca wyjazdu musi być późniejsza niż jego początek.');
      return;
    }
    if (form.earliestReturnDeparture > form.latestReturnDeparture) {
      setValidationError('Koniec przedziału wyjazdu powrotnego nie może być wcześniejszy niż początek.');
      return;
    }

    onSearch({ ...form, origin: form.origin.trim() });
  }

  return (
    <form className="search-card" id="wyszukiwarka" onSubmit={handleSubmit}>
      <div className="form-topline"><span><Icon name="sliders" size={16} /> TWOJE KRYTERIA</span><span className="form-step">01 — 03</span></div>
      <div className="form-header"><div><h2>Znajdź swój wyjazd</h2><p>Od wolnego terminu do gotowego pomysłu.</p></div><span className="form-header-icon"><Icon name="compass" size={24} /></span></div>

      <div className="field-section">
        <div className="section-label"><span>01</span> Skąd i kiedy?</div>
        <label className="field-label">Miasto wyjazdu</label>
        <DropdownSelect
          icon="map-pin"
          value={form.origin}
          options={POLISH_CITIES.map(city => ({ value: city, label: city }))}
          onChange={value => update('origin', value)}
          ariaLabel="Wybierz miasto wyjazdu"
        />

        <label className="field-label">Kiedy masz wolne?</label>
        <AvailabilityDateField selected={form.availableDates} onChange={handleAvailabilityChange} />
        <p className="flight-search-helper">Zaznacz pojedyncze dni, kilka weekendów lub cały miesiąc. Wyjazd i powrót muszą mieścić się w jednym nieprzerwanym okresie wolnym.</p>
        <div className="availability-hint" aria-live="polite">
          <Icon name="calendar" size={16} />
          <span>{spanDays > 62
            ? `Wybrany okres obejmuje ${spanDays} dni — maksimum to 62 dni kalendarzowe.`
            : datePairs > 0
              ? `${freeDays} dni zaznaczonych · ${datePairs} ${datePairs === 1 ? 'możliwa para dat' : 'możliwych par dat'} podróży`
              : 'Zaznacz co najmniej dwa kolejne wolne dni.'}</span>
        </div>
        <p className="availability-cost-note">Loty: orientacyjne ceny z cache Aviasales. {trainAccess === 'active' ? 'Kolej: planowe rozkłady PKP PLK bez cen biletów.' : 'Kolej: oczekiwanie na aktywację PKP PLK.'} Filtry działają lokalnie; SearchApi pozostaje wyłączone.</p>
      </div>

      <div className="field-section divider-top">
        <div className="section-label"><span>02</span> Twój czas i budżet</div>
        <div className="time-range-grid">
          <div className="time-block">
            <div className="time-block-header">
              <div>
                <strong>Wyjazd</strong>
                <span>Godziny odjazdu lub odlotu</span>
              </div>
              <Icon name="clock" size={18} />
            </div>
            <div className="input-pair compact roomy-pair">
              <div>
                <label className="field-label subtle-label">Od</label>
                <DropdownSelect
                  icon="clock"
                  value={form.earliestOutboundDeparture}
                  options={TIME_OPTIONS}
                  onChange={value => update('earliestOutboundDeparture', value)}
                  ariaLabel="Najwcześniejsza godzina wyjazdu"
                />
              </div>
              <div>
                <label className="field-label subtle-label">Do</label>
                <DropdownSelect
                  icon="clock"
                  value={form.latestOutboundDeparture}
                  options={TIME_OPTIONS}
                  onChange={value => update('latestOutboundDeparture', value)}
                  ariaLabel="Najpóźniejsza godzina wyjazdu"
                />
              </div>
            </div>
          </div>
          <div className="time-block">
            <div className="time-block-header">
              <div>
                <strong>Wyjazd powrotny</strong>
                <span>Godziny odjazdu lub odlotu z celu podróży. Przyjazd do domu musi zmieścić się w dniach wolnych.</span>
              </div>
              <Icon name="hourglass" size={18} />
            </div>
            <div className="input-pair compact roomy-pair">
              <div>
                <label className="field-label subtle-label">Od</label>
                <DropdownSelect
                  icon="clock"
                  value={form.earliestReturnDeparture}
                  options={TIME_OPTIONS}
                  onChange={value => update('earliestReturnDeparture', value)}
                  ariaLabel="Najwcześniejsza godzina wyjazdu powrotnego z miejsca pobytu"
                />
              </div>
              <div>
                <label className="field-label subtle-label">Do</label>
                <DropdownSelect
                  icon="clock"
                  value={form.latestReturnDeparture}
                  options={TIME_OPTIONS}
                  onChange={value => update('latestReturnDeparture', value)}
                  ariaLabel="Najpóźniejsza godzina wyjazdu powrotnego z miejsca pobytu"
                />
              </div>
            </div>
          </div>
        </div>

        <div className="flight-test-hint">
          <span><Icon name="info" size={15} /> Brak wyników? Na próbę poluzuj filtry.</span>
          <button type="button" className="flight-wide-button" onClick={setWideSearch}>Ustaw szerokie wyszukiwanie</button>
        </div>
        <label className="field-label" htmlFor="budget">Maksymalny budżet na transport</label>
        {form.transportTypes.includes('TRAIN') && <p className="train-budget-hint"><Icon name="info" size={14} /> PLK nie podaje cen biletów — budżet będzie sprawdzany tylko dla lotów. Koszt pociągu sprawdzisz u przewoźnika.</p>}
        <div className="input-shell budget-shell">
          <Icon name="wallet" size={19} />
          <input id="budget" type="number" min="1" step="1" value={form.maxTransportBudget} onChange={e => update('maxTransportBudget', Number(e.target.value))} required />
          <span className="currency-badge">PLN</span>
        </div>
      </div>

      <div className="field-section divider-top">
        <div className="section-label"><span>03</span> Jak chcesz podróżować?</div>
        <div className="transport-grid" role="group" aria-label="Środki transportu">
          {TRANSPORTS.map(transport => {
            const selected = form.transportTypes.includes(transport.type);
            return (
              <button
                key={transport.type}
                type="button"
                className={`transport-choice transport-${transport.accent} ${selected ? 'selected' : ''} ${transport.type === 'BUS' || transport.type === 'CAR' ? 'coming-soon' : ''} ${transport.type === 'TRAIN' && trainAccess !== 'active' ? 'waiting-access' : ''}`}
                aria-pressed={selected}
                disabled={transport.type === 'BUS' || transport.type === 'CAR' || (transport.type === 'TRAIN' && trainAccess !== 'active')}
                onClick={() => update('transportTypes', selected
                  ? form.transportTypes.filter(mode => mode !== transport.type)
                  : [...form.transportTypes, transport.type])}
                title={transport.type === 'TRAIN' ? (trainAccess === 'active' ? 'Planowane rozkłady PKP PLK' : 'Oczekiwanie na aktywację dostępu PKP PLK') :
                  transport.type === 'FLIGHT' ? 'Historyczne oferty lotnicze z Aviasales' : 'Ten transport dodamy później'}
              >
                <span className="transport-icon"><Icon name={transport.icon} size={20} /></span>
                <span className="transport-name"><strong>{transport.title}</strong><small>{transport.type === 'TRAIN' && trainAccess !== 'active'
                  ? trainAccess === 'checking' ? 'Sprawdzam status…' : 'Oczekiwanie na dostęp'
                  : transport.subtitle}</small></span>
                <span className="transport-checkbox">{selected && <Icon name="check" size={11} strokeWidth={3} />}</span>
              </button>
            );
          })}
        </div>
        {trainAccess !== 'active' && <div className="plk-waiting-notice" role="status">
          <span className="plk-waiting-icon"><Icon name="hourglass" size={20} /></span>
          <div>
            <strong>{trainAccess === 'checking' ? 'Sprawdzam dostęp do PKP PLK…' : 'Kolej — oczekiwanie na dostęp do danych PLK'}</strong>
            <p>{trainAccess === 'unavailable'
              ? 'Nie udało się sprawdzić statusu integracji. Uruchom backend i odśwież stronę.'
              : trainAccess === 'checking'
                ? 'Sprawdzam konfigurację backendu. Wyszukiwanie lotów pozostaje dostępne.'
                : 'Klucz API czeka na zatwierdzenie administratora (zwykle 3–5 dni roboczych). Do tego czasu możesz wyszukiwać loty.'}</p>
          </div>
        </div>}
        <div className="direct-row"><div className="direct-text"><strong>Tylko bezpośrednie</strong><span>Bez przesiadek po drodze</span></div><button type="button" role="switch" aria-label="Tylko połączenia bezpośrednie" aria-checked={form.directOnly} className={`switch ${form.directOnly ? 'on' : ''}`} onClick={() => update('directOnly', !form.directOnly)}><span /></button></div>
      </div>

      {validationError && <p className="form-error" role="alert"><Icon name="alert" size={16} />{validationError}</p>}
      <button className="submit-button" type="submit" disabled={loading}>{loading ? <><span className="button-spinner" />Szukam kierunków...</> : <><Icon name="search" size={20} /> Odkryj kierunki <Icon name="arrow-right" size={20} className="submit-arrow" /></>}</button>
      <div className="form-disclaimer"><Icon name="info" size={14} /> Loty: orientacyjne ceny z cache Aviasales. {trainAccess === 'active' ? 'Pociągi: planowe rozkłady PKP PLK bez cen biletów.' : 'Pociągi: oczekiwanie na aktywację dostępu PKP PLK.'} Zawsze potwierdź rozkład, dostępność i cenę przed podróżą.</div>
    </form>
  );
}

function DropdownSelect({
  value,
  options,
  onChange,
  icon,
  ariaLabel,
}: {
  value: string;
  options: { value: string; label: string }[];
  onChange: (value: string) => void;
  icon: IconName;
  ariaLabel: string;
}) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement | null>(null);
  const buttonId = useId();

  useEffect(() => {
    function handlePointerDown(event: MouseEvent) {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handlePointerDown);
    return () => document.removeEventListener('mousedown', handlePointerDown);
  }, []);

  const selected = options.find(option => option.value === value) ?? options[0];

  return (
    <div className={`dropdown-field ${open ? 'open' : ''}`} ref={rootRef}>
      <button
        id={buttonId}
        type="button"
        className="dropdown-trigger input-shell"
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-label={ariaLabel}
        onClick={() => setOpen(previous => !previous)}
      >
        <Icon name={icon} size={18} />
        <span className="dropdown-trigger-label">{selected.label}</span>
        <Icon name="chevron-down" size={18} className="dropdown-chevron" />
      </button>
      {open && (
        <div className="dropdown-menu" role="listbox" aria-labelledby={buttonId}>
          {options.map(option => {
            const isSelected = option.value === value;
            return (
              <button
                key={option.value}
                type="button"
                role="option"
                aria-selected={isSelected}
                className={`dropdown-option ${isSelected ? 'selected' : ''}`}
                onClick={() => {
                  onChange(option.value);
                  setOpen(false);
                }}
              >
                <span>{option.label}</span>
                {isSelected && <Icon name="check" size={15} strokeWidth={2.6} />}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}

function AvailabilityDateField({ selected, onChange }: {
  selected: string[];
  onChange: (days: string[]) => void;
}) {
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState<string[]>(selected);
  const [displayMonth, setDisplayMonth] = useState(() => parseISODate(selected[0] ?? toISODate(new Date())));
  const rootRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!open) return;
    function closeOnOutside(event: MouseEvent) {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) setOpen(false);
    }
    document.addEventListener('mousedown', closeOnOutside);
    return () => document.removeEventListener('mousedown', closeOnOutside);
  }, [open]);

  const draftSorted = [...draft].sort();
  const draftSpan = draftSorted.length ? daysInclusive(draftSorted[0], draftSorted[draftSorted.length - 1]) : 0;
  const draftPairs = countPossiblePairs(draftSorted);
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const monthLabel = new Intl.DateTimeFormat('pl-PL', { month: 'long', year: 'numeric' }).format(displayMonth);
  const triggerLabel = selected.length === 0 ? 'Wybierz swoje wolne dni'
    : `${selected.length} dni wolnych · ${formatDateLong(selected[0])}${selected.length > 1 ? ` – ${formatDateLong(selected[selected.length - 1])}` : ''}`;

  function selectPreset(weekendsOnly: boolean) {
    const year = displayMonth.getFullYear();
    const month = displayMonth.getMonth();
    const days: string[] = [];
    for (let day = new Date(year, month, 1); day.getMonth() === month; day.setDate(day.getDate() + 1)) {
      if (day >= today && (!weekendsOnly || day.getDay() === 0 || day.getDay() === 6)) {
        days.push(toISODate(day));
      }
    }
    setDraft(days);
  }

  return (
    <div className={`date-range-field availability-field ${open ? 'open' : ''}`} ref={rootRef}>
      <button
        type="button"
        className="date-range-trigger input-shell"
        aria-expanded={open}
        aria-label="Wybierz wolne dni w kalendarzu"
        onClick={() => {
          setDraft(selected);
          setDisplayMonth(parseISODate(selected[0] ?? toISODate(new Date())));
          setOpen(previous => !previous);
        }}
      >
        <Icon name="calendar" size={18} />
        <span className="date-range-text">{triggerLabel}</span>
        <Icon name="chevron-down" size={18} className="dropdown-chevron" />
      </button>
      {open && (
        <div className="date-range-popover availability-popover" role="dialog" aria-label="Kalendarz dni wolnych">
          <div className="date-range-popover-header">
            <strong>Kiedy możesz podróżować?</strong>
            <span>Klikaj dowolne dni, aby je zaznaczać i odznaczać. Możesz wybrać kilka oddzielnych weekendów.</span>
          </div>
          <div className="availability-presets" aria-label="Szybkie zaznaczanie dat">
            <button type="button" onClick={() => selectPreset(false)}>Cały miesiąc</button>
            <button type="button" onClick={() => selectPreset(true)}>Weekendy</button>
            <button type="button" onClick={() => setDraft([])}>Wyczyść</button>
          </div>
          <p className="availability-month-note">Szybki wybór dotyczy: <strong>{monthLabel}</strong>. Minione dni są pomijane.</p>
          <DayPicker
            mode="multiple"
            selected={draft.map(parseISODate)}
            onSelect={dates => setDraft((dates ?? []).map(toISODate).sort())}
            month={displayMonth}
            onMonthChange={setDisplayMonth}
            numberOfMonths={1}
            weekStartsOn={1}
            disabled={{ before: today }}
            className="travel-daypicker"
            classNames={{
              months: 'travel-months', month: 'travel-month',
              month_caption: 'travel-month-caption', caption_label: 'travel-caption-label',
              nav: 'travel-nav', button_previous: 'travel-nav-button', button_next: 'travel-nav-button',
              weekdays: 'travel-weekdays', weekday: 'travel-weekday', week: 'travel-week',
              day: 'travel-day-cell', day_button: 'travel-day-button',
              selected: 'travel-day-selected', today: 'travel-day-today', outside: 'travel-day-outside',
              disabled: 'travel-day-disabled',
            }}
          />
          <div className="availability-calendar-summary" aria-live="polite">
            <span><strong>{draft.length}</strong> wybranych dni</span>
            <span><strong>{draftPairs}</strong> możliwych par dat</span>
          </div>
          {draft.length > 0 && draftPairs === 0 && (
            <p className="availability-calendar-warning">Zaznacz co najmniej dwa kolejne dni — pojedyncze dni nie wystarczą na nocleg.</p>
          )}
          {draftSpan > 62 && (
            <p className="availability-calendar-warning">Wybrany okres jest zbyt długi ({draftSpan} dni). Maksimum to 62 dni.</p>
          )}
          <div className="date-range-actions">
            <button type="button" className="inline-secondary-button" onClick={() => setOpen(false)}>Anuluj</button>
            <button
              type="button"
              className="inline-primary-button"
              disabled={draftPairs === 0 || draftSpan > 62}
              onClick={() => { onChange(draftSorted); setOpen(false); }}
            >
              Zastosuj dni wolne
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

function createTimeOptions(): { value: string; label: string }[] {
  const options: { value: string; label: string }[] = [];
  for (let hour = 0; hour < 24; hour += 1) {
    for (let minute = 0; minute < 60; minute += 30) {
      const label = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
      options.push({ value: label, label });
    }
  }
  return options;
}

function toISODate(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function parseISODate(value: string): Date {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function daysInclusive(from: string, to: string): number {
  if (!from || !to) return 0;
  const begin = Date.parse(`${from}T00:00:00Z`);
  const end = Date.parse(`${to}T00:00:00Z`);
  if (!Number.isFinite(begin) || !Number.isFinite(end)) return 0;
  return Math.max(0, Math.round((end - begin) / 86_400_000) + 1);
}

function datesBetween(from: string, to: string): string[] {
  const current = parseISODate(from);
  const end = parseISODate(to);
  const dates: string[] = [];
  while (current <= end) {
    dates.push(toISODate(current));
    current.setDate(current.getDate() + 1);
  }
  return dates;
}

/** Oddzielne weekendy liczymy osobno: nie łączymy ich w jeden długi urlop. */
function countPossiblePairs(days: string[]): number {
  const sorted = [...new Set(days)].sort();
  let pairs = 0;
  let streak = 0;
  let previous = '';
  for (const day of sorted) {
    if (previous && daysInclusive(previous, day) === 2) streak++;
    else streak = 1;
    pairs += streak - 1;
    previous = day;
  }
  return pairs;
}
