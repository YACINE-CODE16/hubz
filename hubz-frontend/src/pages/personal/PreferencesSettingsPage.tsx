import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Settings, Globe, Clock, Calendar, Moon, Sun, Monitor, Check, Loader2, Mail, CheckCircle2, Target, Repeat, CalendarDays, Bell, AlertTriangle } from 'lucide-react';
import toast from 'react-hot-toast';
import Card from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import { preferencesService } from '../../services/preferences.service';
import { usePreferencesStore } from '../../stores/preferencesStore';
import {
  LANGUAGE_OPTIONS,
  THEME_OPTIONS,
  DATE_FORMAT_OPTIONS,
  TIMEZONE_OPTIONS,
  REMINDER_FREQUENCY_OPTIONS,
  type UpdatePreferencesRequest,
} from '../../types/preferences';

const preferencesSchema = z.object({
  language: z.enum(['FR', 'EN']),
  timezone: z.string().min(1, 'Le fuseau horaire est requis'),
  dateFormat: z.enum(['DD_MM_YYYY', 'MM_DD_YYYY', 'YYYY_MM_DD']),
  theme: z.enum(['SYSTEM', 'LIGHT', 'DARK']),
  digestEnabled: z.boolean(),
  reminderEnabled: z.boolean(),
  reminderFrequency: z.enum(['ONE_DAY', 'THREE_DAYS', 'ONE_WEEK']),
});

type PreferencesForm = z.infer<typeof preferencesSchema>;

