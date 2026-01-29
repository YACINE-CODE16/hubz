import { useLocation } from 'react-router-dom';
import Card from '../../components/ui/Card';

export default function PlaceholderPage() {
  const { pathname } = useLocation();
  const section = pathname.split('/').pop() || '';

  const labels: Record<string, string> = {
    calendar: 'Calendrier',
    goals: 'Objectifs',
    habits: 'Habitudes',
    recap: 'Recap',
    dashboard: 'Dashboard',
    teams: 'Equipes',
    notes: 'Notes',
  };

  return (
    <div className="flex items-center justify-center p-12">
      <Card className="px-8 py-12 text-center">
        <p className="text-lg font-semibold text-gray-900 dark:text-gray-100">
          {labels[section] || section}
        </p>
        <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
          Cette section arrive bientot.
        </p>
      </Card>
    </div>
  );
}
