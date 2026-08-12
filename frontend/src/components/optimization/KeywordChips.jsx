import { Check, X } from 'lucide-react';

/**
 * Keyword match visualization: green = matched, red = missing.
 */
export default function KeywordChips({ matched = [], missing = [] }) {
  return (
    <div className="space-y-3">
      {matched.length > 0 && (
        <div>
          <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-emerald-600 dark:text-emerald-400">
            Matched ({matched.length})
          </p>
          <div className="flex flex-wrap gap-1.5">
            {matched.map((keyword) => (
              <span
                key={keyword}
                className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300"
              >
                <Check className="h-3 w-3" />
                {keyword}
              </span>
            ))}
          </div>
        </div>
      )}
      {missing.length > 0 && (
        <div>
          <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-red-600 dark:text-red-400">
            Missing ({missing.length})
          </p>
          <div className="flex flex-wrap gap-1.5">
            {missing.map((keyword) => (
              <span
                key={keyword}
                className="inline-flex items-center gap-1 rounded-full bg-red-50 px-2.5 py-1 text-xs font-medium text-red-700 dark:bg-red-900/40 dark:text-red-300"
              >
                <X className="h-3 w-3" />
                {keyword}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
