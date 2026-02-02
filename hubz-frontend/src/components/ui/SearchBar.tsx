import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Search,
  X,
  Building2,
  CheckSquare,
  Target,
  Calendar,
  StickyNote,
  Users,
  Loader2,
} from 'lucide-react';
import { searchService } from '../../services/search.service';
import type { SearchResultResponse } from '../../types/search';

interface SearchBarProps {
  className?: string;
}

export default function SearchBar({ className = '' }: SearchBarProps) {
  const [query, setQuery] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const [results, setResults] = useState<SearchResultResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  const debounceTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);

  const performSearch = useCallback(async (searchQuery: string) => {
    if (searchQuery.trim().length < 2) {
      setResults(null);
      return;
    }

    setLoading(true);
    try {
      const data = await searchService.search(searchQuery);
      setResults(data);
    } catch (error) {
      console.error('Search error:', error);
      setResults(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (debounceTimeout.current) {
      clearTimeout(debounceTimeout.current);
    }

    debounceTimeout.current = setTimeout(() => {
      performSearch(query);
    }, 300);

    return () => {
      if (debounceTimeout.current) {
        clearTimeout(debounceTimeout.current);
      }
    };
  }, [query, performSearch]);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        setIsOpen(true);
        inputRef.current?.focus();
      }
      if (e.key === 'Escape') {
        setIsOpen(false);
        setQuery('');
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, []);

  const handleResultClick = (type: string, id: string, organizationId?: string | null) => {
    setIsOpen(false);
    setQuery('');

    switch (type) {
      case 'organization':
        navigate(`/org/${id}/dashboard`);
        break;
      case 'task':
        if (organizationId) navigate(`/org/${organizationId}/tasks`);
        break;
      case 'goal':
        if (organizationId) {
          navigate(`/org/${organizationId}/goals`);
        } else {
          navigate('/personal/goals');
        }
        break;
      case 'event':
        if (organizationId) {
          navigate(`/org/${organizationId}/calendar`);
        } else {
          navigate('/personal/calendar');
        }
        break;
      case 'note':
        if (organizationId) navigate(`/org/${organizationId}/notes`);
        break;
      case 'member':
        if (organizationId) navigate(`/org/${organizationId}/members`);
        break;
    }
  };

  const hasResults =
    results &&
    (results.organizations.length > 0 ||
      results.tasks.length > 0 ||
      results.goals.length > 0 ||
      results.events.length > 0 ||
      results.notes.length > 0 ||
      results.members.length > 0);

  return (
    <div ref={containerRef} className={`relative ${className}`}>
      {/* Search Input */}
      <div
        className={`flex items-center gap-2 rounded-lg border bg-white px-3 py-1.5 transition-all dark:bg-dark-card ${
          isOpen
            ? 'border-accent ring-2 ring-accent/20'
            : 'border-gray-200 dark:border-gray-700'
        }`}
      >
        <Search className="h-4 w-4 text-gray-400" />
        <input
          ref={inputRef}
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onFocus={() => setIsOpen(true)}
          placeholder="Rechercher..."
          className="w-40 bg-transparent text-sm text-gray-900 placeholder-gray-400 outline-none dark:text-gray-100 md:w-56"
        />
        {query && (
          <button
            onClick={() => {
              setQuery('');
              setResults(null);
            }}
            className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
          >
            <X className="h-4 w-4" />
          </button>
        )}
        <kbd className="hidden rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-500 dark:bg-gray-800 dark:text-gray-400 md:inline">
          Cmd+K
        </kbd>
      </div>

      {/* Results Dropdown */}
      {isOpen && query.length >= 2 && (
        <div className="absolute left-0 right-0 top-full z-50 mt-2 max-h-96 overflow-auto rounded-lg border border-gray-200 bg-white shadow-lg dark:border-gray-700 dark:bg-dark-card">
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-6 w-6 animate-spin text-accent" />
            </div>
          ) : hasResults ? (
            <div className="py-2">
              {/* Organizations */}
              {results.organizations.length > 0 && (
                <ResultSection title="Organisations">
                  {results.organizations.map((org) => (
                    <ResultItem
                      key={`org-${org.id}`}
                      icon={<Building2 className="h-4 w-4" />}
                      title={org.name}
                      subtitle={org.description || undefined}
                      color={org.color || undefined}
                      onClick={() => handleResultClick('organization', org.id)}
                    />
                  ))}
                </ResultSection>
              )}

              {/* Tasks */}
              {results.tasks.length > 0 && (
                <ResultSection title="Taches">
                  {results.tasks.map((task) => (
                    <ResultItem
                      key={`task-${task.id}`}
                      icon={<CheckSquare className="h-4 w-4" />}
                      title={task.title}
                      subtitle={task.organizationName}
                      badge={task.status}
                      onClick={() => handleResultClick('task', task.id, task.organizationId)}
                    />
                  ))}
                </ResultSection>
              )}

              {/* Goals */}
              {results.goals.length > 0 && (
                <ResultSection title="Objectifs">
                  {results.goals.map((goal) => (
                    <ResultItem
                      key={`goal-${goal.id}`}
                      icon={<Target className="h-4 w-4" />}
                      title={goal.title}
                      subtitle={goal.organizationName}
                      badge={goal.type}
                      onClick={() => handleResultClick('goal', goal.id, goal.organizationId)}
                    />
                  ))}
                </ResultSection>
              )}

              {/* Events */}
              {results.events.length > 0 && (
                <ResultSection title="Evenements">
                  {results.events.map((event) => (
                    <ResultItem
                      key={`event-${event.id}`}
                      icon={<Calendar className="h-4 w-4" />}
                      title={event.title}
                      subtitle={event.organizationName}
                      onClick={() => handleResultClick('event', event.id, event.organizationId)}
                    />
                  ))}
                </ResultSection>
              )}

              {/* Notes */}
              {results.notes.length > 0 && (
                <ResultSection title="Notes">
                  {results.notes.map((note) => (
                    <ResultItem
                      key={`note-${note.id}`}
                      icon={<StickyNote className="h-4 w-4" />}
                      title={note.title}
                      subtitle={note.organizationName}
                      badge={note.category || undefined}
                      onClick={() => handleResultClick('note', note.id, note.organizationId)}
                    />
                  ))}
                </ResultSection>
              )}

              {/* Members */}
              {results.members.length > 0 && (
                <ResultSection title="Membres">
                  {results.members.map((member) => (
                    <ResultItem
                      key={`member-${member.id}`}
                      icon={<Users className="h-4 w-4" />}
                      title={`${member.firstName} ${member.lastName}`}
                      subtitle={`${member.email} - ${member.organizationName}`}
                      badge={member.role}
                      onClick={() => handleResultClick('member', member.id, member.organizationId)}
                    />
                  ))}
                </ResultSection>
              )}
            </div>
          ) : (
            <div className="py-8 text-center text-sm text-gray-500 dark:text-gray-400">
              Aucun resultat pour "{query}"
            </div>
          )}
        </div>
      )}
    </div>
  );
}

