import { useState, useEffect, useCallback } from 'react';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  AreaChart,
  Area,
  ComposedChart,
  RadarChart,
  Radar,
  PolarGrid,
  PolarAngleAxis,
  PolarRadiusAxis,
  Treemap,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { Maximize2, X } from 'lucide-react';
import { cn } from '../../lib/utils';

// Color palette for charts
const COLORS = {
  primary: '#3B82F6',
  success: '#22C55E',
  warning: '#F59E0B',
  error: '#EF4444',
  info: '#8B5CF6',
  gray: '#6B7280',
  todo: '#9CA3AF',
  inProgress: '#3B82F6',
  done: '#22C55E',
};

const PIE_COLORS = ['#3B82F6', '#22C55E', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899'];

interface ChartContainerProps {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
  className?: string;
  fullscreenContent?: React.ReactNode;
}

export function ChartContainer({ title, subtitle, children, className, fullscreenContent }: ChartContainerProps) {
  return (
    <div
      className={cn(
        'rounded-xl border border-gray-200/50 bg-light-card p-6 dark:border-white/10 dark:bg-dark-card',
        className,
      )}
    >
      <div className="mb-4 flex items-start justify-between">
        <div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{title}</h3>
          {subtitle && (
            <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">{subtitle}</p>
          )}
        </div>
        {fullscreenContent && (
          <ChartFullscreenButton title={title} subtitle={subtitle}>
            {fullscreenContent}
          </ChartFullscreenButton>
        )}
      </div>
      {children}
    </div>
  );
}

// Line Chart Component
interface LineChartData {
  date: string;
  value: number;
  label?: string;
}

interface SimpleLineChartProps {
  data: LineChartData[];
  dataKey?: string;
  color?: string;
  height?: number;
  showGrid?: boolean;
  showTooltip?: boolean;
}

export function SimpleLineChart({
  data,
  dataKey = 'value',
  color = COLORS.primary,
  height = 300,
  showGrid = true,
  showTooltip = true,
}: SimpleLineChartProps) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <LineChart data={data}>
        {showGrid && <CartesianGrid strokeDasharray="3 3" stroke="#374151" opacity={0.3} />}
        <XAxis
          dataKey="date"
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
          tickLine={false}
          axisLine={false}
        />
        <YAxis
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
          tickLine={false}
          axisLine={false}
          width={40}
        />
        {showTooltip && (
          <Tooltip
            contentStyle={{
              backgroundColor: '#1F2937',
              border: 'none',
              borderRadius: '8px',
              color: '#F3F4F6',
            }}
          />
        )}
        <Line
          type="monotone"
          dataKey={dataKey}
          stroke={color}
          strokeWidth={2}
          dot={false}
          activeDot={{ r: 4, fill: color }}
        />
      </LineChart>
    </ResponsiveContainer>
  );
}

// Multi-Line Chart Component
interface MultiLineData {
  date: string;
  [key: string]: string | number;
}

interface MultiLineChartProps {
  data: MultiLineData[];
  lines: { key: string; color: string; name: string }[];
  height?: number;
}

export function MultiLineChart({ data, lines, height = 300 }: MultiLineChartProps) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <LineChart data={data}>
        <CartesianGrid strokeDasharray="3 3" stroke="#374151" opacity={0.3} />
        <XAxis
          dataKey="date"
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
          tickLine={false}
          axisLine={false}
        />
        <YAxis
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
          tickLine={false}
          axisLine={false}
          width={40}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: '#1F2937',
            border: 'none',
            borderRadius: '8px',
            color: '#F3F4F6',
          }}
        />
        <Legend />
        {lines.map((line) => (
          <Line
            key={line.key}
            type="monotone"
            dataKey={line.key}
            stroke={line.color}
            strokeWidth={2}
            dot={false}
            name={line.name}
          />
        ))}
      </LineChart>
    </ResponsiveContainer>
  );
}

