import { createSlice } from '@reduxjs/toolkit';

// Helper function to get user permissions
const getUserPermissions = async (token) => {
  try {
    const response = await fetch('http://localhost:8080/api/v1/auth/permissions', {
      headers: {
        'Authorization': `Basic ${token}`,
      },
    });
    
    if (response.ok) {
      return await response.json();
    }
  } catch (error) {
    console.error('Failed to fetch permissions:', error);
  }
  
  return {};
};

const initialState = {
  isAuthenticated: false,
  user: null,
  token: null,
  loading: false,
  error: null
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    loginStart: (state) => {
      state.loading = true;
      state.error = null;
    },
    loginSuccess: (state, action) => {
      state.isAuthenticated = true;
      state.user = action.payload.user;
      state.token = action.payload.token;
      state.loading = false;
      state.error = null;
    },
    loginFailure: (state, action) => {
      state.isAuthenticated = false;
      state.user = null;
      state.token = null;
      state.loading = false;
      state.error = action.payload;
    },
    logout: (state) => {
      state.isAuthenticated = false;
      state.user = null;
      state.token = null;
      state.loading = false;
      state.error = null;
    },
    clearError: (state) => {
      state.error = null;
    },
    // Initialize auth state from localStorage
    initializeAuth: (state) => {
      const token = localStorage.getItem('authToken');
      const userStr = localStorage.getItem('user');
      
      if (token && userStr) {
        try {
          const user = JSON.parse(userStr);
          state.isAuthenticated = true;
          state.user = user;
          state.token = token;
        } catch (error) {
          // Clear invalid data
          localStorage.removeItem('authToken');
          localStorage.removeItem('user');
        }
      }
    }
  }
});

// Action creators
export const { 
  loginStart, 
  loginSuccess, 
  loginFailure, 
  logout, 
  clearError,
  initializeAuth 
} = authSlice.actions;

// Thunk for login
export const login = (credentials) => async (dispatch) => {
  dispatch(loginStart());
  
  try {
    // Call the actual backend API
    const response = await fetch('http://localhost:8080/api/v1/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(credentials),
    });

    if (!response.ok) {
      const errorData = await response.json();
      dispatch(loginFailure(errorData.error || 'Login failed'));
      return { success: false, error: errorData.error || 'Login failed' };
    }

    const data = await response.json();
    
    const authData = {
      token: btoa(`${credentials.username}:${credentials.password}`), // Basic auth token
      user: {
        ...data.user,
        permissions: await getUserPermissions(btoa(`${credentials.username}:${credentials.password}`))
      }
    };

    // Save to localStorage
    localStorage.setItem('authToken', authData.token);
    localStorage.setItem('user', JSON.stringify(authData.user));

    dispatch(loginSuccess(authData));
    return { success: true };
  } catch (error) {
    dispatch(loginFailure(error.message));
    return { success: false, error: error.message };
  }
};

// Thunk for logout
export const logoutUser = () => (dispatch) => {
  localStorage.removeItem('authToken');
  localStorage.removeItem('user');
  dispatch(logout());
};

// Selectors
export const selectAuth = (state) => state.auth;
export const selectIsAuthenticated = (state) => state.auth.isAuthenticated;
export const selectUser = (state) => state.auth.user;
export const selectAuthLoading = (state) => state.auth.loading;
export const selectAuthError = (state) => state.auth.error;
export const selectAuthToken = (state) => state.auth.token;

// Role-based selectors
export const selectUserRoles = (state) => state.auth.user?.roles || [];
export const selectUserPermissions = (state) => state.auth.user?.permissions || {};

// Permission checkers
export const selectCanManageUsers = (state) => state.auth.user?.permissions?.canManageUsers || false;
export const selectCanViewSystemStats = (state) => state.auth.user?.permissions?.canViewSystemStats || false;
export const selectCanAccessAdminPanel = (state) => state.auth.user?.permissions?.canAccessAdminPanel || false;
export const selectCanIngestLogs = (state) => state.auth.user?.permissions?.canIngestLogs || false;
export const selectCanAccessDeployment = (state) => state.auth.user?.permissions?.canAccessDeployment || false;
export const selectCanViewDetailedLogs = (state) => state.auth.user?.permissions?.canViewDetailedLogs || false;
export const selectCanAccessTesting = (state) => state.auth.user?.permissions?.canAccessTesting || false;
export const selectCanViewMonitoring = (state) => state.auth.user?.permissions?.canViewMonitoring || false;
export const selectCanCreateReports = (state) => state.auth.user?.permissions?.canCreateReports || false;

// Role checkers
export const selectIsAdmin = (state) => {
  const roles = selectUserRoles(state);
  return roles.some(role => role.authority === 'ROLE_ADMIN');
};

export const selectIsDeveloper = (state) => {
  const roles = selectUserRoles(state);
  return roles.some(role => role.authority === 'ROLE_DEVELOPER');
};

export const selectIsQA = (state) => {
  const roles = selectUserRoles(state);
  return roles.some(role => role.authority === 'ROLE_QA');
};

export default authSlice.reducer;
