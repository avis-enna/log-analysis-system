import React, { useEffect, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  ChartBarIcon,
  ExclamationTriangleIcon,
  DocumentTextIcon,
  ClockIcon,
  ServerIcon,
  CpuChipIcon,
} from '@heroicons/react/24/outline';
import {
  LineChart,
  Line,
  AreaChart,
  Area,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import {
  fetchDashboardStats,
  fetchRealtimeMetrics,
  fetchLogVolume,
  fetchTopSources,
  fetchErrorTrends,
  fetchHealthInsights,
  selectStats,
  selectRealtimeMetrics,
  selectHealthInsights,
  selectLogVolumeData,
  selectTopSourcesData,
  selectErrorTrendsData,
  selectIsLoading,
} from '../store/slices/dashboardSlice';
import { selectOpenAlerts } from '../store/slices/alertsSlice';
import LoadingSpinner, { CardSkeleton, ChartSkeleton } from '../components/LoadingSpinner/LoadingSpinner';

/**
 * Dashboard page with real-time metrics and visualizations
 */
const Dashboard = () => {
  const dispatch = useDispatch();
  const stats = useSelector(selectStats);
  const realtimeMetrics = useSelector(selectRealtimeMetrics);
  const healthInsights = useSelector(selectHealthInsights);
  const logVolumeData = useSelector(selectLogVolumeData);
  const topSourcesData = useSelector(selectTopSourcesData);
  const errorTrendsData = useSelector(selectErrorTrendsData);
  const isLoading = useSelector(selectIsLoading);
  const openAlerts = useSelector(selectOpenAlerts);

  const [timeRange, setTimeRange] = useState('24h');
  const [autoRefresh, setAutoRefresh] = useState(true);

  // Fetch initial data
  useEffect(() => {
    const fetchData = () => {
      dispatch(fetchDashboardStats());
      dispatch(fetchRealtimeMetrics());
      dispatch(fetchHealthInsights());
      
      const endTime = new Date();
      const startTime = new Date();
      startTime.setHours(startTime.getHours() - 24);
      
      dispatch(fetchLogVolume({ 
        startTime: startTime.toISOString(), 
        endTime: endTime.toISOString(),
        interval: 'hour'
      }));
      dispatch(fetchTopSources(10));
      dispatch(fetchErrorTrends({ 
        startTime: startTime.toISOString(), 
        endTime: endTime.toISOString()
      }));
    };

    fetchData();

    // Set up auto-refresh
    let interval;
    if (autoRefresh) {
      interval = setInterval(fetchData, 30000); // Refresh every 30 seconds
    }

    return () => {
      if (interval) clearInterval(interval);
    };
  }, [dispatch, autoRefresh]);

  // Format numbers for display
  const formatNumber = (num) => {
    if (num == null || num === undefined || isNaN(num)) return '0';
    if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
    if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
    return num.toString();
  };

  // Format percentage
  const formatPercentage = (num) => {
    if (num == null || num === undefined || isNaN(num)) return '0.0%';
    return `${num.toFixed(1)}%`;
  };

  // Get health status color
  const getHealthColor = (status) => {
    switch (status) {
      case 'healthy': return 'text-green-600 bg-green-100 dark:bg-green-900/20';
      case 'warning': return 'text-yellow-600 bg-yellow-100 dark:bg-yellow-900/20';
      case 'error': return 'text-red-600 bg-red-100 dark:bg-red-900/20';
      default: return 'text-gray-600 bg-gray-100 dark:bg-gray-900/20';
    }
  };

  // Chart colors
  const chartColors = {
    primary: '#3b82f6',
    success: '#22c55e',
    warning: '#f59e0b',
    error: '#ef4444',
    secondary: '#64748b',
  };

  // Pie chart colors for sources
  const pieColors = ['#3b82f6', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#f97316'];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Dashboard
          </h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Real-time system overview and metrics
          </p>
        </div>
        
        <div className="mt-4 sm:mt-0 flex items-center space-x-3">
          {/* Time Range Selector */}
          <select
            value={timeRange}
            onChange={(e) => setTimeRange(e.target.value)}
            className="form-input text-sm"
          >
            <option value="1h">Last Hour</option>
            <option value="24h">Last 24 Hours</option>
            <option value="7d">Last 7 Days</option>
            <option value="30d">Last 30 Days</option>
          </select>
          
          {/* Auto Refresh Toggle */}
          <label className="flex items-center space-x-2 text-sm">
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
              className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
            />
            <span className="text-gray-700 dark:text-gray-300">Auto Refresh</span>
          </label>
        </div>
      </div>

      {/* Key Metrics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {/* Total Logs */}
        <div className="card">
          <div className="card-body">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <DocumentTextIcon className="h-8 w-8 text-primary-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Total Logs
                </p>
                {isLoading.stats ? (
                  <div className="h-8 w-20 bg-gray-200 dark:bg-gray-700 rounded animate-pulse" />
                ) : (
                  <p className="text-2xl font-semibold text-gray-900 dark:text-white">
                    {formatNumber(stats.totalLogs)}
                  </p>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Logs Today */}
        <div className="card">
          <div className="card-body">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <ClockIcon className="h-8 w-8 text-success-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Logs Today
                </p>
                {isLoading.stats ? (
                  <div className="h-8 w-20 bg-gray-200 dark:bg-gray-700 rounded animate-pulse" />
                ) : (
                  <p className="text-2xl font-semibold text-gray-900 dark:text-white">
                    {formatNumber(stats.logsToday)}
                  </p>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Error Rate */}
        <div className="card">
          <div className="card-body">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <ExclamationTriangleIcon className="h-8 w-8 text-warning-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Error Rate
                </p>
                {isLoading.stats ? (
                  <div className="h-8 w-20 bg-gray-200 dark:bg-gray-700 rounded animate-pulse" />
                ) : (
                  <p className="text-2xl font-semibold text-gray-900 dark:text-white">
                    {formatPercentage(stats.errorRate)}
                  </p>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Active Alerts */}
        <div className="card">
          <div className="card-body">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <ExclamationTriangleIcon className="h-8 w-8 text-error-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Active Alerts
                </p>
                <p className="text-2xl font-semibold text-gray-900 dark:text-white">
                  {openAlerts.length}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Real-time Metrics */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Intelligent Health Insights */}
        <div className="card">
          <div className="card-header">
            <h3 className="text-lg font-medium text-gray-900 dark:text-white">
              {healthInsights.title || 'System Health'}
            </h3>
            <span className={`px-2 py-1 rounded-full text-xs font-medium ${
              stats.primaryLogType === 'security' ? 'text-red-600 bg-red-100 dark:bg-red-900/20' :
              stats.primaryLogType === 'deployment' ? 'text-blue-600 bg-blue-100 dark:bg-blue-900/20' :
              stats.primaryLogType === 'performance' ? 'text-yellow-600 bg-yellow-100 dark:bg-yellow-900/20' :
              stats.primaryLogType === 'application' ? 'text-green-600 bg-green-100 dark:bg-green-900/20' :
              'text-gray-600 bg-gray-100 dark:bg-gray-900/20'
            }`}>
              {stats.primaryLogType?.toUpperCase() || 'GENERAL'}
            </span>
          </div>
          <div className="card-body">
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600 dark:text-gray-400">Overall Status</span>
                <span className={`px-2 py-1 rounded-full text-xs font-medium ${getHealthColor(stats.systemHealth)}`}>
                  {stats.systemHealth}
                </span>
              </div>
              
              {/* Dynamic content based on log type */}
              {stats.primaryLogType === 'security' && (
                <>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600 dark:text-gray-400">Auth Failures</span>
                    <span className="text-sm font-medium text-gray-900 dark:text-white">
                      {formatNumber(healthInsights.authenticationFailures || 0)}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600 dark:text-gray-400">Security Events</span>
                    <span className="text-sm font-medium text-gray-900 dark:text-white">
                      {formatNumber(healthInsights.securityEvents || 0)}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600 dark:text-gray-400">Threat Level</span>
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                      healthInsights.threatLevel === 'HIGH' ? 'text-red-600 bg-red-100 dark:bg-red-900/20' :
                      healthInsights.threatLevel === 'MEDIUM' ? 'text-yellow-600 bg-yellow-100 dark:bg-yellow-900/20' :
                      'text-green-600 bg-green-100 dark:bg-green-900/20'
                    }`}>
                      {healthInsights.threatLevel || 'LOW'}
                    </span>
                  </div>
                </>
              )}
              
              {stats.primaryLogType === 'deployment' && (
                <>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600 dark:text-gray-400">Success Rate</span>
                    <span className="text-sm font-medium text-gray-900 dark:text-white">
                      {formatPercentage(healthInsights.deploymentSuccessRate || 0)}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600 dark:text-gray-400">Deploy Errors</span>
                    <span className="text-sm font-medium text-gray-900 dark:text-white">
                      {formatNumber(healthInsights.deploymentErrors || 0)}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600 dark:text-gray-400">Deploy Success</span>
                    <span className="text-sm font-medium text-gray-900 dark:text-white">
                      {formatNumber(healthInsights.deploymentSuccess || 0)}
                    </span>
                  </div>
                </>
              )}
              
              {stats.primaryLogType === 'performance' && (
                <>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600 dark:text-gray-400">Slow Queries</span>
                    <span className="text-sm font-medium text-gray-900 dark:text-white">
                      {formatNumber(healthInsights.slowQueries || 0)}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600 dark:text-gray-400">Perf Warnings</span>
                    <span className="text-sm font-medium text-gray-900 dark:text-white">
                      {formatNumber(healthInsights.performanceWarnings || 0)}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600 dark:text-gray-400">Status</span>
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                      healthInsights.performanceStatus === 'DEGRADED' ? 'text-yellow-600 bg-yellow-100 dark:bg-yellow-900/20' :
                      'text-green-600 bg-green-100 dark:bg-green-900/20'
                    }`}>
                      {healthInsights.performanceStatus || 'OPTIMAL'}
                    </span>
                  </div>
                </>
              )}
              
              {/* Default content for general or other types */}
              {(!stats.primaryLogType || stats.primaryLogType === 'general' || 
                !['security', 'deployment', 'performance'].includes(stats.primaryLogType)) && (
                <>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600 dark:text-gray-400">Error Rate</span>
                    <span className="text-sm font-medium text-gray-900 dark:text-white">
                      {formatPercentage(stats.errorRate)}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600 dark:text-gray-400">Total Errors</span>
                    <span className="text-sm font-medium text-gray-900 dark:text-white">
                      {formatNumber(stats.totalErrors)}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600 dark:text-gray-400">Total Warnings</span>
                    <span className="text-sm font-medium text-gray-900 dark:text-white">
                      {formatNumber(stats.totalWarnings)}
                    </span>
                  </div>
                </>
              )}
            </div>
            
            {/* Intelligent Recommendations */}
            {healthInsights.recommendations && healthInsights.recommendations.length > 0 && (
              <div className="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
                <h4 className="text-sm font-medium text-gray-900 dark:text-white mb-2">
                  Recommendations
                </h4>
                <div className="space-y-2">
                  {healthInsights.recommendations.slice(0, 3).map((rec, index) => (
                    <div key={index} className="flex items-start space-x-2">
                      <div className={`flex-shrink-0 w-2 h-2 rounded-full mt-2 ${
                        rec.priority === 'CRITICAL' ? 'bg-red-500' :
                        rec.priority === 'HIGH' ? 'bg-red-400' :
                        rec.priority === 'MEDIUM' ? 'bg-yellow-400' :
                        'bg-gray-400'
                      }`} />
                      <div className="flex-1 min-w-0">
                        <p className="text-xs text-gray-600 dark:text-gray-400">
                          {rec.action}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Real-time Processing */}
        <div className="card">
          <div className="card-header">
            <h3 className="text-lg font-medium text-gray-900 dark:text-white">
              Real-time Processing
            </h3>
          </div>
          <div className="card-body">
            <div className="space-y-4">
              <div className="text-center">
                <p className="text-3xl font-bold text-primary-600">
                  {realtimeMetrics?.logsPerSecond ?? 0}
                </p>
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  Logs per second
                </p>
              </div>
              <div className="text-center">
                <p className="text-2xl font-semibold text-error-600">
                  {realtimeMetrics?.errorsPerMinute ?? 0}
                </p>
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  Errors per minute
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Response Time */}
        <div className="card">
          <div className="card-header">
            <h3 className="text-lg font-medium text-gray-900 dark:text-white">
              Performance
            </h3>
          </div>
          <div className="card-body">
            <div className="space-y-4">
              <div className="text-center">
                <p className="text-3xl font-bold text-success-600">
                  {stats?.avgResponseTime ?? 0}ms
                </p>
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  Average response time
                </p>
              </div>
              <div className="text-center">
                <p className="text-2xl font-semibold text-secondary-600">
                  {formatNumber(realtimeMetrics.networkIO)}
                </p>
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  Network I/O (KB/s)
                </p>
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
              Log Volume Over Time
            </h3>
          </div>
          <div className="card-body">
            {isLoading.logVolume ? (
              <ChartSkeleton height="h-64" />
            ) : (
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart data={logVolumeData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="timestamp" />
                  <YAxis />
                  <Tooltip />
                  <Area 
                    type="monotone" 
                    dataKey="count" 
                    stroke={chartColors.primary} 
                    fill={chartColors.primary}
                    fillOpacity={0.3}
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
              Error Trends
            </h3>
          </div>
          <div className="card-body">
            {isLoading.errorTrends ? (
              <ChartSkeleton height="h-64" />
            ) : (
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={errorTrendsData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="timestamp" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Line 
                    type="monotone" 
                    dataKey="errors" 
                    stroke={chartColors.error} 
                    strokeWidth={2}
                  />
                  <Line 
                    type="monotone" 
                    dataKey="warnings" 
                    stroke={chartColors.warning} 
                    strokeWidth={2}
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
            {isLoading.topSources ? (
              <ChartSkeleton height="h-64" />
            ) : (
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={topSourcesData}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ name, percent }) => {
                      const percentage = percent != null && !isNaN(percent) ? (percent * 100).toFixed(0) : '0';
                      return `${name} ${percentage}%`;
                    }}
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="count"
                  >
                    {topSourcesData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={pieColors[index % pieColors.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        {/* Recent Activity */}
        <div className="card">
          <div className="card-header">
            <h3 className="text-lg font-medium text-gray-900 dark:text-white">
              Recent Activity
            </h3>
          </div>
          <div className="card-body">
            <div className="space-y-4">
              <div className="flex items-center space-x-3">
                <div className="flex-shrink-0">
                  <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm text-gray-900 dark:text-white">
                    System health check completed
                  </p>
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    2 minutes ago
                  </p>
                </div>
              </div>
              
              <div className="flex items-center space-x-3">
                <div className="flex-shrink-0">
                  <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm text-gray-900 dark:text-white">
                    High error rate detected in web-server
                  </p>
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    5 minutes ago
                  </p>
                </div>
              </div>
              
              <div className="flex items-center space-x-3">
                <div className="flex-shrink-0">
                  <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm text-gray-900 dark:text-white">
                    New log source connected: api-gateway
                  </p>
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    10 minutes ago
                  </p>
                </div>
              </div>
              
              <div className="flex items-center space-x-3">
                <div className="flex-shrink-0">
                  <div className="w-2 h-2 bg-red-500 rounded-full"></div>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm text-gray-900 dark:text-white">
                    Critical alert: Database connection timeout
                  </p>
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    15 minutes ago
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
