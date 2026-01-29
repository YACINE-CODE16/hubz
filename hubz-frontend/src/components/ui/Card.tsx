import type { HTMLAttributes, ReactNode } from 'react';
import { cn } from '../../lib/utils';

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
}

export default function Card({ children, className, ...rest }: CardProps) {
  return (
    <div
      className={cn(
        'rounded-xl border border-gray-200/50 dark:border-white/10 bg-white/70 dark:bg-white/5 backdrop-blur-md shadow-sm',
        className,
      )}
      {...rest}
    >
      {children}
    </div>
  );
}
