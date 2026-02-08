import { useCallback, useEffect, useRef, useState } from 'react';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from '../stores/authStore';

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';
const RECONNECT_DELAY = 5000;
const MAX_RECONNECT_ATTEMPTS = 10;

export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected' | 'error';

interface UseWebSocketOptions {
  autoConnect?: boolean;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (error: Error) => void;
}

interface UseWebSocketReturn {
  client: Client | null;
  status: ConnectionStatus;
  connect: () => void;
  disconnect: () => void;
  subscribe: <T>(destination: string, callback: (message: T) => void) => StompSubscription | null;
  unsubscribe: (subscription: StompSubscription) => void;
  send: <T>(destination: string, body: T) => void;
  isConnected: boolean;
}

export function useWebSocket(options: UseWebSocketOptions = {}): UseWebSocketReturn {
  const { autoConnect = true, onConnect, onDisconnect, onError } = options;
  const { token } = useAuthStore();

  const [status, setStatus] = useState<ConnectionStatus>('disconnected');
  const clientRef = useRef<Client | null>(null);
  const reconnectAttemptsRef = useRef(0);
  const subscriptionsRef = useRef<Map<string, StompSubscription>>(new Map());

  const createClient = useCallback(() => {
    if (!token) {
      console.warn('No auth token available for WebSocket connection');
      return null;
    }

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      debug: (str) => {
        if (import.meta.env.DEV) {
          console.debug('[WebSocket]', str);
        }
      },
      reconnectDelay: RECONNECT_DELAY,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setStatus('connected');
        reconnectAttemptsRef.current = 0;
        console.log('[WebSocket] Connected');
        onConnect?.();
      },
      onDisconnect: () => {
        setStatus('disconnected');
        console.log('[WebSocket] Disconnected');
        onDisconnect?.();
      },
      onStompError: (frame) => {
        console.error('[WebSocket] STOMP error:', frame.headers['message']);
        setStatus('error');
        onError?.(new Error(frame.headers['message'] || 'STOMP error'));
      },
      onWebSocketError: (event) => {
        console.error('[WebSocket] WebSocket error:', event);
        setStatus('error');
        onError?.(new Error('WebSocket connection error'));
      },
      onWebSocketClose: () => {
        reconnectAttemptsRef.current++;
        if (reconnectAttemptsRef.current >= MAX_RECONNECT_ATTEMPTS) {
          console.error('[WebSocket] Max reconnect attempts reached');
          setStatus('error');
          onError?.(new Error('Max reconnect attempts reached'));
        }
      },
    });

    return client;
  }, [token, onConnect, onDisconnect, onError]);

  const connect = useCallback(() => {
    if (clientRef.current?.connected) {
      console.log('[WebSocket] Already connected');
      return;
    }

    if (!token) {
      console.warn('[WebSocket] Cannot connect without auth token');
      return;
    }

    setStatus('connecting');
    const client = createClient();

    if (client) {
      clientRef.current = client;
      client.activate();
    }
  }, [createClient, token]);

  const disconnect = useCallback(() => {
    if (clientRef.current) {
      // Unsubscribe from all subscriptions
      subscriptionsRef.current.forEach((subscription) => {
        try {
          subscription.unsubscribe();
        } catch (e) {
          console.warn('[WebSocket] Error unsubscribing:', e);
        }
      });
      subscriptionsRef.current.clear();

      // Deactivate client
      clientRef.current.deactivate();
      clientRef.current = null;
      setStatus('disconnected');
    }
  }, []);

  const subscribe = useCallback(
    <T>(destination: string, callback: (message: T) => void): StompSubscription | null => {
      if (!clientRef.current?.connected) {
        console.warn('[WebSocket] Cannot subscribe, not connected');
        return null;
      }

      const subscription = clientRef.current.subscribe(destination, (message: IMessage) => {
        try {
          const body = JSON.parse(message.body) as T;
          callback(body);
        } catch (e) {
          console.error('[WebSocket] Error parsing message:', e);
        }
      });

      subscriptionsRef.current.set(destination, subscription);
      return subscription;
    },
    []
  );

  const unsubscribe = useCallback((subscription: StompSubscription) => {
    try {
      subscription.unsubscribe();
      // Remove from map
      subscriptionsRef.current.forEach((sub, key) => {
        if (sub === subscription) {
          subscriptionsRef.current.delete(key);
        }
      });
    } catch (e) {
      console.warn('[WebSocket] Error unsubscribing:', e);
    }
  }, []);

  const send = useCallback(<T>(destination: string, body: T) => {
    if (!clientRef.current?.connected) {
      console.warn('[WebSocket] Cannot send, not connected');
      return;
    }

    clientRef.current.publish({
      destination,
      body: JSON.stringify(body),
    });
  }, []);

  // Auto-connect on mount if token is available
  useEffect(() => {
    if (autoConnect && token) {
      connect();
    }

    return () => {
      disconnect();
    };
  }, [autoConnect, token, connect, disconnect]);

  // Reconnect when token changes
  useEffect(() => {
    if (token && status === 'disconnected' && autoConnect) {
      connect();
    }
  }, [token, status, autoConnect, connect]);

  return {
    client: clientRef.current,
    status,
    connect,
    disconnect,
    subscribe,
    unsubscribe,
    send,
    isConnected: status === 'connected',
  };
}
