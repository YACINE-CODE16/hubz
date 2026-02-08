import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import {
  X,
  Download,
  FileText,
  Image as ImageIcon,
  FileCode,
  File,
  Loader2,
  AlertCircle,
  Maximize2,
  Minimize2,
} from 'lucide-react';
import { organizationDocumentService } from '../../services/organizationDocument.service';
import type { OrganizationDocument, DocumentPreviewResponse } from '../../types/organizationDocument';
import { cn } from '../../lib/utils';
import { useAuthStore } from '../../stores/authStore';
import DocumentVersionHistory from './DocumentVersionHistory';

interface DocumentPreviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  document: OrganizationDocument;
  onDocumentUpdated?: () => void;
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

export default function DocumentPreviewModal({
  isOpen,
  onClose,
  document,
  onDocumentUpdated,
}: DocumentPreviewModalProps) {
  const [preview, setPreview] = useState<DocumentPreviewResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const { token } = useAuthStore();

  useEffect(() => {
    if (!isOpen) return;

    const fetchPreview = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await organizationDocumentService.getDocumentPreview(document.id);
        setPreview(data);
      } catch (err) {
        setError('Erreur lors du chargement de la previsualisation');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchPreview();
  }, [isOpen, document.id]);

  useEffect(() => {
    if (!isOpen) return;

    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        if (isFullscreen) {
          setIsFullscreen(false);
        } else {
          onClose();
        }
      }
    };

    window.addEventListener('keydown', handleEsc);
    document.body.style.overflow = 'hidden';

    return () => {
      window.removeEventListener('keydown', handleEsc);
      document.body.style.overflow = '';
    };
  }, [isOpen, onClose, isFullscreen]);

  const handleDownload = async () => {
    try {
      const blob = await organizationDocumentService.downloadDocument(document.id);
      const url = window.URL.createObjectURL(blob);
      const link = window.document.createElement('a');
      link.href = url;
      link.download = document.originalFileName;
      window.document.body.appendChild(link);
      link.click();
      window.document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Erreur lors du telechargement:', err);
    }
  };

  const getPreviewIcon = (previewType: string) => {
    switch (previewType) {
      case 'image':
        return <ImageIcon className="h-12 w-12 text-blue-400" />;
      case 'pdf':
        return <FileText className="h-12 w-12 text-red-400" />;
      case 'text':
        return <FileCode className="h-12 w-12 text-green-400" />;
      default:
        return <File className="h-12 w-12 text-gray-400" />;
    }
  };

  const renderPreviewContent = () => {
    if (loading) {
      return (
        <div className="flex flex-col items-center justify-center py-16">
          <Loader2 className="h-12 w-12 animate-spin text-blue-500" />
          <p className="mt-4 text-gray-500 dark:text-gray-400">Chargement de la previsualisation...</p>
        </div>
      );
    }

    if (error) {
      return (
        <div className="flex flex-col items-center justify-center py-16">
          <AlertCircle className="h-12 w-12 text-red-500" />
          <p className="mt-4 text-red-500">{error}</p>
        </div>
      );
    }

    if (!preview) {
      return null;
    }

    if (!preview.previewable) {
      return (
        <div className="flex flex-col items-center justify-center py-16">
          {getPreviewIcon('unsupported')}
          <p className="mt-4 text-gray-500 dark:text-gray-400">
            Ce type de fichier ne peut pas etre previsualise
          </p>
          <p className="mt-2 text-sm text-gray-400 dark:text-gray-500">
            Type: {document.contentType || 'Inconnu'}
          </p>
          <button
            onClick={handleDownload}
            className="mt-6 flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 transition-colors"
          >
            <Download className="h-4 w-4" />
            Telecharger le fichier
          </button>
        </div>
      );
    }

    const previewContentUrl = `${organizationDocumentService.getPreviewContentUrl(document.id)}?token=${token}`;

    switch (preview.previewType) {
      case 'image':
        return (
          <div className="flex items-center justify-center p-4">
            <img
              src={previewContentUrl}
              alt={document.originalFileName}
              className="max-h-[70vh] max-w-full rounded-lg object-contain"
              onError={(e) => {
                (e.target as HTMLImageElement).style.display = 'none';
                setError('Erreur lors du chargement de l\'image');
              }}
            />
          </div>
        );

      case 'pdf':
        return (
          <div className="h-[70vh] w-full">
            <iframe
              src={previewContentUrl}
              className="h-full w-full rounded-lg border-0"
              title={document.originalFileName}
            />
          </div>
        );

      case 'text':
        return (
          <div className="max-h-[70vh] overflow-auto rounded-lg bg-gray-900 p-4">
            <pre className="whitespace-pre-wrap break-words font-mono text-sm text-gray-100">
              {preview.textContent || '[Contenu vide]'}
            </pre>
          </div>
        );

      default:
        return null;
    }
  };

  if (!isOpen) return null;

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div
        className="absolute inset-0 bg-black/70 backdrop-blur-sm"
        onClick={onClose}
      />
      <div
        className={cn(
          'relative z-10 flex flex-col rounded-xl border border-gray-200/50 dark:border-white/10 bg-light-card/95 dark:bg-dark-card/95 backdrop-blur-xl shadow-2xl transition-all duration-200',
          isFullscreen
            ? 'fixed inset-4'
            : 'w-full max-w-4xl max-h-[90vh]'
        )}
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b border-gray-200/50 dark:border-white/10 px-6 py-4">
          <div className="flex items-center gap-3 min-w-0">
            {preview && getPreviewIcon(preview.previewType)}
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <h2 className="truncate text-lg font-semibold text-gray-900 dark:text-gray-100">
                  {document.originalFileName}
                </h2>
                {document.totalVersions && document.totalVersions > 1 && (
                  <span className="px-2 py-0.5 text-xs font-medium bg-blue-100 dark:bg-blue-900 text-blue-600 dark:text-blue-400 rounded-full">
                    v{document.currentVersionNumber} ({document.totalVersions} versions)
                  </span>
                )}
              </div>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                {formatFileSize(document.fileSize)}
                {preview && ` - ${preview.previewType === 'unsupported' ? 'Non previewable' : preview.previewType.toUpperCase()}`}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={handleDownload}
              className="rounded-lg p-2 text-gray-500 hover:bg-light-hover dark:hover:bg-dark-hover hover:text-gray-700 dark:hover:text-gray-200 transition-colors"
              title="Telecharger"
            >
              <Download className="h-5 w-5" />
            </button>
            <button
              onClick={() => setIsFullscreen(!isFullscreen)}
              className="rounded-lg p-2 text-gray-500 hover:bg-light-hover dark:hover:bg-dark-hover hover:text-gray-700 dark:hover:text-gray-200 transition-colors"
              title={isFullscreen ? 'Reduire' : 'Plein ecran'}
            >
              {isFullscreen ? (
                <Minimize2 className="h-5 w-5" />
              ) : (
                <Maximize2 className="h-5 w-5" />
              )}
            </button>
            <button
              onClick={onClose}
              className="rounded-lg p-2 text-gray-500 hover:bg-light-hover dark:hover:bg-dark-hover hover:text-gray-700 dark:hover:text-gray-200 transition-colors"
              title="Fermer"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-auto p-4">
          {renderPreviewContent()}

          {/* Version History */}
          <DocumentVersionHistory
            document={document}
            onVersionUploaded={onDocumentUpdated}
          />
        </div>
      </div>
    </div>,
    window.document.body
  );
}
