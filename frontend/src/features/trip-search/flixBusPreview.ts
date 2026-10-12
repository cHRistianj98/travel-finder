/**
 * Opcjonalny publiczny podgląd minimalnych cen publikowanych na stronach relacji
 * oraz deeplinki wyszukiwania (miasta + dzień). Nie są to ceny wybranych kursów GTFS.
 */
export interface FlixBusPublicPreview {
  outboundFromPln: number | null;
  returnFromPln: number | null;
  roundTripFromPln: number | null;
  outboundSearchUrl: string;
  returnSearchUrl: string;
  searchPrefilled: boolean;
  priceKind: 'PUBLIC_ROUTE_MINIMUM' | 'UNAVAILABLE' | 'DISABLED';
  checkedAt: string | null;
}

const cached = new Map<string, { promise: Promise<FlixBusPublicPreview>; created: number }>();

export async function fetchFlixBusPreview(
  from: string,
  to: string,
  departureDate: string,
  returnDate: string,
): Promise<FlixBusPublicPreview> {
  const params = new URLSearchParams({ from, to, departureDate: departureDate.slice(0, 10), returnDate: returnDate.slice(0, 10) });
  const key = params.toString();
  const now = Date.now();
  const existing = cached.get(key);
  if (existing && now - existing.created < 30 * 60_000) return existing.promise;

  const promise = fetch(`/api/trips/bus/preview?${params}`)
    .then(async response => {
      if (!response.ok) throw new Error('Brak publicznych cen FlixBusa');
      return await response.json() as FlixBusPublicPreview;
    })
    .catch(error => {
      cached.delete(key);
      throw error;
    });
  cached.set(key, { promise, created: now });
  if (cached.size > 400) {
    for (const [oldKey, value] of cached) if (now - value.created > 30 * 60_000) cached.delete(oldKey);
  }
  return promise;
}
