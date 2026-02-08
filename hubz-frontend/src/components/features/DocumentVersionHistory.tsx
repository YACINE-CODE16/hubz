import { useState, useEffect, useRef } from 'react';
import {
  History,
  Download,
  Upload,
  Clock,
  User,
  FileText,
  Loader2,
  AlertCircle,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { organizationDocumentService } from '../../services/organizationDocument.service';
import type { OrganizationDocument, DocumentVersion } from '../../types/organizationDocument';
import { toast } from 'react-hot-toast';

interface DocumentVersionHistoryProps {
  document: OrganizationDocument;
  onVersionUploaded?: () => void;
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatDate(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default function DocumentVersionHistory({
  document,
  onVersionUploaded,
}: DocumentVersionHistoryProps) {
  const [versions, setVersions] = useState<DocumentVersion[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [expanded, setExpanded] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    fetchVersions();
  }, [document.id]);

  const fetchVersions = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await organizationDocumentService.getDocumentVersions(document.id);
      setVersions(data);
    } catch (err) {
      setError('Erreur lors du chargement des versions');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleUploadClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploading(true);
    try {
      await organizationDocumentService.uploadNewVersion(document.id, file);
      toast.success('Nouvelle version uploadee');
      await fetchVersions();
      onVersionUploaded?.();
    } catch (err) {
      toast.error("Erreur lors de l'upload de la nouvelle version");
      console.error(err);
    } finally {
      setUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleDownloadVersion = async (version: DocumentVersion) => {
    try {
      const blob = await organizationDocumentService.downloadDocumentVersion(
        document.id,
        version.versionNumber
      );
      const url = window.URL.createObjectURL(blob);
      const link = window.document.createElement('a');
      link.href = url;
      link.download = version.originalFileName;
      window.document.body.appendChild(link);
      link.click();
      window.document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      toast.error('Erreur lors du telechargement');
      console.error(err);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-4">
        <Loader2 className="h-5 w-5 animate-spin text-blue-500" />
        <span className="ml-2 text-sm text-gray-500 dark:text-gray-400">
          Chargement des versions...
        </span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center py-4 text-red-500">
        <AlertCircle className="h-5 w-5 mr-2" />
        <span className="text-sm">{error}</span>
      </div>
    );
  }

  return (
    <div className="border-t border-gray-200/50 dark:border-white/10 pt-4 mt-4">
      {/* Header */}
      <div className="flex items-center justify-between mb-3">
        <button
          onClick={() => setExpanded(!expanded)}
          className="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:text-gray-900 dark:hover:text-gray-100 transition-colors"
        >
          <History className="h-4 w-4" />
          <span>Historique des versions ({versions.length})</span>
          {expanded ? (
            <ChevronUp className="h-4 w-4" />
          ) : (
            <ChevronDown className="h-4 w-4" />
          )}
        </button>

        <button
          onClick={handleUploadClick}
          disabled={uploading}
          className="flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {uploading ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <Upload className="h-4 w-4" />
          )}
          <span>Nouvelle version</span>
        </button>

        <input
          ref={fileInputRef}
          type="file"
          onChange={handleFileChange}
          className="hidden"
        />
      </div>

      {/* Version list */}
      {expanded && (
        <div className="space-y-2 mt-3">
          {versions.map((version, index) => (
            <div
              key={version.id}
              className={`flex items-center justify-between p-3 rounded-lg border transition-colors ${
                index === 0
                  ? 'bg-blue-50/50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800'
                  : 'bg-gray-50/50 dark:bg-gray-800/50 border-gray-200/50 dark:border-white/10'
              }`}
            >
              <div className="flex items-center gap-3 min-w-0">
                <div
                  className={`flex items-center justify-center w-8 h-8 rounded-full text-sm font-semibold ${
                    index === 0
                      ? 'bg-blue-100 dark:bg-blue-900 text-blue-600 dark:text-blue-400'
                      : 'bg-gray-200 dark:bg-gray-700 text-gray-600 dark:text-gray-400'
                  }`}
                >
                  v{version.versionNumber}
                </div>

                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <FileText className="h-4 w-4 text-gray-400 flex-shrink-0" />
                    <span className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">
                      {version.originalFileName}
                    </span>
                    {index === 0 && (
                      <span className="px-1.5 py-0.5 text-xs font-medium bg-blue-100 dark:bg-blue-900 text-blue-600 dark:text-blue-400 rounded">
                        Actuelle
                      </span>
                    )}
                  </div>

                  <div className="flex items-center gap-3 mt-1 text-xs text-gray-500 dark:text-gray-400">
                    <span className="flex items-center gap-1">
                      <User className="h-3 w-3" />
                      {version.uploadedByName}
                    </span>
                    <span className="flex items-center gap-1">
                      <Clock className="h-3 w-3" />
                      {formatDate(version.uploadedAt)}
                    </span>
                    <span>{formatFileSize(version.fileSize)}</span>
                  </div>
                </div>
              </div>

              <button
                onClick={() => handleDownloadVersion(version)}
                className="flex items-center gap-1.5 px-2.5 py-1.5 text-sm text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-100 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
                title="Telecharger cette version"
              >
                <Download className="h-4 w-4" />
              </button>
            </div>
          ))}

          {versions.length === 0 && (
            <div className="text-center py-4 text-sm text-gray-500 dark:text-gray-400">
              Aucune version anterieure
            </div>
          )}
        </div>
      )}
    </div>
  );
}
