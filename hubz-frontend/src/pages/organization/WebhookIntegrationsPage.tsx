import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import {
  Webhook,
  Plus,
  Trash2,
  TestTube2,
  Power,
  PowerOff,
  Pencil,
  X,
  Check,
  Loader2,
  ExternalLink,
  Shield,
} from 'lucide-react';
import toast from 'react-hot-toast';
import Card from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import { cn } from '../../lib/utils';
import { webhookService } from '../../services/webhook.service';
import type {
  WebhookConfig,
  CreateWebhookConfigRequest,
  WebhookServiceType,
  WebhookEventType,
} from '../../types/webhook';

const SERVICE_OPTIONS: { value: WebhookServiceType; label: string; color: string }[] = [
  { value: 'SLACK', label: 'Slack', color: '#4A154B' },
  { value: 'DISCORD', label: 'Discord', color: '#5865F2' },
  { value: 'GITHUB', label: 'GitHub', color: '#24292F' },
  { value: 'CUSTOM', label: 'Custom', color: '#6B7280' },
];

const EVENT_OPTIONS: { value: WebhookEventType; label: string; description: string }[] = [
  { value: 'TASK_CREATED', label: 'Tache creee', description: 'Quand une nouvelle tache est creee' },
  { value: 'TASK_COMPLETED', label: 'Tache terminee', description: 'Quand une tache passe au statut DONE' },
  { value: 'GOAL_COMPLETED', label: 'Objectif atteint', description: 'Quand un objectif est complete' },
  { value: 'NOTE_CREATED', label: 'Note creee', description: 'Quand une nouvelle note est creee' },
  { value: 'MEMBER_JOINED', label: 'Membre rejoint', description: 'Quand un nouveau membre rejoint' },
];

interface WebhookFormData {
  name: string;
  service: WebhookServiceType;
  webhookUrl: string;
  secret: string;
  events: WebhookEventType[];
}

