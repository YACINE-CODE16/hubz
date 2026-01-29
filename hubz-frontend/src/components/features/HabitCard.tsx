import { useState } from 'react';
import { Edit2, Trash2, Check, X, FileText, History } from 'lucide-react';
import Card from '../ui/Card';
import LogHabitModal from './LogHabitModal';
import HabitHistoryModal from './HabitHistoryModal';
import type { Habit, HabitLog } from '../../types/habit';
import { cn } from '../../lib/utils';

interface HabitCardProps {
  habit: Habit;
  logs: HabitLog[];
  onEdit: (habit: Habit) => void;
  onDelete: (id: string) => void;
  onLogHabit: (habitId: string, data: { date: string; completed: boolean; notes?: string; duration?: number }) => Promise<void>;
}

export default function HabitCard({ habit, logs, onEdit, onDelete, onLogHabit }: HabitCardProps) {
  const [isLogModalOpen, setIsLogModalOpen] = useState(false);
  const [isHistoryModalOpen, setIsHistoryModalOpen] = useState(false);
  const [selectedDate, setSelectedDate] = useState<string>('');
  const [selectedLog, setSelectedLog] = useState<HabitLog | undefined>();

  const getLast7Days = () => {
    const days = [];
    for (let i = 6; i >= 0; i--) {
      const date = new Date();
      date.setDate(date.getDate() - i);
      days.push(date.toISOString().split('T')[0]);
    }
    return days;
  };

  const last7Days = getLast7Days();

  const getLogForDate = (date: string) => {
    return logs.find((log) => log.date === date);
  };

  const handleDayClick = (date: string) => {
    const existingLog = getLogForDate(date);
    setSelectedDate(date);
    setSelectedLog(existingLog);
    setIsLogModalOpen(true);
  };

  const handleLogSubmit = async (data: { date: string; completed: boolean; notes?: string; duration?: number }) => {
    await onLogHabit(habit.id, data);
    setIsLogModalOpen(false);
  };

  return (
    <>
      <Card className="p-4">
        <div className="mb-3 flex items-start justify-between">
          <div className="flex items-center gap-3">
            <span className="text-3xl">{habit.icon}</span>
            <div>
              <h3 className="font-semibold text-gray-900 dark:text-gray-100">{habit.name}</h3>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                {habit.frequency === 'DAILY' ? 'Quotidien' : 'Hebdomadaire'}
              </p>
            </div>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => setIsHistoryModalOpen(true)}
              className="rounded-lg p-1.5 text-gray-400 hover:bg-light-hover dark:hover:bg-dark-hover hover:text-gray-600 dark:hover:text-gray-200 transition-colors"
              title="Voir l'historique"
            >
              <History className="h-4 w-4" />
            </button>
            <button
              onClick={() => onEdit(habit)}
              className="rounded-lg p-1.5 text-gray-400 hover:bg-light-hover dark:hover:bg-dark-hover hover:text-gray-600 dark:hover:text-gray-200 transition-colors"
            >
              <Edit2 className="h-4 w-4" />
            </button>
            <button
              onClick={() => onDelete(habit.id)}
              className="rounded-lg p-1.5 text-gray-400 hover:bg-red-50 dark:hover:bg-red-900/20 hover:text-red-600 dark:hover:text-red-400 transition-colors"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          </div>
        </div>

        <div className="flex gap-1.5">
          {last7Days.map((date) => {
            const log = getLogForDate(date);
            const isCompleted = log?.completed;
            const hasNotes = log?.notes && log.notes.trim().length > 0;
            const dayLabel = new Date(date).toLocaleDateString('fr-FR', { weekday: 'short' });

            return (
              <button
                key={date}
                onClick={() => handleDayClick(date)}
                className={cn(
                  'flex flex-1 flex-col items-center gap-1 rounded-lg p-2 transition-all relative',
                  isCompleted
                    ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400'
                    : 'bg-gray-100 dark:bg-gray-800 text-gray-400 dark:text-gray-500 hover:bg-gray-200 dark:hover:bg-gray-700'
                )}
              >
                <span className="text-[10px] font-medium uppercase">{dayLabel}</span>
                {isCompleted ? (
                  <Check className="h-4 w-4" />
                ) : (
                  <X className="h-4 w-4 opacity-30" />
                )}
                {hasNotes && (
                  <FileText className="h-3 w-3 absolute top-1 right-1 opacity-60" />
                )}
              </button>
            );
          })}
        </div>
      </Card>

      <LogHabitModal
        isOpen={isLogModalOpen}
        onClose={() => setIsLogModalOpen(false)}
        habitName={habit.name}
        date={selectedDate}
        existingLog={selectedLog}
        onSubmit={handleLogSubmit}
      />

      <HabitHistoryModal
        isOpen={isHistoryModalOpen}
        onClose={() => setIsHistoryModalOpen(false)}
        habitName={habit.name}
        logs={logs}
      />
    </>
  );
}
