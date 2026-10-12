import { useEffect, useState } from 'react';
import { searchTrips } from '../../api/trips';
import { TripSearchForm } from './TripSearchForm';
import { TripResults, type SearchStatus } from './TripResults';
import type { TripOption, TripSearchRequest } from './types';

export function TripSearchPage() {
  const [trainAccess, setTrainAccess] = useState<'checking' | 'pending' | 'active' | 'unavailable'>('checking');
  const [results, setResults] = useState<TripOption[]>([]);

  useEffect(() => {
    const controller = new AbortController();
    fetch('/api/trips/providers', { signal: controller.signal })
      .then(response => {
        if (!response.ok) throw new Error('Status serwera niedostępny');
        return response.json() as Promise<{ trainAvailable: boolean }>;
      })
      .then(providers => { if (!controller.signal.aborted) setTrainAccess(providers.trainAvailable ? 'active' : 'pending'); })
      .catch(() => { if (!controller.signal.aborted) setTrainAccess('unavailable'); });
    return () => controller.abort();
  }, []);
  const [status, setStatus] = useState<SearchStatus>('idle');
  const [error, setError] = useState<string | null>(null);
  const [searchedRequest, setSearchedRequest] = useState<TripSearchRequest | null>(null);

  async function handleSearch(request: TripSearchRequest) {
    setSearchedRequest(request);
    setStatus('loading');
    setError(null);
    try {
      const trips = await searchTrips(request);
      setResults(trips);
      setStatus('success');
    } catch (cause) {
      setResults([]);
      setStatus('error');
      setError(cause instanceof Error ? cause.message : 'Wystąpił niespodziewany błąd.');
    }
  }

  return <div className="discovery-background"><div className="container discovery-layout"><TripSearchForm onSearch={request => { void handleSearch(request); }} loading={status === 'loading'} trainAccess={trainAccess} /><TripResults status={status} results={results} error={error} searchedRequest={searchedRequest} trainAccess={trainAccess} onRetry={() => { if (searchedRequest) void handleSearch(searchedRequest); }} /></div></div>;
}
