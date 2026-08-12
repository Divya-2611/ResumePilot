/**
 * Empty state placeholder (no data yet).
 */
export default function EmptyState({ icon: Icon, title, description, action }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-slate-300 dark:border-slate-700 py-16 px-6 text-center">
      {Icon && (
        <div className="mb-4 rounded-2xl bg-brand-50 dark:bg-brand-900/30 p-4">
          <Icon className="h-10 w-10 text-brand-600 dark:text-brand-400" />
        </div>
      )}
      <h3 className="text-lg font-semibold">{title}</h3>
      {description && (
        <p className="mt-1 max-w-sm text-sm text-slate-500 dark:text-slate-400">{description}</p>
      )}
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}
