import React, { useState, useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { useSearchParams } from 'react-router-dom';
import {
  MagnifyingGlassIcon,
  AdjustmentsHorizontalIcon,
  ClockIcon,
  DocumentTextIcon,
  ExclamationTriangleIcon,
} from '@heroicons/react/24/outline';
import {
  performSearch,
  performQuickSearch,
  updateQuery,
  setFilters,
  selectSearchResults,
  selectCurrentQuery,
  selectIsLoading,
  selectError,
} from '../store/slices/searchSlice';
import LoadingSpinner, { TableSkeleton } from '../components/LoadingSpinner/LoadingSpinner';

/**
 * Search page for log analysis with Splunk-like functionality
 */
const Search = () => {
  const dispatch = useDispatch();
  const [searchParams, setSearchParams] = useSearchParams();
  const searchResults = useSelector(selectSearchResults);
  const currentQuery = useSelector(selectCurrentQuery);
  const isLoading = useSelector(selectIsLoading);
  const error = useSelector(selectError);

  const [searchInput, setSearchInput] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [selectedLogs, setSelectedLogs] = useState([]);

  // Initialize search from URL params
  useEffect(() => {
    const queryParam = searchParams.get('q');
    if (queryParam) {
      setSearchInput(queryParam);
      dispatch(performQuickSearch({ query: queryParam }));
    }
  }, [searchParams, dispatch]);

  // Handle search
  const handleSearch = (e) => {
    e.preventDefault();
    if (searchInput.trim()) {
      setSearchParams({ q: searchInput.trim() });
      dispatch(performQuickSearch({ query: searchInput.trim() }));
    }
  };

  // Handle filter changes
  const handleFilterChange = (filterType, value) => {
    dispatch(setFilters({ [filterType]: value }));
  };

  // Format timestamp
  const formatTimestamp = (timestamp) => {
    return new Date(timestamp).toLocaleString();
  };

  // Get log level color
  const getLogLevelColor = (level) => {
    switch (level?.toUpperCase()) {
      case 'ERROR':
        return 'text-red-600 bg-red-100 dark:bg-red-900/20';
      case 'WARN':
      case 'WARNING':
        return 'text-yellow-600 bg-yellow-100 dark:bg-yellow-900/20';
      case 'INFO':
        return 'text-blue-600 bg-blue-100 dark:bg-blue-900/20';
      case 'DEBUG':
        return 'text-gray-600 bg-gray-100 dark:bg-gray-900/20';
      default:
        return 'text-gray-600 bg-gray-100 dark:bg-gray-900/20';
    }
  };

  // Handle log selection
  const handleLogSelection = (logId) => {
    setSelectedLogs(prev => 
      prev.includes(logId) 
        ? prev.filter(id => id !== logId)
        : [...prev, logId]
    );
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Search Logs
          </h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Search and analyze log entries with advanced filtering
          </p>
        </div>
      </div>

      {/* Search Form */}
      <div className="card">
        <div className="card-body">
          <form onSubmit={handleSearch} className="space-y-4">
            {/* Main Search Input */}
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <MagnifyingGlassIcon className="h-5 w-5 text-gray-400" />
              </div>
              <input
                type="text"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder="Enter search query (e.g., error OR warning, source:web-server, level:ERROR)"
                className="block w-full pl-10 pr-3 py-3 border border-gray-300 rounded-lg leading-5 bg-white dark:bg-gray-700 dark:border-gray-600 placeholder-gray-500 dark:placeholder-gray-400 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              />
            </div>

            {/* Search Actions */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between space-y-3 sm:space-y-0">
              <div className="flex items-center space-x-3">
                <button
                  type="submit"
                  disabled={isLoading || !searchInput.trim()}
                  className="btn-primary disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isLoading ? (
                    <>
                      <LoadingSpinner size="small" color="white" />
                      <span className="ml-2">Searching...</span>
                    </>
                  ) : (
                    <>
                      <MagnifyingGlassIcon className="h-4 w-4 mr-2" />
                      Search
                    </>
                  )}
                </button>
                
                <button
                  type="button"
                  onClick={() => setShowFilters(!showFilters)}
                  className="btn-outline"
                >
                  <AdjustmentsHorizontalIcon className="h-4 w-4 mr-2" />
                  Filters
                </button>
              </div>

              {/* Search Stats */}
              {searchResults?.totalHits > 0 && (
                <div className="text-sm text-gray-500 dark:text-gray-400">
                  Found {searchResults.totalHits.toLocaleString()} results in {searchResults.searchTimeMs}ms
                </div>
              )}
            </div>

            {/* Advanced Filters */}
            {showFilters && (
              <div className="border-t border-gray-200 dark:border-gray-700 pt-4 mt-4">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div>
                    <label className="form-label">Log Level</label>
                    <select 
                      className="form-input"
                      onChange={(e) => handleFilterChange('level', e.target.value)}
                    >
                      <option value="">All Levels</option>
                      <option value="ERROR">Error</option>
                      <option value="WARN">Warning</option>
                      <option value="INFO">Info</option>
                      <option value="DEBUG">Debug</option>
                    </select>
                  </div>
                  
                  <div>
                    <label className="form-label">Source</label>
                    <input
                      type="text"
                      placeholder="e.g., web-server, api-gateway"
                      className="form-input"
                      onChange={(e) => handleFilterChange('source', e.target.value)}
                    />
                  </div>
                  
                  <div>
                    <label className="form-label">Time Range</label>
                    <select 
                      className="form-input"
                      onChange={(e) => handleFilterChange('timeRange', e.target.value)}
                    >
                      <option value="">All Time</option>
                      <option value="1h">Last Hour</option>
                      <option value="24h">Last 24 Hours</option>
                      <option value="7d">Last 7 Days</option>
                      <option value="30d">Last 30 Days</option>
                    </select>
                  </div>
                </div>
              </div>
            )}
          </form>
        </div>
      </div>

      {/* Error Display */}
      {error && (
        <div className="alert-error">
          <ExclamationTriangleIcon className="h-5 w-5 mr-2" />
          <span>Search failed: {error}</span>
        </div>
      )}

      {/* Search Results */}
      <div className="card">
        <div className="card-header">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-medium text-gray-900 dark:text-white">
              Search Results
            </h3>
            {selectedLogs.length > 0 && (
              <div className="text-sm text-gray-500 dark:text-gray-400">
                {selectedLogs.length} selected
              </div>
            )}
          </div>
        </div>
        
        <div className="card-body p-0">
          {isLoading ? (
            <TableSkeleton rows={10} columns={4} />
          ) : searchResults?.logs?.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="table">
                <thead className="table-header">
                  <tr>
                    <th className="table-header-cell">
                      <input
                        type="checkbox"
                        className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                        onChange={(e) => {
                          if (e.target.checked) {
                            setSelectedLogs(searchResults?.logs?.map(log => log.id) || []);
                          } else {
                            setSelectedLogs([]);
                          }
                        }}
                      />
                    </th>
                    <th className="table-header-cell">Timestamp</th>
                    <th className="table-header-cell">Level</th>
                    <th className="table-header-cell">Source</th>
                    <th className="table-header-cell">Message</th>
                  </tr>
                </thead>
                <tbody className="table-body">
                  {searchResults?.logs?.map((log) => (
                    <tr key={log.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                      <td className="table-cell">
                        <input
                          type="checkbox"
                          checked={selectedLogs.includes(log.id)}
                          onChange={() => handleLogSelection(log.id)}
                          className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                        />
                      </td>
                      <td className="table-cell">
                        <div className="flex items-center text-sm">
                          <ClockIcon className="h-4 w-4 text-gray-400 mr-2" />
                          {formatTimestamp(log.timestamp)}
                        </div>
                      </td>
                      <td className="table-cell">
                        <span className={`badge ${getLogLevelColor(log.level)}`}>
                          {log.level}
                        </span>
                      </td>
                      <td className="table-cell">
                        <span className="text-sm font-medium text-gray-900 dark:text-white">
                          {log.source}
                        </span>
                      </td>
                      <td className="table-cell">
                        <div className="max-w-md">
                          <p className="text-sm text-gray-900 dark:text-white truncate">
                            {log.message}
                          </p>
                          {log.stackTrace && (
                            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                              Stack trace available
                            </p>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : searchInput ? (
            <div className="text-center py-12">
              <DocumentTextIcon className="mx-auto h-12 w-12 text-gray-400" />
              <h3 className="mt-2 text-sm font-medium text-gray-900 dark:text-white">
                No logs found
              </h3>
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                Try adjusting your search query or filters.
              </p>
            </div>
          ) : (
            <div className="text-center py-12">
              <MagnifyingGlassIcon className="mx-auto h-12 w-12 text-gray-400" />
              <h3 className="mt-2 text-sm font-medium text-gray-900 dark:text-white">
                Start searching
              </h3>
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                Enter a search query to find log entries.
              </p>
            </div>
          )}
        </div>

        {/* Pagination */}
        {searchResults?.totalPages > 1 && (
          <div className="card-footer">
            <div className="flex items-center justify-between">
              <div className="text-sm text-gray-500 dark:text-gray-400">
                Showing {((searchResults?.page - 1) * searchResults?.size) + 1} to{' '}
                {Math.min(searchResults?.page * searchResults?.size, searchResults?.totalHits)} of{' '}
                {searchResults?.totalHits} results
              </div>
              <div className="flex items-center space-x-2">
                <button
                  disabled={searchResults?.page <= 1}
                  className="btn-outline disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Previous
                </button>
                <span className="text-sm text-gray-700 dark:text-gray-300">
                  Page {searchResults?.page} of {searchResults?.totalPages}
                </span>
                <button
                  disabled={searchResults?.page >= searchResults?.totalPages}
                  className="btn-outline disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Next
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Search;
