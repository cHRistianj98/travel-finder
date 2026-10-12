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
    .toLowerCase()
    .replace(/ł/g, 'l')
    .replace(/ß/g, 'ss')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, ' ')
    .trim();
}

/**
 * Loty mają osobną bibliotekę obrazów z wygenerowanych panoram AI.
 * Wspólne miasta (np. Berlin, Paryż, Warszawa) nie nadpisują grafik autobusowych.
 * Klucze poniżej odpowiadają nazwom plików /destinations/flights/<key>.webp.
 */
const FLIGHT_DESTINATIONS: Record<string, Place> = {
  budapeszt: { displayName: 'Budapeszt', subtitle: 'Dunaj, mosty i wieczorne panoramy', country: 'Węgry', palette: 'rose', landmark: 'Parlament nad Dunajem' },
  oslo: { displayName: 'Oslo', subtitle: 'Fiord, design i skandynawska architektura', country: 'Norwegia', palette: 'blue', landmark: 'Opera w Oslo' },
  londyn: { displayName: 'Londyn', subtitle: 'Ikony miasta i spacery nad Tamizą', country: 'Wielka Brytania', palette: 'blue', landmark: 'Big Ben i Westminster' },
  kiszyniow: { displayName: 'Kiszyniów', subtitle: 'Odkrywaj stolicę Mołdawii', country: 'Mołdawia', palette: 'sand', landmark: 'Łuk Triumfalny' },
  paryz: { displayName: 'Paryż', subtitle: 'Spacer nad Sekwaną i paryskie bulwary', country: 'Francja', palette: 'rose', landmark: 'Wieża Eiffla' },
  palermo: { displayName: 'Palermo', subtitle: 'Sycylińskie place i śródziemnomorski klimat', country: 'Włochy', palette: 'orange', landmark: 'Katedra w Palermo' },
  malaga: { displayName: 'Malaga', subtitle: 'Słońce i andaluzyjskie uliczki', country: 'Hiszpania', palette: 'sand', landmark: 'Alcazaba i port w Maladze' },
  mediolan: { displayName: 'Mediolan', subtitle: 'Kawiarnie, styl i włoska architektura', country: 'Włochy', palette: 'lilac', landmark: 'Katedra Duomo' },
  barcelona: { displayName: 'Barcelona', subtitle: 'Architektura Gaudíego i śródziemnomorskie słońce', country: 'Hiszpania', palette: 'orange', landmark: 'Sagrada Família' },
  dublin: { displayName: 'Dublin', subtitle: 'Kolorowe fasady i muzyka w pubach', country: 'Irlandia', palette: 'green', landmark: 'Most Ha’penny' },
  alicante: { displayName: 'Alicante', subtitle: 'Hiszpańskie plaże i promenady', country: 'Hiszpania', palette: 'blue', landmark: 'Zamek Santa Bárbara' },
  rzym: { displayName: 'Rzym', subtitle: 'Wieczne Miasto i antyczna historia', country: 'Włochy', palette: 'orange', landmark: 'Koloseum' },
  warszawa: { displayName: 'Warszawa', subtitle: 'Starówka, Wisła i stołeczne życie', country: 'Polska', palette: 'rose', landmark: 'Plac Zamkowy' },
  larnaka: { displayName: 'Larnaka', subtitle: 'Morze, palmy i cypryjskie spacery', country: 'Cypr', palette: 'blue', landmark: 'Promenada Finikoudes' },
  bolonia: { displayName: 'Bolonia', subtitle: 'Portyki, renesans i włoska kuchnia', country: 'Włochy', palette: 'orange', landmark: 'Dwie Wieże' },
  fuerteventura: { displayName: 'Fuerteventura', subtitle: 'Plaże, wydmy i oceaniczne przestrzenie', country: 'Hiszpania · Wyspy Kanaryjskie', palette: 'sand', landmark: 'Wydmy Corralejo' },
  sztokholm: { displayName: 'Sztokholm', subtitle: 'Wyspy, kamienice i nordyckie światło', country: 'Szwecja', palette: 'blue', landmark: 'Gamla Stan' },
  frankfurt: { displayName: 'Frankfurt', subtitle: 'Nowoczesna panorama i historyczne centrum', country: 'Niemcy', palette: 'blue', landmark: 'Römerberg i skyline' },
  teneryfa: { displayName: 'Teneryfa', subtitle: 'Wulkan, ocean i wieczna wiosna', country: 'Hiszpania · Wyspy Kanaryjskie', palette: 'orange', landmark: 'Wulkan Teide' },
  'gran-canaria': { displayName: 'Gran Canaria', subtitle: 'Plaże i wyspiarskie krajobrazy', country: 'Hiszpania · Wyspy Kanaryjskie', palette: 'sand', landmark: 'Las Palmas i Atlantyk' },
  ateny: { displayName: 'Ateny', subtitle: 'Antyczne wzgórza i greckie uliczki', country: 'Grecja', palette: 'sand', landmark: 'Akropol' },
  berlin: { displayName: 'Berlin', subtitle: 'Historia i nowoczesne miejskie życie', country: 'Niemcy', palette: 'blue', landmark: 'Brama Brandenburska' },
  nicea: { displayName: 'Nicea', subtitle: 'Riwiera Francuska i lazurowe morze', country: 'Francja', palette: 'blue', landmark: 'Promenade des Anglais' },
  belgrad: { displayName: 'Belgrad', subtitle: 'Nad Sawą i Dunajem', country: 'Serbia', palette: 'green', landmark: 'Twierdza Kalemegdan' },
  marrakesz: { displayName: 'Marrakesz', subtitle: 'Kolory, targi i marokańskie ogrody', country: 'Maroko', palette: 'orange', landmark: 'Minaret Kutubijja' },
  reykjavik: { displayName: 'Reykjavík', subtitle: 'Gejzery, ocean i islandzka stolica', country: 'Islandia', palette: 'blue', landmark: 'Kościół Hallgrímskirkja' },
  suczawa: { displayName: 'Suczawa', subtitle: 'Bukowina i historyczne twierdze', country: 'Rumunia', palette: 'green', landmark: 'Twierdza tronowa' },
  kutaisi: { displayName: 'Kutaisi', subtitle: 'Gruzja poza utartym szlakiem', country: 'Gruzja', palette: 'green', landmark: 'Katedra Bagrati' },
  sewilla: { displayName: 'Sewilla', subtitle: 'Andaluzja, tapas i słoneczne place', country: 'Hiszpania', palette: 'orange', landmark: 'Plaza de España' },
  walencja: { displayName: 'Walencja', subtitle: 'Morze i futurystyczna architektura', country: 'Hiszpania', palette: 'blue', landmark: 'Miasto Sztuki i Nauki' },
  madryt: { displayName: 'Madryt', subtitle: 'Królewska stolica i hiszpańskie życie', country: 'Hiszpania', palette: 'orange', landmark: 'Plac Cibeles' },
  oviedo: { displayName: 'Oviedo', subtitle: 'Zielona Asturia na północy Hiszpanii', country: 'Hiszpania', palette: 'green', landmark: 'Katedra w Oviedo' },
  monachium: { displayName: 'Monachium', subtitle: 'Bawarskie tradycje i zabytkowe place', country: 'Niemcy', palette: 'sand', landmark: 'Marienplatz' },
  antalya: { displayName: 'Antalya', subtitle: 'Turkusowe wybrzeże i śródziemnomorski klimat', country: 'Turcja', palette: 'blue', landmark: 'Stary port Kaleiçi' },
  erywan: { displayName: 'Erywań', subtitle: 'Kaukaskie krajobrazy i architektura', country: 'Armenia', palette: 'rose', landmark: 'Plac Republiki' },
  zurych: { displayName: 'Zurych', subtitle: 'Jezioro i szwajcarski miejski styl', country: 'Szwajcaria', palette: 'blue', landmark: 'Starówka nad Limmatem' },
  baku: { displayName: 'Baku', subtitle: 'Caspian skyline i stare miasto', country: 'Azerbejdżan', palette: 'blue', landmark: 'Flame Towers' },
  hurghada: { displayName: 'Hurghada', subtitle: 'Słońce i błękit Morza Czerwonego', country: 'Egipt', palette: 'blue', landmark: 'Marina w Hurghadzie' },
  'szarm-el-szejk': { displayName: 'Szarm el-Szejk', subtitle: 'Rafy koralowe i złote plaże', country: 'Egipt', palette: 'blue', landmark: 'Wybrzeże Morza Czerwonego' },
  korfu: { displayName: 'Korfu', subtitle: 'Zatoczki i greckie Stare Miasto', country: 'Grecja', palette: 'green', landmark: 'Stara Forteca Korfu' },
  tbilisi: { displayName: 'Tbilisi', subtitle: 'Kolorowe domy i kaukaskie wzgórza', country: 'Gruzja', palette: 'rose', landmark: 'Twierdza Narikala' },
  wroclaw: { displayName: 'Wrocław', subtitle: 'Mosty i zabytkowy rynek', country: 'Polska', palette: 'green', landmark: 'Wrocławski Rynek' },
};

