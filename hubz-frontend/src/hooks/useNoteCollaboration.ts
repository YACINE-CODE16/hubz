import { useCallback, useEffect, useRef, useState } from 'react';
import { StompSubscription } from '@stomp/stompjs';
import { useWebSocket } from './useWebSocket';
import type {
  NoteSession,
  NoteCollaborator,
  NoteCursor,
  NoteEdit,
  NoteCollaborationEvent,
  EditType,
} from '../types/collaboration';

interface UseNoteCollaborationOptions {
  noteId: string;
  onEdit?: (edit: NoteEdit) => void;
  onCursor?: (cursor: NoteCursor) => void;
  onCollaboratorJoin?: (collaborator: NoteCollaborator) => void;
  onCollaboratorLeave?: (collaborator: NoteCollaborator) => void;
  onTyping?: (collaborator: NoteCollaborator, isTyping: boolean) => void;
  onConflict?: (edit: NoteEdit) => void;
}

interface UseNoteCollaborationReturn {
  session: NoteSession | null;
  collaborators: NoteCollaborator[];
  cursors: NoteCursor[];
  typingUsers: NoteCollaborator[];
  isConnected: boolean;
  version: number;
  joinNote: () => void;
  leaveNote: () => void;
  sendEdit: (type: EditType, title?: string, content?: string) => void;
  sendCursor: (position: number, selectionStart?: number, selectionEnd?: number) => void;
  sendTyping: (isTyping: boolean) => void;
}

