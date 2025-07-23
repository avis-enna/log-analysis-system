import React, { useState, useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  Cog6ToothIcon,
  UserIcon,
  BellIcon,
  EyeIcon,
  ShieldCheckIcon,
  ServerIcon,
} from '@heroicons/react/24/outline';
import {
  updateUserSettingsLocal,
  updateTheme,
  updateNotificationSettings,
  updateSearchSettings,
  selectUserSettings,
  selectHasUnsavedChanges,
  selectIsLoading,
} from '../store/slices/settingsSlice';
import LoadingSpinner from '../components/LoadingSpinner/LoadingSpinner';

/**
 * Settings page for user preferences and system configuration
 */
const Settings = () => {
  const dispatch = useDispatch();
  const userSettings = useSelector(selectUserSettings);
  const hasUnsavedChanges = useSelector(selectHasUnsavedChanges);
  const isLoading = useSelector(selectIsLoading);

  const [activeTab, setActiveTab] = useState('general');

  // Settings tabs
  const tabs = [
    { id: 'general', name: 'General', icon: Cog6ToothIcon },
    { id: 'appearance', name: 'Appearance', icon: EyeIcon },
    { id: 'notifications', name: 'Notifications', icon: BellIcon },
    { id: 'search', name: 'Search', icon: UserIcon },
    { id: 'security', name: 'Security', icon: ShieldCheckIcon },
    { id: 'system', name: 'System', icon: ServerIcon },
  ];

  // Handle settings change
  const handleSettingChange = (path, value) => {
    if (path.includes('.')) {
      // Handle nested settings
      const keys = path.split('.');
      const updates = {};
      let current = updates;
      
      for (let i = 0; i < keys.length - 1; i++) {
        current[keys[i]] = {};
        current = current[keys[i]];
      }
      current[keys[keys.length - 1]] = value;
      
      dispatch(updateUserSettingsLocal(updates));
    } else {
      dispatch(updateUserSettingsLocal({ [path]: value }));
    }
  };

  // Handle theme change
  const handleThemeChange = (theme) => {
    dispatch(updateTheme(theme));
  };

  // Save settings
  const handleSave = () => {
    // In a real app, this would save to the backend
    console.log('Saving settings:', userSettings);
  };

  // Reset settings
  const handleReset = () => {
    // Reset to defaults
    console.log('Resetting settings to defaults');
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Settings
          </h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Manage your preferences and system configuration
          </p>
        </div>
        
        {hasUnsavedChanges && (
          <div className="mt-4 sm:mt-0 flex items-center space-x-3">
            <button
              onClick={handleReset}
              className="btn-outline"
            >
              Reset
            </button>
            <button
              onClick={handleSave}
              className="btn-primary"
              disabled={isLoading.updating}
            >
              {isLoading.updating ? (
                <>
                  <LoadingSpinner size="small" color="white" />
                  <span className="ml-2">Saving...</span>
                </>
              ) : (
                'Save Changes'
              )}
            </button>
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Settings Navigation */}
        <div className="lg:col-span-1">
          <nav className="space-y-1">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`w-full flex items-center px-3 py-2 text-sm font-medium rounded-lg transition-colors ${
                    activeTab === tab.id
                      ? 'bg-primary-100 text-primary-700 dark:bg-primary-900 dark:text-primary-200'
                      : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-gray-200'
                  }`}
                >
                  <Icon className="h-5 w-5 mr-3" />
                  {tab.name}
                </button>
              );
            })}
          </nav>
        </div>

        {/* Settings Content */}
        <div className="lg:col-span-3">
          <div className="card">
            <div className="card-body">
              {/* General Settings */}
              {activeTab === 'general' && (
                <div className="space-y-6">
                  <div>
                    <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-4">
                      General Settings
                    </h3>
                    
                    <div className="space-y-4">
                      <div>
                        <label className="form-label">Language</label>
                        <select
                          value={userSettings.language}
                          onChange={(e) => handleSettingChange('language', e.target.value)}
                          className="form-input"
                        >
                          <option value="en">English</option>
                          <option value="es">Español</option>
                          <option value="fr">Français</option>
                          <option value="de">Deutsch</option>
                        </select>
                      </div>
                      
                      <div>
                        <label className="form-label">Timezone</label>
                        <select
                          value={userSettings.timezone}
                          onChange={(e) => handleSettingChange('timezone', e.target.value)}
                          className="form-input"
                        >
                          <option value="UTC">UTC</option>
                          <option value="America/New_York">Eastern Time</option>
                          <option value="America/Los_Angeles">Pacific Time</option>
                          <option value="Europe/London">London</option>
                          <option value="Asia/Tokyo">Tokyo</option>
                        </select>
                      </div>
                      
                      <div>
                        <label className="form-label">Date Format</label>
                        <select
                          value={userSettings.dateFormat}
                          onChange={(e) => handleSettingChange('dateFormat', e.target.value)}
                          className="form-input"
                        >
                          <option value="YYYY-MM-DD HH:mm:ss">YYYY-MM-DD HH:mm:ss</option>
                          <option value="MM/DD/YYYY HH:mm:ss">MM/DD/YYYY HH:mm:ss</option>
                          <option value="DD/MM/YYYY HH:mm:ss">DD/MM/YYYY HH:mm:ss</option>
                        </select>
                      </div>
                      
                      <div>
                        <label className="form-label">Default Page Size</label>
                        <select
                          value={userSettings.pageSize}
                          onChange={(e) => handleSettingChange('pageSize', parseInt(e.target.value))}
                          className="form-input"
                        >
                          <option value={25}>25</option>
                          <option value={50}>50</option>
                          <option value={100}>100</option>
                          <option value={200}>200</option>
                        </select>
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* Appearance Settings */}
              {activeTab === 'appearance' && (
                <div className="space-y-6">
                  <div>
                    <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-4">
                      Appearance Settings
                    </h3>
                    
                    <div className="space-y-4">
                      <div>
                        <label className="form-label">Theme</label>
                        <div className="mt-2 space-y-2">
                          {['light', 'dark', 'auto'].map((theme) => (
                            <label key={theme} className="flex items-center">
                              <input
                                type="radio"
                                name="theme"
                                value={theme}
                                checked={userSettings.theme === theme}
                                onChange={(e) => handleThemeChange(e.target.value)}
                                className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300"
                              />
                              <span className="ml-2 text-sm text-gray-700 dark:text-gray-300 capitalize">
                                {theme}
                              </span>
                            </label>
                          ))}
                        </div>
                      </div>
                      
                      <div className="flex items-center justify-between">
                        <div>
                          <label className="form-label mb-0">Compact Mode</label>
                          <p className="text-sm text-gray-500 dark:text-gray-400">
                            Reduce spacing and padding for more content
                          </p>
                        </div>
                        <input
                          type="checkbox"
                          checked={userSettings.dashboard?.compactMode || false}
                          onChange={(e) => handleSettingChange('dashboard.compactMode', e.target.checked)}
                          className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                        />
                      </div>
                      
                      <div className="flex items-center justify-between">
                        <div>
                          <label className="form-label mb-0">Animations</label>
                          <p className="text-sm text-gray-500 dark:text-gray-400">
                            Enable smooth transitions and animations
                          </p>
                        </div>
                        <input
                          type="checkbox"
                          checked={userSettings.dashboard?.animationsEnabled !== false}
                          onChange={(e) => handleSettingChange('dashboard.animationsEnabled', e.target.checked)}
                          className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                        />
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* Notification Settings */}
              {activeTab === 'notifications' && (
                <div className="space-y-6">
                  <div>
                    <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-4">
                      Notification Settings
                    </h3>
                    
                    <div className="space-y-4">
                      <div className="flex items-center justify-between">
                        <div>
                          <label className="form-label mb-0">Enable Notifications</label>
                          <p className="text-sm text-gray-500 dark:text-gray-400">
                            Receive notifications for alerts and system events
                          </p>
                        </div>
                        <input
                          type="checkbox"
                          checked={userSettings.notifications?.enabled !== false}
                          onChange={(e) => handleSettingChange('notifications.enabled', e.target.checked)}
                          className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                        />
                      </div>
                      
                      <div className="flex items-center justify-between">
                        <div>
                          <label className="form-label mb-0">Sound Notifications</label>
                          <p className="text-sm text-gray-500 dark:text-gray-400">
                            Play sound for important alerts
                          </p>
                        </div>
                        <input
                          type="checkbox"
                          checked={userSettings.notifications?.sound || false}
                          onChange={(e) => handleSettingChange('notifications.sound', e.target.checked)}
                          className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                        />
                      </div>
                      
                      <div className="flex items-center justify-between">
                        <div>
                          <label className="form-label mb-0">Desktop Notifications</label>
                          <p className="text-sm text-gray-500 dark:text-gray-400">
                            Show browser notifications
                          </p>
                        </div>
                        <input
                          type="checkbox"
                          checked={userSettings.notifications?.desktop !== false}
                          onChange={(e) => handleSettingChange('notifications.desktop', e.target.checked)}
                          className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                        />
                      </div>
                      
                      <div>
                        <label className="form-label">Notification Severity Filter</label>
                        <div className="mt-2 space-y-2">
                          {['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map((severity) => (
                            <label key={severity} className="flex items-center">
                              <input
                                type="checkbox"
                                checked={userSettings.notifications?.severityFilter?.includes(severity) || false}
                                onChange={(e) => {
                                  const current = userSettings.notifications?.severityFilter || [];
                                  const updated = e.target.checked
                                    ? [...current, severity]
                                    : current.filter(s => s !== severity);
                                  handleSettingChange('notifications.severityFilter', updated);
                                }}
                                className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                              />
                              <span className="ml-2 text-sm text-gray-700 dark:text-gray-300">
                                {severity}
                              </span>
                            </label>
                          ))}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* Search Settings */}
              {activeTab === 'search' && (
                <div className="space-y-6">
                  <div>
                    <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-4">
                      Search Settings
                    </h3>
                    
                    <div className="space-y-4">
                      <div>
                        <label className="form-label">Default Search Page Size</label>
                        <select
                          value={userSettings.search?.defaultPageSize || 100}
                          onChange={(e) => handleSettingChange('search.defaultPageSize', parseInt(e.target.value))}
                          className="form-input"
                        >
                          <option value={25}>25</option>
                          <option value={50}>50</option>
                          <option value={100}>100</option>
                          <option value={200}>200</option>
                        </select>
                      </div>
                      
                      <div className="flex items-center justify-between">
                        <div>
                          <label className="form-label mb-0">Highlight Search Results</label>
                          <p className="text-sm text-gray-500 dark:text-gray-400">
                            Highlight matching terms in search results
                          </p>
                        </div>
                        <input
                          type="checkbox"
                          checked={userSettings.search?.highlightEnabled !== false}
                          onChange={(e) => handleSettingChange('search.highlightEnabled', e.target.checked)}
                          className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                        />
                      </div>
                      
                      <div className="flex items-center justify-between">
                        <div>
                          <label className="form-label mb-0">Show Metadata</label>
                          <p className="text-sm text-gray-500 dark:text-gray-400">
                            Display additional log metadata by default
                          </p>
                        </div>
                        <input
                          type="checkbox"
                          checked={userSettings.search?.showMetadata || false}
                          onChange={(e) => handleSettingChange('search.showMetadata', e.target.checked)}
                          className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                        />
                      </div>
                      
                      <div className="flex items-center justify-between">
                        <div>
                          <label className="form-label mb-0">Case Sensitive Search</label>
                          <p className="text-sm text-gray-500 dark:text-gray-400">
                            Make searches case sensitive by default
                          </p>
                        </div>
                        <input
                          type="checkbox"
                          checked={userSettings.search?.caseSensitive || false}
                          onChange={(e) => handleSettingChange('search.caseSensitive', e.target.checked)}
                          className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                        />
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* Security Settings */}
              {activeTab === 'security' && (
                <div className="space-y-6">
                  <div>
                    <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-4">
                      Security Settings
                    </h3>
                    
                    <div className="space-y-4">
                      <div className="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg p-4">
                        <p className="text-sm text-yellow-800 dark:text-yellow-200">
                          Security settings are managed by your system administrator. 
                          Contact support if you need to modify these settings.
                        </p>
                      </div>
                      
                      <div>
                        <label className="form-label">Session Timeout</label>
                        <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">
                          Current: 1 hour
                        </p>
                        <input
                          type="text"
                          value="1 hour"
                          disabled
                          className="form-input opacity-50 cursor-not-allowed"
                        />
                      </div>
                      
                      <div>
                        <label className="form-label">Two-Factor Authentication</label>
                        <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">
                          Status: Not configured
                        </p>
                        <button className="btn-outline" disabled>
                          Configure 2FA
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* System Settings */}
              {activeTab === 'system' && (
                <div className="space-y-6">
                  <div>
                    <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-4">
                      System Information
                    </h3>
                    
                    <div className="space-y-4">
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                          <label className="form-label">Version</label>
                          <p className="text-sm text-gray-900 dark:text-white">1.0.0</p>
                        </div>
                        
                        <div>
                          <label className="form-label">Build Date</label>
                          <p className="text-sm text-gray-900 dark:text-white">2024-01-15</p>
                        </div>
                        
                        <div>
                          <label className="form-label">API Status</label>
                          <span className="badge badge-success">Connected</span>
                        </div>
                        
                        <div>
                          <label className="form-label">Database Status</label>
                          <span className="badge badge-success">Connected</span>
                        </div>
                      </div>
                      
                      <div className="pt-4 border-t border-gray-200 dark:border-gray-700">
                        <h4 className="text-md font-medium text-gray-900 dark:text-white mb-3">
                          System Limits
                        </h4>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                          <div>
                            <span className="text-gray-500 dark:text-gray-400">Max Search Results:</span>
                            <span className="ml-2 text-gray-900 dark:text-white">10,000</span>
                          </div>
                          <div>
                            <span className="text-gray-500 dark:text-gray-400">Max File Upload:</span>
                            <span className="ml-2 text-gray-900 dark:text-white">100MB</span>
                          </div>
                          <div>
                            <span className="text-gray-500 dark:text-gray-400">Concurrent Searches:</span>
                            <span className="ml-2 text-gray-900 dark:text-white">100</span>
                          </div>
                          <div>
                            <span className="text-gray-500 dark:text-gray-400">Dashboard Widgets:</span>
                            <span className="ml-2 text-gray-900 dark:text-white">50</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Settings;
