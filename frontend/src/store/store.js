import { configureStore } from '@reduxjs/toolkit';
import searchReducer from './slices/searchSlice';
import alertsReducer from './slices/alertsSlice';
import dashboardReducer from './slices/dashboardSlice';
import settingsReducer from './slices/settingsSlice';
import uiReducer from './slices/uiSlice';

/**
 * Redux store configuration using Redux Toolkit
 * 
 * Features:
 * - Centralized state management
 * - Redux DevTools integration
 * - Middleware for async actions
 * - Immutable state updates
 * - Type-safe actions and reducers
 */
export const store = configureStore({
  reducer: {
    search: searchReducer,
    alerts: alertsReducer,
    dashboard: dashboardReducer,
    settings: settingsReducer,
    ui: uiReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        // Ignore these action types for serialization checks
        ignoredActions: ['persist/PERSIST', 'persist/REHYDRATE'],
        // Ignore these field paths in all actions
        ignoredActionsPaths: ['meta.arg', 'payload.timestamp'],
        // Ignore these paths in the state
        ignoredPaths: ['items.dates'],
      },
    }),
  devTools: process.env.NODE_ENV !== 'production',
});

// Export types for TypeScript (if using TypeScript)
export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
