import { Users } from 'lucide-react';
import type { NoteCollaborator } from '../../types/collaboration';
import { cn } from '../../lib/utils';

interface NoteCollaboratorsProps {
  collaborators: NoteCollaborator[];
  typingUsers: NoteCollaborator[];
  maxVisible?: number;
  className?: string;
}

export default function NoteCollaborators({
  collaborators,
  typingUsers,
  maxVisible = 4,
  className,
}: NoteCollaboratorsProps) {
  if (collaborators.length === 0) {
    return null;
  }

  const visibleCollaborators = collaborators.slice(0, maxVisible);
  const remainingCount = collaborators.length - maxVisible;
  const typingUserIds = new Set(typingUsers.map((u) => u.userId));

  return (
    <div className={cn('flex items-center gap-2', className)}>
      {/* Online badge */}
      <div className="flex items-center gap-1 px-2 py-1 rounded-full bg-green-500/20 text-green-400 text-xs font-medium">
        <Users className="w-3 h-3" />
        <span>{collaborators.length} en ligne</span>
      </div>

      {/* Avatar stack */}
      <div className="flex -space-x-2">
        {visibleCollaborators.map((collaborator, index) => (
          <div
            key={collaborator.userId}
            className="relative group"
            style={{ zIndex: visibleCollaborators.length - index }}
          >
            {/* Avatar */}
            <div
              className={cn(
                'w-8 h-8 rounded-full border-2 border-dark-card flex items-center justify-center text-xs font-medium',
                'transition-transform hover:scale-110 hover:z-50',
                typingUserIds.has(collaborator.userId) && 'ring-2 ring-offset-2 ring-offset-dark-card'
              )}
              style={{
                backgroundColor: collaborator.color,
                color: getContrastColor(collaborator.color),
                ringColor: typingUserIds.has(collaborator.userId) ? collaborator.color : undefined,
              }}
              title={collaborator.displayName}
            >
              {collaborator.profilePhotoUrl ? (
                <img
                  src={collaborator.profilePhotoUrl}
                  alt={collaborator.displayName}
                  className="w-full h-full rounded-full object-cover"
                />
              ) : (
                collaborator.initials
              )}
            </div>

            {/* Typing indicator */}
            {typingUserIds.has(collaborator.userId) && (
              <div className="absolute -bottom-1 -right-1 flex items-center justify-center">
                <div className="flex gap-0.5">
                  <span
                    className="w-1 h-1 rounded-full animate-bounce"
                    style={{ backgroundColor: collaborator.color, animationDelay: '0ms' }}
                  />
                  <span
                    className="w-1 h-1 rounded-full animate-bounce"
                    style={{ backgroundColor: collaborator.color, animationDelay: '150ms' }}
                  />
                  <span
                    className="w-1 h-1 rounded-full animate-bounce"
                    style={{ backgroundColor: collaborator.color, animationDelay: '300ms' }}
                  />
                </div>
              </div>
            )}

            {/* Tooltip */}
            <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 px-2 py-1 bg-dark-base text-white text-xs rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap pointer-events-none z-50">
              {collaborator.displayName}
              {typingUserIds.has(collaborator.userId) && (
                <span className="text-gray-400 ml-1">est en train d'ecrire...</span>
              )}
            </div>
          </div>
        ))}

        {/* Remaining count */}
        {remainingCount > 0 && (
          <div
            className="w-8 h-8 rounded-full border-2 border-dark-card bg-dark-hover flex items-center justify-center text-xs font-medium text-gray-400"
            title={`${remainingCount} autres collaborateurs`}
          >
            +{remainingCount}
          </div>
        )}
      </div>

      {/* Typing indicator text */}
      {typingUsers.length > 0 && (
        <div className="text-xs text-gray-400 animate-pulse">
          {typingUsers.length === 1 ? (
            <span>{typingUsers[0].firstName || typingUsers[0].email.split('@')[0]} ecrit...</span>
          ) : typingUsers.length === 2 ? (
            <span>
              {typingUsers[0].firstName || typingUsers[0].email.split('@')[0]} et{' '}
              {typingUsers[1].firstName || typingUsers[1].email.split('@')[0]} ecrivent...
            </span>
          ) : (
            <span>Plusieurs personnes ecrivent...</span>
          )}
        </div>
      )}
    </div>
  );
}

function getContrastColor(hexColor: string): string {
  const hex = hexColor.replace('#', '');
  const r = parseInt(hex.substring(0, 2), 16);
  const g = parseInt(hex.substring(2, 4), 16);
  const b = parseInt(hex.substring(4, 6), 16);
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
  return luminance > 0.5 ? '#000000' : '#FFFFFF';
}