// Bar Chart Component
interface BarChartData {
  name: string;
  value: number;
  color?: string;
}

interface SimpleBarChartProps {
  data: BarChartData[];
  color?: string;
  height?: number;
  horizontal?: boolean;
}

export function SimpleBarChart({
  data,
  color = COLORS.primary,
  height = 300,
  horizontal = false,
}: SimpleBarChartProps) {
  if (horizontal) {
    return (
      <ResponsiveContainer width="100%" height={height}>
        <BarChart data={data} layout="vertical">
          <CartesianGrid strokeDasharray="3 3" stroke="#374151" opacity={0.3} />
          <XAxis type="number" tick={{ fill: '#9CA3AF', fontSize: 12 }} axisLine={false} />
          <YAxis
            type="category"
            dataKey="name"
            tick={{ fill: '#9CA3AF', fontSize: 12 }}
            axisLine={false}
            width={100}
          />
          <Tooltip
            contentStyle={{
              backgroundColor: '#1F2937',
              border: 'none',
              borderRadius: '8px',
              color: '#F3F4F6',
            }}
          />
          <Bar dataKey="value" fill={color} radius={[0, 4, 4, 0]} />
        </BarChart>
      </ResponsiveContainer>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={height}>
      <BarChart data={data}>
        <CartesianGrid strokeDasharray="3 3" stroke="#374151" opacity={0.3} />
        <XAxis
          dataKey="name"
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
          tickLine={false}
          axisLine={false}
        />
        <YAxis
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
          tickLine={false}
          axisLine={false}
          width={40}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: '#1F2937',
            border: 'none',
            borderRadius: '8px',
            color: '#F3F4F6',
          }}
        />
        <Bar dataKey="value" fill={color} radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}

// Pie Chart Component
interface PieChartData {
  name: string;
  value: number;
}

interface SimplePieChartProps {
  data: PieChartData[];
  height?: number;
  showLegend?: boolean;
  innerRadius?: number;
}

export function SimplePieChart({
  data,
  height = 300,
  showLegend = true,
  innerRadius = 0,
}: SimplePieChartProps) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <PieChart>
        <Pie
          data={data}
          cx="50%"
          cy="50%"
          innerRadius={innerRadius}
          outerRadius={80}
          paddingAngle={2}
          dataKey="value"
          label={({ name, percent }) => `${name} (${((percent ?? 0) * 100).toFixed(0)}%)`}
          labelLine={false}
        >
          {data.map((_, index) => (
            <Cell key={`cell-${index}`} fill={PIE_COLORS[index % PIE_COLORS.length]} />
          ))}
        </Pie>
        <Tooltip
          contentStyle={{
            backgroundColor: '#1F2937',
            border: 'none',
            borderRadius: '8px',
            color: '#F3F4F6',
          }}
        />
        {showLegend && <Legend />}
      </PieChart>
    </ResponsiveContainer>
  );
}

// Area Chart Component (for Cumulative Flow Diagram)
interface StackedAreaChartProps {
  data: { date: string; [key: string]: string | number }[];
  areas: { key: string; color: string; name: string }[];
  height?: number;
}

export function StackedAreaChart({ data, areas, height = 300 }: StackedAreaChartProps) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <AreaChart data={data}>
        <CartesianGrid strokeDasharray="3 3" stroke="#374151" opacity={0.3} />
        <XAxis
          dataKey="date"
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
          tickLine={false}
          axisLine={false}
        />
        <YAxis
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
          tickLine={false}
          axisLine={false}
          width={40}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: '#1F2937',
            border: 'none',
            borderRadius: '8px',
            color: '#F3F4F6',
          }}
        />
        <Legend />
        {areas.map((area) => (
          <Area
            key={area.key}
            type="monotone"
            dataKey={area.key}
            stackId="1"
            stroke={area.color}
            fill={area.color}
            fillOpacity={0.6}
            name={area.name}
          />
        ))}
      </AreaChart>
    </ResponsiveContainer>
  );
}

