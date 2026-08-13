/**
 * Centralized API endpoint definitions.
 */
export const ENDPOINTS = {
  // Auth
  register: '/auth/register',
  verifyOtp: '/auth/verify-otp',
  resendOtp: '/auth/resend-otp',
  login: '/auth/login',
  refresh: '/auth/refresh',
  logout: '/auth/logout',
  forgotPassword: '/auth/forgot-password',
  verifyResetOtp: '/auth/verify-reset-otp',
  resetPassword: '/auth/reset-password',

  // Users
  profile: '/users/profile',
  changePassword: '/users/change-password',

  // Resumes
  uploadResume: '/resumes/upload',
  createResume: '/resumes/create',
  resumes: '/resumes/all',
  resumeById: (id) => `/resumes/${id}`,
  updateResume: (id) => `/resumes/update/${id}`,
  deleteResume: (id) => `/resumes/delete/${id}`,
  duplicateResume: (id) => `/resumes/duplicate/${id}`,
  favoriteResume: (id) => `/resumes/favorite/${id}`,
  versions: (id) => `/resumes/${id}/versions`,
  saveVersion: '/resumes/save',
  restoreVersion: (id, versionId) => `/resumes/${id}/restore/${versionId}`,
  favoriteVersion: (versionId) => `/resumes/versions/${versionId}/favorite`,
  deleteVersion: (resumeId, versionId) =>
    `/resumes/${resumeId}/versions/${versionId}`,
  renameVersion: (resumeId, versionId) =>
    `/resumes/${resumeId}/versions/${versionId}/rename`,

  // Jobs
  pasteJd: '/jobs/paste-jd',
  uploadJd: '/jobs/upload-jd',

  // Optimization
  optimize: '/optimize',

  // Analytics
  dashboard: '/dashboard',
  history: '/history',
  exportHistory: '/history/export',
  download: '/download',

  // Admin
  adminUsers: '/admin/users',
  adminUserRole: (id) => `/admin/users/${id}/role`,
  adminUserDelete: (id) => `/admin/users/${id}`,
};
