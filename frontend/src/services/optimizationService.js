import api from '../api/axiosClient';
import { ENDPOINTS } from '../api/endpoints';

/**
 * AI optimization endpoint.
 */
const optimizationService = {
  optimize: (resumeId, jobDescriptionId, jobDescriptionText) =>
    api
      .post(ENDPOINTS.optimize, { resumeId, jobDescriptionId, jobDescriptionText })
      .then((r) => r.data.data),
};

export default optimizationService;
