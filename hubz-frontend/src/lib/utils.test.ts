import { describe, it, expect } from 'vitest';
import { cn } from './utils';

describe('cn (className merge utility)', () => {
  it('should merge class names', () => {
    const result = cn('px-4', 'py-2');
    expect(result).toBe('px-4 py-2');
  });

  it('should handle conditional classes', () => {
    const isActive = true;
    const result = cn('base-class', isActive && 'active-class');
    expect(result).toBe('base-class active-class');
  });

  it('should filter out falsy values', () => {
    const result = cn('base', false, null, undefined, '', 'visible');
    expect(result).toBe('base visible');
  });

  it('should resolve Tailwind conflicts (last wins)', () => {
    // tailwind-merge resolves conflicting classes
    const result = cn('px-4', 'px-6');
    expect(result).toBe('px-6');
  });

  it('should resolve Tailwind color conflicts', () => {
    const result = cn('bg-red-500', 'bg-blue-500');
    expect(result).toBe('bg-blue-500');
  });

  it('should handle array of classes', () => {
    const result = cn(['px-4', 'py-2']);
    expect(result).toBe('px-4 py-2');
  });

  it('should handle object notation from clsx', () => {
    const result = cn({ 'text-red-500': true, 'text-blue-500': false });
    expect(result).toBe('text-red-500');
  });

  it('should handle empty arguments', () => {
    const result = cn();
    expect(result).toBe('');
  });

  it('should handle mixed inputs', () => {
    const result = cn('base', ['arr-class'], { 'obj-class': true });
    expect(result).toBe('base arr-class obj-class');
  });

  it('should handle responsive prefix conflicts', () => {
    const result = cn('md:px-4', 'md:px-6');
    expect(result).toBe('md:px-6');
  });

  it('should not merge non-conflicting classes', () => {
    const result = cn('text-lg', 'font-bold', 'text-gray-900');
    expect(result).toBe('text-lg font-bold text-gray-900');
  });
});