/**
 * Mapowanie kodów IATA / miast w różnych językach do panoram.
 * RMO=Kiszyniów (Mołdawia), SCV=Suczawa, OVD=Asturia/Oviedo,
 * FUE=Fuerteventura, a BAK oznacza miasto Baku.
 */
const FLIGHT_ALIASES: Record<string, string> = {
  rmo: 'kiszyniow', chisinau: 'kiszyniow', 'chisinau airport': 'kiszyniow',
  fue: 'fuerteventura',
  tci: 'teneryfa', tfs: 'teneryfa', tfn: 'teneryfa', tenerife: 'teneryfa',
  lpa: 'gran-canaria', 'las palmas': 'gran-canaria',
  rak: 'marrakesz', marrakech: 'marrakesz', marrakesh: 'marrakesz',
  scv: 'suczawa', suceava: 'suczawa',
  ovd: 'oviedo', asturias: 'oviedo',
  evn: 'erywan', yerevan: 'erywan',
  zrh: 'zurych', zurich: 'zurych',
  bak: 'baku', gyd: 'baku',
  hrg: 'hurghada',
  ssh: 'szarm-el-szejk', 'sharm el sheikh': 'szarm-el-szejk', 'sharm el sheik': 'szarm-el-szejk',
  tbs: 'tbilisi',
  kut: 'kutaisi',
  rom: 'rzym',
  lon: 'londyn', london: 'londyn',
  par: 'paryz', paris: 'paryz',
  mil: 'mediolan', milan: 'mediolan',
  waw: 'warszawa', warsaw: 'warszawa',
  ber: 'berlin',
  sto: 'sztokholm', stockholm: 'sztokholm',
  osl: 'oslo', trf: 'oslo',
  agp: 'malaga', malaga: 'malaga',
  blq: 'bolonia', bologna: 'bolonia',
  vlc: 'walencja', valencia: 'walencja',
  sevilla: 'sewilla', svq: 'sewilla',
  ath: 'ateny', athens: 'ateny',
  nce: 'nicea', nice: 'nicea',
  cfu: 'korfu', corfu: 'korfu',
  rey: 'reykjavik', rek: 'reykjavik', kef: 'reykjavik', reykjavik: 'reykjavik',
  pmo: 'palermo', bcn: 'barcelona', dub: 'dublin',
  alc: 'alicante', lca: 'larnaka', larnaca: 'larnaka',
  beg: 'belgrad', belgrade: 'belgrad',
  fra: 'frankfurt', muc: 'monachium', munich: 'monachium',
  ayt: 'antalya', mad: 'madryt', madrid: 'madryt',
  bud: 'budapeszt', budapest: 'budapeszt',
  wro: 'wroclaw',
};

