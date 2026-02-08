import { useNavigate } from 'react-router-dom';
import {
  Building2,
  Code,
  Briefcase,
  Rocket,
  BookOpen,
  Users,
  Gamepad2,
  Music,
  Heart,
  Star,
  User,
  type LucideIcon,
} from 'lucide-react';
import Card from '../ui/Card';
import { cn } from '../../lib/utils';

const iconMap: Record<string, LucideIcon> = {
  building: Building2,
  code: Code,
  briefcase: Briefcase,
  rocket: Rocket,
  book: BookOpen,
  users: Users,
  gamepad: Gamepad2,
  music: Music,
  heart: Heart,
  star: Star,
};

interface SpaceCardProps {
  name: string;
  description?: string | null;
  icon?: string | null;
  color?: string | null;
  logoUrl?: string | null;
  to: string;
  className?: string;
}

export default function SpaceCard({ name, description, icon, color, logoUrl, to, className }: SpaceCardProps) {
  const navigate = useNavigate();
  const Icon = (icon && iconMap[icon]) || User;

  const getLogoUrl = () => {
    if (!logoUrl) return null;
    if (logoUrl.startsWith('http')) return logoUrl;
    return `/uploads/${logoUrl}`;
  };

  return (
    <Card
      onClick={() => navigate(to)}
      className={cn(
        'cursor-pointer p-4 transition-all hover:scale-[1.02] hover:shadow-md sm:p-5',
        className,
      )}
    >
      <div className="flex items-center gap-3 sm:items-start sm:gap-4">
        {logoUrl ? (
          <img
            src={getLogoUrl() || ''}
            alt={name}
            className="h-10 w-10 shrink-0 rounded-lg object-cover sm:h-11 sm:w-11"
          />
        ) : (
          <div
            className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg sm:h-11 sm:w-11"
            style={{ backgroundColor: color || '#3B82F6' }}
          >
            <Icon className="h-5 w-5 text-white" />
          </div>
        )}
        <div className="min-w-0 flex-1">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate sm:text-base">
            {name}
          </h3>
          {description && (
            <p className="mt-0.5 text-xs text-gray-500 dark:text-gray-400 line-clamp-1 sm:mt-1 sm:text-sm sm:line-clamp-2">
              {description}
            </p>
          )}
        </div>
      </div>
    </Card>
  );
}

export { iconMap };
