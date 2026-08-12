import { useEffect, useState } from 'react';

/**
 * Fetches data with loading/error state and re-fetch capability.
 * Keeps pages declarative: const { data, loading, error, refetch } = useFetch(() => api(...), [deps]);
 */
export function useFetch(fetcher, deps = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [tick, setTick] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    fetcher()
      .then((result) => {
        if (!cancelled) setData(result);
      })
      .catch((err) => {
        if (!cancelled) setError(err);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [...deps, tick]); // eslint-disable-line react-hooks/exhaustive-deps

  return { data, loading, error, refetch: () => setTick((t) => t + 1) };
}