// Velocity Chart (Bar chart for weekly velocity)
interface VelocityChartProps {
  data: { weekStart: string; completedTasks: number }[];
  height?: number;
}

export function VelocityChart({ data, height = 300 }: VelocityChartProps) {
  const chartData = data.map((d) => ({
    name: d.weekStart.slice(5), // Show MM-DD
    value: d.completedTasks,
  }));

  return (
    <ResponsiveContainer width="100%" height={height}>
      <BarChart data={chartData}>
        <CartesianGrid strokeDasharray="3 3" stroke="#374151" opacity={0.3} />
        <XAxis
          dataKey="name"
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
          tickLine={false}
          axisLine={false}
        />
        <YAxis
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
          tickLine={false}
          axisLine={false}
          width={40}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: '#1F2937',
            border: 'none',
            borderRadius: '8px',
            color: '#F3F4F6',
          }}
          formatter={(value) => [`${value} taches`, 'Completees']}
        />
        <Bar dataKey="value" fill={COLORS.success} radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}

// Burndown Chart
interface BurndownChartProps {
  data: { date: string; remainingTasks: number; completedTasks: number; totalTasks: number }[];
  height?: number;
}

export function BurndownChart({ data, height = 300 }: BurndownChartProps) {
  const chartData = data.map((d) => ({
    date: d.date.slice(5), // Show MM-DD
    remaining: d.remainingTasks,
    ideal:
      data.length > 0
        ? Math.max(0, data[0].totalTasks - (data[0].totalTasks / data.length) * data.indexOf(d))
        : 0,
  }));

  return (
    <ResponsiveContainer width="100%" height={height}>
      <LineChart data={chartData}>
        <CartesianGrid strokeDasharray="3 3" stroke="#374151" opacity={0.3} />
        <XAxis
          dataKey="date"
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
          tickLine={false}
          axisLine={false}
        />
        <YAxis
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
          tickLine={false}
          axisLine={false}
          width={40}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: '#1F2937',
            border: 'none',
            borderRadius: '8px',
            color: '#F3F4F6',
          }}
        />
        <Legend />
        <Line
          type="monotone"
          dataKey="remaining"
          stroke={COLORS.primary}
          strokeWidth={2}
          dot={false}
          name="Restantes"
        />
        <Line
          type="monotone"
          dataKey="ideal"
          stroke={COLORS.gray}
          strokeWidth={2}
          strokeDasharray="5 5"
          dot={false}
          name="Ideal"
        />
      </LineChart>
    </ResponsiveContainer>
  );
}

// Progress Gauge Component
interface ProgressGaugeProps {
  value: number;
  max?: number;
  label: string;
  color?: string;
  size?: 'sm' | 'md' | 'lg';
}

export function ProgressGauge({
  value,
  max = 100,
  label,
  color = COLORS.primary,
  size = 'md',
}: ProgressGaugeProps) {
  const percentage = Math.min(100, (value / max) * 100);
  const sizeClasses = {
    sm: 'h-16 w-16 text-lg',
    md: 'h-24 w-24 text-2xl',
    lg: 'h-32 w-32 text-3xl',
  };

  return (
    <div className="flex flex-col items-center">
      <div
        className={cn(
          'relative flex items-center justify-center rounded-full',
          sizeClasses[size],
        )}
        style={{
          background: `conic-gradient(${color} ${percentage * 3.6}deg, #374151 ${percentage * 3.6}deg)`,
        }}
      >
        <div className="absolute flex h-[85%] w-[85%] items-center justify-center rounded-full bg-light-card dark:bg-dark-card">
          <span className="font-bold text-gray-900 dark:text-gray-100">{Math.round(value)}%</span>
        </div>
      </div>
      <span className="mt-2 text-sm font-medium text-gray-600 dark:text-gray-400">{label}</span>
    </div>
  );
}

