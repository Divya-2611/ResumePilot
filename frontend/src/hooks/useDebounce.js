import { useEffect, useState } from 'react';

/**
 * Debounces a value (search inputs, editors) - delays state updates until
 * the user stops typing for {@code delay} ms.
 */
export function useDebounce(value, delay = 400) {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);

  return debounced;
}
