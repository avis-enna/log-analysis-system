import React, { useEffect, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  ExclamationTriangleIcon,
  CheckCircleIcon,
  ClockIcon,
  FunnelIcon,
  EyeIcon,
} from '@heroicons/react/24/outline';
import {
  fetchAlerts,
  fetchOpenAlerts,
  acknowledgeAlert,
  resolveAlert,
  setFilters,
  selectAlerts,
  selectOpenAlerts,
  selectIsLoading,
  selectFilters,
  selectStats,
} from '../store/slices/alertsSlice';
import LoadingSpinner, { TableSkeleton } from '../components/LoadingSpinner/LoadingSpinner';

/**
 * Alerts page for managing system alerts and notifications
 */
const Alerts = () => {
  const dispatch = useDispatch();
  const alerts = useSelector(selectAlerts);
  const openAlerts = useSelector(selectOpenAlerts);
  const isLoading = useSelector(selectIsLoading);
  const filters = useSelector(selectFilters);
  const stats = useSelector(selectStats);

  const [selectedAlerts, setSelectedAlerts] = useState([]);
  const [showFilters, setShowFilters] = useState(false);
  const [selectedAlert, setSelectedAlert] = useState(null);

  // Fetch alerts on component mount
  useEffect(() => {
    dispatch(fetchAlerts());
    dispatch(fetchOpenAlerts());
  }, [dispatch]);

  // Use real alerts data from Redux store instead of mock data
  const alertsData = alerts.length > 0 ? alerts : [];

  // Get severity color
  const getSeverityColor = (severity) => {
    switch (severity) {
      case 'CRITICAL':
        return 'text-red-700 bg-red-100 dark:bg-red-900/20 dark:text-red-400';
      case 'HIGH':
        return 'text-orange-700 bg-orange-100 dark:bg-orange-900/20 dark:text-orange-400';
      case 'MEDIUM':
        return 'text-yellow-700 bg-yellow-100 dark:bg-yellow-900/20 dark:text-yellow-400';
      case 'LOW':
        return 'text-blue-700 bg-blue-100 dark:bg-blue-900/20 dark:text-blue-400';
      default:
        return 'text-gray-700 bg-gray-100 dark:bg-gray-900/20 dark:text-gray-400';
    }
  };

  // Get status color
  const getStatusColor = (status) => {
    switch (status) {
      case 'OPEN':
        return 'text-red-700 bg-red-100 dark:bg-red-900/20 dark:text-red-400';
      case 'ACKNOWLEDGED':
        return 'text-yellow-700 bg-yellow-100 dark:bg-yellow-900/20 dark:text-yellow-400';
      case 'RESOLVED':
        return 'text-green-700 bg-green-100 dark:bg-green-900/20 dark:text-green-400';
      default:
        return 'text-gray-700 bg-gray-100 dark:bg-gray-900/20 dark:text-gray-400';
    }
  };

  // Format timestamp
  const formatTimestamp = (timestamp) => {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now - date;
    
    if (diff < 60000) return 'Just now';
    if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`;
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}h ago`;
    return date.toLocaleDateString();
  };

  // Handle alert selection
  const handleAlertSelection = (alertId) => {
    setSelectedAlerts(prev => 
      prev.includes(alertId) 
        ? prev.filter(id => id !== alertId)
        : [...prev, alertId]
    );
  };

  // Handle acknowledge alert
  const handleAcknowledge = (alertId) => {
    dispatch(acknowledgeAlert({ alertId, acknowledgedBy: 'current-user' }));
  };

  // Handle resolve alert
  const handleResolve = (alertId) => {
    dispatch(resolveAlert({ alertId, resolvedBy: 'current-user', notes: 'Resolved via UI' }));
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Alerts
          </h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Monitor and manage system alerts and notifications
          </p>
        </div>
        
        <div className="mt-4 sm:mt-0 flex items-center space-x-3">
          <button
            onClick={() => setShowFilters(!showFilters)}
            className="btn-outline"
          >
            <FunnelIcon className="h-4 w-4 mr-2" />
            Filters
          </button>
        </div>
      </div>

      {/* Alert Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="card">
          <div className="card-body">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <ExclamationTriangleIcon className="h-8 w-8 text-red-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Open Alerts
                </p>
                <p className="text-2xl font-semibold text-gray-900 dark:text-white">
                  {alertsData.filter(a => a.status === 'OPEN').length}
                </p>
              </div>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card-body">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <ClockIcon className="h-8 w-8 text-yellow-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Acknowledged
                </p>
                <p className="text-2xl font-semibold text-gray-900 dark:text-white">
                  {alertsData.filter(a => a.status === 'ACKNOWLEDGED').length}
                </p>
              </div>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card-body">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <CheckCircleIcon className="h-8 w-8 text-green-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Resolved
                </p>
                <p className="text-2xl font-semibold text-gray-900 dark:text-white">
                  {alertsData.filter(a => a.status === 'RESOLVED').length}
                </p>
              </div>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card-body">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <ExclamationTriangleIcon className="h-8 w-8 text-orange-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Critical
                </p>
                <p className="text-2xl font-semibold text-gray-900 dark:text-white">
                  {alertsData.filter(a => a.severity === 'CRITICAL').length}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Filters */}
      {showFilters && (
        <div className="card">
          <div className="card-body">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div>
                <label className="form-label">Severity</label>
                <select className="form-input">
                  <option value="">All Severities</option>
                  <option value="CRITICAL">Critical</option>
                  <option value="HIGH">High</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="LOW">Low</option>
                </select>
              </div>
              
              <div>
                <label className="form-label">Status</label>
                <select className="form-input">
                  <option value="">All Statuses</option>
                  <option value="OPEN">Open</option>
                  <option value="ACKNOWLEDGED">Acknowledged</option>
                  <option value="RESOLVED">Resolved</option>
                </select>
              </div>
              
              <div>
                <label className="form-label">Source</label>
                <input
                  type="text"
                  placeholder="Filter by source"
                  className="form-input"
                />
              </div>
              
              <div>
                <label className="form-label">Time Range</label>
                <select className="form-input">
                  <option value="">All Time</option>
                  <option value="1h">Last Hour</option>
                  <option value="24h">Last 24 Hours</option>
                  <option value="7d">Last 7 Days</option>
                </select>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Alerts Table */}
      <div className="card">
        <div className="card-header">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-medium text-gray-900 dark:text-white">
              All Alerts
            </h3>
            {selectedAlerts.length > 0 && (
              <div className="flex items-center space-x-2">
                <span className="text-sm text-gray-500 dark:text-gray-400">
                  {selectedAlerts.length} selected
                </span>
                <button className="btn-outline btn-sm">
                  Acknowledge Selected
                </button>
                <button className="btn-outline btn-sm">
                  Resolve Selected
                </button>
              </div>
            )}
          </div>
        </div>
        
        <div className="card-body p-0">
          {isLoading ? (
            <TableSkeleton rows={8} columns={6} />
          ) : (
            <div className="overflow-x-auto">
              <table className="table">
                <thead className="table-header">
                  <tr>
                    <th className="table-header-cell">
                      <input
                        type="checkbox"
                        className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                        onChange={(e) => {
                          if (e.target.checked) {
                            setSelectedAlerts(alertsData.map(alert => alert.id));
                          } else {
                            setSelectedAlerts([]);
                          }
                        }}
                      />
                    </th>
                    <th className="table-header-cell">Alert</th>
                    <th className="table-header-cell">Severity</th>
                    <th className="table-header-cell">Status</th>
                    <th className="table-header-cell">Source</th>
                    <th className="table-header-cell">Time</th>
                    <th className="table-header-cell">Actions</th>
                  </tr>
                </thead>
                <tbody className="table-body">
                                    {alertsData.map((alert) => (
                    <tr key={alert.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                      <td className="table-cell">
                        <input
                          type="checkbox"
                          checked={selectedAlerts.includes(alert.id)}
                          onChange={() => handleAlertSelection(alert.id)}
                          className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                        />
                      </td>
                      <td className="table-cell">
                        <div>
                          <div className="font-medium text-gray-900 dark:text-white">
                            {alert.title}
                          </div>
                          <div className="text-sm text-gray-500 dark:text-gray-400">
                            {alert.message}
                          </div>
                          {alert.count > 1 && (
                            <div className="text-xs text-gray-400 mt-1">
                              {alert.count} occurrences
                            </div>
                          )}
                        </div>
                      </td>
                      <td className="table-cell">
                        <span className={`badge ${getSeverityColor(alert.severity)}`}>
                          {alert.severity}
                        </span>
                      </td>
                      <td className="table-cell">
                        <span className={`badge ${getStatusColor(alert.status)}`}>
                          {alert.status}
                        </span>
                      </td>
                      <td className="table-cell">
                        <span className="text-sm font-medium text-gray-900 dark:text-white">
                          {alert.source}
                        </span>
                      </td>
                      <td className="table-cell">
                        <span className="text-sm text-gray-500 dark:text-gray-400">
                          {formatTimestamp(alert.timestamp)}
                        </span>
                      </td>
                      <td className="table-cell">
                        <div className="flex items-center space-x-2">
                          <button
                            onClick={() => setSelectedAlert(alert)}
                            className="text-primary-600 hover:text-primary-700 dark:text-primary-400 dark:hover:text-primary-300"
                            title="View Details"
                          >
                            <EyeIcon className="h-4 w-4" />
                          </button>
                          {alert.status === 'OPEN' && (
                            <button
                              onClick={() => handleAcknowledge(alert.id)}
                              className="text-yellow-600 hover:text-yellow-700 dark:text-yellow-400 dark:hover:text-yellow-300"
                              title="Acknowledge"
                            >
                              <ClockIcon className="h-4 w-4" />
                            </button>
                          )}
                          {(alert.status === 'OPEN' || alert.status === 'ACKNOWLEDGED') && (
                            <button
                              onClick={() => handleResolve(alert.id)}
                              className="text-green-600 hover:text-green-700 dark:text-green-400 dark:hover:text-green-300"
                              title="Resolve"
                            >
                              <CheckCircleIcon className="h-4 w-4" />
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
        </div>
      </div>

      {/* Alert Details Modal */}
      {selectedAlert && (
        <div className="fixed inset-0 z-50 overflow-y-auto">
          <div className="flex items-center justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
            <div className="fixed inset-0 transition-opacity" aria-hidden="true">
              <div className="absolute inset-0 bg-gray-500 opacity-75"></div>
            </div>
            
            <div className="inline-block align-bottom bg-white dark:bg-gray-800 rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full">
              <div className="bg-white dark:bg-gray-800 px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                <div className="sm:flex sm:items-start">
                  <div className="mt-3 text-center sm:mt-0 sm:ml-4 sm:text-left w-full">
                    <h3 className="text-lg leading-6 font-medium text-gray-900 dark:text-white">
                      Alert Details
                    </h3>
                    <div className="mt-4 space-y-4">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                          Title
                        </label>
                        <p className="mt-1 text-sm text-gray-900 dark:text-white">
                          {selectedAlert.title}
                        </p>
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                          Message
                        </label>
                        <p className="mt-1 text-sm text-gray-900 dark:text-white">
                          {selectedAlert.message}
                        </p>
                      </div>
                      <div className="grid grid-cols-2 gap-4">
                        <div>
                          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                            Severity
                          </label>
                          <span className={`mt-1 inline-flex badge ${getSeverityColor(selectedAlert.severity)}`}>
                            {selectedAlert.severity}
                          </span>
                        </div>
                        <div>
                          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                            Status
                          </label>
                          <span className={`mt-1 inline-flex badge ${getStatusColor(selectedAlert.status)}`}>
                            {selectedAlert.status}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div className="bg-gray-50 dark:bg-gray-700 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
                <button
                  onClick={() => setSelectedAlert(null)}
                  className="btn-outline"
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Alerts;
