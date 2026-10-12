export interface DestinationMeta {
  subtitle: string;
  country: string;
  palette: string;
  landmark: string;
  image: string;
}

type Variant =
  | 'eiffel'
  | 'bigben'
  | 'brandenburg'
  | 'colosseum'
  | 'duomo'
  | 'charles'
  | 'sagrada'
  | 'canals'
  | 'parliament'
  | 'castle'
  | 'oldtown'
  | 'harbor'
  | 'opera'
  | 'beach'
  | 'generic';

type PaletteName =
  | 'emerald'
  | 'rose'
  | 'sunset'
  | 'ocean'
  | 'gold'
  | 'violet'
  | 'teal'
  | 'navy'
  | 'sand'
  | 'berry';

interface Palette {
  start: string;
  end: string;
  glow: string;
  line: string;
  panel: string;
}

const PALETTES: Record<PaletteName, Palette> = {
  emerald: { start: '#103b32', end: '#2d7d68', glow: '#d8f8d9', line: '#ebfff1', panel: '#0d221b' },
  rose: { start: '#4c1d39', end: '#b54878', glow: '#ffe2ef', line: '#fff0f7', panel: '#2a1320' },
  sunset: { start: '#572315', end: '#db7d3e', glow: '#ffe3b8', line: '#fff4df', panel: '#3a1a10' },
  ocean: { start: '#153556', end: '#4d89d3', glow: '#dceeff', line: '#edf7ff', panel: '#10263c' },
  gold: { start: '#574015', end: '#d7a63c', glow: '#fff3cc', line: '#fff9eb', panel: '#372a10' },
  violet: { start: '#38214f', end: '#8d62d6', glow: '#efe3ff', line: '#fbf3ff', panel: '#241534' },
  teal: { start: '#123b40', end: '#37919c', glow: '#daf8f6', line: '#effefe', panel: '#0e2628' },
  navy: { start: '#15233f', end: '#4062a3', glow: '#deebff', line: '#f0f5ff', panel: '#0f182b' },
  sand: { start: '#4f3822', end: '#b48252', glow: '#ffe9c9', line: '#fff4e5', panel: '#2c1f13' },
  berry: { start: '#4f1930', end: '#a73b6e', glow: '#ffdceb', line: '#fff1f7', panel: '#2e1020' },
};

const COMMON_FALLBACK: Omit<DestinationMeta, 'image'> & { variant: Variant; artPalette: PaletteName } = {
  subtitle: 'Weekendowy city break',
  country: 'Europa',
  palette: 'green',
  landmark: 'Pocztówkowy kadr miasta',
  variant: 'generic',
  artPalette: 'emerald',
};

