import { useEffect, useState, useCallback } from 'react';
import { Calendar as CalendarIcon, Plus, ChevronLeft, ChevronRight, Clock, Target } from 'lucide-react';
import toast from 'react-hot-toast';
import Card from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Modal from '../../components/ui/Modal';
import Input from '../../components/ui/Input';
import { eventService } from '../../services/event.service';
import type { Event, CreateEventRequest } from '../../types/event';
import { cn } from '../../lib/utils';

export default function PersonalCalendarPage() {
  const [events, setEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentDate, setCurrentDate] = useState(new Date());
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [selectedEvent, setSelectedEvent] = useState<Event | null>(null);

  const fetchEvents = useCallback(async () => {
    setLoading(true);
    try {
      const data = await eventService.getPersonalEvents();
      setEvents(data);
    } catch (error) {
      toast.error('Erreur lors du chargement des événements');
      console.error(error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchEvents();
  }, [fetchEvents]);

  const handleCreate = async (data: CreateEventRequest) => {
    try {
      await eventService.createPersonalEvent(data);
      toast.success('Événement créé');
      setIsCreateModalOpen(false);
      await fetchEvents();
    } catch (error) {
      toast.error('Erreur lors de la création');
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Supprimer cet événement ?')) return;
    try {
      await eventService.delete(id);
      toast.success('Événement supprimé');
      setSelectedEvent(null);
      await fetchEvents();
    } catch (error) {
      toast.error('Erreur lors de la suppression');
    }
  };

  // Calendar logic
  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();
  const firstDayOfMonth = new Date(year, month, 1);
  const lastDayOfMonth = new Date(year, month + 1, 0);
  const startingDayOfWeek = firstDayOfMonth.getDay();
  const daysInMonth = lastDayOfMonth.getDate();

  const monthNames = [
    'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
    'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre',
  ];

  const dayNames = ['Dim', 'Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam'];

  const goToPreviousMonth = () => {
    setCurrentDate(new Date(year, month - 1, 1));
  };

  const goToNextMonth = () => {
    setCurrentDate(new Date(year, month + 1, 1));
  };

  const goToToday = () => {
    setCurrentDate(new Date());
  };

  const getEventsForDate = (day: number) => {
    const date = new Date(year, month, day);
    return events.filter((event) => {
      const eventDate = new Date(event.startTime);
      return (
        eventDate.getDate() === date.getDate() &&
        eventDate.getMonth() === date.getMonth() &&
        eventDate.getFullYear() === date.getFullYear()
      );
    });
  };

  const isToday = (day: number) => {
    const today = new Date();
    return (
      day === today.getDate() &&
      month === today.getMonth() &&
      year === today.getFullYear()
    );
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
          <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Mon calendrier</h2>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Vos événements personnels
          </p>
        </div>
        <Button onClick={() => setIsCreateModalOpen(true)}>
          <Plus className="h-4 w-4" />
          Nouvel événement
        </Button>
      </div>

      {/* Calendar Controls */}
      <Card className="p-6">
        <div className="mb-6 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <h3 className="text-xl font-bold text-gray-900 dark:text-gray-100">
              {monthNames[month]} {year}
            </h3>
            <div className="flex gap-2">
              <button
                onClick={goToPreviousMonth}
                className="rounded-lg p-2 hover:bg-gray-100 dark:hover:bg-gray-800"
              >
                <ChevronLeft className="h-5 w-5" />
              </button>
              <button
                onClick={goToNextMonth}
                className="rounded-lg p-2 hover:bg-gray-100 dark:hover:bg-gray-800"
              >
                <ChevronRight className="h-5 w-5" />
              </button>
            </div>
          </div>
          <Button variant="secondary" onClick={goToToday}>
            Aujourd'hui
          </Button>
        </div>

        {/* Calendar Grid */}
        <div className="grid grid-cols-7 gap-2">
          {/* Day Names */}
          {dayNames.map((day) => (
            <div
              key={day}
              className="py-2 text-center text-sm font-semibold text-gray-500 dark:text-gray-400"
            >
              {day}
            </div>
          ))}

          {/* Empty cells before first day */}
          {Array.from({ length: startingDayOfWeek }).map((_, i) => (
            <div key={`empty-${i}`} className="h-24" />
          ))}

          {/* Calendar days */}
          {Array.from({ length: daysInMonth }).map((_, i) => {
            const day = i + 1;
            const dayEvents = getEventsForDate(day);
            const today = isToday(day);

            return (
              <button
                key={day}
                onClick={() => setSelectedDate(new Date(year, month, day))}
                className={cn(
                  'relative h-24 rounded-lg border p-2 text-left transition-all hover:shadow-md',
                  today
                    ? 'border-accent bg-accent/5'
                    : 'border-gray-200 hover:border-gray-300 dark:border-gray-700 dark:hover:border-gray-600',
                  dayEvents.length > 0 && 'bg-blue-50/50 dark:bg-blue-900/10'
                )}
              >
                <span
                  className={cn(
                    'inline-flex h-6 w-6 items-center justify-center rounded-full text-sm font-medium',
                    today
                      ? 'bg-accent text-white'
                      : 'text-gray-700 dark:text-gray-300'
                  )}
                >
                  {day}
                </span>
                {dayEvents.length > 0 && (
                  <div className="mt-1 space-y-1">
                    {dayEvents.slice(0, 2).map((event) => (
                      <div
                        key={event.id}
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedEvent(event);
                        }}
                        className="truncate rounded bg-accent/10 px-1 py-0.5 text-xs font-medium text-accent hover:bg-accent/20"
                      >
                        {new Date(event.startTime).toLocaleTimeString('fr-FR', {
                          hour: '2-digit',
                          minute: '2-digit',
                        })}{' '}
                        {event.title}
                      </div>
                    ))}
                    {dayEvents.length > 2 && (
                      <div className="text-xs text-gray-500 dark:text-gray-400">
                        +{dayEvents.length - 2} autre{dayEvents.length - 2 > 1 ? 's' : ''}
                      </div>
                    )}
                  </div>
                )}
              </button>
            );
          })}
        </div>
      </Card>

      {/* Create Modal */}
      {isCreateModalOpen && (
        <CreateEventModal
          isOpen={isCreateModalOpen}
          onClose={() => setIsCreateModalOpen(false)}
          onCreate={handleCreate}
          initialDate={selectedDate}
        />
      )}

      {/* Event Detail Modal */}
      {selectedEvent && (
        <EventDetailModal
          isOpen={!!selectedEvent}
          onClose={() => setSelectedEvent(null)}
          event={selectedEvent}
          onDelete={handleDelete}
        />
      )}

      {/* Day Events Modal */}
      {selectedDate && (
        <DayEventsModal
          isOpen={!!selectedDate}
          onClose={() => setSelectedDate(null)}
          date={selectedDate}
          events={getEventsForDate(selectedDate.getDate())}
          onEventClick={setSelectedEvent}
        />
      )}
    </div>
  );
}

interface CreateEventModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (data: CreateEventRequest) => void;
  initialDate: Date | null;
}

