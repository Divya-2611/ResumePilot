import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { useToast } from '../hooks/useToast';
import { getErrorMessage } from '../utils/helpers';
import { isValidEmail } from '../utils/validators';
import Tooltip from '../components/common/Tooltip';
import ThemeToggle from '../components/layout/ThemeToggle';
import Spinner from '../components/common/Spinner';

/**
 * Login page.
 */
export default function Login() {
  const { login } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm();

  const onSubmit = async (data) => {
    setSubmitting(true);
    try {
      await login(data.email, data.password);
      toast.success('Welcome back!');
      navigate(location.state?.from || '/dashboard', { replace: true });
    } catch (err) {
      toast.error(getErrorMessage(err, 'Login failed'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="w-full max-w-md">
        <div className="mb-6 flex items-center justify-between">
          <Link to="/" className="text-lg font-bold">
            ResumePilot
          </Link>
          <ThemeToggle />
        </div>

        <div className="card p-8 animate-slide-up">
          <h1 className="text-2xl font-bold">Welcome back</h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            Log in to continue optimizing your resume
          </p>

          <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4" noValidate>
            <div>
              <label className="label" htmlFor="email">
                Email
              </label>
              <input
                id="email"
                type="email"
                className="input"
                placeholder="Enter your Email"
                {...register('email', {
                  required: 'Email is required',
                  validate: (v) => isValidEmail(v) || 'Invalid email format',
                })}
              />
              {errors.email && (
                <p className="mt-1 text-xs text-red-500">{errors.email.message}</p>
              )}
            </div>

            <div>
              <label className="label" htmlFor="password">
                Password
              </label>
              <div className="relative">
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  className="input pr-10"
                  placeholder="Enter your password"
                  {...register('password', { required: 'Password is required' })}
                />
                <span className="group/tt absolute right-3 top-1/2 -translate-y-1/2">
                  <button
                    type="button"
                    onClick={() => setShowPassword((s) => !s)}
                    className="text-slate-400"
                    aria-label="Toggle password visibility"
                  >
                    {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                  <span className="pointer-events-none absolute -top-9 right-0 z-50 whitespace-nowrap rounded-md bg-slate-900 px-2 py-1 text-xs font-medium text-white opacity-0 shadow-lg transition-opacity duration-150 group-hover/tt:opacity-100 dark:bg-slate-700">
                    {showPassword ? 'Hide password' : 'Show password'}
                  </span>
                </span>
              </div>
              {errors.password && (
                <p className="mt-1 text-xs text-red-500">{errors.password.message}</p>
              )}
            </div>

            <div className="flex justify-end">
              <Link to="/forgot-password" className="text-sm text-brand-600 hover:underline dark:text-brand-400">
                Forgot password?
              </Link>
            </div>

            <Tooltip label="Sign in to your account">
                <button type="submit" className="btn-primary w-full" disabled={submitting}>
                  {submitting ? <Spinner size={16} className="border-white/40 border-t-white" /> : 'Log in'}
                </button>
              </Tooltip>
          </form>

          <p className="mt-5 text-center text-sm text-slate-500">
            No account yet?{' '}
            <Link to="/signup" className="font-medium text-brand-600 hover:underline dark:text-brand-400">
              Create one
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
