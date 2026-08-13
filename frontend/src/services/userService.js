import api from '../api/axiosClient';
import { ENDPOINTS } from '../api/endpoints';

/**
 * Profile and password management API calls.
 */
const userService = {
  getProfile: () => api.get(ENDPOINTS.profile).then((r) => r.data.data),

  updateProfile: (payload) =>
    api.put(ENDPOINTS.profile, payload).then((r) => r.data.data),

  changePassword: (currentPassword, newPassword) =>
    api
      .put(ENDPOINTS.changePassword, { currentPassword, newPassword })
      .then((r) => r.data.data),

  deleteAccount: () => api.delete(ENDPOINTS.profile).then((r) => r.data.data),
};

export default userService;
