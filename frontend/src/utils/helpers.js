/**
 * Extracts a human-readable message from an axios error.
 */
export function getErrorMessage(error, fallback = 'Something went wrong. Please try again.') {
  const message = error?.response?.data?.message;
  const firstFieldError = error?.response?.data?.errors
    ? Object.values(error.response.data.errors)[0]
    : null;
  return message || firstFieldError || error?.message || fallback;
}
