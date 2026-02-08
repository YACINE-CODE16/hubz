import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Users,
  UserPlus,
  CheckSquare,
  Target,
  Calendar,
  StickyNote,
  Repeat,
  TrendingUp,
  Settings,
  Shield,
  SlidersHorizontal,
  Building2,
  Star,
  Webhook,
  MessageSquare,
  type LucideIcon,
} from 'lucide-react';
import { cn } from '../../lib/utils';
import { iconMap } from '../features/SpaceCard';

interface NavItem {
  label: string;
  to: string;
  icon: LucideIcon;
}

const personalNav: NavItem[] = [
  { label: 'Tableau de bord', to: 'dashboard', icon: LayoutDashboard },
  { label: 'Calendrier', to: 'calendar', icon: Calendar },
  { label: 'Objectifs', to: 'goals', icon: Target },
  { label: 'Habitudes', to: 'habits', icon: Repeat },
  { label: 'Messages', to: 'messages', icon: MessageSquare },
  { label: 'Parametres', to: 'settings', icon: Settings },
  { label: 'Securite', to: 'security', icon: Shield },
  { label: 'Preferences', to: 'preferences', icon: SlidersHorizontal },
];

const organizationNav: NavItem[] = [
  { label: 'Dashboard', to: 'dashboard', icon: LayoutDashboard },
  { label: 'Equipes', to: 'teams', icon: Users },
  { label: 'Membres', to: 'members', icon: UserPlus },
  { label: 'Taches', to: 'tasks', icon: CheckSquare },
  { label: 'Objectifs', to: 'goals', icon: Target },
  { label: 'Calendrier', to: 'calendar', icon: Calendar },
  { label: 'Notes', to: 'notes', icon: StickyNote },
  { label: 'Analytics', to: 'analytics', icon: TrendingUp },
  { label: 'Parametres', to: 'settings', icon: Settings },
  { label: 'Webhooks', to: 'webhooks', icon: Webhook },
];

export type SpaceType = 'personal' | 'organization';

interface SidebarProps {
  spaceType: SpaceType;
  basePath: string;
  open: boolean;
  onClose: () => void;
  logoUrl?: string | null;
  color?: string | null;
  icon?: string | null;
  title?: string;
}

export default function Sidebar({ spaceType, basePath, open, onClose, logoUrl, color, icon, title }: SidebarProps) {
  const items = spaceType === 'personal' ? personalNav : organizationNav;

  const getLogoUrl = () => {
    if (!logoUrl) return null;
    if (logoUrl.startsWith('http')) return logoUrl;
    return `/uploads/${logoUrl}`;
  };

  const IconComponent = spaceType === 'personal'
    ? Star
    : (icon && iconMap[icon]) || Building2;

  return (
    <>
      {/* Backdrop (mobile) */}
      <div
        className={cn(
          'fixed inset-0 z-30 bg-black/40 backdrop-blur-sm transition-opacity duration-200 lg:hidden',
          open ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none',
        )}
        onClick={onClose}
      />

      {/* Panel */}
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-gray-200/50 dark:border-white/10 bg-light-card dark:bg-dark-card transition-transform duration-200 ease-in-out lg:static lg:w-60 lg:translate-x-0 lg:bg-light-card/90 lg:dark:bg-dark-card/90 lg:backdrop-blur-md',
          open ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        {/* Organization/Space Header with Logo */}
        {spaceType === 'organization' && (
          <div className="flex items-center gap-3 border-b border-gray-200/50 dark:border-white/10 px-4 py-4">
            {logoUrl ? (
              <img
                src={getLogoUrl() || ''}
                alt={title || 'Organization'}
                className="h-10 w-10 rounded-lg object-cover"
              />
            ) : (
              <div
                className="flex h-10 w-10 items-center justify-center rounded-lg text-white"
                style={{ backgroundColor: color || '#3B82F6' }}
              >
                <IconComponent className="h-5 w-5" />
              </div>
            )}
            <div className="min-w-0 flex-1">
              <h2 className="truncate text-sm font-semibold text-gray-900 dark:text-gray-100">
                {title || 'Organisation'}
              </h2>
            </div>
          </div>
        )}

        {/* Personal Space Header (mobile only, for context) */}
        {spaceType === 'personal' && (
          <div className="flex items-center gap-3 border-b border-gray-200/50 dark:border-white/10 px-4 py-4 lg:hidden">
            <div
              className="flex h-10 w-10 items-center justify-center rounded-lg text-white"
              style={{ backgroundColor: '#6366F1' }}
            >
              <Star className="h-5 w-5" />
            </div>
            <div className="min-w-0 flex-1">
              <h2 className="truncate text-sm font-semibold text-gray-900 dark:text-gray-100">
                Espace personnel
              </h2>
            </div>
          </div>
        )}

        <nav className="flex-1 space-y-1 overflow-y-auto px-3 pt-4 pb-4">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={`${basePath}/${item.to}`}
              onClick={onClose}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-accent/10 text-accent'
                    : 'text-gray-600 dark:text-gray-400 hover:bg-light-hover dark:hover:bg-dark-hover hover:text-gray-900 dark:hover:text-gray-200',
                )
              }
            >
              <item.icon className="h-4 w-4 shrink-0" />
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
    </>
  );
}
