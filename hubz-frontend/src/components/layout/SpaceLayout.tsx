import { type ReactNode, useState } from 'react';
import Sidebar, { type SpaceType } from './Sidebar';
import Header from './Header';
import ChatbotPanel from '../features/ChatbotPanel';

interface SpaceLayoutProps {
  spaceType: SpaceType;
  basePath: string;
  title: string;
  color?: string | null;
  logoUrl?: string | null;
  icon?: string | null;
  organizationId?: string;
  children: ReactNode;
}

export default function SpaceLayout({ spaceType, basePath, title, color, logoUrl, icon, organizationId, children }: SpaceLayoutProps) {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="flex h-screen bg-light-base dark:bg-dark-base">
      <Sidebar
        spaceType={spaceType}
        basePath={basePath}
        open={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
        logoUrl={logoUrl}
        color={color}
        icon={icon}
        title={title}
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

      {/* Chatbot Panel */}
      <ChatbotPanel organizationId={organizationId} />
    </div>
  );
}
