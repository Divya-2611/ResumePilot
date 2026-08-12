import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import { ThemeProvider } from './context/ThemeContext';

import ProtectedRoute from './components/ProtectedRoute';
import Landing from './pages/Landing';
import Login from './pages/Login';
import Signup from './pages/Signup';
import VerifyOtp from './pages/VerifyOtp';
import ForgotPassword from './pages/ForgotPassword';
import Dashboard from './pages/Dashboard';
import MyResumes from './pages/MyResumes';
import UploadResume from './pages/UploadResume';
import ResumeEditor from './pages/ResumeEditor';
import PasteJobDescription from './pages/PasteJobDescription';
import OptimizationResult from './pages/OptimizationResult';
import History from './pages/History';
import Profile from './pages/Profile';
import Settings from './pages/Settings';
import Admin from './pages/Admin';

/**
 * Application shell: providers -> theme -> toasts -> auth -> routes.
 * All authenticated pages are wrapped in ProtectedRoute.
 */
export default function App() {
  return (
    <ThemeProvider>
      <ToastProvider>
        <AuthProvider>
          <BrowserRouter>
            <Routes>
              {/* Public */}
              <Route path="/" element={<Landing />} />
              <Route path="/login" element={<Login />} />
              <Route path="/signup" element={<Signup />} />
              <Route path="/verify-otp" element={<VerifyOtp />} />
              <Route path="/forgot-password" element={<ForgotPassword />} />

              {/* Authenticated */}
              <Route
                path="/dashboard"
                element={
                  <ProtectedRoute>
                    <Dashboard />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/resumes"
                element={
                  <ProtectedRoute>
                    <MyResumes />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/resumes/upload"
                element={
                  <ProtectedRoute>
                    <UploadResume />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/resumes/:id/edit"
                element={
                  <ProtectedRoute>
                    <ResumeEditor />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/optimize"
                element={
                  <ProtectedRoute>
                    <PasteJobDescription />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/optimize/result"
                element={
                  <ProtectedRoute>
                    <OptimizationResult />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/history"
                element={
                  <ProtectedRoute>
                    <History />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/profile"
                element={
                  <ProtectedRoute>
                    <Profile />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/settings"
                element={
                  <ProtectedRoute>
                    <Settings />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin"
                element={
                  <ProtectedRoute adminOnly>
                    <Admin />
                  </ProtectedRoute>
                }
              />

              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </ToastProvider>
    </ThemeProvider>
  );
}