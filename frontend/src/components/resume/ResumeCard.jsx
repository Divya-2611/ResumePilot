import { FileText, Sparkles, Download, MoreVertical, Star, StarOff } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Badge from '../common/Badge';
import Tooltip from '../common/Tooltip';
import { formatRelativeTime } from '../../utils/formatters';
import { useAuth } from '../../hooks/useAuth';
import { tokenStorage } from '../../api/axiosClient';
import { ENDPOINTS } from '../../api/endpoints';

/**
 * Resume card with quick actions (edit, optimize, download, favorite, menu).
 */
export default function ResumeCard({ resume, onDelete, onDuplicate, onRename, onToggleFavorite }) {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);

  const download = (format) => {
    // Direct navigation triggers the file download (auth via header handled by interceptor-free URL; append token query is not used - the download endpoint is authenticated, so we fetch with the token).
    const params = new URLSearchParams({ resumeId: resume.id, format });
    const base = import.meta.env.VITE_API_BASE_URL || '/api/v1';
    fetch(`${base}${ENDPOINTS.download}?${params}`, {
      headers: { Authorization: `Bearer ${tokenStorage.getAccess()}` },
    })
      .then((res) => res.blob())
      .then((blob) => {
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `${resume.name}.${format.toLowerCase()}`;
        link.click();
        URL.revokeObjectURL(link.href);
      })
      .catch(() => {});
  };

  return (
    <div className="card group flex flex-col p-5 animate-fade-in">
      <div className="flex items-start justify-between">
        <div className="rounded-xl bg-brand-50 p-3 dark:bg-brand-900/30">
          <FileText className="h-6 w-6 text-brand-600 dark:text-brand-400" />
        </div>
        <div className="relative">
          <Tooltip label="More actions">
            <button
              onClick={() => setMenuOpen((o) => !o)}
              className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800"
              aria-label="More actions"
            >
              <MoreVertical className="h-4 w-4" />
            </button>
          </Tooltip>
          {menuOpen && (
            <div
              className="absolute right-0 z-20 mt-1 w-40 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 py-1 shadow-lg"
              onMouseLeave={() => setMenuOpen(false)}
            >
              <button
                className="block w-full px-4 py-2 text-left text-sm hover:bg-slate-50 dark:hover:bg-slate-800"
                onClick={() => onDuplicate(resume)}
              >
                Duplicate
              </button>
              <button
                className="block w-full px-4 py-2 text-left text-sm hover:bg-slate-50 dark:hover:bg-slate-800"
                onClick={() => onRename(resume)}
              >
                Rename
              </button>
              <button
                className="block w-full px-4 py-2 text-left text-sm hover:bg-slate-50 dark:hover:bg-slate-800"
                onClick={onDelete}
              >
                Delete
              </button>
            </div>
          )}
        </div>
      </div>

      <h3 className="mt-3 truncate font-semibold" title={resume.name}>
        {resume.name}
      </h3>
      <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
        {resume.fileType ? `${resume.fileType} file` : 'Created'} · Updated{' '}
        {formatRelativeTime(resume.updatedAt)}
      </p>

      <div className="mt-3 flex flex-wrap gap-1.5">
        {resume.optimized && <Badge color="green">Optimized</Badge>}
        {resume.atsScore != null && (
          <Badge color={resume.atsScore >= 80 ? 'green' : resume.atsScore >= 60 ? 'amber' : 'red'}>
            ATS {resume.atsScore}
          </Badge>
        )}
        {resume.favorite && <Badge color="amber">Favorite</Badge>}
      </div>

      <div className="mt-4 flex items-center gap-2 border-t border-slate-100 pt-4 dark:border-slate-800">
        <Tooltip label="Edit resume">
          <button className="btn-secondary flex-1 !px-2 !py-2 text-xs" onClick={() => navigate(`/resumes/${resume.id}/edit`)}>
            Edit
          </button>
        </Tooltip>
        <Tooltip label="Optimize against a job description">
          <button
            className="btn-primary flex-1 !px-2 !py-2 text-xs"
            onClick={() =>
              navigate('/optimize', { state: { resumeId: resume.id, resumeName: resume.name } })
            }
          >
            <Sparkles className="h-3.5 w-3.5" />
            Optimize
          </button>
        </Tooltip>
        <Tooltip label="Download PDF">
          <button
            className="btn-secondary !px-2 !py-2 text-xs"
            onClick={() => download('pdf')}
            title="Download PDF"
          >
            <Download className="h-3.5 w-3.5" />
          </button>
        </Tooltip>
        <Tooltip label={resume.favorite ? 'Remove favorite' : 'Mark favorite'}>
          <button
            onClick={onToggleFavorite}
            className={`rounded-lg p-2 ${
              resume.favorite
                ? 'text-amber-500'
                : 'text-slate-300 dark:text-slate-600 hover:text-amber-400'
            }`}
            aria-label={resume.favorite ? 'Remove favorite' : 'Mark favorite'}
          >
            {resume.favorite ? <Star className="h-4 w-4" /> : <StarOff className="h-4 w-4" />}
          </button>
        </Tooltip>
      </div>
    </div>
  );
}
