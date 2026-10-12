/**
 * Sprawdzone strony relacji FlixBus, nie potwierdzone rezerwacje.
 * Bez oficjalnego API partnera nie dopisujemy do URL rzekomo obsługiwanych dat.
 * Nieznane trasy kierują na stronę główną, gdzie można wybrać miasta ręcznie.
 */
const KNOWN_FROM_WROCLAW = new Set([
  'lubin', 'jelenia-gora', 'zielona-gora', 'swiebodzin',
  'gorzow-wielkopolski', 'szczecin', 'gliwice', 'chemnitz',
  'miedzyzdroje', 'harrachov',
  'poznan', 'berlin', 'praga', 'krakow', 'lodz', 'warszawa',
  'katowice', 'drezno', 'wieden', 'szklarska-poreba', 'port-lotniczy-berlin',
]);

const KNOWN_SLUGS: Record<string, string> = {
  wroclaw: 'wroclaw', poznan: 'poznan', berlin: 'berlin', praga: 'praga',
  prague: 'praga', dresden: 'drezno', drezno: 'drezno',
  krakow: 'krakow', lodz: 'lodz', warszawa: 'warszawa',
  katowice: 'katowice', wieden: 'wieden',
  lubin: 'lubin', 'jelenia gora': 'jelenia-gora', 'zielona gora': 'zielona-gora',
  swiebodzin: 'swiebodzin', 'gorzow wielkopolski': 'gorzow-wielkopolski',
  szczecin: 'szczecin', gliwice: 'gliwice', chemnitz: 'chemnitz',
  miedzyzdroje: 'miedzyzdroje', harrachov: 'harrachov', warsaw: 'warszawa',
};

function slug(value: string): string {
  const normalized = value.toLowerCase().replace(/ł/g, 'l').normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '').replace(/[^a-z0-9]+/g, ' ').trim();
  if (normalized.startsWith('szklarska poreba')) return 'szklarska-poreba';
  if (normalized.startsWith('berlin airport') || normalized.startsWith('port lotniczy berlin')) return 'port-lotniczy-berlin';
  const match = Object.keys(KNOWN_SLUGS).find(key => normalized === key || normalized.startsWith(key + ' '));
  return match ? KNOWN_SLUGS[match] : '';
}

export function flixBusRoutePage(from: string, to: string): string {
  const origin = slug(from);
  const destination = slug(to);
  if (origin === 'wroclaw' && KNOWN_FROM_WROCLAW.has(destination)) {
    return `https://www.flixbus.pl/polaczenia-autobusowe/wroclaw-${destination}`;
  }
  if (destination === 'wroclaw' && KNOWN_FROM_WROCLAW.has(origin)) {
    return `https://www.flixbus.pl/polaczenia-autobusowe/${origin}-wroclaw`;
  }
  return 'https://www.flixbus.pl/';
}

export function flixBusDateSummary(
  from: string, to: string, outboundDeparture: string, returnDeparture: string,
): string {
  return `FlixBus: ${from} → ${to}\nWyjazd: ${outboundDeparture.slice(0, 10)} o ${outboundDeparture.slice(11, 16)}\n` +
    `Powrót: ${returnDeparture.slice(0, 10)} o ${returnDeparture.slice(11, 16)}\n` +
    'Sprawdź kurs oraz ostateczną cenę u przewoźnika.';
}
