/**
 * Client-side validation helpers (server validation is authoritative).
 */
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function isValidEmail(email) {
  return EMAIL_PATTERN.test(email || '');
}

export function isStrongPassword(password) {
  return (
    password &&
    password.length >= 8 &&
    /[A-Za-z]/.test(password) &&
    /\d/.test(password) &&
    /[^A-Za-z0-9]/.test(password)
  );
}

export function isValidOtp(otp) {
  return /^\d{6}$/.test(otp || '');
}

export function isAllowedResumeFile(file) {
  const ext = (file?.name?.split('.').pop() || '').toLowerCase();
  return ext === 'pdf' || ext === 'docx';
}

export function isImageFile(file) {
  const ext = (file?.name?.split('.').pop() || '').toLowerCase();
  return ['jpg', 'jpeg', 'png', 'webp', 'svg'].includes(ext);
}
