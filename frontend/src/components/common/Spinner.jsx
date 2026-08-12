/**
 * Centered loading spinner.
 */
export default function Spinner({ size = 20, className = '' }) {
  return (
    <span
      role="status"
      aria-label="Loading"
      className={`inline-block animate-spin rounded-full border-2 border-slate-300 border-t-brand-600 ${className}`}
      style={{ width: size, height: size }}
    />
  );
}