function CreateEventModal({ isOpen, onClose, onCreate, initialDate }: CreateEventModalProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [startDate, setStartDate] = useState(
    initialDate?.toISOString().split('T')[0] || new Date().toISOString().split('T')[0]
  );
  const [startTime, setStartTime] = useState('09:00');
  const [endDate, setEndDate] = useState(
    initialDate?.toISOString().split('T')[0] || new Date().toISOString().split('T')[0]
  );
  const [endTime, setEndTime] = useState('10:00');
  const [objective, setObjective] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !startDate || !startTime || !endDate || !endTime) return;

    onCreate({
      title: title.trim(),
      description: description.trim() || undefined,
      startTime: `${startDate}T${startTime}:00`,
      endTime: `${endDate}T${endTime}:00`,
      objective: objective.trim() || undefined,
    });

    setTitle('');
    setDescription('');
    setStartDate('');
    setStartTime('09:00');
    setEndDate('');
    setEndTime('10:00');
    setObjective('');
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Nouvel événement">
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          label="Titre"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="Ex: Réunion d'équipe"
          required
        />

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Description
          </label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Détails de l'événement..."
            rows={3}
            className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 placeholder-gray-400 focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent dark:border-gray-600 dark:bg-dark-card dark:text-gray-100 dark:placeholder-gray-500"
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <Input
            label="Date de début"
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            required
          />
          <Input
            label="Heure"
            type="time"
            value={startTime}
            onChange={(e) => setStartTime(e.target.value)}
            required
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <Input
            label="Date de fin"
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            required
          />
          <Input
            label="Heure"
            type="time"
            value={endTime}
            onChange={(e) => setEndTime(e.target.value)}
            required
          />
        </div>

        <Input
          label="Objectif (optionnel)"
          value={objective}
          onChange={(e) => setObjective(e.target.value)}
          placeholder="Ex: Planifier le prochain sprint"
        />

        <div className="flex gap-2">
          <Button type="button" variant="secondary" onClick={onClose} className="flex-1">
            Annuler
          </Button>
          <Button type="submit" className="flex-1">
            Créer
          </Button>
        </div>
      </form>
    </Modal>
  );
}

