import { useCallback, useEffect, useRef, useState } from 'react';
import { Wifi, WifiOff, AlertTriangle, RefreshCw } from 'lucide-react';
import toast from 'react-hot-toast';
import { useNoteCollaboration } from '../../hooks/useNoteCollaboration';
import NoteCollaborators from './NoteCollaborators';
import WysiwygEditor from '../ui/WysiwygEditor';
import type { Note, NoteTag, NoteFolder } from '../../types/note';
import type { NoteEdit, EditType } from '../../types/collaboration';
import { cn } from '../../lib/utils';

interface CollaborativeNoteEditorProps {
  note: Note;
  noteTags: NoteTag[];
  folders: NoteFolder[];
  onSave: (title: string, content: string, category?: string, folderId?: string, tagIds?: string[]) => Promise<void>;
  onClose: () => void;
}

export default function CollaborativeNoteEditor({
  note,
  noteTags,
  folders,
  onSave,
  onClose,
}: CollaborativeNoteEditorProps) {
  const [title, setTitle] = useState(note.title);
  const [content, setContent] = useState(note.content);
  const [category, setCategory] = useState(note.category || '');
  const [folderId, setFolderId] = useState(note.folderId || '');
  const [selectedTagIds, setSelectedTagIds] = useState<string[]>(note.tags?.map((t) => t.id) || []);
  const [hasLocalChanges, setHasLocalChanges] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [showConflict, setShowConflict] = useState(false);
  const [conflictMessage, setConflictMessage] = useState('');

  const typingTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const lastEditRef = useRef<{ title: string; content: string }>({ title: note.title, content: note.content });

  const handleEdit = useCallback((edit: NoteEdit) => {
    // Ignore our own edits
    if (edit.email === note.createdById) return;

    // Apply remote edit if no local changes or if it's a newer version
    if (!hasLocalChanges) {
      if (edit.type === 'TITLE_UPDATE' && edit.title) {
        setTitle(edit.title);
        lastEditRef.current.title = edit.title;
      }
      if (edit.type === 'CONTENT_UPDATE' && edit.content) {
        setContent(edit.content);
        lastEditRef.current.content = edit.content;
      }
      if (edit.type === 'FULL_UPDATE') {
        if (edit.title) {
          setTitle(edit.title);
          lastEditRef.current.title = edit.title;
        }
        if (edit.content) {
          setContent(edit.content);
          lastEditRef.current.content = edit.content;
        }
      }
    }
  }, [hasLocalChanges, note.createdById]);

  const handleConflict = useCallback((edit: NoteEdit) => {
    setShowConflict(true);
    setConflictMessage(edit.conflictMessage || 'Un conflit a ete detecte. Veuillez rafraichir la note.');
    toast.error('Conflit detecte: vos modifications sont basees sur une version anterieure');
  }, []);

  const {
    collaborators,
    typingUsers,
    isConnected,
    version,
    sendEdit,
    sendTyping,
  } = useNoteCollaboration({
    noteId: note.id,
    onEdit: handleEdit,
    onConflict: handleConflict,
    onCollaboratorJoin: (collaborator) => {
      toast.success(`${collaborator.displayName} a rejoint la note`, { duration: 2000 });
    },
    onCollaboratorLeave: (collaborator) => {
      toast(`${collaborator.displayName} a quitte la note`, { duration: 2000, icon: 'ℹ️' });
    },
  });

  // Send typing indicator with debounce
  const handleTypingStart = useCallback(() => {
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }
    sendTyping(true);
    typingTimeoutRef.current = setTimeout(() => {
      sendTyping(false);
    }, 2000);
  }, [sendTyping]);

  // Handle title change
  const handleTitleChange = useCallback(
    (newTitle: string) => {
      setTitle(newTitle);
      setHasLocalChanges(true);
      handleTypingStart();

      // Send edit after a short delay (debounce)
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current);
      }
      typingTimeoutRef.current = setTimeout(() => {
        sendEdit('TITLE_UPDATE', newTitle, undefined);
        sendTyping(false);
      }, 500);
    },
    [handleTypingStart, sendEdit, sendTyping]
  );

  // Handle content change
  const handleContentChange = useCallback(
    (newContent: string) => {
      setContent(newContent);
      setHasLocalChanges(true);
      handleTypingStart();

      // Send edit after a short delay (debounce)
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current);
      }
      typingTimeoutRef.current = setTimeout(() => {
        sendEdit('CONTENT_UPDATE', undefined, newContent);
        sendTyping(false);
      }, 500);
    },
    [handleTypingStart, sendEdit, sendTyping]
  );

  // Handle save
  const handleSave = async () => {
    setIsSaving(true);
    try {
      await onSave(
        title,
        content,
        category || undefined,
        folderId || undefined,
        selectedTagIds.length > 0 ? selectedTagIds : undefined
      );
      setHasLocalChanges(false);
      lastEditRef.current = { title, content };
      toast.success('Note sauvegardee');
    } catch (error) {
      toast.error('Erreur lors de la sauvegarde');
    } finally {
      setIsSaving(false);
    }
  };

  // Handle refresh after conflict
  const handleRefresh = () => {
    setTitle(lastEditRef.current.title);
    setContent(lastEditRef.current.content);
    setHasLocalChanges(false);
    setShowConflict(false);
    setConflictMessage('');
  };

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current);
      }
    };
  }, []);

  // Filter out self from collaborators for display
  const otherCollaborators = collaborators.filter((c) => c.email !== note.createdById);

  return (
    <div className="flex flex-col h-full">
      {/* Header with collaboration info */}
      <div className="flex items-center justify-between gap-4 pb-4 border-b border-gray-200 dark:border-gray-700">
        <div className="flex items-center gap-3">
          {/* Connection status */}
          <div
            className={cn(
              'flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium',
              isConnected ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'
            )}
          >
            {isConnected ? (
              <>
                <Wifi className="w-3 h-3" />
                <span>Connecte</span>
              </>
            ) : (
              <>
                <WifiOff className="w-3 h-3" />
                <span>Deconnecte</span>
              </>
            )}
          </div>

          {/* Collaborators */}
          {otherCollaborators.length > 0 && (
            <NoteCollaborators
              collaborators={otherCollaborators}
              typingUsers={typingUsers.filter((u) => u.email !== note.createdById)}
            />
          )}
        </div>

        <div className="flex items-center gap-2">
          {/* Version indicator */}
          <span className="text-xs text-gray-400">v{version}</span>

          {/* Unsaved changes indicator */}
          {hasLocalChanges && (
            <span className="text-xs text-amber-500 font-medium">Modifications non sauvegardees</span>
          )}
        </div>
      </div>

      {/* Conflict warning */}
      {showConflict && (
        <div className="flex items-center gap-3 p-3 mt-4 rounded-lg bg-amber-500/10 border border-amber-500/30">
          <AlertTriangle className="w-5 h-5 text-amber-500 flex-shrink-0" />
          <div className="flex-1">
            <p className="text-sm text-amber-500 font-medium">Conflit detecte</p>
            <p className="text-xs text-amber-400">{conflictMessage}</p>
          </div>
          <button
            onClick={handleRefresh}
            className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-amber-500 text-white text-sm font-medium hover:bg-amber-600 transition-colors"
          >
            <RefreshCw className="w-4 h-4" />
            Rafraichir
          </button>
        </div>
      )}

      {/* Editor content */}
      <div className="flex-1 overflow-auto py-4 space-y-4">
        {/* Title */}
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            Titre
          </label>
          <input
            type="text"
            value={title}
            onChange={(e) => handleTitleChange(e.target.value)}
            className="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-dark-card text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-accent"
            placeholder="Titre de la note"
          />
        </div>

        {/* Category */}
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            Categorie
          </label>
          <input
            type="text"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            className="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-dark-card text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-accent"
            placeholder="Categorie (optionnel)"
          />
        </div>

        {/* Folder */}
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            Dossier
          </label>
          <select
            value={folderId}
            onChange={(e) => setFolderId(e.target.value)}
            className="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-dark-card text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-accent"
          >
            <option value="">Racine (aucun dossier)</option>
            {folders.map((folder) => (
              <option key={folder.id} value={folder.id}>
                {folder.name}
              </option>
            ))}
          </select>
        </div>

        {/* Tags */}
        {noteTags.length > 0 && (
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Tags
            </label>
            <div className="flex flex-wrap gap-2">
              {noteTags.map((tag) => (
                <button
                  key={tag.id}
                  type="button"
                  onClick={() => {
                    setSelectedTagIds((prev) =>
                      prev.includes(tag.id) ? prev.filter((id) => id !== tag.id) : [...prev, tag.id]
                    );
                  }}
                  className={cn(
                    'px-3 py-1 rounded-full text-xs font-medium transition-all',
                    selectedTagIds.includes(tag.id)
                      ? 'ring-2 ring-offset-1 ring-gray-400 dark:ring-gray-600'
                      : 'opacity-60 hover:opacity-100'
                  )}
                  style={{ backgroundColor: tag.color, color: getContrastColor(tag.color) }}
                >
                  {tag.name}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Content Editor */}
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            Contenu
          </label>
          <WysiwygEditor content={content} onChange={handleContentChange} />
        </div>
      </div>

      {/* Footer actions */}
      <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-200 dark:border-gray-700">
        <button
          onClick={onClose}
          className="px-4 py-2 rounded-lg text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-dark-hover transition-colors"
        >
          Annuler
        </button>
        <button
          onClick={handleSave}
          disabled={isSaving || !title.trim()}
          className={cn(
            'px-4 py-2 rounded-lg text-sm font-medium text-white transition-colors',
            isSaving || !title.trim()
              ? 'bg-gray-400 cursor-not-allowed'
              : 'bg-accent hover:bg-accent/90'
          )}
        >
          {isSaving ? 'Sauvegarde...' : 'Sauvegarder'}
        </button>
      </div>
    </div>
  );
}

function getContrastColor(hexColor: string): string {
  const hex = hexColor.replace('#', '');
  const r = parseInt(hex.substring(0, 2), 16);
  const g = parseInt(hex.substring(2, 4), 16);
  const b = parseInt(hex.substring(4, 6), 16);
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
  return luminance > 0.5 ? '#000000' : '#FFFFFF';
}
