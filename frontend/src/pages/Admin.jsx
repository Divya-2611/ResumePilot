import { useState } from 'react';
import { Search, Trash2, ShieldCheck, Users } from 'lucide-react';
import DashboardLayout from '../components/layout/DashboardLayout';
import Badge from '../components/common/Badge';
import Pagination from '../components/common/Pagination';
import EmptyState from '../components/common/EmptyState';
import Modal from '../components/common/Modal';
import { Skeleton } from '../components/common/Skeleton';
import { useFetch } from '../hooks/useFetch';
import { useDebounce } from '../hooks/useDebounce';
import { useToast } from '../hooks/useToast';
import { useAuth } from '../hooks/useAuth';
import { getErrorMessage } from '../utils/helpers';
import { formatDate } from '../utils/formatters';
import adminService from '../services/adminService';

const ROLE_COLORS = { ADMIN: 'violet', USER: 'blue' };
const VERIFIED_COLORS = { VERIFIED: 'green', PENDING: 'amber' };

export default function Admin() {
  const toast = useToast();
  const { user: currentUser } = useAuth();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [busyId, setBusyId] = useState(null);
  const debouncedSearch = useDebounce(search, 400);

  const { data, loading, refetch } = useFetch(
    () => adminService.listUsers({ page, size: 10, search: debouncedSearch }),
    [page, debouncedSearch],
  );

  const users = data?.content || [];

  const isSelf = (id) => id === currentUser?.id;
  const isAdmin = (roles = []) => (roles || []).includes('ADMIN');

  const changeRole = async (u, role) => {
    setBusyId(u.id);
    try {
      await adminService.changeRole(u.id, role);
      toast.success(`${u.firstName} ${u.lastName || ''} is now ${role}`);
      refetch();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusyId(null);
    }
  };

  const confirmDelete = async () => {
    if (!deleteTarget) return;
    setBusyId(deleteTarget.id);
    try {
      await adminService.deleteUser(deleteTarget.id);
      toast.success(`Deleted ${deleteTarget.email}`);
      setDeleteTarget(null);
      refetch();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="flex items-center gap-2 text-2xl font-bold">
              <ShieldCheck className="h-6 w-6 text-violet-600 dark:text-violet-400" />
              Admin Panel
            </h1>
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Manage all user accounts on ResumePilot.
            </p>
          </div>
        </div>

        <div className="relative max-w-md">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            className="input pl-9"
            placeholder="Search by name or email…"
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
        ) : users.length === 0 ? (
          <EmptyState
            icon={Users}
            title={search ? 'No matching users' : 'No users yet'}
            description={search ? 'Try a different search term.' : 'Signups will appear here.'}
          />
        ) : (
          <div className="card overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-left text-xs uppercase tracking-wide text-slate-400 dark:border-slate-800">
                  <th className="px-4 py-3">User</th>
                  <th className="px-4 py-3">Email</th>
                  <th className="px-4 py-3">Role</th>
                  <th className="px-4 py-3">Verified</th>
                  <th className="px-4 py-3">Joined</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {users.map((u) => (
                  <tr key={u.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/50">
                    <td className="whitespace-nowrap px-4 py-3 font-medium">
                      {u.firstName} {u.lastName}
                      {isSelf(u.id) && (
                        <span className="ml-2 text-xs font-normal text-slate-400">(you)</span>
                      )}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-slate-500">{u.email}</td>
                    <td className="px-4 py-3">
                      <Badge color={ROLE_COLORS[u.roles?.[0]] || 'slate'}>{u.roles?.[0] || '—'}</Badge>
                    </td>
                    <td className="px-4 py-3">
                      <Badge color={VERIFIED_COLORS[u.emailVerified] || 'slate'}>{u.emailVerified || '—'}</Badge>
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-slate-500">{formatDate(u.createdAt)}</td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end gap-1">
                        {!isSelf(u.id) && (
                          <button
                            className="rounded-lg px-2.5 py-1.5 text-xs font-medium text-violet-600 hover:bg-violet-50 dark:text-violet-400 dark:hover:bg-violet-900/20"
                            disabled={busyId === u.id}
                            onClick={() => changeRole(u, isAdmin(u.roles) ? 'USER' : 'ADMIN')}
                          >
                            {isAdmin(u.roles) ? 'Demote' : 'Promote'}
                          </button>
                        )}
                        {!isSelf(u.id) && (
                          <button
                            className="rounded-lg p-2 text-slate-400 hover:bg-red-50 hover:text-red-500 dark:hover:bg-red-900/20"
                            title="Delete user"
                            disabled={busyId === u.id}
                            onClick={() => setDeleteTarget(u)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        )}
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
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        title="Delete user?"
        footer={
          <>
            <button className="btn-secondary" onClick={() => setDeleteTarget(null)}>
              Cancel
            </button>
            <button className="btn-danger" onClick={confirmDelete} disabled={busyId === deleteTarget?.id}>
              {busyId === deleteTarget?.id ? 'Deleting…' : 'Delete user'}
            </button>
          </>
        }
      >
        <p className="text-sm text-slate-500 dark:text-slate-400">
          This is <span className="font-semibold text-red-500">permanent</span>. All of{' '}
          <span className="font-medium text-slate-700 dark:text-slate-200">{deleteTarget?.email}</span>
          {' '}resumes, versions, files and history will be deleted immediately. Are you sure?
        </p>
      </Modal>
    </DashboardLayout>
  );
}