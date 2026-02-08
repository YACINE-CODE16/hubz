import { X } from 'lucide-react';
import { cn } from '../../lib/utils';
import type { Tag } from '../../types/tag';

interface TagChipProps {
  tag: Tag;
  size?: 'sm' | 'md';
  onRemove?: () => void;
  onClick?: () => void;
  className?: string;
}

function getContrastColor(hexColor: string): string {
  // Remove the # if present
  const hex = hexColor.replace('#', '');

  // Convert to RGB
  const r = parseInt(hex.substring(0, 2), 16);
  const g = parseInt(hex.substring(2, 4), 16);
  const b = parseInt(hex.substring(4, 6), 16);

  // Calculate luminance
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;

  // Return black or white based on luminance
  return luminance > 0.5 ? '#000000' : '#FFFFFF';
}

export default function TagChip({ tag, size = 'sm', onRemove, onClick, className }: TagChipProps) {
  const textColor = getContrastColor(tag.color);

  const sizeClasses = {
    sm: 'px-2 py-0.5 text-xs',
    md: 'px-2.5 py-1 text-sm',
  };

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded-full font-medium transition-opacity',
        sizeClasses[size],
        onClick && 'cursor-pointer hover:opacity-80',
        className
      )}
      style={{ backgroundColor: tag.color, color: textColor }}
      onClick={onClick}
    >
      {tag.name}
      {onRemove && (
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onRemove();
          }}
          className="ml-0.5 rounded-full p-0.5 transition-colors hover:bg-black/20"
          aria-label={`Remove tag ${tag.name}`}
        >
          <X className="h-3 w-3" />
        </button>
      )}
    </span>
  );
}