export default function WebhookIntegrationsPage() {
  const { orgId } = useParams<{ orgId: string }>();
  const [webhooks, setWebhooks] = useState<WebhookConfig[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [testingId, setTestingId] = useState<string | null>(null);
  const [togglingId, setTogglingId] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const form = useForm<WebhookFormData>({
    defaultValues: {
      name: '',
      service: 'SLACK',
      webhookUrl: '',
      secret: '',
      events: [],
    },
  });

  const selectedEvents = form.watch('events');

  useEffect(() => {
    if (!orgId) return;
    fetchWebhooks();
  }, [orgId]);

  const fetchWebhooks = async () => {
    if (!orgId) return;
    try {
      const data = await webhookService.getByOrganization(orgId);
      setWebhooks(data);
    } catch {
      toast.error('Erreur lors du chargement des webhooks');
    } finally {
      setIsLoading(false);
    }
  };

  const handleToggleEvent = (event: WebhookEventType) => {
    const current = form.getValues('events');
    if (current.includes(event)) {
      form.setValue(
        'events',
        current.filter((e) => e !== event)
      );
    } else {
      form.setValue('events', [...current, event]);
    }
  };

  const handleCreate = async (data: WebhookFormData) => {
    if (!orgId) return;
    if (data.events.length === 0) {
      toast.error('Selectionnez au moins un evenement');
      return;
    }

    setIsSubmitting(true);
    try {
      const request: CreateWebhookConfigRequest = {
        name: data.name,
        service: data.service,
        webhookUrl: data.webhookUrl,
        secret: data.secret || undefined,
        events: data.events,
      };
      const created = await webhookService.create(orgId, request);
      setWebhooks((prev) => [...prev, created]);
      setShowCreateForm(false);
      form.reset();
      toast.success('Webhook cree avec succes');
    } catch {
      toast.error('Erreur lors de la creation du webhook');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleUpdate = async (data: WebhookFormData) => {
    if (!orgId || !editingId) return;
    if (data.events.length === 0) {
      toast.error('Selectionnez au moins un evenement');
      return;
    }

    setIsSubmitting(true);
    try {
      const updated = await webhookService.update(orgId, editingId, {
        name: data.name,
        service: data.service,
        webhookUrl: data.webhookUrl,
        secret: data.secret || undefined,
        events: data.events,
      });
      setWebhooks((prev) => prev.map((w) => (w.id === editingId ? updated : w)));
      setEditingId(null);
      form.reset();
      toast.success('Webhook mis a jour');
    } catch {
      toast.error('Erreur lors de la mise a jour du webhook');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!orgId) return;
    setDeletingId(id);
    try {
      await webhookService.delete(orgId, id);
      setWebhooks((prev) => prev.filter((w) => w.id !== id));
      toast.success('Webhook supprime');
    } catch {
      toast.error('Erreur lors de la suppression du webhook');
    } finally {
      setDeletingId(null);
    }
  };

  const handleTest = async (id: string) => {
    if (!orgId) return;
    setTestingId(id);
    try {
      const result = await webhookService.test(orgId, id);
      if (result.success) {
        toast.success(`Test reussi (HTTP ${result.statusCode})`);
      } else {
        toast.error(result.message);
      }
    } catch {
      toast.error('Erreur lors du test du webhook');
    } finally {
      setTestingId(null);
    }
  };

  const handleToggleEnabled = async (webhook: WebhookConfig) => {
    if (!orgId) return;
    setTogglingId(webhook.id);
    try {
      const updated = await webhookService.update(orgId, webhook.id, {
        enabled: !webhook.enabled,
      });
      setWebhooks((prev) => prev.map((w) => (w.id === webhook.id ? updated : w)));
      toast.success(updated.enabled ? 'Webhook active' : 'Webhook desactive');
    } catch {
      toast.error('Erreur lors de la modification du webhook');
    } finally {
      setTogglingId(null);
    }
  };

  const startEditing = (webhook: WebhookConfig) => {
    setEditingId(webhook.id);
    setShowCreateForm(false);
    form.reset({
      name: webhook.name,
      service: webhook.service,
      webhookUrl: webhook.webhookUrl,
      secret: '',
      events: webhook.events,
    });
  };

  const cancelForm = () => {
    setShowCreateForm(false);
    setEditingId(null);
    form.reset();
  };

  const getServiceInfo = (service: WebhookServiceType) => {
    return SERVICE_OPTIONS.find((s) => s.value === service) || SERVICE_OPTIONS[3];
  };

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-accent border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="flex h-full flex-col gap-6 overflow-auto p-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
            Integrations Webhooks
          </h2>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Configurez des webhooks pour notifier des services externes lors d'evenements
          </p>
        </div>
        {!showCreateForm && !editingId && (
          <Button onClick={() => { setShowCreateForm(true); form.reset(); }}>
            <Plus className="h-4 w-4" />
            Ajouter un webhook
          </Button>
        )}
      </div>

      {/* Create / Edit Form */}
      {(showCreateForm || editingId) && (
        <Card className="p-6">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="font-semibold text-gray-900 dark:text-gray-100">
              {editingId ? 'Modifier le webhook' : 'Nouveau webhook'}
            </h3>
            <button onClick={cancelForm} className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300">
              <X className="h-5 w-5" />
            </button>
          </div>

          <form
            onSubmit={form.handleSubmit(editingId ? handleUpdate : handleCreate)}
            className="space-y-4"
          >
            {/* Name */}
            <Input
              label="Nom"
              placeholder="Mon webhook Slack"
              {...form.register('name', { required: 'Nom requis' })}
              error={form.formState.errors.name?.message}
            />

            {/* Service Type */}
            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
                Service
              </label>
              <div className="flex flex-wrap gap-2">
                {SERVICE_OPTIONS.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => form.setValue('service', option.value)}
                    className={cn(
                      'rounded-lg border-2 px-4 py-2 text-sm font-medium transition-colors',
                      form.watch('service') === option.value
                        ? 'border-accent bg-accent/10 text-accent'
                        : 'border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400 hover:border-gray-300 dark:hover:border-gray-600'
                    )}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Webhook URL */}
            <Input
              label="URL du webhook"
              placeholder="https://hooks.slack.com/services/..."
              {...form.register('webhookUrl', {
                required: 'URL requise',
                pattern: {
                  value: /^https?:\/\/.+/,
                  message: 'URL invalide',
                },
              })}
              error={form.formState.errors.webhookUrl?.message}
              icon={<ExternalLink className="h-4 w-4" />}
            />

            {/* Secret (optional) */}
            <Input
              label="Secret HMAC (optionnel)"
              placeholder="Cle secrete pour signer les payloads"
              type="password"
              {...form.register('secret')}
              icon={<Shield className="h-4 w-4" />}
            />

            {/* Events */}
            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
                Evenements
              </label>
              <div className="space-y-2">
                {EVENT_OPTIONS.map((event) => (
                  <label
                    key={event.value}
                    className={cn(
                      'flex cursor-pointer items-center gap-3 rounded-lg border-2 p-3 transition-colors',
                      selectedEvents.includes(event.value)
                        ? 'border-accent bg-accent/5'
                        : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'
                    )}
                  >
                    <input
                      type="checkbox"
                      checked={selectedEvents.includes(event.value)}
                      onChange={() => handleToggleEvent(event.value)}
                      className="h-4 w-4 rounded border-gray-300 text-accent focus:ring-accent"
                    />
                    <div>
                      <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                        {event.label}
                      </p>
                      <p className="text-xs text-gray-500 dark:text-gray-400">
                        {event.description}
                      </p>
                    </div>
                  </label>
                ))}
              </div>
            </div>

            {/* Submit */}
            <div className="flex gap-2 pt-2">
              <Button type="submit" loading={isSubmitting}>
                <Check className="h-4 w-4" />
                {editingId ? 'Mettre a jour' : 'Creer'}
              </Button>
              <Button type="button" variant="ghost" onClick={cancelForm}>
                Annuler
              </Button>
            </div>
          </form>
        </Card>
      )}

      {/* Webhooks List */}
      {webhooks.length === 0 && !showCreateForm ? (
        <Card className="flex flex-col items-center justify-center py-16">
          <Webhook className="h-12 w-12 text-gray-400 dark:text-gray-600 mb-4" />
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">
            Aucun webhook configure
          </h3>
          <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
            Ajoutez des webhooks pour recevoir des notifications dans vos outils preferes
          </p>
          <Button onClick={() => { setShowCreateForm(true); form.reset(); }}>
            <Plus className="h-4 w-4" />
            Ajouter un webhook
          </Button>
        </Card>
      ) : (
        <div className="space-y-3">
          {webhooks.map((webhook) => {
            const serviceInfo = getServiceInfo(webhook.service);
            const isTesting = testingId === webhook.id;
            const isToggling = togglingId === webhook.id;
            const isDeleting = deletingId === webhook.id;

            return (
              <Card
                key={webhook.id}
                className={cn(
                  'p-4 transition-opacity',
                  !webhook.enabled && 'opacity-60'
                )}
              >
                <div className="flex items-start justify-between gap-4">
                  {/* Left - Info */}
                  <div className="flex items-start gap-3 min-w-0">
                    <div
                      className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg text-white text-xs font-bold"
                      style={{ backgroundColor: serviceInfo.color }}
                    >
                      {serviceInfo.label.substring(0, 2).toUpperCase()}
                    </div>
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <h4 className="font-medium text-gray-900 dark:text-gray-100 truncate">
                          {webhook.name}
                        </h4>
                        <span
                          className={cn(
                            'inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium',
                            webhook.enabled
                              ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
                              : 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400'
                          )}
                        >
                          {webhook.enabled ? 'Actif' : 'Inactif'}
                        </span>
                        {webhook.hasSecret && (
                          <Shield className="h-3.5 w-3.5 text-gray-400" title="Secret HMAC configure" />
                        )}
                      </div>
                      <p className="text-xs text-gray-500 dark:text-gray-400 truncate mt-0.5">
                        {webhook.webhookUrl}
                      </p>
                      <div className="mt-1.5 flex flex-wrap gap-1">
                        {webhook.events.map((event) => {
                          const eventInfo = EVENT_OPTIONS.find((e) => e.value === event);
                          return (
                            <span
                              key={event}
                              className="inline-flex items-center rounded-md bg-blue-50 dark:bg-blue-900/20 px-2 py-0.5 text-xs font-medium text-blue-700 dark:text-blue-400"
                            >
                              {eventInfo?.label || event}
                            </span>
                          );
                        })}
                      </div>
                    </div>
                  </div>

                  {/* Right - Actions */}
                  <div className="flex items-center gap-1 flex-shrink-0">
                    <button
                      onClick={() => handleTest(webhook.id)}
                      disabled={isTesting || !webhook.enabled}
                      className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-800 hover:text-gray-700 dark:hover:text-gray-300 disabled:opacity-50 transition-colors"
                      title="Tester le webhook"
                    >
                      {isTesting ? (
                        <Loader2 className="h-4 w-4 animate-spin" />
                      ) : (
                        <TestTube2 className="h-4 w-4" />
                      )}
                    </button>
                    <button
                      onClick={() => handleToggleEnabled(webhook)}
                      disabled={isToggling}
                      className={cn(
                        'rounded-lg p-2 transition-colors',
                        webhook.enabled
                          ? 'text-green-600 hover:bg-green-50 dark:hover:bg-green-900/20'
                          : 'text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800'
                      )}
                      title={webhook.enabled ? 'Desactiver' : 'Activer'}
                    >
                      {isToggling ? (
                        <Loader2 className="h-4 w-4 animate-spin" />
                      ) : webhook.enabled ? (
                        <Power className="h-4 w-4" />
                      ) : (
                        <PowerOff className="h-4 w-4" />
                      )}
                    </button>
                    <button
                      onClick={() => startEditing(webhook)}
                      className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-800 hover:text-gray-700 dark:hover:text-gray-300 transition-colors"
                      title="Modifier"
                    >
                      <Pencil className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => handleDelete(webhook.id)}
                      disabled={isDeleting}
                      className="rounded-lg p-2 text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 hover:text-red-700 transition-colors"
                      title="Supprimer"
                    >
                      {isDeleting ? (
                        <Loader2 className="h-4 w-4 animate-spin" />
                      ) : (
                        <Trash2 className="h-4 w-4" />
                      )}
                    </button>
                  </div>
                </div>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
