import { ChevronLeft, ChevronRight } from 'lucide-react';
import Tooltip from './Tooltip';

/**
 * Pagination control bound to a Spring Data page object.
 */
export default function Pagination({ page, totalPages, onPageChange }) {
  if (!totalPages || totalPages <= 1) return null;

  const pages = [];
  const start = Math.max(0, page - 2);
  const end = Math.min(totalPages - 1, page + 2);
  for (let i = start; i <= end; i += 1) {
    pages.push(i);
  }

  return (
    <nav className="flex items-center justify-center gap-1 pt-4" aria-label="Pagination">
      <Tooltip label="Previous page">
        <button
          className="btn-secondary px-2.5 py-2"
          disabled={page === 0}
          onClick={() => onPageChange(page - 1)}
          aria-label="Previous page"
        >
          <ChevronLeft className="h-4 w-4" />
        </button>
      </Tooltip>
      {pages[0] > 0 && <span className="px-2 text-sm text-slate-400">…</span>}
      {pages.map((p) => (
        <button
          key={p}
          onClick={() => onPageChange(p)}
          className={`px-3.5 py-2 text-sm font-medium rounded-lg transition-colors ${
            p === page
              ? 'bg-brand-600 text-white'
              : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
          }`}
          aria-current={p === page ? 'page' : undefined}
        >
          {p + 1}
        </button>
      ))}
      {pages[pages.length - 1] < totalPages - 1 && (
        <span className="px-2 text-sm text-slate-400">…</span>
      )}
      <Tooltip label="Next page">
        <button
          className="btn-secondary px-2.5 py-2"
          disabled={page >= totalPages - 1}
          onClick={() => onPageChange(page + 1)}
          aria-label="Next page"
        >
          <ChevronRight className="h-4 w-4" />
        </button>
      </Tooltip>
    </nav>
  );
}
