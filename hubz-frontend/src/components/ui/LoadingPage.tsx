import { Loader2 } from 'lucide-react';

interface LoadingPageProps {
  message?: string;
}

const LoadingPage = ({ message = 'Chargement...' }: LoadingPageProps) => {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-light-base dark:bg-dark-base">
      <div className="flex flex-col items-center gap-4">
        <div className="relative">
          <div className="absolute inset-0 bg-accent/20 rounded-full blur-xl animate-pulse" />
          <Loader2 className="h-12 w-12 text-accent animate-spin relative z-10" />
        </div>
        <p className="text-gray-600 dark:text-gray-400 font-medium text-sm">
          {message}
        </p>
      </div>
    </div>
  );
};

export default LoadingPage;
