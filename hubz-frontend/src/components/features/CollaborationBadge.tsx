import { Users } from 'lucide-react';
import { cn } from '../../lib/utils';

interface CollaborationBadgeProps {
  count: number;
  className?: string;
  size?: 'sm' | 'md';
}

export default function CollaborationBadge({ count, className, size = 'sm' }: CollaborationBadgeProps) {
  if (count === 0) {
    return null;
  }

  return (
    <div
      className={cn(
        'inline-flex items-center gap-1 rounded-full bg-green-500/20 text-green-400 font-medium',
        size === 'sm' ? 'px-1.5 py-0.5 text-xs' : 'px-2 py-1 text-sm',
        className
      )}
      title={`${count} personne${count > 1 ? 's' : ''} en ligne`}
    >
      <Users className={cn(size === 'sm' ? 'w-3 h-3' : 'w-4 h-4')} />
      <span>{count}</span>
    </div>
  );
}
