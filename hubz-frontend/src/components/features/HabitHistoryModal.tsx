import Modal from '../ui/Modal';
import { Calendar, Clock, FileText } from 'lucide-react';
import type { HabitLog } from '../../types/habit';

interface HabitHistoryModalProps {
  isOpen: boolean;
  onClose: () => void;
  habitName: string;
  logs: HabitLog[];
}

export default function HabitHistoryModal({ isOpen, onClose, habitName, logs }: HabitHistoryModalProps) {
  const sortedLogs = [...logs].sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());

  const formatDate = (dateStr: string) => {
    const d = new Date(dateStr);
    return d.toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  };

  const completedCount = logs.filter((log) => log.completed).length;
  const totalDuration = logs.reduce((sum, log) => sum + (log.duration || 0), 0);

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`Historique: ${habitName}`}>
      <div className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <div className="rounded-lg bg-green-50 dark:bg-green-900/20 p-3">
            <p className="text-xs text-green-600 dark:text-green-400 font-medium">Séances complétées</p>
            <p className="text-2xl font-bold text-green-700 dark:text-green-300">{completedCount}</p>
          </div>
          <div className="rounded-lg bg-blue-50 dark:bg-blue-900/20 p-3">
            <p className="text-xs text-blue-600 dark:text-blue-400 font-medium">Temps total</p>
            <p className="text-2xl font-bold text-blue-700 dark:text-blue-300">{totalDuration} min</p>
          </div>
        </div>

        <div className="max-h-96 overflow-y-auto space-y-2">
          {sortedLogs.length === 0 ? (
            <div className="text-center py-8 text-gray-500 dark:text-gray-400">
              <p>Aucune séance enregistrée</p>
            </div>
          ) : (
            sortedLogs.map((log) => (
              <div
                key={log.id}
                className={`rounded-lg border p-3 ${
                  log.completed
                    ? 'border-green-200 dark:border-green-800 bg-green-50/50 dark:bg-green-900/10'
                    : 'border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800'
                }`}
              >
                <div className="flex items-start justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <Calendar className="h-4 w-4 text-gray-500 dark:text-gray-400" />
                    <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                      {formatDate(log.date)}
                    </p>
                  </div>
                  <span
                    className={`text-xs font-medium px-2 py-0.5 rounded ${
                      log.completed
                        ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400'
                        : 'bg-gray-200 dark:bg-gray-700 text-gray-600 dark:text-gray-400'
                    }`}
                  >
                    {log.completed ? 'Complétée' : 'Non complétée'}
                  </span>
                </div>

                {log.duration && (
                  <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400 mb-1">
                    <Clock className="h-3.5 w-3.5" />
                    <span>{log.duration} minutes</span>
                  </div>
                )}

                {log.notes && (
                  <div className="mt-2 flex gap-2">
                    <FileText className="h-4 w-4 text-gray-400 mt-0.5 flex-shrink-0" />
                    <p className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">
                      {log.notes}
                    </p>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </Modal>
  );
}
