import api from '../api/axiosClient';
import { ENDPOINTS } from '../api/endpoints';

/**
 * Admin-only user management.
 */
const adminService = {
  listUsers: ({ page = 0, size = 10, search = '' } = {}) =>
    api
      .get(ENDPOINTS.adminUsers, { params: { page, size, search: search || undefined } })
      .then((r) => r.data.data),

  changeRole: (id, role) =>
    api.patch(ENDPOINTS.adminUserRole(id), { role }).then((r) => r.data.data),

  deleteUser: (id) => api.delete(ENDPOINTS.adminUserDelete(id)).then((r) => r.data.data),
};

export default adminService;