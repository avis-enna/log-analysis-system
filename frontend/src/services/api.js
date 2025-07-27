import axios from 'axios';

// Create the base API client
const apiClient = axios.create({
  baseURL: process.env.NODE_ENV === 'production' ? '/api/v1' : 'http://localhost:8080/api/v1',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add authentication interceptor
apiClient.interceptors.request.use(
  (config) => {
    // Add auth token if available
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    } else {
      // Use basic auth for admin:admin123 as fallback
      const credentials = btoa('admin:admin123');
      config.headers.Authorization = `Basic ${credentials}`;
    }
    
    console.log('API Request:', config.method?.toUpperCase(), config.url);
    return config;
  },
  (error) => {
    console.error('Request error:', error);
    return Promise.reject(error);
  }
);

// Add response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => {
    console.log('API Response:', response.status, response.config.url);
    return response;
  },
  (error) => {
    console.error('API Error:', error.response?.status, error.config?.url, error.message);
    
    if (error.response?.status === 401) {
      // Handle unauthorized access
      localStorage.removeItem('authToken');
      // Don't redirect here, let components handle it
    }
    
    return Promise.reject(error);
  }
);

// Dashboard API endpoints
export const dashboardAPI = {
  getStats: () => apiClient.get('/dashboard/stats'),
  getRecentLogs: () => apiClient.get('/dashboard/recent-logs'),
  getLogLevels: () => apiClient.get('/dashboard/log-levels'),
  getTopSources: () => apiClient.get('/dashboard/top-sources'),
  getHourlyTrends: () => apiClient.get('/dashboard/hourly-trends'),
  getErrorTrends: () => apiClient.get('/dashboard/error-trends'),
  getSystemHealth: () => apiClient.get('/dashboard/system-health'),
  getAlerts: () => apiClient.get('/dashboard/alerts'),
};

// Search API endpoints
export const searchAPI = {
  searchLogs: (params) => apiClient.get('/search', { params }),
  getFilters: () => apiClient.get('/search/filters'),
  saveSearch: (searchData) => apiClient.post('/search/saved', searchData),
  getSavedSearches: () => apiClient.get('/search/saved'),
  deleteSavedSearch: (id) => apiClient.delete(`/search/saved/${id}`),
};

// Alerts API endpoints
export const alertsAPI = {
  getAlerts: (params) => apiClient.get('/alerts', { params }),
  getAlert: (id) => apiClient.get(`/alerts/${id}`),
  createAlert: (alertData) => apiClient.post('/alerts', alertData),
  updateAlert: (id, alertData) => apiClient.put(`/alerts/${id}`, alertData),
  deleteAlert: (id) => apiClient.delete(`/alerts/${id}`),
  acknowledgeAlert: (id) => apiClient.post(`/alerts/${id}/acknowledge`),
  getAlertStats: () => apiClient.get('/alerts/stats'),
};

// Analytics API endpoints
export const analyticsAPI = {
  getMetrics: (timeRange) => apiClient.get('/analytics/metrics', { params: { timeRange } }),
  getTrendData: (metric, timeRange) => apiClient.get('/analytics/trends', { params: { metric, timeRange } }),
  getTopErrors: (timeRange) => apiClient.get('/analytics/top-errors', { params: { timeRange } }),
  getPerformanceMetrics: () => apiClient.get('/analytics/performance'),
};

// Settings API endpoints
export const settingsAPI = {
  getSettings: () => apiClient.get('/settings'),
  updateSettings: (settings) => apiClient.put('/settings', settings),
  getNotificationSettings: () => apiClient.get('/settings/notifications'),
  updateNotificationSettings: (settings) => apiClient.put('/settings/notifications', settings),
  getRetentionSettings: () => apiClient.get('/settings/retention'),
  updateRetentionSettings: (settings) => apiClient.put('/settings/retention', settings),
};

// Upload API endpoints
export const uploadAPI = {
  uploadLogs: (formData) => apiClient.post('/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  }),
  getUploadHistory: () => apiClient.get('/upload/history'),
};

// Authentication API endpoints
export const authAPI = {
  login: (credentials) => apiClient.post('/auth/login', credentials),
  logout: () => apiClient.post('/auth/logout'),
  verifyToken: () => apiClient.get('/auth/verify'),
};

// Export the configured axios instance for custom requests
export { apiClient };

// Export default API object
const api = {
  dashboard: dashboardAPI,
  search: searchAPI,
  alerts: alertsAPI,
  analytics: analyticsAPI,
  settings: settingsAPI,
  upload: uploadAPI,
  auth: authAPI,
  client: apiClient,
};

export default api;
