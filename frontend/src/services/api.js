import axios from 'axios';

/**
 * API Configuration
 */
const API_BASE_URL = process.env.REACT_APP_API_URL || '/api/v1';

// Create axios instance with default configuration
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000, // 30 seconds
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor for adding auth tokens, logging, etc.
apiClient.interceptors.request.use(
  (config) => {
    // Add timestamp to requests for debugging
    config.metadata = { startTime: new Date() };
    
    // Add auth token if available
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // Log request in development
    if (process.env.NODE_ENV === 'development') {
      console.log(`🚀 API Request: ${config.method?.toUpperCase()} ${config.url}`);
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for error handling, logging, etc.
apiClient.interceptors.response.use(
  (response) => {
    // Calculate request duration
    const duration = new Date() - response.config.metadata.startTime;
    
    // Log response in development
    if (process.env.NODE_ENV === 'development') {
      console.log(`✅ API Response: ${response.status} ${response.config.url} (${duration}ms)`);
    }
    
    return response;
  },
  (error) => {
    // Calculate request duration
    const duration = error.config?.metadata ? 
      new Date() - error.config.metadata.startTime : 0;
    
    // Log error in development
    if (process.env.NODE_ENV === 'development') {
      console.error(`❌ API Error: ${error.response?.status || 'Network'} ${error.config?.url} (${duration}ms)`, error);
    }
    
    // Handle specific error cases
    if (error.response?.status === 401) {
      // Unauthorized - redirect to login
      localStorage.removeItem('authToken');
      window.location.href = '/login';
    }
    
    return Promise.reject(error);
  }
);

/**
 * Search API endpoints
 */
export const searchAPI = {
  // Comprehensive search
  search: (searchQuery) => {
    return apiClient.post('/search', searchQuery);
  },
  
  // Quick search
  quickSearch: (query, page = 1, size = 100) => {
    return apiClient.get('/search/quick', {
      params: { q: query, page, size }
    });
  },
  
  // Search errors
  searchErrors: (startTime, endTime, page = 1, size = 100) => {
    return apiClient.get('/search/errors', {
      params: { startTime, endTime, page, size }
    });
  },
  
  // Search by application
  searchByApplication: (application, environment, startTime, endTime, page = 1, size = 100) => {
    return apiClient.get(`/search/application/${application}`, {
      params: { environment, startTime, endTime, page, size }
    });
  },
  
  // Search by pattern
  searchByPattern: (pattern, mode = 'WILDCARD', startTime, endTime, page = 1, size = 100) => {
    return apiClient.get('/search/pattern', {
      params: { pattern, mode, startTime, endTime, page, size }
    });
  },
  
  // Get statistics
  getStatistics: (startTime, endTime) => {
    return apiClient.get('/search/stats', {
      params: { startTime, endTime }
    });
  },
  
  // Get field suggestions
  getFieldSuggestions: (fieldName, prefix = '', limit = 10) => {
    return apiClient.get(`/search/suggest/${fieldName}`, {
      params: { prefix, limit }
    });
  },
  
  // Get available fields
  getAvailableFields: () => {
    return apiClient.get('/search/fields');
  },
  
  // Validate query
  validateQuery: (searchQuery) => {
    return apiClient.post('/search/validate', searchQuery);
  },
  
  // Get search history
  getSearchHistory: (limit = 20) => {
    return apiClient.get('/search/history', {
      params: { limit }
    });
  },
  
  // Save search
  saveSearch: (name, query) => {
    return apiClient.post('/search/save', query, {
      params: { name }
    });
  },
  
  // Get saved searches
  getSavedSearches: () => {
    return apiClient.get('/search/saved');
  },
};

/**
 * Alerts API endpoints
 */
export const alertsAPI = {
  // Get all alerts
  getAlerts: (page = 1, size = 50) => {
    return apiClient.get('/alerts', {
      params: { page, size }
    });
  },
  
  // Get open alerts
  getOpenAlerts: () => {
    return apiClient.get('/alerts/open');
  },
  
  // Get alerts by severity
  getAlertsBySeverity: (severity) => {
    return apiClient.get(`/alerts/severity/${severity}`);
  },
  
  // Acknowledge alert
  acknowledgeAlert: (alertId, acknowledgedBy) => {
    return apiClient.post(`/alerts/${alertId}/acknowledge`, {
      acknowledgedBy
    });
  },
  
  // Resolve alert
  resolveAlert: (alertId, resolvedBy, notes) => {
    return apiClient.post(`/alerts/${alertId}/resolve`, {
      resolvedBy,
      notes
    });
  },
  
  // Get alert statistics
  getAlertStats: () => {
    return apiClient.get('/alerts/stats');
  },
  
  // Create alert rule
  createAlertRule: (rule) => {
    return apiClient.post('/alerts/rules', rule);
  },
  
  // Get alert rules
  getAlertRules: () => {
    return apiClient.get('/alerts/rules');
  },
  
  // Update alert rule
  updateAlertRule: (ruleId, rule) => {
    return apiClient.put(`/alerts/rules/${ruleId}`, rule);
  },
  
  // Delete alert rule
  deleteAlertRule: (ruleId) => {
    return apiClient.delete(`/alerts/rules/${ruleId}`);
  },
};

/**
 * Dashboard API endpoints
 */
export const dashboardAPI = {
  // Get dashboard statistics
  getStats: () => {
    return apiClient.get('/dashboard/stats');
  },
  
  // Get real-time metrics
  getRealtimeMetrics: () => {
    return apiClient.get('/dashboard/realtime');
  },
  
  // Get log volume over time
  getLogVolume: (startTime, endTime, interval = 'hour') => {
    return apiClient.get('/dashboard/volume', {
      params: { startTime, endTime, interval }
    });
  },
  
  // Get top sources
  getTopSources: (limit = 10) => {
    return apiClient.get('/dashboard/top-sources', {
      params: { limit }
    });
  },
  
  // Get error trends
  getErrorTrends: (startTime, endTime) => {
    return apiClient.get('/dashboard/error-trends', {
      params: { startTime, endTime }
    });
  },
  
  // Get system health
  getSystemHealth: () => {
    return apiClient.get('/dashboard/health');
  },
  
  // Get intelligent health insights based on log analysis
  getHealthInsights: () => {
    return apiClient.get('/dashboard/health-insights');
  },
};

/**
 * Analytics API endpoints
 */
export const analyticsAPI = {
  // Get log analytics
  getLogAnalytics: (startTime, endTime, groupBy = 'hour') => {
    return apiClient.get('/analytics/logs', {
      params: { startTime, endTime, groupBy }
    });
  },
  
  // Get performance metrics
  getPerformanceMetrics: (startTime, endTime) => {
    return apiClient.get('/analytics/performance', {
      params: { startTime, endTime }
    });
  },
  
  // Get error analysis
  getErrorAnalysis: (startTime, endTime) => {
    return apiClient.get('/analytics/errors', {
      params: { startTime, endTime }
    });
  },
  
  // Get usage patterns
  getUsagePatterns: (startTime, endTime) => {
    return apiClient.get('/analytics/patterns', {
      params: { startTime, endTime }
    });
  },
  
  // Export analytics data
  exportData: (format, startTime, endTime, filters = {}) => {
    return apiClient.post('/analytics/export', {
      format,
      startTime,
      endTime,
      filters
    }, {
      responseType: 'blob'
    });
  },
};

/**
 * Settings API endpoints
 */
export const settingsAPI = {
  // Get user settings
  getUserSettings: () => {
    return apiClient.get('/settings/user');
  },
  
  // Update user settings
  updateUserSettings: (settings) => {
    return apiClient.put('/settings/user', settings);
  },
  
  // Get system settings
  getSystemSettings: () => {
    return apiClient.get('/settings/system');
  },
  
  // Update system settings
  updateSystemSettings: (settings) => {
    return apiClient.put('/settings/system', settings);
  },
  
  // Test notification settings
  testNotification: (channel, recipient) => {
    return apiClient.post('/settings/test-notification', {
      channel,
      recipient
    });
  },
};

/**
 * Utility functions
 */
export const apiUtils = {
  // Check if API is healthy
  healthCheck: () => {
    return apiClient.get('/health');
  },
  
  // Get API version
  getVersion: () => {
    return apiClient.get('/version');
  },
  
  // Get API documentation
  getApiDocs: () => {
    return apiClient.get('/docs');
  },
};

/**
 * Upload API endpoints
 */
export const uploadAPI = {
  // Upload log file
  uploadLogFile: (file, source) => {
    const formData = new FormData();
    formData.append('file', file);
    if (source) {
      formData.append('source', source);
    }
    
    return apiClient.post('/logs/upload/file', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },

  // Upload log text
  uploadLogText: (text, source) => {
    const formData = new FormData();
    formData.append('text', text);
    if (source) {
      formData.append('source', source);
    }
    
    return apiClient.post('/logs/upload/text', formData);
  },
};

// Export the configured axios instance for custom requests
export { apiClient };

// Export default API object
const api = {
  search: searchAPI,
  alerts: alertsAPI,
  dashboard: dashboardAPI,
  analytics: analyticsAPI,
  settings: settingsAPI,
  upload: uploadAPI,
  utils: apiUtils,
};

export default api;
