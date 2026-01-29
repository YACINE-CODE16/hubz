import { useState, useMemo, useCallback } from 'react';
import {
  Calendar as CalendarIcon,
  Plus,
  ChevronLeft,
  ChevronRight,
  Clock,
  Target,
} from 'lucide-react';
import Card from '../ui/Card';
import Button from '../ui/Button';
import Modal from '../ui/Modal';
import Input from '../ui/Input';
import { cn } from '../../lib/utils';
import type { Event, CreateEventRequest } from '../../types/event';

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const MONTH_NAMES = [
  'Janvier', 'F\u00e9vrier', 'Mars', 'Avril', 'Mai', 'Juin',
  'Juillet', 'Ao\u00fbt', 'Septembre', 'Octobre', 'Novembre', 'D\u00e9cembre',
];

const DAY_NAMES_SHORT = ['Dim', 'Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam'];

const DAY_NAMES_FULL = [
  'Dimanche', 'Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi',
];

const HOURS = Array.from({ length: 24 }, (_, i) => i);

const HOUR_HEIGHT = 60; // px per hour row

type ViewMode = 'month' | 'week' | 'day';

// ---------------------------------------------------------------------------
// Props
// ---------------------------------------------------------------------------

interface CalendarViewProps {
  events: Event[];
  onCreateEvent: (data: CreateEventRequest) => void;
  onDeleteEvent: (id: string) => void;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function isSameDay(a: Date, b: Date): boolean {
  return (
    a.getDate() === b.getDate() &&
    a.getMonth() === b.getMonth() &&
    a.getFullYear() === b.getFullYear()
  );
}

function getStartOfWeek(date: Date): Date {
  const d = new Date(date);
  const day = d.getDay(); // 0=Sun
  d.setDate(d.getDate() - day);
  d.setHours(0, 0, 0, 0);
  return d;
}

function addDays(date: Date, days: number): Date {
  const d = new Date(date);
  d.setDate(d.getDate() + days);
  return d;
}

function formatHour(hour: number): string {
  return `${hour.toString().padStart(2, '0')}:00`;
}

function formatDateISO(date: Date): string {
  const y = date.getFullYear();
  const m = (date.getMonth() + 1).toString().padStart(2, '0');
  const d = date.getDate().toString().padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function formatTimeHHMM(date: Date): string {
  return date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
}

function getEventsForDate(events: Event[], date: Date): Event[] {
  return events.filter((event) => {
    const eventDate = new Date(event.startTime);
    return isSameDay(eventDate, date);
  });
}

function computeEventPosition(event: Event): { top: number; height: number } {
  const start = new Date(event.startTime);
  const end = new Date(event.endTime);
  const startMinutes = start.getHours() * 60 + start.getMinutes();
  const endMinutes = end.getHours() * 60 + end.getMinutes();
  const durationMinutes = Math.max(endMinutes - startMinutes, 15); // min 15px height
  return {
    top: startMinutes, // 1px per minute since HOUR_HEIGHT=60
    height: durationMinutes,
  };
}

// ---------------------------------------------------------------------------
// Main Component
// ---------------------------------------------------------------------------

export default function CalendarView({ events, onCreateEvent, onDeleteEvent }: CalendarViewProps) {
  const [currentDate, setCurrentDate] = useState(new Date());
  const [viewMode, setViewMode] = useState<ViewMode>('month');
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const [selectedEvent, setSelectedEvent] = useState<Event | null>(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [createInitialDate, setCreateInitialDate] = useState<Date | null>(null);
  const [createInitialTime, setCreateInitialTime] = useState<string | null>(null);

  const today = useMemo(() => {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    return d;
  }, []);

  // -- Navigation -----------------------------------------------------------

  const goToPrevious = useCallback(() => {
    setCurrentDate((prev) => {
      const d = new Date(prev);
      if (viewMode === 'month') {
        d.setMonth(d.getMonth() - 1);
      } else if (viewMode === 'week') {
        d.setDate(d.getDate() - 7);
      } else {
        d.setDate(d.getDate() - 1);
      }
      return d;
    });
  }, [viewMode]);

  const goToNext = useCallback(() => {
    setCurrentDate((prev) => {
      const d = new Date(prev);
      if (viewMode === 'month') {
        d.setMonth(d.getMonth() + 1);
      } else if (viewMode === 'week') {
        d.setDate(d.getDate() + 7);
      } else {
        d.setDate(d.getDate() + 1);
      }
      return d;
    });
  }, [viewMode]);

  const goToToday = useCallback(() => {
    setCurrentDate(new Date());
  }, []);

  // -- Header text ----------------------------------------------------------

  const headerText = useMemo(() => {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();

    if (viewMode === 'month') {
      return `${MONTH_NAMES[month]} ${year}`;
    }

    if (viewMode === 'week') {
      const weekStart = getStartOfWeek(currentDate);
      const weekEnd = addDays(weekStart, 6);
      const startMonth = MONTH_NAMES[weekStart.getMonth()];
      const endMonth = MONTH_NAMES[weekEnd.getMonth()];
      const startDay = weekStart.getDate();
      const endDay = weekEnd.getDate();

      if (weekStart.getMonth() === weekEnd.getMonth()) {
        return `${startDay} - ${endDay} ${startMonth} ${year}`;
      }
      // Abbreviated month names for cross-month weeks
      const startMonthShort = startMonth.substring(0, 3);
      const endMonthShort = endMonth.substring(0, 3);
      if (weekStart.getFullYear() !== weekEnd.getFullYear()) {
        return `${startDay} ${startMonthShort} ${weekStart.getFullYear()} - ${endDay} ${endMonthShort} ${weekEnd.getFullYear()}`;
      }
      return `${startDay} ${startMonthShort} - ${endDay} ${endMonthShort} ${year}`;
    }

    // Day view
    const dayOfWeek = DAY_NAMES_FULL[currentDate.getDay()];
    const dayNum = currentDate.getDate();
    const monthName = MONTH_NAMES[month];
    return `${dayOfWeek} ${dayNum} ${monthName} ${year}`;
  }, [currentDate, viewMode]);

  // -- Open create modal helpers -------------------------------------------

  const openCreateModal = useCallback((date?: Date, time?: string) => {
    setCreateInitialDate(date ?? null);
    setCreateInitialTime(time ?? null);
    setIsCreateModalOpen(true);
  }, []);

  const handleCreate = useCallback(
    (data: CreateEventRequest) => {
      onCreateEvent(data);
      setIsCreateModalOpen(false);
    },
    [onCreateEvent],
  );

  const handleDelete = useCallback(
    (id: string) => {
      onDeleteEvent(id);
      setSelectedEvent(null);
    },
    [onDeleteEvent],
  );

  // -- Month helpers -------------------------------------------------------

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();
  const firstDayOfMonth = new Date(year, month, 1);
  const lastDayOfMonth = new Date(year, month + 1, 0);
  const startingDayOfWeek = firstDayOfMonth.getDay();
  const daysInMonth = lastDayOfMonth.getDate();

  // -- Week helpers ---------------------------------------------------------

  const weekStart = useMemo(() => getStartOfWeek(currentDate), [currentDate]);
  const weekDays = useMemo(
    () => Array.from({ length: 7 }, (_, i) => addDays(weekStart, i)),
    [weekStart],
  );

  // ========================================================================
  // RENDER
  // ========================================================================

  return (
    <>
      <Card className="p-6">
        {/* Controls bar */}
        <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <h3 className="text-xl font-bold text-gray-900 dark:text-gray-100">
              {headerText}
            </h3>
            <div className="flex gap-1">
              <button
                onClick={goToPrevious}
                className="rounded-lg p-2 text-gray-600 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-800 transition-colors"
                aria-label="Pr\u00e9c\u00e9dent"
              >
                <ChevronLeft className="h-5 w-5" />
              </button>
              <button
                onClick={goToNext}
                className="rounded-lg p-2 text-gray-600 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-800 transition-colors"
                aria-label="Suivant"
              >
                <ChevronRight className="h-5 w-5" />
              </button>
            </div>
          </div>

          <div className="flex items-center gap-3">
            {/* View switcher */}
            <div className="flex rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden">
              {(['month', 'week', 'day'] as const).map((mode) => {
                const labels: Record<ViewMode, string> = {
                  month: 'Mois',
                  week: 'Semaine',
                  day: 'Jour',
                };
                return (
                  <button
                    key={mode}
                    onClick={() => setViewMode(mode)}
                    className={cn(
                      'px-3 py-1.5 text-sm font-medium transition-colors',
                      viewMode === mode
                        ? 'bg-accent text-white'
                        : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800',
                    )}
                  >
                    {labels[mode]}
                  </button>
                );
              })}
            </div>

            <Button variant="secondary" size="sm" onClick={goToToday}>
              Aujourd&apos;hui
            </Button>

            <Button size="sm" onClick={() => openCreateModal()}>
              <Plus className="h-4 w-4" />
              Nouveau
            </Button>
          </div>
        </div>

        {/* View content */}
        {viewMode === 'month' && (
          <MonthView
            year={year}
            month={month}
            daysInMonth={daysInMonth}
            startingDayOfWeek={startingDayOfWeek}
            events={events}
            today={today}
            onDayClick={(date) => setSelectedDate(date)}
            onEventClick={setSelectedEvent}
          />
        )}

        {viewMode === 'week' && (
          <WeekView
            weekDays={weekDays}
            events={events}
            today={today}
            onSlotClick={(date, hour) => openCreateModal(date, formatHour(hour))}
            onEventClick={setSelectedEvent}
          />
        )}

        {viewMode === 'day' && (
          <DayView
            date={currentDate}
            events={events}
            today={today}
            onSlotClick={(hour) => openCreateModal(currentDate, formatHour(hour))}
            onEventClick={setSelectedEvent}
          />
        )}
      </Card>

      {/* Create Modal */}
      {isCreateModalOpen && (
        <CreateEventModal
          isOpen={isCreateModalOpen}
          onClose={() => setIsCreateModalOpen(false)}
          onCreate={handleCreate}
          initialDate={createInitialDate}
          initialTime={createInitialTime}
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

      {/* Day Events Modal (month view day-click) */}
      {selectedDate && (
        <DayEventsModal
          isOpen={!!selectedDate}
          onClose={() => setSelectedDate(null)}
          date={selectedDate}
          events={getEventsForDate(events, selectedDate)}
          onEventClick={(event) => {
            setSelectedDate(null);
            setSelectedEvent(event);
          }}
        />
      )}
    </>
  );
}

// ===========================================================================
// MONTH VIEW
// ===========================================================================

interface MonthViewProps {
  year: number;
  month: number;
  daysInMonth: number;
  startingDayOfWeek: number;
  events: Event[];
  today: Date;
  onDayClick: (date: Date) => void;
  onEventClick: (event: Event) => void;
}

function MonthView({
  year,
  month,
  daysInMonth,
  startingDayOfWeek,
  events,
  today,
  onDayClick,
  onEventClick,
}: MonthViewProps) {
  const isDayToday = (day: number) => {
    return isSameDay(new Date(year, month, day), today);
  };

  const getDayEvents = (day: number) => {
    return getEventsForDate(events, new Date(year, month, day));
  };

  return (
    <div className="grid grid-cols-7 gap-2">
      {/* Day names header */}
      {DAY_NAMES_SHORT.map((day) => (
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
        const dayEvents = getDayEvents(day);
        const isCurrentDay = isDayToday(day);

        return (
          <button
            key={day}
            onClick={() => onDayClick(new Date(year, month, day))}
            className={cn(
              'relative h-24 rounded-lg border p-2 text-left transition-all hover:shadow-md',
              isCurrentDay
                ? 'border-accent bg-accent/5'
                : 'border-gray-200 hover:border-gray-300 dark:border-gray-700 dark:hover:border-gray-600',
              dayEvents.length > 0 && 'bg-blue-50/50 dark:bg-blue-900/10',
            )}
          >
            <span
              className={cn(
                'inline-flex h-6 w-6 items-center justify-center rounded-full text-sm font-medium',
                isCurrentDay
                  ? 'bg-accent text-white'
                  : 'text-gray-700 dark:text-gray-300',
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
                      onEventClick(event);
                    }}
                    className="truncate rounded bg-accent/10 px-1 py-0.5 text-xs font-medium text-accent hover:bg-accent/20"
                  >
                    {formatTimeHHMM(new Date(event.startTime))}{' '}
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
  );
}

// ===========================================================================
// WEEK VIEW
// ===========================================================================

interface WeekViewProps {
  weekDays: Date[];
  events: Event[];
  today: Date;
  onSlotClick: (date: Date, hour: number) => void;
  onEventClick: (event: Event) => void;
}

function WeekView({ weekDays, events, today, onSlotClick, onEventClick }: WeekViewProps) {
  return (
    <div className="overflow-auto">
      {/* Header row with day names and dates */}
      <div className="grid grid-cols-[60px_repeat(7,1fr)] border-b border-gray-200 dark:border-gray-700">
        {/* Time column placeholder */}
        <div className="py-3" />
        {weekDays.map((day) => {
          const isCurrentDay = isSameDay(day, today);
          return (
            <div
              key={day.toISOString()}
              className={cn(
                'py-3 text-center border-l border-gray-200 dark:border-gray-700',
                isCurrentDay && 'bg-accent/5',
              )}
            >
              <div className="text-xs font-medium text-gray-500 dark:text-gray-400">
                {DAY_NAMES_SHORT[day.getDay()]}
              </div>
              <div
                className={cn(
                  'mx-auto mt-1 flex h-8 w-8 items-center justify-center rounded-full text-sm font-bold',
                  isCurrentDay
                    ? 'bg-accent text-white'
                    : 'text-gray-900 dark:text-gray-100',
                )}
              >
                {day.getDate()}
              </div>
            </div>
          );
        })}
      </div>

      {/* Time grid */}
      <div className="relative grid grid-cols-[60px_repeat(7,1fr)]">
        {/* Hour labels */}
        <div className="relative">
          {HOURS.map((hour) => (
            <div
              key={hour}
              className="flex items-start justify-end pr-2 text-xs text-gray-400 dark:text-gray-500"
              style={{ height: HOUR_HEIGHT }}
            >
              <span className="-mt-2">{formatHour(hour)}</span>
            </div>
          ))}
        </div>

        {/* Day columns */}
        {weekDays.map((day) => {
          const dayEvents = getEventsForDate(events, day);
          const isCurrentDay = isSameDay(day, today);

          return (
            <div
              key={day.toISOString()}
              className={cn(
                'relative border-l border-gray-200 dark:border-gray-700',
                isCurrentDay && 'bg-accent/5',
              )}
            >
              {/* Hour grid lines (clickable slots) */}
              {HOURS.map((hour) => (
                <button
                  key={hour}
                  onClick={() => onSlotClick(day, hour)}
                  className="block w-full border-b border-gray-100 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
                  style={{ height: HOUR_HEIGHT }}
                  aria-label={`Cr\u00e9er un \u00e9v\u00e9nement le ${day.getDate()} \u00e0 ${formatHour(hour)}`}
                />
              ))}

              {/* Events overlay */}
              {dayEvents.map((event) => {
                const { top, height } = computeEventPosition(event);
                const startDt = new Date(event.startTime);
                const endDt = new Date(event.endTime);
                return (
                  <button
                    key={event.id}
                    onClick={() => onEventClick(event)}
                    className="absolute left-1 right-1 rounded-md bg-accent/80 px-2 py-1 text-left text-xs text-white shadow-sm hover:bg-accent transition-colors overflow-hidden z-10"
                    style={{
                      top: `${top}px`,
                      height: `${height}px`,
                      minHeight: '20px',
                    }}
                    title={event.title}
                  >
                    <div className="font-medium truncate">{event.title}</div>
                    {height >= 30 && (
                      <div className="text-white/80 truncate">
                        {formatTimeHHMM(startDt)} - {formatTimeHHMM(endDt)}
                      </div>
                    )}
                  </button>
                );
              })}
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ===========================================================================
// DAY VIEW
// ===========================================================================

interface DayViewProps {
  date: Date;
  events: Event[];
  today: Date;
  onSlotClick: (hour: number) => void;
  onEventClick: (event: Event) => void;
}

function DayView({ date, events, today, onSlotClick, onEventClick }: DayViewProps) {
  const dayEvents = useMemo(() => getEventsForDate(events, date), [events, date]);
  const isCurrentDay = isSameDay(date, today);

  return (
    <div className="overflow-auto">
      {/* Day header */}
      <div
        className={cn(
          'mb-4 rounded-lg px-4 py-3',
          isCurrentDay
            ? 'bg-accent/10 border border-accent/20'
            : 'bg-gray-50 dark:bg-gray-800/50',
        )}
      >
        <div className="text-sm font-medium text-gray-500 dark:text-gray-400">
          {DAY_NAMES_FULL[date.getDay()]}
        </div>
        <div
          className={cn(
            'text-2xl font-bold',
            isCurrentDay ? 'text-accent' : 'text-gray-900 dark:text-gray-100',
          )}
        >
          {date.getDate()} {MONTH_NAMES[date.getMonth()]} {date.getFullYear()}
        </div>
      </div>

      {/* Time grid */}
      <div className="relative grid grid-cols-[60px_1fr]">
        {/* Hour labels */}
        <div className="relative">
          {HOURS.map((hour) => (
            <div
              key={hour}
              className="flex items-start justify-end pr-2 text-xs text-gray-400 dark:text-gray-500"
              style={{ height: HOUR_HEIGHT }}
            >
              <span className="-mt-2">{formatHour(hour)}</span>
            </div>
          ))}
        </div>

        {/* Main column */}
        <div className={cn(
          'relative border-l border-gray-200 dark:border-gray-700',
          isCurrentDay && 'bg-accent/5',
        )}>
          {/* Hour slots */}
          {HOURS.map((hour) => (
            <button
              key={hour}
              onClick={() => onSlotClick(hour)}
              className="block w-full border-b border-gray-100 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
              style={{ height: HOUR_HEIGHT }}
              aria-label={`Cr\u00e9er un \u00e9v\u00e9nement \u00e0 ${formatHour(hour)}`}
            />
          ))}

          {/* Events overlay */}
          {dayEvents.map((event) => {
            const { top, height } = computeEventPosition(event);
            const startDt = new Date(event.startTime);
            const endDt = new Date(event.endTime);
            return (
              <button
                key={event.id}
                onClick={() => onEventClick(event)}
                className="absolute left-2 right-2 rounded-md bg-accent/80 px-3 py-2 text-left text-sm text-white shadow-sm hover:bg-accent transition-colors overflow-hidden z-10"
                style={{
                  top: `${top}px`,
                  height: `${height}px`,
                  minHeight: '24px',
                }}
                title={event.title}
              >
                <div className="font-semibold truncate">{event.title}</div>
                {height >= 40 && (
                  <div className="text-white/80 text-xs">
                    {formatTimeHHMM(startDt)} - {formatTimeHHMM(endDt)}
                  </div>
                )}
                {height >= 60 && event.description && (
                  <div className="mt-1 text-white/70 text-xs truncate">
                    {event.description}
                  </div>
                )}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}

// ===========================================================================
// CREATE EVENT MODAL
// ===========================================================================

interface CreateEventModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (data: CreateEventRequest) => void;
  initialDate: Date | null;
  initialTime: string | null;
}

function CreateEventModal({ isOpen, onClose, onCreate, initialDate, initialTime }: CreateEventModalProps) {
  const defaultDateStr = initialDate
    ? formatDateISO(initialDate)
    : formatDateISO(new Date());
  const defaultStartTime = initialTime ?? '09:00';
  const defaultEndHour = initialTime
    ? `${(parseInt(initialTime.split(':')[0], 10) + 1).toString().padStart(2, '0')}:00`
    : '10:00';

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [startDate, setStartDate] = useState(defaultDateStr);
  const [startTime, setStartTime] = useState(defaultStartTime);
  const [endDate, setEndDate] = useState(defaultDateStr);
  const [endTime, setEndTime] = useState(defaultEndHour);
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
    <Modal isOpen={isOpen} onClose={onClose} title="Nouvel \u00e9v\u00e9nement">
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          label="Titre"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="Ex: R\u00e9union d'\u00e9quipe"
          required
        />

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Description
          </label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="D\u00e9tails de l'\u00e9v\u00e9nement..."
            rows={3}
            className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 placeholder-gray-400 focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent dark:border-gray-600 dark:bg-dark-card dark:text-gray-100 dark:placeholder-gray-500"
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <Input
            label="Date de d\u00e9but"
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
            Cr\u00e9er
          </Button>
        </div>
      </form>
    </Modal>
  );
}

// ===========================================================================
// EVENT DETAIL MODAL
// ===========================================================================

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
            {formatTimeHHMM(startTime)} -{' '}
            {formatTimeHHMM(endTime)} ({duration} min)
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

// ===========================================================================
// DAY EVENTS MODAL (used by month view when clicking a day)
// ===========================================================================

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
              Aucun \u00e9v\u00e9nement pour cette journ\u00e9e
            </p>
          </div>
        ) : (
          <div className="space-y-2">
            {events
              .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime())
              .map((event) => {
                const startDt = new Date(event.startTime);
                const endDt = new Date(event.endTime);
                const dur = Math.round(
                  (endDt.getTime() - startDt.getTime()) / (1000 * 60),
                );

                return (
                  <button
                    key={event.id}
                    onClick={() => onEventClick(event)}
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
                        {formatTimeHHMM(startDt)} - {formatTimeHHMM(endDt)}
                      </span>
                      <span>({dur} min)</span>
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
