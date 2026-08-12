import { Moon, Sun } from 'lucide-react';
import { useTheme } from '../../hooks/useTheme';
import Tooltip from '../common/Tooltip';

/**
 * Dark / light mode toggle.
 */
export default function ThemeToggle() {
  const { theme, toggleTheme } = useTheme();
  return (
    <Tooltip label={theme === 'dark' ? 'Light mode' : 'Dark mode'}>
      <button
        onClick={toggleTheme}
        className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-700 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-200"
        aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
        title={theme === 'dark' ? 'Light mode' : 'Dark mode'}
      >
        {theme === 'dark' ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
      </button>
    </Tooltip>
  );
}
