import { useNavigate } from 'react-router-dom';
import {
  Lightbulb,
  Flame,
  Target,
  AlertTriangle,
  Trophy,
  TrendingUp,
  X,
  ArrowRight,
} from 'lucide-react';
import type { Insight, InsightType } from '../../types/insight';
import { cn } from '../../lib/utils';

interface InsightCardProps {
  insight: Insight;
  onDismiss: (id: string) => void;
}

const insightConfig: Record<
  InsightType,
  {
    icon: React.ElementType;
    bgColor: string;
    iconColor: string;
    borderColor: string;
    accentColor: string;
  }
> = {
  PRODUCTIVITY_TIP: {
    icon: Lightbulb,
    bgColor: 'bg-blue-50 dark:bg-blue-900/20',
    iconColor: 'text-blue-500',
    borderColor: 'border-blue-200 dark:border-blue-800',
    accentColor: 'text-blue-600 dark:text-blue-400',
  },
  HABIT_SUGGESTION: {
    icon: Flame,
    bgColor: 'bg-orange-50 dark:bg-orange-900/20',
    iconColor: 'text-orange-500',
    borderColor: 'border-orange-200 dark:border-orange-800',
    accentColor: 'text-orange-600 dark:text-orange-400',
  },
  GOAL_ALERT: {
    icon: Target,
    bgColor: 'bg-red-50 dark:bg-red-900/20',
    iconColor: 'text-red-500',
    borderColor: 'border-red-200 dark:border-red-800',
    accentColor: 'text-red-600 dark:text-red-400',
  },
  WORKLOAD_WARNING: {
    icon: AlertTriangle,
    bgColor: 'bg-amber-50 dark:bg-amber-900/20',
    iconColor: 'text-amber-500',
    borderColor: 'border-amber-200 dark:border-amber-800',
    accentColor: 'text-amber-600 dark:text-amber-400',
  },
  CELEBRATION: {
    icon: Trophy,
    bgColor: 'bg-green-50 dark:bg-green-900/20',
    iconColor: 'text-green-500',
    borderColor: 'border-green-200 dark:border-green-800',
    accentColor: 'text-green-600 dark:text-green-400',
  },
  PATTERN_DETECTED: {
    icon: TrendingUp,
    bgColor: 'bg-purple-50 dark:bg-purple-900/20',
    iconColor: 'text-purple-500',
    borderColor: 'border-purple-200 dark:border-purple-800',
    accentColor: 'text-purple-600 dark:text-purple-400',
  },
};

export default function InsightCard({ insight, onDismiss }: InsightCardProps) {
  const navigate = useNavigate();
  const config = insightConfig[insight.type];
  const Icon = config.icon;

  const handleClick = () => {
    if (insight.actionable && insight.actionUrl) {
      navigate(insight.actionUrl);
    }
  };

  return (
    <div
      className={cn(
        'group relative rounded-xl border p-4 transition-all duration-200',
        config.bgColor,
        config.borderColor,
        insight.actionable && 'cursor-pointer hover:shadow-md hover:scale-[1.01]'
      )}
      onClick={insight.actionable ? handleClick : undefined}
    >
      {/* Dismiss button */}
      <button
        onClick={(e) => {
          e.stopPropagation();
          onDismiss(insight.id);
        }}
        className="absolute right-2 top-2 rounded-full p-1 text-gray-400 opacity-0 transition-opacity hover:bg-gray-200 hover:text-gray-600 group-hover:opacity-100 dark:hover:bg-gray-700 dark:hover:text-gray-300"
        title="Ignorer"
      >
        <X className="h-4 w-4" />
      </button>

      <div className="flex gap-3">
        {/* Icon */}
        <div
          className={cn(
            'flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full',
            config.bgColor,
            config.iconColor
          )}
        >
          <Icon className="h-5 w-5" />
        </div>

        {/* Content */}
        <div className="flex-1 min-w-0 pr-6">
          <div className="flex items-center gap-2">
            <h4 className={cn('font-semibold text-sm', config.accentColor)}>
              {insight.title}
            </h4>
            {insight.priority >= 4 && (
              <span className="rounded-full bg-red-100 px-1.5 py-0.5 text-[10px] font-medium text-red-600 dark:bg-red-900/30 dark:text-red-400">
                Important
              </span>
            )}
          </div>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
            {insight.message}
          </p>
          {insight.actionable && (
            <div className={cn('mt-2 flex items-center gap-1 text-xs font-medium', config.accentColor)}>
              <span>Voir</span>
              <ArrowRight className="h-3 w-3" />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
