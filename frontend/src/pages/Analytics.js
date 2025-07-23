import React, { useEffect, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  ChartBarIcon,
  ClockIcon,
  ExclamationTriangleIcon,
  ServerIcon,
} from '@heroicons/react/24/outline';
import {
  LineChart,
  Line,
  AreaChart,
  Area,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { ChartSkeleton } from '../components/LoadingSpinner/LoadingSpinner';

/**
 * Analytics page with advanced log analysis and reporting
 */
const Analytics = () => {
  const [timeRange, setTimeRange] = useState('24h');
  const [selectedMetric, setSelectedMetric] = useState('volume');
  const [isLoading, setIsLoading] = useState(false);

  // Mock data for demonstration
  const [analyticsData, setAnalyticsData] = useState({
    logVolume: [
      { time: '00:00', count: 1200, errors: 45 },
      { time: '04:00', count: 800, errors: 23 },
      { time: '08:00', count: 2100, errors: 67 },
      { time: '12:00', count: 3200, errors: 89 },
      { time: '16:00', count: 2800, errors: 76 },
      { time: '20:00', count: 1900, errors: 54 },
    ],
    errorTrends: [
      { time: '00:00', errors: 45, warnings: 120, critical: 5 },
      { time: '04:00', errors: 23, warnings: 89, critical: 2 },
      { time: '08:00', errors: 67, warnings: 156, critical: 8 },
      { time: '12:00', errors: 89, warnings: 203, critical: 12 },
      { time: '16:00', errors: 76, warnings: 178, critical: 9 },
      { time: '20:00', errors: 54, warnings: 134, critical: 6 },
    ],
    topSources: [
      { name: 'web-server', count: 15420, percentage: 35 },
      { name: 'api-gateway', count: 12340, percentage: 28 },
      { name: 'database', count: 8760, percentage: 20 },
      { name: 'auth-service', count: 4320, percentage: 10 },
      { name: 'cache', count: 3160, percentage: 7 },
    ],
    responseTime: [
      { time: '00:00', avg: 245, p95: 450, p99: 890 },
      { time: '04:00', avg: 189, p95: 320, p99: 650 },
      { time: '08:00', avg: 312, p95: 580, p99: 1200 },
      { time: '12:00', avg: 398, p95: 720, p99: 1450 },
      { time: '16:00', avg: 356, p95: 650, p99: 1300 },
      { time: '20:00', avg: 278, p95: 490, p99: 980 },
    ],
  });

  // Chart colors
  const colors = {
    primary: '#3b82f6',
    success: '#22c55e',
    warning: '#f59e0b',
    error: '#ef4444',
    secondary: '#64748b',
    purple: '#8b5cf6',
  };

  // Handle time range change
  const handleTimeRangeChange = (range) => {
    setTimeRange(range);
    setIsLoading(true);
    // Simulate API call
    setTimeout(() => {
      setIsLoading(false);
    }, 1000);
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Analytics
          </h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Advanced log analysis and performance insights
          </p>
        </div>
        
        <div className="mt-4 sm:mt-0 flex items-center space-x-3">
          {/* Time Range Selector */}
          <select
            value={timeRange}
            onChange={(e) => handleTimeRangeChange(e.target.value)}
            className="form-input text-sm"
          >
            <option value="1h">Last Hour</option>
            <option value="24h">Last 24 Hours</option>
            <option value="7d">Last 7 Days</option>
            <option value="30d">Last 30 Days</option>
          </select>
        </div>
      </div>

      {/* Key Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="card">
          <div className="card-body">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <ChartBarIcon className="h-8 w-8 text-primary-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Total Events
                </p>
                <p className="text-2xl font-semibold text-gray-900 dark:text-white">
                  44.2K
                </p>
                <p className="text-sm text-green-600">+12.5% from yesterday</p>
              </div>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card-body">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <ExclamationTriangleIcon className="h-8 w-8 text-error-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Error Rate
                </p>
                <p className="text-2xl font-semibold text-gray-900 dark:text-white">
                  2.3%
                </p>
                <p className="text-sm text-red-600">+0.4% from yesterday</p>
              </div>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card-body">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <ClockIcon className="h-8 w-8 text-warning-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Avg Response Time
                </p>
                <p className="text-2xl font-semibold text-gray-900 dark:text-white">
                  298ms
                </p>
                <p className="text-sm text-green-600">-15ms from yesterday</p>
              </div>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card-body">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <ServerIcon className="h-8 w-8 text-success-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Active Sources
                </p>
                <p className="text-2xl font-semibold text-gray-900 dark:text-white">
                  23
                </p>
                <p className="text-sm text-gray-500">2 new today</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Charts Row 1 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Log Volume Chart */}
        <div className="card">
          <div className="card-header">
            <h3 className="text-lg font-medium text-gray-900 dark:text-white">
              Log Volume & Errors
            </h3>
          </div>
          <div className="card-body">
            {isLoading ? (
              <ChartSkeleton height="h-64" />
            ) : (
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart data={analyticsData.logVolume}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="time" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Area 
                    type="monotone" 
                    dataKey="count" 
                    stackId="1"
                    stroke={colors.primary} 
                    fill={colors.primary}
                    fillOpacity={0.3}
                    name="Total Logs"
                  />
                  <Area 
                    type="monotone" 
                    dataKey="errors" 
                    stackId="2"
                    stroke={colors.error} 
                    fill={colors.error}
                    fillOpacity={0.6}
                    name="Errors"
                  />
                </AreaChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        {/* Error Trends Chart */}
        <div className="card">
          <div className="card-header">
            <h3 className="text-lg font-medium text-gray-900 dark:text-white">
              Error Trends by Severity
            </h3>
          </div>
          <div className="card-body">
            {isLoading ? (
              <ChartSkeleton height="h-64" />
            ) : (
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={analyticsData.errorTrends}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="time" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Line 
                    type="monotone" 
                    dataKey="critical" 
                    stroke={colors.error} 
                    strokeWidth={3}
                    name="Critical"
                  />
                  <Line 
                    type="monotone" 
                    dataKey="errors" 
                    stroke={colors.warning} 
                    strokeWidth={2}
                    name="Errors"
                  />
                  <Line 
                    type="monotone" 
                    dataKey="warnings" 
                    stroke={colors.secondary} 
                    strokeWidth={2}
                    name="Warnings"
                  />
                </LineChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>
      </div>

      {/* Charts Row 2 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Top Sources */}
        <div className="card">
          <div className="card-header">
            <h3 className="text-lg font-medium text-gray-900 dark:text-white">
              Top Log Sources
            </h3>
          </div>
          <div className="card-body">
            {isLoading ? (
              <ChartSkeleton height="h-64" />
            ) : (
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={analyticsData.topSources} layout="horizontal">
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis type="number" />
                  <YAxis dataKey="name" type="category" width={100} />
                  <Tooltip />
                  <Bar dataKey="count" fill={colors.primary} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        {/* Response Time Analysis */}
        <div className="card">
          <div className="card-header">
            <h3 className="text-lg font-medium text-gray-900 dark:text-white">
              Response Time Analysis
            </h3>
          </div>
          <div className="card-body">
            {isLoading ? (
              <ChartSkeleton height="h-64" />
            ) : (
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={analyticsData.responseTime}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="time" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Line 
                    type="monotone" 
                    dataKey="avg" 
                    stroke={colors.success} 
                    strokeWidth={2}
                    name="Average"
                  />
                  <Line 
                    type="monotone" 
                    dataKey="p95" 
                    stroke={colors.warning} 
                    strokeWidth={2}
                    name="95th Percentile"
                  />
                  <Line 
                    type="monotone" 
                    dataKey="p99" 
                    stroke={colors.error} 
                    strokeWidth={2}
                    name="99th Percentile"
                  />
                </LineChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>
      </div>

      {/* Detailed Analysis Table */}
      <div className="card">
        <div className="card-header">
          <h3 className="text-lg font-medium text-gray-900 dark:text-white">
            Source Analysis
          </h3>
        </div>
        <div className="card-body p-0">
          <div className="overflow-x-auto">
            <table className="table">
              <thead className="table-header">
                <tr>
                  <th className="table-header-cell">Source</th>
                  <th className="table-header-cell">Total Events</th>
                  <th className="table-header-cell">Error Rate</th>
                  <th className="table-header-cell">Avg Response Time</th>
                  <th className="table-header-cell">Status</th>
                </tr>
              </thead>
              <tbody className="table-body">
                {analyticsData.topSources.map((source, index) => (
                  <tr key={source.name}>
                    <td className="table-cell">
                      <div className="font-medium text-gray-900 dark:text-white">
                        {source.name}
                      </div>
                    </td>
                    <td className="table-cell">
                      {source.count.toLocaleString()}
                    </td>
                    <td className="table-cell">
                      <span className={`badge ${
                        source.percentage > 5 ? 'badge-error' : 
                        source.percentage > 2 ? 'badge-warning' : 'badge-success'
                      }`}>
                        {source.percentage}%
                      </span>
                    </td>
                    <td className="table-cell">
                      {Math.floor(Math.random() * 200 + 100)}ms
                    </td>
                    <td className="table-cell">
                      <span className={`badge ${
                        index < 2 ? 'badge-success' : 
                        index < 4 ? 'badge-warning' : 'badge-gray'
                      }`}>
                        {index < 2 ? 'Healthy' : index < 4 ? 'Warning' : 'Normal'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Analytics;
