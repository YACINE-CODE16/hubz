import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Menu, Home, Keyboard, Search, X } from 'lucide-react';
import { useAuthStore } from '../../stores/authStore';
import { cn } from '../../lib/utils';
import SearchBar from '../ui/SearchBar';
import NotificationCenter from '../ui/NotificationCenter';
import { useKeyboardShortcutsContext } from '../providers/KeyboardShortcutsProvider';

interface HeaderProps {
  title: string;
  color?: string | null;
  onMenuToggle: () => void;
}

export default function Header({ title, color, onMenuToggle }: HeaderProps) {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const { openHelp } = useKeyboardShortcutsContext();
  const [mobileSearchOpen, setMobileSearchOpen] = useState(false);

  const initials = user
    ? `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`
    : '??';

  const getPhotoUrl = () => {
    if (!user?.profilePhotoUrl) return null;
    // Handle relative URLs from the backend
    if (user.profilePhotoUrl.startsWith('http')) {
      return user.profilePhotoUrl;
    }
    return `/uploads/${user.profilePhotoUrl}`;
  };

  const photoUrl = getPhotoUrl();

  return (
    <header className="shrink-0 border-b border-gray-200/50 dark:border-white/10 bg-light-card/50 dark:bg-dark-card/50 backdrop-blur-sm">
      {/* Mobile Search Overlay */}
      {mobileSearchOpen && (
        <div className="flex items-center gap-2 px-3 py-2 md:hidden">
          <SearchBar className="flex-1" />
          <button
            onClick={() => setMobileSearchOpen(false)}
            className="shrink-0 rounded-lg p-2 text-gray-500 hover:bg-light-hover dark:hover:bg-dark-hover"
          >
            <X className="h-5 w-5" />
          </button>
        </div>
      )}

      {/* Main header row */}
      <div className={cn(
        'flex items-center gap-2 px-3 py-2 sm:gap-3 sm:px-4 sm:py-3',
        mobileSearchOpen && 'hidden md:flex',
      )}>
        {/* Left: burger + home */}
        <button
          onClick={onMenuToggle}
          className="rounded-lg p-2 text-gray-500 hover:bg-light-hover dark:hover:bg-dark-hover hover:text-gray-700 dark:hover:text-gray-300 transition-colors lg:hidden"
        >
          <Menu className="h-5 w-5" />
        </button>

        <button
          onClick={() => navigate('/hub')}
          className="hidden rounded-lg p-2 text-gray-500 hover:bg-light-hover dark:hover:bg-dark-hover hover:text-gray-700 dark:hover:text-gray-300 transition-colors sm:block"
        >
          <Home className="h-5 w-5" />
        </button>

        {/* Center - Search (desktop) */}
        <div className="hidden flex-1 items-center justify-center md:flex">
          <SearchBar />
        </div>

        {/* Title on mobile */}
        <div className="flex flex-1 items-center justify-center gap-2 md:hidden min-w-0">
          {color && (
            <div
              className="h-2.5 w-2.5 shrink-0 rounded-full"
              style={{ backgroundColor: color }}
            />
          )}
          <h1 className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">
            {title}
          </h1>
        </div>

        {/* Right actions */}
        {/* Mobile search toggle */}
        <button
          onClick={() => setMobileSearchOpen(true)}
          className="rounded-lg p-2 text-gray-500 hover:bg-light-hover dark:hover:bg-dark-hover hover:text-gray-700 dark:hover:text-gray-300 transition-colors md:hidden"
        >
          <Search className="h-5 w-5" />
        </button>

        <button
          onClick={openHelp}
          className="hidden rounded-lg p-2 text-gray-500 hover:bg-light-hover dark:hover:bg-dark-hover hover:text-gray-700 dark:hover:text-gray-300 transition-colors md:block"
          title="Raccourcis clavier (?)"
        >
          <Keyboard className="h-5 w-5" />
        </button>

        <NotificationCenter />

        {/* Profile Photo / Avatar */}
        <button
          onClick={() => navigate('/personal/settings')}
          className="relative h-8 w-8 shrink-0 rounded-full overflow-hidden focus:outline-none focus:ring-2 focus:ring-accent focus:ring-offset-2 dark:focus:ring-offset-dark-base transition-all hover:opacity-80"
        >
          {photoUrl ? (
            <img
              src={photoUrl}
              alt="Profile"
              className="h-full w-full object-cover"
            />
          ) : (
            <div
              className={cn(
                'flex h-full w-full items-center justify-center text-xs font-semibold text-white',
              )}
              style={{ backgroundColor: color || '#3B82F6' }}
            >
              {initials}
            </div>
          )}
        </button>
      </div>
    </header>
  );
}
