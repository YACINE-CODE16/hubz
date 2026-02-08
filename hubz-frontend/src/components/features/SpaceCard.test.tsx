import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import SpaceCard from './SpaceCard';

// Mock react-router-dom's useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('SpaceCard', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
  });

  const renderSpaceCard = (props = {}) => {
    const defaultProps = {
      name: 'Test Organization',
      to: '/organization/123',
    };
    return render(
      <MemoryRouter>
        <SpaceCard {...defaultProps} {...props} />
      </MemoryRouter>
    );
  };

  it('should render organization name', () => {
    renderSpaceCard();
    expect(screen.getByText('Test Organization')).toBeInTheDocument();
  });

  it('should render description when provided', () => {
    renderSpaceCard({ description: 'A great team' });
    expect(screen.getByText('A great team')).toBeInTheDocument();
  });

  it('should not render description when not provided', () => {
    renderSpaceCard({ description: null });
    const card = screen.getByText('Test Organization').closest('div');
    expect(card).not.toBeNull();
    // Only the name should be present, no additional paragraph
    const paragraphs = card?.querySelectorAll('p');
    expect(paragraphs?.length ?? 0).toBe(0);
  });

  it('should navigate when clicked', async () => {
    const user = userEvent.setup();
    renderSpaceCard({ to: '/organization/456' });

    await user.click(screen.getByText('Test Organization'));
    expect(mockNavigate).toHaveBeenCalledWith('/organization/456');
  });

  it('should display icon with custom color', () => {
    const { container } = renderSpaceCard({ color: '#FF5733' });
    const iconContainer = container.querySelector('[style*="background-color"]') as HTMLElement;
    expect(iconContainer).toBeInTheDocument();
    expect(iconContainer?.style.backgroundColor).toBe('rgb(255, 87, 51)');
  });

  it('should use default blue color when no color provided', () => {
    const { container } = renderSpaceCard();
    const iconContainer = container.querySelector('[style*="background-color"]') as HTMLElement;
    expect(iconContainer).toBeInTheDocument();
    expect(iconContainer?.style.backgroundColor).toBe('rgb(59, 130, 246)');
  });

  it('should display logo image when logoUrl is provided', () => {
    renderSpaceCard({ logoUrl: 'http://example.com/logo.png' });
    const img = screen.getByAltText('Test Organization');
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', 'http://example.com/logo.png');
  });

  it('should prepend /uploads/ for relative logo URLs', () => {
    renderSpaceCard({ logoUrl: 'org-logos/logo.png' });
    const img = screen.getByAltText('Test Organization');
    expect(img).toHaveAttribute('src', '/uploads/org-logos/logo.png');
  });

  it('should apply custom className', () => {
    const { container } = renderSpaceCard({ className: 'my-custom-class' });
    const card = container.querySelector('.my-custom-class');
    expect(card).toBeInTheDocument();
  });

  it('should have cursor-pointer for clickability', () => {
    const { container } = renderSpaceCard();
    const card = container.querySelector('.cursor-pointer');
    expect(card).toBeInTheDocument();
  });
});
