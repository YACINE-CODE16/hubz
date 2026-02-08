import { useState, useEffect, useRef } from 'react';
import { Bell, Check, CheckCheck, Trash2, X, Settings } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { notificationService } from '../../services/notification.service';
import type { Notification, NotificationType } from '../../types/notification';
import { cn } from '../../lib/utils';
import NotificationPreferencesModal from '../features/NotificationPreferencesModal';

function getNotificationColor(type: NotificationType): string {
  switch (type) {
    case 'TASK_ASSIGNED':
      return 'bg-blue-500';
    case 'TASK_COMPLETED':
    case 'GOAL_COMPLETED':
      return 'bg-green-500';
    case 'TASK_DUE_SOON':
    case 'GOAL_DEADLINE_APPROACHING':
    case 'GOAL_AT_RISK':
      return 'bg-yellow-500';
    case 'TASK_OVERDUE':
      return 'bg-red-500';
    case 'ORGANIZATION_INVITE':
      return 'bg-purple-500';
    case 'ORGANIZATION_ROLE_CHANGED':
    case 'ORGANIZATION_MEMBER_JOINED':
    case 'ORGANIZATION_MEMBER_LEFT':
      return 'bg-indigo-500';
    case 'EVENT_REMINDER':
    case 'EVENT_UPDATED':
      return 'bg-cyan-500';
    case 'EVENT_CANCELLED':
      return 'bg-orange-500';
    case 'MENTION':
      return 'bg-pink-500';
    case 'SYSTEM':
    default:
      return 'bg-gray-500';
  }
}

function formatTimeAgo(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffSec = Math.floor(diffMs / 1000);
  const diffMin = Math.floor(diffSec / 60);
  const diffHour = Math.floor(diffMin / 60);
  const diffDay = Math.floor(diffHour / 24);

  if (diffSec < 60) return 'A l\'instant';
  if (diffMin < 60) return `Il y a ${diffMin} min`;
  if (diffHour < 24) return `Il y a ${diffHour}h`;
  if (diffDay < 7) return `Il y a ${diffDay}j`;
  return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' });
}

