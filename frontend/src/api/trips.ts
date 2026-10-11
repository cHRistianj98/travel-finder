import type { TripOption, TripSearchRequest } from '../features/trip-search/types';

export async function searchTrips(request: TripSearchRequest): Promise<TripOption[]> {
  let response: Response;
  try {
    response = await fetch('/api/trips/search', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });
  } catch {
    throw new Error('Brak połączenia z backendem. Sprawdź, czy Spring Boot działa na porcie 8081.');
  }

  if (!response.ok) {
    const payload: unknown = await response.json().catch(() => null);
    const message = payload && typeof payload === 'object' && 'message' in payload
      && typeof payload.message === 'string'
      ? payload.message : 'Nie udało się wyszukać podróży. Spróbuj ponownie.';
    throw new Error(message);
  }

  return (await response.json()) as TripOption[];
}
