import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { X, Keyboard, Search, Navigation, Command } from 'lucide-react';
import { cn } from '../../lib/utils';
import type { KeyboardShortcut } from '../../hooks/useKeyboardShortcuts';

interface KeyboardShortcutsModalProps {
  isOpen: boolean;
  onClose: () => void;
  shortcuts: KeyboardShortcut[];
}

export default function KeyboardShortcutsModal({
  isOpen,
  onClose,
  shortcuts,
}: KeyboardShortcutsModalProps) {
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

  // Group shortcuts by category
  const groupedShortcuts = shortcuts.reduce((acc, shortcut) => {
    if (!acc[shortcut.category]) {
      acc[shortcut.category] = [];
    }
    acc[shortcut.category].push(shortcut);
    return acc;
  }, {} as Record<string, KeyboardShortcut[]>);

  const getCategoryIcon = (category: string) => {
    switch (category) {
      case 'General':
        return <Command className="h-4 w-4" />;
      case 'Navigation':
        return <Navigation className="h-4 w-4" />;
      default:
        return <Keyboard className="h-4 w-4" />;
    }
  };

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={onClose}
      />
      <div
        className={cn(
          'relative z-10 w-full max-w-lg rounded-xl border border-gray-200/50 dark:border-white/10',
          'bg-light-card/95 dark:bg-dark-card/95 backdrop-blur-xl shadow-xl'
        )}
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b border-gray-200/50 dark:border-white/10 px-6 py-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-accent/10 text-accent">
              <Keyboard className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                Raccourcis clavier
              </h2>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Naviguez plus rapidement
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-2 text-gray-400 hover:bg-light-hover dark:hover:bg-dark-hover hover:text-gray-600 dark:hover:text-gray-200 transition-colors"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Content */}
        <div className="max-h-[60vh] overflow-y-auto p-6">
          <div className="space-y-6">
            {Object.entries(groupedShortcuts).map(([category, categoryShortcuts]) => (
              <div key={category}>
                <div className="flex items-center gap-2 mb-3">
                  <span className="text-gray-400 dark:text-gray-500">
                    {getCategoryIcon(category)}
                  </span>
                  <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider">
                    {category}
                  </h3>
                </div>
                <div className="space-y-2">
                  {categoryShortcuts.map((shortcut, index) => (
                    <div
                      key={index}
                      className="flex items-center justify-between rounded-lg px-3 py-2 hover:bg-light-hover dark:hover:bg-dark-hover transition-colors"
                    >
                      <span className="text-sm text-gray-600 dark:text-gray-300">
                        {shortcut.description}
                      </span>
                      <ShortcutKeys keys={shortcut.key} />
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Footer */}
        <div className="border-t border-gray-200/50 dark:border-white/10 px-6 py-4">
          <div className="flex items-center justify-between text-sm text-gray-500 dark:text-gray-400">
            <div className="flex items-center gap-2">
              <Search className="h-4 w-4" />
              <span>Appuyez sur <ShortcutKeys keys="?" /> pour afficher ce menu</span>
            </div>
            <button
              onClick={onClose}
              className="rounded-lg bg-light-hover dark:bg-dark-hover px-3 py-1.5 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors"
            >
              Fermer
            </button>
          </div>
        </div>
      </div>
    </div>,
    document.body
  );
}

interface ShortcutKeysProps {
  keys: string;
}

function ShortcutKeys({ keys }: ShortcutKeysProps) {
  // Split keys like "Ctrl/Cmd + K" or "G puis H" into parts
  const parts = keys.split(/(\s*\+\s*|\s+puis\s+)/);

  return (
    <div className="flex items-center gap-1">
      {parts.map((part, index) => {
        const trimmedPart = part.trim();

        // Skip empty parts and separators
        if (!trimmedPart || trimmedPart === '+' || trimmedPart === 'puis') {
          if (trimmedPart === '+' || trimmedPart === 'puis') {
            return (
              <span key={index} className="text-xs text-gray-400 dark:text-gray-500 px-1">
                {trimmedPart === 'puis' ? 'puis' : '+'}
              </span>
            );
          }
          return null;
        }

        // Handle special key names
        const displayKey = trimmedPart
          .replace('Ctrl/Cmd', isMac() ? 'Cmd' : 'Ctrl')
          .replace('Cmd', isMac() ? 'Cmd' : 'Ctrl');

        return (
          <kbd
            key={index}
            className="inline-flex min-w-[24px] items-center justify-center rounded-md border border-gray-200 dark:border-gray-600 bg-gray-50 dark:bg-gray-800 px-2 py-1 text-xs font-medium text-gray-700 dark:text-gray-300 shadow-sm"
          >
            {displayKey}
          </kbd>
        );
      })}
    </div>
  );
}

function isMac(): boolean {
  return typeof navigator !== 'undefined' && /Mac|iPod|iPhone|iPad/.test(navigator.platform);
}