// Stat Card Component
interface StatCardProps {
  title: string;
  value: number | string;
  subtitle?: string;
  trend?: number;
  icon?: React.ReactNode;
  color?: 'blue' | 'green' | 'red' | 'yellow' | 'purple' | 'gray';
}

export function StatCard({ title, value, subtitle, trend, icon, color = 'blue' }: StatCardProps) {
  const colorClasses = {
    blue: 'bg-blue-100 text-blue-600 dark:bg-blue-900/40 dark:text-blue-400',
    green: 'bg-green-100 text-green-600 dark:bg-green-900/40 dark:text-green-400',
    red: 'bg-red-100 text-red-600 dark:bg-red-900/40 dark:text-red-400',
    yellow: 'bg-yellow-100 text-yellow-600 dark:bg-yellow-900/40 dark:text-yellow-400',
    purple: 'bg-purple-100 text-purple-600 dark:bg-purple-900/40 dark:text-purple-400',
    gray: 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400',
  };

  return (
    <div className="rounded-xl border border-gray-200/50 bg-light-card p-6 dark:border-white/10 dark:bg-dark-card">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm font-medium text-gray-500 dark:text-gray-400">{title}</p>
          <p className="mt-2 text-3xl font-bold text-gray-900 dark:text-gray-100">{value}</p>
          {subtitle && (
            <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">{subtitle}</p>
          )}
          {trend !== undefined && (
            <p
              className={cn(
                'mt-2 text-sm font-medium',
                trend >= 0 ? 'text-green-600' : 'text-red-600',
              )}
            >
              {trend >= 0 ? '+' : ''}
              {trend.toFixed(1)}% vs semaine derniere
            </p>
          )}
        </div>
        {icon && <div className={cn('rounded-full p-3', colorClasses[color])}>{icon}</div>}
      </div>
    </div>
  );
}

// Burnup Chart (line chart with 2 lines: cumulative completed and total scope)
interface BurnupChartProps {
  data: { date: string; cumulativeCompleted: number; totalScope: number }[];
  height?: number;
}

export function BurnupChart({ data, height = 300 }: BurnupChartProps) {
  const chartData = data.map((d) => ({
    date: d.date.slice(5), // Show MM-DD
    completed: d.cumulativeCompleted,
    scope: d.totalScope,
  }));

  return (
    <ResponsiveContainer width="100%" height={height}>
      <LineChart data={chartData}>
        <CartesianGrid strokeDasharray="3 3" stroke="#374151" opacity={0.3} />
        <XAxis
          dataKey="date"
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
          tickLine={false}
          axisLine={false}
        />
        <YAxis
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
          tickLine={false}
          axisLine={false}
          width={40}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: '#1F2937',
            border: 'none',
            borderRadius: '8px',
            color: '#F3F4F6',
          }}
        />
        <Legend />
        <Line
          type="monotone"
          dataKey="completed"
          stroke={COLORS.success}
          strokeWidth={2}
          dot={false}
          name="Completees"
        />
        <Line
          type="monotone"
          dataKey="scope"
          stroke={COLORS.primary}
          strokeWidth={2}
          strokeDasharray="5 5"
          dot={false}
          name="Scope total"
        />
      </LineChart>
    </ResponsiveContainer>
  );
}

// Throughput Chart (bar chart with trend line using ComposedChart)
interface ThroughputChartProps {
  data: { date: string; completedCount: number; rollingAverage: number | null }[];
  height?: number;
}

