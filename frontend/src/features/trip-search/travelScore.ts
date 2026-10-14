import type { TripOption, TripSearchRequest } from './types';

export type TravelScoreProfile = 'BALANCED' | 'BUDGET' | 'WEEKEND' | 'COMFORT';
export type TripSort = 'SCORE' | 'PRICE' | 'STAY' | 'DURATION';

export interface TravelScoreCriterion {
  key: string;
  label: string;
  maxPoints: number;
  points: number | null;
  explanation: string;
}

export interface TravelScore {
  value: number;
  availablePoints: number;
  unknownPoints: number;
  partial: boolean;
  profile: TravelScoreProfile;
  criteria: TravelScoreCriterion[];
  strengths: string[];
  cautions: string[];
  distanceKm: number | null;
  weekendOrHolidayDays: number;
  usefulDaylightHours: number;
  attractionExamples: string[];
}

export interface ScoredTrip {
  trip: TripOption;
  score: TravelScore;
}

export const SCORE_PROFILES: Array<{ id: TravelScoreProfile; label: string; description: string; icon: 'sparkles' | 'wallet' | 'calendar' | 'navigation' }> = [
  { id: 'BALANCED', label: 'Zbalansowany', description: 'Równowaga ceny, czasu i wygody', icon: 'sparkles' },
  { id: 'BUDGET', label: 'Najtaniej', description: 'Najważniejsza cena podróży', icon: 'wallet' },
  { id: 'WEEKEND', label: 'Efektywny weekend', description: 'Maksimum zwiedzania i czasu wolnego', icon: 'calendar' },
  { id: 'COMFORT', label: 'Wygodnie', description: 'Przyjazne godziny i mało przesiadek', icon: 'navigation' },
];

export async function scoreTrips(
  request: TripSearchRequest,
  trips: TripOption[],
  profile: TravelScoreProfile,
  signal?: AbortSignal,
): Promise<ScoredTrip[]> {
  const response = await fetch('/api/trips/score', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ request, trips, profile }),
    signal,
  });
  if (!response.ok) throw new Error('Ocena podróży jest tymczasowo niedostępna.');
  const data: unknown = await response.json();
  if (!Array.isArray(data) || data.length !== trips.length) throw new Error('Otrzymano niepełne oceny podróży.');
  return data as ScoredTrip[];
}

export function sortTrips(
  trips: TripOption[], scored: ScoredTrip[], sort: TripSort,
): Array<{ trip: TripOption; score?: TravelScore }> {
  const scoreMap = new Map(scored.map(item => [item.trip.id, item.score]));
  return trips.map(trip => ({ trip, score: scoreMap.get(trip.id) }))
    .sort((a, b) => {
      if (sort === 'PRICE') {
        if (a.trip.price === null) return b.trip.price === null ? fallback(a,b) : 1;
        if (b.trip.price === null) return -1;
        if (a.trip.price !== b.trip.price) return a.trip.price - b.trip.price;
      }
      if (sort === 'STAY' && a.trip.timeAtDestinationMinutes !== b.trip.timeAtDestinationMinutes) {
        return b.trip.timeAtDestinationMinutes - a.trip.timeAtDestinationMinutes;
      }
      if (sort === 'DURATION' && a.trip.totalTravelDurationMinutes !== b.trip.totalTravelDurationMinutes) {
        return a.trip.totalTravelDurationMinutes - b.trip.totalTravelDurationMinutes;
      }
      if (a.score && b.score && a.score.value !== b.score.value) return b.score.value - a.score.value;
      if (a.score && !b.score) return -1;
      if (b.score && !a.score) return 1;
      return fallback(a, b);
    });
}

function fallback(
  a: { trip: TripOption }, b: { trip: TripOption },
): number {
  return b.trip.timeAtDestinationMinutes - a.trip.timeAtDestinationMinutes
    || a.trip.destination.localeCompare(b.trip.destination, 'pl');
}
