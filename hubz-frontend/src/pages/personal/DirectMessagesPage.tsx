import { useEffect, useState, useRef, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Send,
  MessageSquare,
  Check,
  CheckCheck,
  Pencil,
  Trash2,
  X,
  ArrowDown,
  Loader2,
} from 'lucide-react';
import { toast } from 'react-hot-toast';
import Card from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import { cn } from '../../lib/utils';
import { directMessageService } from '../../services/directMessage.service';
import { useAuthStore } from '../../stores/authStore';
import type { Conversation, DirectMessage } from '../../types/directMessage';

function formatMessageTime(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays === 0) {
    return date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  } else if (diffDays === 1) {
    return 'Hier ' + date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  } else if (diffDays < 7) {
    return date.toLocaleDateString('fr-FR', { weekday: 'short' }) +
      ' ' + date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }
  return date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' }) +
    ' ' + date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
}

function formatConversationTime(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays === 0) {
    return date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  } else if (diffDays === 1) {
    return 'Hier';
  } else if (diffDays < 7) {
    return date.toLocaleDateString('fr-FR', { weekday: 'short' });
  }
  return date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' });
}

function UserAvatar({ name, photoUrl, size = 'md' }: { name: string; photoUrl: string | null; size?: 'sm' | 'md' | 'lg' }) {
  const sizeClasses = {
    sm: 'h-8 w-8 text-xs',
    md: 'h-10 w-10 text-sm',
    lg: 'h-12 w-12 text-base',
  };

  const getPhotoUrl = () => {
    if (!photoUrl) return null;
    if (photoUrl.startsWith('http')) return photoUrl;
    return `/uploads/${photoUrl}`;
  };

  const initials = name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);

  const url = getPhotoUrl();

  if (url) {
    return (
      <img
        src={url}
        alt={name}
        className={cn('rounded-full object-cover shrink-0', sizeClasses[size])}
      />
    );
  }

  return (
    <div
      className={cn(
        'flex items-center justify-center rounded-full bg-accent/20 text-accent font-semibold shrink-0',
        sizeClasses[size],
      )}
    >
      {initials}
    </div>
  );
}

