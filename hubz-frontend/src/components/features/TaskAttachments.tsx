import { useState, useEffect, useRef } from 'react';
import {
  Paperclip,
  Upload,
  Trash2,
  Download,
  File,
  FileText,
  Image,
  Video,
  Music,
  Archive,
  Table,
  Presentation,
  X,
} from 'lucide-react';
import type { TaskAttachment } from '../../types/task';
import { taskAttachmentService } from '../../services/taskAttachment.service';
import Button from '../ui/Button';
import { cn } from '../../lib/utils';

interface TaskAttachmentsProps {
  taskId: string;
}

const FileIcon = ({ contentType }: { contentType: string }) => {
  const iconType = taskAttachmentService.getFileIcon(contentType);
  const iconClass = 'h-5 w-5';

  switch (iconType) {
    case 'image':
      return <Image className={cn(iconClass, 'text-green-500')} />;
    case 'file-text':
      return <FileText className={cn(iconClass, 'text-blue-500')} />;
    case 'table':
      return <Table className={cn(iconClass, 'text-emerald-500')} />;
    case 'presentation':
      return <Presentation className={cn(iconClass, 'text-orange-500')} />;
    case 'video':
      return <Video className={cn(iconClass, 'text-purple-500')} />;
    case 'music':
      return <Music className={cn(iconClass, 'text-pink-500')} />;
    case 'archive':
      return <Archive className={cn(iconClass, 'text-yellow-500')} />;
    default:
      return <File className={cn(iconClass, 'text-gray-500')} />;
  }
};

export default function TaskAttachments({ taskId }: TaskAttachmentsProps) {
  const [attachments, setAttachments] = useState<TaskAttachment[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [dragActive, setDragActive] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    loadAttachments();
  }, [taskId]);

  const loadAttachments = async () => {
    try {
      setLoading(true);
      const data = await taskAttachmentService.getAttachments(taskId);
      setAttachments(data);
      setError(null);
    } catch (err) {
      setError('Erreur lors du chargement des fichiers');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleFileUpload = async (files: FileList | null) => {
    if (!files || files.length === 0) return;

    try {
      setUploading(true);
      setError(null);

      for (const file of Array.from(files)) {
        await taskAttachmentService.uploadAttachment(taskId, file);
      }

      await loadAttachments();
    } catch (err: any) {
      setError(err.response?.data?.error || 'Erreur lors de l\'upload');
      console.error(err);
    } finally {
      setUploading(false);
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

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFileUpload(e.dataTransfer.files);
    }
  };

  const handleDownload = async (attachment: TaskAttachment) => {
    try {
      const blob = await taskAttachmentService.downloadAttachment(attachment.id);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = attachment.originalFileName;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      setError('Erreur lors du telechargement');
      console.error(err);
    }
  };

  const handleDelete = async (attachmentId: string) => {
    try {
      await taskAttachmentService.deleteAttachment(attachmentId);
      await loadAttachments();
    } catch (err) {
      setError('Erreur lors de la suppression');
      console.error(err);
    }
  };

  if (loading) {
    return (
      <div className="py-4">
        <div className="animate-pulse space-y-2">
          <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-1/4"></div>
          <div className="h-16 bg-gray-200 dark:bg-gray-700 rounded"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {/* Header */}
      <div className="flex items-center justify-between">
        <label className="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide flex items-center gap-1.5">
          <Paperclip className="h-3.5 w-3.5" />
          Pieces jointes
          {attachments.length > 0 && (
            <span className="text-gray-400">({attachments.length})</span>
          )}
        </label>
      </div>

      {/* Error message */}
      {error && (
        <div className="rounded-lg bg-error/10 border border-error/20 px-3 py-2 text-xs text-error flex items-center justify-between">
          <span>{error}</span>
          <button onClick={() => setError(null)}>
            <X className="h-3.5 w-3.5" />
          </button>
        </div>
      )}

      {/* Drop zone */}
      <div
        onDragEnter={handleDrag}
        onDragLeave={handleDrag}
        onDragOver={handleDrag}
        onDrop={handleDrop}
        className={cn(
          'border-2 border-dashed rounded-lg p-4 text-center transition-colors cursor-pointer',
          dragActive
            ? 'border-accent bg-accent/5'
            : 'border-gray-200 dark:border-gray-700 hover:border-accent/50'
        )}
        onClick={() => fileInputRef.current?.click()}
      >
        <input
          ref={fileInputRef}
          type="file"
          multiple
          onChange={(e) => handleFileUpload(e.target.files)}
          className="hidden"
        />
        <div className="flex flex-col items-center gap-2">
          {uploading ? (
            <>
              <div className="h-8 w-8 border-2 border-accent border-t-transparent rounded-full animate-spin" />
              <span className="text-xs text-gray-500">Upload en cours...</span>
            </>
          ) : (
            <>
              <Upload className="h-6 w-6 text-gray-400" />
              <span className="text-xs text-gray-500 dark:text-gray-400">
                Glissez-deposez ou cliquez pour ajouter des fichiers
              </span>
              <span className="text-xs text-gray-400">
                Max 25 Mo par fichier
              </span>
            </>
          )}
        </div>
      </div>

      {/* Attachments list */}
      {attachments.length > 0 && (
        <div className="space-y-1">
          {attachments.map((attachment) => (
            <div
              key={attachment.id}
              className={cn(
                'group flex items-center gap-3 rounded-lg px-3 py-2 transition-colors',
                'bg-light-hover dark:bg-dark-hover hover:bg-gray-100 dark:hover:bg-white/10'
              )}
            >
              {/* File icon */}
              <FileIcon contentType={attachment.contentType} />

              {/* File info */}
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">
                  {attachment.originalFileName}
                </p>
                <p className="text-xs text-gray-500">
                  {taskAttachmentService.formatFileSize(attachment.fileSize)}
                </p>
              </div>

              {/* Actions */}
              <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                <button
                  onClick={() => handleDownload(attachment)}
                  className="p-1.5 text-gray-400 hover:text-accent transition-colors rounded-lg hover:bg-white/50 dark:hover:bg-white/10"
                  title="Telecharger"
                >
                  <Download className="h-4 w-4" />
                </button>
                <button
                  onClick={() => handleDelete(attachment.id)}
                  className="p-1.5 text-gray-400 hover:text-error transition-colors rounded-lg hover:bg-white/50 dark:hover:bg-white/10"
                  title="Supprimer"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
