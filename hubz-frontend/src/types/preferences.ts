// Supported languages
export type Language = 'FR' | 'EN';

// Theme options
export type Theme = 'SYSTEM' | 'LIGHT' | 'DARK';

// Date format options
export type DateFormat = 'DD_MM_YYYY' | 'MM_DD_YYYY' | 'YYYY_MM_DD';

// Reminder frequency options
export type ReminderFrequency = 'ONE_DAY' | 'THREE_DAYS' | 'ONE_WEEK';

// User preferences response from API
export interface UserPreferences {
  id: string;
  userId: string;
  language: Language;
  timezone: string;
  dateFormat: DateFormat;
  theme: Theme;
  digestEnabled: boolean;
  reminderEnabled: boolean;
  reminderFrequency: ReminderFrequency;
  createdAt: string;
  updatedAt: string;
}

// Request to update preferences
export interface UpdatePreferencesRequest {
  language: Language;
  timezone: string;
  dateFormat: DateFormat;
  theme: Theme;
  digestEnabled: boolean;
  reminderEnabled: boolean;
  reminderFrequency: ReminderFrequency;
}

// Language display info
export interface LanguageOption {
  value: Language;
  label: string;
  nativeLabel: string;
}

// Theme display info
export interface ThemeOption {
  value: Theme;
  label: string;
  labelFr: string;
}

// Date format display info
export interface DateFormatOption {
  value: DateFormat;
  pattern: string;
  example: string;
}

// Reminder frequency display info
export interface ReminderFrequencyOption {
  value: ReminderFrequency;
  label: string;
  labelFr: string;
  description: string;
  descriptionFr: string;
}

// Timezone display info
export interface TimezoneOption {
  value: string;
  label: string;
  region: string;
}

// Predefined options
export const LANGUAGE_OPTIONS: LanguageOption[] = [
  { value: 'FR', label: 'French', nativeLabel: 'Francais' },
  { value: 'EN', label: 'English', nativeLabel: 'English' },
];

export const THEME_OPTIONS: ThemeOption[] = [
  { value: 'SYSTEM', label: 'System', labelFr: 'Systeme' },
  { value: 'LIGHT', label: 'Light', labelFr: 'Clair' },
  { value: 'DARK', label: 'Dark', labelFr: 'Sombre' },
];

export const DATE_FORMAT_OPTIONS: DateFormatOption[] = [
  { value: 'DD_MM_YYYY', pattern: 'DD/MM/YYYY', example: '31/12/2024' },
  { value: 'MM_DD_YYYY', pattern: 'MM/DD/YYYY', example: '12/31/2024' },
  { value: 'YYYY_MM_DD', pattern: 'YYYY-MM-DD', example: '2024-12-31' },
];

export const REMINDER_FREQUENCY_OPTIONS: ReminderFrequencyOption[] = [
  {
    value: 'ONE_DAY',
    label: '1 day before',
    labelFr: '1 jour avant',
    description: 'Get reminded 1 day before deadlines',
    descriptionFr: 'Rappel 1 jour avant les echeances',
  },
  {
    value: 'THREE_DAYS',
    label: '3 days before',
    labelFr: '3 jours avant',
    description: 'Get reminded 3 days and 1 day before deadlines',
    descriptionFr: 'Rappels 3 jours et 1 jour avant les echeances',
  },
  {
    value: 'ONE_WEEK',
    label: '1 week before',
    labelFr: '1 semaine avant',
    description: 'Get reminded 7 days, 3 days, and 1 day before deadlines',
    descriptionFr: 'Rappels 7 jours, 3 jours et 1 jour avant les echeances',
  },
];

export const TIMEZONE_OPTIONS: TimezoneOption[] = [
  // Europe
  { value: 'Europe/Paris', label: 'Paris', region: 'Europe' },
  { value: 'Europe/London', label: 'London', region: 'Europe' },
  { value: 'Europe/Berlin', label: 'Berlin', region: 'Europe' },
  { value: 'Europe/Rome', label: 'Rome', region: 'Europe' },
  { value: 'Europe/Madrid', label: 'Madrid', region: 'Europe' },
  { value: 'Europe/Brussels', label: 'Brussels', region: 'Europe' },
  { value: 'Europe/Amsterdam', label: 'Amsterdam', region: 'Europe' },
  { value: 'Europe/Zurich', label: 'Zurich', region: 'Europe' },
  // Americas
  { value: 'America/New_York', label: 'New York', region: 'Americas' },
  { value: 'America/Los_Angeles', label: 'Los Angeles', region: 'Americas' },
  { value: 'America/Chicago', label: 'Chicago', region: 'Americas' },
  { value: 'America/Denver', label: 'Denver', region: 'Americas' },
  { value: 'America/Toronto', label: 'Toronto', region: 'Americas' },
  { value: 'America/Montreal', label: 'Montreal', region: 'Americas' },
  { value: 'America/Vancouver', label: 'Vancouver', region: 'Americas' },
  { value: 'America/Mexico_City', label: 'Mexico City', region: 'Americas' },
  { value: 'America/Sao_Paulo', label: 'Sao Paulo', region: 'Americas' },
  // Asia
  { value: 'Asia/Tokyo', label: 'Tokyo', region: 'Asia' },
  { value: 'Asia/Shanghai', label: 'Shanghai', region: 'Asia' },
  { value: 'Asia/Hong_Kong', label: 'Hong Kong', region: 'Asia' },
  { value: 'Asia/Singapore', label: 'Singapore', region: 'Asia' },
  { value: 'Asia/Dubai', label: 'Dubai', region: 'Asia' },
  { value: 'Asia/Seoul', label: 'Seoul', region: 'Asia' },
  { value: 'Asia/Kolkata', label: 'Kolkata', region: 'Asia' },
  // Oceania
  { value: 'Australia/Sydney', label: 'Sydney', region: 'Oceania' },
  { value: 'Australia/Melbourne', label: 'Melbourne', region: 'Oceania' },
  { value: 'Pacific/Auckland', label: 'Auckland', region: 'Oceania' },
  // Africa
  { value: 'Africa/Casablanca', label: 'Casablanca', region: 'Africa' },
  { value: 'Africa/Johannesburg', label: 'Johannesburg', region: 'Africa' },
  // UTC
  { value: 'UTC', label: 'UTC', region: 'Universal' },
];
