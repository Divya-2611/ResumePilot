import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { tokenStorage } from '../api/axiosClient';
import authService from '../services/authService';
import userService from '../services/userService';

/**
 * Global authentication state: tokens, user, login/logout/refresh actions.
 */
export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('user') || 'null');
    } catch {
      return null;
    }
  });

  const isAuthenticated = Boolean(tokenStorage.getAccess() && user);

  useEffect(() => {
    // Re-hydrate the profile on first load when a token exists.
    if (tokenStorage.getAccess() && !user) {
      userService
        .getProfile()
        .then((profile) => {
          setUser(profile);
          localStorage.setItem('user', JSON.stringify(profile));
        })
        .catch(() => tokenStorage.clear());
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  /** Stores tokens + user after any auth success. */
  const persistAuth = useCallback(({ accessToken, refreshToken, user: u }) => {
    tokenStorage.setTokens(accessToken, refreshToken);
    setUser(u);
    localStorage.setItem('user', JSON.stringify(u));
  }, []);

  const login = useCallback(
    async (email, password) => {
      const response = await authService.login(email, password);
      persistAuth(response);
      return response;
    },
    [persistAuth],
  );

  const logout = useCallback(async () => {
    try {
      await authService.logout(tokenStorage.getRefresh());
    } catch {
      // Ignore network failures during logout.
    } finally {
      tokenStorage.clear();
      setUser(null);
      localStorage.removeItem('user');
    }
  }, []);

  const updateUser = useCallback((u) => {
    setUser(u);
    localStorage.setItem('user', JSON.stringify(u));
  }, []);

  const value = useMemo(
    () => ({ user, setUser: updateUser, login, logout, authenticate: persistAuth, isAuthenticated }),
    [user, login, logout, persistAuth, isAuthenticated, updateUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
