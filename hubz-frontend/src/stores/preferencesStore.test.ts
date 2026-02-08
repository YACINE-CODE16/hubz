import { describe, it, expect, beforeEach, vi } from 'vitest';
import { usePreferencesStore, formatDate, getTranslation } from './preferencesStore';
import type { UserPreferences } from '../types/preferences';

describe('preferencesStore', () => {
  const mockPreferences: UserPreferences = {
    id: '123e4567-e89b-12d3-a456-426614174000',
    userId: '456e4567-e89b-12d3-a456-426614174000',
    language: 'FR',
    timezone: 'Europe/Paris',
    dateFormat: 'DD_MM_YYYY',
    theme: 'DARK',
    digestEnabled: true,
    reminderEnabled: true,
    reminderFrequency: 'THREE_DAYS',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
  };

  beforeEach(() => {
    // Reset store to initial state before each test
    usePreferencesStore.setState({
      preferences: null,
      isLoading: false,
      error: null,
    });
  });

  it('should have correct initial state', () => {
    const state = usePreferencesStore.getState();
    expect(state.preferences).toBeNull();
    expect(state.isLoading).toBe(false);
    expect(state.error).toBeNull();
  });

  it('should set preferences', () => {
    usePreferencesStore.getState().setPreferences(mockPreferences);

    const state = usePreferencesStore.getState();
    expect(state.preferences).toEqual(mockPreferences);
    expect(state.error).toBeNull();
  });

  it('should set loading state', () => {
    usePreferencesStore.getState().setLoading(true);
    expect(usePreferencesStore.getState().isLoading).toBe(true);

    usePreferencesStore.getState().setLoading(false);
    expect(usePreferencesStore.getState().isLoading).toBe(false);
  });

  it('should set error state', () => {
    usePreferencesStore.getState().setError('Failed to load');
    expect(usePreferencesStore.getState().error).toBe('Failed to load');
  });

  it('should clear preferences', () => {
    usePreferencesStore.getState().setPreferences(mockPreferences);
    usePreferencesStore.getState().clearPreferences();

    const state = usePreferencesStore.getState();
    expect(state.preferences).toBeNull();
    expect(state.error).toBeNull();
  });

  it('should return default language when no preferences set', () => {
    const language = usePreferencesStore.getState().getLanguage();
    expect(language).toBe('FR');
  });

  it('should return user language when preferences are set', () => {
    usePreferencesStore.getState().setPreferences({
      ...mockPreferences,
      language: 'EN',
    });
    const language = usePreferencesStore.getState().getLanguage();
    expect(language).toBe('EN');
  });

  it('should return default theme when no preferences set', () => {
    const theme = usePreferencesStore.getState().getTheme();
    expect(theme).toBe('SYSTEM');
  });

  it('should return user theme when preferences are set', () => {
    usePreferencesStore.getState().setPreferences(mockPreferences);
    const theme = usePreferencesStore.getState().getTheme();
    expect(theme).toBe('DARK');
  });

  it('should return default date format when no preferences set', () => {
    const dateFormat = usePreferencesStore.getState().getDateFormat();
    expect(dateFormat).toBe('DD_MM_YYYY');
  });

  it('should return user date format when preferences are set', () => {
    usePreferencesStore.getState().setPreferences({
      ...mockPreferences,
      dateFormat: 'YYYY_MM_DD',
    });
    const dateFormat = usePreferencesStore.getState().getDateFormat();
    expect(dateFormat).toBe('YYYY_MM_DD');
  });

  it('should return default timezone when no preferences set', () => {
    const timezone = usePreferencesStore.getState().getTimezone();
    expect(timezone).toBe('Europe/Paris');
  });

  it('should return user timezone when preferences are set', () => {
    usePreferencesStore.getState().setPreferences({
      ...mockPreferences,
      timezone: 'America/New_York',
    });
    const timezone = usePreferencesStore.getState().getTimezone();
    expect(timezone).toBe('America/New_York');
  });

  it('should clear error when setting preferences', () => {
    usePreferencesStore.getState().setError('Some error');
    usePreferencesStore.getState().setPreferences(mockPreferences);
    expect(usePreferencesStore.getState().error).toBeNull();
  });

  it('should apply dark theme to document', () => {
    usePreferencesStore.getState().setPreferences({
      ...mockPreferences,
      theme: 'DARK',
    });
    // applyTheme is called inside setPreferences
    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });

  it('should apply light theme to document', () => {
    // First apply dark to make sure it removes it
    document.documentElement.classList.add('dark');
    usePreferencesStore.getState().setPreferences({
      ...mockPreferences,
      theme: 'LIGHT',
    });
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });
});

describe('formatDate', () => {
  it('should format date as DD/MM/YYYY', () => {
    const result = formatDate(new Date(2024, 11, 31), 'DD_MM_YYYY');
    expect(result).toBe('31/12/2024');
  });

  it('should format date as MM/DD/YYYY', () => {
    const result = formatDate(new Date(2024, 11, 31), 'MM_DD_YYYY');
    expect(result).toBe('12/31/2024');
  });

  it('should format date as YYYY-MM-DD', () => {
    const result = formatDate(new Date(2024, 11, 31), 'YYYY_MM_DD');
    expect(result).toBe('2024-12-31');
  });

  it('should format date string input', () => {
    const result = formatDate('2024-06-15T10:00:00Z', 'DD_MM_YYYY');
    expect(result).toBe('15/06/2024');
  });

  it('should pad single-digit day and month', () => {
    const result = formatDate(new Date(2024, 0, 5), 'DD_MM_YYYY');
    expect(result).toBe('05/01/2024');
  });

  it('should use store default format when no format specified', () => {
    // With no preferences set, default is DD_MM_YYYY
    usePreferencesStore.setState({ preferences: null });
    const result = formatDate(new Date(2024, 2, 15));
    expect(result).toBe('15/03/2024');
  });
});

describe('getTranslation', () => {
  beforeEach(() => {
    usePreferencesStore.setState({ preferences: null });
  });

  it('should return French text when language is FR', () => {
    // Default language is FR
    const result = getTranslation('Bonjour', 'Hello');
    expect(result).toBe('Bonjour');
  });

  it('should return English text when language is EN', () => {
    usePreferencesStore.getState().setPreferences({
      id: '1',
      userId: '1',
      language: 'EN',
      timezone: 'UTC',
      dateFormat: 'DD_MM_YYYY',
      theme: 'SYSTEM',
      digestEnabled: true,
      reminderEnabled: true,
      reminderFrequency: 'ONE_DAY',
      createdAt: '',
      updatedAt: '',
    });
    const result = getTranslation('Bonjour', 'Hello');
    expect(result).toBe('Hello');
  });
});
