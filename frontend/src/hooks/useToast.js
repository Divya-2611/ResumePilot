import { useContext } from 'react';
import { ToastContext } from '../context/ToastContext';

/**
 * Convenience hook exposing toast.success / toast.error / toast.info.
 */
export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within ToastProvider');
  }
  return context;
}
