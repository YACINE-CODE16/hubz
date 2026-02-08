import { useState, useEffect, useCallback } from 'react';
import { Sparkles, RefreshCw, ChevronDown, ChevronUp } from 'lucide-react';
import toast from 'react-hot-toast';
import Card from '../ui/Card';
import Button from '../ui/Button';
import InsightCard from './InsightCard';
import { insightService } from '../../services/insight.service';
import type { Insight } from '../../types/insight';
import { cn } from '../../lib/utils';

const DISMISSED_INSIGHTS_KEY = 'hubz_dismissed_insights';
const MAX_VISIBLE_INSIGHTS = 3;

interface InsightsPanelProps {
  refreshKey?: number;
}

export default function InsightsPanel({ refreshKey }: InsightsPanelProps) {
  const [insights, setInsights] = useState<Insight[]>([]);
  const [loading, setLoading] = useState(true);
  const [expanded, setExpanded] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  // Get dismissed insight IDs from localStorage
  const getDismissedIds = (): Set<string> => {
    try {
      const stored = localStorage.getItem(DISMISSED_INSIGHTS_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
        // Filter out old dismissed insights (older than 24 hours)
        const now = Date.now();
        const validEntries = Object.entries(parsed).filter(
          ([, timestamp]) => now - (timestamp as number) < 24 * 60 * 60 * 1000
        );
        return new Set(validEntries.map(([id]) => id));
      }
    } catch {
      // Ignore parse errors
    }
    return new Set();
  };

  // Save dismissed insight ID to localStorage
  const saveDismissedId = (id: string) => {
    try {
      const stored = localStorage.getItem(DISMISSED_INSIGHTS_KEY);
      const dismissed = stored ? JSON.parse(stored) : {};
      dismissed[id] = Date.now();
      localStorage.setItem(DISMISSED_INSIGHTS_KEY, JSON.stringify(dismissed));
    } catch {
      // Ignore storage errors
    }
  };

  const loadInsights = useCallback(async () => {
    try {
      setLoading(true);
      const data = await insightService.getInsights();
      // Filter out dismissed insights
      const dismissedIds = getDismissedIds();
      const filteredInsights = data.filter((i) => !dismissedIds.has(i.id));
      setInsights(filteredInsights);
    } catch (error) {
      console.error('Failed to load insights:', error);
      // Don't show toast on initial load failure - silently fail
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadInsights();
  }, [loadInsights, refreshKey]);

  const handleRefresh = async () => {
    setRefreshing(true);
    try {
      await loadInsights();
      toast.success('Insights mis a jour');
    } catch {
      toast.error('Erreur lors du rafraichissement');
    } finally {
      setRefreshing(false);
    }
  };

  const handleDismiss = (id: string) => {
    saveDismissedId(id);
    setInsights((prev) => prev.filter((i) => i.id !== id));
  };

  // Don't render if loading or no insights
  if (loading) {
    return (
      <Card className="p-6">
        <div className="flex items-center gap-2">
          <Sparkles className="h-5 w-5 text-accent animate-pulse" />
          <h3 className="font-semibold text-gray-900 dark:text-gray-100">
            Chargement des insights...
          </h3>
        </div>
        <div className="mt-4 space-y-3">
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className="h-20 animate-pulse rounded-xl bg-gray-100 dark:bg-gray-800"
            />
          ))}
        </div>
      </Card>
    );
  }

  if (insights.length === 0) {
    return null; // Don't show the panel if there are no insights
  }

  const visibleInsights = expanded
    ? insights
    : insights.slice(0, MAX_VISIBLE_INSIGHTS);
  const hasMore = insights.length > MAX_VISIBLE_INSIGHTS;

  return (
    <Card className="p-6">
      <div className="mb-4 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Sparkles className="h-5 w-5 text-accent" />
          <h3 className="font-semibold text-gray-900 dark:text-gray-100">
            Insights et recommandations
          </h3>
          <span className="rounded-full bg-accent/10 px-2 py-0.5 text-xs font-medium text-accent">
            {insights.length}
          </span>
        </div>
        <Button
          variant="secondary"
          onClick={handleRefresh}
          disabled={refreshing}
          className="flex items-center gap-1 text-sm"
        >
          <RefreshCw
            className={cn('h-4 w-4', refreshing && 'animate-spin')}
          />
          Actualiser
        </Button>
      </div>

      <div className="space-y-3">
        {visibleInsights.map((insight) => (
          <InsightCard
            key={insight.id}
            insight={insight}
            onDismiss={handleDismiss}
          />
        ))}
      </div>

      {hasMore && (
        <button
          onClick={() => setExpanded(!expanded)}
          className="mt-4 flex w-full items-center justify-center gap-1 rounded-lg py-2 text-sm font-medium text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-700 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-gray-300"
        >
          {expanded ? (
            <>
              Voir moins
              <ChevronUp className="h-4 w-4" />
            </>
          ) : (
            <>
              Voir {insights.length - MAX_VISIBLE_INSIGHTS} de plus
              <ChevronDown className="h-4 w-4" />
            </>
          )}
        </button>
      )}
    </Card>
  );
}
