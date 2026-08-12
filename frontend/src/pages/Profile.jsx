import { useCallback, useEffect, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Camera, Trash2, KeyRound } from 'lucide-react';
import DashboardLayout from '../components/layout/DashboardLayout';
import Modal from '../components/common/Modal';
import Spinner from '../components/common/Spinner';
import { useAuth } from '../hooks/useAuth';
import { useToast } from '../hooks/useToast';
import { getErrorMessage } from '../utils/helpers';
import { isValidEmail, isImageFile } from '../utils/validators';
import userService from '../services/userService';
import { tokenStorage } from '../api/axiosClient';

/**
 * Profile: personal info, avatar, change password, delete account.
 */
export default function Profile() {
  const { user, updateUser, logout } = useAuth();
  const toast = useToast();
  const fileRef = useRef(null);

  const [saving, setSaving] = useState(false);
  const [uploadingPic, setUploadingPic] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [pictureUrl, setPictureUrl] = useState(null);

  // The picture endpoint requires auth, so fetch it as a blob through axios
  // (a plain <img src> cannot send the Bearer header).
  useEffect(() => {
    if (!user?.profilePictureUrl) {
      setPictureUrl(null);
      return;
    }
    let revoked = false;
    userService
      .getProfilePicture()
      .then((url) => {
        if (!revoked) setPictureUrl(url);
      })
      .catch(() => setPictureUrl(null));
    return () => {
      revoked = true;
      setPictureUrl((prev) => {
        if (prev) URL.revokeObjectURL(prev);
        return null;
      });
    };
  }, [user?.profilePictureUrl]);

  const refreshPicture = useCallback(async () => {
    try {
      const url = await userService.getProfilePicture();
      setPictureUrl((prev) => {
        if (prev) URL.revokeObjectURL(prev);
        return url;
      });
    } catch {
      setPictureUrl(null);
    }
  }, []);

  const { register, handleSubmit, formState: { errors } } = useForm({
    defaultValues: {
      firstName: user?.firstName || '',
      lastName: user?.lastName || '',
      email: user?.email || '',
    },
  });

  const passwordForm = useForm();

  const saveProfile = async (data) => {
    setSaving(true);
    try {
      const updated = await userService.updateProfile(data);
      updateUser(updated);
      toast.success('Profile updated');
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const uploadPicture = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!isImageFile(file)) {
      toast.error('Only image files (jpg, png, webp, svg) are allowed');
      return;
    }
    setUploadingPic(true);
    try {
      const updated = await userService.uploadProfilePicture(file);
      updateUser(updated);
      await refreshPicture();
      toast.success('Profile picture updated');
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setUploadingPic(false);
      e.target.value = '';
    }
  };

  const changePassword = async (data) => {
    setSaving(true);
    try {
      await userService.changePassword(data.currentPassword, data.newPassword);
      toast.success('Password changed. Please log in again.');
      await logout();
      window.location.href = '/login';
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const deleteAccount = async () => {
    setDeleting(true);
    try {
      await userService.deleteAccount();
      tokenStorage.clear();
      localStorage.removeItem('user');
      toast.success('Account deleted. Goodbye!');
      window.location.href = '/';
    } catch (err) {
      toast.error(getErrorMessage(err));
      setDeleting(false);
    }
  };

  return (
    <DashboardLayout>
      <div className="mx-auto max-w-2xl space-y-6">
        <h1 className="text-2xl font-bold">Profile</h1>

        {/* Avatar + info */}
        <div className="card p-6">
          <div className="flex items-center gap-4">
            <div className="relative">
              <div className="flex h-20 w-20 items-center justify-center overflow-hidden rounded-full bg-brand-100 text-3xl font-bold text-brand-700 dark:bg-brand-900/40 dark:text-brand-300">
                {pictureUrl ? (
                  <img
                    src={pictureUrl}
                    alt="Profile"
                    className="h-full w-full object-cover"
                  />
                ) : (
                  (user?.firstName?.[0] || '?').toUpperCase()
                )}
              </div>
              <button
                onClick={() => fileRef.current?.click()}
                className="absolute -bottom-1 -right-1 rounded-full bg-brand-600 p-2 text-white shadow hover:bg-brand-700"
                aria-label="Change profile picture"
              >
                {uploadingPic ? <Spinner size={14} className="border-white/40 border-t-white" /> : <Camera className="h-3.5 w-3.5" />}
              </button>
              <input ref={fileRef} type="file" accept="image/*" className="hidden" onChange={uploadPicture} />
            </div>
            <div>
              <p className="text-lg font-semibold">
                {user?.firstName} {user?.lastName}
              </p>
              <p className="text-sm text-slate-500">{user?.email}</p>
              <span className="mt-1 inline-block rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-medium text-emerald-600 dark:bg-emerald-900/40 dark:text-emerald-300">
                {user?.emailVerified === 'VERIFIED' ? 'Email verified' : 'Email pending'}
              </span>
            </div>
          </div>

          <form onSubmit={handleSubmit(saveProfile)} className="mt-6 grid gap-4 sm:grid-cols-2" noValidate>
            <div>
              <label className="label" htmlFor="firstName">First name</label>
              <input
                id="firstName"
                className="input"
                {...register('firstName', { required: 'First name is required' })}
              />
              {errors.firstName && <p className="mt-1 text-xs text-red-500">{errors.firstName.message}</p>}
            </div>
            <div>
              <label className="label" htmlFor="lastName">Last name</label>
              <input
                id="lastName"
                className="input"
                {...register('lastName', { required: 'Last name is required' })}
              />
              {errors.lastName && <p className="mt-1 text-xs text-red-500">{errors.lastName.message}</p>}
            </div>
            <div className="sm:col-span-2">
              <label className="label" htmlFor="email">Email</label>
              <input
                id="email"
                type="email"
                className="input"
                {...register('email', {
                  required: 'Email is required',
                  validate: (v) => isValidEmail(v) || 'Invalid email format',
                })}
              />
              {errors.email && <p className="mt-1 text-xs text-red-500">{errors.email.message}</p>}
            </div>
            <div className="sm:col-span-2">
              <button type="submit" className="btn-primary" disabled={saving}>
                {saving ? <Spinner size={16} className="border-white/40 border-t-white" /> : 'Save changes'}
              </button>
            </div>
          </form>
        </div>

        {/* Change password */}
        <div className="card p-6">
          <h2 className="mb-4 flex items-center gap-2 font-semibold">
            <KeyRound className="h-4 w-4 text-brand-600 dark:text-brand-400" />
            Change password
          </h2>
          <form onSubmit={passwordForm.handleSubmit(changePassword)} className="grid gap-4 sm:grid-cols-3" noValidate>
            <div>
              <label className="label" htmlFor="currentPassword">Current password</label>
              <input
                id="currentPassword"
                type="password"
                className="input"
                {...passwordForm.register('currentPassword', { required: 'Required' })}
              />
              {passwordForm.formState.errors.currentPassword && (
                <p className="mt-1 text-xs text-red-500">Required</p>
              )}
            </div>
            <div>
              <label className="label" htmlFor="newPassword">New password</label>
              <input
                id="newPassword"
                type="password"
                className="input"
                {...passwordForm.register('newPassword', {
                  required: 'Required',
                  minLength: { value: 8, message: 'Min 8 characters' },
                })}
              />
              {passwordForm.formState.errors.newPassword && (
                <p className="mt-1 text-xs text-red-500">
                  {passwordForm.formState.errors.newPassword.message}
                </p>
              )}
            </div>
            <div className="flex items-end">
              <button type="submit" className="btn-secondary w-full" disabled={saving}>
                {saving ? <Spinner size={16} /> : 'Update password'}
              </button>
            </div>
          </form>
        </div>

        {/* Danger zone */}
        <div className="card border-red-200 p-6 dark:border-red-900/50">
          <h2 className="font-semibold text-red-600 dark:text-red-400">Danger zone</h2>
          <p className="mt-1 text-sm text-slate-500">
            Deleting your account removes all resumes, versions, job descriptions and history permanently.
          </p>
          <button className="btn-danger mt-4" onClick={() => setShowDeleteModal(true)}>
            <Trash2 className="h-4 w-4" />
            Delete account
          </button>
        </div>
      </div>

      <Modal
        open={showDeleteModal}
        onClose={() => setShowDeleteModal(false)}
        title="Delete account?"
        footer={
          <>
            <button className="btn-secondary" onClick={() => setShowDeleteModal(false)}>Cancel</button>
            <button className="btn-danger" onClick={deleteAccount} disabled={deleting}>
              {deleting ? 'Deleting…' : 'Delete my account'}
            </button>
          </>
        }
      >
        <p className="text-sm text-slate-500 dark:text-slate-400">
          This action is <span className="font-semibold text-red-500">permanent</span>. All your data will
          be deleted immediately. Are you sure?
        </p>
      </Modal>
    </DashboardLayout>
  );
}
