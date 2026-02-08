import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import LoadingPage from './LoadingPage';

describe('LoadingPage', () => {
  it('should render with default loading message', () => {
    render(<LoadingPage />);
    expect(screen.getByText('Chargement...')).toBeInTheDocument();
  });

  it('should render with custom message', () => {
    render(<LoadingPage message="Loading data..." />);
    expect(screen.getByText('Loading data...')).toBeInTheDocument();
  });

  it('should display a spinner animation', () => {
    const { container } = render(<LoadingPage />);
    const spinner = container.querySelector('.animate-spin');
    expect(spinner).toBeInTheDocument();
  });

  it('should have full-screen layout', () => {
    const { container } = render(<LoadingPage />);
    const wrapper = container.firstChild as HTMLElement;
    expect(wrapper).toHaveClass('min-h-screen');
    expect(wrapper).toHaveClass('flex');
    expect(wrapper).toHaveClass('items-center');
    expect(wrapper).toHaveClass('justify-center');
  });

  it('should have a pulse animation on the glow effect', () => {
    const { container } = render(<LoadingPage />);
    const pulseElement = container.querySelector('.animate-pulse');
    expect(pulseElement).toBeInTheDocument();
  });
});
