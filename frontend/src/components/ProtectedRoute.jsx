import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

/**
 * Guards authenticated routes. Redirects to /login (remembering the
 * intended destination) when there is no valid session. With
 * `adminOnly`, non-admins are sent back to /dashboard.
 */
export default function ProtectedRoute({ adminOnly = false, children }) {
  const { isAuthenticated, user } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }
  if (adminOnly && !(user?.roles || []).includes('ADMIN')) {
    return <Navigate to="/dashboard" replace />;
  }
  return children;
}
