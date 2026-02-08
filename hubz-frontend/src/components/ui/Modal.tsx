import { type ReactNode, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';
import { cn } from '../../lib/utils';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  children: ReactNode;
  className?: string;
  /** When true, the modal will not go full-screen on mobile */
  compact?: boolean;
}

export default function Modal({ isOpen, onClose, title, children, className, compact = false }: ModalProps) {
  useEffect(() => {
    if (!isOpen) return;

    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };

    document.addEventListener('keydown', handleEsc);
    document.body.style.overflow = 'hidden';

    return () => {
      document.removeEventListener('keydown', handleEsc);
      document.body.style.overflow = '';
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center">
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={onClose}
      />
      <div
        className={cn(
          'relative z-10 w-full bg-light-card dark:bg-dark-card shadow-xl',
          // Mobile: full-screen or bottom-sheet style
          compact
            ? 'max-h-[90vh] rounded-t-2xl sm:rounded-xl sm:max-w-md border border-gray-200/50 dark:border-white/10 bg-light-card/80 dark:bg-dark-card/80 backdrop-blur-xl p-4 sm:p-6'
            : 'h-full sm:h-auto sm:max-h-[90vh] rounded-none sm:rounded-xl sm:max-w-md sm:border border-gray-200/50 dark:border-white/10 sm:bg-light-card/80 sm:dark:bg-dark-card/80 sm:backdrop-blur-xl p-4 sm:p-6',
          'overflow-y-auto',
          className,
        )}
      >
        {/* Close button always visible, title optional */}
        {title ? (
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 pr-2">
              {title}
            </h2>
            <button
              onClick={onClose}
              className="shrink-0 rounded-lg p-1.5 text-gray-400 hover:bg-light-hover dark:hover:bg-dark-hover hover:text-gray-600 dark:hover:text-gray-200 transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        ) : (
          <button
            onClick={onClose}
            className="absolute right-3 top-3 z-10 rounded-lg p-1.5 text-gray-400 hover:bg-light-hover dark:hover:bg-dark-hover hover:text-gray-600 dark:hover:text-gray-200 transition-colors sm:hidden"
          >
            <X className="h-5 w-5" />
          </button>
        )}
        {children}
      </div>
    </div>,
    document.body,
  );
}