export default function NotificationCenter() {
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [showPreferencesModal, setShowPreferencesModal] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const fetchNotifications = async () => {
    try {
      setLoading(true);
      const [notifs, countRes] = await Promise.all([
        notificationService.getNotifications(20),
        notificationService.getUnreadCount(),
      ]);
      setNotifications(notifs);
      setUnreadCount(countRes.unreadCount);
    } catch (error) {
      console.error('Failed to fetch notifications:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();
    // Poll for new notifications every 30 seconds
    const interval = setInterval(fetchNotifications, 30000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen]);

  const handleMarkAsRead = async (notificationId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await notificationService.markAsRead(notificationId);
      setNotifications((prev) =>
        prev.map((n) => (n.id === notificationId ? { ...n, read: true } : n))
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch (error) {
      console.error('Failed to mark notification as read:', error);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch (error) {
      console.error('Failed to mark all notifications as read:', error);
    }
  };

  const handleDelete = async (notificationId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await notificationService.deleteNotification(notificationId);
      const notification = notifications.find((n) => n.id === notificationId);
      setNotifications((prev) => prev.filter((n) => n.id !== notificationId));
      if (notification && !notification.read) {
        setUnreadCount((prev) => Math.max(0, prev - 1));
      }
    } catch (error) {
      console.error('Failed to delete notification:', error);
    }
  };

  const handleNotificationClick = async (notification: Notification) => {
    if (!notification.read) {
      await notificationService.markAsRead(notification.id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === notification.id ? { ...n, read: true } : n))
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    }
    if (notification.link) {
      setIsOpen(false);
      navigate(notification.link);
    }
  };

  return (
    <div ref={dropdownRef} className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative rounded-lg p-2 text-gray-500 hover:bg-light-hover dark:hover:bg-dark-hover hover:text-gray-700 dark:hover:text-gray-300 transition-colors"
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <span className="absolute -top-0.5 -right-0.5 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[10px] font-bold text-white">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 top-full mt-2 w-80 sm:w-96 rounded-xl bg-light-card dark:bg-dark-card border border-gray-200/50 dark:border-white/10 shadow-lg z-50 overflow-hidden">
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200/50 dark:border-white/10">
            <h3 className="font-semibold text-gray-900 dark:text-gray-100">
              Notifications
            </h3>
            <div className="flex items-center gap-2">
              {unreadCount > 0 && (
                <button
                  onClick={handleMarkAllAsRead}
                  className="flex items-center gap-1 text-xs text-blue-500 hover:text-blue-600 transition-colors"
                >
                  <CheckCheck className="h-3.5 w-3.5" />
                  Tout marquer comme lu
                </button>
              )}
              <button
                onClick={() => {
                  setIsOpen(false);
                  setShowPreferencesModal(true);
                }}
                className="rounded p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
                title="Parametres de notification"
              >
                <Settings className="h-4 w-4" />
              </button>
              <button
                onClick={() => setIsOpen(false)}
                className="rounded p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          </div>

          {/* Notification List */}
          <div className="max-h-96 overflow-y-auto">
            {loading && notifications.length === 0 ? (
              <div className="px-4 py-8 text-center text-gray-500">
                Chargement...
              </div>
            ) : notifications.length === 0 ? (
              <div className="px-4 py-8 text-center text-gray-500">
                <Bell className="h-8 w-8 mx-auto mb-2 opacity-50" />
                <p>Aucune notification</p>
              </div>
            ) : (
              notifications.map((notification) => (
                <div
                  key={notification.id}
                  onClick={() => handleNotificationClick(notification)}
                  className={cn(
                    'flex items-start gap-3 px-4 py-3 cursor-pointer transition-colors border-b border-gray-100 dark:border-white/5 last:border-b-0',
                    notification.read
                      ? 'bg-transparent hover:bg-light-hover dark:hover:bg-dark-hover'
                      : 'bg-blue-50/50 dark:bg-blue-500/10 hover:bg-blue-50 dark:hover:bg-blue-500/20'
                  )}
                >
                  {/* Icon */}
                  <div
                    className={cn(
                      'flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center text-white text-xs',
                      getNotificationColor(notification.type)
                    )}
                  >
                    <Bell className="h-4 w-4" />
                  </div>

                  {/* Content */}
                  <div className="flex-1 min-w-0">
                    <p className={cn(
                      'text-sm truncate',
                      notification.read
                        ? 'text-gray-700 dark:text-gray-300'
                        : 'text-gray-900 dark:text-gray-100 font-medium'
                    )}>
                      {notification.title}
                    </p>
                    <p className="text-xs text-gray-500 dark:text-gray-400 line-clamp-2 mt-0.5">
                      {notification.message}
                    </p>
                    <p className="text-[10px] text-gray-400 dark:text-gray-500 mt-1">
                      {formatTimeAgo(notification.createdAt)}
                    </p>
                  </div>

                  {/* Actions */}
                  <div className="flex-shrink-0 flex items-center gap-1">
                    {!notification.read && (
                      <button
                        onClick={(e) => handleMarkAsRead(notification.id, e)}
                        className="rounded p-1 text-gray-400 hover:text-blue-500 hover:bg-blue-50 dark:hover:bg-blue-500/20 transition-colors"
                        title="Marquer comme lu"
                      >
                        <Check className="h-3.5 w-3.5" />
                      </button>
                    )}
                    <button
                      onClick={(e) => handleDelete(notification.id, e)}
                      className="rounded p-1 text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-500/20 transition-colors"
                      title="Supprimer"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* Preferences Modal */}
      <NotificationPreferencesModal
        isOpen={showPreferencesModal}
        onClose={() => setShowPreferencesModal(false)}
      />
    </div>
  );
}
