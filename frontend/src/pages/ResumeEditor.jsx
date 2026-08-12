import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Save,
  Sparkles,
  Download,
  History,
  Undo2,
  Star,
  Trash2,
  Pencil,
  Check,
  ChevronDown,
} from 'lucide-react';
import DashboardLayout from '../components/layout/DashboardLayout';
import Spinner from '../components/common/Spinner';
import Modal from '../components/common/Modal';
import Badge from '../components/common/Badge';
import Tooltip from '../components/common/Tooltip';
import { useToast } from '../hooks/useToast';
import { getErrorMessage } from '../utils/helpers';
import { formatRelativeTime } from '../utils/formatters';
import { tokenStorage } from '../api/axiosClient';
import { ENDPOINTS } from '../api/endpoints';
import resumeService from '../services/resumeService';

/**
 * Resume editor: plain-text editing, section structure, version history,
 * restore (undo), download and one-click optimize.
 */
export default function ResumeEditor() {
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();

  const [resume, setResume] = useState(null);
  const [content, setContent] = useState('');
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);
  const [showVersions, setShowVersions] = useState(false);
  const [versions, setVersions] = useState([]);
  const [restoreTarget, setRestoreTarget] = useState(null);
  const [renameTargetId, setRenameTargetId] = useState(null);
  const [renameValue, setRenameValue] = useState('');

  const handleRename = async (version) => {
    const name = renameValue.trim();
    if (!name) return;
    try {
      await resumeService.renameVersion(id, version.id, name);
      setVersions(await resumeService.getVersions(id));
      toast.success('Version renamed');
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setRenameTargetId(null);
      setRenameValue('');
    }
  };

  useEffect(() => {
    let cancelled = false;
    Promise.all([resumeService.getById(id), resumeService.getVersions(id)])
      .then(([r, v]) => {
        if (cancelled) return;
        setResume(r);
        setContent(r.content || '');
        setVersions(v);
      })
      .catch((err) => toast.error(getErrorMessage(err)))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [id]); // eslint-disable-line react-hooks/exhaustive-deps

  const save = async () => {
    setSaving(true);
    try {
      const updated = await resumeService.update(id, { content });
      setResume(updated);
      const v = await resumeService.getVersions(id);
      setVersions(v);
      toast.success('Resume saved');
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const loadVersions = async () => {
    setShowVersions(true);
    try {
      setVersions(await resumeService.getVersions(id));
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  const confirmRestore = async () => {
    try {
      const restored = await resumeService.restoreVersion(id, restoreTarget.id);
      setResume(restored);
      setContent(restored.content || '');
      setVersions(await resumeService.getVersions(id));
      toast.success(`Restored "${restoreTarget.name}"`);
      setRestoreTarget(null);
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  const download = (format) => {
    const base = import.meta.env.VITE_API_BASE_URL || '/api/v1';
    fetch(
      `${base}${ENDPOINTS.download}?${new URLSearchParams({ resumeId: id, format })}`,
      { headers: { Authorization: `Bearer ${tokenStorage.getAccess()}` } },
    )
      .then((res) => res.blob())
      .then((blob) => {
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `${resume?.name || 'resume'}.${format.toLowerCase()}`;
        link.click();
        URL.revokeObjectURL(link.href);
        toast.success(`${format} download started`);
      })
      .catch(() => toast.error('Download failed'));
  };

  if (loading) {
    return (
      <DashboardLayout>
        <div className="flex justify-center py-24">
          <Spinner size={32} />
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold">{resume?.name}</h1>
            <div className="mt-1 flex flex-wrap items-center gap-2">
              {resume?.fileType && <Badge color="blue">{resume.fileType}</Badge>}
              {resume?.optimized && <Badge color="green">AI Optimized</Badge>}
              {resume?.atsScore != null && (
                <Badge color={resume.atsScore >= 80 ? 'green' : resume.atsScore >= 60 ? 'amber' : 'red'}>
                  ATS {resume.atsScore}
                </Badge>
              )}
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            <button className="btn-secondary" onClick={loadVersions}>
              <History className="h-4 w-4" />
              Versions
            </button>
            <button className="btn-secondary" onClick={() => download('docx')}>
              <Download className="h-4 w-4" />
              DOCX
            </button>
            <button className="btn-secondary" onClick={() => download('pdf')}>
              <Download className="h-4 w-4" />
              PDF
            </button>
            <button
              className="btn-primary"
              onClick={() => navigate('/optimize', { state: { resumeId: id, resumeName: resume?.name } })}
            >
              <Sparkles className="h-4 w-4" />
              Optimize
            </button>
            <button className="btn-primary !bg-emerald-600 hover:!bg-emerald-700" onClick={save} disabled={saving}>
              {saving ? <Spinner size={16} className="border-white/40 border-t-white" /> : <Save className="h-4 w-4" />}
              Save
            </button>
          </div>
        </div>

        <div className="card overflow-hidden">
          <div className="flex items-center justify-between border-b border-slate-200 px-4 py-2.5 dark:border-slate-800">
            <p className="text-sm font-medium text-slate-500 dark:text-slate-400">
              Use standard section headers (SUMMARY, EXPERIENCE, SKILLS, PROJECTS, EDUCATION) for best ATS parsing.
            </p>
            <ChevronDown className="h-4 w-4 text-slate-400" />
          </div>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            className="min-h-[70vh] w-full resize-y bg-transparent p-5 font-mono text-sm leading-relaxed focus:outline-none"
            spellCheck="true"
            aria-label="Resume content"
          />
        </div>
      </div>

      {/* Versions drawer */}
      <Modal
        open={showVersions}
        onClose={() => setShowVersions(false)}
        title={`Version History (${versions.length})`}
        size="lg"
      >
        <ul className="space-y-2">
          {versions.map((version) => (
            <li
              key={version.id}
              className="flex items-center justify-between gap-3 rounded-lg border border-slate-100 p-3 dark:border-slate-800"
            >
              <div className="min-w-0">
                {renameTargetId === version.id ? (
                  <div className="flex items-center gap-1.5">
                    <input
                      autoFocus
                      value={renameValue}
                      onChange={(e) => setRenameValue(e.target.value)}
                      onKeyDown={(e) =>
                        e.key === 'Enter' && handleRename(version)
                      }
                      onBlur={() => { setRenameTargetId(null); setRenameValue(''); }}
                      className="input !py-1.5 text-sm"
                      maxLength={120}
                    />
                    <button
                      className="rounded-lg p-1.5 text-brand-600 hover:bg-brand-50 dark:hover:bg-brand-900/30"
                      title="Save name"
                      onClick={() => handleRename(version)}
                    >
                      <Check className="h-4 w-4" />
                    </button>
                  </div>
                ) : (
                  <p className="flex items-center gap-2 truncate text-sm font-medium">
                    {version.name}
                    {version.favorite && <Star className="h-3.5 w-3.5 shrink-0 text-amber-400" />}
                    {version.aiGenerated && <Badge color="violet">AI</Badge>}
                  </p>
                )}
                <p className="text-xs text-slate-400">
                  {formatRelativeTime(version.createdAt)}
                  {version.atsScore != null ? ` · ATS ${version.atsScore}` : ''}
                </p>
              </div>
              <div className="flex shrink-0 items-center gap-1">
                <Tooltip label="Rename version">
                  <button
                    className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-brand-600 dark:hover:bg-slate-800"
                    title="Rename version"
                    onClick={() => {
                      setRenameTargetId(version.id);
                      setRenameValue(version.name);
                    }}
                  >
                    <Pencil className="h-4 w-4" />
                  </button>
                </Tooltip>
                <Tooltip label="Restore this version">
                  <button
                    className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-brand-600 dark:hover:bg-slate-800"
                    title="Restore this version"
                    onClick={() => setRestoreTarget(version)}
                  >
                    <Undo2 className="h-4 w-4" />
                  </button>
                </Tooltip>
                <Tooltip label={version.favorite ? 'Remove favorite' : 'Mark favorite'}>
                  <button
                    className={`rounded-lg p-2 hover:bg-slate-100 dark:hover:bg-slate-800 ${
                      version.favorite ? 'text-amber-400' : 'text-slate-400'
                    }`}
                    title="Favorite"
                    onClick={async () => {
                      await resumeService.toggleVersionFavorite(version.id);
                      setVersions(await resumeService.getVersions(id));
                    }}
                  >
                    <Star className="h-4 w-4" />
                  </button>
                </Tooltip>
                <Tooltip label="Delete version">
                  <button
                    className="rounded-lg p-2 text-slate-400 hover:bg-red-50 hover:text-red-500 dark:hover:bg-red-900/20"
                    title="Delete version"
                    onClick={async () => {
                      await resumeService.deleteVersion(id, version.id);
                      setVersions(await resumeService.getVersions(id));
                      toast.success('Version deleted');
                    }}
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </Tooltip>
              </div>
            </li>
          ))}
        </ul>
      </Modal>

      {/* Restore confirmation */}
      <Modal
        open={Boolean(restoreTarget)}
        onClose={() => setRestoreTarget(null)}
        title="Restore version?"
        footer={
          <>
            <button className="btn-secondary" onClick={() => setRestoreTarget(null)}>
              Cancel
            </button>
            <button className="btn-primary" onClick={confirmRestore}>
              Restore
            </button>
          </>
        }
      >
        <p className="text-sm text-slate-500 dark:text-slate-400">
          The current content will be snapshotted as a new version, and{' '}
          <span className="font-medium text-slate-700 dark:text-slate-200">
            {restoreTarget?.name}
          </span>{' '}
          becomes the active content. This can be undone from history.
        </p>
      </Modal>
    </DashboardLayout>
  );
}
