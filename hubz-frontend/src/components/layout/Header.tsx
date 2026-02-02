import { useNavigate } from 'react-router-dom';
import { Menu, Home } from 'lucide-react';
import { useAuthStore } from '../../stores/authStore';
import { cn } from '../../lib/utils';
import SearchBar from '../ui/SearchBar';
import NotificationCenter from '../ui/NotificationCenter';

interface HeaderProps {
  title: string;
  color?: string | null;
  onMenuToggle: () => void;
}

export default function Header({ title, color, onMenuToggle }: HeaderProps) {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);

  const initials = user
    ? `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`
    : '??';

  return (
    <header className="shrink-0 border-b border-gray-200/50 dark:border-white/10 bg-light-card/50 dark:bg-dark-card/50 backdrop-blur-sm">
      <div className="flex items-center gap-3 px-4 py-3">
        {/* Left */}
        <button
          onClick={onMenuToggle}
          className="rounded-lg p-2 text-gray-500 hover:bg-light-hover dark:hover:bg-dark-hover hover:text-gray-700 dark:hover:text-gray-300 transition-colors lg:hidden"
        >
          <Menu className="h-5 w-5" />
        </button>

        <button
          onClick={() => navigate('/hub')}
          className="rounded-lg p-2 text-gray-500 hover:bg-light-hover dark:hover:bg-dark-hover hover:text-gray-700 dark:hover:text-gray-300 transition-colors"
        >
          <Home className="h-5 w-5" />
        </button>

        {/* Center - Search */}
        <div className="hidden flex-1 items-center justify-center md:flex">
          <SearchBar />
        </div>

        {/* Title on mobile */}
        <div className="flex flex-1 items-center justify-center gap-2 md:hidden">
          {color && (
            <div
              className="h-3 w-3 rounded-full"
              style={{ backgroundColor: color }}
            />
          )}
          <h1 className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">
            {title}
          </h1>
        </div>

        {/* Right */}
        <NotificationCenter />

        <div
          className={cn(
            'flex h-8 w-8 items-center justify-center rounded-full text-xs font-semibold text-white',
          )}
          style={{ backgroundColor: color || '#3B82F6' }}
        >
          {initials}
        </div>
      </div>
    </header>
  );
}
