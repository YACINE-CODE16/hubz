import { useState, useEffect, useRef, useCallback } from 'react';
import { cn } from '../../lib/utils';
import type { MentionableUser } from '../../types/mention';

interface MentionInputProps {
  value: string;
  onChange: (value: string) => void;
  mentionableUsers: MentionableUser[];
  placeholder?: string;
  rows?: number;
  className?: string;
  disabled?: boolean;
  autoFocus?: boolean;
}

/**
 * A textarea component with @mention autocomplete functionality.
 * Shows a dropdown with matching users when typing @ followed by characters.
 */
export default function MentionInput({
  value,
  onChange,
  mentionableUsers,
  placeholder = 'Write a comment...',
  rows = 2,
  className,
  disabled = false,
  autoFocus = false,
}: MentionInputProps) {
  const [showDropdown, setShowDropdown] = useState(false);
  const [filteredUsers, setFilteredUsers] = useState<MentionableUser[]>([]);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [mentionStartIndex, setMentionStartIndex] = useState<number | null>(null);
  const [mentionQuery, setMentionQuery] = useState('');

  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Filter users based on the mention query
  useEffect(() => {
    if (mentionQuery) {
      const query = mentionQuery.toLowerCase();
      const filtered = mentionableUsers.filter(
        (user) =>
          user.firstName.toLowerCase().includes(query) ||
          user.lastName.toLowerCase().includes(query) ||
          user.displayName.toLowerCase().includes(query) ||
          user.mentionName.toLowerCase().includes(query)
      );
      setFilteredUsers(filtered.slice(0, 8)); // Limit to 8 suggestions
      setSelectedIndex(0);
    } else {
      setFilteredUsers(mentionableUsers.slice(0, 8));
      setSelectedIndex(0);
    }
  }, [mentionQuery, mentionableUsers]);

  // Detect @ character and manage mention state
  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      const newValue = e.target.value;
      const cursorPos = e.target.selectionStart || 0;

      onChange(newValue);

      // Find the start of the current mention (if any)
      let mentionStart: number | null = null;
      for (let i = cursorPos - 1; i >= 0; i--) {
        const char = newValue[i];
        if (char === '@') {
          // Check if @ is at the start or preceded by whitespace
          if (i === 0 || /\s/.test(newValue[i - 1])) {
            mentionStart = i;
            break;
          }
        } else if (/\s/.test(char)) {
          break;
        }
      }

      if (mentionStart !== null) {
        const query = newValue.slice(mentionStart + 1, cursorPos);
        // Only show dropdown if query doesn't contain spaces
        if (!/\s/.test(query)) {
          setMentionStartIndex(mentionStart);
          setMentionQuery(query);
          setShowDropdown(true);
          return;
        }
      }

      setShowDropdown(false);
      setMentionStartIndex(null);
      setMentionQuery('');
    },
    [onChange]
  );

  // Handle keyboard navigation in dropdown
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
      if (!showDropdown || filteredUsers.length === 0) {
        return;
      }

      switch (e.key) {
        case 'ArrowDown':
          e.preventDefault();
          setSelectedIndex((prev) =>
            prev < filteredUsers.length - 1 ? prev + 1 : 0
          );
          break;
        case 'ArrowUp':
          e.preventDefault();
          setSelectedIndex((prev) =>
            prev > 0 ? prev - 1 : filteredUsers.length - 1
          );
          break;
        case 'Enter':
        case 'Tab':
          e.preventDefault();
          insertMention(filteredUsers[selectedIndex]);
          break;
        case 'Escape':
          e.preventDefault();
          setShowDropdown(false);
          break;
      }
    },
    [showDropdown, filteredUsers, selectedIndex]
  );

  // Insert the selected mention into the text
  const insertMention = useCallback(
    (user: MentionableUser) => {
      if (mentionStartIndex === null || !textareaRef.current) {
        return;
      }

      const cursorPos = textareaRef.current.selectionStart || 0;
      const beforeMention = value.slice(0, mentionStartIndex);
      const afterMention = value.slice(cursorPos);
      const mentionText = `@${user.mentionName} `;

      const newValue = beforeMention + mentionText + afterMention;
      onChange(newValue);

      // Set cursor position after the mention
      const newCursorPos = mentionStartIndex + mentionText.length;
      setTimeout(() => {
        textareaRef.current?.focus();
        textareaRef.current?.setSelectionRange(newCursorPos, newCursorPos);
      }, 0);

      setShowDropdown(false);
      setMentionStartIndex(null);
      setMentionQuery('');
    },
    [value, onChange, mentionStartIndex]
  );

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(e.target as Node) &&
        textareaRef.current &&
        !textareaRef.current.contains(e.target as Node)
      ) {
        setShowDropdown(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Scroll selected item into view
  useEffect(() => {
    if (showDropdown && dropdownRef.current) {
      const selectedItem = dropdownRef.current.children[selectedIndex] as HTMLElement;
      if (selectedItem) {
        selectedItem.scrollIntoView({ block: 'nearest' });
      }
    }
  }, [selectedIndex, showDropdown]);

  return (
    <div className="relative">
      <textarea
        ref={textareaRef}
        value={value}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        rows={rows}
        disabled={disabled}
        autoFocus={autoFocus}
        className={cn(
          'w-full rounded-lg border border-gray-200 dark:border-gray-600',
          'bg-white dark:bg-dark-card px-3 py-2 text-sm',
          'text-gray-900 dark:text-gray-100 placeholder:text-gray-400',
          'focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/20',
          'resize-none',
          disabled && 'cursor-not-allowed opacity-50',
          className
        )}
      />

      {/* Mention dropdown */}
      {showDropdown && filteredUsers.length > 0 && (
        <div
          ref={dropdownRef}
          className={cn(
            'absolute z-50 mt-1 w-64 max-h-64 overflow-y-auto',
            'rounded-lg border border-gray-200 dark:border-gray-700',
            'bg-white dark:bg-dark-card shadow-lg'
          )}
        >
          {filteredUsers.map((user, index) => (
            <button
              key={user.userId}
              type="button"
              onClick={() => insertMention(user)}
              className={cn(
                'w-full flex items-center gap-3 px-3 py-2 text-left',
                'transition-colors',
                index === selectedIndex
                  ? 'bg-accent/10 text-accent'
                  : 'hover:bg-gray-100 dark:hover:bg-dark-hover'
              )}
            >
              {/* Avatar */}
              <div className="h-8 w-8 flex-shrink-0 rounded-full bg-accent/20 flex items-center justify-center overflow-hidden">
                {user.profilePhotoUrl ? (
                  <img
                    src={user.profilePhotoUrl}
                    alt={user.displayName}
                    className="h-full w-full object-cover"
                  />
                ) : (
                  <span className="text-sm font-medium text-accent">
                    {user.firstName.charAt(0).toUpperCase()}
                  </span>
                )}
              </div>

              {/* User info */}
              <div className="flex-1 min-w-0">
                <div className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">
                  {user.displayName}
                </div>
                <div className="text-xs text-gray-500 dark:text-gray-400 truncate">
                  @{user.mentionName}
                </div>
              </div>
            </button>
          ))}
        </div>
      )}

      {/* No results message */}
      {showDropdown && mentionQuery && filteredUsers.length === 0 && (
        <div
          ref={dropdownRef}
          className={cn(
            'absolute z-50 mt-1 w-64 px-3 py-2',
            'rounded-lg border border-gray-200 dark:border-gray-700',
            'bg-white dark:bg-dark-card shadow-lg'
          )}
        >
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Aucun utilisateur trouve
          </p>
        </div>
      )}
    </div>
  );
}