const RAW_META: Record<string, Omit<DestinationMeta, 'image'> & { variant: Variant; artPalette: PaletteName }> = {
  berlin: { subtitle: 'Kultura i energia metropolii', country: 'Niemcy', palette: 'blue', landmark: 'Brama Brandenburska', variant: 'brandenburg', artPalette: 'ocean' },
  drezno: { subtitle: 'Barok tuż za granicą', country: 'Niemcy', palette: 'sand', landmark: 'Frauenkirche', variant: 'castle', artPalette: 'sand' },
  praga: { subtitle: 'Romantyczne uliczki i panorama', country: 'Czechy', palette: 'rose', landmark: 'Most Karola', variant: 'charles', artPalette: 'rose' },
  wieden: { subtitle: 'Cesarski klimat na weekend', country: 'Austria', palette: 'gold', landmark: 'Katedra św. Szczepana', variant: 'castle', artPalette: 'gold' },
  budapeszt: { subtitle: 'Termy, Dunaj i nocne światła', country: 'Węgry', palette: 'violet', landmark: 'Parlament', variant: 'parliament', artPalette: 'violet' },
  bratyslawa: { subtitle: 'Mała stolica z dużym urokiem', country: 'Słowacja', palette: 'teal', landmark: 'Zamek Bratysławski', variant: 'castle', artPalette: 'teal' },
  brno: { subtitle: 'Morawski city break blisko Polski', country: 'Czechy', palette: 'teal', landmark: 'Zamek Špilberk', variant: 'castle', artPalette: 'teal' },
  londyn: { subtitle: 'Klasyk na szybki wypad', country: 'Wielka Brytania', palette: 'navy', landmark: 'Big Ben', variant: 'bigben', artPalette: 'navy' },
  paryz: { subtitle: 'Wieczorny blask nad Sekwaną', country: 'Francja', palette: 'rose', landmark: 'Wieża Eiffla', variant: 'eiffel', artPalette: 'rose' },
  rzym: { subtitle: 'Historia na każdym kroku', country: 'Włochy', palette: 'orange', landmark: 'Koloseum', variant: 'colosseum', artPalette: 'sunset' },
  mediolan: { subtitle: 'Moda i espresso w centrum Lombardii', country: 'Włochy', palette: 'lilac', landmark: 'Duomo di Milano', variant: 'duomo', artPalette: 'violet' },
  barcelona: { subtitle: 'Słońce, tapas i architektura Gaudíego', country: 'Hiszpania', palette: 'orange', landmark: 'Sagrada Família', variant: 'sagrada', artPalette: 'sunset' },
  amsterdam: { subtitle: 'Kanały, rowery i luźny klimat', country: 'Holandia', palette: 'blue', landmark: 'Kamienice nad kanałem', variant: 'canals', artPalette: 'ocean' },
  kopenhaga: { subtitle: 'Skandynawski minimalizm i hygge', country: 'Dania', palette: 'teal', landmark: 'Nyhavn', variant: 'oldtown', artPalette: 'teal' },
  sztokholm: { subtitle: 'Wyspy i elegancka północ', country: 'Szwecja', palette: 'ocean', landmark: 'Gamla Stan', variant: 'oldtown', artPalette: 'ocean' },
  oslo: { subtitle: 'Fiordowy oddech i nowoczesna architektura', country: 'Norwegia', palette: 'navy', landmark: 'Opera w Oslo', variant: 'opera', artPalette: 'navy' },
  dublin: { subtitle: 'Puby, muzyka i klimat miasta', country: 'Irlandia', palette: 'green', landmark: 'Most Ha’penny', variant: 'charles', artPalette: 'emerald' },
  malta: { subtitle: 'Słońce i kamienne uliczki Valletty', country: 'Malta', palette: 'gold', landmark: 'Panorama Valletty', variant: 'harbor', artPalette: 'gold' },
  split: { subtitle: 'Adriatyk i zabytkowe centrum', country: 'Chorwacja', palette: 'teal', landmark: 'Pałac Dioklecjana', variant: 'harbor', artPalette: 'teal' },
  zadar: { subtitle: 'Dalmatyński klimat na krótki urlop', country: 'Chorwacja', palette: 'ocean', landmark: 'Starówka nad Adriatykiem', variant: 'harbor', artPalette: 'ocean' },
  dubrownik: { subtitle: 'Mury, morze i zachody słońca', country: 'Chorwacja', palette: 'berry', landmark: 'Mury miejskie', variant: 'harbor', artPalette: 'berry' },
  sofia: { subtitle: 'Bałkański kierunek poza utartym szlakiem', country: 'Bułgaria', palette: 'violet', landmark: 'Sobór Aleksandra Newskiego', variant: 'castle', artPalette: 'violet' },
  gdansk: { subtitle: 'Nadmorski klasyk na szybki reset', country: 'Polska', palette: 'blue', landmark: 'Żuraw nad Motławą', variant: 'harbor', artPalette: 'ocean' },
  krakow: { subtitle: 'Rynek, Wawel i wieczorne spacery', country: 'Polska', palette: 'rose', landmark: 'Wawel', variant: 'castle', artPalette: 'rose' },
  warszawa: { subtitle: 'Nowoczesność i historia w jednym', country: 'Polska', palette: 'navy', landmark: 'Pałac Kultury i Nauki', variant: 'bigben', artPalette: 'navy' },
  poznan: { subtitle: 'Stary Rynek i koziołki', country: 'Polska', palette: 'orange', landmark: 'Ratusz w Poznaniu', variant: 'oldtown', artPalette: 'sunset' },
  szczecin: { subtitle: 'Portowy charakter zachodniej Polski', country: 'Polska', palette: 'teal', landmark: 'Wały Chrobrego', variant: 'harbor', artPalette: 'teal' },
  wroclaw: { subtitle: 'Mosty, rynek i klimat Dolnego Śląska', country: 'Polska', palette: 'green', landmark: 'Ratusz we Wrocławiu', variant: 'oldtown', artPalette: 'emerald' },
  monachium: { subtitle: 'Bawarski porządek i ogródki piwne', country: 'Niemcy', palette: 'ocean', landmark: 'Nowy Ratusz', variant: 'oldtown', artPalette: 'ocean' },
  frankfurt: { subtitle: 'Skyline i szybki miejski wypad', country: 'Niemcy', palette: 'navy', landmark: 'Wieżowce centrum', variant: 'generic', artPalette: 'navy' },
};

function normalizeKey(value: string): string {
  return value
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, ' ')
    .trim();
}

