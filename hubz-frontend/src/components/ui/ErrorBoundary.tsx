import { Component, type ErrorInfo, type ReactNode } from 'react';
import { AlertTriangle, RefreshCw, Home } from 'lucide-react';
import Button from './Button';

interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    console.error('ErrorBoundary caught an error:', error, errorInfo);
  }

  handleRetry = (): void => {
    this.setState({ hasError: false, error: null });
    window.location.reload();
  };

  handleGoHome = (): void => {
    this.setState({ hasError: false, error: null });
    window.location.href = '/hub';
  };

  render(): ReactNode {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div className="min-h-screen flex flex-col items-center justify-center bg-light-base dark:bg-dark-base px-4">
          <div className="flex flex-col items-center gap-6 max-w-md text-center">
            <div className="relative">
              <div className="absolute inset-0 bg-red-500/20 rounded-full blur-xl" />
              <div className="relative z-10 p-4 bg-red-100 dark:bg-red-900/30 rounded-full">
                <AlertTriangle className="h-12 w-12 text-red-600 dark:text-red-400" />
              </div>
            </div>

            <div className="space-y-2">
              <h1 className="text-xl font-semibold text-gray-900 dark:text-white">
                Une erreur est survenue
              </h1>
              <p className="text-gray-600 dark:text-gray-400 text-sm">
                Nous n'avons pas pu charger cette page. Veuillez reessayer ou retourner a l'accueil.
              </p>
            </div>

            {this.state.error && (
              <div className="w-full p-3 bg-light-card dark:bg-dark-card rounded-lg border border-gray-200 dark:border-gray-700">
                <p className="text-xs font-mono text-gray-500 dark:text-gray-400 break-all">
                  {this.state.error.message}
                </p>
              </div>
            )}

            <div className="flex gap-3">
              <Button variant="secondary" onClick={this.handleGoHome}>
                <Home className="h-4 w-4" />
                Accueil
              </Button>
              <Button onClick={this.handleRetry}>
                <RefreshCw className="h-4 w-4" />
                Reessayer
              </Button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
