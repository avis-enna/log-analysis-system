import { createSlice } from '@reduxjs/toolkit';

/**
 * Initial state for UI slice
 */
const initialState = {
  // Navigation
  sidebarOpen: true,
  sidebarCollapsed: false,
  currentPage: 'dashboard',
  breadcrumbs: [],
  
  // Modals and overlays
  modals: {
    confirmDialog: {
      open: false,
      title: '',
      message: '',
      onConfirm: null,
      onCancel: null,
      confirmText: 'Confirm',
      cancelText: 'Cancel',
      type: 'info', // info, warning, error, success
    },
    logDetails: {
      open: false,
      logEntry: null,
    },
    alertDetails: {
      open: false,
      alert: null,
    },
    settings: {
      open: false,
      activeTab: 'general',
    },
    fileUpload: {
      open: false,
      acceptedTypes: ['.log', '.txt'],
      maxSize: 100 * 1024 * 1024, // 100MB
    },
  },
  
  // Loading states
  globalLoading: false,
  loadingMessage: '',
  
  // Notifications
  notifications: [],
  
  // Theme and appearance
  theme: 'light', // light, dark, auto
  compactMode: false,
  animationsEnabled: true,
  
  // Layout
  layout: {
    headerHeight: 64,
    sidebarWidth: 256,
    sidebarCollapsedWidth: 64,
    footerHeight: 48,
  },
  
  // Search UI state
  searchUI: {
    showFilters: false,
    showAdvanced: false,
    showHistory: false,
    showSuggestions: false,
    selectedSuggestion: -1,
  },
  
  // Dashboard UI state
  dashboardUI: {
    editMode: false,
    selectedWidget: null,
    widgetBeingResized: null,
    showWidgetSelector: false,
  },
  
  // Alerts UI state
  alertsUI: {
    selectedAlerts: [],
    showBulkActions: false,
    filterPanelOpen: false,
    sortBy: 'timestamp',
    sortOrder: 'desc',
  },
  
  // Table states
  tables: {
    logs: {
      selectedRows: [],
      sortBy: 'timestamp',
      sortOrder: 'desc',
      columnVisibility: {},
      columnOrder: [],
    },
    alerts: {
      selectedRows: [],
      sortBy: 'timestamp',
      sortOrder: 'desc',
      columnVisibility: {},
      columnOrder: [],
    },
  },
  
  // Form states
  forms: {
    search: {
      isDirty: false,
      errors: {},
      touched: {},
    },
    settings: {
      isDirty: false,
      errors: {},
      touched: {},
    },
  },
  
  // Responsive design
  viewport: {
    width: typeof window !== 'undefined' ? window.innerWidth : 1920,
    height: typeof window !== 'undefined' ? window.innerHeight : 1080,
    isMobile: false,
    isTablet: false,
    isDesktop: true,
  },
  
  // Performance monitoring
  performance: {
    renderTimes: {},
    apiResponseTimes: {},
    lastUpdate: null,
  },
  
  // Accessibility
  accessibility: {
    highContrast: false,
    reducedMotion: false,
    screenReader: false,
    keyboardNavigation: false,
  },
};

/**
 * UI slice with reducers and actions
 */
