import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { UserPreferences, Language, Theme, DateFormat } from '../types/preferences';

interface PreferencesState {
  preferences: UserPreferences | null;
  isLoading: boolean;
  error: string | null;

  // Actions
  setPreferences: (preferences: UserPreferences) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  clearPreferences: () => void;

  // Getters for convenience
  getLanguage: () => Language;
  getTheme: () => Theme;
  getDateFormat: () => DateFormat;
  getTimezone: () => string;

  // Apply theme to document
  applyTheme: () => void;
}

// Default preferences
const DEFAULT_LANGUAGE: Language = 'FR';
const DEFAULT_THEME: Theme = 'SYSTEM';
const DEFAULT_DATE_FORMAT: DateFormat = 'DD_MM_YYYY';
const DEFAULT_TIMEZONE = 'Europe/Paris';

export const usePreferencesStore = create<PreferencesState>()(
  persist(
    (set, get) => ({
      preferences: null,
      isLoading: false,
      error: null,

      setPreferences: (preferences) => {
        set({ preferences, error: null });
        // Apply theme when preferences change
        get().applyTheme();
      },

      setLoading: (isLoading) => set({ isLoading }),

      setError: (error) => set({ error }),

      clearPreferences: () => set({ preferences: null, error: null }),

      getLanguage: () => get().preferences?.language ?? DEFAULT_LANGUAGE,

      getTheme: () => get().preferences?.theme ?? DEFAULT_THEME,

      getDateFormat: () => get().preferences?.dateFormat ?? DEFAULT_DATE_FORMAT,

      getTimezone: () => get().preferences?.timezone ?? DEFAULT_TIMEZONE,

      applyTheme: () => {
        const theme = get().preferences?.theme ?? DEFAULT_THEME;
        const root = document.documentElement;

        if (theme === 'DARK') {
          root.classList.add('dark');
        } else if (theme === 'LIGHT') {
          root.classList.remove('dark');
        } else {
          // SYSTEM - follow OS preference
          const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
          if (prefersDark) {
            root.classList.add('dark');
          } else {
            root.classList.remove('dark');
          }
        }
      },
    }),
    {
      name: 'hubz-preferences',
      partialize: (state) => ({ preferences: state.preferences }),
    },
  ),
);

// Helper function to format dates according to user preferences
export function formatDate(date: Date | string, dateFormat?: DateFormat): string {
  const d = typeof date === 'string' ? new Date(date) : date;
  const format = dateFormat ?? usePreferencesStore.getState().getDateFormat();

  const day = String(d.getDate()).padStart(2, '0');
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const year = d.getFullYear();

  switch (format) {
    case 'DD_MM_YYYY':
      return `${day}/${month}/${year}`;
    case 'MM_DD_YYYY':
      return `${month}/${day}/${year}`;
    case 'YYYY_MM_DD':
      return `${year}-${month}-${day}`;
    default:
      return `${day}/${month}/${year}`;
  }
}

// Helper function to get translated text based on language preference
export function getTranslation(frText: string, enText: string): string {
  const language = usePreferencesStore.getState().getLanguage();
  return language === 'FR' ? frText : enText;
}

// Apply theme on initial load (after hydration from localStorage)
// This runs once when the module is loaded
if (typeof window !== 'undefined') {
  // Small delay to ensure hydration is complete
  setTimeout(() => {
    usePreferencesStore.getState().applyTheme();
  }, 0);
}
