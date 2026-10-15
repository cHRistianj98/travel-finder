import { useEffect, useRef, useState } from 'react';
import { getCarVisual, type CarVisual } from './carAttractionVisuals';

/** Źródłowe dane autora i licencji pozostają związane z każdą fotografią Commons. */
interface CommonsPhoto {
  url: string;
  author: string;
  license: string;
  licenseUrl: string;
  source: string;
}

interface WikiImageInfo {
  thumburl?: string;
  url?: string;
  descriptionurl?: string;
  width?: number;
  height?: number;
  mime?: string;
  extmetadata?: Record<string, { value?: string }>;
}

interface WikiPage {
  title?: string;
  imageinfo?: WikiImageInfo[];
}

const cachedRequests = new Map<string, Promise<CommonsPhoto | null>>();
let activeRequests = 0;
const MAX_PARALLEL = 3;
const waiters: Array<() => void> = [];

async function throttled<T>(action: () => Promise<T>): Promise<T> {
  if (activeRequests >= MAX_PARALLEL) await new Promise<void>(resolve => waiters.push(resolve));
  activeRequests++;
  try { return await action(); }
  finally {
    activeRequests--;
    waiters.shift()?.();
  }
}

function normalize(value: string) {
  return value.toLowerCase().replace(/ł/g, 'l').normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9 ]/g, ' ');
}

function cleanText(value: string | undefined): string {
  return (value ?? '').replace(/<[^>]*>/g, '').replace(/&nbsp;|&#160;/gi, ' ')
    .replace(/&amp;/gi, '&').replace(/&quot;/gi, '"').replace(/&#39;/gi, "'")
    .replace(/\s+/g, ' ').trim().slice(0, 110);
}

function rankPhoto(page: WikiPage, query: string): number {
  const image = page.imageinfo?.[0];
  if (!image?.thumburl || !image.width || !image.height) return -100;
  const title = normalize(page.title ?? '');
  if (!/\.(jpg|jpeg|webp|png|tif|tiff)$/i.test(page.title ?? '')) return -100;
  if (/map|logo|flag|coat of arms|diagram|plan |schemat|herb|icon|poster|drawing/i.test(title)) return -100;
  const significant = normalize(query).split(/\s+/).filter(term => term.length > 3 &&
    !['castle', 'poland', 'czechia', 'germany', 'church', 'square', 'park', 'palace', 'bridge'].includes(term));
  const matches = significant.filter(term => title.includes(term)).length;
  const ratio = image.width / image.height;
  const photoBonus = /\.(jpg|jpeg)$/i.test(page.title ?? '') ? 2 : 0;
  return matches * 3 + photoBonus + (ratio >= 1.2 && ratio <= 3.5 ? 2 : -1)
    + (image.width > 700 ? 1 : 0);
}

async function searchCommons(query: string): Promise<CommonsPhoto | null> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 8000);
  try {
    const params = new URLSearchParams({
      action: 'query', generator: 'search', gsrsearch: query,
      gsrnamespace: '6', gsrlimit: '12',
      prop: 'imageinfo', iiprop: 'url|size|mime|extmetadata', iiurlwidth: '960',
      format: 'json', origin: '*',
    });
    const response = await fetch(`https://commons.wikimedia.org/w/api.php?${params}`, {
      signal: controller.signal, mode: 'cors', credentials: 'omit',
    });
    if (!response.ok) return null;
    const data: { query?: { pages?: Record<string, WikiPage> } } = await response.json();
    const pages = Object.values(data.query?.pages ?? {});
    const best = pages.sort((a, b) => rankPhoto(b, query) - rankPhoto(a, query))[0];
    if (!best || rankPhoto(best, query) < 1) return null;
    const image = best.imageinfo?.[0];
    if (!image?.thumburl || !image.descriptionurl) return null;
    const meta = image.extmetadata ?? {};
    const author = cleanText(meta.Artist?.value || meta.Credit?.value) || 'autor podany w Wikimedia Commons';
    const license = cleanText(meta.LicenseShortName?.value) || 'licencja na stronie pliku';
    // Brak jawnej informacji licencyjnej -> nie pokazujemy obrazu jako poprawnie przypisanego.
    if (!meta.LicenseShortName?.value && !meta.LicenseUrl?.value) return null;
    return { url: image.thumburl, author, license,
      licenseUrl: meta.LicenseUrl?.value?.startsWith('https://') ? meta.LicenseUrl.value : image.descriptionurl,
      source: image.descriptionurl };
  } catch { return null; }
  finally { clearTimeout(timer); }
}

