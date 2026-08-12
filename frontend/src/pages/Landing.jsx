import { Link, useNavigate } from 'react-router-dom';
import { Sparkles, FileText, Gauge, Download, Zap, ShieldCheck } from 'lucide-react';
import ThemeToggle from '../components/layout/ThemeToggle';
import { useAuth } from '../hooks/useAuth';
import { useEffect } from 'react';

/**
 * Public landing page.
 */
const FEATURES = [
  {
    icon: Sparkles,
    title: 'AI Optimization',
    text: 'Rewrite summary, bullets and skills for each job - while staying truthful to your real experience.',
  },
  {
    icon: Gauge,
    title: 'ATS Score & Keywords',
    text: 'Real-time keyword match, missing technology detection and a transparent 0-100 ATS score.',
  },
  {
    icon: FileText,
    title: 'PDF & DOCX',
    text: 'Upload, edit and download professionally formatted resumes. Drag & drop support.',
  },
  {
    icon: Download,
    title: 'Version History',
    text: 'Every optimization is saved. Compare, restore and download any version, anytime.',
  },
  {
    icon: ShieldCheck,
    title: 'Secure by Design',
    text: 'JWT with refresh-token rotation, BCrypt passwords, OTP email verification and rate limiting.',
  },
  {
    icon: Zap,
    title: 'Fast & Scalable',
    text: 'Spring Boot + React clean architecture, swappable AI providers (OpenAI, Gemini, Claude).',
  },
];

export default function Landing() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  useEffect(() => {
    if (isAuthenticated) navigate('/dashboard', { replace: true });
  }, [isAuthenticated, navigate]);

  return (
    <div className="min-h-screen">
      <header className="mx-auto flex max-w-6xl items-center justify-between px-4 py-5">
        <div className="flex items-center gap-2 font-bold">
          <span className="rounded-lg bg-brand-600 p-1.5 text-white">
            <Zap className="h-4 w-4" />
          </span>
          ResumePilot
        </div>
        <div className="flex items-center gap-3">
          <ThemeToggle />
          <Link to="/login" className="btn-secondary">
            Log in
          </Link>
          <Link to="/signup" className="btn-primary">
            Get started
          </Link>
        </div>
      </header>

      {/* Hero */}
      <section className="mx-auto max-w-6xl px-4 pt-16 pb-12 text-center animate-fade-in">
        <h1 className="mx-auto max-w-3xl text-4xl font-extrabold leading-tight sm:text-6xl">
          Turn your resume into the{' '}
          <span className="bg-gradient-to-r from-brand-500 to-brand-700 bg-clip-text text-transparent">
            perfect match
          </span>{' '}
          for any job
        </h1>
        <p className="mx-auto mt-5 max-w-2xl text-lg text-slate-600 dark:text-slate-300">
          Upload your resume, paste a job description, and let AI optimize every section for ATS
          compatibility - with keyword matching, rewrites and side-by-side comparison.
        </p>
        <div className="mt-8 flex flex-wrap justify-center gap-3">
          <Link to="/signup" className="btn-primary !px-6 !py-3 !text-base">
            Start optimizing free
          </Link>
          <Link to="/login" className="btn-secondary !px-6 !py-3 !text-base">
            Log in
          </Link>
        </div>
      </section>

      {/* Features */}
      <section className="mx-auto max-w-6xl px-4 py-12">
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {FEATURES.map(({ icon: Icon, title, text }) => (
            <div key={title} className="card p-6 animate-slide-up">
              <div className="mb-3 inline-block rounded-xl bg-brand-50 p-3 dark:bg-brand-900/30">
                <Icon className="h-6 w-6 text-brand-600 dark:text-brand-400" />
              </div>
              <h3 className="font-semibold">{title}</h3>
              <p className="mt-1.5 text-sm text-slate-500 dark:text-slate-400">{text}</p>
            </div>
          ))}
        </div>
      </section>

      <footer className="border-t border-slate-200 py-8 text-center text-sm text-slate-400 dark:border-slate-800">
        ResumePilot · Java 21 · Spring Boot · React · MySQL
      </footer>
    </div>
  );
}
