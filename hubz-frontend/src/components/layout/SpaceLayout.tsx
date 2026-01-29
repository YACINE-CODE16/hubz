import { type ReactNode, useState } from 'react';
import Sidebar, { type SpaceType } from './Sidebar';
import Header from './Header';

interface SpaceLayoutProps {
  spaceType: SpaceType;
  basePath: string;
  title: string;
  color?: string | null;
  children: ReactNode;
}

export default function SpaceLayout({ spaceType, basePath, title, color, children }: SpaceLayoutProps) {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="flex h-screen bg-light-base dark:bg-dark-base">
      <Sidebar
        spaceType={spaceType}
        basePath={basePath}
        open={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
      />

      <div className="flex flex-1 flex-col overflow-hidden">
        <Header
          title={title}
          color={color}
          onMenuToggle={() => setSidebarOpen((prev) => !prev)}
        />

        <main className="flex-1 overflow-auto">
          {children}
        </main>
      </div>
    </div>
  );
}
