import { useEffect, useId, useMemo, useRef, useState, type FormEvent } from 'react';
import { DayPicker, type DateRange } from 'react-day-picker';
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
  { type: 'TRAIN', icon: 'train', title: 'Pociąg', subtitle: 'Bez stresu', accent: 'train' },
  { type: 'BUS', icon: 'bus', title: 'Autobus', subtitle: 'Budżetowo', accent: 'bus' },
  { type: 'CAR', icon: 'car', title: 'Samochód', subtitle: 'Pełna swoboda', accent: 'car' },
];

const TIME_OPTIONS = createTimeOptions();
const DEFAULT_WEEKEND = nextWeekend();

interface Props {
  onSearch: (request: TripSearchRequest) => void;
  loading: boolean;
}

export function TripSearchForm({ onSearch, loading }: Props) {
  const [form, setForm] = useState<TripSearchRequest>(() => ({
    origin: 'Wrocław',
    outboundDate: DEFAULT_WEEKEND.outboundDate,
    returnDate: DEFAULT_WEEKEND.returnDate,
    earliestOutboundDeparture: '15:00',
    latestOutboundDeparture: '23:00',
    earliestReturnArrival: '15:00',
    latestReturnArrival: '23:00',
    maxTransportBudget: 500,
    currency: 'PLN',
    transportTypes: ['FLIGHT', 'TRAIN', 'BUS', 'CAR'],
    directOnly: true,
  }));
  const [validationError, setValidationError] = useState<string | null>(null);

  function update<K extends keyof TripSearchRequest>(key: K, value: TripSearchRequest[K]) {
    setForm(previous => ({ ...previous, [key]: value }));
    setValidationError(null);
  }

  function toggleTransport(type: TransportType) {
    update('transportTypes', form.transportTypes.includes(type)
      ? form.transportTypes.filter(item => item !== type)
      : [...form.transportTypes, type]);
  }

  function handleDateRangeChange(range: DateRange | undefined) {
    if (!range?.from || !range.to) {
      return;
    }
    const outboundDate = toISODate(range.from);
    const returnDate = toISODate(range.to);
    setForm(previous => ({ ...previous, outboundDate, returnDate }));
    setValidationError(null);
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!form.origin.trim()) {
      setValidationError('Wybierz miasto, z którego chcesz wyjechać.');
      return;
    }
    if (!form.outboundDate || !form.returnDate || form.returnDate < form.outboundDate) {
      setValidationError('Wybierz poprawny zakres wyjazdu i powrotu.');
      return;
    }
    if (form.outboundDate === form.returnDate) {
      setValidationError('Na start potrzebujemy przynajmniej jednego noclegu — wybierz późniejszy powrót.');
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
    if (form.earliestReturnArrival > form.latestReturnArrival) {
      setValidationError('Godzina końca powrotu musi być późniejsza niż jego początek.');
      return;
    }

    onSearch({ ...form, origin: form.origin.trim() });
  }

  const selectedRange = useMemo<DateRange>(() => ({
    from: parseISODate(form.outboundDate),
    to: parseISODate(form.returnDate),
  }), [form.outboundDate, form.returnDate]);

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

        <label className="field-label">Zakres wyjazdu</label>
        <DateRangeField range={selectedRange} onChange={handleDateRangeChange} />
      </div>

      <div className="field-section divider-top">
        <div className="section-label"><span>02</span> Twój czas i budżet</div>
        <div className="time-range-grid">
          <div className="time-block">
            <div className="time-block-header">
              <div>
                <strong>Wyjazd</strong>
                <span>Przedział godzin rozpoczęcia podróży</span>
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
                <strong>Powrót do domu</strong>
                <span>Przedział godzin dotarcia z powrotem</span>
              </div>
              <Icon name="hourglass" size={18} />
            </div>
            <div className="input-pair compact roomy-pair">
              <div>
                <label className="field-label subtle-label">Od</label>
                <DropdownSelect
                  icon="clock"
                  value={form.earliestReturnArrival}
                  options={TIME_OPTIONS}
                  onChange={value => update('earliestReturnArrival', value)}
                  ariaLabel="Najwcześniejsza godzina powrotu"
                />
              </div>
              <div>
                <label className="field-label subtle-label">Do</label>
                <DropdownSelect
                  icon="clock"
                  value={form.latestReturnArrival}
                  options={TIME_OPTIONS}
                  onChange={value => update('latestReturnArrival', value)}
                  ariaLabel="Najpóźniejsza godzina powrotu"
                />
              </div>
            </div>
          </div>
        </div>

        <label className="field-label" htmlFor="budget">Maksymalny budżet na transport</label>
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
                className={`transport-choice transport-${transport.accent} ${selected ? 'selected' : ''}`}
                aria-pressed={selected}
                onClick={() => toggleTransport(transport.type)}
              >
                <span className="transport-icon"><Icon name={transport.icon} size={20} /></span>
                <span className="transport-name"><strong>{transport.title}</strong><small>{transport.subtitle}</small></span>
                <span className="transport-checkbox">{selected && <Icon name="check" size={11} strokeWidth={3} />}</span>
              </button>
            );
          })}
        </div>
        <div className="direct-row"><div className="direct-text"><strong>Tylko bezpośrednie</strong><span>Bez przesiadek po drodze</span></div><button type="button" role="switch" aria-label="Tylko połączenia bezpośrednie" aria-checked={form.directOnly} className={`switch ${form.directOnly ? 'on' : ''}`} onClick={() => update('directOnly', !form.directOnly)}><span /></button></div>
      </div>

      {validationError && <p className="form-error" role="alert"><Icon name="alert" size={16} />{validationError}</p>}
      <button className="submit-button" type="submit" disabled={loading}>{loading ? <><span className="button-spinner" />Szukam kierunków...</> : <><Icon name="search" size={20} /> Odkryj kierunki <Icon name="arrow-right" size={20} className="submit-arrow" /></>}</button>
      <div className="form-disclaimer"><Icon name="info" size={14} /> Na razie pokazujemy przykładowe połączenia demonstracyjne, ale już w docelowym flow aplikacji.</div>
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

