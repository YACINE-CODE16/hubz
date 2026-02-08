import { useState, useEffect } from 'react';
import { X, Mail, Bell, Save, Loader2 } from 'lucide-react';
import { notificationService } from '../../services/notification.service';
import type { NotificationPreferences, UpdateNotificationPreferencesRequest } from '../../types/notification';

interface NotificationPreferencesModalProps {
  isOpen: boolean;
  onClose: () => void;
}

interface PreferenceToggleProps {
  label: string;
  description: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  disabled?: boolean;
}

function PreferenceToggle({ label, description, checked, onChange, disabled }: PreferenceToggleProps) {
  return (
    <div className="flex items-center justify-between py-3 border-b border-gray-100 dark:border-white/5 last:border-b-0">
      <div className="flex-1 mr-4">
        <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{label}</p>
        <p className="text-xs text-gray-500 dark:text-gray-400">{description}</p>
      </div>
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        disabled={disabled}
        onClick={() => onChange(!checked)}
        className={`relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 dark:focus:ring-offset-dark-card ${
          checked ? 'bg-blue-500' : 'bg-gray-200 dark:bg-gray-600'
        } ${disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
      >
        <span
          className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
            checked ? 'translate-x-5' : 'translate-x-0'
          }`}
        />
      </button>
    </div>
  );
}

