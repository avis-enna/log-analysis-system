import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { alertsAPI } from '../../services/api';

/**
 * Async thunk for fetching alerts
 */
export const fetchAlerts = createAsyncThunk(
  'alerts/fetchAlerts',
  async ({ page = 1, size = 50 } = {}, { rejectWithValue }) => {
    try {
      const response = await alertsAPI.getAlerts(page, size);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

/**
 * Async thunk for fetching open alerts
 */
export const fetchOpenAlerts = createAsyncThunk(
  'alerts/fetchOpenAlerts',
  async (_, { rejectWithValue }) => {
    try {
      const response = await alertsAPI.getOpenAlerts();
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

/**
 * Async thunk for acknowledging an alert
 */
export const acknowledgeAlert = createAsyncThunk(
  'alerts/acknowledgeAlert',
  async ({ alertId, acknowledgedBy }, { rejectWithValue }) => {
    try {
      const response = await alertsAPI.acknowledgeAlert(alertId, acknowledgedBy);
      return { alertId, ...response.data };
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

/**
 * Async thunk for resolving an alert
 */
export const resolveAlert = createAsyncThunk(
  'alerts/resolveAlert',
  async ({ alertId, resolvedBy, notes }, { rejectWithValue }) => {
    try {
      const response = await alertsAPI.resolveAlert(alertId, resolvedBy, notes);
      return { alertId, ...response.data };
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

/**
 * Async thunk for fetching alerts by severity
 */
export const fetchAlertsBySeverity = createAsyncThunk(
  'alerts/fetchAlertsBySeverity',
  async (severity, { rejectWithValue }) => {
    try {
      const response = await alertsAPI.getAlertsBySeverity(severity);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

/**
 * Initial state for alerts slice
 */
const initialState = {
  // Alerts data
  alerts: [],
  openAlerts: [],
  totalAlerts: 0,
  page: 1,
  size: 50,
  totalPages: 0,
  
  // Loading states
  isLoading: false,
  isLoadingOpen: false,
  isAcknowledging: {},
  isResolving: {},
  
  // Error states
  error: null,
  
  // Filters
  filters: {
    severity: [],
    status: [],
    source: [],
    dateRange: null,
    search: '',
  },
  
  // UI state
  selectedAlert: null,
  showAcknowledgeModal: false,
  showResolveModal: false,
  
  // Real-time updates
  newAlertsCount: 0,
  lastUpdate: null,
  
  // Statistics
  stats: {
    total: 0,
    open: 0,
    acknowledged: 0,
    resolved: 0,
    critical: 0,
    high: 0,
    medium: 0,
    low: 0,
  },
  
  // Preferences
  preferences: {
    autoRefresh: true,
    refreshInterval: 30000, // 30 seconds
    showNotifications: true,
    soundEnabled: false,
    groupBySeverity: false,
  },
};

/**
 * Alerts slice with reducers and actions
 */
const alertsSlice = createSlice({
  name: 'alerts',
  initialState,
  reducers: {
    // Add new alert (from WebSocket)
    addAlert: (state, action) => {
      const newAlert = action.payload;
      state.alerts = [newAlert, ...state.alerts];
      state.openAlerts = [newAlert, ...state.openAlerts];
      state.newAlertsCount += 1;
      state.lastUpdate = new Date().toISOString();
      
      // Update statistics
      state.stats.total += 1;
      state.stats.open += 1;
      state.stats[newAlert.severity.toLowerCase()] += 1;
    },
    
    // Update alert status
    updateAlertStatus: (state, action) => {
      const { alertId, status, acknowledgedBy, resolvedBy, notes } = action.payload;
      
      // Update in alerts array
      const alertIndex = state.alerts.findIndex(alert => alert.id === alertId);
      if (alertIndex !== -1) {
        state.alerts[alertIndex] = {
          ...state.alerts[alertIndex],
          status,
          acknowledgedBy,
          resolvedBy,
          notes,
          updatedAt: new Date().toISOString(),
        };
      }
      
      // Update in open alerts if needed
      if (status !== 'OPEN') {
        state.openAlerts = state.openAlerts.filter(alert => alert.id !== alertId);
        state.stats.open -= 1;
        
        if (status === 'ACKNOWLEDGED') {
          state.stats.acknowledged += 1;
        } else if (status === 'RESOLVED') {
          state.stats.resolved += 1;
        }
      }
    },
    
    // Set filters
    setFilters: (state, action) => {
      state.filters = { ...state.filters, ...action.payload };
    },
    
    // Clear filters
    clearFilters: (state) => {
      state.filters = initialState.filters;
    },
    
    // Set selected alert
    setSelectedAlert: (state, action) => {
      state.selectedAlert = action.payload;
    },
    
    // Show/hide modals
    setShowAcknowledgeModal: (state, action) => {
      state.showAcknowledgeModal = action.payload;
    },
    
    setShowResolveModal: (state, action) => {
      state.showResolveModal = action.payload;
    },
    
    // Clear new alerts count
    clearNewAlertsCount: (state) => {
      state.newAlertsCount = 0;
    },
    
    // Update preferences
    updatePreferences: (state, action) => {
      state.preferences = { ...state.preferences, ...action.payload };
    },
    
    // Clear error
    clearError: (state) => {
      state.error = null;
    },
    
    // Update statistics
    updateStats: (state, action) => {
      state.stats = { ...state.stats, ...action.payload };
    },
  },
  
  extraReducers: (builder) => {
    // Fetch alerts
    builder
      .addCase(fetchAlerts.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(fetchAlerts.fulfilled, (state, action) => {
        state.isLoading = false;
        state.alerts = action.payload.alerts || action.payload;
        state.totalAlerts = action.payload.total || action.payload.length;
        state.page = action.payload.page || 1;
        state.totalPages = action.payload.totalPages || 1;
        state.lastUpdate = new Date().toISOString();
      })
      .addCase(fetchAlerts.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload;
      });
    
    // Fetch open alerts
    builder
      .addCase(fetchOpenAlerts.pending, (state) => {
        state.isLoadingOpen = true;
      })
      .addCase(fetchOpenAlerts.fulfilled, (state, action) => {
        state.isLoadingOpen = false;
        state.openAlerts = action.payload;
      })
      .addCase(fetchOpenAlerts.rejected, (state, action) => {
        state.isLoadingOpen = false;
        state.error = action.payload;
      });
    
    // Acknowledge alert
    builder
      .addCase(acknowledgeAlert.pending, (state, action) => {
        const alertId = action.meta.arg.alertId;
        state.isAcknowledging[alertId] = true;
      })
      .addCase(acknowledgeAlert.fulfilled, (state, action) => {
        const { alertId } = action.payload;
        state.isAcknowledging[alertId] = false;
        
        // Update alert status
        alertsSlice.caseReducers.updateAlertStatus(state, {
          payload: { alertId, status: 'ACKNOWLEDGED', ...action.payload }
        });
      })
      .addCase(acknowledgeAlert.rejected, (state, action) => {
        const alertId = action.meta.arg.alertId;
        state.isAcknowledging[alertId] = false;
        state.error = action.payload;
      });
    
    // Resolve alert
    builder
      .addCase(resolveAlert.pending, (state, action) => {
        const alertId = action.meta.arg.alertId;
        state.isResolving[alertId] = true;
      })
      .addCase(resolveAlert.fulfilled, (state, action) => {
        const { alertId } = action.payload;
        state.isResolving[alertId] = false;
        
        // Update alert status
        alertsSlice.caseReducers.updateAlertStatus(state, {
          payload: { alertId, status: 'RESOLVED', ...action.payload }
        });
      })
      .addCase(resolveAlert.rejected, (state, action) => {
        const alertId = action.meta.arg.alertId;
        state.isResolving[alertId] = false;
        state.error = action.payload;
      });
  },
});

// Export actions
export const {
  addAlert,
  updateAlertStatus,
  setFilters,
  clearFilters,
  setSelectedAlert,
  setShowAcknowledgeModal,
  setShowResolveModal,
  clearNewAlertsCount,
  updatePreferences,
  clearError,
  updateStats,
} = alertsSlice.actions;

// Selectors
export const selectAlerts = (state) => state.alerts.alerts;
export const selectOpenAlerts = (state) => state.alerts.openAlerts;
export const selectIsLoading = (state) => state.alerts.isLoading;
export const selectError = (state) => state.alerts.error;
export const selectFilters = (state) => state.alerts.filters;
export const selectSelectedAlert = (state) => state.alerts.selectedAlert;
export const selectNewAlertsCount = (state) => state.alerts.newAlertsCount;
export const selectStats = (state) => state.alerts.stats;
export const selectPreferences = (state) => state.alerts.preferences;

// Export reducer
export default alertsSlice.reducer;
