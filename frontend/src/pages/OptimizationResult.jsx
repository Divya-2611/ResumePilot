import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  Download,
  Save,
  Wand2,
  AlertTriangle,
  SpellCheck,
  Layout,
  BookOpen,
} from 'lucide-react';
import DashboardLayout from '../components/layout/DashboardLayout';
import AtsScoreGauge from '../components/optimization/AtsScoreGauge';
import KeywordChips from '../components/optimization/KeywordChips';
import SideBySideView from '../components/optimization/SideBySideView';
import Spinner from '../components/common/Spinner';
import Modal from '../components/common/Modal';
import { useToast } from '../hooks/useToast';
import { getErrorMessage } from '../utils/helpers';
import resumeService from '../services/resumeService';
import { tokenStorage } from '../api/axiosClient';
import { ENDPOINTS } from '../api/endpoints';

/**
 * Optimization result: ATS score, keyword match, suggestions, side-by-side
 * comparison, save-as-version and downloads.
 */
export default function OptimizationResult() {
  const location = useLocation();
  const navigate = useNavigate();
  const toast = useToast();
  const result = location.state?.result;

  const [originalContent, setOriginalContent] = useState(location.state?.originalContent || '');
  const [originalNote, setOriginalNote] = useState('');
  const [saveModal, setSaveModal] = useState(false);
  const [versionName, setVersionName] = useState('');
  const [saving, setSaving] = useState(false);

  // Load the resume's current content for comparison (captured before optimize).
  useEffect(() => {
    if (!result) return;
    let cancelled = false;
    resumeService
      .getById(result.resumeId)
      .then((resume) => {
        if (cancelled) return;
        setOriginalContent((prev) => prev || resume.content || '');
        if (!resume.content) setOriginalNote('The original resume text could not be loaded for comparison.');
      })
      .catch(() => {
        if (!cancelled) setOriginalNote('Could not load the original resume for comparison - the backend may be offline.');
      });
    return () => {
      cancelled = true;
    };
  }, [result]);

  if (!result) {
    return (
      <DashboardLayout>
        <div className="card p-8 text-center">
          <p className="text-slate-500">No optimization result found. Run an optimization first.</p>
          <button className="btn-primary mt-4" onClick={() => navigate('/optimize')}>
            Go to optimize
          </button>
        </div>
      </DashboardLayout>
    );
  }

  const { analysis, optimizedContent, optimizedSections, aiGenerated, aiProvider, resumeName } = result;

  const saveVersion = async () => {
    setSaving(true);
    try {
      await resumeService.saveVersion({
        resumeId: result.resumeId,
        name: versionName.trim() || 'Optimized version',
        label: 'AI optimization result',
        content: optimizedContent,
        structure: JSON.stringify(optimizedSections || {}),
        atsScore: analysis.atsScore,
        keywordMatchPercent: analysis.keywordMatchPercent,
      });
      toast.success('Version saved to your resume');
      setSaveModal(false);
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const download = (format) => {
    const base = import.meta.env.VITE_API_BASE_URL || '/api/v1';
    const params = new URLSearchParams({ resumeId: result.resumeId, format });
    // Download the auto-saved optimized version, not the original resume.
    if (result.resultVersionId) params.set('versionId', result.resultVersionId);
    fetch(
      `${base}${ENDPOINTS.download}?${params}`,
      { headers: { Authorization: `Bearer ${tokenStorage.getAccess()}` } },
    )
      .then((res) => res.blob())
      .then((blob) => {
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `${resumeName || 'resume'}-optimized.${format.toLowerCase()}`;
        link.click();
      })
      .catch(() => toast.error('Download failed'));
  };

  const scoreColor = analysis.atsScore >= 80 ? 'text-emerald-500' : analysis.atsScore >= 60 ? 'text-amber-500' : 'text-red-500';

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold">Optimization Result</h1>
            <p className="text-sm text-slate-500">
              {resumeName}
              {aiGenerated ? (
                <span className="ml-2 text-emerald-500">· optimized by {aiProvider}</span>
              ) : (
                <span className="ml-2 text-amber-500">· AI unavailable - local analysis</span>
              )}
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button className="btn-primary" onClick={() => setSaveModal(true)}>
              <Save className="h-4 w-4" /> Save version
            </button>
            <button className="btn-secondary" onClick={() => download('docx')}>
              <Download className="h-4 w-4" /> DOCX
            </button>
            <button className="btn-secondary" onClick={() => download('pdf')}>
              <Download className="h-4 w-4" /> PDF
            </button>
          </div>
        </div>

        {/* Score + keywords */}
        <div className="grid gap-4 lg:grid-cols-3">
          <div className="card flex flex-col items-center justify-center p-6">
            <AtsScoreGauge score={analysis.atsScore} />
            <div className="mt-2 grid w-full grid-cols-2 gap-2 text-center text-xs">
              <div className="rounded-lg bg-slate-50 p-2 dark:bg-slate-800">
                <p className="font-bold text-slate-700 dark:text-slate-200">{analysis.keywordMatchPercent}%</p>
                <p className="text-slate-400">Keyword match</p>
              </div>
              <div className="rounded-lg bg-slate-50 p-2 dark:bg-slate-800">
                <p className="font-bold text-slate-700 dark:text-slate-200">{analysis.readabilityScore}</p>
                <p className="text-slate-400">Readability</p>
              </div>
            </div>
          </div>

          <div className="card p-6 lg:col-span-2">
            <h2 className="mb-3 font-semibold">Keyword Analysis</h2>
            <KeywordChips matched={analysis.matchedKeywords} missing={analysis.missingKeywords} />
          </div>
        </div>

        {/* Suggestions */}
        <div className="grid gap-4 md:grid-cols-2">
          <div className="card p-5">
            <h2 className="mb-3 flex items-center gap-2 font-semibold">
              <AlertTriangle className="h-4 w-4 text-red-500" /> Weak Bullet Points
            </h2>
            {analysis.weakBulletPoints?.length ? (
              <ul className="space-y-2 text-sm text-slate-600 dark:text-slate-300">
                {analysis.weakBulletPoints.map((point, i) => (
                  <li key={i} className="rounded-lg bg-red-50 px-3 py-2 dark:bg-red-900/20">
                    {point}
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-sm text-emerald-500">No weak bullets detected</p>
            )}
          </div>
          <div className="space-y-4">
            <div className="card p-5">
              <h2 className="mb-3 flex items-center gap-2 font-semibold">
                <SpellCheck className="h-4 w-4 text-violet-500" /> Grammar Suggestions
              </h2>
              <ul className="space-y-1.5 text-sm text-slate-600 dark:text-slate-300">
                {analysis.grammarSuggestions?.map((s, i) => (
                  <li key={i}>· {s}</li>
                ))}
              </ul>
            </div>
            <div className="card p-5">
              <h2 className="mb-3 flex items-center gap-2 font-semibold">
                <Layout className="h-4 w-4 text-brand-500" /> Formatting Suggestions
              </h2>
              <ul className="space-y-1.5 text-sm text-slate-600 dark:text-slate-300">
                {analysis.formattingSuggestions?.map((s, i) => (
                  <li key={i}>· {s}</li>
                ))}
              </ul>
            </div>
          </div>
        </div>

        {/* Score breakdown */}
        <div className="card p-5">
          <h2 className="mb-3 flex items-center gap-2 font-semibold">
            <BookOpen className="h-4 w-4 text-emerald-500" /> Score Breakdown
          </h2>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {Object.entries(analysis.scoreBreakdown || {}).map(([key, value]) => (
              <div key={key} className="rounded-lg border border-slate-100 p-3 dark:border-slate-800">
                <p className="text-xs uppercase tracking-wide text-slate-400">{key}</p>
                <p className={`mt-0.5 text-lg font-bold ${scoreColor}`}>{value}</p>
              </div>
            ))}
          </div>
        </div>

        {/* Side-by-side */}
        <div>
          <h2 className="mb-3 font-semibold">Comparison</h2>
          {originalNote && <p className="mb-2 text-xs text-amber-500">{originalNote}</p>}
          <SideBySideView
            original={originalContent}
            optimized={optimizedContent}
            missingKeywords={analysis.missingKeywords}
          />
        </div>
      </div>

      {/* Save version modal */}
      <Modal
        open={saveModal}
        onClose={() => setSaveModal(false)}
        title="Save optimized version"
        footer={
          <>
            <button className="btn-secondary" onClick={() => setSaveModal(false)}>Cancel</button>
            <button className="btn-primary" onClick={saveVersion} disabled={saving}>
              {saving ? <Spinner size={16} className="border-white/40 border-t-white" /> : 'Save version'}
            </button>
          </>
        }
      >
        <div>
          <label className="label" htmlFor="versionName">Version name</label>
          <input
            id="versionName"
            className="input"
            placeholder="e.g. Optimized for Senior Java role"
            value={versionName}
            onChange={(e) => setVersionName(e.target.value)}
          />
          <p className="mt-2 flex items-center gap-1.5 text-xs text-slate-400">
            <Wand2 className="h-3.5 w-3.5" />
            Saved as a new version - your original is preserved in history.
          </p>
        </div>
      </Modal>
    </DashboardLayout>
  );
}
