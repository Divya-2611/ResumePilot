import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';

/**
 * Dark/light theme. Persisted to localStorage; defaults to system preference.
 * Toggles the `dark` class on <html> for Tailwind's `darkMode: 'class'`.
 */
export const ThemeContext = createContext(null);

function initialTheme() {
  const stored = localStorage.getItem('theme');
  if (stored) {
    return stored;
  }
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(initialTheme);

  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark');
    localStorage.setItem('theme', theme);
  }, [theme]);

  const toggleTheme = useCallback(() => {
    setTheme((current) => (current === 'dark' ? 'light' : 'dark'));
  }, []);

  const value = useMemo(() => ({ theme, toggleTheme }), [theme, toggleTheme]);

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within ThemeProvider');
  }
  return context;
}