interface EventDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  event: Event;
  onDelete: (id: string) => void;
}

function EventDetailModal({ isOpen, onClose, event, onDelete }: EventDetailModalProps) {
  const startTime = new Date(event.startTime);
  const endTime = new Date(event.endTime);
  const duration = Math.round((endTime.getTime() - startTime.getTime()) / (1000 * 60));

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={event.title}>
      <div className="space-y-4">
        {event.description && (
          <div>
            <p className="text-sm text-gray-700 dark:text-gray-300">{event.description}</p>
          </div>
        )}

        <div className="space-y-2 rounded-lg bg-gray-50 p-4 dark:bg-gray-800/50">
          <div className="flex items-center gap-2 text-sm">
            <Clock className="h-4 w-4 text-gray-400" />
            <span className="font-medium text-gray-900 dark:text-gray-100">
              {startTime.toLocaleDateString('fr-FR', {
                weekday: 'long',
                year: 'numeric',
                month: 'long',
                day: 'numeric',
              })}
            </span>
          </div>
          <div className="ml-6 text-sm text-gray-600 dark:text-gray-400">
            {startTime.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })} -{' '}
            {endTime.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })} ({duration}{' '}
            min)
          </div>
        </div>

        {event.objective && (
          <div className="flex items-start gap-2 rounded-lg bg-blue-50 p-4 dark:bg-blue-900/20">
            <Target className="mt-0.5 h-4 w-4 text-blue-600 dark:text-blue-400" />
            <div>
              <p className="text-sm font-medium text-blue-900 dark:text-blue-100">Objectif</p>
              <p className="mt-1 text-sm text-blue-700 dark:text-blue-300">{event.objective}</p>
            </div>
          </div>
        )}

        <div className="flex gap-2">
          <Button
            variant="secondary"
            onClick={() => onDelete(event.id)}
            className="flex-1 text-red-600 hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-900/20"
          >
            Supprimer
          </Button>
          <Button onClick={onClose} className="flex-1">
            Fermer
          </Button>
        </div>
      </div>
    </Modal>
  );
}

interface DayEventsModalProps {
  isOpen: boolean;
  onClose: () => void;
  date: Date;
  events: Event[];
  onEventClick: (event: Event) => void;
}

function DayEventsModal({ isOpen, onClose, date, events, onEventClick }: DayEventsModalProps) {
  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={date.toLocaleDateString('fr-FR', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      })}
    >
      <div className="space-y-4">
        {events.length === 0 ? (
          <div className="py-8 text-center">
            <CalendarIcon className="mx-auto h-12 w-12 text-gray-400" />
            <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
              Aucun événement pour cette journée
            </p>
          </div>
        ) : (
          <div className="space-y-2">
            {events
              .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime())
              .map((event) => {
                const startTime = new Date(event.startTime);
                const endTime = new Date(event.endTime);
                const duration = Math.round((endTime.getTime() - startTime.getTime()) / (1000 * 60));

                return (
                  <button
                    key={event.id}
                    onClick={() => {
                      onClose();
                      onEventClick(event);
                    }}
                    className="w-full rounded-lg border border-gray-200 p-4 text-left transition-all hover:border-accent hover:shadow-md dark:border-gray-700 dark:hover:border-accent"
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <h4 className="font-semibold text-gray-900 dark:text-gray-100">
                          {event.title}
                        </h4>
                        {event.description && (
                          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
                            {event.description}
                          </p>
                        )}
                      </div>
                    </div>
                    <div className="mt-2 flex items-center gap-4 text-xs text-gray-500 dark:text-gray-400">
                      <span className="flex items-center gap-1">
                        <Clock className="h-3 w-3" />
                        {startTime.toLocaleTimeString('fr-FR', {
                          hour: '2-digit',
                          minute: '2-digit',
                        })}{' '}
                        - {endTime.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}
                      </span>
                      <span>({duration} min)</span>
                    </div>
                  </button>
                );
              })}
          </div>
        )}
        <Button onClick={onClose} className="w-full">
          Fermer
        </Button>
      </div>
    </Modal>
  );
}
