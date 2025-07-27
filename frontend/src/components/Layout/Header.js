import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { 
  Bars3Icon, 
  MagnifyingGlassIcon, 
  BellIcon, 
  UserCircleIcon,
  SunIcon,
  MoonIcon,
  ComputerDesktopIcon,
  Cog6ToothIcon,
  ArrowRightOnRectangleIcon
} from '@heroicons/react/24/outline';
import { 
  toggleSidebar, 
  toggleTheme, 
  openModal,
  selectTheme,
  selectNotifications 
} from '../../store/slices/uiSlice';
import { selectOpenAlerts } from '../../store/slices/alertsSlice';
import { logoutUser, selectUser } from '../../store/slices/authSlice';

/**
 * Header component with navigation, search, and user controls
 */
const Header = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const theme = useSelector(selectTheme);
  const notifications = useSelector(selectNotifications);
  const openAlerts = useSelector(selectOpenAlerts);
  const user = useSelector(selectUser);

  // Get theme icon
  const getThemeIcon = () => {
    switch (theme) {
      case 'light':
        return <SunIcon className="h-5 w-5" />;
      case 'dark':
        return <MoonIcon className="h-5 w-5" />;
      case 'auto':
        return <ComputerDesktopIcon className="h-5 w-5" />;
      default:
        return <SunIcon className="h-5 w-5" />;
    }
  };

  // Handle search
  const handleSearch = (e) => {
    if (e.key === 'Enter' && e.target.value.trim()) {
      navigate(`/search?q=${encodeURIComponent(e.target.value.trim())}`);
    }
  };

  // Handle notifications click
  const handleNotificationsClick = () => {
    navigate('/alerts');
  };

  // Handle settings click
  const handleSettingsClick = () => {
    navigate('/settings');
  };

  // Handle user menu click
  const handleUserMenuClick = () => {
    dispatch(openModal({
      modalName: 'confirmDialog',
      props: {
        title: 'Logout',
        message: 'Are you sure you want to logout?',
        type: 'warning',
        confirmText: 'Logout',
        cancelText: 'Cancel',
        onConfirm: () => {
          dispatch(logoutUser());
          dispatch({ type: 'ui/closeModal', payload: 'confirmDialog' });
          navigate('/login');
        },
        onCancel: () => dispatch({ type: 'ui/closeModal', payload: 'confirmDialog' }),
      }
    }));
  };

  return (
    <header className="fixed top-0 left-0 right-0 z-50 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 shadow-sm">
      <div className="flex items-center justify-between h-16 px-4">
        {/* Left Section */}
        <div className="flex items-center space-x-4">
          {/* Menu Toggle */}
          <button
            onClick={() => dispatch(toggleSidebar())}
            className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100 dark:text-gray-400 dark:hover:text-gray-200 dark:hover:bg-gray-700 transition-colors"
            aria-label="Toggle sidebar"
          >
            <Bars3Icon className="h-6 w-6" />
          </button>

          {/* Logo and Title */}
          <div className="flex items-center space-x-3">
            <div className="flex items-center justify-center w-8 h-8 bg-primary-600 rounded-lg">
              <span className="text-white font-bold text-sm">LA</span>
            </div>
            <div className="hidden sm:block">
              <h1 className="text-xl font-semibold text-gray-900 dark:text-white">
                Log Analysis System
              </h1>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                Enterprise Edition
              </p>
            </div>
          </div>
        </div>

        {/* Center Section - Search */}
        <div className="flex-1 max-w-2xl mx-4">
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <MagnifyingGlassIcon className="h-5 w-5 text-gray-400" />
            </div>
            <input
              type="text"
              placeholder="Search logs, alerts, or enter Splunk-like query..."
              className="block w-full pl-10 pr-3 py-2 border border-gray-300 rounded-lg leading-5 bg-white dark:bg-gray-700 dark:border-gray-600 placeholder-gray-500 dark:placeholder-gray-400 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500 sm:text-sm"
              onKeyPress={handleSearch}
            />
          </div>
        </div>

        {/* Right Section */}
        <div className="flex items-center space-x-2">
          {/* Theme Toggle */}
          <button
            onClick={() => dispatch(toggleTheme())}
            className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100 dark:text-gray-400 dark:hover:text-gray-200 dark:hover:bg-gray-700 transition-colors"
            aria-label="Toggle theme"
          >
            {getThemeIcon()}
          </button>

          {/* Notifications */}
          <button
            onClick={handleNotificationsClick}
            className="relative p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100 dark:text-gray-400 dark:hover:text-gray-200 dark:hover:bg-gray-700 transition-colors"
            aria-label="View notifications"
          >
            <BellIcon className="h-6 w-6" />
            {(openAlerts.length > 0 || notifications.length > 0) && (
              <span className="absolute top-1 right-1 block h-2 w-2 rounded-full bg-red-500 ring-2 ring-white dark:ring-gray-800"></span>
            )}
          </button>

          {/* Settings */}
          <button
            onClick={handleSettingsClick}
            className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100 dark:text-gray-400 dark:hover:text-gray-200 dark:hover:bg-gray-700 transition-colors"
            aria-label="Settings"
          >
            <Cog6ToothIcon className="h-6 w-6" />
          </button>

          {/* User Menu */}
          <div className="flex items-center space-x-2">
            {user && (
              <div className="hidden sm:block text-right">
                <div className="text-sm text-gray-700 dark:text-gray-300">
                  Welcome, {user.username}
                </div>
                {user.roles && user.roles.length > 0 && (
                  <div className="flex space-x-1 mt-1">
                    {user.roles.map((role, index) => (
                      <span
                        key={index}
                        className="inline-flex px-1.5 py-0.5 text-xs font-medium rounded-full bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200"
                      >
                        {role.name || role.authority?.replace('ROLE_', '')}
                      </span>
                    ))}
                  </div>
                )}
              </div>
            )}
            <button
              onClick={handleUserMenuClick}
              className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100 dark:text-gray-400 dark:hover:text-gray-200 dark:hover:bg-gray-700 transition-colors"
              aria-label="Logout"
              title="Logout"
            >
              <ArrowRightOnRectangleIcon className="h-6 w-6" />
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};

export default Header;
