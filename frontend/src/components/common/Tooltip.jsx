/**
 * Hover tooltip: shows a small label under any wrapped element on hover.
 */
export default function Tooltip({ label, children }) {
  if (!label) return children;
  return (
    <span className="group/tt relative inline-flex">
      {children}
      <span
        className="pointer-events-none absolute left-1/2 top-full z-50 mt-1.5 -translate-x-1/2 whitespace-nowrap rounded-md bg-slate-900 px-2 py-1 text-xs font-medium text-white opacity-0 shadow-lg transition-opacity duration-150 group-hover/tt:opacity-100 dark:bg-slate-700"
      >
        {label}
      </span>
    </span>
  );
}