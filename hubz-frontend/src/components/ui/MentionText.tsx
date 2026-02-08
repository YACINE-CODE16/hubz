import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { cn } from '../../lib/utils';

interface MentionTextProps {
  content: string;
  organizationId?: string;
  className?: string;
}

// Regex to match @mentions: @word or @word.word
const MENTION_REGEX = /@([a-zA-Z]+(?:\.[a-zA-Z]+)?)/g;

interface TextPart {
  type: 'text' | 'mention';
  content: string;
  mentionName?: string;
}

/**
 * Component that renders text with highlighted @mentions.
 * Mentions are rendered as styled links.
 */
export default function MentionText({
  content,
  organizationId,
  className,
}: MentionTextProps) {
  // Parse content into text and mention parts
  const parts = useMemo(() => {
    if (!content) return [];

    const result: TextPart[] = [];
    let lastIndex = 0;
    let match;

    // Reset regex lastIndex
    MENTION_REGEX.lastIndex = 0;

    while ((match = MENTION_REGEX.exec(content)) !== null) {
      // Add text before the mention
      if (match.index > lastIndex) {
        result.push({
          type: 'text',
          content: content.slice(lastIndex, match.index),
        });
      }

      // Add the mention
      result.push({
        type: 'mention',
        content: match[0], // Full match including @
        mentionName: match[1], // Just the name without @
      });

      lastIndex = match.index + match[0].length;
    }

    // Add remaining text after last mention
    if (lastIndex < content.length) {
      result.push({
        type: 'text',
        content: content.slice(lastIndex),
      });
    }

    return result;
  }, [content]);

  return (
    <span className={cn('whitespace-pre-wrap', className)}>
      {parts.map((part, index) => {
        if (part.type === 'mention') {
          // For now, render mentions as styled spans
          // In the future, this could link to the user's profile
          return (
            <span
              key={index}
              className={cn(
                'inline-block rounded px-1 py-0.5',
                'bg-accent/10 text-accent font-medium',
                'cursor-pointer hover:bg-accent/20 transition-colors'
              )}
              title={`Mention: ${part.mentionName}`}
            >
              {part.content}
            </span>
          );
        }
        return <span key={index}>{part.content}</span>;
      })}
    </span>
  );
}
