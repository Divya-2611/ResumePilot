import api from '../api/axiosClient';
import { ENDPOINTS } from '../api/endpoints';

/**
 * Resume CRUD, upload, versions and downloads.
 */
const resumeService = {
  upload: (file, name) => {
    const form = new FormData();
    form.append('file', file);
    if (name) form.append('name', name);
    return api
      .post(ENDPOINTS.uploadResume, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data.data);
  },

  create: (payload) =>
    api.post(ENDPOINTS.createResume, payload).then((r) => r.data.data),

  getAll: ({ page = 0, size = 9, search = '', favorites = false } = {}) =>
    api
      .get(ENDPOINTS.resumes, {
        params: { page, size, search: search || undefined, favorites },
      })
      .then((r) => r.data.data),

  getById: (id) => api.get(ENDPOINTS.resumeById(id)).then((r) => r.data.data),

  update: (id, payload) =>
    api.put(ENDPOINTS.updateResume(id), payload).then((r) => r.data.data),

  delete: (id) => api.delete(ENDPOINTS.deleteResume(id)).then((r) => r.data.data),

  duplicate: (id) =>
    api.post(ENDPOINTS.duplicateResume(id)).then((r) => r.data.data),

  toggleFavorite: (id) =>
    api.post(ENDPOINTS.favoriteResume(id)).then((r) => r.data.data),

  getVersions: (id) => api.get(ENDPOINTS.versions(id)).then((r) => r.data.data),

  saveVersion: (payload) =>
    api.post(ENDPOINTS.saveVersion, payload).then((r) => r.data.data),

  restoreVersion: (resumeId, versionId) =>
    api
      .post(ENDPOINTS.restoreVersion(resumeId, versionId))
      .then((r) => r.data.data),

  toggleVersionFavorite: (versionId) =>
    api.post(ENDPOINTS.favoriteVersion(versionId)).then((r) => r.data.data),

  renameVersion: (resumeId, versionId, name) =>
    api.put(ENDPOINTS.renameVersion(resumeId, versionId), { name }).then((r) => r.data.data),

  deleteVersion: (resumeId, versionId) =>
    api.delete(ENDPOINTS.deleteVersion(resumeId, versionId)).then((r) => r.data.data),
};

export default resumeService;
