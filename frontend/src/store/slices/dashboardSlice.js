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
  async ({ limit = 10 }, { rejectWithValue }) => {
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
 * Async thunk for fetching intelligent health insights
 */
export const fetchHealthInsights = createAsyncThunk(
  'dashboard/fetchHealthInsights',
  async (_, { rejectWithValue }) => {
    try {
      const response = await dashboardAPI.getHealthInsights();
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

// Initial state
const initialState = {
  stats: {
    totalLogs: 0,
    logsToday: 0,
    totalErrors: 0,
    totalWarnings: 0,
    errorRate: 0,
    totalSources: 0,
    totalApplications: 0,
    totalEnvironments: 0,
    systemHealth: 'unknown',
    primaryLogType: 'general',
    categoryBreakdown: {},
    lastUpdate: null,
  },
  realtimeMetrics: {
    logsPerSecond: 0,
    errorsPerMinute: 0,
    errorRate: 0,
    warningRate: 0,
    activeConnections: 0,
    cpuUsage: 0,
    memoryUsage: 0,
    diskUsage: 0,
    networkIO: 0,
    timestamp: null,
  },
  healthInsights: {
    primaryLogType: 'general',
    categoryBreakdown: {},
    title: 'System Health',
    type: 'general',
    recommendations: [],
  },
  logVolumeData: [],
  topSourcesData: [],
  errorTrendsData: [],
  isLoading: false,
  error: null,
};

// Dashboard slice
const dashboardSlice = createSlice({
  name: 'dashboard',
  initialState,
  reducers: {
    clearError: (state) => {
      state.error = null;
    },
    resetDashboard: (state) => {
      return initialState;
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch dashboard stats
      .addCase(fetchDashboardStats.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(fetchDashboardStats.fulfilled, (state, action) => {
        state.isLoading = false;
        state.stats = action.payload;
      })
      .addCase(fetchDashboardStats.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload;
      })
      // Fetch realtime metrics
      .addCase(fetchRealtimeMetrics.pending, (state) => {
        state.isLoading = true;
      })
      .addCase(fetchRealtimeMetrics.fulfilled, (state, action) => {
        state.isLoading = false;
        state.realtimeMetrics = action.payload;
      })
      .addCase(fetchRealtimeMetrics.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload;
      })
      // Fetch health insights
      .addCase(fetchHealthInsights.pending, (state) => {
        state.isLoading = true;
      })
      .addCase(fetchHealthInsights.fulfilled, (state, action) => {
        state.isLoading = false;
        state.healthInsights = action.payload;
      })
      .addCase(fetchHealthInsights.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload;
      })
      // Fetch log volume data
      .addCase(fetchLogVolume.pending, (state) => {
        state.isLoading = true;
      })
      .addCase(fetchLogVolume.fulfilled, (state, action) => {
        state.isLoading = false;
        state.logVolumeData = action.payload;
      })
      .addCase(fetchLogVolume.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload;
      })
      // Fetch top sources data
      .addCase(fetchTopSources.pending, (state) => {
        state.isLoading = true;
      })
      .addCase(fetchTopSources.fulfilled, (state, action) => {
        state.isLoading = false;
        state.topSourcesData = action.payload;
      })
      .addCase(fetchTopSources.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload;
      })
      // Fetch error trends data
      .addCase(fetchErrorTrends.pending, (state) => {
        state.isLoading = true;
      })
      .addCase(fetchErrorTrends.fulfilled, (state, action) => {
        state.isLoading = false;
        state.errorTrendsData = action.payload;
      })
      .addCase(fetchErrorTrends.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload;
      });
  },
});

// Action creators
export const { clearError, resetDashboard } = dashboardSlice.actions;

// Selectors
export const selectStats = (state) => state.dashboard.stats;
export const selectRealtimeMetrics = (state) => state.dashboard.realtimeMetrics;
export const selectHealthInsights = (state) => state.dashboard.healthInsights;
export const selectLogVolumeData = (state) => state.dashboard.logVolumeData;
export const selectTopSourcesData = (state) => state.dashboard.topSourcesData;
export const selectErrorTrendsData = (state) => state.dashboard.errorTrendsData;
export const selectIsLoading = (state) => state.dashboard.isLoading;
export const selectError = (state) => state.dashboard.error;
export const selectSystemStatus = (state) => state.dashboard.stats.systemHealth;

export default dashboardSlice.reducer;