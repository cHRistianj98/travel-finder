import { useEffect, useRef, useState } from 'react';
import { searchTrips } from '../../api/trips';
import { TripSearchForm } from './TripSearchForm';
import { TripResults, type SearchStatus } from './TripResults';
import type { TripOption, TripSearchRequest } from './types';
import { scoreTrips, type ScoredTrip, type TravelScoreProfile, type TripSort } from './travelScore';

export function TripSearchPage() {
  const [trainAccess, setTrainAccess] = useState<'checking' | 'pending' | 'active' | 'unavailable'>('checking');
  const [busAccess, setBusAccess] = useState(false);
  const [results, setResults] = useState<TripOption[]>([]);
  const [scores, setScores] = useState<ScoredTrip[]>([]);
  const [profile, setProfile] = useState<TravelScoreProfile>('BALANCED');
  const [sort, setSort] = useState<TripSort>('SCORE');
  const [scoreBusy, setScoreBusy] = useState(false);
  const [scoreError, setScoreError] = useState<string | null>(null);
  const scoreController = useRef<AbortController | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    fetch('/api/trips/providers', { signal: controller.signal })
      .then(response => {
        if (!response.ok) throw new Error('Status serwera niedostępny');
        return response.json() as Promise<{ trainAvailable: boolean; busAvailable?: boolean }>;
      })
      .then(providers => { if (!controller.signal.aborted) { setTrainAccess(providers.trainAvailable ? 'active' : 'pending'); setBusAccess(providers.busAvailable === true); } })
      .catch(() => { if (!controller.signal.aborted) { setTrainAccess('unavailable'); setBusAccess(false); } });
    return () => controller.abort();
  }, []);
  const [status, setStatus] = useState<SearchStatus>('idle');
  const [error, setError] = useState<string | null>(null);
  const [searchedRequest, setSearchedRequest] = useState<TripSearchRequest | null>(null);

  // Zmiana profilu tylko przelicza ranking: nie pobieramy ponownie ofert z providerów.
  async function rank(request: TripSearchRequest, trips: TripOption[], selected: TravelScoreProfile) {
    scoreController.current?.abort();
    const controller = new AbortController();
    scoreController.current = controller;
    setScores([]);
    setScoreBusy(true);
    setScoreError(null);
    try {
      const ranked = await scoreTrips(request, trips, selected, controller.signal);
      if (!controller.signal.aborted) setScores(ranked);
    } catch {
      if (!controller.signal.aborted) setScoreError('Nie udało się obliczyć punktów. Wyniki pozostają dostępne.');
    } finally {
      if (!controller.signal.aborted) setScoreBusy(false);
    }
  }

  async function handleSearch(request: TripSearchRequest) {
    scoreController.current?.abort();
    setSearchedRequest(request);
    setStatus('loading');
    setResults([]);
    setScores([]);
    setScoreError(null);
    setError(null);
    try {
      const trips = await searchTrips(request);
      setResults(trips);
      setStatus('success');
      if (trips.length) await rank(request, trips, profile);
    } catch (cause) {
      setResults([]);
      setStatus('error');
      setError(cause instanceof Error ? cause.message : 'Wystąpił niespodziewany błąd.');
    }
  }

  function handleProfileChange(next: TravelScoreProfile) {
    if (next === profile) return;
    setProfile(next);
    if (searchedRequest && results.length > 0) void rank(searchedRequest, results, next);
  }

  return <div className="discovery-background"><div className="container discovery-layout">
    <TripSearchForm onSearch={request => { void handleSearch(request); }} loading={status === 'loading'} trainAccess={trainAccess} busAccess={busAccess} />
    <TripResults status={status} results={results} scores={scores} error={error} searchedRequest={searchedRequest}
      profile={profile} onProfileChange={handleProfileChange} sort={sort} onSortChange={setSort}
      scoreBusy={scoreBusy} scoreError={scoreError}
      trainAccess={trainAccess} busAccess={busAccess}
      onRetry={() => { if (searchedRequest) void handleSearch(searchedRequest); }} />
  </div></div>;
}
