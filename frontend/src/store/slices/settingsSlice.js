import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { settingsAPI } from '../../services/api';

/**
 * Async thunk for fetching user settings
 */
export const fetchUserSettings = createAsyncThunk(
  'settings/fetchUserSettings',
  async (_, { rejectWithValue }) => {
    try {
      const response = await settingsAPI.getUserSettings();
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

/**
 * Async thunk for updating user settings
 */
export const updateUserSettings = createAsyncThunk(
  'settings/updateUserSettings',
  async (settings, { rejectWithValue }) => {
    try {
      const response = await settingsAPI.updateUserSettings(settings);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

/**
 * Async thunk for fetching system configuration
 */
export const fetchSystemConfig = createAsyncThunk(
  'settings/fetchSystemConfig',
  async (_, { rejectWithValue }) => {
    try {
      const response = await settingsAPI.getSystemConfig();
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

/**
 * Initial state for settings slice
 */
const initialState = {
  // User preferences
  userSettings: {
    theme: 'light', // light, dark, auto
    language: 'en',
    timezone: 'UTC',
    dateFormat: 'YYYY-MM-DD HH:mm:ss',
    pageSize: 100,
    autoRefresh: true,
    refreshInterval: 30000, // 30 seconds
    notifications: {
      enabled: true,
      sound: false,
      desktop: true,
      email: false,
      severityFilter: ['HIGH', 'CRITICAL'],
    },
    dashboard: {
      layout: 'grid',
      widgets: [],
      compactMode: false,
      showLegends: true,
      animationsEnabled: true,
    },
    search: {
      defaultPageSize: 100,
      highlightEnabled: true,
      showMetadata: false,
      showStackTrace: false,
      caseSensitive: false,
      useRegex: false,
    },
  },
  
  // System configuration
  systemConfig: {
    version: '1.0.0',
    buildDate: null,
    features: {
      realTimeProcessing: true,
      patternDetection: true,
      alerting: true,
      dashboards: true,
      apiAccess: true,
    },
    limits: {
      maxSearchResults: 10000,
      maxDashboardWidgets: 50,
      maxConcurrentSearches: 100,
      maxFileUploadSize: '100MB',
    },
    integrations: {
      elasticsearch: { enabled: true, status: 'connected' },
      kafka: { enabled: true, status: 'connected' },
      redis: { enabled: true, status: 'connected' },
      influxdb: { enabled: true, status: 'connected' },
    },
  },
  
  // Application settings
  appSettings: {
    maintenance: {
      enabled: false,
      message: '',
      scheduledTime: null,
    },
    security: {
      sessionTimeout: 3600000, // 1 hour
      maxLoginAttempts: 5,
      passwordPolicy: {
        minLength: 8,
        requireUppercase: true,
        requireLowercase: true,
        requireNumbers: true,
        requireSpecialChars: true,
      },
    },
    performance: {
      cacheEnabled: true,
      cacheTTL: 300, // 5 minutes
      compressionEnabled: true,
      rateLimiting: {
        enabled: true,
        requestsPerMinute: 1000,
      },
    },
  },
  
  // Loading states
  isLoading: {
    userSettings: false,
    systemConfig: false,
    updating: false,
  },
  
  // Error states
  errors: {
    userSettings: null,
    systemConfig: null,
    updating: null,
  },
  
  // UI state
  activeTab: 'general',
  hasUnsavedChanges: false,
  
  // Available options
  availableOptions: {
    themes: ['light', 'dark', 'auto'],
    languages: [
      { code: 'en', name: 'English' },
      { code: 'es', name: 'Español' },
      { code: 'fr', name: 'Français' },
      { code: 'de', name: 'Deutsch' },
    ],
    timezones: [
      'UTC',
      'America/New_York',
      'America/Los_Angeles',
      'Europe/London',
      'Europe/Paris',
      'Asia/Tokyo',
      'Asia/Shanghai',
    ],
    dateFormats: [
      'YYYY-MM-DD HH:mm:ss',
      'MM/DD/YYYY HH:mm:ss',
      'DD/MM/YYYY HH:mm:ss',
      'YYYY-MM-DD',
      'MM/DD/YYYY',
      'DD/MM/YYYY',
    ],
    pageSizes: [25, 50, 100, 200, 500],
    refreshIntervals: [
      { value: 5000, label: '5 seconds' },
      { value: 10000, label: '10 seconds' },
      { value: 30000, label: '30 seconds' },
      { value: 60000, label: '1 minute' },
      { value: 300000, label: '5 minutes' },
    ],
  },
};

/**
 * Settings slice with reducers and actions
 */
const settingsSlice = createSlice({
  name: 'settings',
  initialState,
  reducers: {
    // Update user settings locally
    updateUserSettingsLocal: (state, action) => {
      state.userSettings = { ...state.userSettings, ...action.payload };
      state.hasUnsavedChanges = true;
    },
    
    // Update nested user settings
    updateNestedUserSettings: (state, action) => {
      const { path, value } = action.payload;
      const keys = path.split('.');
      let current = state.userSettings;
      
      for (let i = 0; i < keys.length - 1; i++) {
        if (!current[keys[i]]) {
          current[keys[i]] = {};
        }
        current = current[keys[i]];
      }
      
      current[keys[keys.length - 1]] = value;
      state.hasUnsavedChanges = true;
    },
    
    // Set active tab
    setActiveTab: (state, action) => {
      state.activeTab = action.payload;
    },
    
    // Mark changes as saved
    markChangesSaved: (state) => {
      state.hasUnsavedChanges = false;
    },
    
    // Reset to defaults
    resetToDefaults: (state) => {
      state.userSettings = initialState.userSettings;
      state.hasUnsavedChanges = true;
    },
    
    // Update theme
    updateTheme: (state, action) => {
      state.userSettings.theme = action.payload;
      state.hasUnsavedChanges = true;
      
      // Apply theme immediately to document
      if (typeof document !== 'undefined') {
        const root = document.documentElement;
        if (action.payload === 'dark') {
          root.classList.add('dark');
        } else if (action.payload === 'light') {
          root.classList.remove('dark');
        } else if (action.payload === 'auto') {
          const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
          if (prefersDark) {
            root.classList.add('dark');
          } else {
            root.classList.remove('dark');
          }
        }
      }
    },
    
    // Update notification settings
    updateNotificationSettings: (state, action) => {
      state.userSettings.notifications = {
        ...state.userSettings.notifications,
        ...action.payload,
      };
      state.hasUnsavedChanges = true;
    },
    
    // Update dashboard settings
    updateDashboardSettings: (state, action) => {
      state.userSettings.dashboard = {
        ...state.userSettings.dashboard,
        ...action.payload,
      };
      state.hasUnsavedChanges = true;
    },
    
    // Update search settings
    updateSearchSettings: (state, action) => {
      state.userSettings.search = {
        ...state.userSettings.search,
        ...action.payload,
      };
      state.hasUnsavedChanges = true;
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
    
    // Update system status
    updateSystemStatus: (state, action) => {
      const { service, status } = action.payload;
      if (state.systemConfig.integrations[service]) {
        state.systemConfig.integrations[service].status = status;
      }
    },
  },
  
  extraReducers: (builder) => {
    // Fetch user settings
    builder
      .addCase(fetchUserSettings.pending, (state) => {
        state.isLoading.userSettings = true;
        state.errors.userSettings = null;
      })
      .addCase(fetchUserSettings.fulfilled, (state, action) => {
        state.isLoading.userSettings = false;
        state.userSettings = { ...state.userSettings, ...action.payload };
        state.hasUnsavedChanges = false;
      })
      .addCase(fetchUserSettings.rejected, (state, action) => {
        state.isLoading.userSettings = false;
        state.errors.userSettings = action.payload;
      });
    
    // Update user settings
    builder
      .addCase(updateUserSettings.pending, (state) => {
        state.isLoading.updating = true;
        state.errors.updating = null;
      })
      .addCase(updateUserSettings.fulfilled, (state, action) => {
        state.isLoading.updating = false;
        state.userSettings = { ...state.userSettings, ...action.payload };
        state.hasUnsavedChanges = false;
      })
      .addCase(updateUserSettings.rejected, (state, action) => {
        state.isLoading.updating = false;
        state.errors.updating = action.payload;
      });
    
    // Fetch system config
    builder
      .addCase(fetchSystemConfig.pending, (state) => {
        state.isLoading.systemConfig = true;
        state.errors.systemConfig = null;
      })
      .addCase(fetchSystemConfig.fulfilled, (state, action) => {
        state.isLoading.systemConfig = false;
        state.systemConfig = { ...state.systemConfig, ...action.payload };
      })
      .addCase(fetchSystemConfig.rejected, (state, action) => {
        state.isLoading.systemConfig = false;
        state.errors.systemConfig = action.payload;
      });
  },
});

// Export actions
export const {
  updateUserSettingsLocal,
  updateNestedUserSettings,
  setActiveTab,
  markChangesSaved,
  resetToDefaults,
  updateTheme,
  updateNotificationSettings,
  updateDashboardSettings,
  updateSearchSettings,
  clearError,
  updateSystemStatus,
} = settingsSlice.actions;

// Selectors
export const selectUserSettings = (state) => state.settings.userSettings;
export const selectSystemConfig = (state) => state.settings.systemConfig;
export const selectAppSettings = (state) => state.settings.appSettings;
export const selectIsLoading = (state) => state.settings.isLoading;
export const selectErrors = (state) => state.settings.errors;
export const selectActiveTab = (state) => state.settings.activeTab;
export const selectHasUnsavedChanges = (state) => state.settings.hasUnsavedChanges;
export const selectAvailableOptions = (state) => state.settings.availableOptions;
export const selectTheme = (state) => state.settings.userSettings.theme;

// Export reducer
export default settingsSlice.reducer;