const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    // Navigation actions
    setSidebarOpen: (state, action) => {
      state.sidebarOpen = action.payload;
    },
    
    setSidebarCollapsed: (state, action) => {
      state.sidebarCollapsed = action.payload;
    },
    
    toggleSidebar: (state) => {
      state.sidebarOpen = !state.sidebarOpen;
    },
    
    toggleSidebarCollapsed: (state) => {
      state.sidebarCollapsed = !state.sidebarCollapsed;
    },
    
    setCurrentPage: (state, action) => {
      state.currentPage = action.payload;
    },
    
    setBreadcrumbs: (state, action) => {
      state.breadcrumbs = action.payload;
    },
    
    // Modal actions
    openModal: (state, action) => {
      const { modalName, props = {} } = action.payload;
      if (state.modals[modalName]) {
        state.modals[modalName] = { ...state.modals[modalName], open: true, ...props };
      }
    },
    
    closeModal: (state, action) => {
      const modalName = action.payload;
      if (state.modals[modalName]) {
        state.modals[modalName].open = false;
      }
    },
    
    closeAllModals: (state) => {
      Object.keys(state.modals).forEach(modalName => {
        state.modals[modalName].open = false;
      });
    },
    
    // Loading actions
    setGlobalLoading: (state, action) => {
      state.globalLoading = action.payload;
    },
    
    setLoadingMessage: (state, action) => {
      state.loadingMessage = action.payload;
    },
    
    // Notification actions
    addNotification: (state, action) => {
      const notification = {
        id: Date.now(),
        timestamp: new Date().toISOString(),
        ...action.payload,
      };
      state.notifications.push(notification);
    },
    
    removeNotification: (state, action) => {
      const id = action.payload;
      state.notifications = state.notifications.filter(n => n.id !== id);
    },
    
    clearNotifications: (state) => {
      state.notifications = [];
    },
    
    // Theme actions
    setTheme: (state, action) => {
      state.theme = action.payload;
    },
    
    toggleTheme: (state) => {
      state.theme = state.theme === 'light' ? 'dark' : 'light';
    },
    
    setCompactMode: (state, action) => {
      state.compactMode = action.payload;
    },
    
    setAnimationsEnabled: (state, action) => {
      state.animationsEnabled = action.payload;
    },
    
    // Search UI actions
    setSearchUIState: (state, action) => {
      state.searchUI = { ...state.searchUI, ...action.payload };
    },
    
    toggleSearchFilters: (state) => {
      state.searchUI.showFilters = !state.searchUI.showFilters;
    },
    
    toggleSearchAdvanced: (state) => {
      state.searchUI.showAdvanced = !state.searchUI.showAdvanced;
    },
    
    // Dashboard UI actions
    setDashboardUIState: (state, action) => {
      state.dashboardUI = { ...state.dashboardUI, ...action.payload };
    },
    
    toggleDashboardEditMode: (state) => {
      state.dashboardUI.editMode = !state.dashboardUI.editMode;
    },
    
    // Alerts UI actions
    setAlertsUIState: (state, action) => {
      state.alertsUI = { ...state.alertsUI, ...action.payload };
    },
    
    toggleAlertSelection: (state, action) => {
      const alertId = action.payload;
      const index = state.alertsUI.selectedAlerts.indexOf(alertId);
      if (index > -1) {
        state.alertsUI.selectedAlerts.splice(index, 1);
      } else {
        state.alertsUI.selectedAlerts.push(alertId);
      }
    },
    
    clearAlertSelection: (state) => {
      state.alertsUI.selectedAlerts = [];
    },
    
    // Table actions
    setTableState: (state, action) => {
      const { tableName, updates } = action.payload;
      if (state.tables[tableName]) {
        state.tables[tableName] = { ...state.tables[tableName], ...updates };
      }
    },
    
    toggleRowSelection: (state, action) => {
      const { tableName, rowId } = action.payload;
      if (state.tables[tableName]) {
        const selectedRows = state.tables[tableName].selectedRows;
        const index = selectedRows.indexOf(rowId);
        if (index > -1) {
          selectedRows.splice(index, 1);
        } else {
          selectedRows.push(rowId);
        }
      }
    },
    
    clearRowSelection: (state, action) => {
      const tableName = action.payload;
      if (state.tables[tableName]) {
        state.tables[tableName].selectedRows = [];
      }
    },
    
    // Form actions
    setFormState: (state, action) => {
      const { formName, updates } = action.payload;
      if (state.forms[formName]) {
        state.forms[formName] = { ...state.forms[formName], ...updates };
      }
    },
    
    setFormError: (state, action) => {
      const { formName, field, error } = action.payload;
      if (state.forms[formName]) {
        state.forms[formName].errors[field] = error;
      }
    },
    
    clearFormErrors: (state, action) => {
      const formName = action.payload;
      if (state.forms[formName]) {
        state.forms[formName].errors = {};
      }
    },
    
    // Viewport actions
    setViewport: (state, action) => {
      const { width, height } = action.payload;
      state.viewport.width = width;
      state.viewport.height = height;
      state.viewport.isMobile = width < 768;
      state.viewport.isTablet = width >= 768 && width < 1024;
      state.viewport.isDesktop = width >= 1024;
    },
    
    // Performance actions
    recordRenderTime: (state, action) => {
      const { component, time } = action.payload;
      state.performance.renderTimes[component] = time;
      state.performance.lastUpdate = new Date().toISOString();
    },
    
    recordApiResponseTime: (state, action) => {
      const { endpoint, time } = action.payload;
      state.performance.apiResponseTimes[endpoint] = time;
    },
    
    // Accessibility actions
    setAccessibilityState: (state, action) => {
      state.accessibility = { ...state.accessibility, ...action.payload };
    },
    
    // Reset UI state
    resetUIState: (state) => {
      return { ...initialState, theme: state.theme, viewport: state.viewport };
    },
  },
});

// Export actions
export const {
  setSidebarOpen,
  setSidebarCollapsed,
  toggleSidebar,
  toggleSidebarCollapsed,
  setCurrentPage,
  setBreadcrumbs,
  openModal,
  closeModal,
  closeAllModals,
  setGlobalLoading,
  setLoadingMessage,
  addNotification,
  removeNotification,
  clearNotifications,
  setTheme,
  toggleTheme,
  setCompactMode,
  setAnimationsEnabled,
  setSearchUIState,
  toggleSearchFilters,
  toggleSearchAdvanced,
  setDashboardUIState,
  toggleDashboardEditMode,
  setAlertsUIState,
  toggleAlertSelection,
  clearAlertSelection,
  setTableState,
  toggleRowSelection,
  clearRowSelection,
  setFormState,
  setFormError,
  clearFormErrors,
  setViewport,
  recordRenderTime,
  recordApiResponseTime,
  setAccessibilityState,
  resetUIState,
} = uiSlice.actions;

// Selectors
export const selectSidebarOpen = (state) => state.ui.sidebarOpen;
export const selectSidebarCollapsed = (state) => state.ui.sidebarCollapsed;
export const selectCurrentPage = (state) => state.ui.currentPage;
export const selectBreadcrumbs = (state) => state.ui.breadcrumbs;
export const selectModals = (state) => state.ui.modals;
export const selectGlobalLoading = (state) => state.ui.globalLoading;
export const selectNotifications = (state) => state.ui.notifications;
export const selectTheme = (state) => state.ui.theme;
export const selectSearchUI = (state) => state.ui.searchUI;
export const selectDashboardUI = (state) => state.ui.dashboardUI;
export const selectAlertsUI = (state) => state.ui.alertsUI;
export const selectTables = (state) => state.ui.tables;
export const selectViewport = (state) => state.ui.viewport;
export const selectAccessibility = (state) => state.ui.accessibility;

// Export reducer
export default uiSlice.reducer;
