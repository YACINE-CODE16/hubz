import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import Card from './Card';

describe('Card', () => {
  it('should render children content', () => {
    render(<Card><p>Card content</p></Card>);
    expect(screen.getByText('Card content')).toBeInTheDocument();
  });

  it('should apply default styling classes', () => {
    const { container } = render(<Card>Content</Card>);
    const card = container.firstChild as HTMLElement;
    expect(card).toHaveClass('rounded-xl');
    expect(card).toHaveClass('backdrop-blur-md');
    expect(card).toHaveClass('shadow-sm');
  });

  it('should merge custom className', () => {
    const { container } = render(<Card className="p-4 mt-2">Content</Card>);
    const card = container.firstChild as HTMLElement;
    expect(card).toHaveClass('p-4');
    expect(card).toHaveClass('mt-2');
    expect(card).toHaveClass('rounded-xl');
  });

  it('should handle onClick events when provided', async () => {
    const handleClick = vi.fn();
    const user = userEvent.setup();

    render(<Card onClick={handleClick}>Clickable card</Card>);
    await user.click(screen.getByText('Clickable card'));

    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('should render as a div element', () => {
    const { container } = render(<Card>Content</Card>);
    expect(container.firstChild?.nodeName).toBe('DIV');
  });

  it('should pass through additional HTML attributes', () => {
    render(<Card data-testid="my-card" role="article">Content</Card>);
    expect(screen.getByTestId('my-card')).toBeInTheDocument();
    expect(screen.getByRole('article')).toBeInTheDocument();
  });

  it('should render multiple children', () => {
    render(
      <Card>
        <h2>Title</h2>
        <p>Description</p>
      </Card>
    );
    expect(screen.getByText('Title')).toBeInTheDocument();
    expect(screen.getByText('Description')).toBeInTheDocument();
  });
});
