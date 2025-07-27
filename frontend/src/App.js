import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Provider, useDispatch, useSelector } from 'react-redux';
import { Toaster } from 'react-hot-toast';
import { store } from './store/store';
import { useWebSocket } from './hooks/useWebSocket';
import { initializeAuth, selectIsAuthenticated } from './store/slices/authSlice';
import Layout from './components/Layout/Layout';
import Login from './components/Login/Login';
import ProtectedRoute from './components/ProtectedRoute/ProtectedRoute';
import Dashboard from './pages/Dashboard';
import Search from './pages/Search';
import Analytics from './pages/Analytics';
import Alerts from './pages/Alerts';
import Settings from './pages/Settings';
import Upload from './pages/Upload';
import Documentation from './pages/Documentation';
import RoleManagement from './pages/RoleManagement';
import DemoAccounts from './pages/DemoAccounts';
import ErrorBoundary from './components/ErrorBoundary/ErrorBoundary';
import LoadingSpinner from './components/LoadingSpinner/LoadingSpinner';
import NotFound from './components/NotFound/NotFound';
import ConfirmDialog from './components/ConfirmDialog/ConfirmDialog';
import './App.css';

/**
 * Main Application Component
 * 
 * Features:
 * - React Router for navigation
 * - Redux for state management
 * - WebSocket for real-time updates
 * - Error boundaries for graceful error handling
 * - Hot toast notifications
 * - Responsive layout
 * - Authentication system
 */
function AppContent() {
  const { isConnected, connectionError } = useWebSocket();
  const dispatch = useDispatch();
  const isAuthenticated = useSelector(selectIsAuthenticated);

  useEffect(() => {
    // Initialize auth state from localStorage
    dispatch(initializeAuth());
    
    // Initialize application
    // eslint-disable-next-line no-console
    console.log('Log Analysis System initialized');
    
    // Set up performance monitoring
    if ('performance' in window && 'measure' in window.performance) {
      window.performance.mark('app-start');
    }
    
    return () => {
      if ('performance' in window && 'measure' in window.performance) {
        window.performance.mark('app-end');
        window.performance.measure('app-duration', 'app-start', 'app-end');
      }
    };
  }, [dispatch]);

  return (
    <ErrorBoundary>
      <Router>
        <div className="App min-h-screen bg-gray-50">
          {/* Connection Status Indicator */}
          <div className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
            connectionError ? 'bg-red-500' : isConnected ? 'bg-green-500' : 'bg-yellow-500'
          } text-white text-center py-1 text-sm ${
            isConnected && !connectionError ? 'transform -translate-y-full' : ''
          }`}>
            {connectionError ? (
              `Connection Error: ${connectionError}`
            ) : isConnected ? (
              'Connected to real-time updates'
            ) : (
              'Connecting to real-time updates...'
            )}
          </div>

          <Layout>
            <React.Suspense fallback={<LoadingSpinner />}>
              <Routes>
                {/* Login route */}
                <Route path="/login" element={
                  isAuthenticated ? <Navigate to="/dashboard" replace /> : <Login />
                } />
                
                {/* Default route redirects to dashboard if authenticated, login if not */}
                <Route path="/" element={
                  <Navigate to={isAuthenticated ? "/dashboard" : "/login"} replace />
                } />
                
                {/* Protected routes */}
                <Route path="/dashboard" element={
                  <ProtectedRoute>
                    <Dashboard />
                  </ProtectedRoute>
                } />
                <Route path="/search" element={
                  <ProtectedRoute>
                    <Search />
                  </ProtectedRoute>
                } />
                <Route path="/analytics" element={
                  <ProtectedRoute>
                    <Analytics />
                  </ProtectedRoute>
                } />
                <Route path="/alerts" element={
                  <ProtectedRoute>
                    <Alerts />
                  </ProtectedRoute>
                } />
                <Route path="/settings" element={
                  <ProtectedRoute>
                    <Settings />
                  </ProtectedRoute>
                } />
                <Route path="/upload" element={
                  <ProtectedRoute>
                    <Upload />
                  </ProtectedRoute>
                } />
                <Route path="/docs" element={
                  <ProtectedRoute>
                    <Documentation />
                  </ProtectedRoute>
                } />
                <Route path="/roles" element={
                  <ProtectedRoute>
                    <RoleManagement />
                  </ProtectedRoute>
                } />
                <Route path="/demo" element={
                  <ProtectedRoute>
                    <DemoAccounts />
                  </ProtectedRoute>
                } />
                
                {/* Catch-all route for 404 */}
                <Route path="*" element={<NotFound />} />
              </Routes>
            </React.Suspense>
          </Layout>

          {/* Global Toast Notifications */}
          <Toaster
            position="top-right"
            toastOptions={{
              duration: 4000,
              style: {
                background: '#363636',
                color: '#fff',
              },
              success: {
                duration: 3000,
                iconTheme: {
                  primary: '#4ade80',
                  secondary: '#fff',
                },
              },
              error: {
                duration: 5000,
                iconTheme: {
                  primary: '#ef4444',
                  secondary: '#fff',
                },
              },
            }}
          />

          {/* Global Modals */}
          <ConfirmDialog />
        </div>
      </Router>
    </ErrorBoundary>
  );
}



/**
 * Root App Component with Redux Provider
 */
function App() {
  return (
    <Provider store={store}>
      <AppContent />
    </Provider>
  );
}

export default App;
