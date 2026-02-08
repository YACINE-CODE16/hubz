import { ChartContainer, SimpleTreemap, PIE_COLORS } from '../ui/Charts';
import type { MemberAnalytics } from '../../types/analytics';

interface TreemapCardProps {
  memberAnalytics: MemberAnalytics;
}

export default function TreemapCard({ memberAnalytics }: TreemapCardProps) {
  // Build treemap data from member workload
  const treemapData = memberAnalytics.memberWorkload
    .filter((member) => member.activeTasks > 0)
    .sort((a, b) => b.activeTasks - a.activeTasks)
    .map((member, index) => ({
      name: member.memberName.split(' ')[0], // First name for brevity
      value: member.activeTasks,
      color: PIE_COLORS[index % PIE_COLORS.length],
    }));

  if (treemapData.length === 0) {
    return (
      <ChartContainer
        title="Repartition des taches par membre"
        subtitle="Distribution hierarchique des taches actives"
      >
        <div className="flex h-48 items-center justify-center text-sm text-gray-500 dark:text-gray-400">
          Aucune tache active a afficher
        </div>
      </ChartContainer>
    );
  }

  const fullscreenChart = <SimpleTreemap data={treemapData} height={500} />;

  return (
    <ChartContainer
      title="Repartition des taches par membre"
      subtitle="Distribution hierarchique des taches actives"
      fullscreenContent={fullscreenChart}
    >
      <SimpleTreemap data={treemapData} height={350} />

      {/* Legend */}
      <div className="mt-4 grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-4">
        {treemapData.map((item, index) => (
          <div
            key={index}
            className="flex items-center gap-2 text-xs"
          >
            <div
              className="h-3 w-3 rounded"
              style={{ backgroundColor: item.color }}
            />
            <span className="text-gray-600 dark:text-gray-400">
              {item.name}
            </span>
            <span className="font-medium text-gray-900 dark:text-gray-100">
              {item.value}
            </span>
          </div>
        ))}
      </div>
    </ChartContainer>
  );
}