export default function DirectMessagesPage() {
  const { user } = useAuthStore();
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedUserId = searchParams.get('user');

  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [messages, setMessages] = useState<DirectMessage[]>([]);
  const [loading, setLoading] = useState(true);
  const [messagesLoading, setMessagesLoading] = useState(false);
  const [newMessage, setNewMessage] = useState('');
  const [sending, setSending] = useState(false);
  const [editingMessageId, setEditingMessageId] = useState<string | null>(null);
  const [editContent, setEditContent] = useState('');
  const [showScrollButton, setShowScrollButton] = useState(false);
  const [hasMore, setHasMore] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  // Fetch conversations list
  const fetchConversations = useCallback(async () => {
    try {
      const data = await directMessageService.getConversations();
      setConversations(data);
    } catch {
      // Silently handle error for polling
    }
  }, []);

  // Fetch messages for selected conversation
  const fetchMessages = useCallback(async (userId: string, page = 0, append = false) => {
    try {
      if (page === 0) setMessagesLoading(true);
      const data = await directMessageService.getConversation(userId, page, 50);
      const reversed = [...data.content].reverse();

      if (append) {
        setMessages((prev) => [...reversed, ...prev]);
      } else {
        setMessages(reversed);
      }
      setHasMore(!data.last);
      setCurrentPage(page);
    } catch {
      toast.error('Erreur lors du chargement des messages');
    } finally {
      setMessagesLoading(false);
    }
  }, []);

  // Initial load
  useEffect(() => {
    const init = async () => {
      setLoading(true);
      await fetchConversations();
      setLoading(false);
    };
    init();
  }, [fetchConversations]);

  // Load messages when conversation is selected
  useEffect(() => {
    if (selectedUserId) {
      fetchMessages(selectedUserId);
      // Mark conversation as read
      directMessageService.markConversationAsRead(selectedUserId).catch(() => {});
    } else {
      setMessages([]);
    }
  }, [selectedUserId, fetchMessages]);

  // Polling: refresh conversations and messages every 5 seconds
  useEffect(() => {
    const interval = setInterval(() => {
      fetchConversations();
      if (selectedUserId) {
        fetchMessages(selectedUserId);
      }
    }, 5000);
    return () => clearInterval(interval);
  }, [fetchConversations, fetchMessages, selectedUserId]);

  // Auto-scroll to bottom when new messages arrive
  useEffect(() => {
    if (messagesEndRef.current && !showScrollButton) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, showScrollButton]);

  // Detect scroll position for scroll-to-bottom button
  const handleScroll = () => {
    const container = messagesContainerRef.current;
    if (!container) return;
    const isNearBottom = container.scrollHeight - container.scrollTop - container.clientHeight < 100;
    setShowScrollButton(!isNearBottom);
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const selectConversation = (userId: string) => {
    setSearchParams({ user: userId });
  };

  const handleSend = async () => {
    if (!newMessage.trim() || !selectedUserId || sending) return;

    try {
      setSending(true);
      await directMessageService.sendMessage({
        receiverId: selectedUserId,
        content: newMessage.trim(),
      });
      setNewMessage('');
      await fetchMessages(selectedUserId);
      await fetchConversations();
      inputRef.current?.focus();
    } catch {
      toast.error('Erreur lors de l\'envoi du message');
    } finally {
      setSending(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleEdit = async (messageId: string) => {
    if (!editContent.trim()) return;
    try {
      await directMessageService.editMessage(messageId, { content: editContent.trim() });
      setEditingMessageId(null);
      setEditContent('');
      if (selectedUserId) await fetchMessages(selectedUserId);
    } catch {
      toast.error('Erreur lors de la modification');
    }
  };

  const handleDelete = async (messageId: string) => {
    try {
      await directMessageService.deleteMessage(messageId);
      if (selectedUserId) await fetchMessages(selectedUserId);
      await fetchConversations();
    } catch {
      toast.error('Erreur lors de la suppression');
    }
  };

  const loadOlderMessages = async () => {
    if (!selectedUserId || !hasMore) return;
    await fetchMessages(selectedUserId, currentPage + 1, true);
  };

  const selectedConversation = conversations.find((c) => c.userId === selectedUserId);

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-accent" />
      </div>
    );
  }

  return (
    <div className="flex h-[calc(100vh-8rem)] gap-4">
      {/* Conversations List (left panel) */}
      <Card
        className={cn(
          'flex w-80 shrink-0 flex-col overflow-hidden',
          selectedUserId ? 'hidden lg:flex' : 'flex w-full lg:w-80',
        )}
      >
        <div className="border-b border-gray-200/50 dark:border-white/10 p-4">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
            Messages
          </h2>
        </div>

        <div className="flex-1 overflow-y-auto">
          {conversations.length === 0 ? (
            <div className="flex flex-col items-center justify-center p-8 text-center">
              <MessageSquare className="h-12 w-12 text-gray-400 dark:text-gray-600 mb-3" />
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Aucune conversation
              </p>
              <p className="text-xs text-gray-400 dark:text-gray-500 mt-1">
                Envoyez un message depuis la page Membres
              </p>
            </div>
          ) : (
            conversations.map((conv) => (
              <button
                key={conv.userId}
                onClick={() => selectConversation(conv.userId)}
                className={cn(
                  'flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-light-hover dark:hover:bg-dark-hover',
                  selectedUserId === conv.userId && 'bg-accent/10 dark:bg-accent/10',
                )}
              >
                <UserAvatar name={conv.userName} photoUrl={conv.userProfilePhotoUrl} />
                <div className="min-w-0 flex-1">
                  <div className="flex items-center justify-between">
                    <span className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">
                      {conv.userName}
                    </span>
                    <span className="ml-2 shrink-0 text-xs text-gray-400 dark:text-gray-500">
                      {formatConversationTime(conv.lastMessageAt)}
                    </span>
                  </div>
                  <div className="flex items-center justify-between mt-0.5">
                    <p className="truncate text-xs text-gray-500 dark:text-gray-400">
                      {conv.lastMessageSenderId === user?.id ? 'Vous: ' : ''}
                      {conv.lastMessageContent}
                    </p>
                    {conv.unreadCount > 0 && (
                      <span className="ml-2 flex h-5 min-w-[1.25rem] shrink-0 items-center justify-center rounded-full bg-accent px-1.5 text-[10px] font-bold text-white">
                        {conv.unreadCount}
                      </span>
                    )}
                  </div>
                </div>
              </button>
            ))
          )}
        </div>
      </Card>

      {/* Conversation Panel (right panel) */}
      <Card
        className={cn(
          'flex flex-1 flex-col overflow-hidden',
          !selectedUserId && 'hidden lg:flex',
        )}
      >
        {selectedUserId ? (
          <>
            {/* Conversation Header */}
            <div className="flex items-center gap-3 border-b border-gray-200/50 dark:border-white/10 px-4 py-3">
              <button
                onClick={() => setSearchParams({})}
                className="lg:hidden mr-1 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
              >
                <X className="h-5 w-5" />
              </button>
              {selectedConversation && (
                <>
                  <UserAvatar
                    name={selectedConversation.userName}
                    photoUrl={selectedConversation.userProfilePhotoUrl}
                  />
                  <div>
                    <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                      {selectedConversation.userName}
                    </h3>
                  </div>
                </>
              )}
            </div>

            {/* Messages */}
            <div
              ref={messagesContainerRef}
              onScroll={handleScroll}
              className="relative flex-1 overflow-y-auto px-4 py-4 space-y-1"
            >
              {messagesLoading ? (
                <div className="flex items-center justify-center h-full">
                  <Loader2 className="h-6 w-6 animate-spin text-accent" />
                </div>
              ) : (
                <>
                  {hasMore && (
                    <div className="flex justify-center pb-3">
                      <Button variant="ghost" size="sm" onClick={loadOlderMessages}>
                        Charger les messages precedents
                      </Button>
                    </div>
                  )}

                  {messages.length === 0 ? (
                    <div className="flex flex-col items-center justify-center h-full text-center">
                      <MessageSquare className="h-10 w-10 text-gray-400 dark:text-gray-600 mb-2" />
                      <p className="text-sm text-gray-500 dark:text-gray-400">
                        Commencez la conversation
                      </p>
                    </div>
                  ) : (
                    messages.map((msg) => {
                      const isMine = msg.senderId === user?.id;
                      const isEditing = editingMessageId === msg.id;

                      return (
                        <div
                          key={msg.id}
                          className={cn(
                            'group flex gap-2 py-1',
                            isMine ? 'flex-row-reverse' : 'flex-row',
                          )}
                        >
                          <div
                            className={cn(
                              'max-w-[70%] rounded-2xl px-4 py-2 text-sm',
                              msg.deleted
                                ? 'bg-gray-100 dark:bg-gray-800 text-gray-400 dark:text-gray-500 italic'
                                : isMine
                                  ? 'bg-accent text-white'
                                  : 'bg-gray-100 dark:bg-dark-hover text-gray-900 dark:text-gray-100',
                            )}
                          >
                            {isEditing ? (
                              <div className="space-y-2">
                                <textarea
                                  value={editContent}
                                  onChange={(e) => setEditContent(e.target.value)}
                                  className="w-full rounded-lg border border-white/30 bg-white/20 px-2 py-1 text-sm text-white placeholder:text-white/60 focus:outline-none"
                                  rows={2}
                                  autoFocus
                                />
                                <div className="flex gap-1 justify-end">
                                  <button
                                    onClick={() => { setEditingMessageId(null); setEditContent(''); }}
                                    className="rounded p-1 hover:bg-white/20"
                                  >
                                    <X className="h-3.5 w-3.5" />
                                  </button>
                                  <button
                                    onClick={() => handleEdit(msg.id)}
                                    className="rounded p-1 hover:bg-white/20"
                                  >
                                    <Check className="h-3.5 w-3.5" />
                                  </button>
                                </div>
                              </div>
                            ) : (
                              <>
                                <p className="whitespace-pre-wrap break-words">{msg.content}</p>
                                <div
                                  className={cn(
                                    'mt-1 flex items-center gap-1 text-[10px]',
                                    msg.deleted
                                      ? 'text-gray-400 dark:text-gray-500'
                                      : isMine
                                        ? 'text-white/70 justify-end'
                                        : 'text-gray-400 dark:text-gray-500',
                                  )}
                                >
                                  <span>{formatMessageTime(msg.createdAt)}</span>
                                  {msg.edited && !msg.deleted && (
                                    <span className="italic">(modifie)</span>
                                  )}
                                  {isMine && !msg.deleted && (
                                    msg.read ? (
                                      <CheckCheck className="h-3 w-3" />
                                    ) : (
                                      <Check className="h-3 w-3" />
                                    )
                                  )}
                                </div>
                              </>
                            )}
                          </div>

                          {/* Actions (edit/delete) for own non-deleted messages */}
                          {isMine && !msg.deleted && !isEditing && (
                            <div className="flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
                              <button
                                onClick={() => {
                                  setEditingMessageId(msg.id);
                                  setEditContent(msg.content);
                                }}
                                className="rounded p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-light-hover dark:hover:bg-dark-hover"
                                title="Modifier"
                              >
                                <Pencil className="h-3.5 w-3.5" />
                              </button>
                              <button
                                onClick={() => handleDelete(msg.id)}
                                className="rounded p-1 text-gray-400 hover:text-red-500 hover:bg-light-hover dark:hover:bg-dark-hover"
                                title="Supprimer"
                              >
                                <Trash2 className="h-3.5 w-3.5" />
                              </button>
                            </div>
                          )}
                        </div>
                      );
                    })
                  )}

                  <div ref={messagesEndRef} />
                </>
              )}

              {/* Scroll to bottom button */}
              {showScrollButton && (
                <button
                  onClick={scrollToBottom}
                  className="absolute bottom-4 right-4 flex h-8 w-8 items-center justify-center rounded-full bg-accent text-white shadow-lg hover:bg-blue-600 transition-colors"
                >
                  <ArrowDown className="h-4 w-4" />
                </button>
              )}
            </div>

            {/* Message Input */}
            <div className="border-t border-gray-200/50 dark:border-white/10 p-3">
              <div className="flex items-end gap-2">
                <textarea
                  ref={inputRef}
                  value={newMessage}
                  onChange={(e) => setNewMessage(e.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder="Ecrivez un message..."
                  className="flex-1 resize-none rounded-xl border border-gray-200 dark:border-white/10 bg-white/60 dark:bg-white/5 backdrop-blur-sm px-4 py-2.5 text-sm text-gray-900 dark:text-gray-100 placeholder:text-gray-400 dark:placeholder:text-gray-500 focus:border-accent dark:focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/20"
                  rows={1}
                  style={{ maxHeight: '120px' }}
                />
                <Button
                  onClick={handleSend}
                  disabled={!newMessage.trim() || sending}
                  loading={sending}
                  size="md"
                  className="shrink-0 rounded-xl"
                >
                  <Send className="h-4 w-4" />
                </Button>
              </div>
            </div>
          </>
        ) : (
          <div className="flex flex-col items-center justify-center h-full text-center p-8">
            <MessageSquare className="h-16 w-16 text-gray-300 dark:text-gray-600 mb-4" />
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">
              Messagerie directe
            </h3>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Selectionnez une conversation ou envoyez un message depuis la page Membres
            </p>
          </div>
        )}
      </Card>
    </div>
  );
}
