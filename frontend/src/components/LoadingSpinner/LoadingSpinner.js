import React from 'react';

/**
 * Loading spinner component with various sizes and styles
 */
const LoadingSpinner = ({ 
  size = 'medium', 
  color = 'primary', 
  text = '', 
  fullScreen = false,
  overlay = false 
}) => {
  // Size classes
  const sizeClasses = {
    small: 'h-4 w-4',
    medium: 'h-8 w-8',
    large: 'h-12 w-12',
    xlarge: 'h-16 w-16',
  };

  // Color classes
  const colorClasses = {
    primary: 'border-primary-600',
    secondary: 'border-gray-600',
    white: 'border-white',
    success: 'border-green-600',
    warning: 'border-yellow-600',
    error: 'border-red-600',
  };

  // Get spinner classes
  const spinnerClasses = `
    ${sizeClasses[size]} 
    ${colorClasses[color]} 
    border-2 border-t-transparent rounded-full animate-spin
  `;

  // Spinner element
  const spinner = (
    <div className="flex flex-col items-center justify-center space-y-3">
      <div className={spinnerClasses}></div>
      {text && (
        <p className="text-sm text-gray-600 dark:text-gray-400 animate-pulse">
          {text}
        </p>
      )}
    </div>
  );

  // Full screen loading
  if (fullScreen) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-white dark:bg-gray-900">
        {spinner}
      </div>
    );
  }

  // Overlay loading
  if (overlay) {
    return (
      <div className="absolute inset-0 z-40 flex items-center justify-center bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm">
        {spinner}
      </div>
    );
  }

  // Regular loading
  return spinner;
};

/**
 * Inline loading spinner for buttons and small spaces
 */
export const InlineSpinner = ({ size = 'small', color = 'white' }) => {
  const sizeClasses = {
    small: 'h-3 w-3',
    medium: 'h-4 w-4',
  };

  const colorClasses = {
    primary: 'border-primary-600',
    white: 'border-white',
    gray: 'border-gray-600',
  };

  return (
    <div 
      className={`
        ${sizeClasses[size]} 
        ${colorClasses[color]} 
        border border-t-transparent rounded-full animate-spin
      `}
    />
  );
};

/**
 * Skeleton loading component for content placeholders
 */
export const SkeletonLoader = ({ 
  lines = 3, 
  height = 'h-4', 
  className = '' 
}) => {
  return (
    <div className={`animate-pulse space-y-3 ${className}`}>
      {Array.from({ length: lines }).map((_, index) => (
        <div
          key={index}
          className={`bg-gray-200 dark:bg-gray-700 rounded ${height} ${
            index === lines - 1 ? 'w-3/4' : 'w-full'
          }`}
        />
      ))}
    </div>
  );
};

/**
 * Card skeleton loader
 */
export const CardSkeleton = ({ showHeader = true, lines = 3 }) => {
  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow-soft border border-gray-200 dark:border-gray-700 p-6 animate-pulse">
      {showHeader && (
        <div className="mb-4">
          <div className="h-6 bg-gray-200 dark:bg-gray-700 rounded w-1/3 mb-2" />
          <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-1/2" />
        </div>
      )}
      <div className="space-y-3">
        {Array.from({ length: lines }).map((_, index) => (
          <div
            key={index}
            className={`h-4 bg-gray-200 dark:bg-gray-700 rounded ${
              index === lines - 1 ? 'w-3/4' : 'w-full'
            }`}
          />
        ))}
      </div>
    </div>
  );
};

/**
 * Table skeleton loader
 */
export const TableSkeleton = ({ rows = 5, columns = 4 }) => {
  return (
    <div className="animate-pulse">
      {/* Table Header */}
      <div className="bg-gray-50 dark:bg-gray-800 p-4 border-b border-gray-200 dark:border-gray-700">
        <div className="grid gap-4" style={{ gridTemplateColumns: `repeat(${columns}, 1fr)` }}>
          {Array.from({ length: columns }).map((_, index) => (
            <div key={index} className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-3/4" />
          ))}
        </div>
      </div>
      
      {/* Table Rows */}
      {Array.from({ length: rows }).map((_, rowIndex) => (
        <div key={rowIndex} className="p-4 border-b border-gray-200 dark:border-gray-700">
          <div className="grid gap-4" style={{ gridTemplateColumns: `repeat(${columns}, 1fr)` }}>
            {Array.from({ length: columns }).map((_, colIndex) => (
              <div 
                key={colIndex} 
                className={`h-4 bg-gray-200 dark:bg-gray-700 rounded ${
                  colIndex === 0 ? 'w-full' : 'w-2/3'
                }`} 
              />
            ))}
          </div>
        </div>
      ))}
    </div>
  );
};

/**
 * Chart skeleton loader
 */
export const ChartSkeleton = ({ height = 'h-64' }) => {
  return (
    <div className={`bg-white dark:bg-gray-800 rounded-lg shadow-soft border border-gray-200 dark:border-gray-700 p-6 animate-pulse`}>
      <div className="mb-4">
        <div className="h-6 bg-gray-200 dark:bg-gray-700 rounded w-1/4 mb-2" />
        <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-1/3" />
      </div>
      <div className={`bg-gray-200 dark:bg-gray-700 rounded ${height}`} />
    </div>
  );
};

/**
 * Loading dots animation
 */
export const LoadingDots = ({ size = 'medium', color = 'primary' }) => {
  const sizeClasses = {
    small: 'w-1 h-1',
    medium: 'w-2 h-2',
    large: 'w-3 h-3',
  };

  const colorClasses = {
    primary: 'bg-primary-600',
    secondary: 'bg-gray-600',
    white: 'bg-white',
  };

  return (
    <div className="flex space-x-1">
      {[0, 1, 2].map((index) => (
        <div
          key={index}
          className={`
            ${sizeClasses[size]} 
            ${colorClasses[color]} 
            rounded-full animate-bounce
          `}
          style={{
            animationDelay: `${index * 0.1}s`,
            animationDuration: '0.6s',
          }}
        />
      ))}
    </div>
  );
};

export default LoadingSpinner;