function keyCandidates(destination: string): string[] {
  const normalized = normalizeKey(destination);
  const parts = normalized.split(/\s+/).filter(Boolean);
  const withCommaSplit = normalized.split(',').map(part => part.trim()).filter(Boolean);
  return [...new Set([normalized, ...withCommaSplit, parts.slice(0, 2).join(' '), parts[0]].filter(Boolean))];
}

function paletteFor(palette: PaletteName): Palette {
  return PALETTES[palette] ?? PALETTES.emerald;
}

function iconPath(variant: Variant, line: string, glow: string): string {
  switch (variant) {
    case 'eiffel':
      return `
        <path d="M460 350 L520 170 L580 350" fill="none" stroke="${line}" stroke-width="14" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M485 280 H555" stroke="${line}" stroke-width="10" stroke-linecap="round"/>
        <path d="M474 312 H566" stroke="${line}" stroke-width="9" stroke-linecap="round"/>
        <path d="M452 350 H588" stroke="${line}" stroke-width="14" stroke-linecap="round"/>
        <circle cx="520" cy="146" r="12" fill="${glow}" />`;
    case 'bigben':
      return `
        <rect x="470" y="160" width="96" height="190" rx="8" fill="none" stroke="${line}" stroke-width="12"/>
        <rect x="492" y="122" width="52" height="38" rx="6" fill="none" stroke="${line}" stroke-width="10"/>
        <circle cx="518" cy="224" r="25" fill="none" stroke="${glow}" stroke-width="10"/>
        <path d="M518 224 L518 208 M518 224 L532 232" stroke="${glow}" stroke-width="8" stroke-linecap="round"/>
        <path d="M470 350 H566" stroke="${line}" stroke-width="14" stroke-linecap="round"/>`;
    case 'brandenburg':
      return `
        <path d="M442 214 L520 166 L598 214" fill="none" stroke="${line}" stroke-width="12" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M454 214 H586" stroke="${line}" stroke-width="12"/>
        <path d="M468 214 V344 M496 214 V344 M544 214 V344 M572 214 V344" stroke="${line}" stroke-width="10" stroke-linecap="round"/>
        <path d="M452 344 H588" stroke="${line}" stroke-width="14" stroke-linecap="round"/>
        <path d="M510 154 H530" stroke="${glow}" stroke-width="9" stroke-linecap="round"/>`;
    case 'colosseum':
      return `
        <path d="M432 336 Q520 138 608 336" fill="none" stroke="${line}" stroke-width="14" stroke-linecap="round"/>
        <path d="M456 312 V260 M490 298 V246 M520 292 V236 M550 298 V246 M584 312 V260" stroke="${glow}" stroke-width="10" stroke-linecap="round"/>
        <path d="M444 350 H596" stroke="${line}" stroke-width="14" stroke-linecap="round"/>
        <path d="M458 260 Q470 244 482 260 M492 246 Q504 230 516 246 M524 236 Q536 220 548 236 M558 246 Q570 230 582 246" fill="none" stroke="${glow}" stroke-width="8" stroke-linecap="round"/>`;
    case 'duomo':
      return `
        <path d="M446 350 H594" stroke="${line}" stroke-width="14" stroke-linecap="round"/>
        <path d="M468 350 V224 L520 176 L572 224 V350" fill="none" stroke="${line}" stroke-width="12" stroke-linejoin="round"/>
        <path d="M520 176 V136" stroke="${glow}" stroke-width="10" stroke-linecap="round"/>
        <path d="M482 224 L482 188 M520 208 L520 176 M558 224 L558 188" stroke="${glow}" stroke-width="8" stroke-linecap="round"/>
        <path d="M490 264 H550" stroke="${glow}" stroke-width="8" stroke-linecap="round"/>`;
    case 'charles':
      return `
        <path d="M430 324 Q520 260 610 324" fill="none" stroke="${line}" stroke-width="14" stroke-linecap="round"/>
        <path d="M444 324 H596" stroke="${line}" stroke-width="14" stroke-linecap="round"/>
        <path d="M458 324 Q478 284 498 324 M500 324 Q520 274 540 324 M542 324 Q562 286 582 324" fill="none" stroke="${glow}" stroke-width="9" stroke-linecap="round"/>
        <path d="M454 296 V226 M584 296 V226" stroke="${line}" stroke-width="12" stroke-linecap="round"/>
        <path d="M444 226 H464 M574 226 H594" stroke="${line}" stroke-width="12" stroke-linecap="round"/>`;
    case 'sagrada':
      return `
        <path d="M444 350 H596" stroke="${line}" stroke-width="14" stroke-linecap="round"/>
        <path d="M464 350 V230 M502 350 V190 M540 350 V214 M576 350 V248" stroke="${line}" stroke-width="12" stroke-linecap="round"/>
        <path d="M464 230 L480 202 M502 190 L518 156 M540 214 L556 180 M576 248 L590 224" stroke="${glow}" stroke-width="8" stroke-linecap="round"/>
        <circle cx="480" cy="200" r="7" fill="${glow}"/><circle cx="518" cy="154" r="7" fill="${glow}"/><circle cx="556" cy="178" r="7" fill="${glow}"/><circle cx="590" cy="222" r="7" fill="${glow}"/>`;
    case 'canals':
      return `
        <path d="M420 320 H620" stroke="${line}" stroke-width="12" stroke-linecap="round"/>
        <path d="M448 320 V246 H486 V320 M500 320 V222 H548 V320 M562 320 V236 H602 V320" fill="none" stroke="${line}" stroke-width="10" stroke-linejoin="round"/>
        <path d="M438 352 Q478 338 518 352 T598 352" fill="none" stroke="${glow}" stroke-width="9" stroke-linecap="round"/>
        <path d="M458 246 L468 228 L478 246 M514 222 L524 202 L534 222 M574 236 L584 216 L594 236" fill="none" stroke="${glow}" stroke-width="8" stroke-linecap="round" stroke-linejoin="round"/>`;
    case 'parliament':
      return `
        <path d="M436 350 H604" stroke="${line}" stroke-width="14" stroke-linecap="round"/>
        <path d="M458 350 V238 H582 V350" fill="none" stroke="${line}" stroke-width="12" stroke-linejoin="round"/>
        <path d="M520 238 V164" stroke="${glow}" stroke-width="10" stroke-linecap="round"/>
        <path d="M480 238 V196 M520 238 V188 M560 238 V206" stroke="${glow}" stroke-width="8" stroke-linecap="round"/>
        <path d="M446 238 H594" stroke="${line}" stroke-width="10"/>
        <path d="M470 282 H570" stroke="${glow}" stroke-width="8" stroke-linecap="round"/>`;
    case 'castle':
      return `
        <path d="M440 350 H600" stroke="${line}" stroke-width="14" stroke-linecap="round"/>
        <path d="M456 350 V226 H584 V350" fill="none" stroke="${line}" stroke-width="12" stroke-linejoin="round"/>
        <path d="M456 226 L478 202 L500 226 L520 196 L540 226 L562 202 L584 226" fill="none" stroke="${glow}" stroke-width="8" stroke-linejoin="round"/>
        <path d="M496 350 V280 H544 V350" stroke="${glow}" stroke-width="9" stroke-linecap="round"/>
        <path d="M476 270 H564" stroke="${glow}" stroke-width="8" stroke-linecap="round"/>`;
    case 'oldtown':
      return `
        <path d="M438 350 H602" stroke="${line}" stroke-width="14" stroke-linecap="round"/>
        <path d="M456 350 V246 H492 V350 M502 350 V222 H546 V350 M556 350 V236 H588 V350" fill="none" stroke="${line}" stroke-width="10" stroke-linejoin="round"/>
        <path d="M474 246 L474 226 M524 222 L524 194 M572 236 L572 216" stroke="${glow}" stroke-width="8" stroke-linecap="round"/>
        <path d="M456 246 L474 224 L492 246 M502 222 L524 192 L546 222 M556 236 L572 214 L588 236" fill="none" stroke="${glow}" stroke-width="8" stroke-linejoin="round"/>`;
    case 'harbor':
      return `
        <path d="M430 334 H610" stroke="${line}" stroke-width="12" stroke-linecap="round"/>
        <path d="M444 350 Q482 338 520 350 T596 350" fill="none" stroke="${glow}" stroke-width="9" stroke-linecap="round"/>
        <path d="M468 334 V236 H552" stroke="${line}" stroke-width="10" stroke-linecap="round"/>
        <path d="M552 236 L580 256 L552 276" fill="none" stroke="${line}" stroke-width="10" stroke-linejoin="round"/>
        <path d="M510 334 V266 M524 334 V286" stroke="${glow}" stroke-width="8" stroke-linecap="round"/>
        <path d="M470 284 H520" stroke="${glow}" stroke-width="8" stroke-linecap="round"/>`;
    case 'opera':
      return `
        <path d="M434 348 H606" stroke="${line}" stroke-width="14" stroke-linecap="round"/>
        <path d="M450 348 L500 230 L580 230 L592 348" fill="none" stroke="${line}" stroke-width="12" stroke-linejoin="round"/>
        <path d="M470 308 H572" stroke="${glow}" stroke-width="8" stroke-linecap="round"/>
        <path d="M438 360 Q478 346 518 360 T598 360" fill="none" stroke="${glow}" stroke-width="8" stroke-linecap="round"/>`;
    case 'beach':
      return `
        <path d="M430 352 H610" stroke="${line}" stroke-width="12" stroke-linecap="round"/>
        <path d="M448 352 Q485 324 520 352 T592 352" fill="none" stroke="${glow}" stroke-width="8" stroke-linecap="round"/>
        <path d="M496 350 V220" stroke="${line}" stroke-width="12" stroke-linecap="round"/>
        <path d="M496 220 Q540 236 564 270" fill="none" stroke="${glow}" stroke-width="9" stroke-linecap="round"/>
        <path d="M496 248 Q464 228 446 196" fill="none" stroke="${glow}" stroke-width="9" stroke-linecap="round"/>
        <circle cx="590" cy="118" r="24" fill="${glow}" opacity="0.95"/>`;
    default:
      return `
        <path d="M430 350 H610" stroke="${line}" stroke-width="14" stroke-linecap="round"/>
        <path d="M446 350 V252 H484 V350 M500 350 V220 H544 V350 M560 350 V242 H594 V350" fill="none" stroke="${line}" stroke-width="10" stroke-linejoin="round"/>
        <path d="M446 252 L465 228 L484 252 M500 220 L522 188 L544 220 M560 242 L578 214 L594 242" fill="none" stroke="${glow}" stroke-width="8" stroke-linejoin="round"/>`;
  }
}

