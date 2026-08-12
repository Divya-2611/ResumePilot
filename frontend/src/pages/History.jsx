import { useState } from 'react';
import { Search, Download, Undo2, FileSpreadsheet } from 'lucide-react';
import DashboardLayout from '../components/layout/DashboardLayout';
import Badge from '../components/common/Badge';
import Pagination from '../components/common/Pagination';
import EmptyState from '../components/common/EmptyState';
import Modal from '../components/common/Modal';
import Tooltip from '../components/common/Tooltip';
import { Skeleton } from '../components/common/Skeleton';
import { useFetch } from '../hooks/useFetch';
import { useDebounce } from '../hooks/useDebounce';
import { useToast } from '../hooks/useToast';
import { getErrorMessage } from '../utils/helpers';
import { formatDate } from '../utils/formatters';
import historyService from '../services/historyService';
import resumeService from '../services/resumeService';
import { tokenStorage } from '../api/axiosClient';
import { ENDPOINTS } from '../api/endpoints';

/**
 * Optimization history: searchable, paginated table with download, restore
 * and CSV export.
 */
export default function History() {
  const toast = useToast();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [restoreTarget, setRestoreTarget] = useState(null);
  const debouncedSearch = useDebounce(search, 400);

  const { data, loading, refetch } = useFetch(
    () => historyService.getAll({ page, size: 10, search: debouncedSearch }),
    [page, debouncedSearch],
  );

  const rows = data?.content || [];

  const download = (item, format) => {
    if (!item.resumeId) {
      toast.info('This resume no longer exists');
      return;
    }
    const params = new URLSearchParams({ resumeId: item.resumeId, format });
    if (item.resultVersionId) params.set('versionId', item.resultVersionId);
    const base = import.meta.env.VITE_API_BASE_URL || '/api/v1';
    fetch(`${base}${ENDPOINTS.download}?${params}`, {
      headers: { Authorization: `Bearer ${tokenStorage.getAccess()}` },
    })
      .then((res) => res.blob())
      .then((blob) => {
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `${item.resumeName}.${format.toLowerCase()}`;
        link.click();
      })
      .catch(() => toast.error('Download failed'));
  };

  const confirmRestore = async () => {
    if (!restoreTarget?.resumeId || !restoreTarget?.resultVersionId) {
      toast.error('This entry cannot be restored');
      setRestoreTarget(null);
      return;
    }
    try {
      await resumeService.restoreVersion(restoreTarget.resumeId, restoreTarget.resultVersionId);
      toast.success(`Restored "${restoreTarget.resumeName}"`);
      setRestoreTarget(null);
      refetch();
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  const exportCsv = async () => {
    try {
      await historyService.exportCsv();
      toast.success('History exported');
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold">Optimization History</h1>
            <p className="text-sm text-slate-500 dark:text-slate-400">
              {data?.totalElements ?? 0} optimization{data?.totalElements === 1 ? '' : 's'}
            </p>
          </div>
          <button className="btn-secondary" onClick={exportCsv}>
            <FileSpreadsheet className="h-4 w-4" />
            Export CSV
          </button>
        </div>

        <div className="relative max-w-md">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            className="input pl-9"
            placeholder="Search by resume or job title…"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
          />
        </div>

        {loading ? (
          <div className="space-y-2">
            {[1, 2, 3, 4, 5].map((i) => (
              <Skeleton key={i} className="h-16" />
            ))}
          </div>
        ) : rows.length === 0 ? (
          <EmptyState
            icon={Search}
            title={debouncedSearch ? 'No matching history' : 'No optimizations yet'}
            description={
              debouncedSearch
                ? 'Try a different search term.'
                : 'Optimize a resume against a job description to build your history.'
            }
          />
        ) : (
          <div className="card overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-left text-xs uppercase tracking-wide text-slate-400 dark:border-slate-800">
                  <th className="px-4 py-3">Date</th>
                  <th className="px-4 py-3">Resume</th>
                  <th className="px-4 py-3">Job Description</th>
                  <th className="px-4 py-3 text-center">ATS</th>
                  <th className="px-4 py-3 text-center">Keywords</th>
                  <th className="px-4 py-3">Provider</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {rows.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/50">
                    <td className="whitespace-nowrap px-4 py-3 text-slate-500">{formatDate(item.createdAt)}</td>
                    <td className="max-w-44 truncate px-4 py-3 font-medium">{item.resumeName}</td>
                    <td className="max-w-52 truncate px-4 py-3 text-slate-500">{item.jobTitle || '—'}</td>
                    <td className="px-4 py-3 text-center">
                      <Badge
                        color={item.atsScore >= 80 ? 'green' : item.atsScore >= 60 ? 'amber' : 'red'}
                      >
                        {item.atsScore ?? '—'}
                      </Badge>
                    </td>
                    <td className="px-4 py-3 text-center text-slate-500">{item.keywordMatchPercent ?? '—'}%</td>
                    <td className="px-4 py-3">
                      <Badge color="blue">{item.aiProvider || 'LOCAL'}</Badge>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end gap-1">
                        <Tooltip label="Restore this result">
                          <button
                            className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-brand-600 dark:hover:bg-slate-800"
                            title="Restore this result"
                            onClick={() => setRestoreTarget(item)}
                          >
                            <Undo2 className="h-4 w-4" />
                          </button>
                        </Tooltip>
                        <Tooltip label="Download PDF">
                          <button
                            className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-brand-600 dark:hover:bg-slate-800"
                            title="Download PDF"
                            onClick={() => download(item, 'pdf')}
                          >
                            <Download className="h-4 w-4" />
                          </button>
                        </Tooltip>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <Pagination page={page} totalPages={data?.totalPages || 0} onPageChange={setPage} />
      </div>

      <Modal
        open={Boolean(restoreTarget)}
        onClose={() => setRestoreTarget(null)}
        title="Restore version?"
        footer={
          <>
            <button className="btn-secondary" onClick={() => setRestoreTarget(null)}>Cancel</button>
            <button className="btn-primary" onClick={confirmRestore}>Restore</button>
          </>
        }
      >
        <p className="text-sm text-slate-500 dark:text-slate-400">
          Restore the result of <span className="font-medium">{restoreTarget?.resumeName}</span> from{' '}
          {formatDate(restoreTarget?.createdAt)}. You can undo this from the resume's version history.
        </p>
      </Modal>
    </DashboardLayout>
  );
}
