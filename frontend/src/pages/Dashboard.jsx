import { Link } from 'react-router-dom';
import {
  FileText,
  Sparkles,
  Download,
  Star,
  TrendingUp,
  ArrowRight,
} from 'lucide-react';
import DashboardLayout from '../components/layout/DashboardLayout';
import StatCard from '../components/common/StatCard';
import { Skeleton, SkeletonCard } from '../components/common/Skeleton';
import EmptyState from '../components/common/EmptyState';
import { useFetch } from '../hooks/useFetch';
import historyService from '../services/historyService';
import { formatRelativeTime } from '../utils/formatters';

/**
 * User dashboard: stats, recent resumes, recent activity.
 */
export default function Dashboard() {
  const { data, loading } = useFetch(() => historyService.dashboard(), []);

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold">Dashboard</h1>
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Your resume optimization overview
            </p>
          </div>
          <div className="flex gap-2">
            <Link to="/resumes/upload" className="btn-primary">
              <FileText className="h-4 w-4" />
              Upload
            </Link>
            <Link to="/optimize" className="btn-secondary">
              <Sparkles className="h-4 w-4" />
              Optimize
            </Link>
          </div>
        </div>

        {/* Stats */}
        {loading ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {[1, 2, 3, 4].map((i) => (
              <Skeleton key={i} className="h-28" />
            ))}
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard icon={FileText} label="Total Resumes" value={data?.totalResumes ?? 0} hint="All versions included" />
            <StatCard icon={Sparkles} label="Optimizations" value={data?.totalOptimizations ?? 0} hint={`${data?.optimizationsLast30Days ?? 0} in the last 30 days`} color="violet" />
            <StatCard icon={Download} label="Downloads" value={data?.totalDownloads ?? 0} color="emerald" />
            <StatCard icon={Star} label="Saved Resumes" value={data?.savedResumes ?? 0} hint="Favorites" color="amber" />
          </div>
        )}

        <div className="grid gap-6 lg:grid-cols-2">
          {/* Recent resumes */}
          <section className="card p-5">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="flex items-center gap-2 font-semibold">
                <FileText className="h-4 w-4 text-brand-600 dark:text-brand-400" />
                Recent Resumes
              </h2>
              <Link to="/resumes" className="flex items-center gap-1 text-sm text-brand-600 hover:underline dark:text-brand-400">
                View all <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </div>
            {loading ? (
              <SkeletonCard />
            ) : data?.recentResumes?.length ? (
              <ul className="divide-y divide-slate-100 dark:divide-slate-800">
                {data.recentResumes.map((resume) => (
                  <li key={resume.id} className="flex items-center justify-between py-3">
                    <Link
                      to={`/resumes/${resume.id}/edit`}
                      className="min-w-0 flex-1 hover:text-brand-600 dark:hover:text-brand-400"
                    >
                      <p className="truncate font-medium">{resume.name}</p>
                      <p className="text-xs text-slate-400">
                        Updated {formatRelativeTime(resume.updatedAt)}
                      </p>
                    </Link>
                    <div className="ml-3 flex items-center gap-2">
                      {resume.atsScore != null && (
                        <span
                          className={`text-sm font-bold ${
                            resume.atsScore >= 80
                              ? 'text-emerald-500'
                              : resume.atsScore >= 60
                                ? 'text-amber-500'
                                : 'text-red-500'
                          }`}
                        >
                          {resume.atsScore}
                        </span>
                      )}
                      {resume.favorite && <Star className="h-4 w-4 text-amber-400" />}
                    </div>
                  </li>
                ))}
              </ul>
            ) : (
              <EmptyState
                icon={FileText}
                title="No resumes yet"
                description="Upload a PDF/DOCX or create a resume to start optimizing."
                action={
                  <Link to="/resumes/upload" className="btn-primary">
                    Upload your first resume
                  </Link>
                }
              />
            )}
          </section>

          {/* Recent activity */}
          <section className="card p-5">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="flex items-center gap-2 font-semibold">
                <TrendingUp className="h-4 w-4 text-violet-600 dark:text-violet-400" />
                Recent Activity
              </h2>
              <Link to="/history" className="flex items-center gap-1 text-sm text-brand-600 hover:underline dark:text-brand-400">
                History <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </div>
            {loading ? (
              <SkeletonCard />
            ) : data?.recentActivity?.length ? (
              <ul className="space-y-3">
                {data.recentActivity.map((item) => (
                  <li key={item.id} className="rounded-lg border border-slate-100 p-3 dark:border-slate-800">
                    <div className="flex items-center justify-between gap-2">
                      <p className="truncate text-sm font-medium">{item.resumeName}</p>
                      {item.atsScore != null && (
                        <span className="shrink-0 rounded-full bg-brand-50 px-2 py-0.5 text-xs font-bold text-brand-700 dark:bg-brand-900/40 dark:text-brand-300">
                          ATS {item.atsScore}
                        </span>
                      )}
                    </div>
                    <p className="mt-0.5 truncate text-xs text-slate-400">
                      {item.jobTitle || 'No JD title'} · {formatRelativeTime(item.createdAt)}
                      {item.aiProvider ? ` · ${item.aiProvider}` : ''}
                    </p>
                  </li>
                ))}
              </ul>
            ) : (
              <EmptyState
                icon={Sparkles}
                title="No optimizations yet"
                description="Optimize a resume against a job description to see activity here."
                action={
                  <Link to="/optimize" className="btn-primary">
                    <Sparkles className="h-4 w-4" /> Optimize now
                  </Link>
                }
              />
            )}
          </section>
        </div>
      </div>
    </DashboardLayout>
  );
}
