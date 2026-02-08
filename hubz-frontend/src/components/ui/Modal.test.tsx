import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import Modal from './Modal';

describe('Modal', () => {
  it('should not render when isOpen is false', () => {
    render(
      <Modal isOpen={false} onClose={vi.fn()}>
        <p>Modal content</p>
      </Modal>
    );
    expect(screen.queryByText('Modal content')).not.toBeInTheDocument();
  });

  it('should render children when isOpen is true', () => {
    render(
      <Modal isOpen={true} onClose={vi.fn()}>
        <p>Modal content</p>
      </Modal>
    );
    expect(screen.getByText('Modal content')).toBeInTheDocument();
  });

  it('should render title when provided', () => {
    render(
      <Modal isOpen={true} onClose={vi.fn()} title="My Modal">
        <p>Content</p>
      </Modal>
    );
    expect(screen.getByText('My Modal')).toBeInTheDocument();
  });

  it('should not render title section when title is not provided', () => {
    render(
      <Modal isOpen={true} onClose={vi.fn()}>
        <p>Content only</p>
      </Modal>
    );
    // There should be no heading element
    expect(screen.queryByRole('heading')).not.toBeInTheDocument();
  });

  it('should call onClose when close button is clicked', async () => {
    const handleClose = vi.fn();
    const user = userEvent.setup();

    render(
      <Modal isOpen={true} onClose={handleClose} title="Close Me">
        <p>Content</p>
      </Modal>
    );

    // The X close button is inside the title bar
    const closeButton = screen.getByRole('button');
    await user.click(closeButton);

    expect(handleClose).toHaveBeenCalledTimes(1);
  });

  it('should call onClose when backdrop is clicked', async () => {
    const handleClose = vi.fn();
    const user = userEvent.setup();

    const { container } = render(
      <Modal isOpen={true} onClose={handleClose}>
        <p>Content</p>
      </Modal>
    );

    // The backdrop is the element with bg-black/50
    const backdrop = container.querySelector('.bg-black\\/50') as HTMLElement;
    if (backdrop) {
      await user.click(backdrop);
    }

    expect(handleClose).toHaveBeenCalledTimes(1);
  });

  it('should call onClose when Escape key is pressed', async () => {
    const handleClose = vi.fn();
    const user = userEvent.setup();

    render(
      <Modal isOpen={true} onClose={handleClose}>
        <p>Content</p>
      </Modal>
    );

    await user.keyboard('{Escape}');
    expect(handleClose).toHaveBeenCalledTimes(1);
  });

  it('should apply custom className to modal container', () => {
    render(
      <Modal isOpen={true} onClose={vi.fn()} className="max-w-lg">
        <p>Content</p>
      </Modal>
    );
    // The modal dialog should have the custom class
    const modal = screen.getByText('Content').closest('.max-w-lg');
    expect(modal).toBeInTheDocument();
  });

  it('should prevent body scroll when open', () => {
    render(
      <Modal isOpen={true} onClose={vi.fn()}>
        <p>Content</p>
      </Modal>
    );
    expect(document.body.style.overflow).toBe('hidden');
  });

  it('should restore body scroll when closed', () => {
    const { rerender } = render(
      <Modal isOpen={true} onClose={vi.fn()}>
        <p>Content</p>
      </Modal>
    );

    rerender(
      <Modal isOpen={false} onClose={vi.fn()}>
        <p>Content</p>
      </Modal>
    );

    expect(document.body.style.overflow).toBe('');
  });
});
