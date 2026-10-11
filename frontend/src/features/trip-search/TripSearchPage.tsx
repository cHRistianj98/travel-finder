import { useState } from 'react';
import { searchTrips } from '../../api/trips';
import { TripSearchForm } from './TripSearchForm';
import { TripResults, type SearchStatus } from './TripResults';
import type { TripOption, TripSearchRequest } from './types';

export function TripSearchPage() {
  const [results, setResults] = useState<TripOption[]>([]);
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

  return <div className="discovery-background"><div className="container discovery-layout"><TripSearchForm onSearch={request => { void handleSearch(request); }} loading={status === 'loading'} /><TripResults status={status} results={results} error={error} searchedRequest={searchedRequest} onRetry={() => { if (searchedRequest) void handleSearch(searchedRequest); }} /></div></div>;
}