function svgFor(destination: string, landmark: string, variant: Variant, paletteName: PaletteName): string {
  const palette = paletteFor(paletteName);
  const safeDestination = escapeXml(destination);
  const safeLandmark = escapeXml(landmark);
  const svg = `
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1040 660" role="img" aria-label="${safeDestination}">
    <defs>
      <linearGradient id="bg" x1="0" x2="1" y1="0" y2="1">
        <stop offset="0%" stop-color="${palette.start}" />
        <stop offset="100%" stop-color="${palette.end}" />
      </linearGradient>
      <linearGradient id="haze" x1="0" x2="0" y1="0" y2="1">
        <stop offset="0%" stop-color="#ffffff" stop-opacity="0.35" />
        <stop offset="100%" stop-color="#ffffff" stop-opacity="0" />
      </linearGradient>
    </defs>
    <rect width="1040" height="660" rx="0" fill="url(#bg)"/>
    <circle cx="816" cy="132" r="78" fill="${palette.glow}" opacity="0.94"/>
    <circle cx="180" cy="106" r="34" fill="#ffffff" opacity="0.18"/>
    <circle cx="248" cy="84" r="12" fill="#ffffff" opacity="0.26"/>
    <path d="M0 438 C180 388 296 438 450 400 C646 352 774 420 1040 356 V660 H0 Z" fill="#ffffff" opacity="0.09"/>
    <path d="M0 484 C168 456 316 496 484 452 C660 406 850 472 1040 430 V660 H0 Z" fill="#ffffff" opacity="0.10"/>
    <g transform="translate(0 -10)">${iconPath(variant, palette.line, palette.glow)}</g>
    <rect x="44" y="42" width="262" height="42" rx="21" fill="${palette.panel}" fill-opacity="0.56"/>
    <text x="66" y="69" font-size="22" font-weight="700" fill="#ffffff" font-family="Inter, Arial, sans-serif">${safeLandmark}</text>
    <text x="58" y="586" font-size="72" font-weight="800" fill="#ffffff" letter-spacing="-1.8" font-family="Inter, Arial, sans-serif">${safeDestination}</text>
    <text x="60" y="622" font-size="24" font-weight="600" fill="rgba(255,255,255,0.88)" font-family="Inter, Arial, sans-serif">Travel Finder • inspiracja miejscem</text>
    <rect x="58" y="538" width="168" height="6" rx="3" fill="url(#haze)" opacity="0.72"/>
  </svg>`;
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
}

function escapeXml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

export function resolveDestinationMeta(destination: string): DestinationMeta {
  const candidates = keyCandidates(destination);
  const raw = candidates.map(candidate => RAW_META[candidate]).find(Boolean) ?? COMMON_FALLBACK;
  return {
    subtitle: raw.subtitle,
    country: raw.country,
    palette: raw.palette,
    landmark: raw.landmark,
    image: svgFor(destination, raw.landmark, raw.variant, raw.artPalette),
  };
}