function loadCommonsPhoto(query: string): Promise<CommonsPhoto | null> {
  const key = query.toLowerCase();
  const existing = cachedRequests.get(key);
  if (existing) return existing;
  const promise = throttled(() => searchCommons(query));
  cachedRequests.set(key, promise);
  return promise;
}

interface Props {
  destination: string;
  landmark: string;
  className?: string;
}

/**
 * Zdjęcie dla konkretnej atrakcji samochodowej.
 * 27 lokalnych panoram: bez dodatkowych zapytań.
 * Reszta: leniwe pobieranie fotografii z Commons wraz z autorstwem/licencją.
 * Każde miejsce ma odrębną frazę wyszukiwania i wizualizację zastępczą.
 */
export function CarAttractionCover({ destination, landmark, className }: Props) {
  const rootRef = useRef<HTMLDivElement | null>(null);
  const visual: CarVisual | undefined = getCarVisual(destination);
  const [photo, setPhoto] = useState<CommonsPhoto | null>(null);
  const [imageFailed, setImageFailed] = useState(false);

  useEffect(() => {
    setPhoto(null);
    setImageFailed(false);
    if (!visual?.commonsQuery) return;
    let alive = true;
    const node = rootRef.current;
    if (!node) return;
    let launched = false;
    const load = () => {
      if (launched) return;
      launched = true;
      void loadCommonsPhoto(visual.commonsQuery!).then(result => {
        if (alive && result) setPhoto(result);
      });
    };
    if (typeof IntersectionObserver === 'undefined') load();
    else {
      const observer = new IntersectionObserver(entries => {
        if (entries.some(entry => entry.isIntersecting)) { load(); observer.disconnect(); }
      }, { rootMargin: '500px' });
      observer.observe(node);
      return () => { alive = false; observer.disconnect(); };
    }
    return () => { alive = false; };
  }, [visual?.commonsQuery, destination]);

  const cover = !imageFailed && photo ? photo.url : visual?.local ?? visual?.fallback ?? '';
  const isRealPhoto = Boolean(photo && !imageFailed);
  return (
    <div ref={rootRef} className={`car-attraction-cover ${className ?? ''}`}>
      {cover ? <img src={cover} alt={isRealPhoto ? `Fotografia: ${destination} — ${landmark}` : `Wizualizacja AI (poglądowa): ${destination}`}
        loading="lazy" decoding="async" onError={() => setImageFailed(true)} />
        : <div className="car-attraction-cover-placeholder" aria-label={`Brak fotografii: ${destination}`} />}
      {isRealPhoto ? <span className="car-attraction-image-source" title={`Zdjęcie: ${photo!.author}; ${photo!.license}; Wikimedia Commons`}>
          <a href={photo!.source} target="_blank" rel="noopener noreferrer">Fot. {photo!.author}</a>
          <span aria-hidden="true">·</span>
          <a href={photo!.licenseUrl} target="_blank" rel="noopener noreferrer">{photo!.license}</a>
        </span>
        : <span className="car-attraction-image-source car-attraction-ai-label" title="Kadr wygenerowany przez AI. Może nie odzwierciedlać dokładnego wyglądu obiektu.">Wizualizacja AI</span>}
    </div>
  );
}