export function ThroughputChart({ data, height = 300 }: ThroughputChartProps) {
  const chartData = data.map((d) => ({
    date: d.date.slice(5), // Show MM-DD
    completed: d.completedCount,
    average: d.rollingAverage,
  }));

  return (
    <ResponsiveContainer width="100%" height={height}>
      <ComposedChart data={chartData}>
        <CartesianGrid strokeDasharray="3 3" stroke="#374151" opacity={0.3} />
        <XAxis
          dataKey="date"
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
          tickLine={false}
          axisLine={false}
        />
        <YAxis
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
          tickLine={false}
          axisLine={false}
          width={40}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: '#1F2937',
            border: 'none',
            borderRadius: '8px',
            color: '#F3F4F6',
          }}
        />
        <Legend />
        <Bar dataKey="completed" fill={COLORS.success} radius={[4, 4, 0, 0]} name="Completees" />
        <Line
          type="monotone"
          dataKey="average"
          stroke={COLORS.warning}
          strokeWidth={2}
          dot={false}
          name="Moyenne 7j"
        />
      </ComposedChart>
    </ResponsiveContainer>
  );
}

// Cycle Time Histogram
interface CycleTimeHistogramProps {
  data: { bucket: string; count: number; percentage: number }[];
  height?: number;
}

export function CycleTimeHistogram({ data, height = 300 }: CycleTimeHistogramProps) {
  const chartData = data.map((d) => ({
    name: d.bucket,
    value: d.count,
    percentage: d.percentage,
  }));

  return (
    <ResponsiveContainer width="100%" height={height}>
      <BarChart data={chartData}>
        <CartesianGrid strokeDasharray="3 3" stroke="#374151" opacity={0.3} />
        <XAxis
          dataKey="name"
          tick={{ fill: '#9CA3AF', fontSize: 11 }}
          tickLine={false}
          axisLine={false}
        />
        <YAxis
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
          tickLine={false}
          axisLine={false}
          width={40}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: '#1F2937',
            border: 'none',
            borderRadius: '8px',
            color: '#F3F4F6',
          }}
          formatter={(value, name) => {
            if (name === 'value') {
              const item = chartData.find((d) => d.value === value);
              return [`${value} taches (${item?.percentage || 0}%)`, 'Nombre'];
            }
            return [value, name];
          }}
        />
        <Bar dataKey="value" fill={COLORS.info} radius={[4, 4, 0, 0]} name="Nombre de taches" />
      </BarChart>
    </ResponsiveContainer>
  );
}

// Lead Time Trend Chart
interface LeadTimeTrendChartProps {
  data: { date: string; averageLeadTimeHours: number | null; taskCount: number }[];
  height?: number;
}

export function LeadTimeTrendChart({ data, height = 300 }: LeadTimeTrendChartProps) {
  const chartData = data.map((d) => ({
    date: d.date.slice(5), // Show MM-DD
    leadTime: d.averageLeadTimeHours ? Math.round(d.averageLeadTimeHours / 24 * 10) / 10 : null, // Convert to days
    tasks: d.taskCount,
  }));

  return (
    <ResponsiveContainer width="100%" height={height}>
      <LineChart data={chartData}>
        <CartesianGrid strokeDasharray="3 3" stroke="#374151" opacity={0.3} />
        <XAxis
          dataKey="date"
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
          tickLine={false}
          axisLine={false}
        />
        <YAxis
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
          tickLine={false}
          axisLine={false}
          width={40}
          label={{ value: 'Jours', angle: -90, position: 'insideLeft', fill: '#9CA3AF', fontSize: 12 }}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: '#1F2937',
            border: 'none',
            borderRadius: '8px',
            color: '#F3F4F6',
          }}
          formatter={(value: number | null, name: string) => {
            if (name === 'leadTime' && value !== null) {
              return [`${value} jours`, 'Lead time moyen'];
            }
            if (name === 'tasks') {
              return [`${value} taches`, 'Taches completees'];
            }
            return [value, name];
          }}
        />
        <Legend />
        <Line
          type="monotone"
          dataKey="leadTime"
          stroke={COLORS.primary}
          strokeWidth={2}
          dot={{ r: 4, fill: COLORS.primary }}
          connectNulls
          name="Lead time (jours)"
        />
      </LineChart>
    </ResponsiveContainer>
  );
}

