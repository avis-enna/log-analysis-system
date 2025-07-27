import React, { useState, useEffect } from 'react';
import { useSelector } from 'react-redux';
import { 
  UsersIcon, 
  ShieldCheckIcon, 
  CogIcon,
  InformationCircleIcon
} from '@heroicons/react/24/outline';
import { selectUser, selectUserPermissions, selectIsAdmin } from '../store/slices/authSlice';
import RoleBasedComponent from '../components/RoleBasedComponent/RoleBasedComponent';

/**
 * Role Management Page
 * Shows available users, their roles, and test credentials
 */
const RoleManagement = () => {
  const user = useSelector(selectUser);
  const permissions = useSelector(selectUserPermissions);
  const isAdmin = useSelector(selectIsAdmin);
  const [testCredentials, setTestCredentials] = useState([]);
  const [availableRoles, setAvailableRoles] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchTestCredentials();
    if (isAdmin) {
      fetchAvailableRoles();
    }
  }, [isAdmin]);

  const fetchTestCredentials = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/v1/auth/test-credentials');
      if (response.ok) {
        const data = await response.json();
        setTestCredentials(data.users || []);
      }
    } catch (error) {
      // Error fetching test credentials
      // eslint-disable-next-line no-console
      console.error('Failed to fetch test credentials:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchAvailableRoles = async () => {
    try {
      const authToken = localStorage.getItem('authToken');
      if (!authToken) return;

      const response = await fetch('http://localhost:8080/api/v1/auth/roles', {
        headers: {
          'Authorization': `Basic ${authToken}`,
        },
      });
      
      if (response.ok) {
        const roles = await response.json();
        setAvailableRoles(roles);
      }
    } catch (error) {
      // Error fetching roles
      // eslint-disable-next-line no-console
      console.error('Failed to fetch roles:', error);
    }
  };

  const getRoleBadgeColor = (role) => {
    switch (role) {
      case 'ADMIN':
        return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200';
      case 'DEVELOPER':
        return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200';
      case 'QA':
        return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200';
      default:
        return 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-200';
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
          Role Management
        </h1>
        <p className="mt-2 text-gray-600 dark:text-gray-400">
          Manage user roles and permissions in the system
        </p>
      </div>

      {/* Current User Info */}
      <div className="bg-white dark:bg-gray-800 shadow rounded-lg p-6 mb-8">
        <div className="flex items-center space-x-3 mb-4">
          <UsersIcon className="h-6 w-6 text-blue-600" />
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
            Current User
          </h2>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
              Username
            </label>
            <p className="mt-1 text-sm text-gray-900 dark:text-white font-mono">
              {user?.username}
            </p>
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
              Email
            </label>
            <p className="mt-1 text-sm text-gray-900 dark:text-white">
              {user?.email || 'Not specified'}
            </p>
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
              Roles
            </label>
            <div className="mt-1 flex flex-wrap gap-1">
              {user?.roles?.map((role, index) => (
                <span
                  key={index}
                  className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${getRoleBadgeColor(role.name)}`}
                >
                  {role.name}
                </span>
              )) || (
                <span className="text-sm text-gray-500 dark:text-gray-400">No roles assigned</span>
              )}
            </div>
          </div>
        </div>

        {/* Permissions */}
        <div className="mt-6">
          <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-3">
            Your Permissions
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            {Object.entries(permissions).filter(([key, value]) => 
              key.startsWith('can') && value === true
            ).map(([permission, _]) => (
              <div 
                key={permission}
                className="flex items-center space-x-2 text-sm text-green-600 dark:text-green-400"
              >
                <ShieldCheckIcon className="h-4 w-4" />
                <span>{permission.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase())}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Available Roles (Admin Only) */}
      <RoleBasedComponent requiresAdmin>
        <div className="bg-white dark:bg-gray-800 shadow rounded-lg p-6 mb-8">
          <div className="flex items-center space-x-3 mb-4">
            <CogIcon className="h-6 w-6 text-purple-600" />
            <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
              Available Roles
            </h2>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {availableRoles.map((role, index) => (
              <div key={index} className="border border-gray-200 dark:border-gray-700 rounded-lg p-4">
                <div className="flex items-center justify-between mb-2">
                  <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${getRoleBadgeColor(role.name)}`}>
                    {role.name}
                  </span>
                  <span className="text-xs text-gray-500 dark:text-gray-400 font-mono">
                    {role.authority}
                  </span>
                </div>
                <p className="text-sm text-gray-600 dark:text-gray-400">
                  {role.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </RoleBasedComponent>

      {/* Test Credentials */}
      <div className="bg-white dark:bg-gray-800 shadow rounded-lg p-6">
        <div className="flex items-center space-x-3 mb-4">
          <InformationCircleIcon className="h-6 w-6 text-yellow-600" />
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
            Test Credentials
          </h2>
        </div>
        
        <div className="mb-4 p-4 bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-700 rounded-lg">
          <p className="text-sm text-yellow-800 dark:text-yellow-200">
            <strong>Note:</strong> These are development-only credentials. In production, use proper authentication.
          </p>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
            <thead className="bg-gray-50 dark:bg-gray-700">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                  Username
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                  Password
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                  Role
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                  Description
                </th>
              </tr>
            </thead>
            <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
              {testCredentials.map((credential, index) => (
                <tr key={index} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-mono text-gray-900 dark:text-white">
                    {credential.username}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-mono text-gray-900 dark:text-white">
                    {credential.password}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${getRoleBadgeColor(credential.role.split('+')[0])}`}>
                      {credential.role}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-600 dark:text-gray-400">
                    {credential.description}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="mt-4 p-4 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-700 rounded-lg">
          <h4 className="text-sm font-medium text-blue-800 dark:text-blue-200 mb-2">
            How to test different roles:
          </h4>
          <ol className="text-sm text-blue-700 dark:text-blue-300 space-y-1">
            <li>1. Logout from the current session</li>
            <li>2. Login with any of the credentials above</li>
            <li>3. Observe different UI elements and permissions based on the role</li>
            <li>4. Admin users can see this entire page, others see limited information</li>
          </ol>
        </div>
      </div>
    </div>
  );
};

export default RoleManagement;
