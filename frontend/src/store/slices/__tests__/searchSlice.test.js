import { configureStore } from '@reduxjs/toolkit';
import searchReducer, {
  updateQuery,
  setFilters,
  clearFilters,
  addToHistory,
  clearHistory,
  updatePreferences,
  clearResults,
  clearError,
  performSearch,
  performQuickSearch,
  getSearchSuggestions,
} from '../searchSlice';
import { searchAPI } from '../../../services/api';

// Mock the API
jest.mock('../../../services/api');

describe('searchSlice', () => {
  let store;

  beforeEach(() => {
    store = configureStore({
      reducer: {
        search: searchReducer,
      },
    });
    jest.clearAllMocks();
  });

  describe('initial state', () => {
    it('should have correct initial state', () => {
      const state = store.getState().search;
      
      expect(state.results.logs).toEqual([]);
      expect(state.results.totalHits).toBe(0);
      expect(state.results.page).toBe(1);
      expect(state.results.size).toBe(100);
      expect(state.currentQuery.query).toBe('');
      expect(state.currentQuery.page).toBe(1);
      expect(state.currentQuery.size).toBe(100);
      expect(state.isLoading).toBe(false);
      expect(state.error).toBeNull();
      expect(state.searchHistory).toEqual([]);
      expect(state.savedSearches).toEqual([]);
    });
  });

  describe('synchronous actions', () => {
    it('should update query', () => {
      const queryUpdate = {
        query: 'ERROR',
        page: 2,
        size: 50,
      };

      store.dispatch(updateQuery(queryUpdate));
      const state = store.getState().search;

      expect(state.currentQuery.query).toBe('ERROR');
      expect(state.currentQuery.page).toBe(2);
      expect(state.currentQuery.size).toBe(50);
    });

    it('should set filters', () => {
      const filters = {
        levels: ['ERROR', 'FATAL'],
        sources: ['web-service'],
        timeRange: '24h',
      };

      store.dispatch(setFilters(filters));
      const state = store.getState().search;

      expect(state.activeFilters.levels).toEqual(['ERROR', 'FATAL']);
      expect(state.activeFilters.sources).toEqual(['web-service']);
      expect(state.activeFilters.timeRange).toBe('24h');
    });

    it('should clear filters', () => {
      // First set some filters
      store.dispatch(setFilters({
        levels: ['ERROR'],
        sources: ['web-service'],
      }));

      // Then clear them
      store.dispatch(clearFilters());
      const state = store.getState().search;

      expect(state.activeFilters.levels).toEqual([]);
      expect(state.activeFilters.sources).toEqual([]);
      expect(state.activeFilters.timeRange).toBeNull();
    });

    it('should add to search history', () => {
      const historyItem = {
        query: 'ERROR',
        timestamp: '2024-01-01T12:00:00Z',
        results: 150,
      };

      store.dispatch(addToHistory(historyItem));
      const state = store.getState().search;

      expect(state.searchHistory).toHaveLength(1);
      expect(state.searchHistory[0].query).toBe('ERROR');
      expect(state.searchHistory[0].results).toBe(150);
      expect(state.searchHistory[0].id).toBeDefined();
    });

    it('should limit search history to 50 items', () => {
      // Add 52 items to history
      for (let i = 0; i < 52; i++) {
        store.dispatch(addToHistory({
          query: `query-${i}`,
          timestamp: new Date().toISOString(),
          results: i,
        }));
      }

      const state = store.getState().search;
      expect(state.searchHistory).toHaveLength(50);
      expect(state.searchHistory[0].query).toBe('query-51'); // Most recent first
    });

    it('should clear search history', () => {
      // Add some history items
      store.dispatch(addToHistory({ query: 'test1', results: 10 }));
      store.dispatch(addToHistory({ query: 'test2', results: 20 }));

      // Clear history
      store.dispatch(clearHistory());
      const state = store.getState().search;

      expect(state.searchHistory).toEqual([]);
    });

    it('should update preferences', () => {
      const preferences = {
        defaultPageSize: 200,
        autoRefresh: true,
        refreshInterval: 60000,
      };

      store.dispatch(updatePreferences(preferences));
      const state = store.getState().search;

      expect(state.preferences.defaultPageSize).toBe(200);
      expect(state.preferences.autoRefresh).toBe(true);
      expect(state.preferences.refreshInterval).toBe(60000);
    });

    it('should clear results', () => {
      // First set some results (simulate a successful search)
      const mockResults = {
        logs: [{ id: '1', message: 'test' }],
        totalHits: 1,
        page: 1,
        size: 100,
      };

      store.dispatch({
        type: performSearch.fulfilled.type,
        payload: mockResults,
      });

      // Then clear results
      store.dispatch(clearResults());
      const state = store.getState().search;

      expect(state.results.logs).toEqual([]);
      expect(state.results.totalHits).toBe(0);
    });

    it('should clear error', () => {
      // First set an error
      store.dispatch({
        type: performSearch.rejected.type,
        payload: 'Search failed',
      });

      // Then clear error
      store.dispatch(clearError());
      const state = store.getState().search;

      expect(state.error).toBeNull();
    });
  });

  describe('async actions', () => {
    describe('performSearch', () => {
      it('should handle successful search', async () => {
        const mockResponse = {
          data: {
            logs: [
              { id: '1', message: 'Error occurred', level: 'ERROR' },
              { id: '2', message: 'Another error', level: 'ERROR' },
            ],
            totalHits: 2,
            page: 1,
            size: 100,
            searchTimeMs: 150,
            searchId: 'search-123',
          },
        };

        searchAPI.search.mockResolvedValue(mockResponse);

        const searchQuery = {
          query: 'ERROR',
          page: 1,
          size: 100,
        };

        await store.dispatch(performSearch(searchQuery));
        const state = store.getState().search;

        expect(state.isLoading).toBe(false);
        expect(state.error).toBeNull();
        expect(state.results.logs).toHaveLength(2);
        expect(state.results.totalHits).toBe(2);
        expect(state.results.searchTimeMs).toBe(150);
        expect(state.searchHistory).toHaveLength(1);
      });

      it('should handle search error', async () => {
        const mockError = {
          response: {
            data: 'Invalid search query',
          },
        };

        searchAPI.search.mockRejectedValue(mockError);

        const searchQuery = {
          query: 'invalid[[[query',
          page: 1,
          size: 100,
        };

        await store.dispatch(performSearch(searchQuery));
        const state = store.getState().search;

        expect(state.isLoading).toBe(false);
        expect(state.error).toBe('Invalid search query');
        expect(state.results.logs).toEqual([]);
      });

      it('should set loading state during search', () => {
        const mockPromise = new Promise(() => {}); // Never resolves
        searchAPI.search.mockReturnValue(mockPromise);

        store.dispatch(performSearch({ query: 'test' }));
        const state = store.getState().search;

        expect(state.isLoading).toBe(true);
        expect(state.error).toBeNull();
      });
    });

    describe('performQuickSearch', () => {
      it('should handle successful quick search', async () => {
        const mockResponse = {
          data: {
            logs: [{ id: '1', message: 'Quick search result' }],
            totalHits: 1,
            page: 1,
            size: 100,
            searchTimeMs: 50,
          },
        };

        searchAPI.quickSearch.mockResolvedValue(mockResponse);

        await store.dispatch(performQuickSearch({
          query: 'test',
          page: 1,
          size: 100,
        }));

        const state = store.getState().search;

        expect(state.isLoading).toBe(false);
        expect(state.error).toBeNull();
        expect(state.results.logs).toHaveLength(1);
        expect(state.results.searchTimeMs).toBe(50);
      });

      it('should handle quick search error', async () => {
        const mockError = {
          message: 'Network error',
        };

        searchAPI.quickSearch.mockRejectedValue(mockError);

        await store.dispatch(performQuickSearch({
          query: 'test',
          page: 1,
          size: 100,
        }));

        const state = store.getState().search;

        expect(state.isLoading).toBe(false);
        expect(state.error).toBe('Network error');
      });
    });

    describe('getSearchSuggestions', () => {
      it('should handle successful suggestions fetch', async () => {
        const mockResponse = {
          data: ['web-service', 'web-api', 'web-frontend'],
        };

        searchAPI.getFieldSuggestions.mockResolvedValue(mockResponse);

        await store.dispatch(getSearchSuggestions({
          fieldName: 'application',
          prefix: 'web',
          limit: 5,
        }));

        const state = store.getState().search;

        expect(state.suggestions.application).toEqual(['web-service', 'web-api', 'web-frontend']);
      });

      it('should handle suggestions fetch error', async () => {
        const mockError = {
          response: {
            data: 'Field not found',
          },
        };

        searchAPI.getFieldSuggestions.mockRejectedValue(mockError);

        await store.dispatch(getSearchSuggestions({
          fieldName: 'nonexistent',
          prefix: 'test',
        }));

        const state = store.getState().search;

        expect(state.suggestions.nonexistent).toBeUndefined();
      });
    });
  });

  describe('selectors', () => {
    it('should select search results', () => {
      const mockState = {
        search: {
          results: {
            logs: [{ id: '1', message: 'test' }],
            totalHits: 1,
          },
        },
      };

      const { selectSearchResults } = require('../searchSlice');
      const results = selectSearchResults(mockState);

      expect(results.logs).toHaveLength(1);
      expect(results.totalHits).toBe(1);
    });

    it('should select current query', () => {
      const mockState = {
        search: {
          currentQuery: {
            query: 'ERROR',
            page: 2,
            size: 50,
          },
        },
      };

      const { selectCurrentQuery } = require('../searchSlice');
      const query = selectCurrentQuery(mockState);

      expect(query.query).toBe('ERROR');
      expect(query.page).toBe(2);
      expect(query.size).toBe(50);
    });

    it('should select loading state', () => {
      const mockState = {
        search: {
          isLoading: true,
        },
      };

      const { selectIsLoading } = require('../searchSlice');
      const isLoading = selectIsLoading(mockState);

      expect(isLoading).toBe(true);
    });
  });
});
