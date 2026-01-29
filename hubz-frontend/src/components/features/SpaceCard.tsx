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
  to: string;
  className?: string;
}

export default function SpaceCard({ name, description, icon, color, to, className }: SpaceCardProps) {
  const navigate = useNavigate();
  const Icon = (icon && iconMap[icon]) || User;

  return (
    <Card
      onClick={() => navigate(to)}
      className={cn(
        'cursor-pointer p-5 transition-all hover:scale-[1.02] hover:shadow-md',
        className,
      )}
    >
      <div className="flex items-start gap-4">
        <div
          className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg"
          style={{ backgroundColor: color || '#3B82F6' }}
        >
          <Icon className="h-5 w-5 text-white" />
        </div>
        <div className="min-w-0">
          <h3 className="font-semibold text-gray-900 dark:text-gray-100 truncate">
            {name}
          </h3>
          {description && (
            <p className="mt-1 text-sm text-gray-500 dark:text-gray-400 line-clamp-2">
              {description}
            </p>
          )}
        </div>
      </div>
    </Card>
  );
}

export { iconMap };
