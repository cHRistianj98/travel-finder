export type TransportType = 'FLIGHT' | 'TRAIN' | 'BUS' | 'CAR';

export interface TripSearchRequest {
  origin: string;
  outboundDate: string; // YYYY-MM-DD
  returnDate: string; // YYYY-MM-DD
  earliestOutboundDeparture: string; // HH:mm
  latestOutboundDeparture: string; // HH:mm
  earliestReturnArrival: string; // HH:mm
  latestReturnArrival: string; // HH:mm
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
  price: number;
  currency: string;
  totalTravelDurationMinutes: number;
  timeAtDestinationMinutes: number;
  outboundTransfers: number;
  returnTransfers: number;
  provider: string;
  bookingUrl: string | null;
}

export const TRANSPORT_LABELS: Record<TransportType, string> = {
  FLIGHT: 'Samolot',
  TRAIN: 'Pociąg',
  BUS: 'Autobus',
  CAR: 'Samochód',
};

export const DESTINATION_META: Record<string, {
  subtitle: string;
  country: string;
  image: string;
  palette: string;
}> = {
  Rzym: {
    subtitle: 'Historia na każdym rogu', country: 'Włochy', palette: 'orange',
    image: 'https://images.unsplash.com/photo-1552832230-c0197dd311b5?auto=format&fit=crop&w=1100&q=85',
  },
  Praga: {
    subtitle: 'Magia starych uliczek', country: 'Czechy', palette: 'rose',
    image: 'https://images.unsplash.com/photo-1519677100203-a0e668c92439?auto=format&fit=crop&w=1100&q=85',
  },
  Berlin: {
    subtitle: 'Miasto, które nie zasypia', country: 'Niemcy', palette: 'blue',
    image: 'https://images.unsplash.com/photo-1560969184-10fe8719e047?auto=format&fit=crop&w=1100&q=85',
  },
  Karpacz: {
    subtitle: 'Weekend bliżej natury', country: 'Polska', palette: 'green',
    image: 'https://images.unsplash.com/photo-1470770841072-f978cf4d019e?auto=format&fit=crop&w=1100&q=85',
  },
  Mediolan: {
    subtitle: 'Włoskie dolce vita', country: 'Włochy', palette: 'lilac',
    image: 'https://images.unsplash.com/photo-1513326738677-b964603b136d?auto=format&fit=crop&w=1100&q=85',
  },
  Drezno: {
    subtitle: 'Sztuka tuż za granicą', country: 'Niemcy', palette: 'sand',
    image: 'https://images.unsplash.com/photo-1549421263-6064833b071b?auto=format&fit=crop&w=1100&q=85',
  },
};

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

export function formatPrice(amount: number, currency = 'PLN'): string {
  return new Intl.NumberFormat('pl-PL', {
    style: 'currency', currency, maximumFractionDigits: 0,
  }).format(amount);
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
