/**
 * Fotorealistyczne wizualizacje znanych miejsc, wygenerowane dla Travel Finder.
 * Pliki WebP znajdują się lokalnie w frontend/public/destinations/.
 * Nie są fotografiami dokumentalnymi i nie potwierdzają miejsca przystanku.
 */
export interface DestinationMeta {
  displayName: string;
  subtitle: string;
  country: string;
  palette: string;
  landmark: string;
  image: string;
}

type Place = Omit<DestinationMeta, 'image'>;

const DESTINATIONS: Record<string, Place> = {
  lubin: { displayName: 'Lubin', subtitle: 'Rynek i spacer po dolnośląskim mieście', country: 'Polska', palette: 'green', landmark: 'Ratusz w Lubinie' },
  poznan: { displayName: 'Poznań', subtitle: 'Kamienice, koziołki i miejski klimat', country: 'Polska', palette: 'orange', landmark: 'Ratusz na Starym Rynku' },
  'jelenia-gora': { displayName: 'Jelenia Góra', subtitle: 'Kolorowy rynek u stóp Karkonoszy', country: 'Polska', palette: 'green', landmark: 'Rynek w Jeleniej Górze' },
  'zielona-gora': { displayName: 'Zielona Góra', subtitle: 'Winnice i lubuskie krajobrazy', country: 'Polska', palette: 'green', landmark: 'Winnice Zielonej Góry' },
  katowice: { displayName: 'Katowice', subtitle: 'Nowoczesna architektura i Strefa Kultury', country: 'Polska', palette: 'blue', landmark: 'Spodek w Katowicach' },
  'szklarska-poreba': { displayName: 'Szklarska Poręba', subtitle: 'Górskie szlaki i kaskady wodospadów', country: 'Polska', palette: 'green', landmark: 'Wodospad Kamieńczyka' },
  krakow: { displayName: 'Kraków', subtitle: 'Wawel, Wisła i Stare Miasto', country: 'Polska', palette: 'rose', landmark: 'Zamek Królewski na Wawelu' },
  swiebodzin: { displayName: 'Świebodzin', subtitle: 'Miasto z monumentalną figurą', country: 'Polska', palette: 'sand', landmark: 'Figura Chrystusa Króla' },
  harrachov: { displayName: 'Harrachov', subtitle: 'Czeskie góry i sportowe tradycje', country: 'Czechy', palette: 'green', landmark: 'Skocznie narciarskie' },
  lodz: { displayName: 'Łódź', subtitle: 'Kamienice, sztuka i kulinarny city break', country: 'Polska', palette: 'rose', landmark: 'Ulica Piotrkowska' },
  'gorzow-wielkopolski': { displayName: 'Gorzów Wielkopolski', subtitle: 'Spacer nad Wartą i historia miasta', country: 'Polska', palette: 'orange', landmark: 'Bulwary nad Wartą' },
  gliwice: { displayName: 'Gliwice', subtitle: 'Śląska architektura i miejskie odkrycia', country: 'Polska', palette: 'sand', landmark: 'Radiostacja Gliwicka' },
  chemnitz: { displayName: 'Chemnitz', subtitle: 'Architektura i sztuka Saksonii', country: 'Niemcy', palette: 'blue', landmark: 'Pomnik Karola Marksa' },
  drezno: { displayName: 'Drezno', subtitle: 'Barokowa perła nad Łabą', country: 'Niemcy', palette: 'sand', landmark: 'Frauenkirche' },
  miedzyzdroje: { displayName: 'Międzyzdroje', subtitle: 'Bałtyk i zachody słońca nad morzem', country: 'Polska', palette: 'blue', landmark: 'Molo w Międzyzdrojach' },
  szczecin: { displayName: 'Szczecin', subtitle: 'Tarasy nad Odrą i nadmorski klimat', country: 'Polska', palette: 'blue', landmark: 'Wały Chrobrego' },
  warszawa: { displayName: 'Warszawa', subtitle: 'Historia i tętniące życiem centrum', country: 'Polska', palette: 'rose', landmark: 'Zamek Królewski i kolumna Zygmunta' },
  praga: { displayName: 'Praga', subtitle: 'Zabytkowe mosty i widoki nad Wełtawą', country: 'Czechy', palette: 'rose', landmark: 'Most Karola' },
  berlin: { displayName: 'Berlin', subtitle: 'Kultura, historia i puls wielkiego miasta', country: 'Niemcy', palette: 'blue', landmark: 'Brama Brandenburska' },
  mediolan: { displayName: 'Mediolan', subtitle: 'Włoski design i kawiarnie', country: 'Włochy', palette: 'lilac', landmark: 'Duomo di Milano' },
  rzym: { displayName: 'Rzym', subtitle: 'Zabytki na każdym kroku', country: 'Włochy', palette: 'orange', landmark: 'Koloseum' },
  paryz: { displayName: 'Paryż', subtitle: 'Romantyczne spacery nad Sekwaną', country: 'Francja', palette: 'rose', landmark: 'Wieża Eiffla' },
};

/**
 * GTFS FlixBusa zwraca nazwy przystanków, np. "Berlin Airport BER".
 * Tłumaczymy je wyłącznie na potrzeby prezentacji, a pełną nazwę pozostawiamy w danych podróży.
 * Aliasy zapisane najpierw od najdłuższych/konkretniejszych nazw.
 */
const ALIASES: Array<[string, string]> = [
  ['szklarska poreba gorna', 'szklarska-poreba'],
  ['szklarska poreba', 'szklarska-poreba'],
  ['gorzow wielkopolski', 'gorzow-wielkopolski'],
  ['jelenia gora', 'jelenia-gora'],
  ['zielona gora', 'zielona-gora'],
  ['swiebodzin', 'swiebodzin'],
  ['miedzyzdroje', 'miedzyzdroje'],
  ['dresden', 'drezno'],
  ['drezno', 'drezno'],
  ['prague', 'praga'],
  ['prag', 'praga'],
  ['praga', 'praga'],
  ['warsaw', 'warszawa'],
  ['warszawa', 'warszawa'],
  ['poznan', 'poznan'],
  ['krakow', 'krakow'],
  ['lodz', 'lodz'],
  ['lubin', 'lubin'],
  ['katowice', 'katowice'],
  ['harrachov', 'harrachov'],
  ['gliwice', 'gliwice'],
  ['chemnitz', 'chemnitz'],
  ['szczecin', 'szczecin'],
  ['berlin', 'berlin'],
  ['milan', 'mediolan'],
  ['mediolan', 'mediolan'],
  ['rome', 'rzym'],
  ['rzym', 'rzym'],
  ['paris', 'paryz'],
  ['paryz', 'paryz'],
];

function normalize(input: string): string {
  return input
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, ' ')
    .trim();
}

const UNKNOWN_DESTINATION: DestinationMeta = {
  displayName: '',
  subtitle: 'Odkryj nowe miejsce',
  country: 'NOWY KIERUNEK',
  palette: 'green',
  landmark: '',
  image: '',
};

export function resolveDestinationMeta(rawDestination: string): DestinationMeta {
  const normalized = normalize(rawDestination);
  const alias = ALIASES.find(([prefix]) => normalized === prefix || normalized.startsWith(`${prefix} `));
  const key = alias?.[1];
  const place = key ? DESTINATIONS[key] : undefined;
  if (!key || !place) return { ...UNKNOWN_DESTINATION, displayName: rawDestination };
  return { ...place, image: `/destinations/${key}.webp` };
}
