import React from 'react';
import { Link } from 'react-router-dom';
import { HomeIcon, MagnifyingGlassIcon } from '@heroicons/react/24/outline';

/**
 * 404 Not Found page component
 */
const NotFound = () => {
  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center p-4">
      <div className="max-w-md w-full text-center">
        {/* 404 Illustration */}
        <div className="mb-8">
          <div className="text-9xl font-bold text-gray-300 dark:text-gray-700">
            404
          </div>
          <div className="text-2xl font-semibold text-gray-600 dark:text-gray-400 mt-2">
            Page Not Found
          </div>
        </div>

        {/* Error Message */}
        <div className="mb-8">
          <p className="text-gray-500 dark:text-gray-400 text-lg mb-4">
            The page you're looking for doesn't exist or has been moved.
          </p>
          <p className="text-gray-400 dark:text-gray-500 text-sm">
            Don't worry, it happens to the best of us. Let's get you back on track.
          </p>
        </div>

        {/* Action Buttons */}
        <div className="space-y-4">
          <Link
            to="/dashboard"
            className="inline-flex items-center justify-center w-full px-6 py-3 border border-transparent text-base font-medium rounded-lg text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 transition-colors"
          >
            <HomeIcon className="w-5 h-5 mr-2" />
            Go to Dashboard
          </Link>
          
          <Link
            to="/search"
            className="inline-flex items-center justify-center w-full px-6 py-3 border border-gray-300 dark:border-gray-600 text-base font-medium rounded-lg text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 transition-colors"
          >
            <MagnifyingGlassIcon className="w-5 h-5 mr-2" />
            Search Logs
          </Link>
        </div>

        {/* Help Links */}
        <div className="mt-8 pt-6 border-t border-gray-200 dark:border-gray-700">
          <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
            Need help? Try these resources:
          </p>
          <div className="flex justify-center space-x-6 text-sm">
            <Link
              to="/docs"
              className="text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300 transition-colors"
            >
              Documentation
            </Link>
            <a
              href="mailto:support@loganalysis.com"
              className="text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300 transition-colors"
            >
              Contact Support
            </a>
          </div>
        </div>
      </div>
    </div>
  );
};

export default NotFound;