// Avoid matching airport suffixes too eagerly (e.g. "Frankfurt Hahn" can be a distinct airport).
function findFlightKey(normalized: string): string | undefined {
  const explicit = FLIGHT_ALIASES[normalized] ?? normalized.replaceAll(' ', '-');
  if (FLIGHT_DESTINATIONS[explicit]) return explicit;
  const names = Object.keys(FLIGHT_DESTINATIONS).map(key => [key.replaceAll('-', ' '), key] as const);
  // Prefer longer city names over one-word prefixes to avoid accidental matches.
  names.sort((a, b) => b[0].length - a[0].length);
  return names.find(([city]) => normalized.startsWith(`${city} `))?.[1];
}

const UNKNOWN_DESTINATION: DestinationMeta = {
  displayName: '',
  subtitle: 'Odkryj nowe miejsce',
  country: 'NOWY KIERUNEK',
  palette: 'green',
  landmark: '',
  image: '',
};

/**
 * Zwraca metadata miejsca dla karty. Domyślnie pozostaje stara biblioteka FlixBusa.
 * Loty korzystają z oddzielnych fotografio-podobnych panoram AI.
 */
export function resolveDestinationMeta(
  rawDestination: string,
  transportType: 'FLIGHT' | 'TRAIN' | 'BUS' | 'CAR' = 'BUS',
): DestinationMeta {
  const normalized = normalize(rawDestination);
  if (transportType === 'FLIGHT') {
    const flightKey = findFlightKey(normalized);
    const flightPlace = flightKey ? FLIGHT_DESTINATIONS[flightKey] : undefined;
    if (flightKey && flightPlace) {
      return { ...flightPlace, image: `/destinations/flights/${flightKey}.webp` };
    }
  }

  // Dotychczasowe zdjęcia autobusów/pociągów pozostają bez zmian.
  const alias = ALIASES.find(([prefix]) => normalized === prefix || normalized.startsWith(`${prefix} `));
  const key = alias?.[1];
  const place = key ? DESTINATIONS[key] : undefined;
  if (!key || !place) return { ...UNKNOWN_DESTINATION, displayName: rawDestination };
  return { ...place, image: `/destinations/${key}.webp` };
}
