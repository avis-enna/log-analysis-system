import React from 'react';
import { useSelector } from 'react-redux';
import { 
  selectUserPermissions, 
  selectUserRoles,
  selectIsAdmin,
  selectIsDeveloper,
  selectIsQA 
} from '../../store/slices/authSlice';

/**
 * RoleBasedComponent - Conditionally render components based on user roles and permissions
 * 
 * Usage examples:
 * <RoleBasedComponent roles={['ADMIN']}>
 *   <AdminOnlyButton />
 * </RoleBasedComponent>
 * 
 * <RoleBasedComponent permissions={['canManageUsers']}>
 *   <UserManagementPanel />
 * </RoleBasedComponent>
 * 
 * <RoleBasedComponent requiresAdmin>
 *   <AdminPanel />
 * </RoleBasedComponent>
 */
const RoleBasedComponent = ({ 
  children, 
  roles = [], 
  permissions = [], 
  requiresAdmin = false,
  requiresDeveloper = false,
  requiresQA = false,
  requiresAnyRole = false,
  fallback = null 
}) => {
  const userRoles = useSelector(selectUserRoles);
  const userPermissions = useSelector(selectUserPermissions);
  const isAdmin = useSelector(selectIsAdmin);
  const isDeveloper = useSelector(selectIsDeveloper);
  const isQA = useSelector(selectIsQA);

  // Check specific role requirements
  if (requiresAdmin && !isAdmin) {
    return fallback;
  }

  if (requiresDeveloper && !isDeveloper && !isAdmin) {
    return fallback;
  }

  if (requiresQA && !isQA && !isAdmin) {
    return fallback;
  }

  if (requiresAnyRole && !isAdmin && !isDeveloper && !isQA) {
    return fallback;
  }

  // Check role array
  if (roles.length > 0) {
    const hasRequiredRole = roles.some(requiredRole => 
      userRoles.some(userRole => 
        userRole.authority === `ROLE_${requiredRole}` || 
        userRole.name === requiredRole
      )
    );
    if (!hasRequiredRole) {
      return fallback;
    }
  }

  // Check permissions array
  if (permissions.length > 0) {
    const hasRequiredPermission = permissions.some(permission => 
      userPermissions[permission] === true
    );
    if (!hasRequiredPermission) {
      return fallback;
    }
  }

  return children;
};

export default RoleBasedComponent;
