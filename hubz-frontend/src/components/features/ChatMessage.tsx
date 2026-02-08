import { Bot, User, ExternalLink, AlertCircle, CheckCircle, Sparkles } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { cn } from '../../lib/utils';
import type { ChatMessage as ChatMessageType, QuickAction } from '../../types/chatbot';

interface ChatMessageProps {
  message: ChatMessageType;
  onQuickAction?: (action: QuickAction) => void;
}

function formatTime(date: Date): string {
  return date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
}

export default function ChatMessage({ message, onQuickAction }: ChatMessageProps) {
  const navigate = useNavigate();
  const isUser = message.role === 'user';
  const response = message.response;

  const handleQuickAction = (action: QuickAction) => {
    if (action.action === 'navigate' && action.url) {
      navigate(action.url);
    } else if (onQuickAction) {
      onQuickAction(action);
    }
  };

  const handleActionUrlClick = () => {
    if (response?.actionUrl) {
      navigate(response.actionUrl);
    }
  };

  return (
    <div
      className={cn(
        'flex gap-3 p-3',
        isUser ? 'flex-row-reverse' : 'flex-row'
      )}
    >
      {/* Avatar */}
      <div
        className={cn(
          'flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center',
          isUser
            ? 'bg-accent text-white'
            : 'bg-purple-500/20 text-purple-500'
        )}
      >
        {isUser ? (
          <User className="h-4 w-4" />
        ) : (
          <Bot className="h-4 w-4" />
        )}
      </div>

      {/* Message Content */}
      <div
        className={cn(
          'flex flex-col max-w-[80%] gap-2',
          isUser ? 'items-end' : 'items-start'
        )}
      >
        {/* Message Bubble */}
        <div
          className={cn(
            'rounded-xl px-4 py-2.5',
            isUser
              ? 'bg-accent text-white rounded-br-sm'
              : 'bg-light-hover dark:bg-dark-hover text-gray-900 dark:text-gray-100 rounded-bl-sm'
          )}
        >
          <p className="text-sm whitespace-pre-wrap">{message.content}</p>
        </div>

        {/* Bot Response Details */}
        {!isUser && response && (
          <div className="w-full space-y-2">
            {/* Status Indicator */}
            {response.actionExecuted && !response.errorMessage && (
              <div className="flex items-center gap-1.5 text-xs text-green-500">
                <CheckCircle className="h-3.5 w-3.5" />
                <span>Action executee</span>
              </div>
            )}

            {response.errorMessage && (
              <div className="flex items-center gap-1.5 text-xs text-red-500">
                <AlertCircle className="h-3.5 w-3.5" />
                <span>Erreur</span>
              </div>
            )}

            {/* AI Indicator - shows when Ollama was used */}
            {response.usedOllama && (
              <div
                className="flex items-center gap-1.5 text-xs text-purple-500"
                title={response.ollamaModel ? `Powered by ${response.ollamaModel}` : 'Powered by AI'}
              >
                <Sparkles className="h-3.5 w-3.5" />
                <span>Powered by AI{response.ollamaModel ? ` (${response.ollamaModel})` : ''}</span>
              </div>
            )}

            {/* Action URL Button */}
            {response.actionUrl && response.actionExecuted && (
              <button
                onClick={handleActionUrlClick}
                className="flex items-center gap-1.5 text-xs text-accent hover:underline"
              >
                <ExternalLink className="h-3 w-3" />
                Voir le resultat
              </button>
            )}

            {/* Query Results Summary */}
            {response.queryResults && (
              <div className="text-xs text-gray-500 dark:text-gray-400 bg-light-base dark:bg-dark-base rounded-lg p-2">
                {response.queryResults.totalCount} resultat(s)
              </div>
            )}

            {/* Quick Actions */}
            {response.quickActions && response.quickActions.length > 0 && (
              <div className="flex flex-wrap gap-2">
                {response.quickActions.map((action, index) => (
                  <button
                    key={index}
                    onClick={() => handleQuickAction(action)}
                    className="text-xs px-3 py-1.5 rounded-full bg-light-base dark:bg-dark-base text-gray-700 dark:text-gray-300 hover:bg-light-hover dark:hover:bg-dark-hover border border-gray-200 dark:border-white/10 transition-colors"
                  >
                    {action.label}
                  </button>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Timestamp */}
        <span className="text-[10px] text-gray-400 dark:text-gray-500">
          {formatTime(message.timestamp)}
        </span>
      </div>
    </div>
  );
}