interface ResultSectionProps {
  title: string;
  children: React.ReactNode;
}

function ResultSection({ title, children }: ResultSectionProps) {
  return (
    <div className="py-1">
      <div className="px-3 py-1 text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400">
        {title}
      </div>
      {children}
    </div>
  );
}

interface ResultItemProps {
  icon: React.ReactNode;
  title: string;
  subtitle?: string;
  badge?: string;
  color?: string;
  onClick: () => void;
}

function ResultItem({ icon, title, subtitle, badge, color, onClick }: ResultItemProps) {
  return (
    <button
      onClick={onClick}
      className="flex w-full items-center gap-3 px-3 py-2 text-left hover:bg-gray-100 dark:hover:bg-dark-hover"
    >
      <div
        className="flex h-8 w-8 items-center justify-center rounded-lg bg-gray-100 text-gray-500 dark:bg-gray-800 dark:text-gray-400"
        style={color ? { backgroundColor: color, color: 'white' } : undefined}
      >
        {icon}
      </div>
      <div className="flex-1 overflow-hidden">
        <p className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">{title}</p>
        {subtitle && (
          <p className="truncate text-xs text-gray-500 dark:text-gray-400">{subtitle}</p>
        )}
      </div>
      {badge && (
        <span className="rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-600 dark:bg-gray-800 dark:text-gray-400">
          {badge}
        </span>
      )}
    </button>
  );
}
