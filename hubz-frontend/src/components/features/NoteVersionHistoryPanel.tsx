import { useState, useEffect } from 'react';
import { History, RotateCcw, ChevronDown, ChevronUp, User, Calendar, Loader2, X, Eye } from 'lucide-react';
import toast from 'react-hot-toast';
import Button from '../ui/Button';
import { noteVersionService } from '../../services/note.service';
import type { NoteVersion } from '../../types/note';
import { cn } from '../../lib/utils';

interface NoteVersionHistoryPanelProps {
  noteId: string;
  isOpen: boolean;
  onClose: () => void;
  onRestore: () => void;
}

function formatDate(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString('fr-FR', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default function NoteVersionHistoryPanel({
  noteId,
  isOpen,
  onClose,
  onRestore,
}: NoteVersionHistoryPanelProps) {
  const [versions, setVersions] = useState<NoteVersion[]>([]);
  const [loading, setLoading] = useState(false);
  const [restoring, setRestoring] = useState<string | null>(null);
  const [expandedVersionId, setExpandedVersionId] = useState<string | null>(null);
  const [previewVersion, setPreviewVersion] = useState<NoteVersion | null>(null);

  useEffect(() => {
    if (isOpen && noteId) {
      fetchVersions();
    }
  }, [isOpen, noteId]);

  const fetchVersions = async () => {
    setLoading(true);
    try {
      const data = await noteVersionService.getVersions(noteId);
      setVersions(data);
    } catch (error) {
      console.error('Error fetching versions:', error);
      toast.error('Erreur lors du chargement des versions');
    } finally {
      setLoading(false);
    }
  };

  const handleRestore = async (versionId: string) => {
    if (!confirm('Restaurer cette version ? La version actuelle sera sauvegardee.')) {
      return;
    }

    setRestoring(versionId);
    try {
      await noteVersionService.restoreVersion(noteId, versionId);
      toast.success('Version restauree avec succes');
      onRestore();
      fetchVersions();
    } catch (error) {
      console.error('Error restoring version:', error);
      toast.error('Erreur lors de la restauration');
    } finally {
      setRestoring(null);
    }
  };

  const toggleExpanded = (versionId: string) => {
    setExpandedVersionId(expandedVersionId === versionId ? null : versionId);
  };

  if (!isOpen) return null;

  return (
    <>
      <div className="fixed inset-0 bg-black/50 z-40" onClick={onClose} />
      <div className="fixed right-0 top-0 h-full w-full max-w-md bg-dark-card border-l border-gray-700 shadow-xl z-50 flex flex-col overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-gray-700">
          <div className="flex items-center gap-2">
            <History className="w-5 h-5 text-blue-500" />
            <h2 className="text-lg font-semibold text-white">Historique des versions</h2>
          </div>
          <button
            onClick={onClose}
            className="p-1 hover:bg-gray-700 rounded-lg transition-colors"
          >
            <X className="w-5 h-5 text-gray-400" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-4">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="w-8 h-8 animate-spin text-blue-500" />
            </div>
          ) : versions.length === 0 ? (
            <div className="text-center py-12">
              <History className="w-12 h-12 text-gray-500 mx-auto mb-3" />
              <p className="text-gray-400">Aucune version anterieure</p>
              <p className="text-gray-500 text-sm mt-1">
                Les versions sont creees automatiquement lors des modifications.
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {versions.map((version, index) => (
                <div
                  key={version.id}
                  className={cn(
                    'bg-dark-hover rounded-xl border transition-all',
                    index === 0
                      ? 'border-blue-500/30 bg-blue-500/5'
                      : 'border-gray-700'
                  )}
                >
                  {/* Version Header */}
                  <div
                    className="p-4 cursor-pointer"
                    onClick={() => toggleExpanded(version.id)}
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1">
                          <span className="text-sm font-medium text-blue-400">
                            Version {version.versionNumber}
                          </span>
                          {index === 0 && (
                            <span className="px-2 py-0.5 bg-blue-500/20 text-blue-400 text-xs rounded-full">
                              Plus recente
                            </span>
                          )}
                        </div>
                        <h3 className="text-white font-medium truncate">
                          {version.title}
                        </h3>
                        <div className="flex items-center gap-4 mt-2 text-sm text-gray-400">
                          <div className="flex items-center gap-1">
                            <User className="w-3.5 h-3.5" />
                            <span>{version.createdByName}</span>
                          </div>
                          <div className="flex items-center gap-1">
                            <Calendar className="w-3.5 h-3.5" />
                            <span>{formatDate(version.createdAt)}</span>
                          </div>
                        </div>
                      </div>
                      <div className="flex items-center gap-2 ml-4">
                        {expandedVersionId === version.id ? (
                          <ChevronUp className="w-5 h-5 text-gray-400" />
                        ) : (
                          <ChevronDown className="w-5 h-5 text-gray-400" />
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Expanded Content */}
                  {expandedVersionId === version.id && (
                    <div className="px-4 pb-4 border-t border-gray-700/50 pt-3">
                      <div className="mb-3">
                        <p className="text-xs text-gray-500 uppercase mb-1">Apercu du contenu</p>
                        <div className="bg-dark-base rounded-lg p-3 max-h-40 overflow-y-auto">
                          <p className="text-gray-300 text-sm whitespace-pre-wrap line-clamp-6">
                            {version.content || '(Contenu vide)'}
                          </p>
                        </div>
                      </div>
                      <div className="flex gap-2">
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={(e) => {
                            e.stopPropagation();
                            setPreviewVersion(version);
                          }}
                          className="flex-1"
                        >
                          <Eye className="w-4 h-4 mr-1" />
                          Voir
                        </Button>
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleRestore(version.id);
                          }}
                          disabled={restoring === version.id}
                          className="flex-1"
                        >
                          {restoring === version.id ? (
                            <Loader2 className="w-4 h-4 animate-spin mr-1" />
                          ) : (
                            <RotateCcw className="w-4 h-4 mr-1" />
                          )}
                          Restaurer
                        </Button>
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-gray-700">
          <p className="text-xs text-gray-500 text-center">
            {versions.length} version{versions.length !== 1 ? 's' : ''} disponible{versions.length !== 1 ? 's' : ''}
          </p>
        </div>
      </div>

      {/* Preview Modal */}
      {previewVersion && (
        <>
          <div
            className="fixed inset-0 bg-black/70 z-50"
            onClick={() => setPreviewVersion(null)}
          />
          <div className="fixed inset-4 md:inset-8 lg:inset-16 bg-dark-card rounded-xl shadow-2xl z-50 flex flex-col overflow-hidden">
            <div className="flex items-center justify-between p-4 border-b border-gray-700">
              <div>
                <h3 className="text-lg font-semibold text-white">
                  {previewVersion.title}
                </h3>
                <p className="text-sm text-gray-400">
                  Version {previewVersion.versionNumber} - {formatDate(previewVersion.createdAt)}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => {
                    handleRestore(previewVersion.id);
                    setPreviewVersion(null);
                  }}
                  disabled={restoring === previewVersion.id}
                >
                  {restoring === previewVersion.id ? (
                    <Loader2 className="w-4 h-4 animate-spin mr-1" />
                  ) : (
                    <RotateCcw className="w-4 h-4 mr-1" />
                  )}
                  Restaurer cette version
                </Button>
                <button
                  onClick={() => setPreviewVersion(null)}
                  className="p-2 hover:bg-gray-700 rounded-lg transition-colors"
                >
                  <X className="w-5 h-5 text-gray-400" />
                </button>
              </div>
            </div>
            <div className="flex-1 overflow-y-auto p-6">
              <div className="prose prose-invert max-w-none">
                <pre className="whitespace-pre-wrap text-gray-300 font-sans text-base leading-relaxed">
                  {previewVersion.content}
                </pre>
              </div>
            </div>
          </div>
        </>
      )}
    </>
  );
}
