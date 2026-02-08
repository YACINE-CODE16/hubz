import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import Input from './Input';

describe('Input', () => {
  it('should render an input element', () => {
    render(<Input placeholder="Enter text" />);
    expect(screen.getByPlaceholderText('Enter text')).toBeInTheDocument();
  });

  it('should render a label when provided', () => {
    render(<Input label="Email" />);
    expect(screen.getByText('Email')).toBeInTheDocument();
  });

  it('should associate label with input via htmlFor/id', () => {
    render(<Input label="Username" />);
    const label = screen.getByText('Username');
    const input = screen.getByLabelText('Username');
    expect(label).toHaveAttribute('for', 'username');
    expect(input).toHaveAttribute('id', 'username');
  });

  it('should use custom id when provided', () => {
    render(<Input label="Name" id="custom-id" />);
    const input = screen.getByLabelText('Name');
    expect(input).toHaveAttribute('id', 'custom-id');
  });

  it('should display error message when error prop is set', () => {
    render(<Input error="This field is required" />);
    expect(screen.getByText('This field is required')).toBeInTheDocument();
  });

  it('should apply error styling when error is present', () => {
    render(<Input error="Required" placeholder="input" />);
    const input = screen.getByPlaceholderText('input');
    expect(input).toHaveClass('border-error');
  });

  it('should render icon when provided', () => {
    render(<Input icon={<span data-testid="icon">@</span>} />);
    expect(screen.getByTestId('icon')).toBeInTheDocument();
  });

  it('should add left padding when icon is present', () => {
    render(<Input icon={<span>@</span>} placeholder="email" />);
    const input = screen.getByPlaceholderText('email');
    expect(input).toHaveClass('pl-10');
  });

  it('should accept user input', async () => {
    const user = userEvent.setup();
    render(<Input placeholder="Type here" />);
    const input = screen.getByPlaceholderText('Type here');

    await user.type(input, 'Hello World');
    expect(input).toHaveValue('Hello World');
  });

  it('should call onChange handler', async () => {
    const handleChange = vi.fn();
    const user = userEvent.setup();

    render(<Input placeholder="input" onChange={handleChange} />);
    await user.type(screen.getByPlaceholderText('input'), 'a');

    expect(handleChange).toHaveBeenCalled();
  });

  it('should forward ref to input element', () => {
    const ref = vi.fn();
    render(<Input ref={ref} />);
    expect(ref).toHaveBeenCalledWith(expect.any(HTMLInputElement));
  });

  it('should merge custom className', () => {
    render(<Input className="my-class" placeholder="input" />);
    const input = screen.getByPlaceholderText('input');
    expect(input).toHaveClass('my-class');
  });

  it('should support different input types', () => {
    render(<Input type="password" placeholder="password" />);
    const input = screen.getByPlaceholderText('password');
    expect(input).toHaveAttribute('type', 'password');
  });

  it('should not render label when not provided', () => {
    const { container } = render(<Input placeholder="no label" />);
    const labels = container.querySelectorAll('label');
    expect(labels).toHaveLength(0);
  });

  it('should not render error text when no error', () => {
    render(<Input placeholder="no error" />);
    const errorElements = document.querySelectorAll('.text-error');
    expect(errorElements).toHaveLength(0);
  });
});
