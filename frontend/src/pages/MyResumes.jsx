import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Plus, Search, Star } from 'lucide-react';
import DashboardLayout from '../components/layout/DashboardLayout';
import ResumeCard from '../components/resume/ResumeCard';
import { SkeletonGrid } from '../components/common/Skeleton';
import EmptyState from '../components/common/EmptyState';
import Pagination from '../components/common/Pagination';
import Modal from '../components/common/Modal';
import { useFetch } from '../hooks/useFetch';
import { useDebounce } from '../hooks/useDebounce';
import { useToast } from '../hooks/useToast';
import { getErrorMessage } from '../utils/helpers';
import resumeService from '../services/resumeService';

/**
 * Resume library: grid, search, favorites filter, pagination and quick actions.
 */
export default function MyResumes() {
  const toast = useToast();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [favoritesOnly, setFavoritesOnly] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [renameTarget, setRenameTarget] = useState(null);
  const [renameValue, setRenameValue] = useState('');
  const [renaming, setRenaming] = useState(false);
  const debouncedSearch = useDebounce(search, 400);

  const { data, loading, refetch } = useFetch(
    () =>
      resumeService.getAll({
        page,
        size: 9,
        search: debouncedSearch,
        favorites: favoritesOnly,
      }),
    [page, debouncedSearch, favoritesOnly],
  );

  const resumes = data?.content || [];
  const totalPages = data?.totalPages || 0;

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await resumeService.delete(deleteTarget.id);
      toast.success(`Deleted "${deleteTarget.name}"`);
      setDeleteTarget(null);
      refetch();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setDeleting(false);
    }
  };

  const handleDuplicate = async (resume) => {
    try {
      await resumeService.duplicate(resume.id);
      toast.success('Resume duplicated');
      refetch();
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  const handleRename = async () => {
    if (!renameTarget) return;
    const name = renameValue.trim();
    if (!name) return;
    setRenaming(true);
    try {
      await resumeService.update(renameTarget.id, { name });
      toast.success(`Renamed to "${name}"`);
      setRenameTarget(null);
      refetch();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setRenaming(false);
    }
  };

  const handleFavorite = async (resume) => {
    try {
      await resumeService.toggleFavorite(resume.id);
      refetch();
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold">My Resumes</h1>
            <p className="text-sm text-slate-500 dark:text-slate-400">
              {data?.totalElements ?? 0} resume{(data?.totalElements ?? 0) === 1 ? '' : 's'}
            </p>
          </div>
          <div className="flex gap-2">
            <Link to="/resumes/upload" className="btn-secondary">
              <Plus className="h-4 w-4" />
              Upload
            </Link>
            <Link to="/resumes/upload?mode=create" className="btn-primary">
              <Plus className="h-4 w-4" />
              New Resume
            </Link>
          </div>
        </div>

        {/* Search + filters */}
        <div className="flex flex-wrap items-center gap-3">
          <div className="relative min-w-56 flex-1">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              className="input pl-9"
              placeholder="Search resumes…"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
            />
          </div>
          <button
            onClick={() => {
              setFavoritesOnly((f) => !f);
              setPage(0);
            }}
            className={`inline-flex items-center gap-2 rounded-lg px-4 py-2.5 text-sm font-medium transition-colors ${
              favoritesOnly
                ? 'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300'
                : 'border border-slate-300 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800'
            }`}
          >
            <Star className="h-4 w-4" />
            Favorites
          </button>
        </div>

        {/* Grid */}
        {loading ? (
          <SkeletonGrid count={6} />
        ) : resumes.length === 0 ? (
          <EmptyState
            icon={Search}
            title={debouncedSearch || favoritesOnly ? 'No matching resumes' : 'No resumes yet'}
            description={
              debouncedSearch || favoritesOnly
                ? 'Try a different search or clear the favorites filter.'
                : 'Upload a PDF/DOCX or create a new resume to get started.'
            }
            action={
              !debouncedSearch && !favoritesOnly ? (
                <Link to="/resumes/upload" className="btn-primary">
                  Upload your first resume
                </Link>
              ) : null
            }
          />
        ) : (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {resumes.map((resume) => (
              <ResumeCard
                key={resume.id}
                resume={resume}
                onDelete={() => setDeleteTarget(resume)}
                onDuplicate={handleDuplicate}
                onRename={(r) => {
                  setRenameTarget(r);
                  setRenameValue(r.name);
                }}
                onToggleFavorite={() => handleFavorite(resume)}
              />
            ))}
          </div>
        )}

        <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
      </div>

      {/* Rename resume */}
      <Modal
        open={Boolean(renameTarget)}
        onClose={() => setRenameTarget(null)}
        title="Rename resume"
        footer={
          <>
            <button className="btn-secondary" onClick={() => setRenameTarget(null)}>
              Cancel
            </button>
            <button className="btn-primary" onClick={handleRename} disabled={renaming || !renameValue.trim()}>
              {renaming ? 'Saving…' : 'Save'}
            </button>
          </>
        }
      >
        <input
          autoFocus
          value={renameValue}
          onChange={(e) => setRenameValue(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleRename()}
          className="input"
          maxLength={120}
          placeholder="Resume name"
        />
      </Modal>

      {/* Delete confirmation */}
      <Modal
        open={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        title="Delete resume?"
        footer={
          <>
            <button className="btn-secondary" onClick={() => setDeleteTarget(null)}>
              Cancel
            </button>
            <button className="btn-danger" onClick={handleDelete} disabled={deleting}>
              {deleting ? 'Deleting…' : 'Delete forever'}
            </button>
          </>
        }
      >
        <p className="text-sm text-slate-500 dark:text-slate-400">
          <span className="font-medium text-slate-700 dark:text-slate-200">
            {deleteTarget?.name}
          </span>{' '}
          and all of its versions will be permanently deleted. This cannot be undone.
        </p>
      </Modal>
    </DashboardLayout>
  );
}
