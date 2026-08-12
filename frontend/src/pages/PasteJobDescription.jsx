import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Sparkles, ClipboardPaste, FileUp, ChevronDown } from 'lucide-react';
import DashboardLayout from '../components/layout/DashboardLayout';
import FileDropzone from '../components/common/FileDropzone';
import Spinner from '../components/common/Spinner';
import Badge from '../components/common/Badge';
import { useToast } from '../hooks/useToast';
import { getErrorMessage } from '../utils/helpers';
import { useFetch } from '../hooks/useFetch';
import resumeService from '../services/resumeService';
import jobDescriptionService from '../services/jobDescriptionService';
import optimizationService from '../services/optimizationService';

/**
 * Optimize flow: pick a resume, paste or upload a job description,
 * see extracted insights, then run AI optimization.
 */
export default function PasteJobDescription() {
  const toast = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const preselectedResumeId = location.state?.resumeId;

  const [tab, setTab] = useState('paste');
  const [resumeId, setResumeId] = useState(preselectedResumeId || '');
  const [jdText, setJdText] = useState('');
  const [jdTitle, setJdTitle] = useState('');
  const [jdResult, setJdResult] = useState(null);
  const [jdUploading, setJdUploading] = useState(false);
  const [optimizing, setOptimizing] = useState(false);

  const { data: resumesData } = useFetch(() => resumeService.getAll({ size: 50 }), []);

  useEffect(() => {
    if (preselectedResumeId) setResumeId(preselectedResumeId);
  }, [preselectedResumeId]);

  const analyzeJd = async () => {
    if (jdText.trim().length < 50) {
      toast.error('Job description is too short - paste at least a few sentences');
      return;
    }
    setJdUploading(true);
    try {
      const result = await jobDescriptionService.paste(jdText, jdTitle || undefined);
      setJdResult(result);
      toast.success('Job description analyzed');
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setJdUploading(false);
    }
  };

  const uploadJd = async (file) => {
    if (file?.error) {
      toast.error(file.error);
      return;
    }
    setJdUploading(true);
    try {
      const result = await jobDescriptionService.upload(file);
      setJdResult(result);
      setTab('paste');
      toast.success('Job description uploaded and analyzed');
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setJdUploading(false);
    }
  };

  const optimize = async () => {
    if (!resumeId) {
      toast.error('Select a resume first');
      return;
    }
    if (!jdResult) {
      toast.error('Analyze a job description first');
      return;
    }
    setOptimizing(true);
    try {
      const result = await optimizationService.optimize(resumeId, jdResult.id, null);
      const chosen = (resumesData?.content || []).find((r) => r.id === Number(resumeId));
      navigate('/optimize/result', {
        state: { result, originalContent: chosen?.content || '' },
      });
    } catch (err) {
      toast.error(getErrorMessage(err, 'Optimization failed'));
    } finally {
      setOptimizing(false);
    }
  };

  return (
    <DashboardLayout>
      <div className="mx-auto max-w-3xl space-y-6">
        <div>
          <h1 className="flex items-center gap-2 text-2xl font-bold">
            <Sparkles className="h-6 w-6 text-brand-600 dark:text-brand-400" />
            Optimize your resume
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Step 1: pick a resume. Step 2: provide the job description. Step 3: watch the AI work.
          </p>
        </div>

        {/* Step 1: resume */}
        <section className="card p-5">
          <h2 className="mb-3 font-semibold">1. Choose a resume</h2>
          <div className="relative">
            <select
              className="input w-full cursor-pointer appearance-none pr-10"
              value={resumeId}
              onChange={(e) => setResumeId(e.target.value)}
            >
              <option value="">Select a resume…</option>
              {(resumesData?.content || []).map((resume) => (
                <option key={resume.id} value={resume.id}>
                  {resume.name}
                  {resume.atsScore != null ? ` (ATS ${resume.atsScore})` : ''}
                </option>
              ))}
            </select>
            <ChevronDown className="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          </div>
        </section>

        {/* Step 2: job description */}
        <section className="card p-5">
          <h2 className="mb-3 font-semibold">2. Job description</h2>
          <div className="mb-4 inline-flex rounded-lg border border-slate-200 p-0.5 text-sm dark:border-slate-700">
            <button
              onClick={() => setTab('paste')}
              className={`flex items-center gap-1.5 rounded-md px-4 py-2 font-medium transition-colors ${
                tab === 'paste' ? 'bg-brand-600 text-white' : 'text-slate-500'
              }`}
            >
              <ClipboardPaste className="h-4 w-4" /> Paste
            </button>
            <button
              onClick={() => setTab('upload')}
              className={`flex items-center gap-1.5 rounded-md px-4 py-2 font-medium transition-colors ${
                tab === 'upload' ? 'bg-brand-600 text-white' : 'text-slate-500'
              }`}
            >
              <FileUp className="h-4 w-4" /> Upload
            </button>
          </div>

          {tab === 'paste' ? (
            <div className="space-y-3">
              <input
                className="input"
                placeholder="Job title (optional)"
                value={jdTitle}
                onChange={(e) => setJdTitle(e.target.value)}
              />
              <textarea
                className="input min-h-56 resize-y font-mono text-sm"
                placeholder="Paste the full job description here…"
                value={jdText}
                onChange={(e) => setJdText(e.target.value)}
              />
              <button className="btn-primary" onClick={analyzeJd} disabled={jdUploading}>
                {jdUploading ? <Spinner size={16} className="border-white/40 border-t-white" /> : 'Analyze job description'}
              </button>
            </div>
          ) : (
            <FileDropzone
              onFile={uploadJd}
              label="Upload the job description file"
              sublabel="PDF or DOCX - text will be extracted automatically"
            />
          )}
        </section>

        {/* Extracted insights */}
        {jdResult && (
          <section className="card animate-fade-in p-5">
            <h2 className="mb-3 font-semibold">Extracted insights</h2>
            <div className="grid gap-4 sm:grid-cols-2">
              {[
                { label: 'Required Skills', items: jdResult.skills },
                { label: 'Tools & Technologies', items: jdResult.tools },
                { label: 'Responsibilities', items: jdResult.responsibilities },
                { label: 'Keywords', items: jdResult.keywords },
                { label: 'Experience', items: jdResult.experience },
                { label: 'Education', items: jdResult.education },
              ].map(({ label, items }) => (
                <div key={label}>
                  <p className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-slate-400">{label}</p>
                  {items?.length ? (
                    <div className="flex flex-wrap gap-1.5">
                      {items.map((item) => (
                        <Badge key={item} color="blue">{item}</Badge>
                      ))}
                    </div>
                  ) : (
                    <p className="text-sm text-slate-400">Not detected</p>
                  )}
                </div>
              ))}
            </div>
          </section>
        )}

        {/* Step 3: run */}
        {jdResult && (
          <button
            className="btn-primary w-full !py-4 !text-base"
            onClick={optimize}
            disabled={optimizing}
          >
            {optimizing ? (
              <>
                <Spinner size={18} className="border-white/40 border-t-white" />
                AI is optimizing your resume…
              </>
            ) : (
              <>
                <Sparkles className="h-5 w-5" />
                Optimize resume with AI
              </>
            )}
          </button>
        )}
      </div>
    </DashboardLayout>
  );
}