function DateRangeField({ range, onChange }: { range: DateRange; onChange: (range: DateRange | undefined) => void }) {
  const [open, setOpen] = useState(false);
  const [draftRange, setDraftRange] = useState<DateRange | undefined>(range);
  const rootRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    setDraftRange(range);
  }, [range.from?.getTime(), range.to?.getTime()]);

  useEffect(() => {
    function closeAndMaybeApply(event: MouseEvent) {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        if (draftRange?.from && draftRange.to) {
          onChange(draftRange);
        }
        setOpen(false);
      }
    }

    document.addEventListener('mousedown', closeAndMaybeApply);
    return () => document.removeEventListener('mousedown', closeAndMaybeApply);
  }, [draftRange, onChange]);

  const label = range.from && range.to
    ? `${formatDateLong(toISODate(range.from))} — ${formatDateLong(toISODate(range.to))}`
    : 'Wybierz zakres dat';

  const draftLabelFrom = draftRange?.from ? formatDateLong(toISODate(draftRange.from)) : 'Brak wyjazdu';
  const draftLabelTo = draftRange?.to ? formatDateLong(toISODate(draftRange.to)) : 'Brak powrotu';
  const canConfirm = Boolean(draftRange?.from && draftRange.to);

  return (
    <div className={`date-range-field ${open ? 'open' : ''}`} ref={rootRef}>
      <button
        type="button"
        className="date-range-trigger input-shell"
        onClick={() => {
          setDraftRange(range);
          setOpen(previous => !previous);
        }}
        aria-expanded={open}
      >
        <Icon name="calendar" size={18} />
        <span className="date-range-text">{label}</span>
        <Icon name="chevron-down" size={18} className="dropdown-chevron" />
      </button>
      {open && (
        <div className="date-range-popover" role="dialog" aria-label="Wybór zakresu dat">
          <div className="date-range-popover-header">
            <div>
              <strong>Wybierz weekend lub przedział</strong>
              <span>Kliknij datę wyjazdu, a potem datę powrotu. Okno zamknie się dopiero po kliknięciu OK lub poza nim.</span>
            </div>
          </div>
          <DayPicker
            mode="range"
            selected={draftRange}
            onSelect={setDraftRange}
            numberOfMonths={2}
            pagedNavigation
            weekStartsOn={1}
            min={1}
            defaultMonth={draftRange?.from ?? range.from}
            className="travel-daypicker"
            classNames={{
              months: 'travel-months',
              month: 'travel-month',
              month_caption: 'travel-month-caption',
              caption_label: 'travel-caption-label',
              nav: 'travel-nav',
              button_previous: 'travel-nav-button',
              button_next: 'travel-nav-button',
              weekdays: 'travel-weekdays',
              weekday: 'travel-weekday',
              week: 'travel-week',
              day: 'travel-day-cell',
              day_button: 'travel-day-button',
              selected: 'travel-day-selected',
              range_start: 'travel-day-range-start',
              range_middle: 'travel-day-range-middle',
              range_end: 'travel-day-range-end',
              today: 'travel-day-today',
              outside: 'travel-day-outside',
            }}
          />
          <div className="date-range-summary">
            <span>
              <strong>{draftLabelFrom}</strong>
              <small>Wyjazd</small>
            </span>
            <span>
              <strong>{draftLabelTo}</strong>
              <small>Powrót</small>
            </span>
          </div>
          <div className="date-range-actions">
            <button
              type="button"
              className="inline-secondary-button"
              onClick={() => {
                setDraftRange(range);
                setOpen(false);
              }}
            >
              Anuluj
            </button>
            <button
              type="button"
              className="inline-primary-button"
              disabled={!canConfirm}
              onClick={() => {
                if (draftRange?.from && draftRange.to) {
                  onChange(draftRange);
                  setOpen(false);
                }
              }}
            >
              OK
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
