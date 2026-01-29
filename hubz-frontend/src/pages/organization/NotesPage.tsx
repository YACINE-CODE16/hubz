import { useEffect, useState, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { StickyNote, Plus, Edit2, Trash2, Tag, FileText, Link as LinkIcon, Upload, Download, File, FolderOpen } from 'lucide-react';
import toast from 'react-hot-toast';
import Card from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Modal from '../../components/ui/Modal';
import Input from '../../components/ui/Input';
import { noteService } from '../../services/note.service';
import { attachmentService } from '../../services/attachment.service';
import { organizationDocumentService } from '../../services/organizationDocument.service';
import type { Note, CreateNoteRequest, UpdateNoteRequest } from '../../types/note';
import type { NoteAttachment } from '../../types/attachment';
import type { OrganizationDocument } from '../../types/organizationDocument';
import { cn } from '../../lib/utils';

const CATEGORIES = ['Réunions', 'Idées', 'Documentation', 'Documents projet', 'Autre'];

export default function NotesPage() {
  const { orgId } = useParams<{ orgId: string }>();
  const [notes, setNotes] = useState<Note[]>([]);
  const [organizationDocuments, setOrganizationDocuments] = useState<OrganizationDocument[]>([]);
  const [loading, setLoading] = useState(true);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [selectedNote, setSelectedNote] = useState<Note | null>(null);
  const [selectedCategory, setSelectedCategory] = useState<string | undefined>(undefined);
  const [uploadingOrgDoc, setUploadingOrgDoc] = useState(false);
  const [dragActiveOrgDoc, setDragActiveOrgDoc] = useState(false);

  const fetchNotes = useCallback(async () => {
    if (!orgId) return;
    setLoading(true);
    try {
      const data = await noteService.getByOrganization(orgId, selectedCategory);
      setNotes(data);
    } catch (error) {
      toast.error('Erreur lors du chargement des notes');
      console.error(error);
    } finally {
      setLoading(false);
    }
  }, [orgId, selectedCategory]);

  const fetchOrganizationDocuments = useCallback(async () => {
    if (!orgId) return;
    try {
      const data = await organizationDocumentService.getDocuments(orgId);
      setOrganizationDocuments(data);
    } catch (error) {
      console.error('Erreur lors du chargement des documents:', error);
    }
  }, [orgId]);

  useEffect(() => {
    fetchNotes();
    fetchOrganizationDocuments();
  }, [fetchNotes, fetchOrganizationDocuments]);

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
      await noteService.create(orgId, data);
      toast.success('Note créée');
      setIsCreateModalOpen(false);
      await fetchNotes();
    } catch (error) {
      toast.error('Erreur lors de la création');
    }
  };

  const handleUpdate = async (data: UpdateNoteRequest) => {
    if (!selectedNote) return;
    try {
      await noteService.update(selectedNote.id, data);
      toast.success('Note mise à jour');
      setIsEditModalOpen(false);
      setSelectedNote(null);
      await fetchNotes();
    } catch (error) {
      toast.error('Erreur lors de la mise à jour');
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Supprimer cette note ?')) return;
    try {
      await noteService.delete(id);
      toast.success('Note supprimée');
      await fetchNotes();
    } catch (error) {
      toast.error('Erreur lors de la suppression');
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
      toast.success(`${files.length} document(s) uploadé(s)`);
      await fetchOrganizationDocuments();
    } catch (error) {
      toast.error('Erreur lors de l\'upload');
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
      toast.error('Erreur lors du téléchargement');
      console.error(error);
    }
  };

  const handleDeleteOrgDoc = async (documentId: string) => {
    if (!confirm('Supprimer ce document ?')) return;

    try {
      await organizationDocumentService.deleteDocument(documentId);
      toast.success('Document supprimé');
      await fetchOrganizationDocuments();
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

  // Get categories from existing notes
  const existingCategories = Array.from(
    new Set(notes.map((n) => n.category).filter(Boolean))
  );

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
          Notes et documents partagés de votre organisation
        </p>
      </div>

      {/* Main Layout: 2 Columns */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 h-full overflow-hidden">
        {/* LEFT COLUMN: Notes */}
        <div className="flex flex-col gap-4 overflow-auto">
          <div className="flex items-center justify-between sticky top-0 bg-light-base dark:bg-dark-base pb-2 z-10">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
              <StickyNote className="h-5 w-5" />
              Notes
            </h3>
            <Button onClick={() => setIsCreateModalOpen(true)} size="sm">
              <Plus className="h-4 w-4" />
              Nouvelle note
            </Button>
          </div>

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
                Commencez par créer votre première note.
              </p>
            </Card>
          ) : (
            <div className="grid gap-4 sm:grid-cols-1 xl:grid-cols-2">
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
                />
              ))}
            </div>
          )}
        </div>

        {/* RIGHT COLUMN: Organization Documents */}
        <div className="flex flex-col gap-4 overflow-auto">
          <div className="flex items-center justify-between sticky top-0 bg-light-base dark:bg-dark-base pb-2 z-10">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
              <FolderOpen className="h-5 w-5" />
              Documents de l'organisation
            </h3>
          </div>

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
                <p className="mt-2 text-xs text-gray-400">
                  Max 10MB par fichier
                </p>
              </div>
            </div>
          </div>

          {/* Documents List */}
          <div className="space-y-2">
            {organizationDocuments.length === 0 ? (
              <div className="text-center py-8 text-sm text-gray-500 dark:text-gray-400">
                Aucun document uploadé
              </div>
            ) : (
              organizationDocuments.map((doc) => (
                <div
                  key={doc.id}
                  className="group flex items-center justify-between rounded-lg bg-light-card dark:bg-dark-card p-3 hover:bg-light-hover dark:hover:bg-dark-hover transition-colors"
                >
                  <div className="flex items-center gap-3 flex-1 min-w-0">
                    <File className="h-5 w-5 flex-shrink-0 text-blue-600 dark:text-blue-400" />
                    <div className="flex-1 min-w-0">
                      <p className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">
                        {doc.originalFileName}
                      </p>
                      <p className="text-xs text-gray-500 dark:text-gray-400">
                        {formatFileSize(doc.fileSize)} • {formatDate(doc.uploadedAt)}
                      </p>
                    </div>
                  </div>
                  <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                    <button
                      onClick={() => handleDownloadOrgDoc(doc)}
                      className="rounded-lg p-2 text-gray-400 hover:bg-blue-100 hover:text-blue-600 dark:hover:bg-blue-900/30 dark:hover:text-blue-400 transition-colors"
                      title="Télécharger"
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
              ))
            )}
          </div>
        </div>
      </div>

      {/* Create Modal */}
      {isCreateModalOpen && (
        <CreateNoteModal
          isOpen={isCreateModalOpen}
          onClose={() => setIsCreateModalOpen(false)}
          onCreate={handleCreate}
        />
      )}

      {/* Edit Modal */}
      {selectedNote && (
        <EditNoteModal
          isOpen={isEditModalOpen}
          onClose={() => {
            setIsEditModalOpen(false);
            setSelectedNote(null);
          }}
          onUpdate={handleUpdate}
          note={selectedNote}
        />
      )}

      {/* View Modal */}
      {selectedNote && (
        <ViewNoteModal
          isOpen={isViewModalOpen}
          onClose={() => {
            setIsViewModalOpen(false);
            setSelectedNote(null);
          }}
          note={selectedNote}
        />
      )}
    </div>
  );
}

