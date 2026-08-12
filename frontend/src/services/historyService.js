import api from '../api/axiosClient';
import { ENDPOINTS } from '../api/endpoints';

/**
 * Dashboard, history and exports.
 */
const historyService = {
  dashboard: () => api.get(ENDPOINTS.dashboard).then((r) => r.data.data),

  getAll: ({ page = 0, size = 10, search = '' } = {}) =>
    api
      .get(ENDPOINTS.history, { params: { page, size, search: search || undefined } })
      .then((r) => r.data.data),

  /** Triggers a browser download of the CSV export. */
  exportCsv: () => {
    const url = `${api.defaults.baseURL}${ENDPOINTS.exportHistory}`;
    const token = localStorage.getItem('accessToken');
    // Fetch with the auth header so the download is authorized.
    return fetch(url, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => res.blob())
      .then((blob) => {
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = 'optimization-history.csv';
        link.click();
        URL.revokeObjectURL(link.href);
      });
  },
};

export default historyService;
