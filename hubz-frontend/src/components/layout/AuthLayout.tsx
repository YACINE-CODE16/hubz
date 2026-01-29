import type { ReactNode } from 'react';

interface AuthLayoutProps {
  children: ReactNode;
}

export default function AuthLayout({ children }: AuthLayoutProps) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-light-base via-blue-50 to-light-base dark:from-dark-base dark:via-dark-card dark:to-dark-base px-4">
      {children}
    </div>
  );
}
