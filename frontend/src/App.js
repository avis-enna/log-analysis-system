import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Provider } from 'react-redux';
import { Toaster } from 'react-hot-toast';
import { store } from './store/store';
import { useWebSocket } from './hooks/useWebSocket';
import Layout from './components/Layout/Layout';
import Dashboard from './pages/Dashboard';
import Search from './pages/Search';
import Analytics from './pages/Analytics';
import Alerts from './pages/Alerts';
import Settings from './pages/Settings';
import Upload from './pages/Upload';
import Documentation from './pages/Documentation';
import ErrorBoundary from './components/ErrorBoundary/ErrorBoundary';
import LoadingSpinner from './components/LoadingSpinner/LoadingSpinner';
import NotFound from './components/NotFound/NotFound';
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
 */
function AppContent() {
  const { isConnected, connectionError } = useWebSocket();

  useEffect(() => {
    // Initialize application
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
  }, []);

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
                {/* Default route redirects to dashboard */}
                <Route path="/" element={<Navigate to="/dashboard" replace />} />
                
                {/* Main application routes */}
                <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/search" element={<Search />} />
                <Route path="/analytics" element={<Analytics />} />
                <Route path="/alerts" element={<Alerts />} />
                <Route path="/settings" element={<Settings />} />
                <Route path="/upload" element={<Upload />} />
                <Route path="/docs" element={<Documentation />} />
                
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
