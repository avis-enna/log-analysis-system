import React from 'react';
import { useSelector } from 'react-redux';
import { selectSystemStatus } from '../../store/slices/dashboardSlice';

/**
 * Footer component with system status and information
 */
const Footer = () => {
  const systemStatus = useSelector(selectSystemStatus);

  // Get status color
  const getStatusColor = (status) => {
    switch (status) {
      case 'healthy':
        return 'text-green-500';
      case 'warning':
        return 'text-yellow-500';
      case 'error':
        return 'text-red-500';
      default:
        return 'text-gray-500';
    }
  };

  // Get status indicator
  const getStatusIndicator = (status) => {
    switch (status) {
      case 'healthy':
        return '●';
      case 'warning':
        return '◐';
      case 'error':
        return '●';
      default:
        return '○';
    }
  };

  return (
    <footer className="bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 px-6 py-4">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between space-y-2 sm:space-y-0">
        {/* Left Section - Copyright */}
        <div className="text-sm text-gray-500 dark:text-gray-400">
          <p>© 2024 Log Analysis System. Enterprise Edition.</p>
        </div>

        {/* Center Section - System Status */}
        <div className="flex items-center space-x-4 text-xs">
          <span className="text-gray-500 dark:text-gray-400">System Status:</span>
          
          {/* API Status */}
          <div className="flex items-center space-x-1">
            <span className={`${getStatusColor(systemStatus.api)}`}>
              {getStatusIndicator(systemStatus.api)}
            </span>
            <span className="text-gray-600 dark:text-gray-300">API</span>
          </div>

          {/* Database Status */}
          <div className="flex items-center space-x-1">
            <span className={`${getStatusColor(systemStatus.database)}`}>
              {getStatusIndicator(systemStatus.database)}
            </span>
            <span className="text-gray-600 dark:text-gray-300">DB</span>
          </div>

          {/* Elasticsearch Status */}
          <div className="flex items-center space-x-1">
            <span className={`${getStatusColor(systemStatus.elasticsearch)}`}>
              {getStatusIndicator(systemStatus.elasticsearch)}
            </span>
            <span className="text-gray-600 dark:text-gray-300">ES</span>
          </div>

          {/* Kafka Status */}
          <div className="flex items-center space-x-1">
            <span className={`${getStatusColor(systemStatus.kafka)}`}>
              {getStatusIndicator(systemStatus.kafka)}
            </span>
            <span className="text-gray-600 dark:text-gray-300">Kafka</span>
          </div>

          {/* Redis Status */}
          <div className="flex items-center space-x-1">
            <span className={`${getStatusColor(systemStatus.redis)}`}>
              {getStatusIndicator(systemStatus.redis)}
            </span>
            <span className="text-gray-600 dark:text-gray-300">Redis</span>
          </div>
        </div>

        {/* Right Section - Version and Links */}
        <div className="flex items-center space-x-4 text-sm text-gray-500 dark:text-gray-400">
          <span>v1.0.0</span>
          <a 
            href="/docs" 
            className="hover:text-gray-700 dark:hover:text-gray-300 transition-colors"
          >
            Docs
          </a>
          <a 
            href="/api/health" 
            target="_blank" 
            rel="noopener noreferrer"
            className="hover:text-gray-700 dark:hover:text-gray-300 transition-colors"
          >
            Health
          </a>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
