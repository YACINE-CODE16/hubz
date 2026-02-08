import { useEffect, useCallback, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';

export interface KeyboardShortcut {
  key: string;
  ctrl?: boolean;
  meta?: boolean;
  shift?: boolean;
  description: string;
  category: string;
  action: () => void;
}

interface UseKeyboardShortcutsOptions {
  onOpenSearch?: () => void;
  onOpenHelp?: () => void;
  enabled?: boolean;
}

export function useKeyboardShortcuts(options: UseKeyboardShortcutsOptions = {}) {
  const { onOpenSearch, onOpenHelp, enabled = true } = options;
  const navigate = useNavigate();
  const location = useLocation();

  // Track 'g' key press for navigation sequences
  const gPressedRef = useRef(false);
  const gTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Get the current organization ID from the URL if we're in an organization context
  const getOrgId = useCallback(() => {
    const match = location.pathname.match(/\/organization\/([^/]+)/);
    return match ? match[1] : null;
  }, [location.pathname]);

  const handleKeyDown = useCallback(
    (event: KeyboardEvent) => {
      if (!enabled) return;

      // Don't trigger shortcuts when typing in inputs
      const target = event.target as HTMLElement;
      const isInputField =
        target.tagName === 'INPUT' ||
        target.tagName === 'TEXTAREA' ||
        target.isContentEditable;

      // Ctrl/Cmd+K should work even in input fields (for search)
      if ((event.metaKey || event.ctrlKey) && event.key === 'k') {
        event.preventDefault();
        onOpenSearch?.();
        return;
      }

      // Skip other shortcuts if in input field
      if (isInputField) return;

      // ? key for help (without modifiers)
      if (event.key === '?' && !event.ctrlKey && !event.metaKey && !event.altKey) {
        event.preventDefault();
        onOpenHelp?.();
        return;
      }

      // Handle 'g' key sequences for navigation
      if (event.key === 'g' && !event.ctrlKey && !event.metaKey && !event.altKey && !event.shiftKey) {
        if (!gPressedRef.current) {
          gPressedRef.current = true;
          // Reset after 1.5 seconds
          if (gTimeoutRef.current) {
            clearTimeout(gTimeoutRef.current);
          }
          gTimeoutRef.current = setTimeout(() => {
            gPressedRef.current = false;
          }, 1500);
        }
        return;
      }

      // Navigation shortcuts after 'g' press
      if (gPressedRef.current && !event.ctrlKey && !event.metaKey && !event.altKey && !event.shiftKey) {
        gPressedRef.current = false;
        if (gTimeoutRef.current) {
          clearTimeout(gTimeoutRef.current);
        }

        const orgId = getOrgId();

        switch (event.key.toLowerCase()) {
          case 'h':
            // G+H: Go to Hub
            event.preventDefault();
            navigate('/hub');
            break;
          case 't':
            // G+T: Go to Tasks
            event.preventDefault();
            if (orgId) {
              navigate(`/organization/${orgId}/tasks`);
            } else {
              navigate('/hub');
            }
            break;
          case 'c':
            // G+C: Go to Calendar
            event.preventDefault();
            if (orgId) {
              navigate(`/organization/${orgId}/calendar`);
            } else {
              navigate('/personal/calendar');
            }
            break;
          case 'd':
            // G+D: Go to Dashboard
            event.preventDefault();
            if (orgId) {
              navigate(`/organization/${orgId}/dashboard`);
            } else {
              navigate('/personal/dashboard');
            }
            break;
          case 'g':
            // G+G: Go to Goals
            event.preventDefault();
            if (orgId) {
              navigate(`/organization/${orgId}/goals`);
            } else {
              navigate('/personal/goals');
            }
            break;
          case 'n':
            // G+N: Go to Notes (org only)
            event.preventDefault();
            if (orgId) {
              navigate(`/organization/${orgId}/notes`);
            }
            break;
          case 'm':
            // G+M: Go to Members (org only)
            event.preventDefault();
            if (orgId) {
              navigate(`/organization/${orgId}/members`);
            }
            break;
          case 'a':
            // G+A: Go to Analytics (org only)
            event.preventDefault();
            if (orgId) {
              navigate(`/organization/${orgId}/analytics`);
            }
            break;
          case 'p':
            // G+P: Go to Personal space
            event.preventDefault();
            navigate('/personal/dashboard');
            break;
          case 's':
            // G+S: Go to Settings
            event.preventDefault();
            navigate('/personal/settings');
            break;
          default:
            break;
        }
      }
    },
    [enabled, navigate, onOpenSearch, onOpenHelp, getOrgId]
  );

  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      if (gTimeoutRef.current) {
        clearTimeout(gTimeoutRef.current);
      }
    };
  }, [handleKeyDown]);

  // Return the list of available shortcuts for display in help modal
  const shortcuts: KeyboardShortcut[] = [
    // General shortcuts
    {
      key: 'Ctrl/Cmd + K',
      description: 'Ouvrir la recherche',
      category: 'General',
      action: () => onOpenSearch?.(),
    },
    {
      key: '?',
      description: 'Afficher les raccourcis clavier',
      category: 'General',
      action: () => onOpenHelp?.(),
    },
    {
      key: 'Escape',
      description: 'Fermer le modal/popup',
      category: 'General',
      action: () => {},
    },
    // Navigation shortcuts
    {
      key: 'G puis H',
      description: 'Aller au Hub',
      category: 'Navigation',
      action: () => navigate('/hub'),
    },
    {
      key: 'G puis T',
      description: 'Aller aux Taches',
      category: 'Navigation',
      action: () => {
        const orgId = getOrgId();
        if (orgId) navigate(`/organization/${orgId}/tasks`);
      },
    },
    {
      key: 'G puis C',
      description: 'Aller au Calendrier',
      category: 'Navigation',
      action: () => {
        const orgId = getOrgId();
        if (orgId) {
          navigate(`/organization/${orgId}/calendar`);
        } else {
          navigate('/personal/calendar');
        }
      },
    },
    {
      key: 'G puis D',
      description: 'Aller au Dashboard',
      category: 'Navigation',
      action: () => {
        const orgId = getOrgId();
        if (orgId) {
          navigate(`/organization/${orgId}/dashboard`);
        } else {
          navigate('/personal/dashboard');
        }
      },
    },
    {
      key: 'G puis G',
      description: 'Aller aux Objectifs',
      category: 'Navigation',
      action: () => {
        const orgId = getOrgId();
        if (orgId) {
          navigate(`/organization/${orgId}/goals`);
        } else {
          navigate('/personal/goals');
        }
      },
    },
    {
      key: 'G puis N',
      description: 'Aller aux Notes (organisation)',
      category: 'Navigation',
      action: () => {
        const orgId = getOrgId();
        if (orgId) navigate(`/organization/${orgId}/notes`);
      },
    },
    {
      key: 'G puis M',
      description: 'Aller aux Membres (organisation)',
      category: 'Navigation',
      action: () => {
        const orgId = getOrgId();
        if (orgId) navigate(`/organization/${orgId}/members`);
      },
    },
    {
      key: 'G puis A',
      description: 'Aller aux Analytics (organisation)',
      category: 'Navigation',
      action: () => {
        const orgId = getOrgId();
        if (orgId) navigate(`/organization/${orgId}/analytics`);
      },
    },
    {
      key: 'G puis P',
      description: 'Aller a l\'espace personnel',
      category: 'Navigation',
      action: () => navigate('/personal/dashboard'),
    },
    {
      key: 'G puis S',
      description: 'Aller aux parametres',
      category: 'Navigation',
      action: () => navigate('/personal/settings'),
    },
  ];

  return { shortcuts };
}
