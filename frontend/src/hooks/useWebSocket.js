import { useEffect, useRef, useState, useCallback } from 'react';
import { useDispatch } from 'react-redux';
import SockJS from 'sockjs-client/dist/sockjs';
import { Stomp } from '@stomp/stompjs';
import toast from 'react-hot-toast';

/**
 * Custom hook for WebSocket connection management using STOMP
 * Handles real-time updates for logs, alerts, and dashboard metrics
 */
export const useWebSocket = () => {
  const [isConnected, setIsConnected] = useState(false);
  const [connectionError, setConnectionError] = useState(null);
  const [lastMessage, setLastMessage] = useState(null);
  const stompClientRef = useRef(null);
  const dispatch = useDispatch();
  const reconnectTimeoutRef = useRef(null);
  const reconnectAttempts = useRef(0);
  const maxReconnectAttempts = 5;

  // WebSocket server URL - adjust based on environment
  const getSocketUrl = useCallback(() => {
    const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
    const host = process.env.NODE_ENV === 'production' 
      ? window.location.host 
      : 'localhost:8080';
    return `${protocol}//${host}/api/v1/ws`;
  }, []);

  // Subscribe to various topics for real-time updates
  const subscribeToTopics = useCallback(() => {
    if (!stompClientRef.current?.connected) return;

    try {
      // Subscribe to dashboard metrics
      stompClientRef.current.subscribe('/topic/dashboard/metrics', (message) => {
        const data = JSON.parse(message.body);
        console.log('Received dashboard metrics:', data);
        setLastMessage({ type: 'metrics', data, timestamp: new Date() });
      });

      // Subscribe to log updates
      stompClientRef.current.subscribe('/topic/logs/updates', (message) => {
        const data = JSON.parse(message.body);
        console.log('Received log updates:', data);
        setLastMessage({ type: 'logs', data, timestamp: new Date() });
      });

      // Subscribe to system health
      stompClientRef.current.subscribe('/topic/system/health', (message) => {
        const data = JSON.parse(message.body);
        console.log('Received system health:', data);
        setLastMessage({ type: 'health', data, timestamp: new Date() });
      });

      // Subscribe to alerts
      stompClientRef.current.subscribe('/topic/alerts/new', (message) => {
        const data = JSON.parse(message.body);
        console.log('Received new alert:', data);
        toast.error(`Alert: ${data.title}`, {
          description: data.message,
          duration: 5000,
        });
        setLastMessage({ type: 'alert', data, timestamp: new Date() });
      });

      // Send subscription confirmation
      stompClientRef.current.send('/app/subscribe', {}, JSON.stringify({
        clientId: 'dashboard-client',
        topics: ['metrics', 'logs', 'health', 'alerts']
      }));

    } catch (error) {
      console.error('Error subscribing to topics:', error);
    }
  }, [dispatch]);

  // Handle reconnection with exponential backoff
  const handleReconnect = useCallback(() => {
    if (reconnectAttempts.current >= maxReconnectAttempts) {
      console.log('Max reconnection attempts reached');
      setConnectionError('Failed to connect after multiple attempts');
      return;
    }

    const delay = Math.min(1000 * Math.pow(2, reconnectAttempts.current), 30000);
    reconnectAttempts.current += 1;
    
    console.log(`Attempting to reconnect in ${delay}ms (attempt ${reconnectAttempts.current})`);
    
    reconnectTimeoutRef.current = setTimeout(() => {
      connect();
    }, delay);
  }, []);

  // Initialize WebSocket connection using STOMP
  const connect = useCallback(() => {
    try {
      if (stompClientRef.current?.connected) {
        return;
      }

      const socketUrl = getSocketUrl();
      console.log('Connecting to WebSocket:', socketUrl);

      // Create SockJS connection
      const socket = new SockJS(socketUrl);
      stompClientRef.current = Stomp.over(socket);
      
      // Disable debug logging in production
      if (process.env.NODE_ENV === 'production') {
        stompClientRef.current.debug = null;
      }

      // Connection headers
      const connectHeaders = {
        'Accept-Version': '1.0,1.1,2.0',
        'heart-beat': '10000,10000'
      };

      // Connect to STOMP server
      stompClientRef.current.connect(connectHeaders, 
        // Success callback
        (frame) => {
          console.log('WebSocket connected:', frame);
          setIsConnected(true);
          setConnectionError(null);
          reconnectAttempts.current = 0;

          // Subscribe to different topics
          subscribeToTopics();
        },
        // Error callback
        (error) => {
          console.error('WebSocket connection error:', error);
          setIsConnected(false);
          setConnectionError(error.toString());
          handleReconnect();
        }
      );

    } catch (error) {
      console.error('Failed to initialize WebSocket connection:', error);
      setConnectionError(error.toString());
      handleReconnect();
    }
  }, [getSocketUrl, subscribeToTopics, handleReconnect]);

  // Disconnect from WebSocket
  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }

    if (stompClientRef.current?.connected) {
      try {
        stompClientRef.current.disconnect(() => {
          console.log('WebSocket disconnected');
          setIsConnected(false);
        });
      } catch (error) {
        console.error('Error disconnecting:', error);
      }
    }
    
    stompClientRef.current = null;
    setIsConnected(false);
    reconnectAttempts.current = 0;
  }, []);

  // Send message to server
  const sendMessage = useCallback((destination, message) => {
    if (stompClientRef.current?.connected) {
      try {
        stompClientRef.current.send(destination, {}, JSON.stringify(message));
        return true;
      } catch (error) {
        console.error('Error sending message:', error);
        return false;
      }
    }
    console.warn('Cannot send message - WebSocket not connected');
    return false;
  }, []);

  // Request dashboard metrics refresh
  const requestMetricsRefresh = useCallback(() => {
    return sendMessage('/app/dashboard/refresh', { 
      timestamp: new Date().toISOString() 
    });
  }, [sendMessage]);

  // Request log search
  const requestLogSearch = useCallback((query) => {
    return sendMessage('/app/logs/search', { 
      query,
      timestamp: new Date().toISOString()
    });
  }, [sendMessage]);

  // Effect to handle connection lifecycle
  useEffect(() => {
    connect();

    // Cleanup on unmount
    return () => {
      disconnect();
    };
  }, [connect, disconnect]);

  // Cleanup timeouts on unmount
  useEffect(() => {
    return () => {
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
    };
  }, []);

  return {
    isConnected,
    connectionError,
    lastMessage,
    sendMessage,
    requestMetricsRefresh,
    requestLogSearch,
    connect,
    disconnect,
  };
};