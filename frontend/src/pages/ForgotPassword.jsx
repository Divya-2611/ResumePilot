import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import authService from '../services/authService';
import { useToast } from '../hooks/useToast';
import { getErrorMessage } from '../utils/helpers';
import { isValidEmail, isValidOtp, isStrongPassword } from '../utils/validators';
import ThemeToggle from '../components/layout/ThemeToggle';
import Spinner from '../components/common/Spinner';

/**
 * Multi-step forgot-password flow: email -> OTP -> new password.
 */
export default function ForgotPassword() {
  const toast = useToast();
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [email, setEmail] = useState('');
  const [resetToken, setResetToken] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm();
  const resetForm = useForm();

  const sendOtp = async (data) => {
    setSubmitting(true);
    try {
      await authService.forgotPassword(data.email);
      setEmail(data.email);
      setStep(2);
      toast.success('If the email exists, a reset code has been sent.');
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  const verifyOtp = async (data) => {
    setSubmitting(true);
    try {
      const token = await authService.verifyResetOtp(email, data.otp);
      setResetToken(token);
      setStep(3);
      toast.success('Code verified - choose a new password.');
    } catch (err) {
      toast.error(getErrorMessage(err, 'Invalid code'));
    } finally {
      setSubmitting(false);
    }
  };

  const resetPassword = async (data) => {
    setSubmitting(true);
    try {
      await authService.resetPassword(resetToken, data.newPassword);
      toast.success('Password reset. Please log in.');
      navigate('/login', { replace: true });
    } catch (err) {
      toast.error(getErrorMessage(err, 'Could not reset password'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="w-full max-w-md">
        <div className="mb-6 flex items-center justify-between">
          <Link to="/" className="text-lg font-bold">ResumePilot</Link>
          <ThemeToggle />
        </div>

        <div className="card p-8 animate-slide-up">
          {/* Step indicator */}
          <div className="mb-6 flex items-center gap-2">
            {['Email', 'Verify OTP', 'New password'].map((label, i) => (
              <div key={label} className="flex items-center gap-2">
                <span
                  className={`flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold ${
                    step === i + 1
                      ? 'bg-brand-600 text-white'
                      : step > i + 1
                        ? 'bg-emerald-500 text-white'
                        : 'bg-slate-200 text-slate-500 dark:bg-slate-800'
                  }`}
                >
                  {i + 1}
                </span>
                <span className="hidden text-xs sm:inline">{label}</span>
                {i < 2 && <span className="h-px w-4 bg-slate-300 dark:bg-slate-700" />}
              </div>
            ))}
          </div>

          {step === 1 && (
            <form onSubmit={handleSubmit(sendOtp)} className="space-y-4" noValidate>
              <h1 className="text-2xl font-bold">Forgot password?</h1>
              <p className="text-sm text-slate-500 dark:text-slate-400">
                Enter your account email and we will send a 6-digit reset code.
              </p>
              <div>
                <label className="label" htmlFor="email">Email</label>
                <input
                  id="email"
                  type="email"
                  className="input"
                  placeholder="you@example.com"
                  {...register('email', {
                    required: 'Email is required',
                    validate: (v) => isValidEmail(v) || 'Invalid email format',
                  })}
                />
                {errors.email && <p className="mt-1 text-xs text-red-500">{errors.email.message}</p>}
              </div>
              <button type="submit" className="btn-primary w-full" disabled={submitting}>
                {submitting ? <Spinner size={16} className="border-white/40 border-t-white" /> : 'Send reset code'}
              </button>
            </form>
          )}

          {step === 2 && (
            <form onSubmit={resetForm.handleSubmit(verifyOtp)} className="space-y-4" noValidate>
              <h1 className="text-2xl font-bold">Enter the code</h1>
              <p className="text-sm text-slate-500 dark:text-slate-400">
                We sent a 6-digit code to <span className="font-medium">{email}</span>
              </p>
              <div>
                <input
                  inputMode="numeric"
                  maxLength={6}
                  className="input text-center text-2xl font-bold tracking-[0.5em]"
                  placeholder="______"
                  {...resetForm.register('otp', {
                    required: 'OTP is required',
                    validate: (v) => isValidOtp(v) || 'OTP must be 6 digits',
                  })}
                />
                {resetForm.formState.errors.otp && (
                  <p className="mt-1 text-xs text-red-500">
                    {resetForm.formState.errors.otp.message}
                  </p>
                )}
              </div>
              <button type="submit" className="btn-primary w-full" disabled={submitting}>
                {submitting ? <Spinner size={16} className="border-white/40 border-t-white" /> : 'Verify code'}
              </button>
            </form>
          )}

          {step === 3 && (
            <form onSubmit={resetForm.handleSubmit(resetPassword)} className="space-y-4" noValidate>
              <h1 className="text-2xl font-bold">Choose a new password</h1>
              <div>
                <label className="label" htmlFor="newPassword">New password</label>
                <input
                  id="newPassword"
                  type="password"
                  className="input"
                  {...resetForm.register('newPassword', {
                    required: 'Password is required',
                    validate: (v) => isStrongPassword(v) || 'Min 8 chars, incl. number and symbol',
                  })}
                />
                {resetForm.formState.errors.newPassword && (
                  <p className="mt-1 text-xs text-red-500">
                    {resetForm.formState.errors.newPassword.message}
                  </p>
                )}
              </div>
              <button type="submit" className="btn-primary w-full" disabled={submitting}>
                {submitting ? <Spinner size={16} className="border-white/40 border-t-white" /> : 'Reset password'}
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
