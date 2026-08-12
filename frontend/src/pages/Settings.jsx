import { useState } from 'react';
import { Moon, Bell, Database, Palette } from 'lucide-react';
import DashboardLayout from '../components/layout/DashboardLayout';
import { useTheme } from '../hooks/useTheme';
import { useToast } from '../hooks/useToast';

/**
 * Settings: appearance, notification prefs and account info (read-only).
 */
export default function Settings() {
  const { theme, toggleTheme } = useTheme();
  const toast = useToast();

  const [notifications, setNotifications] = useState(() => {
    return JSON.parse(localStorage.getItem('settings') || '{}').notifications ?? true;
  });

  const saveNotifications = (value) => {
    setNotifications(value);
    const settings = JSON.parse(localStorage.getItem('settings') || '{}');
    settings.notifications = value;
    localStorage.setItem('settings', JSON.stringify(settings));
    toast.success(`Email notifications ${value ? 'enabled' : 'disabled'}`);
  };

  const rows = [
    {
      icon: Database,
      title: 'Data retention',
      description: 'Uploaded files are stored securely per user. Deleting your account removes everything.',
      action: null,
    },
  ];

  return (
    <DashboardLayout>
      <div className="mx-auto max-w-2xl space-y-6">
        <h1 className="text-2xl font-bold">Settings</h1>

        {/* Appearance */}
        <div className="card p-6">
          <h2 className="mb-4 flex items-center gap-2 font-semibold">
            <Palette className="h-4 w-4 text-brand-600 dark:text-brand-400" />
            Appearance
          </h2>
          <div className="flex items-center justify-between">
            <div>
              <p className="font-medium">Dark mode</p>
              <p className="text-sm text-slate-500">Reduce eye strain in low light</p>
            </div>
            <button
              onClick={toggleTheme}
              className="relative h-8 w-14 rounded-full bg-slate-200 transition-colors dark:bg-brand-600"
              role="switch"
              aria-checked={theme === 'dark'}
              aria-label="Toggle dark mode"
            >
              <span
                className={`absolute top-1 flex h-6 w-6 items-center justify-center rounded-full bg-white shadow transition-all ${
                  theme === 'dark' ? 'left-7' : 'left-1'
                }`}
              >
                <Moon className="h-3.5 w-3.5 text-slate-600" />
              </span>
            </button>
          </div>
        </div>

        {/* Notifications */}
        <div className="card p-6">
          <h2 className="mb-4 flex items-center gap-2 font-semibold">
            <Bell className="h-4 w-4 text-violet-500" />
            Notifications
          </h2>
          <div className="flex items-center justify-between">
            <div>
              <p className="font-medium">Email notifications</p>
              <p className="text-sm text-slate-500">
                OTP codes, password changes and security alerts
              </p>
            </div>
            <input
              type="checkbox"
              checked={notifications}
              onChange={(e) => saveNotifications(e.target.checked)}
              className="h-5 w-5 accent-brand-600"
              aria-label="Email notifications"
            />
          </div>
        </div>

        {/* Info */}
        {rows.map(({ icon: Icon, title, description }) => (
          <div key={title} className="card p-6">
            <h2 className="mb-2 flex items-center gap-2 font-semibold">
              <Icon className="h-4 w-4 text-emerald-500" />
              {title}
            </h2>
            <p className="text-sm text-slate-500">{description}</p>
          </div>
        ))}

        <p className="text-center text-xs text-slate-400">
          ResumePilot v1.0.0
        </p>
      </div>
    </DashboardLayout>
  );
}