export default function NotificationPreferencesModal({ isOpen, onClose }: NotificationPreferencesModalProps) {
  const [preferences, setPreferences] = useState<NotificationPreferences | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [hasChanges, setHasChanges] = useState(false);

  useEffect(() => {
    if (isOpen) {
      fetchPreferences();
    }
  }, [isOpen]);

  const fetchPreferences = async () => {
    try {
      setLoading(true);
      setError(null);
      const prefs = await notificationService.getPreferences();
      setPreferences(prefs);
      setHasChanges(false);
    } catch (err) {
      console.error('Failed to fetch notification preferences:', err);
      setError('Impossible de charger les preferences');
    } finally {
      setLoading(false);
    }
  };

  const updatePreference = <K extends keyof UpdateNotificationPreferencesRequest>(
    key: K,
    value: UpdateNotificationPreferencesRequest[K]
  ) => {
    if (!preferences) return;
    setPreferences({ ...preferences, [key]: value });
    setHasChanges(true);
  };

  const handleSave = async () => {
    if (!preferences) return;

    try {
      setSaving(true);
      setError(null);
      const request: UpdateNotificationPreferencesRequest = {
        emailEnabled: preferences.emailEnabled,
        taskAssigned: preferences.taskAssigned,
        taskCompleted: preferences.taskCompleted,
        taskDueSoon: preferences.taskDueSoon,
        mentions: preferences.mentions,
        invitations: preferences.invitations,
        roleChanges: preferences.roleChanges,
        comments: preferences.comments,
        goalDeadlines: preferences.goalDeadlines,
        eventReminders: preferences.eventReminders,
      };
      await notificationService.updatePreferences(request);
      setHasChanges(false);
      onClose();
    } catch (err) {
      console.error('Failed to save notification preferences:', err);
      setError('Impossible de sauvegarder les preferences');
    } finally {
      setSaving(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={onClose}
      />

      {/* Modal */}
      <div className="relative w-full max-w-lg mx-4 bg-light-card dark:bg-dark-card rounded-xl shadow-xl max-h-[90vh] overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200/50 dark:border-white/10">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-10 h-10 rounded-full bg-blue-100 dark:bg-blue-500/20">
              <Bell className="w-5 h-5 text-blue-500" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                Parametres de notification
              </h2>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Configurez vos preferences email
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-light-hover dark:hover:bg-dark-hover transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="px-6 py-4 overflow-y-auto max-h-[60vh]">
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="w-6 h-6 animate-spin text-blue-500" />
              <span className="ml-2 text-gray-500">Chargement...</span>
            </div>
          ) : error ? (
            <div className="py-8 text-center text-red-500">
              {error}
            </div>
          ) : preferences ? (
            <div className="space-y-6">
              {/* Master toggle */}
              <div className="p-4 bg-blue-50 dark:bg-blue-500/10 rounded-xl">
                <div className="flex items-center gap-3 mb-3">
                  <Mail className="w-5 h-5 text-blue-500" />
                  <h3 className="font-medium text-gray-900 dark:text-gray-100">
                    Notifications par email
                  </h3>
                </div>
                <PreferenceToggle
                  label="Activer les notifications email"
                  description="Recevez des emails pour les notifications importantes"
                  checked={preferences.emailEnabled}
                  onChange={(checked) => updatePreference('emailEnabled', checked)}
                />
              </div>

              {/* Individual toggles */}
              <div className={preferences.emailEnabled ? '' : 'opacity-50 pointer-events-none'}>
                <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
                  Types de notifications
                </h3>

                <div className="space-y-1 bg-light-hover/50 dark:bg-dark-hover/50 rounded-xl p-4">
                  <h4 className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-2">
                    Taches
                  </h4>
                  <PreferenceToggle
                    label="Taches assignees"
                    description="Quand une tache vous est assignee"
                    checked={preferences.taskAssigned}
                    onChange={(checked) => updatePreference('taskAssigned', checked)}
                    disabled={!preferences.emailEnabled}
                  />
                  <PreferenceToggle
                    label="Taches terminees"
                    description="Quand une tache assignee est terminee"
                    checked={preferences.taskCompleted}
                    onChange={(checked) => updatePreference('taskCompleted', checked)}
                    disabled={!preferences.emailEnabled}
                  />
                  <PreferenceToggle
                    label="Echeances proches"
                    description="Rappels pour les taches dont l'echeance approche"
                    checked={preferences.taskDueSoon}
                    onChange={(checked) => updatePreference('taskDueSoon', checked)}
                    disabled={!preferences.emailEnabled}
                  />
                </div>

                <div className="space-y-1 bg-light-hover/50 dark:bg-dark-hover/50 rounded-xl p-4 mt-4">
                  <h4 className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-2">
                    Collaboration
                  </h4>
                  <PreferenceToggle
                    label="Mentions"
                    description="Quand quelqu'un vous mentionne dans un commentaire"
                    checked={preferences.mentions}
                    onChange={(checked) => updatePreference('mentions', checked)}
                    disabled={!preferences.emailEnabled}
                  />
                  <PreferenceToggle
                    label="Commentaires"
                    description="Nouveaux commentaires sur vos taches"
                    checked={preferences.comments}
                    onChange={(checked) => updatePreference('comments', checked)}
                    disabled={!preferences.emailEnabled}
                  />
                </div>

                <div className="space-y-1 bg-light-hover/50 dark:bg-dark-hover/50 rounded-xl p-4 mt-4">
                  <h4 className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-2">
                    Organisation
                  </h4>
                  <PreferenceToggle
                    label="Invitations"
                    description="Invitations a rejoindre des organisations"
                    checked={preferences.invitations}
                    onChange={(checked) => updatePreference('invitations', checked)}
                    disabled={!preferences.emailEnabled}
                  />
                  <PreferenceToggle
                    label="Changements de role"
                    description="Quand votre role dans une organisation change"
                    checked={preferences.roleChanges}
                    onChange={(checked) => updatePreference('roleChanges', checked)}
                    disabled={!preferences.emailEnabled}
                  />
                </div>

                <div className="space-y-1 bg-light-hover/50 dark:bg-dark-hover/50 rounded-xl p-4 mt-4">
                  <h4 className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-2">
                    Objectifs & Evenements
                  </h4>
                  <PreferenceToggle
                    label="Echeances d'objectifs"
                    description="Rappels pour les objectifs dont l'echeance approche"
                    checked={preferences.goalDeadlines}
                    onChange={(checked) => updatePreference('goalDeadlines', checked)}
                    disabled={!preferences.emailEnabled}
                  />
                  <PreferenceToggle
                    label="Rappels d'evenements"
                    description="Rappels avant vos evenements planifies"
                    checked={preferences.eventReminders}
                    onChange={(checked) => updatePreference('eventReminders', checked)}
                    disabled={!preferences.emailEnabled}
                  />
                </div>
              </div>
            </div>
          ) : null}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-gray-200/50 dark:border-white/10 bg-light-hover/30 dark:bg-dark-hover/30">
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-light-hover dark:hover:bg-dark-hover rounded-lg transition-colors"
          >
            Annuler
          </button>
          <button
            onClick={handleSave}
            disabled={saving || !hasChanges}
            className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-500 hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed rounded-lg transition-colors"
          >
            {saving ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                Enregistrement...
              </>
            ) : (
              <>
                <Save className="w-4 h-4" />
                Enregistrer
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
