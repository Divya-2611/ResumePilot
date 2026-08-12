import api from '../api/axiosClient';
import { ENDPOINTS } from '../api/endpoints';

/**
 * Job description paste/upload and analysis.
 */
const jobDescriptionService = {
  paste: (content, title) =>
    api.post(ENDPOINTS.pasteJd, { content, title }).then((r) => r.data.data),

  upload: (file) => {
    const form = new FormData();
    form.append('file', file);
    return api
      .post(ENDPOINTS.uploadJd, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data.data);
  },
};

export default jobDescriptionService;
