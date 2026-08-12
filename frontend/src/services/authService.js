import api from '../api/axiosClient';
import { ENDPOINTS } from '../api/endpoints';

/**
 * Authentication API calls.
 * Every method returns the `data` payload from the ApiResponse envelope.
 */
const authService = {
  register: (payload) =>
    api.post(ENDPOINTS.register, payload).then((r) => r.data.data),

  verifyOtp: (email, otp) =>
    api.post(ENDPOINTS.verifyOtp, { email, otp }).then((r) => r.data.data),

  resendOtp: (email) =>
    api.post(ENDPOINTS.resendOtp, { email }).then((r) => r.data.data),

  login: (email, password) =>
    api.post(ENDPOINTS.login, { email, password }).then((r) => r.data.data),

  logout: (refreshToken) =>
    api.post(ENDPOINTS.logout, { refreshToken }).then((r) => r.data.data),

  forgotPassword: (email) =>
    api.post(ENDPOINTS.forgotPassword, { email }).then((r) => r.data.data),

  verifyResetOtp: (email, otp) =>
    api.post(ENDPOINTS.verifyResetOtp, { email, otp }).then((r) => r.data.data),

  resetPassword: (resetToken, newPassword) =>
    api.post(ENDPOINTS.resetPassword, { resetToken, newPassword }).then((r) => r.data.data),
};

export default authService;