export default function PreferencesSettingsPage() {
  const { setPreferences } = usePreferencesStore();
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isDirty },
  } = useForm<PreferencesForm>({
    resolver: zodResolver(preferencesSchema),
    defaultValues: {
      language: 'FR',
      timezone: 'Europe/Paris',
      dateFormat: 'DD_MM_YYYY',
      theme: 'SYSTEM',
      digestEnabled: true,
      reminderEnabled: true,
      reminderFrequency: 'THREE_DAYS',
    },
  });

  const watchedLanguage = watch('language');
  const watchedTheme = watch('theme');

  // Load preferences on mount
  useEffect(() => {
    const loadPreferences = async () => {
      try {
        const prefs = await preferencesService.getPreferences();
        setPreferences(prefs);
        setValue('language', prefs.language);
        setValue('timezone', prefs.timezone);
        setValue('dateFormat', prefs.dateFormat);
        setValue('theme', prefs.theme);
        setValue('digestEnabled', prefs.digestEnabled ?? true);
        setValue('reminderEnabled', prefs.reminderEnabled ?? true);
        setValue('reminderFrequency', prefs.reminderFrequency ?? 'THREE_DAYS');
      } catch (error) {
        toast.error('Erreur lors du chargement des preferences');
        console.error('Failed to load preferences:', error);
      } finally {
        setInitialLoading(false);
      }
    };
    loadPreferences();
  }, [setPreferences, setValue]);

  const onSubmit = async (data: PreferencesForm) => {
    setLoading(true);
    try {
      const request: UpdatePreferencesRequest = {
        language: data.language,
        timezone: data.timezone,
        dateFormat: data.dateFormat,
        theme: data.theme,
        digestEnabled: data.digestEnabled,
        reminderEnabled: data.reminderEnabled,
        reminderFrequency: data.reminderFrequency,
      };
      const updatedPrefs = await preferencesService.updatePreferences(request);
      setPreferences(updatedPrefs);
      toast.success(
        watchedLanguage === 'FR'
          ? 'Preferences mises a jour avec succes'
          : 'Preferences updated successfully',
      );
    } catch (error) {
      toast.error(
        watchedLanguage === 'FR'
          ? 'Erreur lors de la mise a jour des preferences'
          : 'Failed to update preferences',
      );
      console.error('Failed to update preferences:', error);
    } finally {
      setLoading(false);
    }
  };

  // Get translated labels
  const t = (fr: string, en: string) => (watchedLanguage === 'FR' ? fr : en);

  // Group timezones by region
  const timezonesByRegion = TIMEZONE_OPTIONS.reduce(
    (acc, tz) => {
      if (!acc[tz.region]) {
        acc[tz.region] = [];
      }
      acc[tz.region].push(tz);
      return acc;
    },
    {} as Record<string, typeof TIMEZONE_OPTIONS>,
  );

  if (initialLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-accent" />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6 p-6">
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-accent/10">
          <Settings className="h-5 w-5 text-accent" />
        </div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
          {t('Preferences', 'Preferences')}
        </h1>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* Language Section */}
        <Card className="p-6">
          <div className="flex items-start gap-4">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-info/10">
              <Globe className="h-5 w-5 text-info" />
            </div>
            <div className="flex-1">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                {t('Langue', 'Language')}
              </h2>
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                {t(
                  "Choisissez la langue de l'interface",
                  'Choose your interface language',
                )}
              </p>
              <div className="mt-4 grid grid-cols-2 gap-3">
                {LANGUAGE_OPTIONS.map((option) => (
                  <label
                    key={option.value}
                    className={`flex cursor-pointer items-center justify-between rounded-lg border-2 p-4 transition-all ${
                      watch('language') === option.value
                        ? 'border-accent bg-accent/5'
                        : 'border-gray-200 hover:border-gray-300 dark:border-gray-700 dark:hover:border-gray-600'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <input
                        type="radio"
                        value={option.value}
                        {...register('language')}
                        className="sr-only"
                      />
                      <div>
                        <p className="font-medium text-gray-900 dark:text-white">
                          {option.nativeLabel}
                        </p>
                        <p className="text-sm text-gray-500 dark:text-gray-400">
                          {option.label}
                        </p>
                      </div>
                    </div>
                    {watch('language') === option.value && (
                      <Check className="h-5 w-5 text-accent" />
                    )}
                  </label>
                ))}
              </div>
              {errors.language && (
                <p className="mt-2 text-sm text-error">{errors.language.message}</p>
              )}
            </div>
          </div>
        </Card>

        {/* Theme Section */}
        <Card className="p-6">
          <div className="flex items-start gap-4">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-warning/10">
              {watchedTheme === 'DARK' ? (
                <Moon className="h-5 w-5 text-warning" />
              ) : watchedTheme === 'LIGHT' ? (
                <Sun className="h-5 w-5 text-warning" />
              ) : (
                <Monitor className="h-5 w-5 text-warning" />
              )}
            </div>
            <div className="flex-1">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                {t('Theme', 'Theme')}
              </h2>
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                {t(
                  "Choisissez le theme de l'application",
                  'Choose your application theme',
                )}
              </p>
              <div className="mt-4 grid grid-cols-3 gap-3">
                {THEME_OPTIONS.map((option) => (
                  <label
                    key={option.value}
                    className={`flex cursor-pointer flex-col items-center justify-center rounded-lg border-2 p-4 transition-all ${
                      watch('theme') === option.value
                        ? 'border-accent bg-accent/5'
                        : 'border-gray-200 hover:border-gray-300 dark:border-gray-700 dark:hover:border-gray-600'
                    }`}
                  >
                    <input
                      type="radio"
                      value={option.value}
                      {...register('theme')}
                      className="sr-only"
                    />
                    <div className="mb-2">
                      {option.value === 'DARK' && (
                        <Moon className="h-6 w-6 text-gray-600 dark:text-gray-300" />
                      )}
                      {option.value === 'LIGHT' && (
                        <Sun className="h-6 w-6 text-gray-600 dark:text-gray-300" />
                      )}
                      {option.value === 'SYSTEM' && (
                        <Monitor className="h-6 w-6 text-gray-600 dark:text-gray-300" />
                      )}
                    </div>
                    <span className="text-sm font-medium text-gray-900 dark:text-white">
                      {watchedLanguage === 'FR' ? option.labelFr : option.label}
                    </span>
                    {watch('theme') === option.value && (
                      <Check className="mt-2 h-4 w-4 text-accent" />
                    )}
                  </label>
                ))}
              </div>
              {errors.theme && (
                <p className="mt-2 text-sm text-error">{errors.theme.message}</p>
              )}
            </div>
          </div>
        </Card>

        {/* Timezone Section */}
        <Card className="p-6">
          <div className="flex items-start gap-4">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-success/10">
              <Clock className="h-5 w-5 text-success" />
            </div>
            <div className="flex-1">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                {t('Fuseau horaire', 'Timezone')}
              </h2>
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                {t(
                  'Definissez votre fuseau horaire pour les dates et heures',
                  'Set your timezone for dates and times',
                )}
              </p>
              <div className="mt-4">
                <select
                  {...register('timezone')}
                  className="w-full rounded-lg border border-gray-200 bg-white px-4 py-3 text-gray-900 transition-colors focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/20 dark:border-gray-700 dark:bg-gray-800 dark:text-white"
                >
                  {Object.entries(timezonesByRegion).map(([region, timezones]) => (
                    <optgroup key={region} label={region}>
                      {timezones.map((tz) => (
                        <option key={tz.value} value={tz.value}>
                          {tz.label} ({tz.value})
                        </option>
                      ))}
                    </optgroup>
                  ))}
                </select>
              </div>
              {errors.timezone && (
                <p className="mt-2 text-sm text-error">{errors.timezone.message}</p>
              )}
            </div>
          </div>
        </Card>

        {/* Date Format Section */}
        <Card className="p-6">
          <div className="flex items-start gap-4">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-accent/10">
              <Calendar className="h-5 w-5 text-accent" />
            </div>
            <div className="flex-1">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                {t('Format de date', 'Date Format')}
              </h2>
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                {t(
                  "Choisissez comment les dates sont affichees dans l'application",
                  'Choose how dates are displayed in the application',
                )}
              </p>
              <div className="mt-4 space-y-2">
                {DATE_FORMAT_OPTIONS.map((option) => (
                  <label
                    key={option.value}
                    className={`flex cursor-pointer items-center justify-between rounded-lg border-2 p-4 transition-all ${
                      watch('dateFormat') === option.value
                        ? 'border-accent bg-accent/5'
                        : 'border-gray-200 hover:border-gray-300 dark:border-gray-700 dark:hover:border-gray-600'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <input
                        type="radio"
                        value={option.value}
                        {...register('dateFormat')}
                        className="sr-only"
                      />
                      <div>
                        <p className="font-medium text-gray-900 dark:text-white">
                          {option.pattern}
                        </p>
                        <p className="text-sm text-gray-500 dark:text-gray-400">
                          {t('Exemple', 'Example')}: {option.example}
                        </p>
                      </div>
                    </div>
                    {watch('dateFormat') === option.value && (
                      <Check className="h-5 w-5 text-accent" />
                    )}
                  </label>
                ))}
              </div>
              {errors.dateFormat && (
                <p className="mt-2 text-sm text-error">{errors.dateFormat.message}</p>
              )}
            </div>
          </div>
        </Card>

        {/* Weekly Digest Section */}
        <Card className="p-6">
          <div className="flex items-start gap-4">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-success/10">
              <Mail className="h-5 w-5 text-success" />
            </div>
            <div className="flex-1">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                {t('Digest hebdomadaire', 'Weekly Digest')}
              </h2>
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                {t(
                  'Recevez un recapitulatif de votre semaine chaque lundi matin',
                  'Receive a summary of your week every Monday morning',
                )}
              </p>
              <div className="mt-4">
                <label className="flex cursor-pointer items-center justify-between">
                  <div className="flex items-center gap-3">
                    <input
                      type="checkbox"
                      {...register('digestEnabled')}
                      className="h-5 w-5 rounded border-gray-300 text-accent focus:ring-accent"
                    />
                    <span className="font-medium text-gray-900 dark:text-white">
                      {t('Activer le digest hebdomadaire', 'Enable weekly digest')}
                    </span>
                  </div>
                </label>
              </div>

              {/* Digest Preview */}
              <div className="mt-6 rounded-lg border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-gray-800/50">
                <h3 className="mb-3 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  {t('Contenu du digest', 'Digest content')}
                </h3>
                <div className="space-y-2">
                  <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
                    <CheckCircle2 className="h-4 w-4 text-success" />
                    <span>{t('Taches completees cette semaine vs semaine derniere', 'Tasks completed this week vs last week')}</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
                    <Target className="h-4 w-4 text-accent" />
                    <span>{t('Progression des objectifs', 'Goals progress summary')}</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
                    <Repeat className="h-4 w-4 text-warning" />
                    <span>{t('Taux de completion des habitudes', 'Habits completion rate')}</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
                    <CalendarDays className="h-4 w-4 text-info" />
                    <span>{t('Evenements a venir pour la semaine prochaine', 'Upcoming events for next week')}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </Card>

        {/* Deadline Reminders Section */}
        <Card className="p-6">
          <div className="flex items-start gap-4">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-warning/10">
              <Bell className="h-5 w-5 text-warning" />
            </div>
            <div className="flex-1">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                {t("Rappels d'echeance", 'Deadline Reminders')}
              </h2>
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                {t(
                  'Recevez des rappels par email pour vos taches, objectifs et evenements a venir',
                  'Receive email reminders for your upcoming tasks, goals, and events',
                )}
              </p>
              <div className="mt-4">
                <label className="flex cursor-pointer items-center justify-between">
                  <div className="flex items-center gap-3">
                    <input
                      type="checkbox"
                      {...register('reminderEnabled')}
                      className="h-5 w-5 rounded border-gray-300 text-accent focus:ring-accent"
                    />
                    <span className="font-medium text-gray-900 dark:text-white">
                      {t("Activer les rappels d'echeance", 'Enable deadline reminders')}
                    </span>
                  </div>
                </label>
              </div>

              {/* Reminder Frequency Options */}
              {watch('reminderEnabled') && (
                <div className="mt-6">
                  <h3 className="mb-3 text-sm font-semibold text-gray-700 dark:text-gray-300">
                    {t('Frequence des rappels', 'Reminder Frequency')}
                  </h3>
                  <div className="space-y-2">
                    {REMINDER_FREQUENCY_OPTIONS.map((option) => (
                      <label
                        key={option.value}
                        className={`flex cursor-pointer items-center justify-between rounded-lg border-2 p-4 transition-all ${
                          watch('reminderFrequency') === option.value
                            ? 'border-accent bg-accent/5'
                            : 'border-gray-200 hover:border-gray-300 dark:border-gray-700 dark:hover:border-gray-600'
                        }`}
                      >
                        <div className="flex items-center gap-3">
                          <input
                            type="radio"
                            value={option.value}
                            {...register('reminderFrequency')}
                            className="sr-only"
                          />
                          <div>
                            <p className="font-medium text-gray-900 dark:text-white">
                              {watchedLanguage === 'FR' ? option.labelFr : option.label}
                            </p>
                            <p className="text-sm text-gray-500 dark:text-gray-400">
                              {watchedLanguage === 'FR' ? option.descriptionFr : option.description}
                            </p>
                          </div>
                        </div>
                        {watch('reminderFrequency') === option.value && (
                          <Check className="h-5 w-5 text-accent" />
                        )}
                      </label>
                    ))}
                  </div>
                </div>
              )}

              {/* Reminder Preview */}
              <div className="mt-6 rounded-lg border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-gray-800/50">
                <h3 className="mb-3 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  {t('Types de rappels', 'Reminder types')}
                </h3>
                <div className="space-y-2">
                  <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
                    <CheckCircle2 className="h-4 w-4 text-error" />
                    <span>{t('Taches avec date limite proche', 'Tasks with approaching due dates')}</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
                    <Target className="h-4 w-4 text-warning" />
                    <span>{t('Objectifs avec echeance proche', 'Goals with approaching deadlines')}</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
                    <CalendarDays className="h-4 w-4 text-info" />
                    <span>{t('Evenements a venir', 'Upcoming events')}</span>
                  </div>
                </div>
                <div className="mt-4 flex items-start gap-2 rounded-md bg-warning/10 p-3">
                  <AlertTriangle className="h-4 w-4 flex-shrink-0 text-warning" />
                  <p className="text-xs text-gray-600 dark:text-gray-400">
                    {t(
                      'Les rappels sont envoyes chaque jour a 8h00 pour les echeances dans votre fenetre de rappel',
                      'Reminders are sent daily at 8:00 AM for deadlines within your reminder window',
                    )}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </Card>

        {/* Save Button */}
        <div className="flex justify-end">
          <Button type="submit" loading={loading} disabled={!isDirty}>
            {t('Enregistrer les preferences', 'Save Preferences')}
          </Button>
        </div>
      </form>
    </div>
  );
}
