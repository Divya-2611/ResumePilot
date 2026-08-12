import { describe, it, expect } from 'vitest';
import { formatRelativeTime } from '../utils/formatters';
import { isValidEmail, isStrongPassword, isValidOtp, isAllowedResumeFile } from '../utils/validators';

describe('formatters', () => {
  it('formats relative time', () => {
    expect(formatRelativeTime(new Date().toISOString())).toBe('just now');
    expect(formatRelativeTime(null)).toBe('—');
  });
});

describe('validators', () => {
  it('validates emails', () => {
    expect(isValidEmail('jane@example.com')).toBe(true);
    expect(isValidEmail('not-an-email')).toBe(false);
  });

  it('validates strong passwords', () => {
    expect(isStrongPassword('Password1!')).toBe(true);
    expect(isStrongPassword('weak')).toBe(false);
    expect(isStrongPassword('NoNumber!')).toBe(false);
  });

  it('validates OTPs', () => {
    expect(isValidOtp('123456')).toBe(true);
    expect(isValidOtp('12')).toBe(false);
  });

  it('validates resume files', () => {
    expect(isAllowedResumeFile({ name: 'resume.pdf' })).toBe(true);
    expect(isAllowedResumeFile({ name: 'resume.docx' })).toBe(true);
    expect(isAllowedResumeFile({ name: 'resume.txt' })).toBe(false);
  });
});
