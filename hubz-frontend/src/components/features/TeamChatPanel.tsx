import { useState, useEffect, useRef, useCallback } from 'react';
import {
  MessageSquare,
  Send,
  Edit2,
  Trash2,
  X,
  MoreVertical,
  ChevronDown,
  Loader2,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { teamChatService } from '../../services/teamChat.service';
import type { ChatMessage } from '../../types/chat';
import { useAuthStore } from '../../stores/authStore';
import Button from '../ui/Button';
import { cn } from '../../lib/utils';

interface TeamChatPanelProps {
  teamId: string;
  teamName: string;
  onClose: () => void;
}

export default function TeamChatPanel({ teamId, teamName, onClose }: TeamChatPanelProps) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [newMessage, setNewMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editContent, setEditContent] = useState('');
  const [hasMore, setHasMore] = useState(true);
  const [page, setPage] = useState(0);
  const [showScrollButton, setShowScrollButton] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const currentUser = useAuthStore((s) => s.user);

  const PAGE_SIZE = 50;
  const POLL_INTERVAL = 5000;

  // Load initial messages
  useEffect(() => {
    loadMessages();
  }, [teamId]);

  // Poll for new messages
  useEffect(() => {
    const interval = setInterval(() => {
      pollNewMessages();
    }, POLL_INTERVAL);
    return () => clearInterval(interval);
  }, [teamId, messages]);

  // Auto-focus input
  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  const loadMessages = async () => {
    try {
      setLoading(true);
      const data = await teamChatService.getMessages(teamId, 0, PAGE_SIZE);
      // Reverse so oldest are first (API returns newest first)
      setMessages(data.content.reverse());
      setHasMore(!data.last);
      setPage(0);
      // Scroll to bottom after initial load
      setTimeout(() => scrollToBottom(), 100);
    } catch (error) {
      console.error('Failed to load messages:', error);
      toast.error('Erreur lors du chargement des messages');
    } finally {
      setLoading(false);
    }
  };

  const pollNewMessages = async () => {
    try {
      const data = await teamChatService.getMessages(teamId, 0, PAGE_SIZE);
      const newMsgs = data.content.reverse();

      // Check if there are truly new messages
      if (newMsgs.length > 0 && messages.length > 0) {
        const lastKnownId = messages[messages.length - 1]?.id;
        const lastNewId = newMsgs[newMsgs.length - 1]?.id;
        if (lastKnownId !== lastNewId) {
          setMessages(newMsgs);
          // Only auto-scroll if user is near the bottom
          if (isNearBottom()) {
            setTimeout(() => scrollToBottom(), 100);
          }
        }
      } else if (newMsgs.length > 0 && messages.length === 0) {
        setMessages(newMsgs);
        setTimeout(() => scrollToBottom(), 100);
      }
    } catch {
      // Silent polling failure
    }
  };

  const loadOlderMessages = async () => {
    if (loadingMore || !hasMore) return;

    try {
      setLoadingMore(true);
      const nextPage = page + 1;
      const data = await teamChatService.getMessages(teamId, nextPage, PAGE_SIZE);
      const olderMessages = data.content.reverse();
      setMessages((prev) => [...olderMessages, ...prev]);
      setHasMore(!data.last);
      setPage(nextPage);
    } catch (error) {
      console.error('Failed to load older messages:', error);
      toast.error('Erreur lors du chargement des anciens messages');
    } finally {
      setLoadingMore(false);
    }
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const isNearBottom = (): boolean => {
    const container = messagesContainerRef.current;
    if (!container) return true;
    const threshold = 150;
    return container.scrollHeight - container.scrollTop - container.clientHeight < threshold;
  };

  const handleScroll = useCallback(() => {
    const container = messagesContainerRef.current;
    if (!container) return;

    // Show scroll-to-bottom button when not near bottom
    setShowScrollButton(!isNearBottom());

    // Load older messages when scrolled to top
    if (container.scrollTop === 0 && hasMore && !loadingMore) {
      loadOlderMessages();
    }
  }, [hasMore, loadingMore]);

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newMessage.trim() || submitting) return;

    setSubmitting(true);
    try {
      const sent = await teamChatService.sendMessage(teamId, {
        content: newMessage.trim(),
      });
      setMessages((prev) => [...prev, sent]);
      setNewMessage('');
      setTimeout(() => scrollToBottom(), 100);
      inputRef.current?.focus();
    } catch (error) {
      console.error('Failed to send message:', error);
      toast.error("Erreur lors de l'envoi du message");
    } finally {
      setSubmitting(false);
    }
  };

  const handleEdit = async (messageId: string) => {
    if (!editContent.trim()) return;

    try {
      const updated = await teamChatService.editMessage(teamId, messageId, {
        content: editContent.trim(),
      });
      setMessages((prev) =>
        prev.map((m) => (m.id === messageId ? updated : m))
      );
      setEditingId(null);
      setEditContent('');
      toast.success('Message modifie');
    } catch (error) {
      console.error('Failed to edit message:', error);
      toast.error('Erreur lors de la modification');
    }
  };

  const handleDelete = async (messageId: string) => {
    if (!confirm('Supprimer ce message ?')) return;

    try {
      await teamChatService.deleteMessage(teamId, messageId);
      // Update locally to show as deleted
      setMessages((prev) =>
        prev.map((m) =>
          m.id === messageId
            ? { ...m, content: 'Ce message a ete supprime.', deleted: true }
            : m
        )
      );
      toast.success('Message supprime');
    } catch (error) {
      console.error('Failed to delete message:', error);
      toast.error('Erreur lors de la suppression');
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend(e);
    }
  };

  const formatMessageDate = (dateString: string): string => {
    const date = new Date(dateString);
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const yesterday = new Date(today.getTime() - 86400000);
    const messageDay = new Date(date.getFullYear(), date.getMonth(), date.getDate());

    if (messageDay.getTime() === today.getTime()) {
      return "Aujourd'hui";
    } else if (messageDay.getTime() === yesterday.getTime()) {
      return 'Hier';
    } else {
      return date.toLocaleDateString('fr-FR', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        year: date.getFullYear() !== now.getFullYear() ? 'numeric' : undefined,
      });
    }
  };

  const formatTime = (dateString: string): string => {
    return new Date(dateString).toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // Group messages by date
  const groupedMessages = messages.reduce<{ date: string; messages: ChatMessage[] }[]>(
    (groups, message) => {
      const dateLabel = formatMessageDate(message.createdAt);
      const lastGroup = groups[groups.length - 1];
      if (lastGroup && lastGroup.date === dateLabel) {
        lastGroup.messages.push(message);
      } else {
        groups.push({ date: dateLabel, messages: [message] });
      }
      return groups;
    },
    []
  );

  const getProfilePhotoUrl = (url?: string | null) => {
    if (!url) return null;
    if (url.startsWith('http')) return url;
    return `/uploads/${url}`;
  };

  return (
    <div className="flex h-full flex-col bg-white dark:bg-dark-card rounded-xl border border-gray-200/50 dark:border-white/10 overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-gray-200/50 dark:border-white/10 px-4 py-3">
        <div className="flex items-center gap-2">
          <MessageSquare className="h-5 w-5 text-accent" />
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">
            Chat - {teamName}
          </h3>
          <span className="text-xs text-gray-500 dark:text-gray-400">
            ({messages.length} messages)
          </span>
        </div>
        <button
          onClick={onClose}
          className="rounded-lg p-1 text-gray-400 hover:bg-gray-100 dark:hover:bg-dark-hover hover:text-gray-600 dark:hover:text-gray-300"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      {/* Messages area */}
      <div
        ref={messagesContainerRef}
        onScroll={handleScroll}
        className="flex-1 overflow-y-auto px-4 py-3 space-y-1"
      >
        {loading ? (
          <div className="flex h-full items-center justify-center">
            <Loader2 className="h-6 w-6 animate-spin text-accent" />
          </div>
        ) : messages.length === 0 ? (
          <div className="flex h-full flex-col items-center justify-center text-gray-500 dark:text-gray-400">
            <MessageSquare className="h-12 w-12 mb-3 opacity-50" />
            <p className="text-sm font-medium">Aucun message</p>
            <p className="text-xs mt-1">Commencez la conversation !</p>
          </div>
        ) : (
          <>
            {/* Load more button */}
            {hasMore && (
              <div className="flex justify-center py-2">
                <button
                  onClick={loadOlderMessages}
                  disabled={loadingMore}
                  className="text-xs text-accent hover:underline disabled:opacity-50"
                >
                  {loadingMore ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    'Charger les anciens messages'
                  )}
                </button>
              </div>
            )}

            {/* Grouped messages */}
            {groupedMessages.map((group) => (
              <div key={group.date}>
                {/* Date separator */}
                <div className="flex items-center gap-3 py-3">
                  <div className="flex-1 border-t border-gray-200 dark:border-gray-700" />
                  <span className="text-xs font-medium text-gray-500 dark:text-gray-400 whitespace-nowrap">
                    {group.date}
                  </span>
                  <div className="flex-1 border-t border-gray-200 dark:border-gray-700" />
                </div>

                {/* Messages */}
                {group.messages.map((message) => (
                  <MessageBubble
                    key={message.id}
                    message={message}
                    isOwn={message.userId === currentUser?.id}
                    isEditing={editingId === message.id}
                    editContent={editContent}
                    onStartEdit={() => {
                      setEditingId(message.id);
                      setEditContent(message.content);
                    }}
                    onCancelEdit={() => {
                      setEditingId(null);
                      setEditContent('');
                    }}
                    onEditContentChange={setEditContent}
                    onSaveEdit={() => handleEdit(message.id)}
                    onDelete={() => handleDelete(message.id)}
                    formatTime={formatTime}
                    getProfilePhotoUrl={getProfilePhotoUrl}
                  />
                ))}
              </div>
            ))}
            <div ref={messagesEndRef} />
          </>
        )}
      </div>

      {/* Scroll to bottom button */}
      {showScrollButton && (
        <div className="absolute bottom-20 right-6">
          <button
            onClick={scrollToBottom}
            className="rounded-full bg-accent p-2 text-white shadow-lg hover:bg-blue-600 transition-colors"
          >
            <ChevronDown className="h-4 w-4" />
          </button>
        </div>
      )}

      {/* Input area */}
      <form
        onSubmit={handleSend}
        className="border-t border-gray-200/50 dark:border-white/10 px-4 py-3"
      >
        <div className="flex items-end gap-2">
          <textarea
            ref={inputRef}
            value={newMessage}
            onChange={(e) => setNewMessage(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ecrivez un message... (Entree pour envoyer)"
            rows={1}
            className="flex-1 resize-none rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 placeholder-gray-400 focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent dark:border-gray-600 dark:bg-dark-hover dark:text-gray-100 dark:placeholder-gray-500 max-h-24"
            style={{ minHeight: '38px' }}
          />
          <Button
            type="submit"
            size="sm"
            loading={submitting}
            disabled={!newMessage.trim()}
          >
            <Send className="h-4 w-4" />
          </Button>
        </div>
      </form>
    </div>
  );
}

// --- MessageBubble sub-component ---

interface MessageBubbleProps {
  message: ChatMessage;
  isOwn: boolean;
  isEditing: boolean;
  editContent: string;
  onStartEdit: () => void;
  onCancelEdit: () => void;
  onEditContentChange: (value: string) => void;
  onSaveEdit: () => void;
  onDelete: () => void;
  formatTime: (date: string) => string;
  getProfilePhotoUrl: (url?: string | null) => string | null;
}

function MessageBubble({
  message,
  isOwn,
  isEditing,
  editContent,
  onStartEdit,
  onCancelEdit,
  onEditContentChange,
  onSaveEdit,
  onDelete,
  formatTime,
  getProfilePhotoUrl,
}: MessageBubbleProps) {
  const [showActions, setShowActions] = useState(false);

  if (message.deleted) {
    return (
      <div className="flex items-start gap-2 py-1.5">
        <div className="h-8 w-8 shrink-0" />
        <div className="text-xs italic text-gray-400 dark:text-gray-500 py-1">
          {message.content}
        </div>
      </div>
    );
  }

  const photoUrl = getProfilePhotoUrl(message.authorProfilePhotoUrl);

  return (
    <div className={cn('group flex items-start gap-2 py-1.5', isOwn && 'flex-row-reverse')}>
      {/* Avatar */}
      <div className="shrink-0">
        {photoUrl ? (
          <img
            src={photoUrl}
            alt={message.authorName}
            className="h-8 w-8 rounded-full object-cover"
          />
        ) : (
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-accent/20">
            <span className="text-xs font-medium text-accent">
              {message.authorName.charAt(0).toUpperCase()}
            </span>
          </div>
        )}
      </div>

      {/* Message content */}
      <div className={cn('max-w-[75%] min-w-0', isOwn && 'items-end')}>
        {/* Author name & time */}
        <div className={cn('flex items-center gap-2 mb-0.5', isOwn && 'flex-row-reverse')}>
          <span className="text-xs font-medium text-gray-700 dark:text-gray-300">
            {message.authorName}
          </span>
          <span className="text-xs text-gray-400 dark:text-gray-500">
            {formatTime(message.createdAt)}
            {message.edited && ' (modifie)'}
          </span>
        </div>

        {/* Bubble */}
        <div className="relative">
          {isEditing ? (
            <div className="space-y-2">
              <textarea
                value={editContent}
                onChange={(e) => onEditContentChange(e.target.value)}
                className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent dark:border-gray-600 dark:bg-dark-hover dark:text-gray-100"
                rows={2}
                autoFocus
              />
              <div className="flex gap-2">
                <Button size="sm" onClick={onSaveEdit}>
                  Enregistrer
                </Button>
                <Button size="sm" variant="ghost" onClick={onCancelEdit}>
                  Annuler
                </Button>
              </div>
            </div>
          ) : (
            <div
              className={cn(
                'rounded-xl px-3 py-2 text-sm whitespace-pre-wrap break-words',
                isOwn
                  ? 'bg-accent text-white rounded-tr-sm'
                  : 'bg-gray-100 dark:bg-dark-hover text-gray-900 dark:text-gray-100 rounded-tl-sm'
              )}
            >
              {message.content}
            </div>
          )}

          {/* Actions menu (only on hover, only for non-editing) */}
          {!isEditing && isOwn && (
            <div className={cn(
              'absolute top-0 opacity-0 group-hover:opacity-100 transition-opacity',
              isOwn ? '-left-8' : '-right-8'
            )}>
              <div className="relative">
                <button
                  onClick={() => setShowActions(!showActions)}
                  className="p-1 rounded hover:bg-gray-200 dark:hover:bg-gray-600"
                >
                  <MoreVertical className="h-3.5 w-3.5 text-gray-400" />
                </button>

                {showActions && (
                  <>
                    <div
                      className="fixed inset-0 z-10"
                      onClick={() => setShowActions(false)}
                    />
                    <div className="absolute right-0 top-full mt-1 z-20 bg-white dark:bg-dark-card rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 py-1 min-w-[120px]">
                      <button
                        onClick={() => {
                          onStartEdit();
                          setShowActions(false);
                        }}
                        className="w-full flex items-center gap-2 px-3 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-dark-hover"
                      >
                        <Edit2 className="h-3.5 w-3.5" />
                        Modifier
                      </button>
                      <button
                        onClick={() => {
                          onDelete();
                          setShowActions(false);
                        }}
                        className="w-full flex items-center gap-2 px-3 py-2 text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                        Supprimer
                      </button>
                    </div>
                  </>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
