import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import SearchBar from '../SearchBar/SearchBar';
import searchReducer from '../../store/slices/searchSlice';
import { searchAPI } from '../../services/api';

// Mock the API
jest.mock('../../services/api');

// Mock toast notifications
jest.mock('react-hot-toast', () => ({
  toast: {
    success: jest.fn(),
    error: jest.fn(),
  },
}));

// Create a test store
const createTestStore = (initialState = {}) => {
  return configureStore({
    reducer: {
      search: searchReducer,
    },
    preloadedState: {
      search: {
        currentQuery: {
          query: '',
          page: 1,
          size: 100,
          ...initialState.currentQuery,
        },
        isLoading: false,
        error: null,
        suggestions: {},
        availableFields: ['timestamp', 'level', 'message', 'source', 'host'],
        ...initialState,
      },
    },
  });
};

// Test wrapper component
const TestWrapper = ({ children, store }) => (
  <Provider store={store}>
    {children}
  </Provider>
);

describe('SearchBar Component', () => {
  let store;
  let user;

  beforeEach(() => {
    store = createTestStore();
    user = userEvent.setup();
    jest.clearAllMocks();
  });

  describe('Rendering', () => {
    it('should render search input field', () => {
      render(
        <TestWrapper store={store}>
          <SearchBar />
        </TestWrapper>
      );

      const searchInput = screen.getByPlaceholderText(/search logs/i);
      expect(searchInput).toBeInTheDocument();
      expect(searchInput).toHaveAttribute('type', 'text');
    });

    it('should render search button', () => {
      render(
        <TestWrapper store={store}>
          <SearchBar />
        </TestWrapper>
      );

      const searchButton = screen.getByRole('button', { name: /search/i });
      expect(searchButton).toBeInTheDocument();
      expect(searchButton).not.toBeDisabled();
    });

    it('should render filters button', () => {
      render(
        <TestWrapper store={store}>
          <SearchBar />
        </TestWrapper>
      );

      const filtersButton = screen.getByRole('button', { name: /filters/i });
      expect(filtersButton).toBeInTheDocument();
    });

    it('should display current query value', () => {
      const storeWithQuery = createTestStore({
        currentQuery: { query: 'ERROR' },
      });

      render(
        <TestWrapper store={storeWithQuery}>
          <SearchBar />
        </TestWrapper>
      );

      const searchInput = screen.getByDisplayValue('ERROR');
      expect(searchInput).toBeInTheDocument();
    });
  });

  describe('User Interactions', () => {
    it('should update query when typing in search input', async () => {
      render(
        <TestWrapper store={store}>
          <SearchBar />
        </TestWrapper>
      );

      const searchInput = screen.getByPlaceholderText(/search logs/i);
      
      await user.type(searchInput, 'ERROR');
      
      expect(searchInput).toHaveValue('ERROR');
    });

    it('should trigger search when search button is clicked', async () => {
      const mockSearchResponse = {
        data: {
          logs: [],
          totalHits: 0,
          page: 1,
          size: 100,
        },
      };
      searchAPI.search.mockResolvedValue(mockSearchResponse);

      render(
        <TestWrapper store={store}>
          <SearchBar />
        </TestWrapper>
      );

      const searchInput = screen.getByPlaceholderText(/search logs/i);
      const searchButton = screen.getByRole('button', { name: /search/i });

      await user.type(searchInput, 'ERROR');
      await user.click(searchButton);

      await waitFor(() => {
        expect(searchAPI.search).toHaveBeenCalledWith(
          expect.objectContaining({
            query: 'ERROR',
          })
        );
      });
    });

    it('should trigger search when Enter key is pressed', async () => {
      const mockSearchResponse = {
        data: {
          logs: [],
          totalHits: 0,
          page: 1,
          size: 100,
        },
      };
      searchAPI.search.mockResolvedValue(mockSearchResponse);

      render(
        <TestWrapper store={store}>
          <SearchBar />
        </TestWrapper>
      );

      const searchInput = screen.getByPlaceholderText(/search logs/i);

      await user.type(searchInput, 'ERROR');
      await user.keyboard('{Enter}');

      await waitFor(() => {
        expect(searchAPI.search).toHaveBeenCalledWith(
          expect.objectContaining({
            query: 'ERROR',
          })
        );
      });
    });

    it('should show loading state during search', async () => {
      // Mock a delayed response
      const mockSearchPromise = new Promise((resolve) => {
        setTimeout(() => resolve({
          data: { logs: [], totalHits: 0, page: 1, size: 100 }
        }), 100);
      });
      searchAPI.search.mockReturnValue(mockSearchPromise);

      render(
        <TestWrapper store={store}>
          <SearchBar />
        </TestWrapper>
      );

      const searchInput = screen.getByPlaceholderText(/search logs/i);
      const searchButton = screen.getByRole('button', { name: /search/i });

      await user.type(searchInput, 'ERROR');
      await user.click(searchButton);

      // Check for loading state
      expect(searchButton).toBeDisabled();
      expect(screen.getByText(/searching/i)).toBeInTheDocument();

      // Wait for search to complete
      await waitFor(() => {
        expect(searchButton).not.toBeDisabled();
      });
    });

    it('should open filters panel when filters button is clicked', async () => {
      render(
        <TestWrapper store={store}>
          <SearchBar />
        </TestWrapper>
      );

      const filtersButton = screen.getByRole('button', { name: /filters/i });
      await user.click(filtersButton);

      expect(screen.getByText(/search filters/i)).toBeInTheDocument();
    });
  });

  describe('Search Suggestions', () => {
    it('should show suggestions when typing', async () => {
      const mockSuggestions = {
        data: ['ERROR', 'ERROR_RATE', 'ERROR_COUNT'],
      };
      searchAPI.getFieldSuggestions.mockResolvedValue(mockSuggestions);

      render(
        <TestWrapper store={store}>
          <SearchBar />
        </TestWrapper>
      );

      const searchInput = screen.getByPlaceholderText(/search logs/i);
      
      await user.type(searchInput, 'ERR');

      await waitFor(() => {
        expect(screen.getByText('ERROR')).toBeInTheDocument();
        expect(screen.getByText('ERROR_RATE')).toBeInTheDocument();
        expect(screen.getByText('ERROR_COUNT')).toBeInTheDocument();
      });
    });

    it('should select suggestion when clicked', async () => {
      const mockSuggestions = {
        data: ['ERROR', 'ERROR_RATE'],
      };
      searchAPI.getFieldSuggestions.mockResolvedValue(mockSuggestions);

      render(
        <TestWrapper store={store}>
          <SearchBar />
        </TestWrapper>
      );

      const searchInput = screen.getByPlaceholderText(/search logs/i);
      
      await user.type(searchInput, 'ERR');

      await waitFor(() => {
        expect(screen.getByText('ERROR')).toBeInTheDocument();
      });

      await user.click(screen.getByText('ERROR'));

      expect(searchInput).toHaveValue('ERROR');
    });

    it('should hide suggestions when clicking outside', async () => {
      const mockSuggestions = {
        data: ['ERROR', 'ERROR_RATE'],
      };
      searchAPI.getFieldSuggestions.mockResolvedValue(mockSuggestions);

      render(
        <TestWrapper store={store}>
          <SearchBar />
        </TestWrapper>
      );

      const searchInput = screen.getByPlaceholderText(/search logs/i);
      
      await user.type(searchInput, 'ERR');

      await waitFor(() => {
        expect(screen.getByText('ERROR')).toBeInTheDocument();
      });

      // Click outside
      await user.click(document.body);

      await waitFor(() => {
        expect(screen.queryByText('ERROR')).not.toBeInTheDocument();
      });
    });
  });

  describe('Search Validation', () => {
    it('should prevent search with empty query', async () => {
      render(
        <TestWrapper store={store}>
          <SearchBar />
        </TestWrapper>
      );

      const searchButton = screen.getByRole('button', { name: /search/i });
      await user.click(searchButton);

      expect(searchAPI.search).not.toHaveBeenCalled();
      expect(screen.getByText(/please enter a search query/i)).toBeInTheDocument();
    });

    it('should show validation error for invalid query syntax', async () => {
      const mockValidationResponse = {
        data: {
          valid: false,
          error: 'Invalid query syntax',
        },
      };
      searchAPI.validateQuery.mockResolvedValue(mockValidationResponse);

      render(
        <TestWrapper store={store}>
          <SearchBar />
        </TestWrapper>
      );

      const searchInput = screen.getByPlaceholderText(/search logs/i);
      const searchButton = screen.getByRole('button', { name: /search/i });

      await user.type(searchInput, 'invalid[[[query');
      await user.click(searchButton);

      await waitFor(() => {
        expect(screen.getByText(/invalid query syntax/i)).toBeInTheDocument();
      });
    });
  });

  describe('Keyboard Navigation', () => {
    it('should navigate suggestions with arrow keys', async () => {
      const mockSuggestions = {
        data: ['ERROR', 'ERROR_RATE', 'ERROR_COUNT'],
      };
      searchAPI.getFieldSuggestions.mockResolvedValue(mockSuggestions);

      render(
        <TestWrapper store={store}>
          <SearchBar />
        </TestWrapper>
      );

      const searchInput = screen.getByPlaceholderText(/search logs/i);
      
      await user.type(searchInput, 'ERR');

      await waitFor(() => {
        expect(screen.getByText('ERROR')).toBeInTheDocument();
      });

      // Navigate down
      await user.keyboard('{ArrowDown}');
      expect(screen.getByText('ERROR')).toHaveClass('highlighted');

      // Navigate down again
      await user.keyboard('{ArrowDown}');
      expect(screen.getByText('ERROR_RATE')).toHaveClass('highlighted');

      // Navigate up
      await user.keyboard('{ArrowUp}');
      expect(screen.getByText('ERROR')).toHaveClass('highlighted');
    });

    it('should select highlighted suggestion with Enter key', async () => {
      const mockSuggestions = {
        data: ['ERROR', 'ERROR_RATE'],
      };
      searchAPI.getFieldSuggestions.mockResolvedValue(mockSuggestions);

      render(
        <TestWrapper store={store}>
          <SearchBar />
        </TestWrapper>
      );

      const searchInput = screen.getByPlaceholderText(/search logs/i);
      
      await user.type(searchInput, 'ERR');

      await waitFor(() => {
        expect(screen.getByText('ERROR')).toBeInTheDocument();
      });

      // Navigate to first suggestion and select it
      await user.keyboard('{ArrowDown}');
      await user.keyboard('{Enter}');

      expect(searchInput).toHaveValue('ERROR');
    });
  });

  describe('Accessibility', () => {
    it('should have proper ARIA labels', () => {
      render(
        <TestWrapper store={store}>
          <SearchBar />
        </TestWrapper>
      );

      const searchInput = screen.getByLabelText(/search query/i);
      const searchButton = screen.getByRole('button', { name: /search/i });

      expect(searchInput).toHaveAttribute('aria-label');
      expect(searchButton).toHaveAttribute('aria-label');
    });

    it('should announce search results to screen readers', async () => {
      const mockSearchResponse = {
        data: {
          logs: [{ id: '1', message: 'test' }],
          totalHits: 1,
          page: 1,
          size: 100,
        },
      };
      searchAPI.search.mockResolvedValue(mockSearchResponse);

      render(
        <TestWrapper store={store}>
          <SearchBar />
        </TestWrapper>
      );

      const searchInput = screen.getByPlaceholderText(/search logs/i);
      const searchButton = screen.getByRole('button', { name: /search/i });

      await user.type(searchInput, 'ERROR');
      await user.click(searchButton);

      await waitFor(() => {
        const announcement = screen.getByRole('status');
        expect(announcement).toHaveTextContent(/1 result found/i);
      });
    });

    it('should support keyboard navigation', async () => {
      render(
        <TestWrapper store={store}>
          <SearchBar />
        </TestWrapper>
      );

      const searchInput = screen.getByPlaceholderText(/search logs/i);
      const searchButton = screen.getByRole('button', { name: /search/i });
      const filtersButton = screen.getByRole('button', { name: /filters/i });

      // Tab navigation
      searchInput.focus();
      expect(searchInput).toHaveFocus();

      await user.keyboard('{Tab}');
      expect(searchButton).toHaveFocus();

      await user.keyboard('{Tab}');
      expect(filtersButton).toHaveFocus();
    });
  });
});
