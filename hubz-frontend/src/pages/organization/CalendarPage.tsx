import { useEffect, useState, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import CalendarView from '../../components/features/CalendarView';
import { eventService } from '../../services/event.service';
import type { Event, CreateEventRequest } from '../../types/event';

export default function CalendarPage() {
  const { orgId } = useParams<{ orgId: string }>();
  const [events, setEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchEvents = useCallback(async () => {
    if (!orgId) return;
    setLoading(true);
    try {
      const data = await eventService.getByOrganization(orgId);
      setEvents(data);
    } catch (error) {
      toast.error('Erreur lors du chargement des \u00e9v\u00e9nements');
      console.error(error);
    } finally {
      setLoading(false);
    }
  }, [orgId]);

  useEffect(() => {
    fetchEvents();
  }, [fetchEvents]);

  const handleCreate = async (data: CreateEventRequest) => {
    if (!orgId) return;
    try {
      await eventService.createOrganizationEvent(orgId, data);
      toast.success('\u00c9v\u00e9nement cr\u00e9\u00e9');
      await fetchEvents();
    } catch (error) {
      toast.error('Erreur lors de la cr\u00e9ation');
    }
  };

  const handleDelete = async (id: string, deleteAllOccurrences?: boolean) => {
    try {
      await eventService.delete(id, deleteAllOccurrences);
      toast.success('Evenement supprime');
      await fetchEvents();
    } catch (error) {
      toast.error('Erreur lors de la suppression');
    }
  };

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="text-gray-500 dark:text-gray-400">Chargement...</div>
      </div>
    );
  }

  return (
    <div className="flex h-full flex-col gap-6 overflow-auto p-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Calendrier</h2>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            \u00c9v\u00e9nements de votre organisation
          </p>
        </div>
      </div>

      {/* Calendar */}
      <CalendarView
        events={events}
        onCreateEvent={handleCreate}
        onDeleteEvent={handleDelete}
      />
    </div>
  );
}
