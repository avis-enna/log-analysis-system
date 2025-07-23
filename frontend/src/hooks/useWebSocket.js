import { useEffect, useRef, useState, useCallback } from 'react';
import { useDispatch } from 'react-redux';
import { io } from 'socket.io-client';
import toast from 'react-hot-toast';

/**
 * Custom hook for WebSocket connection management
 * Handles real-time updates for logs, alerts, and dashboard metrics
 */
export const useWebSocket = () => {
  const [isConnected, setIsConnected] = useState(false);
  const [connectionError, setConnectionError] = useState(null);
  const [lastMessage, setLastMessage] = useState(null);
  const socketRef = useRef(null);
  const dispatch = useDispatch();
  const reconnectTimeoutRef = useRef(null);
  const reconnectAttempts = useRef(0);
  const maxReconnectAttempts = 5;

  // WebSocket server URL - adjust based on environment
  const getSocketUrl = useCallback(() => {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = process.env.NODE_ENV === 'production' 
      ? window.location.host 
      : 'localhost:8080';
    return `${protocol}//${host}`;
  }, []);

  // Initialize WebSocket connection
  const connect = useCallback(() => {
    try {
      if (socketRef.current?.connected) {
        return;
      }

      const socketUrl = getSocketUrl();
      console.log('Connecting to WebSocket:', socketUrl);

      socketRef.current = io(socketUrl, {
        transports: ['websocket', 'polling'],
        timeout: 20000,
        reconnection: true,
        reconnectionDelay: 1000,
        reconnectionDelayMax: 5000,
        maxReconnectionAttempts: maxReconnectAttempts,
        forceNew: true,
      });

      // Connection event handlers
      socketRef.current.on('connect', () => {
        console.log('WebSocket connected');
        setIsConnected(true);
        setConnectionError(null);
        reconnectAttempts.current = 0;
        
        // Subscribe to real-time channels
        socketRef.current.emit('subscribe', {
          channels: ['logs', 'alerts', 'dashboard', 'system']
        });
      });

      socketRef.current.on('disconnect', (reason) => {
        console.log('WebSocket disconnected:', reason);
        setIsConnected(false);
        
        if (reason === 'io server disconnect') {
          // Server initiated disconnect, try to reconnect
          setTimeout(() => connect(), 1000);
        }
      });

      socketRef.current.on('connect_error', (error) => {
        console.error('WebSocket connection error:', error);
        setConnectionError(error.message);
        setIsConnected(false);
        
        reconnectAttempts.current += 1;
        if (reconnectAttempts.current >= maxReconnectAttempts) {
          toast.error('Failed to connect to real-time updates');
        }
      });

      // Real-time data handlers
      socketRef.current.on('log_entry', (data) => {
        console.log('New log entry:', data);
        setLastMessage({ type: 'log_entry', data, timestamp: Date.now() });
        
        // Dispatch to Redux store if needed
        // dispatch(addLogEntry(data));
      });

      socketRef.current.on('alert', (data) => {
        console.log('New alert:', data);
        setLastMessage({ type: 'alert', data, timestamp: Date.now() });
        
        // Show toast notification for high severity alerts
        if (data.severity === 'HIGH' || data.severity === 'CRITICAL') {
          toast.error(`Alert: ${data.title}`, {
            duration: 5000,
            position: 'top-right',
          });
        }
        
        // Dispatch to Redux store
        // dispatch(addAlert(data));
      });

      socketRef.current.on('dashboard_update', (data) => {
        console.log('Dashboard update:', data);
        setLastMessage({ type: 'dashboard_update', data, timestamp: Date.now() });
        
        // Dispatch to Redux store
        // dispatch(updateDashboardMetrics(data));
      });

      socketRef.current.on('system_status', (data) => {
        console.log('System status update:', data);
        setLastMessage({ type: 'system_status', data, timestamp: Date.now() });
        
        // Handle system status changes
        if (data.status === 'error') {
          toast.error(`System Error: ${data.message}`);
        }
      });

      // Heartbeat to keep connection alive
      socketRef.current.on('ping', () => {
        socketRef.current.emit('pong');
      });

    } catch (error) {
      console.error('Failed to initialize WebSocket:', error);
      setConnectionError(error.message);
    }
  }, [getSocketUrl, dispatch]);

  // Disconnect WebSocket
  const disconnect = useCallback(() => {
    if (socketRef.current) {
      socketRef.current.disconnect();
      socketRef.current = null;
    }
    setIsConnected(false);
    setConnectionError(null);
    
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
    }
  }, []);

  // Send message through WebSocket
  const sendMessage = useCallback((event, data) => {
    if (socketRef.current?.connected) {
      socketRef.current.emit(event, data);
      return true;
    } else {
      console.warn('WebSocket not connected, cannot send message');
      return false;
    }
  }, []);

  // Subscribe to specific channel
  const subscribe = useCallback((channel) => {
    if (socketRef.current?.connected) {
      socketRef.current.emit('subscribe', { channel });
    }
  }, []);

  // Unsubscribe from specific channel
  const unsubscribe = useCallback((channel) => {
    if (socketRef.current?.connected) {
      socketRef.current.emit('unsubscribe', { channel });
    }
  }, []);

  // Initialize connection on mount
  useEffect(() => {
    connect();

    // Cleanup on unmount
    return () => {
      disconnect();
    };
  }, [connect, disconnect]);

  // Handle page visibility changes
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.hidden) {
        // Page is hidden, reduce activity
        if (socketRef.current?.connected) {
          socketRef.current.emit('page_hidden');
        }
      } else {
        // Page is visible, resume normal activity
        if (socketRef.current?.connected) {
          socketRef.current.emit('page_visible');
        } else {
          // Reconnect if disconnected while page was hidden
          connect();
        }
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [connect]);

  // Return hook interface
  return {
    isConnected,
    connectionError,
    lastMessage,
    sendMessage,
    subscribe,
    unsubscribe,
    connect,
    disconnect,
  };
};
