import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { dashboardAPI } from '../../services/api';

/**
 * Async thunk for fetching dashboard statistics
 */
export const fetchDashboardStats = createAsyncThunk(
  'dashboard/fetchStats',
  async (_, { rejectWithValue }) => {
    try {
      const response = await dashboardAPI.getStats();
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

/**
 * Async thunk for fetching real-time metrics
 */
export const fetchRealtimeMetrics = createAsyncThunk(
  'dashboard/fetchRealtimeMetrics',
  async (_, { rejectWithValue }) => {
    try {
      const response = await dashboardAPI.getRealtimeMetrics();
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

/**
 * Async thunk for fetching log volume data
 */
export const fetchLogVolume = createAsyncThunk(
  'dashboard/fetchLogVolume',
  async ({ startTime, endTime, interval = 'hour' }, { rejectWithValue }) => {
    try {
      const response = await dashboardAPI.getLogVolume(startTime, endTime, interval);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

/**
 * Async thunk for fetching top sources
 */
export const fetchTopSources = createAsyncThunk(
  'dashboard/fetchTopSources',
  async (limit = 10, { rejectWithValue }) => {
    try {
      const response = await dashboardAPI.getTopSources(limit);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

/**
 * Async thunk for fetching error trends
 */
export const fetchErrorTrends = createAsyncThunk(
  'dashboard/fetchErrorTrends',
  async ({ startTime, endTime }, { rejectWithValue }) => {
    try {
      const response = await dashboardAPI.getErrorTrends(startTime, endTime);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

/**
 * Initial state for dashboard slice
 */
const initialState = {
  // Statistics
  stats: {
    totalLogs: 0,
    logsToday: 0,
    errorRate: 0,
    avgResponseTime: 0,
    activeAlerts: 0,
    systemHealth: 'healthy',
    lastUpdate: null,
  },
  
  // Real-time metrics
  realtimeMetrics: {
    logsPerSecond: 0,
    errorsPerMinute: 0,
    cpuUsage: 0,
    memoryUsage: 0,
    diskUsage: 0,
    networkIO: 0,
  },
  
  // Chart data
  logVolumeData: [],
  errorTrendsData: [],
  topSourcesData: [],
  
  // Time range
  timeRange: {
    startTime: null,
    endTime: null,
    interval: 'hour',
  },
  
  // Loading states
  isLoading: {
    stats: false,
    realtime: false,
    logVolume: false,
    topSources: false,
    errorTrends: false,
  },
  
  // Error states
  errors: {
    stats: null,
    realtime: null,
    logVolume: null,
    topSources: null,
    errorTrends: null,
  },
  
  // UI state
  selectedWidget: null,
  refreshInterval: 30000, // 30 seconds
  autoRefresh: true,
  
  // Widget configuration
  widgets: [
    { id: 'stats', title: 'System Statistics', enabled: true, order: 1 },
    { id: 'logVolume', title: 'Log Volume', enabled: true, order: 2 },
    { id: 'errorTrends', title: 'Error Trends', enabled: true, order: 3 },
    { id: 'topSources', title: 'Top Sources', enabled: true, order: 4 },
    { id: 'realtimeMetrics', title: 'Real-time Metrics', enabled: true, order: 5 },
  ],
  
  // Dashboard preferences
  preferences: {
    theme: 'light',
    layout: 'grid',
    showLegends: true,
    animationsEnabled: true,
    compactMode: false,
  },
  
  // Recent activity
  recentActivity: [],
  
  // System status
  systemStatus: {
    elasticsearch: 'healthy',
    database: 'healthy',
    kafka: 'healthy',
    redis: 'healthy',
    api: 'healthy',
  },
};

/**
 * Dashboard slice with reducers and actions
 */
const dashboardSlice = createSlice({
  name: 'dashboard',
  initialState,
  reducers: {
    // Update real-time metrics (from WebSocket)
    updateRealtimeMetrics: (state, action) => {
      state.realtimeMetrics = { ...state.realtimeMetrics, ...action.payload };
    },
    
    // Update statistics
    updateStats: (state, action) => {
      state.stats = { ...state.stats, ...action.payload, lastUpdate: new Date().toISOString() };
    },
    
    // Set time range
    setTimeRange: (state, action) => {
      state.timeRange = { ...state.timeRange, ...action.payload };
    },
    
    // Set selected widget
    setSelectedWidget: (state, action) => {
      state.selectedWidget = action.payload;
    },
    
    // Update widget configuration
    updateWidget: (state, action) => {
      const { id, updates } = action.payload;
      const widgetIndex = state.widgets.findIndex(widget => widget.id === id);
      if (widgetIndex !== -1) {
        state.widgets[widgetIndex] = { ...state.widgets[widgetIndex], ...updates };
      }
    },
    
    // Reorder widgets
    reorderWidgets: (state, action) => {
      state.widgets = action.payload;
    },
    
    // Update preferences
    updatePreferences: (state, action) => {
      state.preferences = { ...state.preferences, ...action.payload };
    },
    
    // Set auto refresh
    setAutoRefresh: (state, action) => {
      state.autoRefresh = action.payload;
    },
    
    // Set refresh interval
    setRefreshInterval: (state, action) => {
      state.refreshInterval = action.payload;
    },
    
    // Add recent activity
    addRecentActivity: (state, action) => {
      const activity = {
        ...action.payload,
        timestamp: new Date().toISOString(),
        id: Date.now(),
      };
      state.recentActivity = [activity, ...state.recentActivity.slice(0, 49)];
    },
    
    // Update system status
    updateSystemStatus: (state, action) => {
      state.systemStatus = { ...state.systemStatus, ...action.payload };
    },
    
    // Clear errors
    clearError: (state, action) => {
      const errorType = action.payload;
      if (errorType) {
        state.errors[errorType] = null;
      } else {
        state.errors = initialState.errors;
      }
    },
    
    // Reset dashboard
    resetDashboard: (state) => {
      return { ...initialState, preferences: state.preferences };
    },
  },
  
  extraReducers: (builder) => {
    // Fetch dashboard stats
    builder
      .addCase(fetchDashboardStats.pending, (state) => {
        state.isLoading.stats = true;
        state.errors.stats = null;
      })
      .addCase(fetchDashboardStats.fulfilled, (state, action) => {
        state.isLoading.stats = false;
        state.stats = { ...action.payload, lastUpdate: new Date().toISOString() };
      })
      .addCase(fetchDashboardStats.rejected, (state, action) => {
        state.isLoading.stats = false;
        state.errors.stats = action.payload;
      });
    
    // Fetch realtime metrics
    builder
      .addCase(fetchRealtimeMetrics.pending, (state) => {
        state.isLoading.realtime = true;
        state.errors.realtime = null;
      })
      .addCase(fetchRealtimeMetrics.fulfilled, (state, action) => {
        state.isLoading.realtime = false;
        state.realtimeMetrics = action.payload;
      })
      .addCase(fetchRealtimeMetrics.rejected, (state, action) => {
        state.isLoading.realtime = false;
        state.errors.realtime = action.payload;
      });
    
    // Fetch log volume
    builder
      .addCase(fetchLogVolume.pending, (state) => {
        state.isLoading.logVolume = true;
        state.errors.logVolume = null;
      })
      .addCase(fetchLogVolume.fulfilled, (state, action) => {
        state.isLoading.logVolume = false;
        state.logVolumeData = action.payload;
      })
      .addCase(fetchLogVolume.rejected, (state, action) => {
        state.isLoading.logVolume = false;
        state.errors.logVolume = action.payload;
      });
    
    // Fetch top sources
    builder
      .addCase(fetchTopSources.pending, (state) => {
        state.isLoading.topSources = true;
        state.errors.topSources = null;
      })
      .addCase(fetchTopSources.fulfilled, (state, action) => {
        state.isLoading.topSources = false;
        state.topSourcesData = action.payload;
      })
      .addCase(fetchTopSources.rejected, (state, action) => {
        state.isLoading.topSources = false;
        state.errors.topSources = action.payload;
      });
    
    // Fetch error trends
    builder
      .addCase(fetchErrorTrends.pending, (state) => {
        state.isLoading.errorTrends = true;
        state.errors.errorTrends = null;
      })
      .addCase(fetchErrorTrends.fulfilled, (state, action) => {
        state.isLoading.errorTrends = false;
        state.errorTrendsData = action.payload;
      })
      .addCase(fetchErrorTrends.rejected, (state, action) => {
        state.isLoading.errorTrends = false;
        state.errors.errorTrends = action.payload;
      });
  },
});

// Export actions
export const {
  updateRealtimeMetrics,
  updateStats,
  setTimeRange,
  setSelectedWidget,
  updateWidget,
  reorderWidgets,
  updatePreferences,
  setAutoRefresh,
  setRefreshInterval,
  addRecentActivity,
  updateSystemStatus,
  clearError,
  resetDashboard,
} = dashboardSlice.actions;

// Selectors
export const selectStats = (state) => state.dashboard.stats;
export const selectRealtimeMetrics = (state) => state.dashboard.realtimeMetrics;
export const selectLogVolumeData = (state) => state.dashboard.logVolumeData;
export const selectErrorTrendsData = (state) => state.dashboard.errorTrendsData;
export const selectTopSourcesData = (state) => state.dashboard.topSourcesData;
export const selectTimeRange = (state) => state.dashboard.timeRange;
export const selectIsLoading = (state) => state.dashboard.isLoading;
export const selectErrors = (state) => state.dashboard.errors;
export const selectWidgets = (state) => state.dashboard.widgets;
export const selectPreferences = (state) => state.dashboard.preferences;
export const selectRecentActivity = (state) => state.dashboard.recentActivity;
export const selectSystemStatus = (state) => state.dashboard.systemStatus;
export const selectAutoRefresh = (state) => state.dashboard.autoRefresh;

// Export reducer
export default dashboardSlice.reducer;
