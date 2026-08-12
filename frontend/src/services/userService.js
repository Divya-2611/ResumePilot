import api from '../api/axiosClient';
import { ENDPOINTS } from '../api/endpoints';

/**
 * Profile and password management API calls.
 */
const userService = {
  getProfile: () => api.get(ENDPOINTS.profile).then((r) => r.data.data),

  updateProfile: (payload) =>
    api.put(ENDPOINTS.profile, payload).then((r) => r.data.data),

  uploadProfilePicture: (file) => {
    const form = new FormData();
    form.append('file', file);
    return api
      .put(ENDPOINTS.profilePicture, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data.data);
  },

  changePassword: (currentPassword, newPassword) =>
    api
      .put(ENDPOINTS.changePassword, { currentPassword, newPassword })
      .then((r) => r.data.data),

  deleteAccount: () => api.delete(ENDPOINTS.profile).then((r) => r.data.data),

  /**
   * Fetches the profile picture as a blob (auth header included) and returns
   * an object URL. Returns null when the user has no picture.
   */
  getProfilePicture: async () => {
    try {
      const res = await api.get(ENDPOINTS.profilePictureGet, { responseType: 'blob' });
      return URL.createObjectURL(res.data);
    } catch (err) {
      if (err?.response?.status === 404) return null;
      throw err;
    }
  },
};

export default userService;
