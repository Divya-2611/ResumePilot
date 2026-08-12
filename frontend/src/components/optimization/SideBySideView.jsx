import { useState } from 'react';

/**
 * Side-by-side original vs optimized comparison with a highlighted keyword
 * legend. Toggleable to a single-pane diff view.
 */
export default function SideBySideView({ original, optimized, missingKeywords }) {
  const [mode, setMode] = useState('split');

  const highlight = (text) => {
    if (!text || !missingKeywords?.length) return text;
    const escaped = missingKeywords
      .filter(Boolean)
      .map((k) => k.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))
      .join('|');
    const pattern = new RegExp(`(${escaped})`, 'gi');
    return text
      .split(pattern)
      .filter((part) => part)
      .map((part, i) =>
        pattern.test(part) ? (
          <mark key={i} className="rounded bg-yellow-200 px-0.5 text-yellow-900 dark:bg-yellow-500/40 dark:text-yellow-100">
            {part}
          </mark>
        ) : (
          part
        ),
      );
  };

  const renderPane = (content, title, tone) => (
    <div className="card overflow-hidden">
      <div className={`border-b border-slate-200 px-4 py-2.5 text-sm font-semibold dark:border-slate-800 ${tone}`}>
        {title}
      </div>
      <pre className="max-h-[600px] overflow-auto whitespace-pre-wrap p-4 font-sans text-sm leading-relaxed">
        {highlight(content)}
      </pre>
    </div>
  );

  return (
    <div>
      <div className="mb-3 flex justify-end">
        <div className="inline-flex rounded-lg border border-slate-200 dark:border-slate-700 p-0.5 text-xs">
          {['split', 'optimized', 'original'].map((m) => (
            <button
              key={m}
              onClick={() => setMode(m)}
              className={`rounded-md px-3 py-1.5 font-medium capitalize transition-colors ${
                mode === m
                  ? 'bg-brand-600 text-white'
                  : 'text-slate-500 hover:text-slate-700 dark:hover:text-slate-300'
              }`}
            >
              {m === 'split' ? 'Side by side' : m}
            </button>
          ))}
        </div>
      </div>

      {mode === 'split' && (
        <div className="grid gap-4 lg:grid-cols-2">
          {renderPane(original, 'Original Resume', 'text-slate-600 dark:text-slate-300')}
          {renderPane(optimized, 'Optimized Resume', 'text-emerald-600 dark:text-emerald-400')}
        </div>
      )}
      {mode === 'optimized' && renderPane(optimized, 'Optimized Resume', 'text-emerald-600 dark:text-emerald-400')}
      {mode === 'original' && renderPane(original, 'Original Resume', 'text-slate-600 dark:text-slate-300')}

      {missingKeywords?.length > 0 && (
        <p className="mt-3 text-xs text-slate-500">
          <mark className="rounded bg-yellow-200 px-1 text-yellow-900 dark:bg-yellow-500/40 dark:text-yellow-100">
            Highlighted
          </mark>
          &nbsp;terms are keywords present in the job description but missing from your resume.
        </p>
      )}
    </div>
  );
}
