import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { FileCheck2 } from 'lucide-react';
import DashboardLayout from '../components/layout/DashboardLayout';
import FileDropzone from '../components/common/FileDropzone';
import Modal from '../components/common/Modal';
import Spinner from '../components/common/Spinner';
import { useToast } from '../hooks/useToast';
import { getErrorMessage } from '../utils/helpers';
import { isAllowedResumeFile } from '../utils/validators';
import { EMPTY_RESUME_TEMPLATE } from '../utils/constants';
import resumeService from '../services/resumeService';

/**
 * Upload (or create) a resume.
 * URL param ?mode=create switches to blank resume creation from a template.
 */
export default function UploadResume() {
  const toast = useToast();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const mode = searchParams.get('mode');

  const [file, setFile] = useState(null);
  const [error, setError] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [name, setName] = useState('');
  const [showNameModal, setShowNameModal] = useState(false);

  const [createName, setCreateName] = useState('');
  const [template, setTemplate] = useState('modern');
  const [creating, setCreating] = useState(false);

  const handleFile = (result) => {
    if (result?.error) {
      setError(result.error);
      setFile(null);
      return;
    }
    if (!isAllowedResumeFile(result)) {
      setError('Only PDF and DOCX files are supported');
      return;
    }
    setError(null);
    setFile(result);
    setName(result.name.replace(/\.[^.]+$/, ''));
    setShowNameModal(true);
  };

  const confirmUpload = async () => {
    setUploading(true);
    try {
      const created = await resumeService.upload(file, name.trim() || undefined);
      toast.success('Resume uploaded and parsed');
      navigate(`/resumes/${created.id}/edit`);
    } catch (err) {
      toast.error(getErrorMessage(err, 'Upload failed'));
    } finally {
      setUploading(false);
      setShowNameModal(false);
    }
  };

  const createResume = async () => {
    setCreating(true);
    try {
      const created = await resumeService.create({
        name: createName.trim() || 'Untitled resume',
        template,
        content: EMPTY_RESUME_TEMPLATE,
      });
      toast.success('Resume created');
      navigate(`/resumes/${created.id}/edit`);
    } catch (err) {
      toast.error(getErrorMessage(err, 'Could not create resume'));
    } finally {
      setCreating(false);
    }
  };

  return (
    <DashboardLayout>
      <div className="mx-auto max-w-2xl space-y-6">
        <div>
          <h1 className="text-2xl font-bold">
            {mode === 'create' ? 'Create a resume' : 'Upload your resume'}
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            {mode === 'create'
              ? 'Start from a clean, ATS-friendly template.'
              : 'We extract the text, keep the original file, and you can edit everything.'}
          </p>
        </div>

        {mode === 'create' ? (
          <div className="card space-y-4 p-6">
            <div>
              <label className="label" htmlFor="createName">
                Resume name
              </label>
              <input
                id="createName"
                className="input"
                placeholder="e.g. Software Engineer Resume"
                value={createName}
                onChange={(e) => setCreateName(e.target.value)}
              />
            </div>
            <div>
              <label className="label">Template</label>
              <div className="grid gap-3 sm:grid-cols-2">
                {[
                  { id: 'modern', label: 'Modern', desc: 'Clean two-tone layout' },
                  { id: 'classic', label: 'Classic', desc: 'Traditional serif' },
                  { id: 'compact', label: 'Compact', desc: 'Dense one page' },
                  { id: 'creative', label: 'Creative', desc: 'Accent skills bars' },
                ].map((t) => (
                  <button
                    key={t.id}
                    type="button"
                    onClick={() => setTemplate(t.id)}
                    className={`rounded-lg border p-4 text-left transition-colors ${
                      template === t.id
                        ? 'border-brand-500 bg-brand-50 dark:bg-brand-900/30'
                        : 'border-slate-200 dark:border-slate-800 hover:border-brand-300'
                    }`}
                  >
                    <p className="font-medium">{t.label}</p>
                    <p className="text-xs text-slate-500">{t.desc}</p>
                  </button>
                ))}
              </div>
            </div>
            <button className="btn-primary w-full" onClick={createResume} disabled={creating}>
              {creating ? <Spinner size={16} className="border-white/40 border-t-white" /> : 'Create resume'}
            </button>
          </div>
        ) : (
          <>
            <FileDropzone onFile={handleFile} />
            {error && (
              <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600 dark:bg-red-900/30 dark:text-red-300">
                {error}
              </p>
            )}
            {file && (
              <div className="card flex items-center gap-3 p-4">
                <FileCheck2 className="h-6 w-6 text-emerald-500" />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium">{file.name}</p>
                  <p className="text-xs text-slate-400">
                    {(file.size / 1024).toFixed(1)} KB · ready to parse
                  </p>
                </div>
                <button className="btn-primary" onClick={() => setShowNameModal(true)}>
                  Continue
                </button>
              </div>
            )}
          </>
        )}
      </div>

      {/* Name confirmation modal */}
      <Modal
        open={showNameModal}
        onClose={() => setShowNameModal(false)}
        title="Name your resume"
        footer={
          <>
            <button className="btn-secondary" onClick={() => setShowNameModal(false)}>
              Cancel
            </button>
            <button className="btn-primary" onClick={confirmUpload} disabled={uploading}>
              {uploading ? <Spinner size={16} className="border-white/40 border-t-white" /> : 'Upload & parse'}
            </button>
          </>
        }
      >
        <div>
          <label className="label" htmlFor="resumeName">Resume name</label>
          <input
            id="resumeName"
            className="input"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </div>
      </Modal>
    </DashboardLayout>
  );
}
