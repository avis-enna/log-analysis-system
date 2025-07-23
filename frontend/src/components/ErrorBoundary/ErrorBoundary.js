import React from 'react';
import { ExclamationTriangleIcon, ArrowPathIcon } from '@heroicons/react/24/outline';

/**
 * Error Boundary component to catch and handle React errors
 */
class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
      errorId: null,
    };
  }

  static getDerivedStateFromError(error) {
    // Update state so the next render will show the fallback UI
    return {
      hasError: true,
      errorId: Date.now().toString(36) + Math.random().toString(36).substr(2),
    };
  }

  componentDidCatch(error, errorInfo) {
    // Log error details
    console.error('ErrorBoundary caught an error:', error, errorInfo);
    
    this.setState({
      error,
      errorInfo,
    });

    // Report error to monitoring service (if available)
    if (window.reportError) {
      window.reportError(error, errorInfo);
    }
  }

  handleReload = () => {
    window.location.reload();
  };

  handleReset = () => {
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null,
      errorId: null,
    });
  };

  render() {
    if (this.state.hasError) {
      const { error, errorInfo, errorId } = this.state;
      const isDevelopment = process.env.NODE_ENV === 'development';

      return (
        <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center p-4">
          <div className="max-w-2xl w-full">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 p-8">
              {/* Error Icon */}
              <div className="flex justify-center mb-6">
                <div className="flex items-center justify-center w-16 h-16 bg-red-100 dark:bg-red-900/20 rounded-full">
                  <ExclamationTriangleIcon className="w-8 h-8 text-red-600 dark:text-red-400" />
                </div>
              </div>

              {/* Error Title */}
              <div className="text-center mb-6">
                <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
                  Something went wrong
                </h1>
                <p className="text-gray-600 dark:text-gray-400">
                  We're sorry, but an unexpected error occurred. Please try refreshing the page or contact support if the problem persists.
                </p>
              </div>

              {/* Error ID */}
              <div className="bg-gray-50 dark:bg-gray-700 rounded-lg p-4 mb-6">
                <div className="text-sm">
                  <span className="font-medium text-gray-700 dark:text-gray-300">Error ID: </span>
                  <span className="font-mono text-gray-600 dark:text-gray-400">{errorId}</span>
                </div>
              </div>

              {/* Development Error Details */}
              {isDevelopment && error && (
                <div className="mb-6">
                  <details className="bg-red-50 dark:bg-red-900/10 border border-red-200 dark:border-red-800 rounded-lg p-4">
                    <summary className="cursor-pointer font-medium text-red-800 dark:text-red-400 mb-2">
                      Error Details (Development Mode)
                    </summary>
                    <div className="space-y-4">
                      <div>
                        <h4 className="font-medium text-red-700 dark:text-red-300 mb-1">Error Message:</h4>
                        <pre className="text-sm text-red-600 dark:text-red-400 bg-red-100 dark:bg-red-900/20 p-2 rounded overflow-x-auto">
                          {error.toString()}
                        </pre>
                      </div>
                      {errorInfo && errorInfo.componentStack && (
                        <div>
                          <h4 className="font-medium text-red-700 dark:text-red-300 mb-1">Component Stack:</h4>
                          <pre className="text-sm text-red-600 dark:text-red-400 bg-red-100 dark:bg-red-900/20 p-2 rounded overflow-x-auto">
                            {errorInfo.componentStack}
                          </pre>
                        </div>
                      )}
                    </div>
                  </details>
                </div>
              )}

              {/* Action Buttons */}
              <div className="flex flex-col sm:flex-row gap-3 justify-center">
                <button
                  onClick={this.handleReset}
                  className="inline-flex items-center justify-center px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700 hover:bg-gray-50 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 transition-colors"
                >
                  <ArrowPathIcon className="w-4 h-4 mr-2" />
                  Try Again
                </button>
                <button
                  onClick={this.handleReload}
                  className="inline-flex items-center justify-center px-4 py-2 border border-transparent rounded-lg text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 transition-colors"
                >
                  Reload Page
                </button>
              </div>

              {/* Support Information */}
              <div className="mt-8 pt-6 border-t border-gray-200 dark:border-gray-700 text-center">
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  If this error persists, please contact support with the error ID above.
                </p>
                <div className="mt-2 space-x-4 text-sm">
                  <a 
                    href="/docs" 
                    className="text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300"
                  >
                    Documentation
                  </a>
                  <a 
                    href="mailto:support@loganalysis.com" 
                    className="text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300"
                  >
                    Contact Support
                  </a>
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