interface NoteCardProps {
  note: Note;
  onView: (note: Note) => void;
  onEdit: (note: Note) => void;
  onDelete: (id: string) => void;
}

function NoteCard({ note, onView, onEdit, onDelete }: NoteCardProps) {
  const isDocument = note.category === 'Documents projet';
  const hasLink = note.content.includes('http://') || note.content.includes('https://');
  const preview = note.content.slice(0, 150) + (note.content.length > 150 ? '...' : '');

  const categoryColors: Record<string, string> = {
    Réunions: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300',
    Idées: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300',
    Documentation: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300',
    'Documents projet': 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-300',
    Autre: 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300',
  };

  return (
    <Card
      className="flex flex-col gap-3 p-6 transition-all hover:scale-105 hover:shadow-lg cursor-pointer"
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
      <p className="text-sm text-gray-600 dark:text-gray-400 line-clamp-3">{preview}</p>

      {hasLink && !isDocument && (
        <div className="flex items-center gap-1 text-xs text-accent">
          <LinkIcon className="h-3 w-3" />
          <span>Contient un lien</span>
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
}

function CreateNoteModal({ isOpen, onClose, onCreate }: CreateNoteModalProps) {
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) return;

    onCreate({
      title: title.trim(),
      content: content.trim(),
      category: category || undefined,
    });

    setTitle('');
    setContent('');
    setCategory('');
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Nouvelle note">
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          label="Titre"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="Ex: Réunion du 27 janvier"
          required
        />

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Catégorie
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

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Contenu
          </label>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder={
              category === 'Documents projet'
                ? 'Ajoutez des liens vers vos documents : https://...'
                : 'Écrivez votre note ici...'
            }
            rows={10}
            required
            className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 placeholder-gray-400 focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent dark:border-gray-600 dark:bg-dark-card dark:text-gray-100 dark:placeholder-gray-500"
          />
          {category === 'Documents projet' && (
            <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
              💡 Ajoutez des liens vers vos documents (Google Drive, Notion, etc.)
            </p>
          )}
        </div>

        <div className="flex gap-2">
          <Button type="button" variant="secondary" onClick={onClose} className="flex-1">
            Annuler
          </Button>
          <Button type="submit" className="flex-1">
            Créer
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
}

function EditNoteModal({ isOpen, onClose, onUpdate, note }: EditNoteModalProps) {
  const [title, setTitle] = useState(note.title);
  const [content, setContent] = useState(note.content);
  const [category, setCategory] = useState(note.category || '');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) return;

    onUpdate({
      title: title.trim(),
      content: content.trim(),
      category: category || undefined,
    });
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Modifier la note">
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          label="Titre"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
        />

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Catégorie
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

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Contenu
          </label>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={10}
            required
            className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 placeholder-gray-400 focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent dark:border-gray-600 dark:bg-dark-card dark:text-gray-100 dark:placeholder-gray-500"
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
}

function ViewNoteModal({ isOpen, onClose, note }: ViewNoteModalProps) {
  const [attachments, setAttachments] = useState<NoteAttachment[]>([]);
  const [uploading, setUploading] = useState(false);
  const [dragActive, setDragActive] = useState(false);

  useEffect(() => {
    if (isOpen) {
      fetchAttachments();
    }
  }, [isOpen, note.id]);

  // Prevent browser default drag & drop behavior (only outside drop zones)
  useEffect(() => {
    const preventDefaults = (e: DragEvent) => {
      // Only prevent if not dropping on our upload zone
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
      toast.success(`${files.length} fichier(s) uploadé(s)`);
      await fetchAttachments();
    } catch (error) {
      toast.error('Erreur lors de l\'upload');
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
      toast.error('Erreur lors du téléchargement');
      console.error(error);
    }
  };

  const handleDelete = async (attachmentId: string) => {
    if (!confirm('Supprimer ce fichier ?')) return;

    try {
      await attachmentService.deleteAttachment(attachmentId);
      toast.success('Fichier supprimé');
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

  // Convert URLs in content to clickable links
  const renderContent = (text: string) => {
    const urlRegex = /(https?:\/\/[^\s]+)/g;
    const parts = text.split(urlRegex);

    return parts.map((part, index) => {
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
    });
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={note.title} className="max-w-5xl">
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Section Note */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
              Note
            </h3>
            {note.category && (
              <div className="flex items-center gap-2">
                <Tag className="h-4 w-4 text-gray-400" />
                <span className="text-sm font-medium text-gray-600 dark:text-gray-400">
                  {note.category}
                </span>
              </div>
            )}
          </div>

          <div className="rounded-lg bg-gray-50 p-4 dark:bg-gray-800/50 min-h-[300px]">
            <div className="whitespace-pre-wrap text-sm text-gray-700 dark:text-gray-300">
              {renderContent(note.content)}
            </div>
          </div>

          <div className="text-xs text-gray-500 dark:text-gray-400 space-y-1">
            <p>Créé le {new Date(note.createdAt).toLocaleString('fr-FR')}</p>
            <p>Modifié le {new Date(note.updatedAt).toLocaleString('fr-FR')}</p>
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
                <p className="mt-2 text-xs text-gray-400">
                  Max 10MB par fichier
                </p>
              </div>
            </div>
          </div>

          {/* Attachments List */}
          <div className="space-y-2 max-h-[400px] overflow-y-auto">
            {attachments.length === 0 ? (
              <div className="text-center py-8 text-sm text-gray-500 dark:text-gray-400">
                Aucun document attaché
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
                        {formatFileSize(attachment.fileSize)} • {formatDate(attachment.uploadedAt)}
                      </p>
                    </div>
                  </div>
                  <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                    <button
                      onClick={() => handleDownload(attachment)}
                      className="rounded-lg p-2 text-gray-400 hover:bg-blue-100 hover:text-blue-600 dark:hover:bg-blue-900/30 dark:hover:text-blue-400 transition-colors"
                      title="Télécharger"
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
  );
}