export function useNoteCollaboration(options: UseNoteCollaborationOptions): UseNoteCollaborationReturn {
  const {
    noteId,
    onEdit,
    onCursor,
    onCollaboratorJoin,
    onCollaboratorLeave,
    onTyping,
    onConflict,
  } = options;

  const [session, setSession] = useState<NoteSession | null>(null);
  const [collaborators, setCollaborators] = useState<NoteCollaborator[]>([]);
  const [cursors, setCursors] = useState<NoteCursor[]>([]);
  const [typingUsers, setTypingUsers] = useState<NoteCollaborator[]>([]);
  const [version, setVersion] = useState(0);

  const subscriptionsRef = useRef<StompSubscription[]>([]);
  const typingTimeoutsRef = useRef<Map<string, NodeJS.Timeout>>(new Map());
  const hasJoinedRef = useRef(false);

  const { isConnected, subscribe, unsubscribe, send } = useWebSocket({
    autoConnect: true,
  });

  // Join note session
  const joinNote = useCallback(() => {
    if (!isConnected || hasJoinedRef.current) return;

    send('/app/note/join', { noteId });
    hasJoinedRef.current = true;
  }, [isConnected, noteId, send]);

  // Leave note session
  const leaveNote = useCallback(() => {
    if (!isConnected || !hasJoinedRef.current) return;

    send('/app/note/leave', { noteId });
    hasJoinedRef.current = false;

    // Clear state
    setSession(null);
    setCollaborators([]);
    setCursors([]);
    setTypingUsers([]);
  }, [isConnected, noteId, send]);

  // Send edit
  const sendEdit = useCallback(
    (type: EditType, title?: string, content?: string) => {
      if (!isConnected || !hasJoinedRef.current) return;

      send('/app/note/edit', {
        noteId,
        type,
        title,
        content,
        baseVersion: version,
      });
    },
    [isConnected, noteId, version, send]
  );

  // Send cursor position
  const sendCursor = useCallback(
    (position: number, selectionStart?: number, selectionEnd?: number) => {
      if (!isConnected || !hasJoinedRef.current) return;

      send('/app/note/cursor', {
        noteId,
        position,
        selectionStart,
        selectionEnd,
      });
    },
    [isConnected, noteId, send]
  );

  // Send typing indicator
  const sendTyping = useCallback(
    (isTyping: boolean) => {
      if (!isConnected || !hasJoinedRef.current) return;

      if (isTyping) {
        send('/app/note/typing', { noteId });
      } else {
        send('/app/note/stopped-typing', { noteId });
      }
    },
    [isConnected, noteId, send]
  );

  // Handle session response (after joining)
  const handleSessionResponse = useCallback((sessionData: NoteSession) => {
    setSession(sessionData);
    setCollaborators(sessionData.collaborators);
    setCursors(sessionData.cursors);
    setVersion(sessionData.version);
  }, []);

  // Handle edit broadcast
  const handleEdit = useCallback(
    (edit: NoteEdit) => {
      if (edit.hasConflict) {
        onConflict?.(edit);
        return;
      }

      setVersion(edit.version);
      onEdit?.(edit);
    },
    [onEdit, onConflict]
  );

  // Handle cursor update
  const handleCursor = useCallback(
    (cursor: NoteCursor) => {
      setCursors((prev) => {
        const filtered = prev.filter((c) => c.userId !== cursor.userId);
        return [...filtered, cursor];
      });
      onCursor?.(cursor);
    },
    [onCursor]
  );

  // Handle collaboration events
  const handleEvent = useCallback(
    (event: NoteCollaborationEvent) => {
      switch (event.eventType) {
        case 'USER_JOINED':
          setCollaborators((prev) => {
            if (prev.some((c) => c.userId === event.collaborator.userId)) {
              return prev;
            }
            return [...prev, event.collaborator];
          });
          onCollaboratorJoin?.(event.collaborator);
          break;

        case 'USER_LEFT':
          setCollaborators((prev) => prev.filter((c) => c.userId !== event.collaborator.userId));
          setCursors((prev) => prev.filter((c) => c.userId !== event.collaborator.userId));
          setTypingUsers((prev) => prev.filter((u) => u.userId !== event.collaborator.userId));
          onCollaboratorLeave?.(event.collaborator);
          break;

        case 'USER_TYPING':
          setTypingUsers((prev) => {
            if (prev.some((u) => u.userId === event.collaborator.userId)) {
              return prev;
            }
            return [...prev, event.collaborator];
          });
          onTyping?.(event.collaborator, true);

          // Auto-remove typing indicator after 3 seconds
          const existingTimeout = typingTimeoutsRef.current.get(event.collaborator.userId);
          if (existingTimeout) {
            clearTimeout(existingTimeout);
          }
          const timeout = setTimeout(() => {
            setTypingUsers((prev) => prev.filter((u) => u.userId !== event.collaborator.userId));
            typingTimeoutsRef.current.delete(event.collaborator.userId);
          }, 3000);
          typingTimeoutsRef.current.set(event.collaborator.userId, timeout);
          break;

        case 'USER_STOPPED_TYPING':
          setTypingUsers((prev) => prev.filter((u) => u.userId !== event.collaborator.userId));
          onTyping?.(event.collaborator, false);

          // Clear timeout if exists
          const stoppedTimeout = typingTimeoutsRef.current.get(event.collaborator.userId);
          if (stoppedTimeout) {
            clearTimeout(stoppedTimeout);
            typingTimeoutsRef.current.delete(event.collaborator.userId);
          }
          break;
      }
    },
    [onCollaboratorJoin, onCollaboratorLeave, onTyping]
  );

  // Set up subscriptions when connected
  useEffect(() => {
    if (!isConnected || !noteId) return;

    // Subscribe to session response (user-specific queue)
    const sessionSub = subscribe<NoteSession>('/user/queue/note/session', handleSessionResponse);
    if (sessionSub) subscriptionsRef.current.push(sessionSub);

    // Subscribe to edits
    const editsSub = subscribe<NoteEdit>(`/topic/note/${noteId}/edits`, handleEdit);
    if (editsSub) subscriptionsRef.current.push(editsSub);

    // Subscribe to cursors
    const cursorsSub = subscribe<NoteCursor>(`/topic/note/${noteId}/cursors`, handleCursor);
    if (cursorsSub) subscriptionsRef.current.push(cursorsSub);

    // Subscribe to events (join/leave/typing)
    const eventsSub = subscribe<NoteCollaborationEvent>(
      `/topic/note/${noteId}/events`,
      handleEvent
    );
    if (eventsSub) subscriptionsRef.current.push(eventsSub);

    // Subscribe to errors (user-specific)
    const errorsSub = subscribe<NoteEdit>('/user/queue/note/errors', (error) => {
      if (error.hasConflict) {
        onConflict?.(error);
      }
    });
    if (errorsSub) subscriptionsRef.current.push(errorsSub);

    // Auto-join the note
    joinNote();

    return () => {
      // Leave note and unsubscribe
      leaveNote();
      subscriptionsRef.current.forEach((sub) => unsubscribe(sub));
      subscriptionsRef.current = [];

      // Clear typing timeouts
      typingTimeoutsRef.current.forEach((timeout) => clearTimeout(timeout));
      typingTimeoutsRef.current.clear();
    };
  }, [
    isConnected,
    noteId,
    subscribe,
    unsubscribe,
    joinNote,
    leaveNote,
    handleSessionResponse,
    handleEdit,
    handleCursor,
    handleEvent,
    onConflict,
  ]);

  return {
    session,
    collaborators,
    cursors,
    typingUsers,
    isConnected,
    version,
    joinNote,
    leaveNote,
    sendEdit,
    sendCursor,
    sendTyping,
  };
}