// WIP Chart (Area chart)
interface WIPChartProps {
  data: { date: string; wipCount: number; todoCount: number; totalActive: number }[];
  height?: number;
}

export function WIPChart({ data, height = 300 }: WIPChartProps) {
  const chartData = data.map((d) => ({
    date: d.date.slice(5), // Show MM-DD
    wip: d.wipCount,
    todo: d.todoCount,
    total: d.totalActive,
  }));

  return (
    <ResponsiveContainer width="100%" height={height}>
      <AreaChart data={chartData}>
        <CartesianGrid strokeDasharray="3 3" stroke="#374151" opacity={0.3} />
        <XAxis
          dataKey="date"
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
          tickLine={false}
          axisLine={false}
        />
        <YAxis
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
          tickLine={false}
          axisLine={false}
          width={40}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: '#1F2937',
            border: 'none',
            borderRadius: '8px',
            color: '#F3F4F6',
          }}
        />
        <Legend />
        <Area
          type="monotone"
          dataKey="wip"
          stackId="1"
          stroke={COLORS.inProgress}
          fill={COLORS.inProgress}
          fillOpacity={0.6}
          name="En cours"
        />
        <Area
          type="monotone"
          dataKey="todo"
          stackId="1"
          stroke={COLORS.todo}
          fill={COLORS.todo}
          fillOpacity={0.6}
          name="A faire"
        />
      </AreaChart>
    </ResponsiveContainer>
  );
}

// ===== Fullscreen Chart Modal =====

interface ChartFullscreenButtonProps {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
}

function ChartFullscreenButton({ title, subtitle, children }: ChartFullscreenButtonProps) {
  const [isOpen, setIsOpen] = useState(false);

  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        setIsOpen(false);
      }
    },
    [isOpen],
  );

  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen]);

  return (
    <>
      <button
        onClick={() => setIsOpen(true)}
        className="rounded-lg p-1.5 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600 dark:hover:bg-gray-800 dark:hover:text-gray-300"
        title="Plein ecran"
      >
        <Maximize2 className="h-4 w-4" />
      </button>

      {isOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
          onClick={(e) => {
            if (e.target === e.currentTarget) setIsOpen(false);
          }}
        >
          <div className="relative mx-4 flex h-[90vh] w-full max-w-6xl flex-col rounded-2xl border border-gray-200/50 bg-light-card p-8 shadow-2xl dark:border-white/10 dark:bg-dark-card">
            {/* Header */}
            <div className="mb-6 flex items-start justify-between">
              <div>
                <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{title}</h2>
                {subtitle && (
                  <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">{subtitle}</p>
                )}
              </div>
              <button
                onClick={() => setIsOpen(false)}
                className="rounded-lg p-2 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600 dark:hover:bg-gray-800 dark:hover:text-gray-300"
                title="Fermer"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {/* Chart content */}
            <div className="flex-1 overflow-auto">
              {children}
            </div>
          </div>
        </div>
      )}
    </>
  );
}

export function ChartFullscreenModal({
  isOpen,
  onClose,
  title,
  subtitle,
  children,
}: {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  subtitle?: string;
  children: React.ReactNode;
}) {
  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    },
    [onClose],
  );

  useEffect(() => {
    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown);
      document.body.style.overflow = 'hidden';
    }
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [isOpen, handleKeyDown]);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="relative mx-4 flex h-[90vh] w-full max-w-6xl flex-col rounded-2xl border border-gray-200/50 bg-light-card p-8 shadow-2xl dark:border-white/10 dark:bg-dark-card">
        <div className="mb-6 flex items-start justify-between">
          <div>
            <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{title}</h2>
            {subtitle && (
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">{subtitle}</p>
            )}
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-2 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600 dark:hover:bg-gray-800 dark:hover:text-gray-300"
            title="Fermer (Echap)"
          >
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="flex-1 overflow-auto">{children}</div>
      </div>
    </div>
  );
}

