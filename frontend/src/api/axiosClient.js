import axios from 'axios';

/**
 * Single axios instance for the whole app.
 *
 * - Attaches the JWT access token from localStorage on every request.
 * - On a 401, performs a single-flight refresh using the refresh token
 *   (with rotation) and replays the original request.
 * - Redirects to /login when refresh fails.
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';

export const tokenStorage = {
  getAccess: () => localStorage.getItem(ACCESS_TOKEN_KEY),
  getRefresh: () => localStorage.getItem(REFRESH_TOKEN_KEY),
  setTokens: (access, refresh) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, access);
    localStorage.setItem(REFRESH_TOKEN_KEY, refresh);
  },
  clear: () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },
};

// Tracks an in-flight refresh so concurrent 401s share one call.
let refreshPromise = null;

async function refreshTokens() {
  const refreshToken = tokenStorage.getRefresh();
  if (!refreshToken) {
    throw new Error('No refresh token');
  }
  const response = await axios.post(
    `${api.defaults.baseURL}/auth/refresh`,
    { refreshToken },
  );
  const { accessToken, refreshToken: newRefresh } = response.data.data;
  tokenStorage.setTokens(accessToken, newRefresh);
  return accessToken;
}

// Request interceptor: attach access token.
api.interceptors.request.use((config) => {
  const token = tokenStorage.getAccess();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor: refresh-and-retry on 401 (excluding the refresh call itself).
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const isAuthCall = original?.url?.includes('/auth/');

    if (
      error.response?.status === 401 &&
      !original?._retried &&
      !isAuthCall
    ) {
      original._retried = true;
      try {
        refreshPromise = refreshPromise || refreshTokens();
        const newAccess = await refreshPromise;
        refreshPromise = null;
        original.headers.Authorization = `Bearer ${newAccess}`;
        return api(original);
      } catch (refreshError) {
        refreshPromise = null;
        tokenStorage.clear();
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  },
);

export default api;
