import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { NavLink, useLocation } from 'react-router-dom';
import {
  HomeIcon,
  MagnifyingGlassIcon,
  ChartBarIcon,
  ExclamationTriangleIcon,
  Cog6ToothIcon,
  DocumentTextIcon,
  CloudArrowUpIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  UsersIcon,
  AcademicCapIcon,
} from '@heroicons/react/24/outline';
import {
  HomeIcon as HomeIconSolid,
  MagnifyingGlassIcon as MagnifyingGlassIconSolid,
  ChartBarIcon as ChartBarIconSolid,
  ExclamationTriangleIcon as ExclamationTriangleIconSolid,
  Cog6ToothIcon as Cog6ToothIconSolid,
  UsersIcon as UsersIconSolid,
} from '@heroicons/react/24/solid';
import { 
  selectSidebarOpen, 
  selectSidebarCollapsed, 
  setSidebarOpen,
  toggleSidebarCollapsed 
} from '../../store/slices/uiSlice';
import { selectOpenAlerts } from '../../store/slices/alertsSlice';
import { selectIsAdmin } from '../../store/slices/authSlice';

/**
 * Sidebar navigation component
 */
const Sidebar = () => {
  const dispatch = useDispatch();
  const location = useLocation();
  const sidebarOpen = useSelector(selectSidebarOpen);
  const sidebarCollapsed = useSelector(selectSidebarCollapsed);
  const openAlerts = useSelector(selectOpenAlerts);
  const isAdmin = useSelector(selectIsAdmin);

  // Navigation items
  const navigationItems = [
    {
      name: 'Dashboard',
      href: '/dashboard',
      icon: HomeIcon,
      iconSolid: HomeIconSolid,
      description: 'System overview and metrics',
    },
    {
      name: 'Search',
      href: '/search',
      icon: MagnifyingGlassIcon,
      iconSolid: MagnifyingGlassIconSolid,
      description: 'Search and analyze logs',
    },
    {
      name: 'Analytics',
      href: '/analytics',
      icon: ChartBarIcon,
      iconSolid: ChartBarIconSolid,
      description: 'Advanced analytics and reports',
    },
    {
      name: 'Alerts',
      href: '/alerts',
      icon: ExclamationTriangleIcon,
      iconSolid: ExclamationTriangleIconSolid,
      description: 'Alert management',
      badge: openAlerts.length > 0 ? openAlerts.length : null,
    },
    {
      name: 'Settings',
      href: '/settings',
      icon: Cog6ToothIcon,
      iconSolid: Cog6ToothIconSolid,
      description: 'System configuration',
    },
    ...(isAdmin ? [{
      name: 'Role Management',
      href: '/roles',
      icon: UsersIcon,
      iconSolid: UsersIconSolid,
      description: 'Manage user roles and permissions',
    }] : []),
  ];

  // Additional tools
  const toolItems = [
    {
      name: 'Upload Logs',
      href: '/upload',
      icon: CloudArrowUpIcon,
      description: 'Upload log files',
    },
    {
      name: 'Demo Accounts',
      href: '/demo',
      icon: AcademicCapIcon,
      description: 'Demo user accounts and scenarios',
    },
    {
      name: 'Documentation',
      href: '/docs',
      icon: DocumentTextIcon,
      description: 'System documentation',
    },
  ];

  // Check if current path matches navigation item
  const isActive = (href) => {
    return location.pathname === href || 
           (href !== '/dashboard' && location.pathname.startsWith(href));
  };

  // Handle sidebar collapse toggle
  const handleCollapseToggle = () => {
    dispatch(toggleSidebarCollapsed());
  };

  // Handle mobile sidebar close
  const handleMobileClose = () => {
    if (window.innerWidth < 1024) {
      dispatch(setSidebarOpen(false));
    }
  };

  // Sidebar width classes
  const sidebarWidth = sidebarCollapsed ? 'w-16' : 'w-64';
  const sidebarTransform = sidebarOpen ? 'translate-x-0' : '-translate-x-full';

  return (
    <>
      {/* Sidebar */}
      <div
        className={`fixed top-16 left-0 bottom-0 z-40 ${sidebarWidth} ${sidebarTransform} lg:translate-x-0 bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 transition-all duration-300 ease-in-out`}
      >
        <div className="flex flex-col h-full">
          {/* Sidebar Header */}
          <div className="flex items-center justify-between p-4 border-b border-gray-200 dark:border-gray-700">
            {!sidebarCollapsed && (
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                Navigation
              </h2>
            )}
            <button
              onClick={handleCollapseToggle}
              className="p-1.5 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100 dark:text-gray-400 dark:hover:text-gray-200 dark:hover:bg-gray-700 transition-colors"
              aria-label={sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            >
              {sidebarCollapsed ? (
                <ChevronRightIcon className="h-5 w-5" />
              ) : (
                <ChevronLeftIcon className="h-5 w-5" />
              )}
            </button>
          </div>

          {/* Navigation */}
          <nav className="flex-1 p-4 space-y-2 overflow-y-auto">
            {/* Main Navigation */}
            <div className="space-y-1">
              {!sidebarCollapsed && (
                <h3 className="px-3 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-3">
                  Main
                </h3>
              )}
              {navigationItems.map((item) => {
                const Icon = isActive(item.href) ? item.iconSolid : item.icon;
                return (
                  <NavLink
                    key={item.name}
                    to={item.href}
                    onClick={handleMobileClose}
                    className={({ isActive }) =>
                      `group flex items-center px-3 py-2 text-sm font-medium rounded-lg transition-colors duration-200 ${
                        isActive
                          ? 'bg-primary-100 text-primary-700 dark:bg-primary-900 dark:text-primary-200'
                          : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-gray-200'
                      }`
                    }
                    title={sidebarCollapsed ? item.name : ''}
                  >
                    <Icon className="h-5 w-5 flex-shrink-0" />
                    {!sidebarCollapsed && (
                      <>
                        <span className="ml-3 flex-1">{item.name}</span>
                        {item.badge && (
                          <span className="ml-2 inline-flex items-center justify-center px-2 py-1 text-xs font-bold leading-none text-white bg-red-500 rounded-full">
                            {item.badge}
                          </span>
                        )}
                      </>
                    )}
                    {sidebarCollapsed && item.badge && (
                      <span className="absolute left-8 top-1 inline-flex items-center justify-center px-1.5 py-0.5 text-xs font-bold leading-none text-white bg-red-500 rounded-full">
                        {item.badge}
                      </span>
                    )}
                  </NavLink>
                );
              })}
            </div>

            {/* Tools Section */}
            <div className="pt-6 space-y-1">
              {!sidebarCollapsed && (
                <h3 className="px-3 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-3">
                  Tools
                </h3>
              )}
              {toolItems.map((item) => (
                <NavLink
                  key={item.name}
                  to={item.href}
                  onClick={handleMobileClose}
                  className={({ isActive }) =>
                    `group flex items-center px-3 py-2 text-sm font-medium rounded-lg transition-colors duration-200 ${
                      isActive
                        ? 'bg-primary-100 text-primary-700 dark:bg-primary-900 dark:text-primary-200'
                        : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-gray-200'
                    }`
                  }
                  title={sidebarCollapsed ? item.name : ''}
                >
                  <item.icon className="h-5 w-5 flex-shrink-0" />
                  {!sidebarCollapsed && (
                    <span className="ml-3">{item.name}</span>
                  )}
                </NavLink>
              ))}
            </div>
          </nav>

          {/* Sidebar Footer */}
          {!sidebarCollapsed && (
            <div className="p-4 border-t border-gray-200 dark:border-gray-700">
              <div className="text-xs text-gray-500 dark:text-gray-400">
                <p>Log Analysis System</p>
                <p>Version 1.0.0</p>
              </div>
            </div>
          )}
        </div>
      </div>
    </>
  );
};

export default Sidebar;
