import { useState, useRef, useEffect } from 'react';
import { Plus, Check, X } from 'lucide-react';
import { cn } from '../../lib/utils';
import type { Tag } from '../../types/tag';
import TagChip from '../ui/TagChip';

interface TagSelectorProps {
  availableTags: Tag[];
  selectedTags: Tag[];
  onTagsChange: (tags: Tag[]) => void;
  onCreateTag?: (name: string, color: string) => Promise<Tag>;
  disabled?: boolean;
  className?: string;
}

const DEFAULT_COLORS = [
  '#EF4444', // red
  '#F97316', // orange
  '#F59E0B', // amber
  '#EAB308', // yellow
  '#84CC16', // lime
  '#22C55E', // green
  '#14B8A6', // teal
  '#06B6D4', // cyan
  '#3B82F6', // blue
  '#6366F1', // indigo
  '#8B5CF6', // violet
  '#A855F7', // purple
  '#D946EF', // fuchsia
  '#EC4899', // pink
  '#64748B', // slate
];

export default function TagSelector({
  availableTags,
  selectedTags,
  onTagsChange,
  onCreateTag,
  disabled = false,
  className,
}: TagSelectorProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [newTagName, setNewTagName] = useState('');
  const [newTagColor, setNewTagColor] = useState(DEFAULT_COLORS[0]);
  const [isCreating, setIsCreating] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
        setShowCreate(false);
        setSearchQuery('');
      }
    }

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const selectedTagIds = new Set(selectedTags.map((t) => t.id));

  const filteredTags = availableTags.filter(
    (tag) =>
      tag.name.toLowerCase().includes(searchQuery.toLowerCase()) &&
      !selectedTagIds.has(tag.id)
  );

  const handleToggleTag = (tag: Tag) => {
    if (selectedTagIds.has(tag.id)) {
      onTagsChange(selectedTags.filter((t) => t.id !== tag.id));
    } else {
      onTagsChange([...selectedTags, tag]);
    }
  };

  const handleRemoveTag = (tagId: string) => {
    onTagsChange(selectedTags.filter((t) => t.id !== tagId));
  };

  const handleCreateTag = async () => {
    if (!onCreateTag || !newTagName.trim() || isCreating) return;

    setIsCreating(true);
    try {
      const newTag = await onCreateTag(newTagName.trim(), newTagColor);
      onTagsChange([...selectedTags, newTag]);
      setNewTagName('');
      setNewTagColor(DEFAULT_COLORS[Math.floor(Math.random() * DEFAULT_COLORS.length)]);
      setShowCreate(false);
      setSearchQuery('');
    } catch (error) {
      console.error('Failed to create tag:', error);
    } finally {
      setIsCreating(false);
    }
  };

  const showCreateOption =
    onCreateTag &&
    searchQuery.trim() &&
    !availableTags.some((t) => t.name.toLowerCase() === searchQuery.toLowerCase());

  return (
    <div className={cn('relative', className)} ref={dropdownRef}>
      {/* Selected tags display */}
      <div className="flex flex-wrap items-center gap-1.5 min-h-[32px]">
        {selectedTags.map((tag) => (
          <TagChip
            key={tag.id}
            tag={tag}
            size="sm"
            onRemove={disabled ? undefined : () => handleRemoveTag(tag.id)}
          />
        ))}
        {!disabled && (
          <button
            type="button"
            onClick={() => {
              setIsOpen(!isOpen);
              setTimeout(() => inputRef.current?.focus(), 0);
            }}
            className="inline-flex items-center gap-1 rounded-full border border-dashed border-gray-300 px-2 py-0.5 text-xs text-gray-500 transition-colors hover:border-gray-400 hover:text-gray-600 dark:border-gray-600 dark:text-gray-400 dark:hover:border-gray-500 dark:hover:text-gray-300"
          >
            <Plus className="h-3 w-3" />
            Ajouter
          </button>
        )}
      </div>

      {/* Dropdown */}
      {isOpen && !disabled && (
        <div className="absolute left-0 top-full z-50 mt-1 w-64 rounded-lg border border-gray-200 bg-white shadow-lg dark:border-gray-700 dark:bg-dark-card">
          {/* Search input */}
          <div className="border-b border-gray-200 p-2 dark:border-gray-700">
            <input
              ref={inputRef}
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Rechercher ou creer..."
              className="w-full rounded-md border border-gray-200 bg-transparent px-3 py-1.5 text-sm focus:border-accent focus:outline-none dark:border-gray-700"
            />
          </div>

          {/* Tag list */}
          <div className="max-h-48 overflow-y-auto p-1">
            {filteredTags.length === 0 && !showCreateOption && (
              <div className="px-3 py-2 text-center text-sm text-gray-500 dark:text-gray-400">
                Aucun tag trouve
              </div>
            )}

            {filteredTags.map((tag) => (
              <button
                key={tag.id}
                type="button"
                onClick={() => handleToggleTag(tag)}
                className="flex w-full items-center gap-2 rounded-md px-3 py-1.5 text-left text-sm transition-colors hover:bg-light-hover dark:hover:bg-dark-hover"
              >
                <span
                  className="h-3 w-3 rounded-full"
                  style={{ backgroundColor: tag.color }}
                />
                <span className="flex-1 truncate">{tag.name}</span>
                {selectedTagIds.has(tag.id) && (
                  <Check className="h-4 w-4 text-accent" />
                )}
              </button>
            ))}

            {/* Create new tag option */}
            {showCreateOption && (
              <button
                type="button"
                onClick={() => {
                  setNewTagName(searchQuery);
                  setShowCreate(true);
                }}
                className="flex w-full items-center gap-2 rounded-md px-3 py-1.5 text-left text-sm text-accent transition-colors hover:bg-light-hover dark:hover:bg-dark-hover"
              >
                <Plus className="h-4 w-4" />
                Creer "{searchQuery}"
              </button>
            )}
          </div>

          {/* Create tag form */}
          {showCreate && (
            <div className="border-t border-gray-200 p-3 dark:border-gray-700">
              <div className="mb-2 text-xs font-medium text-gray-600 dark:text-gray-400">
                Nouveau tag
              </div>
              <input
                type="text"
                value={newTagName}
                onChange={(e) => setNewTagName(e.target.value)}
                placeholder="Nom du tag"
                className="mb-2 w-full rounded-md border border-gray-200 bg-transparent px-3 py-1.5 text-sm focus:border-accent focus:outline-none dark:border-gray-700"
              />
              <div className="mb-3 flex flex-wrap gap-1">
                {DEFAULT_COLORS.map((color) => (
                  <button
                    key={color}
                    type="button"
                    onClick={() => setNewTagColor(color)}
                    className={cn(
                      'h-5 w-5 rounded-full transition-transform',
                      newTagColor === color && 'ring-2 ring-accent ring-offset-2 scale-110'
                    )}
                    style={{ backgroundColor: color }}
                    aria-label={`Select color ${color}`}
                  />
                ))}
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => {
                    setShowCreate(false);
                    setNewTagName('');
                  }}
                  className="flex-1 rounded-md border border-gray-200 px-3 py-1.5 text-sm transition-colors hover:bg-light-hover dark:border-gray-700 dark:hover:bg-dark-hover"
                >
                  <X className="mx-auto h-4 w-4" />
                </button>
                <button
                  type="button"
                  onClick={handleCreateTag}
                  disabled={!newTagName.trim() || isCreating}
                  className="flex-1 rounded-md bg-accent px-3 py-1.5 text-sm text-white transition-colors hover:bg-blue-600 disabled:opacity-50"
                >
                  {isCreating ? 'Creation...' : 'Creer'}
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
