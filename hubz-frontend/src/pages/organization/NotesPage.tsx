import { useEffect, useState, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import {
  StickyNote,
  Plus,
  Edit2,
  Trash2,
  Tag,
  FileText,
  Link as LinkIcon,
  Upload,
  Download,
  File,
  FolderOpen,
  Settings,
  X,
  Folder,
  FolderPlus,
  ChevronRight,
  ChevronDown,
  MoreVertical,
  MoveRight,
  Search,
  Loader2,
  Eye,
  History,
} from 'lucide-react';
import toast from 'react-hot-toast';
import Card from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Modal from '../../components/ui/Modal';
import Input from '../../components/ui/Input';
import TagChip from '../../components/ui/TagChip';
import WysiwygEditor from '../../components/ui/WysiwygEditor';
import DocumentPreviewModal from '../../components/features/DocumentPreviewModal';
import NoteVersionHistoryPanel from '../../components/features/NoteVersionHistoryPanel';
import { noteService, noteFolderService, noteTagService } from '../../services/note.service';
import { attachmentService } from '../../services/attachment.service';
import { organizationDocumentService } from '../../services/organizationDocument.service';
import { tagService } from '../../services/tag.service';
import type {
  Note,
  CreateNoteRequest,
  UpdateNoteRequest,
  NoteFolder,
  CreateNoteFolderRequest,
  NoteTag,
  CreateNoteTagRequest,
} from '../../types/note';
import type { NoteAttachment } from '../../types/attachment';
import type { OrganizationDocument } from '../../types/organizationDocument';
import type { Tag as TagType } from '../../types/tag';
import { cn } from '../../lib/utils';

const CATEGORIES = ['Reunions', 'Idees', 'Documentation', 'Documents projet', 'Autre'];

function getContrastColor(hexColor: string): string {
  const hex = hexColor.replace('#', '');
  const r = parseInt(hex.substring(0, 2), 16);
  const g = parseInt(hex.substring(2, 4), 16);
  const b = parseInt(hex.substring(4, 6), 16);
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
  return luminance > 0.5 ? '#000000' : '#FFFFFF';
}

export default function NotesPage() {
  const { orgId } = useParams<{ orgId: string }>();
  const [notes, setNotes] = useState<Note[]>([]);
  const [folders, setFolders] = useState<NoteFolder[]>([]);
  const [flatFolders, setFlatFolders] = useState<NoteFolder[]>([]);
  const [noteTags, setNoteTags] = useState<NoteTag[]>([]);
  const [organizationDocuments, setOrganizationDocuments] = useState<OrganizationDocument[]>([]);
  const [loading, setLoading] = useState(true);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [selectedNote, setSelectedNote] = useState<Note | null>(null);
  const [selectedCategory, setSelectedCategory] = useState<string | undefined>(undefined);
  const [selectedFolderId, setSelectedFolderId] = useState<string | undefined>(undefined);
  const [selectedNoteTagId, setSelectedNoteTagId] = useState<string | null>(null);
  const [uploadingOrgDoc, setUploadingOrgDoc] = useState(false);
  const [dragActiveOrgDoc, setDragActiveOrgDoc] = useState(false);
  const [availableTags, setAvailableTags] = useState<TagType[]>([]);
  const [selectedTagFilter, setSelectedTagFilter] = useState<string | null>(null);
  const [isTagManageModalOpen, setIsTagManageModalOpen] = useState(false);
  const [selectedDocumentForTags, setSelectedDocumentForTags] = useState<OrganizationDocument | null>(null);
  const [selectedDocumentForPreview, setSelectedDocumentForPreview] = useState<OrganizationDocument | null>(null);
  const [isFolderModalOpen, setIsFolderModalOpen] = useState(false);
  const [isNoteTagModalOpen, setIsNoteTagModalOpen] = useState(false);
  const [isMoveNoteModalOpen, setIsMoveNoteModalOpen] = useState(false);
  const [noteToMove, setNoteToMove] = useState<Note | null>(null);
  const [editingFolder, setEditingFolder] = useState<NoteFolder | null>(null);
  const [showRootNotes, setShowRootNotes] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<Note[]>([]);
  const [isSearching, setIsSearching] = useState(false);

  const fetchNotes = useCallback(async () => {
    if (!orgId) return;
    setLoading(true);
    try {
      const options: { category?: string; folderId?: string; rootOnly?: boolean } = {};
      if (selectedCategory) options.category = selectedCategory;
      if (selectedFolderId) {
        options.folderId = selectedFolderId;
      } else if (showRootNotes) {
        options.rootOnly = true;
      }
      const data = await noteService.getByOrganization(orgId, options);
      // Filter by note tag if selected
      let filteredData = data;
      if (selectedNoteTagId) {
        filteredData = data.filter((note) => note.tags?.some((tag) => tag.id === selectedNoteTagId));
      }
      setNotes(filteredData);
    } catch (error) {
      toast.error('Erreur lors du chargement des notes');
      console.error(error);
    } finally {
      setLoading(false);
    }
  }, [orgId, selectedCategory, selectedFolderId, showRootNotes, selectedNoteTagId]);

  const fetchFolders = useCallback(async () => {
    if (!orgId) return;
    try {
      const [treeData, flatData] = await Promise.all([
        noteFolderService.getByOrganization(orgId),
        noteFolderService.getByOrganization(orgId, true),
      ]);
      setFolders(treeData);
      setFlatFolders(flatData);
    } catch (error) {
      console.error('Erreur lors du chargement des dossiers:', error);
    }
  }, [orgId]);

  const fetchNoteTags = useCallback(async () => {
    if (!orgId) return;
    try {
      const data = await noteTagService.getByOrganization(orgId);
      setNoteTags(data);
    } catch (error) {
      console.error('Erreur lors du chargement des tags de notes:', error);
    }
  }, [orgId]);

  const fetchOrganizationDocuments = useCallback(async () => {
    if (!orgId) return;
    try {
      const data = await organizationDocumentService.getDocuments(orgId);
      setOrganizationDocuments(data);
    } catch (error) {
      console.error('Erreur lors du chargement des documents:', error);
    }
  }, [orgId]);

  const fetchTags = useCallback(async () => {
    if (!orgId) return;
    try {
      const data = await tagService.getByOrganization(orgId);
      setAvailableTags(data);
    } catch (error) {
      console.error('Erreur lors du chargement des tags:', error);
    }
  }, [orgId]);

  useEffect(() => {
    fetchNotes();
    fetchFolders();
    fetchNoteTags();
    fetchOrganizationDocuments();
    fetchTags();
  }, [fetchNotes, fetchFolders, fetchNoteTags, fetchOrganizationDocuments, fetchTags]);

  // Debounced search effect
  useEffect(() => {
    if (!orgId) return;

    const debounceTimeout = setTimeout(async () => {
      if (searchQuery.trim().length >= 2) {
        setIsSearching(true);
        try {
          const results = await noteService.search(orgId, searchQuery.trim());
          setSearchResults(results);
        } catch (error) {
          console.error('Search error:', error);
          setSearchResults([]);
        } finally {
          setIsSearching(false);
        }
      } else {
        setSearchResults([]);
      }
    }, 300);

    return () => clearTimeout(debounceTimeout);
  }, [orgId, searchQuery]);

  // Prevent browser default drag & drop behavior (only outside drop zones)
  useEffect(() => {
    const preventDefaults = (e: DragEvent) => {
      const target = e.target as HTMLElement;
      const isInUploadZone = target.closest('[data-upload-zone="true"]');
      if (!isInUploadZone) {
        e.preventDefault();
        e.stopPropagation();
      }
    };

    document.addEventListener('dragover', preventDefaults, true);
    document.addEventListener('drop', preventDefaults, true);

    return () => {
      document.removeEventListener('dragover', preventDefaults, true);
      document.removeEventListener('drop', preventDefaults, true);
    };
  }, []);

  const handleCreate = async (data: CreateNoteRequest) => {
    if (!orgId) return;
    try {
      // Add current folder to the note
      const noteData = { ...data, folderId: selectedFolderId };
      await noteService.create(orgId, noteData);
      toast.success('Note creee');
      setIsCreateModalOpen(false);
      await fetchNotes();
    } catch (error) {
      toast.error('Erreur lors de la creation');
    }
  };

  const handleUpdate = async (data: UpdateNoteRequest) => {
    if (!selectedNote) return;
    try {
      await noteService.update(selectedNote.id, data);
      toast.success('Note mise a jour');
      setIsEditModalOpen(false);
      setSelectedNote(null);
      await fetchNotes();
    } catch (error) {
      toast.error('Erreur lors de la mise a jour');
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Supprimer cette note ?')) return;
    try {
      await noteService.delete(id);
      toast.success('Note supprimee');
      await fetchNotes();
    } catch (error) {
      toast.error('Erreur lors de la suppression');
    }
  };

  const handleMoveNote = async (noteId: string, folderId?: string) => {
    try {
      await noteService.moveToFolder(noteId, folderId);
      toast.success('Note deplacee');
      setIsMoveNoteModalOpen(false);
      setNoteToMove(null);
      await fetchNotes();
    } catch (error) {
      toast.error('Erreur lors du deplacement');
    }
  };

  // Folder handlers
  const handleCreateFolder = async (data: CreateNoteFolderRequest) => {
    if (!orgId) return;
    try {
      await noteFolderService.create(orgId, data);
      toast.success('Dossier cree');
      setIsFolderModalOpen(false);
      await fetchFolders();
    } catch (error) {
      toast.error('Erreur lors de la creation du dossier');
    }
  };

  const handleUpdateFolder = async (folderId: string, name: string) => {
    try {
      await noteFolderService.update(folderId, { name });
      toast.success('Dossier mis a jour');
      setEditingFolder(null);
      await fetchFolders();
    } catch (error) {
      toast.error('Erreur lors de la mise a jour du dossier');
    }
  };

  const handleDeleteFolder = async (folderId: string) => {
    if (!confirm('Supprimer ce dossier ? Il doit etre vide.')) return;
    try {
      await noteFolderService.delete(folderId);
      toast.success('Dossier supprime');
      if (selectedFolderId === folderId) {
        setSelectedFolderId(undefined);
      }
      await fetchFolders();
    } catch (error) {
      toast.error('Erreur lors de la suppression du dossier');
    }
  };

  // Note Tag handlers
  const handleCreateNoteTag = async (data: CreateNoteTagRequest) => {
    if (!orgId) return;
    try {
      await noteTagService.create(orgId, data);
      toast.success('Tag cree');
      await fetchNoteTags();
    } catch (error) {
      toast.error('Erreur lors de la creation du tag');
    }
  };

  const handleDeleteNoteTag = async (tagId: string) => {
    try {
      await noteTagService.delete(tagId);
      toast.success('Tag supprime');
      if (selectedNoteTagId === tagId) {
        setSelectedNoteTagId(null);
      }
      await fetchNoteTags();
    } catch (error) {
      toast.error('Erreur lors de la suppression du tag');
    }
  };

  // Organization Documents handlers
  const handleOrgDocDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActiveOrgDoc(true);
    } else if (e.type === 'dragleave') {
      setDragActiveOrgDoc(false);
    }
  };

  const handleOrgDocDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActiveOrgDoc(false);

    if (!orgId) return;

    const files = Array.from(e.dataTransfer.files);
    if (files.length > 0) {
      await uploadOrgDocuments(files);
    }
  };

  const handleOrgDocFileInput = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!orgId) return;
    const files = e.target.files ? Array.from(e.target.files) : [];
    if (files.length > 0) {
      await uploadOrgDocuments(files);
    }
  };

  const uploadOrgDocuments = async (files: File[]) => {
    if (!orgId) return;
    setUploadingOrgDoc(true);
    try {
      for (const file of files) {
        await organizationDocumentService.uploadDocument(orgId, file);
      }
      toast.success(`${files.length} document(s) uploade(s)`);
      await fetchOrganizationDocuments();
    } catch (error) {
      toast.error("Erreur lors de l'upload");
      console.error(error);
    } finally {
      setUploadingOrgDoc(false);
    }
  };

  const handleDownloadOrgDoc = async (doc: OrganizationDocument) => {
    try {
      const blob = await organizationDocumentService.downloadDocument(doc.id);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = doc.originalFileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      toast.error('Erreur lors du telechargement');
      console.error(error);
    }
  };

  const handleDeleteOrgDoc = async (documentId: string) => {
    if (!confirm('Supprimer ce document ?')) return;

    try {
      await organizationDocumentService.deleteDocument(documentId);
      toast.success('Document supprime');
      await fetchOrganizationDocuments();
    } catch (error) {
      toast.error('Erreur lors de la suppression');
      console.error(error);
    }
  };

  const handleAddTagToDocument = async (documentId: string, tagId: string) => {
    try {
      await tagService.addTagToDocument(documentId, tagId);
      await fetchOrganizationDocuments();
    } catch (error) {
      toast.error("Erreur lors de l'ajout du tag");
      console.error(error);
    }
  };

  const handleRemoveTagFromDocument = async (documentId: string, tagId: string) => {
    try {
      await tagService.removeTagFromDocument(documentId, tagId);
      await fetchOrganizationDocuments();
    } catch (error) {
      toast.error('Erreur lors de la suppression du tag');
      console.error(error);
    }
  };

  const handleCreateTag = async (name: string, color: string): Promise<TagType> => {
    if (!orgId) throw new Error('Organization ID not found');
    const newTag = await tagService.create(orgId, { name, color });
    await fetchTags();
    return newTag;
  };

  // Filter documents by selected tag
  const filteredDocuments = selectedTagFilter
    ? organizationDocuments.filter((doc) => doc.tags?.some((tag) => tag.id === selectedTagFilter))
    : organizationDocuments;

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // Get categories from existing notes
  const existingCategories = Array.from(new Set(notes.map((n) => n.category).filter(Boolean)));

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="text-gray-500 dark:text-gray-400">Chargement...</div>
      </div>
    );
  }

  return (
    <div className="flex h-full flex-col gap-6 overflow-auto p-6">
      {/* Header */}
      <div>
        <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Notes & Documents</h2>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Notes et documents partages de votre organisation
        </p>
      </div>

      {/* Main Layout: 3 Columns */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 h-full overflow-hidden">
        {/* LEFT SIDEBAR: Folder Tree */}
        <div className="lg:col-span-3 flex flex-col gap-4 overflow-auto">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
              <Folder className="h-4 w-4" />
              Dossiers
            </h3>
            <button
              onClick={() => setIsFolderModalOpen(true)}
              className="rounded-lg p-1.5 text-gray-400 hover:bg-light-hover hover:text-gray-600 dark:hover:bg-dark-hover dark:hover:text-gray-300 transition-colors"
              title="Nouveau dossier"
            >
              <FolderPlus className="h-4 w-4" />
            </button>
          </div>

          {/* Folder Tree */}
          <div className="rounded-lg bg-light-card dark:bg-dark-card p-2 space-y-1">
            {/* Root/All Notes */}
            <button
              onClick={() => {
                setSelectedFolderId(undefined);
                setShowRootNotes(false);
              }}
              className={cn(
                'w-full flex items-center gap-2 px-2 py-1.5 rounded-lg text-sm text-left transition-colors',
                selectedFolderId === undefined && !showRootNotes
                  ? 'bg-accent/10 text-accent font-medium'
                  : 'text-gray-700 dark:text-gray-300 hover:bg-light-hover dark:hover:bg-dark-hover'
              )}
            >
              <StickyNote className="h-4 w-4" />
              <span>Toutes les notes</span>
            </button>

            {/* Root only */}
            <button
              onClick={() => {
                setSelectedFolderId(undefined);
                setShowRootNotes(true);
              }}
              className={cn(
                'w-full flex items-center gap-2 px-2 py-1.5 rounded-lg text-sm text-left transition-colors',
                selectedFolderId === undefined && showRootNotes
                  ? 'bg-accent/10 text-accent font-medium'
                  : 'text-gray-700 dark:text-gray-300 hover:bg-light-hover dark:hover:bg-dark-hover'
              )}
            >
              <FileText className="h-4 w-4" />
              <span>Notes sans dossier</span>
            </button>

            {/* Folder Tree */}
            {folders.map((folder) => (
              <FolderTreeItem
                key={folder.id}
                folder={folder}
                selectedFolderId={selectedFolderId}
                onSelect={(id) => {
                  setSelectedFolderId(id);
                  setShowRootNotes(false);
                }}
                onEdit={setEditingFolder}
                onDelete={handleDeleteFolder}
              />
            ))}
          </div>

          {/* Note Tags Section */}
          <div className="mt-4">
            <div className="flex items-center justify-between mb-2">
              <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
                <Tag className="h-4 w-4" />
                Tags de notes
              </h3>
              <button
                onClick={() => setIsNoteTagModalOpen(true)}
                className="rounded-lg p-1.5 text-gray-400 hover:bg-light-hover hover:text-gray-600 dark:hover:bg-dark-hover dark:hover:text-gray-300 transition-colors"
                title="Gerer les tags"
              >
                <Settings className="h-4 w-4" />
              </button>
            </div>

            <div className="rounded-lg bg-light-card dark:bg-dark-card p-2 space-y-1">
              <button
                onClick={() => setSelectedNoteTagId(null)}
                className={cn(
                  'w-full flex items-center gap-2 px-2 py-1.5 rounded-lg text-sm text-left transition-colors',
                  selectedNoteTagId === null
                    ? 'bg-accent/10 text-accent font-medium'
                    : 'text-gray-700 dark:text-gray-300 hover:bg-light-hover dark:hover:bg-dark-hover'
                )}
              >
                Tous les tags
              </button>
              {noteTags.map((tag) => (
                <button
                  key={tag.id}
                  onClick={() => setSelectedNoteTagId(tag.id)}
                  className={cn(
                    'w-full flex items-center gap-2 px-2 py-1.5 rounded-lg text-sm text-left transition-colors',
                    selectedNoteTagId === tag.id
                      ? 'ring-1 ring-accent'
                      : 'hover:bg-light-hover dark:hover:bg-dark-hover'
                  )}
                >
                  <span
                    className="h-3 w-3 rounded-full flex-shrink-0"
                    style={{ backgroundColor: tag.color }}
                  />
                  <span className="truncate text-gray-700 dark:text-gray-300">{tag.name}</span>
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* CENTER: Notes */}
        <div className="lg:col-span-5 flex flex-col gap-4 overflow-auto">
          <div className="flex items-center justify-between sticky top-0 bg-light-base dark:bg-dark-base pb-2 z-10">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
              <StickyNote className="h-5 w-5" />
              Notes
              {selectedFolderId && (
                <span className="text-sm font-normal text-gray-500">
                  - {flatFolders.find((f) => f.id === selectedFolderId)?.name}
                </span>
              )}
            </h3>
            <Button onClick={() => setIsCreateModalOpen(true)} size="sm">
              <Plus className="h-4 w-4" />
              Nouvelle note
            </Button>
          </div>

          {/* Search Input */}
          <div className="relative">
            <div className="flex items-center gap-2 rounded-lg border bg-white px-3 py-2 dark:bg-dark-card border-gray-200 dark:border-gray-700 focus-within:border-accent focus-within:ring-2 focus-within:ring-accent/20">
              <Search className="h-4 w-4 text-gray-400" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Rechercher dans les notes..."
                className="flex-1 bg-transparent text-sm text-gray-900 placeholder-gray-400 outline-none dark:text-gray-100"
              />
              {isSearching && <Loader2 className="h-4 w-4 animate-spin text-accent" />}
              {searchQuery && !isSearching && (
                <button
                  onClick={() => setSearchQuery('')}
                  className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                >
                  <X className="h-4 w-4" />
                </button>
              )}
            </div>
          </div>

          {/* Search Results */}
          {searchQuery.trim().length >= 2 && (
            <div className="rounded-lg border border-accent/20 bg-accent/5 p-3">
              <p className="text-sm text-gray-600 dark:text-gray-400 mb-2">
                {isSearching ? (
                  'Recherche en cours...'
                ) : searchResults.length > 0 ? (
                  <>Resultats de recherche pour "{searchQuery}" ({searchResults.length} note{searchResults.length > 1 ? 's' : ''})</>
                ) : (
                  <>Aucun resultat pour "{searchQuery}"</>
                )}
              </p>
              {!isSearching && searchResults.length > 0 && (
                <div className="grid gap-3 sm:grid-cols-1">
                  {searchResults.map((note) => (
                    <NoteCard
                      key={`search-${note.id}`}
                      note={note}
                      onView={(note) => {
                        setSelectedNote(note);
                        setIsViewModalOpen(true);
                      }}
                      onEdit={(note) => {
                        setSelectedNote(note);
                        setIsEditModalOpen(true);
                      }}
                      onDelete={handleDelete}
                      onMove={(note) => {
                        setNoteToMove(note);
                        setIsMoveNoteModalOpen(true);
                      }}
                    />
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Category Filter */}
          {existingCategories.length > 0 && (
            <div className="flex gap-2 overflow-x-auto pb-2">
              <button
                onClick={() => setSelectedCategory(undefined)}
                className={cn(
                  'whitespace-nowrap rounded-full px-4 py-2 text-sm font-medium transition-all',
                  selectedCategory === undefined
                    ? 'bg-accent text-white shadow-md'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200 dark:bg-gray-800 dark:text-gray-300 dark:hover:bg-gray-700'
                )}
              >
                Toutes
              </button>
              {existingCategories.map((category) => (
                <button
                  key={category}
                  onClick={() => setSelectedCategory(category)}
                  className={cn(
                    'whitespace-nowrap rounded-full px-4 py-2 text-sm font-medium transition-all',
                    selectedCategory === category
                      ? 'bg-accent text-white shadow-md'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200 dark:bg-gray-800 dark:text-gray-300 dark:hover:bg-gray-700'
                  )}
                >
                  {category}
                </button>
              ))}
            </div>
          )}

          {/* Notes Grid */}
          {notes.length === 0 ? (
            <Card className="flex flex-col items-center justify-center p-12">
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-gray-100 dark:bg-gray-800">
                <StickyNote className="h-8 w-8 text-gray-400" />
              </div>
              <h3 className="mt-4 text-lg font-semibold text-gray-900 dark:text-gray-100">
                Aucune note
              </h3>
              <p className="mt-2 text-center text-sm text-gray-500 dark:text-gray-400">
                Commencez par creer votre premiere note.
              </p>
            </Card>
          ) : (
            <div className="grid gap-4 sm:grid-cols-1">
              {notes.map((note) => (
                <NoteCard
                  key={note.id}
                  note={note}
                  onView={(note) => {
                    setSelectedNote(note);
                    setIsViewModalOpen(true);
                  }}
                  onEdit={(note) => {
                    setSelectedNote(note);
                    setIsEditModalOpen(true);
                  }}
                  onDelete={handleDelete}
                  onMove={(note) => {
                    setNoteToMove(note);
                    setIsMoveNoteModalOpen(true);
                  }}
                />
              ))}
            </div>
          )}
        </div>

        {/* RIGHT COLUMN: Organization Documents */}
        <div className="lg:col-span-4 flex flex-col gap-4 overflow-auto">
          <div className="flex items-center justify-between sticky top-0 bg-light-base dark:bg-dark-base pb-2 z-10">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
              <FolderOpen className="h-5 w-5" />
              Documents
            </h3>
            <button
              onClick={() => setIsTagManageModalOpen(true)}
              className="rounded-lg p-2 text-gray-400 hover:bg-light-hover hover:text-gray-600 dark:hover:bg-dark-hover dark:hover:text-gray-300 transition-colors"
              title="Gerer les tags"
            >
              <Settings className="h-4 w-4" />
            </button>
          </div>

          {/* Tag Filter */}
          {availableTags.length > 0 && (
            <div className="flex gap-2 flex-wrap">
              <button
                onClick={() => setSelectedTagFilter(null)}
                className={cn(
                  'whitespace-nowrap rounded-full px-3 py-1 text-xs font-medium transition-all',
                  selectedTagFilter === null
                    ? 'bg-accent text-white shadow-md'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200 dark:bg-gray-800 dark:text-gray-300 dark:hover:bg-gray-700'
                )}
              >
                Tous
              </button>
              {availableTags.map((tag) => (
                <button
                  key={tag.id}
                  onClick={() => setSelectedTagFilter(tag.id)}
                  className={cn(
                    'whitespace-nowrap rounded-full px-3 py-1 text-xs font-medium transition-all',
                    selectedTagFilter === tag.id
                      ? 'shadow-md ring-2 ring-offset-1 ring-gray-400 dark:ring-gray-600'
                      : 'opacity-80 hover:opacity-100'
                  )}
                  style={{ backgroundColor: tag.color, color: getContrastColor(tag.color) }}
                >
                  {tag.name}
                </button>
              ))}
            </div>
          )}

          {/* Upload Zone */}
          <div
            data-upload-zone="true"
            onDragEnter={handleOrgDocDrag}
            onDragLeave={handleOrgDocDrag}
            onDragOver={handleOrgDocDrag}
            onDrop={handleOrgDocDrop}
            className={cn(
              'relative rounded-lg border-2 border-dashed p-8 transition-colors',
              dragActiveOrgDoc
                ? 'border-accent bg-accent/10'
                : 'border-gray-300 dark:border-gray-600 hover:border-accent/50'
            )}
          >
            <div className="flex flex-col items-center justify-center gap-3 text-center">
              <Upload className={cn('h-8 w-8', dragActiveOrgDoc ? 'text-accent' : 'text-gray-400')} />
              <div>
                <p className="text-sm font-medium text-gray-700 dark:text-gray-300">
                  {uploadingOrgDoc ? 'Upload en cours...' : 'Glissez vos fichiers ici'}
                </p>
                <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                  ou{' '}
                  <label className="cursor-pointer text-accent hover:underline font-medium">
                    parcourez votre ordinateur
                    <input
                      type="file"
                      multiple
                      className="hidden"
                      onChange={handleOrgDocFileInput}
                      disabled={uploadingOrgDoc}
                    />
                  </label>
                </p>
                <p className="mt-2 text-xs text-gray-400">Max 10MB par fichier</p>
              </div>
            </div>
          </div>

          {/* Documents List */}
          <div className="space-y-2">
            {filteredDocuments.length === 0 ? (
              <div className="text-center py-8 text-sm text-gray-500 dark:text-gray-400">
                {selectedTagFilter ? 'Aucun document avec ce tag' : 'Aucun document uploade'}
              </div>
            ) : (
              filteredDocuments.map((doc) => (
                <div
                  key={doc.id}
                  className="group rounded-lg bg-light-card dark:bg-dark-card p-3 hover:bg-light-hover dark:hover:bg-dark-hover transition-colors"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3 flex-1 min-w-0">
                      <File className="h-5 w-5 flex-shrink-0 text-blue-600 dark:text-blue-400" />
                      <div className="flex-1 min-w-0">
                        <p className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">
                          {doc.originalFileName}
                        </p>
                        <p className="text-xs text-gray-500 dark:text-gray-400">
                          {formatFileSize(doc.fileSize)} - {formatDate(doc.uploadedAt)}
                        </p>
                      </div>
                    </div>
                    <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button
                        onClick={() => setSelectedDocumentForPreview(doc)}
                        className="rounded-lg p-2 text-gray-400 hover:bg-green-100 hover:text-green-600 dark:hover:bg-green-900/30 dark:hover:text-green-400 transition-colors"
                        title="Previsualiser"
                      >
                        <Eye className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => setSelectedDocumentForTags(doc)}
                        className="rounded-lg p-2 text-gray-400 hover:bg-purple-100 hover:text-purple-600 dark:hover:bg-purple-900/30 dark:hover:text-purple-400 transition-colors"
                        title="Gerer les tags"
                      >
                        <Tag className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => handleDownloadOrgDoc(doc)}
                        className="rounded-lg p-2 text-gray-400 hover:bg-blue-100 hover:text-blue-600 dark:hover:bg-blue-900/30 dark:hover:text-blue-400 transition-colors"
                        title="Telecharger"
                      >
                        <Download className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => handleDeleteOrgDoc(doc.id)}
                        className="rounded-lg p-2 text-gray-400 hover:bg-red-100 hover:text-red-600 dark:hover:bg-red-900/30 dark:hover:text-red-400 transition-colors"
                        title="Supprimer"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                  {/* Tags display */}
                  {doc.tags && doc.tags.length > 0 && (
                    <div className="flex flex-wrap gap-1 mt-2 ml-8">
                      {doc.tags.map((tag) => (
                        <TagChip key={tag.id} tag={tag} size="sm" />
                      ))}
                    </div>
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Create Note Modal */}
      {isCreateModalOpen && (
        <CreateNoteModal
          isOpen={isCreateModalOpen}
          onClose={() => setIsCreateModalOpen(false)}
          onCreate={handleCreate}
          noteTags={noteTags}
          folders={flatFolders}
          currentFolderId={selectedFolderId}
        />
      )}

      {/* Edit Note Modal */}
      {selectedNote && (
        <EditNoteModal
          isOpen={isEditModalOpen}
          onClose={() => {
            setIsEditModalOpen(false);
            setSelectedNote(null);
          }}
          onUpdate={handleUpdate}
          note={selectedNote}
          noteTags={noteTags}
          folders={flatFolders}
        />
      )}

      {/* View Note Modal */}
      {selectedNote && (
        <ViewNoteModal
          isOpen={isViewModalOpen}
          onClose={() => {
            setIsViewModalOpen(false);
            setSelectedNote(null);
          }}
          note={selectedNote}
          onNoteUpdated={async () => {
            await fetchNotes();
            // Refresh the selected note if it was updated
            if (selectedNote) {
              try {
                const updatedNote = await noteService.getById(selectedNote.id);
                setSelectedNote(updatedNote);
              } catch (error) {
                console.error('Error refreshing note:', error);
              }
            }
          }}
        />
      )}

      {/* Create Folder Modal */}
      {isFolderModalOpen && (
        <CreateFolderModal
          isOpen={isFolderModalOpen}
          onClose={() => setIsFolderModalOpen(false)}
          onCreate={handleCreateFolder}
          folders={flatFolders}
        />
      )}

      {/* Edit Folder Modal */}
      {editingFolder && (
        <EditFolderModal
          isOpen={!!editingFolder}
          onClose={() => setEditingFolder(null)}
          folder={editingFolder}
          onUpdate={handleUpdateFolder}
        />
      )}

      {/* Move Note Modal */}
      {isMoveNoteModalOpen && noteToMove && (
        <MoveNoteModal
          isOpen={isMoveNoteModalOpen}
          onClose={() => {
            setIsMoveNoteModalOpen(false);
            setNoteToMove(null);
          }}
          note={noteToMove}
          folders={flatFolders}
          onMove={handleMoveNote}
        />
      )}

      {/* Note Tag Management Modal */}
      {isNoteTagModalOpen && (
        <NoteTagManagementModal
          isOpen={isNoteTagModalOpen}
          onClose={() => setIsNoteTagModalOpen(false)}
          tags={noteTags}
          onCreateTag={handleCreateNoteTag}
          onDeleteTag={handleDeleteNoteTag}
          onUpdateTag={async (tagId, name, color) => {
            await noteTagService.update(tagId, { name, color });
            await fetchNoteTags();
          }}
        />
      )}

      {/* Document Tag Management Modal */}
      {isTagManageModalOpen && (
        <TagManagementModal
          isOpen={isTagManageModalOpen}
          onClose={() => setIsTagManageModalOpen(false)}
          tags={availableTags}
          onCreateTag={handleCreateTag}
          onDeleteTag={async (tagId) => {
            await tagService.delete(tagId);
            await fetchTags();
            await fetchOrganizationDocuments();
          }}
          onUpdateTag={async (tagId, name, color) => {
            await tagService.update(tagId, { name, color });
            await fetchTags();
            await fetchOrganizationDocuments();
          }}
        />
      )}

      {/* Document Tags Modal */}
      {selectedDocumentForTags && (
        <DocumentTagsModal
          isOpen={!!selectedDocumentForTags}
          onClose={() => setSelectedDocumentForTags(null)}
          document={selectedDocumentForTags}
          availableTags={availableTags}
          onAddTag={(tagId) => handleAddTagToDocument(selectedDocumentForTags.id, tagId)}
          onRemoveTag={(tagId) => handleRemoveTagFromDocument(selectedDocumentForTags.id, tagId)}
          onCreateTag={handleCreateTag}
        />
      )}

      {/* Document Preview Modal */}
      {selectedDocumentForPreview && (
        <DocumentPreviewModal
          isOpen={!!selectedDocumentForPreview}
          onClose={() => setSelectedDocumentForPreview(null)}
          document={selectedDocumentForPreview}
        />
      )}
    </div>
  );
}

// Folder Tree Item Component
interface FolderTreeItemProps {
  folder: NoteFolder;
  selectedFolderId?: string;
  onSelect: (id: string) => void;
  onEdit: (folder: NoteFolder) => void;
  onDelete: (id: string) => void;
  depth?: number;
}

function FolderTreeItem({
  folder,
  selectedFolderId,
  onSelect,
  onEdit,
  onDelete,
  depth = 0,
}: FolderTreeItemProps) {
  const [isExpanded, setIsExpanded] = useState(true);
  const [showMenu, setShowMenu] = useState(false);
  const hasChildren = folder.children && folder.children.length > 0;

  return (
    <div>
      <div
        className={cn(
          'group flex items-center gap-1 px-2 py-1.5 rounded-lg text-sm transition-colors cursor-pointer',
          selectedFolderId === folder.id
            ? 'bg-accent/10 text-accent font-medium'
            : 'text-gray-700 dark:text-gray-300 hover:bg-light-hover dark:hover:bg-dark-hover'
        )}
        style={{ paddingLeft: `${depth * 12 + 8}px` }}
        onClick={() => onSelect(folder.id)}
      >
        {hasChildren ? (
          <button
            onClick={(e) => {
              e.stopPropagation();
              setIsExpanded(!isExpanded);
            }}
            className="p-0.5 hover:bg-gray-200 dark:hover:bg-gray-700 rounded"
          >
            {isExpanded ? <ChevronDown className="h-3 w-3" /> : <ChevronRight className="h-3 w-3" />}
          </button>
        ) : (
          <span className="w-4" />
        )}
        <Folder className="h-4 w-4 flex-shrink-0" />
        <span className="flex-1 truncate">{folder.name}</span>
        {folder.noteCount !== undefined && folder.noteCount > 0 && (
          <span className="text-xs text-gray-400">{folder.noteCount}</span>
        )}
        <div className="relative">
          <button
            onClick={(e) => {
              e.stopPropagation();
              setShowMenu(!showMenu);
            }}
            className="p-1 rounded opacity-0 group-hover:opacity-100 hover:bg-gray-200 dark:hover:bg-gray-700"
          >
            <MoreVertical className="h-3 w-3" />
          </button>
          {showMenu && (
            <div className="absolute right-0 top-6 z-20 w-32 rounded-lg bg-white dark:bg-dark-card shadow-lg border border-gray-200 dark:border-gray-700 py-1">
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  setShowMenu(false);
                  onEdit(folder);
                }}
                className="w-full px-3 py-1.5 text-left text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800"
              >
                Renommer
              </button>
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  setShowMenu(false);
                  onDelete(folder.id);
                }}
                className="w-full px-3 py-1.5 text-left text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20"
              >
                Supprimer
              </button>
            </div>
          )}
        </div>
      </div>
      {isExpanded && hasChildren && (
        <div>
          {folder.children!.map((child) => (
            <FolderTreeItem
              key={child.id}
              folder={child}
              selectedFolderId={selectedFolderId}
              onSelect={onSelect}
              onEdit={onEdit}
              onDelete={onDelete}
              depth={depth + 1}
            />
          ))}
        </div>
      )}
    </div>
  );
}

interface NoteCardProps {
  note: Note;
  onView: (note: Note) => void;
  onEdit: (note: Note) => void;
  onDelete: (id: string) => void;
  onMove: (note: Note) => void;
}

function NoteCard({ note, onView, onEdit, onDelete, onMove }: NoteCardProps) {
  const isDocument = note.category === 'Documents projet';
  const hasLink = note.content.includes('http://') || note.content.includes('https://');
  const preview = note.content.slice(0, 150) + (note.content.length > 150 ? '...' : '');

  const categoryColors: Record<string, string> = {
    Reunions: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300',
    Idees: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300',
    Documentation: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300',
    'Documents projet': 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-300',
    Autre: 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300',
  };

  return (
    <Card
      className="flex flex-col gap-3 p-4 transition-all hover:shadow-lg cursor-pointer"
      onClick={() => onView(note)}
    >
      {/* Header */}
      <div className="flex items-start justify-between">
        <div className="flex items-start gap-2 flex-1">
          {isDocument ? (
            <FileText className="h-5 w-5 text-orange-600 dark:text-orange-400 flex-shrink-0 mt-0.5" />
          ) : (
            <StickyNote className="h-5 w-5 text-gray-400 flex-shrink-0 mt-0.5" />
          )}
          <h3 className="flex-1 font-semibold text-gray-900 dark:text-gray-100">{note.title}</h3>
        </div>
        <div className="flex gap-1">
          <button
            onClick={(e) => {
              e.stopPropagation();
              onMove(note);
            }}
            className="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600 dark:hover:bg-gray-800 dark:hover:text-gray-300"
            title="Deplacer"
          >
            <MoveRight className="h-4 w-4" />
          </button>
          <button
            onClick={(e) => {
              e.stopPropagation();
              onEdit(note);
            }}
            className="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600 dark:hover:bg-gray-800 dark:hover:text-gray-300"
            title="Modifier"
          >
            <Edit2 className="h-4 w-4" />
          </button>
          <button
            onClick={(e) => {
              e.stopPropagation();
              onDelete(note.id);
            }}
            className="rounded-lg p-1.5 text-gray-400 hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-900/20 dark:hover:text-red-400"
            title="Supprimer"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* Preview */}
      <p className="text-sm text-gray-600 dark:text-gray-400 line-clamp-2">{preview}</p>

      {hasLink && !isDocument && (
        <div className="flex items-center gap-1 text-xs text-accent">
          <LinkIcon className="h-3 w-3" />
          <span>Contient un lien</span>
        </div>
      )}

      {/* Tags */}
      {note.tags && note.tags.length > 0 && (
        <div className="flex flex-wrap gap-1">
          {note.tags.map((tag) => (
            <span
              key={tag.id}
              className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium"
              style={{ backgroundColor: tag.color, color: getContrastColor(tag.color) }}
            >
              {tag.name}
            </span>
          ))}
        </div>
      )}

      {/* Footer */}
      <div className="mt-auto flex items-center justify-between">
        {note.category && (
          <span
            className={cn(
              'inline-flex items-center gap-1 rounded-full px-2 py-1 text-xs font-medium',
              categoryColors[note.category] || categoryColors.Autre
            )}
          >
            <Tag className="h-3 w-3" />
            {note.category}
          </span>
        )}
        <span className="text-xs text-gray-500 dark:text-gray-400">
          {new Date(note.updatedAt).toLocaleDateString('fr-FR')}
        </span>
      </div>
    </Card>
  );
}

interface CreateNoteModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (data: CreateNoteRequest) => void;
  noteTags: NoteTag[];
  folders: NoteFolder[];
  currentFolderId?: string;
}

function CreateNoteModal({
  isOpen,
  onClose,
  onCreate,
  noteTags,
  folders,
  currentFolderId,
}: CreateNoteModalProps) {
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState('');
  const [folderId, setFolderId] = useState(currentFolderId || '');
  const [selectedTagIds, setSelectedTagIds] = useState<string[]>([]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) return;

    onCreate({
      title: title.trim(),
      content: content.trim(),
      category: category || undefined,
      folderId: folderId || undefined,
      tagIds: selectedTagIds.length > 0 ? selectedTagIds : undefined,
    });

    setTitle('');
    setContent('');
    setCategory('');
    setFolderId('');
    setSelectedTagIds([]);
  };

  const toggleTag = (tagId: string) => {
    setSelectedTagIds((prev) =>
      prev.includes(tagId) ? prev.filter((id) => id !== tagId) : [...prev, tagId]
    );
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Nouvelle note">
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          label="Titre"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="Ex: Reunion du 27 janvier"
          required
        />

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Dossier
          </label>
          <select
            value={folderId}
            onChange={(e) => setFolderId(e.target.value)}
            className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent dark:border-gray-600 dark:bg-dark-card dark:text-gray-100"
          >
            <option value="">Aucun dossier</option>
            {folders.map((folder) => (
              <option key={folder.id} value={folder.id}>
                {folder.name}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Categorie
          </label>
          <div className="grid grid-cols-2 gap-2">
            {CATEGORIES.map((cat) => (
              <button
                key={cat}
                type="button"
                onClick={() => setCategory(cat)}
                className={cn(
                  'rounded-lg border px-3 py-2 text-sm font-medium transition-colors text-left',
                  category === cat
                    ? 'border-accent bg-accent/10 text-accent'
                    : 'border-gray-300 text-gray-700 hover:border-gray-400 dark:border-gray-600 dark:text-gray-300 dark:hover:border-gray-500'
                )}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>

        {noteTags.length > 0 && (
          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Tags
            </label>
            <div className="flex flex-wrap gap-2">
              {noteTags.map((tag) => (
                <button
                  key={tag.id}
                  type="button"
                  onClick={() => toggleTag(tag.id)}
                  className={cn(
                    'rounded-full px-3 py-1 text-xs font-medium transition-all',
                    selectedTagIds.includes(tag.id)
                      ? 'ring-2 ring-accent ring-offset-1'
                      : 'opacity-70 hover:opacity-100'
                  )}
                  style={{ backgroundColor: tag.color, color: getContrastColor(tag.color) }}
                >
                  {tag.name}
                </button>
              ))}
            </div>
          </div>
        )}

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Contenu
          </label>
          <WysiwygEditor
            content={content}
            onChange={setContent}
            placeholder={
              category === 'Documents projet'
                ? 'Ajoutez des liens vers vos documents...'
                : 'Ecrivez votre note ici...'
            }
            minHeight="250px"
          />
        </div>

        <div className="flex gap-2">
          <Button type="button" variant="secondary" onClick={onClose} className="flex-1">
            Annuler
          </Button>
          <Button type="submit" className="flex-1">
            Creer
          </Button>
        </div>
      </form>
    </Modal>
  );
}

interface EditNoteModalProps {
  isOpen: boolean;
  onClose: () => void;
  onUpdate: (data: UpdateNoteRequest) => void;
  note: Note;
  noteTags: NoteTag[];
  folders: NoteFolder[];
}

function EditNoteModal({ isOpen, onClose, onUpdate, note, noteTags, folders }: EditNoteModalProps) {
  const [title, setTitle] = useState(note.title);
  const [content, setContent] = useState(note.content);
  const [category, setCategory] = useState(note.category || '');
  const [folderId, setFolderId] = useState(note.folderId || '');
  const [selectedTagIds, setSelectedTagIds] = useState<string[]>(
    note.tags?.map((t) => t.id) || []
  );

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) return;

    onUpdate({
      title: title.trim(),
      content: content.trim(),
      category: category || undefined,
      folderId: folderId || undefined,
      tagIds: selectedTagIds.length > 0 ? selectedTagIds : undefined,
    });
  };

  const toggleTag = (tagId: string) => {
    setSelectedTagIds((prev) =>
      prev.includes(tagId) ? prev.filter((id) => id !== tagId) : [...prev, tagId]
    );
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Modifier la note">
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input label="Titre" value={title} onChange={(e) => setTitle(e.target.value)} required />

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Dossier
          </label>
          <select
            value={folderId}
            onChange={(e) => setFolderId(e.target.value)}
            className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent dark:border-gray-600 dark:bg-dark-card dark:text-gray-100"
          >
            <option value="">Aucun dossier</option>
            {folders.map((folder) => (
              <option key={folder.id} value={folder.id}>
                {folder.name}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Categorie
          </label>
          <div className="grid grid-cols-2 gap-2">
            {CATEGORIES.map((cat) => (
              <button
                key={cat}
                type="button"
                onClick={() => setCategory(cat)}
                className={cn(
                  'rounded-lg border px-3 py-2 text-sm font-medium transition-colors text-left',
                  category === cat
                    ? 'border-accent bg-accent/10 text-accent'
                    : 'border-gray-300 text-gray-700 hover:border-gray-400 dark:border-gray-600 dark:text-gray-300 dark:hover:border-gray-500'
                )}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>

        {noteTags.length > 0 && (
          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Tags
            </label>
            <div className="flex flex-wrap gap-2">
              {noteTags.map((tag) => (
                <button
                  key={tag.id}
                  type="button"
                  onClick={() => toggleTag(tag.id)}
                  className={cn(
                    'rounded-full px-3 py-1 text-xs font-medium transition-all',
                    selectedTagIds.includes(tag.id)
                      ? 'ring-2 ring-accent ring-offset-1'
                      : 'opacity-70 hover:opacity-100'
                  )}
                  style={{ backgroundColor: tag.color, color: getContrastColor(tag.color) }}
                >
                  {tag.name}
                </button>
              ))}
            </div>
          </div>
        )}

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Contenu
          </label>
          <WysiwygEditor
            content={content}
            onChange={setContent}
            placeholder="Ecrivez votre note ici..."
            minHeight="250px"
          />
        </div>

        <div className="flex gap-2">
          <Button type="button" variant="secondary" onClick={onClose} className="flex-1">
            Annuler
          </Button>
          <Button type="submit" className="flex-1">
            Enregistrer
          </Button>
        </div>
      </form>
    </Modal>
  );
}

interface ViewNoteModalProps {
  isOpen: boolean;
  onClose: () => void;
  note: Note;
  onNoteUpdated?: () => void;
}

function ViewNoteModal({ isOpen, onClose, note, onNoteUpdated }: ViewNoteModalProps) {
  const [attachments, setAttachments] = useState<NoteAttachment[]>([]);
  const [uploading, setUploading] = useState(false);
  const [dragActive, setDragActive] = useState(false);
  const [isVersionHistoryOpen, setIsVersionHistoryOpen] = useState(false);

  useEffect(() => {
    if (isOpen) {
      fetchAttachments();
    }
  }, [isOpen, note.id]);

  // Prevent browser default drag & drop behavior (only outside drop zones)
  useEffect(() => {
    const preventDefaults = (e: DragEvent) => {
      const target = e.target as HTMLElement;
      const isInUploadZone = target.closest('[data-upload-zone="true"]');

      if (!isInUploadZone) {
        e.preventDefault();
        e.stopPropagation();
      }
    };

    if (isOpen) {
      document.addEventListener('dragover', preventDefaults, true);
      document.addEventListener('drop', preventDefaults, true);
    }

    return () => {
      document.removeEventListener('dragover', preventDefaults, true);
      document.removeEventListener('drop', preventDefaults, true);
    };
  }, [isOpen]);

  const fetchAttachments = async () => {
    try {
      const data = await attachmentService.getAttachments(note.id);
      setAttachments(data);
    } catch (error) {
      console.error('Erreur lors du chargement des fichiers:', error);
    }
  };

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    const files = Array.from(e.dataTransfer.files);
    if (files.length > 0) {
      await uploadFiles(files);
    }
  };

  const handleFileInput = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files ? Array.from(e.target.files) : [];
    if (files.length > 0) {
      await uploadFiles(files);
    }
  };

  const uploadFiles = async (files: File[]) => {
    setUploading(true);
    try {
      for (const file of files) {
        await attachmentService.uploadAttachment(note.id, file);
      }
      toast.success(`${files.length} fichier(s) uploade(s)`);
      await fetchAttachments();
    } catch (error) {
      toast.error("Erreur lors de l'upload");
      console.error(error);
    } finally {
      setUploading(false);
    }
  };

  const handleDownload = async (attachment: NoteAttachment) => {
    try {
      const blob = await attachmentService.downloadAttachment(attachment.id);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = attachment.originalFileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      toast.error('Erreur lors du telechargement');
      console.error(error);
    }
  };

  const handleDelete = async (attachmentId: string) => {
    if (!confirm('Supprimer ce fichier ?')) return;

    try {
      await attachmentService.deleteAttachment(attachmentId);
      toast.success('Fichier supprime');
      await fetchAttachments();
    } catch (error) {
      toast.error('Erreur lors de la suppression');
      console.error(error);
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // Check if content is HTML (from WYSIWYG editor) or plain text (legacy)
  const isHtmlContent = (content: string) => {
    return content.startsWith('<') && content.includes('</');
  };

  // Render content - supports both HTML (new) and plain text (legacy)
  const renderContent = (content: string) => {
    if (isHtmlContent(content)) {
      // HTML content from WYSIWYG editor - render as HTML
      return (
        <div
          className="prose prose-sm dark:prose-invert max-w-none note-content"
          dangerouslySetInnerHTML={{ __html: content }}
        />
      );
    }

    // Legacy plain text - convert URLs to links
    const urlRegex = /(https?:\/\/[^\s]+)/g;
    const parts = content.split(urlRegex);

    return (
      <div className="whitespace-pre-wrap">
        {parts.map((part, index) => {
          if (part.match(urlRegex)) {
            return (
              <a
                key={index}
                href={part}
                target="_blank"
                rel="noopener noreferrer"
                className="text-accent hover:underline break-all"
              >
                {part}
              </a>
            );
          }
          return <span key={index}>{part}</span>;
        })}
      </div>
    );
  };

  return (
    <>
    <Modal isOpen={isOpen} onClose={onClose} title={note.title} className="max-w-5xl">
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Section Note */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Note</h3>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setIsVersionHistoryOpen(true)}
                className="flex items-center gap-1.5 px-2.5 py-1.5 text-sm text-gray-600 dark:text-gray-400 hover:text-blue-600 dark:hover:text-blue-400 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-lg transition-colors"
                title="Voir l'historique des versions"
              >
                <History className="h-4 w-4" />
                <span>Historique</span>
              </button>
              {note.category && (
                <>
                  <Tag className="h-4 w-4 text-gray-400" />
                  <span className="text-sm font-medium text-gray-600 dark:text-gray-400">
                    {note.category}
                  </span>
                </>
              )}
            </div>
          </div>

          {/* Tags */}
          {note.tags && note.tags.length > 0 && (
            <div className="flex flex-wrap gap-1">
              {note.tags.map((tag) => (
                <span
                  key={tag.id}
                  className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium"
                  style={{ backgroundColor: tag.color, color: getContrastColor(tag.color) }}
                >
                  {tag.name}
                </span>
              ))}
            </div>
          )}

          <div className="rounded-lg bg-gray-50 p-4 dark:bg-gray-800/50 min-h-[300px] overflow-auto">
            <div className="text-sm text-gray-700 dark:text-gray-300">
              {renderContent(note.content)}
            </div>
          </div>

          {/* Note content styles for HTML rendering */}
          <style>{`
            .note-content h1 {
              font-size: 1.75rem;
              font-weight: 700;
              margin-top: 1rem;
              margin-bottom: 0.5rem;
            }
            .note-content h2 {
              font-size: 1.5rem;
              font-weight: 600;
              margin-top: 0.875rem;
              margin-bottom: 0.5rem;
            }
            .note-content h3 {
              font-size: 1.25rem;
              font-weight: 600;
              margin-top: 0.75rem;
              margin-bottom: 0.5rem;
            }
            .note-content p {
              margin: 0.5rem 0;
            }
            .note-content ul,
            .note-content ol {
              padding-left: 1.5rem;
              margin: 0.5rem 0;
            }
            .note-content li {
              margin: 0.25rem 0;
            }
            .note-content blockquote {
              border-left: 3px solid #3B82F6;
              padding-left: 1rem;
              margin: 0.5rem 0;
              font-style: italic;
              color: #6b7280;
            }
            .dark .note-content blockquote {
              color: #9ca3af;
            }
            .note-content code {
              background-color: #f3f4f6;
              padding: 0.125rem 0.25rem;
              border-radius: 0.25rem;
              font-family: ui-monospace, monospace;
              font-size: 0.875em;
            }
            .dark .note-content code {
              background-color: #374151;
            }
            .note-content pre {
              background-color: #111827;
              color: #f9fafb;
              padding: 1rem;
              border-radius: 0.5rem;
              margin: 0.5rem 0;
              overflow-x: auto;
            }
            .note-content pre code {
              background: none;
              padding: 0;
              font-size: 0.875rem;
            }
            .note-content hr {
              border: none;
              border-top: 1px solid #e5e7eb;
              margin: 1rem 0;
            }
            .dark .note-content hr {
              border-top-color: #374151;
            }
            .note-content img {
              max-width: 100%;
              height: auto;
              border-radius: 0.5rem;
              margin: 1rem 0;
            }
            .note-content a {
              color: #3B82F6;
              text-decoration: none;
            }
            .note-content a:hover {
              text-decoration: underline;
            }
          `}</style>

          <div className="text-xs text-gray-500 dark:text-gray-400 space-y-1">
            <p>Cree le {new Date(note.createdAt).toLocaleString('fr-FR')}</p>
            <p>Modifie le {new Date(note.updatedAt).toLocaleString('fr-FR')}</p>
          </div>
        </div>

        {/* Section Documents */}
        <div className="space-y-4">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
            Documents du projet
          </h3>

          {/* Upload Zone */}
          <div
            data-upload-zone="true"
            onDragEnter={handleDrag}
            onDragLeave={handleDrag}
            onDragOver={handleDrag}
            onDrop={handleDrop}
            className={`relative rounded-lg border-2 border-dashed p-8 transition-colors ${
              dragActive
                ? 'border-accent bg-accent/10'
                : 'border-gray-300 dark:border-gray-600 hover:border-accent/50'
            }`}
          >
            <div className="flex flex-col items-center justify-center gap-3 text-center">
              <Upload className={`h-8 w-8 ${dragActive ? 'text-accent' : 'text-gray-400'}`} />
              <div>
                <p className="text-sm font-medium text-gray-700 dark:text-gray-300">
                  {uploading ? 'Upload en cours...' : 'Glissez vos fichiers ici'}
                </p>
                <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                  ou{' '}
                  <label className="cursor-pointer text-accent hover:underline font-medium">
                    parcourez votre ordinateur
                    <input
                      type="file"
                      multiple
                      className="hidden"
                      onChange={handleFileInput}
                      disabled={uploading}
                    />
                  </label>
                </p>
                <p className="mt-2 text-xs text-gray-400">Max 10MB par fichier</p>
              </div>
            </div>
          </div>

          {/* Attachments List */}
          <div className="space-y-2 max-h-[400px] overflow-y-auto">
            {attachments.length === 0 ? (
              <div className="text-center py-8 text-sm text-gray-500 dark:text-gray-400">
                Aucun document attache
              </div>
            ) : (
              attachments.map((attachment) => (
                <div
                  key={attachment.id}
                  className="group flex items-center justify-between rounded-lg bg-gray-50 p-3 dark:bg-gray-800/50 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                >
                  <div className="flex items-center gap-3 flex-1 min-w-0">
                    <File className="h-5 w-5 flex-shrink-0 text-blue-600 dark:text-blue-400" />
                    <div className="flex-1 min-w-0">
                      <p className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">
                        {attachment.originalFileName}
                      </p>
                      <p className="text-xs text-gray-500 dark:text-gray-400">
                        {formatFileSize(attachment.fileSize)} - {formatDate(attachment.uploadedAt)}
                      </p>
                    </div>
                  </div>
                  <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                    <button
                      onClick={() => handleDownload(attachment)}
                      className="rounded-lg p-2 text-gray-400 hover:bg-blue-100 hover:text-blue-600 dark:hover:bg-blue-900/30 dark:hover:text-blue-400 transition-colors"
                      title="Telecharger"
                    >
                      <Download className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => handleDelete(attachment.id)}
                      className="rounded-lg p-2 text-gray-400 hover:bg-red-100 hover:text-red-600 dark:hover:bg-red-900/30 dark:hover:text-red-400 transition-colors"
                      title="Supprimer"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Footer */}
      <div className="mt-6 pt-4 border-t border-gray-200 dark:border-gray-700">
        <Button onClick={onClose} className="w-full">
          Fermer
        </Button>
      </div>
    </Modal>

    {/* Version History Panel */}
    <NoteVersionHistoryPanel
      noteId={note.id}
      isOpen={isVersionHistoryOpen}
      onClose={() => setIsVersionHistoryOpen(false)}
      onRestore={() => {
        setIsVersionHistoryOpen(false);
        onNoteUpdated?.();
      }}
    />
    </>
  );
}

// Create Folder Modal
interface CreateFolderModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (data: CreateNoteFolderRequest) => void;
  folders: NoteFolder[];
}

function CreateFolderModal({ isOpen, onClose, onCreate, folders }: CreateFolderModalProps) {
  const [name, setName] = useState('');
  const [parentFolderId, setParentFolderId] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    onCreate({
      name: name.trim(),
      parentFolderId: parentFolderId || undefined,
    });

    setName('');
    setParentFolderId('');
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Nouveau dossier">
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          label="Nom du dossier"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Ex: Projet Alpha"
          required
        />

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Dossier parent (optionnel)
          </label>
          <select
            value={parentFolderId}
            onChange={(e) => setParentFolderId(e.target.value)}
            className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent dark:border-gray-600 dark:bg-dark-card dark:text-gray-100"
          >
            <option value="">Racine</option>
            {folders.map((folder) => (
              <option key={folder.id} value={folder.id}>
                {folder.name}
              </option>
            ))}
          </select>
        </div>

        <div className="flex gap-2">
          <Button type="button" variant="secondary" onClick={onClose} className="flex-1">
            Annuler
          </Button>
          <Button type="submit" className="flex-1">
            Creer
          </Button>
        </div>
      </form>
    </Modal>
  );
}

// Edit Folder Modal
interface EditFolderModalProps {
  isOpen: boolean;
  onClose: () => void;
  folder: NoteFolder;
  onUpdate: (folderId: string, name: string) => void;
}

function EditFolderModal({ isOpen, onClose, folder, onUpdate }: EditFolderModalProps) {
  const [name, setName] = useState(folder.name);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    onUpdate(folder.id, name.trim());
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Renommer le dossier">
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          label="Nom du dossier"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />

        <div className="flex gap-2">
          <Button type="button" variant="secondary" onClick={onClose} className="flex-1">
            Annuler
          </Button>
          <Button type="submit" className="flex-1">
            Enregistrer
          </Button>
        </div>
      </form>
    </Modal>
  );
}

// Move Note Modal
interface MoveNoteModalProps {
  isOpen: boolean;
  onClose: () => void;
  note: Note;
  folders: NoteFolder[];
  onMove: (noteId: string, folderId?: string) => void;
}

function MoveNoteModal({ isOpen, onClose, note, folders, onMove }: MoveNoteModalProps) {
  const [selectedFolderId, setSelectedFolderId] = useState(note.folderId || '');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onMove(note.id, selectedFolderId || undefined);
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Deplacer la note">
      <form onSubmit={handleSubmit} className="space-y-4">
        <p className="text-sm text-gray-600 dark:text-gray-400">
          Deplacer "{note.title}" vers :
        </p>

        <div className="space-y-2 max-h-64 overflow-y-auto">
          <button
            type="button"
            onClick={() => setSelectedFolderId('')}
            className={cn(
              'w-full flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-left transition-colors',
              selectedFolderId === ''
                ? 'bg-accent/10 text-accent font-medium ring-1 ring-accent'
                : 'text-gray-700 dark:text-gray-300 hover:bg-light-hover dark:hover:bg-dark-hover'
            )}
          >
            <FileText className="h-4 w-4" />
            <span>Sans dossier (racine)</span>
          </button>
          {folders.map((folder) => (
            <button
              key={folder.id}
              type="button"
              onClick={() => setSelectedFolderId(folder.id)}
              className={cn(
                'w-full flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-left transition-colors',
                selectedFolderId === folder.id
                  ? 'bg-accent/10 text-accent font-medium ring-1 ring-accent'
                  : 'text-gray-700 dark:text-gray-300 hover:bg-light-hover dark:hover:bg-dark-hover'
              )}
            >
              <Folder className="h-4 w-4" />
              <span>{folder.name}</span>
            </button>
          ))}
        </div>

        <div className="flex gap-2">
          <Button type="button" variant="secondary" onClick={onClose} className="flex-1">
            Annuler
          </Button>
          <Button type="submit" className="flex-1">
            Deplacer
          </Button>
        </div>
      </form>
    </Modal>
  );
}

// Note Tag Management Modal
const DEFAULT_COLORS = [
  '#EF4444',
  '#F97316',
  '#F59E0B',
  '#EAB308',
  '#84CC16',
  '#22C55E',
  '#14B8A6',
  '#06B6D4',
  '#3B82F6',
  '#6366F1',
  '#8B5CF6',
  '#A855F7',
  '#D946EF',
  '#EC4899',
  '#64748B',
];

interface NoteTagManagementModalProps {
  isOpen: boolean;
  onClose: () => void;
  tags: NoteTag[];
  onCreateTag: (data: CreateNoteTagRequest) => Promise<void>;
  onDeleteTag: (tagId: string) => Promise<void>;
  onUpdateTag: (tagId: string, name: string, color: string) => Promise<void>;
}

function NoteTagManagementModal({
  isOpen,
  onClose,
  tags,
  onCreateTag,
  onDeleteTag,
  onUpdateTag,
}: NoteTagManagementModalProps) {
  const [newTagName, setNewTagName] = useState('');
  const [newTagColor, setNewTagColor] = useState(DEFAULT_COLORS[0]);
  const [isCreating, setIsCreating] = useState(false);
  const [editingTag, setEditingTag] = useState<NoteTag | null>(null);
  const [editName, setEditName] = useState('');
  const [editColor, setEditColor] = useState('');

  const handleCreate = async () => {
    if (!newTagName.trim() || isCreating) return;
    setIsCreating(true);
    try {
      await onCreateTag({ name: newTagName.trim(), color: newTagColor });
      setNewTagName('');
      setNewTagColor(DEFAULT_COLORS[Math.floor(Math.random() * DEFAULT_COLORS.length)]);
      toast.success('Tag cree');
    } catch (error) {
      toast.error('Erreur lors de la creation du tag');
    } finally {
      setIsCreating(false);
    }
  };

  const handleDelete = async (tagId: string) => {
    if (!confirm('Supprimer ce tag ? Il sera retire de toutes les notes.')) return;
    try {
      await onDeleteTag(tagId);
      toast.success('Tag supprime');
    } catch (error) {
      toast.error('Erreur lors de la suppression');
    }
  };

  const handleUpdate = async () => {
    if (!editingTag || !editName.trim()) return;
    try {
      await onUpdateTag(editingTag.id, editName.trim(), editColor);
      setEditingTag(null);
      toast.success('Tag mis a jour');
    } catch (error) {
      toast.error('Erreur lors de la mise a jour');
    }
  };

  const startEdit = (tag: NoteTag) => {
    setEditingTag(tag);
    setEditName(tag.name);
    setEditColor(tag.color);
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Gerer les tags de notes">
      <div className="space-y-6">
        {/* Create new tag */}
        <div className="space-y-3">
          <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300">Nouveau tag</h4>
          <div className="flex gap-2">
            <Input
              value={newTagName}
              onChange={(e) => setNewTagName(e.target.value)}
              placeholder="Nom du tag"
              className="flex-1"
            />
            <Button onClick={handleCreate} disabled={!newTagName.trim() || isCreating}>
              {isCreating ? 'Creation...' : 'Creer'}
            </Button>
          </div>
          <div className="flex flex-wrap gap-1">
            {DEFAULT_COLORS.map((color) => (
              <button
                key={color}
                type="button"
                onClick={() => setNewTagColor(color)}
                className={cn(
                  'h-6 w-6 rounded-full transition-transform',
                  newTagColor === color && 'ring-2 ring-accent ring-offset-2 scale-110'
                )}
                style={{ backgroundColor: color }}
              />
            ))}
          </div>
        </div>

        {/* Existing tags */}
        <div className="space-y-3">
          <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300">Tags existants</h4>
          {tags.length === 0 ? (
            <p className="text-sm text-gray-500 dark:text-gray-400">Aucun tag cree</p>
          ) : (
            <div className="space-y-2 max-h-64 overflow-y-auto">
              {tags.map((tag) => (
                <div
                  key={tag.id}
                  className="flex items-center justify-between rounded-lg bg-gray-50 p-2 dark:bg-gray-800/50"
                >
                  {editingTag?.id === tag.id ? (
                    <div className="flex-1 flex items-center gap-2">
                      <input
                        type="text"
                        value={editName}
                        onChange={(e) => setEditName(e.target.value)}
                        className="flex-1 rounded border border-gray-300 px-2 py-1 text-sm dark:border-gray-600 dark:bg-dark-card"
                      />
                      <div className="flex gap-1">
                        {DEFAULT_COLORS.slice(0, 8).map((color) => (
                          <button
                            key={color}
                            type="button"
                            onClick={() => setEditColor(color)}
                            className={cn(
                              'h-5 w-5 rounded-full',
                              editColor === color && 'ring-2 ring-accent ring-offset-1'
                            )}
                            style={{ backgroundColor: color }}
                          />
                        ))}
                      </div>
                      <button
                        onClick={handleUpdate}
                        className="rounded bg-accent px-2 py-1 text-xs text-white"
                      >
                        OK
                      </button>
                      <button
                        onClick={() => setEditingTag(null)}
                        className="rounded border px-2 py-1 text-xs"
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </div>
                  ) : (
                    <>
                      <span
                        className="inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium"
                        style={{ backgroundColor: tag.color, color: getContrastColor(tag.color) }}
                      >
                        {tag.name}
                      </span>
                      <div className="flex gap-1">
                        <button
                          onClick={() => startEdit(tag)}
                          className="rounded p-1 text-gray-400 hover:bg-gray-200 hover:text-gray-600 dark:hover:bg-gray-700 dark:hover:text-gray-300"
                          title="Modifier"
                        >
                          <Edit2 className="h-3.5 w-3.5" />
                        </button>
                        <button
                          onClick={() => handleDelete(tag.id)}
                          className="rounded p-1 text-gray-400 hover:bg-red-100 hover:text-red-600 dark:hover:bg-red-900/30 dark:hover:text-red-400"
                          title="Supprimer"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        <Button onClick={onClose} variant="secondary" className="w-full">
          Fermer
        </Button>
      </div>
    </Modal>
  );
}

// Tag Management Modal (for documents)
interface TagManagementModalProps {
  isOpen: boolean;
  onClose: () => void;
  tags: TagType[];
  onCreateTag: (name: string, color: string) => Promise<TagType>;
  onDeleteTag: (tagId: string) => Promise<void>;
  onUpdateTag: (tagId: string, name: string, color: string) => Promise<void>;
}

function TagManagementModal({
  isOpen,
  onClose,
  tags,
  onCreateTag,
  onDeleteTag,
  onUpdateTag,
}: TagManagementModalProps) {
  const [newTagName, setNewTagName] = useState('');
  const [newTagColor, setNewTagColor] = useState(DEFAULT_COLORS[0]);
  const [isCreating, setIsCreating] = useState(false);
  const [editingTag, setEditingTag] = useState<TagType | null>(null);
  const [editName, setEditName] = useState('');
  const [editColor, setEditColor] = useState('');

  const handleCreate = async () => {
    if (!newTagName.trim() || isCreating) return;
    setIsCreating(true);
    try {
      await onCreateTag(newTagName.trim(), newTagColor);
      setNewTagName('');
      setNewTagColor(DEFAULT_COLORS[Math.floor(Math.random() * DEFAULT_COLORS.length)]);
      toast.success('Tag cree');
    } catch (error) {
      toast.error('Erreur lors de la creation du tag');
    } finally {
      setIsCreating(false);
    }
  };

  const handleDelete = async (tagId: string) => {
    if (!confirm('Supprimer ce tag ? Il sera retire de tous les documents.')) return;
    try {
      await onDeleteTag(tagId);
      toast.success('Tag supprime');
    } catch (error) {
      toast.error('Erreur lors de la suppression');
    }
  };

  const handleUpdate = async () => {
    if (!editingTag || !editName.trim()) return;
    try {
      await onUpdateTag(editingTag.id, editName.trim(), editColor);
      setEditingTag(null);
      toast.success('Tag mis a jour');
    } catch (error) {
      toast.error('Erreur lors de la mise a jour');
    }
  };

  const startEdit = (tag: TagType) => {
    setEditingTag(tag);
    setEditName(tag.name);
    setEditColor(tag.color);
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Gerer les tags">
      <div className="space-y-6">
        {/* Create new tag */}
        <div className="space-y-3">
          <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300">Nouveau tag</h4>
          <div className="flex gap-2">
            <Input
              value={newTagName}
              onChange={(e) => setNewTagName(e.target.value)}
              placeholder="Nom du tag"
              className="flex-1"
            />
            <Button onClick={handleCreate} disabled={!newTagName.trim() || isCreating}>
              {isCreating ? 'Creation...' : 'Creer'}
            </Button>
          </div>
          <div className="flex flex-wrap gap-1">
            {DEFAULT_COLORS.map((color) => (
              <button
                key={color}
                type="button"
                onClick={() => setNewTagColor(color)}
                className={cn(
                  'h-6 w-6 rounded-full transition-transform',
                  newTagColor === color && 'ring-2 ring-accent ring-offset-2 scale-110'
                )}
                style={{ backgroundColor: color }}
              />
            ))}
          </div>
        </div>

        {/* Existing tags */}
        <div className="space-y-3">
          <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300">Tags existants</h4>
          {tags.length === 0 ? (
            <p className="text-sm text-gray-500 dark:text-gray-400">Aucun tag cree</p>
          ) : (
            <div className="space-y-2 max-h-64 overflow-y-auto">
              {tags.map((tag) => (
                <div
                  key={tag.id}
                  className="flex items-center justify-between rounded-lg bg-gray-50 p-2 dark:bg-gray-800/50"
                >
                  {editingTag?.id === tag.id ? (
                    <div className="flex-1 flex items-center gap-2">
                      <input
                        type="text"
                        value={editName}
                        onChange={(e) => setEditName(e.target.value)}
                        className="flex-1 rounded border border-gray-300 px-2 py-1 text-sm dark:border-gray-600 dark:bg-dark-card"
                      />
                      <div className="flex gap-1">
                        {DEFAULT_COLORS.slice(0, 8).map((color) => (
                          <button
                            key={color}
                            type="button"
                            onClick={() => setEditColor(color)}
                            className={cn(
                              'h-5 w-5 rounded-full',
                              editColor === color && 'ring-2 ring-accent ring-offset-1'
                            )}
                            style={{ backgroundColor: color }}
                          />
                        ))}
                      </div>
                      <button
                        onClick={handleUpdate}
                        className="rounded bg-accent px-2 py-1 text-xs text-white"
                      >
                        OK
                      </button>
                      <button
                        onClick={() => setEditingTag(null)}
                        className="rounded border px-2 py-1 text-xs"
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </div>
                  ) : (
                    <>
                      <TagChip tag={tag} />
                      <div className="flex gap-1">
                        <button
                          onClick={() => startEdit(tag)}
                          className="rounded p-1 text-gray-400 hover:bg-gray-200 hover:text-gray-600 dark:hover:bg-gray-700 dark:hover:text-gray-300"
                          title="Modifier"
                        >
                          <Edit2 className="h-3.5 w-3.5" />
                        </button>
                        <button
                          onClick={() => handleDelete(tag.id)}
                          className="rounded p-1 text-gray-400 hover:bg-red-100 hover:text-red-600 dark:hover:bg-red-900/30 dark:hover:text-red-400"
                          title="Supprimer"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        <Button onClick={onClose} variant="secondary" className="w-full">
          Fermer
        </Button>
      </div>
    </Modal>
  );
}

// Document Tags Modal
interface DocumentTagsModalProps {
  isOpen: boolean;
  onClose: () => void;
  document: OrganizationDocument;
  availableTags: TagType[];
  onAddTag: (tagId: string) => Promise<void>;
  onRemoveTag: (tagId: string) => Promise<void>;
  onCreateTag: (name: string, color: string) => Promise<TagType>;
}

function DocumentTagsModal({
  isOpen,
  onClose,
  document,
  availableTags,
  onAddTag,
  onRemoveTag,
  onCreateTag,
}: DocumentTagsModalProps) {
  const [isAdding, setIsAdding] = useState(false);
  const [newTagName, setNewTagName] = useState('');
  const [newTagColor, setNewTagColor] = useState(DEFAULT_COLORS[0]);

  const documentTagIds = new Set(document.tags?.map((t) => t.id) || []);
  const unassignedTags = availableTags.filter((t) => !documentTagIds.has(t.id));

  const handleAddTag = async (tagId: string) => {
    setIsAdding(true);
    try {
      await onAddTag(tagId);
    } finally {
      setIsAdding(false);
    }
  };

  const handleRemoveTag = async (tagId: string) => {
    await onRemoveTag(tagId);
  };

  const handleCreateAndAdd = async () => {
    if (!newTagName.trim()) return;
    setIsAdding(true);
    try {
      const newTag = await onCreateTag(newTagName.trim(), newTagColor);
      await onAddTag(newTag.id);
      setNewTagName('');
      setNewTagColor(DEFAULT_COLORS[Math.floor(Math.random() * DEFAULT_COLORS.length)]);
    } catch (error) {
      toast.error('Erreur lors de la creation du tag');
    } finally {
      setIsAdding(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Tags du document">
      <div className="space-y-4">
        <div>
          <p className="text-sm text-gray-600 dark:text-gray-400 mb-2 truncate">
            {document.originalFileName}
          </p>
        </div>

        {/* Current tags */}
        <div>
          <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Tags actuels
          </h4>
          {document.tags && document.tags.length > 0 ? (
            <div className="flex flex-wrap gap-2">
              {document.tags.map((tag) => (
                <TagChip key={tag.id} tag={tag} onRemove={() => handleRemoveTag(tag.id)} />
              ))}
            </div>
          ) : (
            <p className="text-sm text-gray-500 dark:text-gray-400">Aucun tag</p>
          )}
        </div>

        {/* Add existing tags */}
        {unassignedTags.length > 0 && (
          <div>
            <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Ajouter un tag
            </h4>
            <div className="flex flex-wrap gap-2">
              {unassignedTags.map((tag) => (
                <button
                  key={tag.id}
                  onClick={() => handleAddTag(tag.id)}
                  disabled={isAdding}
                  className="transition-opacity hover:opacity-80 disabled:opacity-50"
                >
                  <TagChip tag={tag} />
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Create new tag */}
        <div className="border-t border-gray-200 dark:border-gray-700 pt-4">
          <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Creer un nouveau tag
          </h4>
          <div className="flex gap-2 mb-2">
            <Input
              value={newTagName}
              onChange={(e) => setNewTagName(e.target.value)}
              placeholder="Nom du tag"
              className="flex-1"
            />
            <Button
              onClick={handleCreateAndAdd}
              disabled={!newTagName.trim() || isAdding}
              size="sm"
            >
              Creer & Ajouter
            </Button>
          </div>
          <div className="flex flex-wrap gap-1">
            {DEFAULT_COLORS.map((color) => (
              <button
                key={color}
                type="button"
                onClick={() => setNewTagColor(color)}
                className={cn(
                  'h-5 w-5 rounded-full transition-transform',
                  newTagColor === color && 'ring-2 ring-accent ring-offset-1 scale-110'
                )}
                style={{ backgroundColor: color }}
              />
            ))}
          </div>
        </div>

        <Button onClick={onClose} variant="secondary" className="w-full">
          Fermer
        </Button>
      </div>
    </Modal>
  );
}