// ===== Radar Chart Component =====

interface RadarChartData {
  subject: string;
  value: number;
  fullMark?: number;
}

interface SimpleRadarChartProps {
  data: RadarChartData[];
  height?: number;
  color?: string;
  fillOpacity?: number;
}

export function SimpleRadarChart({
  data,
  height = 350,
  color = COLORS.primary,
  fillOpacity = 0.3,
}: SimpleRadarChartProps) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <RadarChart cx="50%" cy="50%" outerRadius="75%" data={data}>
        <PolarGrid stroke="#374151" strokeOpacity={0.5} />
        <PolarAngleAxis
          dataKey="subject"
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
        />
        <PolarRadiusAxis
          angle={30}
          domain={[0, 100]}
          tick={{ fill: '#9CA3AF', fontSize: 10 }}
          tickCount={5}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: '#1F2937',
            border: 'none',
            borderRadius: '8px',
            color: '#F3F4F6',
          }}
          formatter={(value: number) => [`${value}%`, 'Taux de completion']}
        />
        <Radar
          name="Completion"
          dataKey="value"
          stroke={color}
          fill={color}
          fillOpacity={fillOpacity}
          strokeWidth={2}
        />
      </RadarChart>
    </ResponsiveContainer>
  );
}

// Multi-data Radar Chart (for comparison - e.g., this week vs last week)
interface ComparisonRadarChartProps {
  data: RadarChartData[];
  comparisonData?: RadarChartData[];
  height?: number;
  color?: string;
  comparisonColor?: string;
  labels?: { current: string; comparison: string };
}

export function ComparisonRadarChart({
  data,
  comparisonData,
  height = 350,
  color = COLORS.primary,
  comparisonColor = COLORS.warning,
  labels = { current: 'Cette semaine', comparison: 'Semaine derniere' },
}: ComparisonRadarChartProps) {
  // Merge data - both datasets must have the same subjects
  const mergedData = data.map((item) => {
    const comparison = comparisonData?.find((c) => c.subject === item.subject);
    return {
      subject: item.subject,
      current: item.value,
      comparison: comparison?.value ?? 0,
      fullMark: item.fullMark ?? 100,
    };
  });

  return (
    <ResponsiveContainer width="100%" height={height}>
      <RadarChart cx="50%" cy="50%" outerRadius="75%" data={mergedData}>
        <PolarGrid stroke="#374151" strokeOpacity={0.5} />
        <PolarAngleAxis
          dataKey="subject"
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
        />
        <PolarRadiusAxis
          angle={30}
          domain={[0, 100]}
          tick={{ fill: '#9CA3AF', fontSize: 10 }}
          tickCount={5}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: '#1F2937',
            border: 'none',
            borderRadius: '8px',
            color: '#F3F4F6',
          }}
          formatter={(value: number) => [`${value}%`]}
        />
        <Legend />
        <Radar
          name={labels.current}
          dataKey="current"
          stroke={color}
          fill={color}
          fillOpacity={0.3}
          strokeWidth={2}
        />
        {comparisonData && (
          <Radar
            name={labels.comparison}
            dataKey="comparison"
            stroke={comparisonColor}
            fill={comparisonColor}
            fillOpacity={0.15}
            strokeWidth={2}
            strokeDasharray="5 5"
          />
        )}
      </RadarChart>
    </ResponsiveContainer>
  );
}

// ===== Treemap Chart Component =====

interface TreemapDataItem {
  name: string;
  value: number;
  color?: string;
  children?: TreemapDataItem[];
}

interface SimpleTreemapProps {
  data: TreemapDataItem[];
  height?: number;
}

