import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { searchAPI } from '../../services/api';

/**
 * Async thunk for performing log search
 */
export const performSearch = createAsyncThunk(
  'search/performSearch',
  async (searchQuery, { rejectWithValue }) => {
    try {
      const response = await searchAPI.search(searchQuery);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

/**
 * Async thunk for quick search
 */
export const performQuickSearch = createAsyncThunk(
  'search/performQuickSearch',
  async ({ query, page = 1, size = 100 }, { rejectWithValue }) => {
    try {
      // Convert to 0-based pagination for backend
      const response = await searchAPI.quickSearch(query, page - 1, size);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

/**
 * Async thunk for getting search suggestions
 */
export const getSearchSuggestions = createAsyncThunk(
  'search/getSearchSuggestions',
  async ({ fieldName, prefix, limit = 10 }, { rejectWithValue }) => {
    try {
      const response = await searchAPI.getFieldSuggestions(fieldName, prefix, limit);
      return { fieldName, suggestions: response.data };
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

/**
 * Async thunk for getting available fields
 */
export const getAvailableFields = createAsyncThunk(
  'search/getAvailableFields',
  async (_, { rejectWithValue }) => {
    try {
      const response = await searchAPI.getAvailableFields();
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

/**
 * Async thunk for saving search
 */
export const saveSearch = createAsyncThunk(
  'search/saveSearch',
  async ({ name, query }, { rejectWithValue }) => {
    try {
      const response = await searchAPI.saveSearch(name, query);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

/**
 * Async thunk for getting saved searches
 */
export const getSavedSearches = createAsyncThunk(
  'search/getSavedSearches',
  async (_, { rejectWithValue }) => {
    try {
      const response = await searchAPI.getSavedSearches();
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

/**
 * Initial state for search slice
 */
const initialState = {
  // Search results
  results: {
    logs: [],
    totalHits: 0,
    page: 1,
    size: 100,
    totalPages: 0,
    searchTimeMs: 0,
    searchId: null,
    aggregations: {},
    highlights: {},
    metadata: null,
  },
  
  // Current search query
  currentQuery: {
    query: '',
    startTime: null,
    endTime: null,
    page: 1,
    size: 100,
    sources: [],
    levels: [],
    hosts: [],
    applications: [],
    environments: [],
    filters: {},
    sortFields: [],
    includeStackTrace: false,
    includeMetadata: false,
    highlightMatches: true,
    searchMode: 'FULL_TEXT',
    caseSensitive: false,
    useRegex: false,
    timeZone: 'UTC',
  },
  
  // UI state
  isLoading: false,
  error: null,
  
  // Search history
  searchHistory: [],
  
  // Saved searches
  savedSearches: [],
  
  // Field suggestions
  suggestions: {},
  availableFields: [],
  
  // Search filters
  activeFilters: {
    timeRange: null,
    levels: [],
    sources: [],
    hosts: [],
    applications: [],
    environments: [],
    custom: {},
  },
  
  // Search preferences
  preferences: {
    defaultPageSize: 100,
    autoRefresh: false,
    refreshInterval: 30000, // 30 seconds
    highlightEnabled: true,
    showMetadata: false,
    showStackTrace: false,
  },
};

/**
 * Search slice with reducers and actions
 */
const searchSlice = createSlice({
  name: 'search',
  initialState,
  reducers: {
    // Update current query
    updateQuery: (state, action) => {
      state.currentQuery = { ...state.currentQuery, ...action.payload };
    },
    
    // Set search filters
    setFilters: (state, action) => {
      state.activeFilters = { ...state.activeFilters, ...action.payload };
    },
    
    // Clear search filters
    clearFilters: (state) => {
      state.activeFilters = initialState.activeFilters;
    },
    
    // Add to search history
    addToHistory: (state, action) => {
      const { query, timestamp, results } = action.payload;
      const historyItem = {
        query,
        timestamp: timestamp || new Date().toISOString(),
        results: results || 0,
        id: Date.now(),
      };
      
      // Add to beginning and limit to 50 items
      state.searchHistory = [historyItem, ...state.searchHistory.slice(0, 49)];
    },
    
    // Clear search history
    clearHistory: (state) => {
      state.searchHistory = [];
    },
    
    // Update preferences
    updatePreferences: (state, action) => {
      state.preferences = { ...state.preferences, ...action.payload };
    },
    
    // Clear search results
    clearResults: (state) => {
      state.results = initialState.results;
    },
    
    // Clear error
    clearError: (state) => {
      state.error = null;
    },
    
    // Set loading state
    setLoading: (state, action) => {
      state.isLoading = action.payload;
    },
  },
  
  extraReducers: (builder) => {
    // Perform search
    builder
      .addCase(performSearch.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(performSearch.fulfilled, (state, action) => {
        state.isLoading = false;
        state.results = action.payload;
        
        // Add to search history
        const historyItem = {
          query: state.currentQuery.query,
          timestamp: new Date().toISOString(),
          results: action.payload.totalHits,
          id: Date.now(),
        };
        state.searchHistory = [historyItem, ...state.searchHistory.slice(0, 49)];
      })
      .addCase(performSearch.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload;
      });
    
    // Quick search
    builder
      .addCase(performQuickSearch.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(performQuickSearch.fulfilled, (state, action) => {
        state.isLoading = false;
        
        // Adjust page number to 1-based for frontend
        const results = {
          ...action.payload,
          page: (action.payload?.page || 0) + 1
        };
        
        state.results = results;
      })
      .addCase(performQuickSearch.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload;
      });
    
    // Search suggestions
    builder
      .addCase(getSearchSuggestions.fulfilled, (state, action) => {
        const { fieldName, suggestions } = action.payload;
        state.suggestions[fieldName] = suggestions;
      });
    
    // Available fields
    builder
      .addCase(getAvailableFields.fulfilled, (state, action) => {
        state.availableFields = action.payload;
      });
    
    // Save search
    builder
      .addCase(saveSearch.fulfilled, (state, action) => {
        state.savedSearches = [action.payload, ...state.savedSearches];
      });
    
    // Get saved searches
    builder
      .addCase(getSavedSearches.fulfilled, (state, action) => {
        state.savedSearches = action.payload;
      });
  },
});

// Export actions
export const {
  updateQuery,
  setFilters,
  clearFilters,
  addToHistory,
  clearHistory,
  updatePreferences,
  clearResults,
  clearError,
  setLoading,
} = searchSlice.actions;

// Selectors
export const selectSearchResults = (state) => state.search.results;
export const selectCurrentQuery = (state) => state.search.currentQuery;
export const selectIsLoading = (state) => state.search.isLoading;
export const selectError = (state) => state.search.error;
export const selectSearchHistory = (state) => state.search.searchHistory;
export const selectSavedSearches = (state) => state.search.savedSearches;
export const selectActiveFilters = (state) => state.search.activeFilters;
export const selectPreferences = (state) => state.search.preferences;
export const selectSuggestions = (state) => state.search.suggestions;
export const selectAvailableFields = (state) => state.search.availableFields;

// Export reducer
export default searchSlice.reducer;
