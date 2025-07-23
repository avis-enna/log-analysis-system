import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { useLocation } from 'react-router-dom';
import Header from './Header';
import Sidebar from './Sidebar';
import Footer from './Footer';
import { setCurrentPage, setViewport } from '../../store/slices/uiSlice';
import { selectSidebarOpen, selectSidebarCollapsed, selectTheme } from '../../store/slices/uiSlice';

/**
 * Main layout component that wraps all pages
 * Provides consistent header, sidebar, and footer
 */
const Layout = ({ children }) => {
  const dispatch = useDispatch();
  const location = useLocation();
  const sidebarOpen = useSelector(selectSidebarOpen);
  const sidebarCollapsed = useSelector(selectSidebarCollapsed);
  const theme = useSelector(selectTheme);

  // Update current page based on route
  useEffect(() => {
    const path = location.pathname.replace('/', '') || 'dashboard';
    dispatch(setCurrentPage(path));
  }, [location.pathname, dispatch]);

  // Handle viewport changes
  useEffect(() => {
    const handleResize = () => {
      dispatch(setViewport({
        width: window.innerWidth,
        height: window.innerHeight,
      }));
    };

    // Set initial viewport
    handleResize();

    // Add event listener
    window.addEventListener('resize', handleResize);

    // Cleanup
    return () => window.removeEventListener('resize', handleResize);
  }, [dispatch]);

  // Apply theme to document
  useEffect(() => {
    const root = document.documentElement;
    
    if (theme === 'dark') {
      root.classList.add('dark');
    } else if (theme === 'light') {
      root.classList.remove('dark');
    } else if (theme === 'auto') {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      if (prefersDark) {
        root.classList.add('dark');
      } else {
        root.classList.remove('dark');
      }
    }
  }, [theme]);

  // Calculate main content styles based on sidebar state
  const getMainContentStyles = () => {
    let marginLeft = 0;
    
    if (sidebarOpen) {
      marginLeft = sidebarCollapsed ? 64 : 256; // 64px collapsed, 256px expanded
    }

    return {
      marginLeft: `${marginLeft}px`,
      transition: 'margin-left 0.3s ease-in-out',
    };
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 transition-colors duration-200">
      {/* Header */}
      <Header />
      
      {/* Sidebar */}
      <Sidebar />
      
      {/* Main Content Area */}
      <div 
        className="flex flex-col min-h-screen pt-16" // pt-16 for header height
        style={getMainContentStyles()}
      >
        {/* Main Content */}
        <main className="flex-1 p-6">
          <div className="max-w-7xl mx-auto">
            {children}
          </div>
        </main>
        
        {/* Footer */}
        <Footer />
      </div>
      
      {/* Sidebar Overlay for Mobile */}
      {sidebarOpen && (
        <div 
          className="fixed inset-0 bg-black bg-opacity-50 z-40 lg:hidden"
          onClick={() => dispatch({ type: 'ui/setSidebarOpen', payload: false })}
        />
      )}
    </div>
  );
};

export default Layout;