// Custom content renderer for Treemap cells
// eslint-disable-next-line @typescript-eslint/no-explicit-any
function TreemapContent(props: any) {
  const { x, y, width, height: h, name, size, index, color } = props;
  const fillColor = color || PIE_COLORS[(index || 0) % PIE_COLORS.length];
  const showText = width > 50 && h > 30;
  const showValue = width > 70 && h > 45;

  return (
    <g>
      <rect
        x={x}
        y={y}
        width={width}
        height={h}
        fill={fillColor}
        fillOpacity={0.85}
        stroke="#1F2937"
        strokeWidth={2}
        rx={4}
        ry={4}
      />
      {showText && (
        <text
          x={x + width / 2}
          y={y + h / 2 - (showValue ? 8 : 0)}
          textAnchor="middle"
          dominantBaseline="middle"
          fill="#FFFFFF"
          fontSize={width > 100 ? 13 : 11}
          fontWeight={600}
        >
          {name && name.length > 12 ? name.slice(0, 11) + '...' : name}
        </text>
      )}
      {showValue && (
        <text
          x={x + width / 2}
          y={y + h / 2 + 12}
          textAnchor="middle"
          dominantBaseline="middle"
          fill="#FFFFFFCC"
          fontSize={11}
        >
          {size}
        </text>
      )}
    </g>
  );
}

export function SimpleTreemap({ data, height = 350 }: SimpleTreemapProps) {
  // Recharts Treemap requires the data in a specific format
  const treemapData = data.map((item, index) => ({
    name: item.name,
    size: item.value,
    color: item.color || PIE_COLORS[index % PIE_COLORS.length],
  }));

  return (
    <ResponsiveContainer width="100%" height={height}>
      <Treemap
        data={treemapData}
        dataKey="size"
        nameKey="name"
        content={<TreemapContent />}
      >
        <Tooltip
          contentStyle={{
            backgroundColor: '#1F2937',
            border: 'none',
            borderRadius: '8px',
            color: '#F3F4F6',
          }}
          formatter={(value: number) => [value, 'Taches']}
        />
      </Treemap>
    </ResponsiveContainer>
  );
}

// ===== Period Comparison Indicator =====

interface PeriodComparisonProps {
  currentValue: number;
  previousValue: number;
  label?: string;
  format?: 'number' | 'percentage' | 'hours';
  invertColors?: boolean;
}

export function PeriodComparisonIndicator({
  currentValue,
  previousValue,
  label = 'vs semaine derniere',
  format = 'number',
  invertColors = false,
}: PeriodComparisonProps) {
  const diff = currentValue - previousValue;
  const percentChange = previousValue > 0
    ? Math.round((diff / previousValue) * 100)
    : currentValue > 0 ? 100 : 0;

  const isPositive = invertColors ? diff < 0 : diff > 0;
  const isNeutral = diff === 0;

  const formatValue = (val: number) => {
    switch (format) {
      case 'percentage':
        return `${val}%`;
      case 'hours':
        return `${val}h`;
      default:
        return val.toString();
    }
  };

  return (
    <div className="flex items-center gap-3 rounded-lg border border-gray-200/50 bg-gray-50 p-3 dark:border-white/5 dark:bg-gray-800/50">
      <div className="flex-1">
        <div className="flex items-baseline gap-2">
          <span className="text-lg font-bold text-gray-900 dark:text-gray-100">
            {formatValue(currentValue)}
          </span>
          <span className="text-sm text-gray-400">
            vs {formatValue(previousValue)}
          </span>
        </div>
        <p className="text-xs text-gray-500 dark:text-gray-400">{label}</p>
      </div>
      <div
        className={cn(
          'flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-semibold',
          isNeutral
            ? 'bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-400'
            : isPositive
              ? 'bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-400'
              : 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-400',
        )}
      >
        {isNeutral ? (
          '='
        ) : (
          <>
            {diff > 0 ? '+' : ''}
            {percentChange}%
          </>
        )}
      </div>
    </div>
  );
}

export { COLORS, PIE_COLORS };
