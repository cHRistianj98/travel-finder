import { resolveDestinationMeta } from './destinationVisuals';

export type TransportType = 'FLIGHT' | 'TRAIN' | 'BUS' | 'CAR';

export interface TripSearchRequest {
  origin: string;
  outboundDate: string; // YYYY-MM-DD
  returnDate: string; // YYYY-MM-DD
  availableDates: string[]; // Konkretne wolne dni YYYY-MM-DD, również rozłączne okresy
  earliestOutboundDeparture: string; // HH:mm
  latestOutboundDeparture: string; // HH:mm
  earliestReturnDeparture: string; // HH:mm
  latestReturnDeparture: string; // HH:mm
  maxTransportBudget: number;
  currency: 'PLN';
  transportTypes: TransportType[];
  directOnly: boolean;
}

/** Niezależny od konkretnego API providera — lustrzany DTO backendu. */
export interface TripOption {
  id: string;
  origin: string;
  destination: string;
  outboundDeparture: string; // ISO-8601 z offsetem
  outboundArrival: string;
  returnDeparture: string;
  returnArrival: string;
  transportType: TransportType;
  price: number | null;
  currency: string;
  totalTravelDurationMinutes: number;
  timeAtDestinationMinutes: number;
  outboundTransfers: number;
  returnTransfers: number;
  provider: string;
  bookingUrl: string | null;
}

export interface DestinationMeta {
  displayName: string;
  subtitle: string;
  country: string;
  image: string;
  palette: string;
  landmark: string;
}

export const TRANSPORT_LABELS: Record<TransportType, string> = {
  FLIGHT: 'Samolot',
  TRAIN: 'Pociąg',
  BUS: 'Autobus',
  CAR: 'Samochód',
};

/**
 * Lokalna biblioteka wizualna pod najczęstsze kierunki z Wrocławia.
 * Zamiast zależeć od zewnętrznych stocków generujemy lekkie ilustracje SVG
 * z motywem znanego miejsca danego miasta.
 */
export const DESTINATION_META: Record<string, DestinationMeta> = {
  Berlin: resolveDestinationMeta('Berlin'),
  Drezno: resolveDestinationMeta('Drezno'),
  Praga: resolveDestinationMeta('Praga'),
  Wiedeń: resolveDestinationMeta('Wiedeń'),
  Budapeszt: resolveDestinationMeta('Budapeszt'),
  Bratysława: resolveDestinationMeta('Bratysława'),
  Brno: resolveDestinationMeta('Brno'),
  Londyn: resolveDestinationMeta('Londyn'),
  Paryż: resolveDestinationMeta('Paryż'),
  Rzym: resolveDestinationMeta('Rzym'),
  Mediolan: resolveDestinationMeta('Mediolan'),
  Barcelona: resolveDestinationMeta('Barcelona'),
  Amsterdam: resolveDestinationMeta('Amsterdam'),
  Kopenhaga: resolveDestinationMeta('Kopenhaga'),
  Sztokholm: resolveDestinationMeta('Sztokholm'),
  Oslo: resolveDestinationMeta('Oslo'),
  Dublin: resolveDestinationMeta('Dublin'),
  Malta: resolveDestinationMeta('Malta'),
  Split: resolveDestinationMeta('Split'),
  Zadar: resolveDestinationMeta('Zadar'),
  Dubrownik: resolveDestinationMeta('Dubrownik'),
  Sofia: resolveDestinationMeta('Sofia'),
  Gdańsk: resolveDestinationMeta('Gdańsk'),
  Kraków: resolveDestinationMeta('Kraków'),
  Warszawa: resolveDestinationMeta('Warszawa'),
  Poznań: resolveDestinationMeta('Poznań'),
  Szczecin: resolveDestinationMeta('Szczecin'),
  Wrocław: resolveDestinationMeta('Wrocław'),
  Monachium: resolveDestinationMeta('Monachium'),
  Frankfurt: resolveDestinationMeta('Frankfurt'),
};

export function formatPrice(amount: number, currency = 'PLN'): string {
  return new Intl.NumberFormat('pl-PL', {
    style: 'currency', currency, maximumFractionDigits: 0,
  }).format(amount);
}

export const POLISH_CITIES = [
  'Wrocław',
  'Warszawa',
  'Kraków',
  'Gdańsk',
  'Poznań',
  'Łódź',
  'Katowice',
  'Szczecin',
  'Lublin',
  'Bydgoszcz',
  'Białystok',
  'Rzeszów',
] as const;

export function nextWeekend(): { outboundDate: string; returnDate: string } {
  const now = new Date();
  const friday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const delta = (5 - friday.getDay() + 7) % 7;
  friday.setDate(friday.getDate() + (delta === 0 ? 7 : delta));
  const sunday = new Date(friday);
  sunday.setDate(sunday.getDate() + 2);

  const localISO = (date: Date): string =>
    `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;

  return { outboundDate: localISO(friday), returnDate: localISO(sunday) };
}

export function formatDate(date: string): string {
  return new Intl.DateTimeFormat('pl-PL', { day: 'numeric', month: 'short' })
    .format(new Date(`${date.slice(0, 10)}T12:00:00`));
}

export function formatDateLong(date: string): string {
  return new Intl.DateTimeFormat('pl-PL', { day: 'numeric', month: 'long', year: 'numeric' })
    .format(new Date(`${date.slice(0, 10)}T12:00:00`));
}

export function formatWeekday(iso: string): string {
  return new Intl.DateTimeFormat('pl-PL', { weekday: 'short' })
    .format(new Date(`${iso.slice(0, 10)}T12:00:00`));
}

export function formatTime(iso: string): string {
  return iso.slice(11, 16);
}

export function durationLabel(minutes: number): string {
  const hours = Math.floor(minutes / 60);
  const remaining = minutes % 60;
  return remaining === 0 ? `${hours} godz.` : `${hours} godz. ${remaining} min`;
}
