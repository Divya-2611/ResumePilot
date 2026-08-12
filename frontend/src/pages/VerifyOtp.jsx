import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import authService from '../services/authService';
import { useAuth } from '../hooks/useAuth';
import { useToast } from '../hooks/useToast';
import { getErrorMessage } from '../utils/helpers';
import ThemeToggle from '../components/layout/ThemeToggle';
import Spinner from '../components/common/Spinner';

const OTP_EXPIRY_SECONDS = 5 * 60; // 5 minutes

/**
 * Email OTP verification with countdown + resend.
 */
export default function VerifyOtp() {
  const [searchParams] = useSearchParams();
  const email = searchParams.get('email') || '';
  const { authenticate } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();

  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [submitting, setSubmitting] = useState(false);
  const [resending, setResending] = useState(false);
  const [secondsLeft, setSecondsLeft] = useState(OTP_EXPIRY_SECONDS);
  const inputsRef = useRef([]);

  // Countdown timer.
  useEffect(() => {
    const timer = setInterval(() => {
      setSecondsLeft((s) => (s > 0 ? s - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const handleChange = (index, value) => {
    const digit = value.replace(/\D/g, '').slice(-1);
    setOtp((current) => {
      const next = [...current];
      next[index] = digit;
      return next;
    });
    if (digit && index < 5) {
      inputsRef.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      inputsRef.current[index - 1]?.focus();
    }
  };

  const submit = async () => {
    const code = otp.join('');
    if (code.length !== 6) {
      toast.error('Please enter all 6 digits');
      return;
    }
    setSubmitting(true);
    try {
      const auth = await authService.verifyOtp(email, code);
      // Auto-login after successful verification.
      authenticate(auth);
      toast.success('Email verified! Welcome aboard.');
      navigate('/dashboard', { replace: true });
    } catch (err) {
      toast.error(getErrorMessage(err, 'Verification failed'));
    } finally {
      setSubmitting(false);
    }
  };

  const resend = async () => {
    setResending(true);
    try {
      await authService.resendOtp(email);
      toast.success('A new OTP has been sent');
      setSecondsLeft(OTP_EXPIRY_SECONDS);
      setOtp(['', '', '', '', '', '']);
      inputsRef.current[0]?.focus();
    } catch (err) {
      toast.error(getErrorMessage(err, 'Could not resend OTP'));
    } finally {
      setResending(false);
    }
  };

  const minutes = Math.floor(secondsLeft / 60);
  const seconds = secondsLeft % 60;

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="w-full max-w-md">
        <div className="mb-6 flex items-center justify-between">
          <span className="text-lg font-bold">ResumePilot</span>
          <ThemeToggle />
        </div>

        <div className="card p-8 animate-slide-up">
          <h1 className="text-2xl font-bold">Verify your email</h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            Enter the 6-digit code sent to <span className="font-medium">{email || 'your email'}</span>
          </p>

          <div className="mt-6 flex justify-center gap-2">
            {otp.map((digit, index) => (
              <input
                key={index}
                ref={(el) => {
                  inputsRef.current[index] = el;
                }}
                inputMode="numeric"
                maxLength={2}
                value={digit}
                onChange={(e) => handleChange(index, e.target.value)}
                onKeyDown={(e) => handleKeyDown(index, e)}
                className="input h-14 w-12 text-center text-xl font-bold"
                aria-label={`Digit ${index + 1}`}
              />
            ))}
          </div>

          <div className="mt-4 text-center text-sm text-slate-500">
            {secondsLeft > 0 ? (
              <>
                Code expires in{' '}
                <span className="font-semibold text-slate-700 dark:text-slate-200">
                  {minutes}:{String(seconds).padStart(2, '0')}
                </span>
              </>
            ) : (
              <span className="text-red-500">Code expired - request a new one</span>
            )}
          </div>

          <button onClick={submit} className="btn-primary mt-5 w-full" disabled={submitting || secondsLeft === 0}>
            {submitting ? <Spinner size={16} className="border-white/40 border-t-white" /> : 'Verify email'}
          </button>

          <button
            onClick={resend}
            disabled={resending || secondsLeft > 0}
            className="btn-secondary mt-3 w-full"
          >
            {resending ? 'Sending…' : 'Resend OTP'}
          </button>
        </div>
      </div>
    </div>
  );
}
