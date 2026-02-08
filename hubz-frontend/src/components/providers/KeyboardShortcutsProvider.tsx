import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import { useKeyboardShortcuts, type KeyboardShortcut } from '../../hooks/useKeyboardShortcuts';
import KeyboardShortcutsModal from '../ui/KeyboardShortcutsModal';

interface KeyboardShortcutsContextValue {
  openSearch: () => void;
  openHelp: () => void;
  shortcuts: KeyboardShortcut[];
}

const KeyboardShortcutsContext = createContext<KeyboardShortcutsContextValue | null>(null);

interface KeyboardShortcutsProviderProps {
  children: ReactNode;
  onOpenSearch?: () => void;
}

export function KeyboardShortcutsProvider({
  children,
  onOpenSearch,
}: KeyboardShortcutsProviderProps) {
  const [isHelpOpen, setIsHelpOpen] = useState(false);

  const handleOpenSearch = useCallback(() => {
    if (onOpenSearch) {
      onOpenSearch();
    } else {
      // Fallback: try to focus the search input
      const searchInput = document.querySelector('input[placeholder*="Rechercher"]') as HTMLInputElement;
      if (searchInput) {
        searchInput.focus();
      }
    }
  }, [onOpenSearch]);

  const handleOpenHelp = useCallback(() => {
    setIsHelpOpen(true);
  }, []);

  const handleCloseHelp = useCallback(() => {
    setIsHelpOpen(false);
  }, []);

  const { shortcuts } = useKeyboardShortcuts({
    onOpenSearch: handleOpenSearch,
    onOpenHelp: handleOpenHelp,
    enabled: true,
  });

  const contextValue: KeyboardShortcutsContextValue = {
    openSearch: handleOpenSearch,
    openHelp: handleOpenHelp,
    shortcuts,
  };

  return (
    <KeyboardShortcutsContext.Provider value={contextValue}>
      {children}
      <KeyboardShortcutsModal
        isOpen={isHelpOpen}
        onClose={handleCloseHelp}
        shortcuts={shortcuts}
      />
    </KeyboardShortcutsContext.Provider>
  );
}

export function useKeyboardShortcutsContext() {
  const context = useContext(KeyboardShortcutsContext);
  if (!context) {
    throw new Error('useKeyboardShortcutsContext must be used within a KeyboardShortcutsProvider');
  }
  return context;
}
