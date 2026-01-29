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
  BarChart3,
  type LucideIcon,
} from 'lucide-react';
import { cn } from '../../lib/utils';

interface NavItem {
  label: string;
  to: string;
  icon: LucideIcon;
}

const personalNav: NavItem[] = [
  { label: 'Calendrier', to: 'calendar', icon: Calendar },
  { label: 'Objectifs', to: 'goals', icon: Target },
  { label: 'Habitudes', to: 'habits', icon: Repeat },
  { label: 'Recap', to: 'recap', icon: BarChart3 },
];

const organizationNav: NavItem[] = [
  { label: 'Dashboard', to: 'dashboard', icon: LayoutDashboard },
  { label: 'Equipes', to: 'teams', icon: Users },
  { label: 'Membres', to: 'members', icon: UserPlus },
  { label: 'Taches', to: 'tasks', icon: CheckSquare },
  { label: 'Objectifs', to: 'goals', icon: Target },
  { label: 'Calendrier', to: 'calendar', icon: Calendar },
  { label: 'Notes', to: 'notes', icon: StickyNote },
];

export type SpaceType = 'personal' | 'organization';

interface SidebarProps {
  spaceType: SpaceType;
  basePath: string;
  open: boolean;
  onClose: () => void;
}

export default function Sidebar({ spaceType, basePath, open, onClose }: SidebarProps) {
  const items = spaceType === 'personal' ? personalNav : organizationNav;

  return (
    <>
      {/* Backdrop (mobile) */}
      {open && (
        <div
          className="fixed inset-0 z-30 bg-black/40 backdrop-blur-sm lg:hidden"
          onClick={onClose}
        />
      )}

      {/* Panel */}
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-40 flex w-60 flex-col border-r border-gray-200/50 dark:border-white/10 bg-light-card/90 dark:bg-dark-card/90 backdrop-blur-md transition-transform duration-200 lg:static lg:translate-x-0',
          open ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <nav className="flex-1 space-y-1 px-3 pt-4">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={`${basePath}/${item.to}`}
              onClick={onClose}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
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
