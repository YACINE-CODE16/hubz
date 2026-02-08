import { useState, useRef, useEffect, useCallback } from 'react';
import { Bot, X, Send, Loader2, Trash2, Minimize2, Maximize2, Sparkles } from 'lucide-react';
import { cn } from '../../lib/utils';
import { chatbotService } from '../../services/chatbot.service';
import type { ChatMessage as ChatMessageType, QuickAction, ChatbotResponse } from '../../types/chatbot';
import ChatMessage from './ChatMessage';

const LOCAL_STORAGE_KEY = 'hubz_chatbot_history';
const MAX_MESSAGES = 50;

const EXAMPLE_COMMANDS = [
  'Creer une tache: finir le rapport',
  'J\'ai un rdv demain a 14h',
  'Quelles sont mes taches?',
  'Mes statistiques',
];

interface ChatbotPanelProps {
  organizationId?: string;
}

export default function ChatbotPanel({ organizationId }: ChatbotPanelProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const [messages, setMessages] = useState<ChatMessageType[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isAiActive, setIsAiActive] = useState(false);
  const [aiModel, setAiModel] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Load messages from localStorage on mount
  useEffect(() => {
    const stored = localStorage.getItem(LOCAL_STORAGE_KEY);
    if (stored) {
      try {
        const parsed = JSON.parse(stored);
        const messagesWithDates = parsed.map((m: ChatMessageType) => ({
          ...m,
          timestamp: new Date(m.timestamp),
        }));
        setMessages(messagesWithDates);
      } catch {
        // Ignore parse errors
      }
    }
  }, []);

  // Save messages to localStorage whenever they change
  useEffect(() => {
    if (messages.length > 0) {
      const toStore = messages.slice(-MAX_MESSAGES);
      localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(toStore));
    }
  }, [messages]);

  // Scroll to bottom when messages change
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Focus input when panel opens
  useEffect(() => {
    if (isOpen) {
      setTimeout(() => inputRef.current?.focus(), 100);
    }
  }, [isOpen]);

  const addMessage = useCallback((role: 'user' | 'bot', content: string, response?: ChatbotResponse) => {
    const newMessage: ChatMessageType = {
      id: crypto.randomUUID(),
      role,
      content,
      timestamp: new Date(),
      response,
    };
    setMessages((prev) => [...prev.slice(-MAX_MESSAGES + 1), newMessage]);
  }, []);

  const handleSend = async () => {
    const message = inputValue.trim();
    if (!message || isLoading) return;

    setInputValue('');
    addMessage('user', message);
    setIsLoading(true);

    try {
      const response = await chatbotService.sendMessage({
        message,
        organizationId,
      });
      addMessage('bot', response.confirmationText, response);
      // Update AI status based on response
      if (response.usedOllama !== undefined) {
        setIsAiActive(response.usedOllama);
        setAiModel(response.ollamaModel || null);
      }
    } catch (error) {
      console.error('Chatbot error:', error);
      addMessage('bot', 'Desole, une erreur s\'est produite. Veuillez reessayer.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleQuickAction = (action: QuickAction) => {
    if (action.action === 'create_task') {
      setInputValue('Creer une tache: ');
      inputRef.current?.focus();
    } else if (action.action === 'create_event') {
      setInputValue('J\'ai un rdv ');
      inputRef.current?.focus();
    } else if (action.action === 'create_goal') {
      setInputValue('Objectif: ');
      inputRef.current?.focus();
    } else if (action.action === 'create_note') {
      setInputValue('Note: ');
      inputRef.current?.focus();
    } else if (action.action === 'query_tasks') {
      setInputValue('Quelles sont mes taches?');
      handleSend();
    }
  };

  const handleExampleClick = (example: string) => {
    setInputValue(example);
    inputRef.current?.focus();
  };

  const clearHistory = () => {
    setMessages([]);
    localStorage.removeItem(LOCAL_STORAGE_KEY);
  };

  return (
    <>
      {/* Floating Button */}
      {!isOpen && (
        <button
          onClick={() => setIsOpen(true)}
          className="fixed bottom-6 right-6 z-50 w-14 h-14 rounded-full bg-purple-500 text-white shadow-lg hover:bg-purple-600 hover:scale-105 transition-all flex items-center justify-center group"
          title="Assistant Hubz"
        >
          <Bot className="h-6 w-6" />
          <span className="absolute -top-10 right-0 bg-dark-card text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap">
            Assistant Hubz
          </span>
        </button>
      )}

      {/* Chat Panel */}
      {isOpen && (
        <div
          className={cn(
            'fixed z-50 bg-light-card dark:bg-dark-card border border-gray-200 dark:border-white/10 shadow-2xl flex flex-col transition-all duration-300',
            isExpanded
              ? 'inset-4 rounded-2xl'
              : 'bottom-6 right-6 w-96 h-[32rem] rounded-2xl'
          )}
        >
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200 dark:border-white/10">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-full bg-purple-500/20 flex items-center justify-center">
                <Bot className="h-4 w-4 text-purple-500" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="font-semibold text-gray-900 dark:text-gray-100 text-sm">
                    Assistant Hubz
                  </h3>
                  {isAiActive && (
                    <span
                      className="flex items-center gap-1 px-1.5 py-0.5 text-[10px] font-medium bg-purple-500/10 text-purple-500 rounded-full"
                      title={aiModel ? `Powered by ${aiModel}` : 'AI Mode Active'}
                    >
                      <Sparkles className="h-2.5 w-2.5" />
                      AI
                    </span>
                  )}
                </div>
                <p className="text-[10px] text-gray-500 dark:text-gray-400">
                  {isAiActive
                    ? `Powered by ${aiModel || 'AI'}`
                    : 'Tapez une commande en langage naturel'}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-1">
              <button
                onClick={clearHistory}
                className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-light-hover dark:hover:bg-dark-hover transition-colors"
                title="Effacer l'historique"
              >
                <Trash2 className="h-4 w-4" />
              </button>
              <button
                onClick={() => setIsExpanded(!isExpanded)}
                className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-light-hover dark:hover:bg-dark-hover transition-colors"
                title={isExpanded ? 'Reduire' : 'Agrandir'}
              >
                {isExpanded ? (
                  <Minimize2 className="h-4 w-4" />
                ) : (
                  <Maximize2 className="h-4 w-4" />
                )}
              </button>
              <button
                onClick={() => setIsOpen(false)}
                className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-light-hover dark:hover:bg-dark-hover transition-colors"
                title="Fermer"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          </div>

          {/* Messages */}
          <div className="flex-1 overflow-y-auto">
            {messages.length === 0 ? (
              <div className="flex flex-col items-center justify-center h-full p-6 text-center">
                <div className="w-16 h-16 rounded-full bg-purple-500/10 flex items-center justify-center mb-4">
                  <Bot className="h-8 w-8 text-purple-500" />
                </div>
                <h4 className="font-medium text-gray-900 dark:text-gray-100 mb-2">
                  Bienvenue !
                </h4>
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                  Je peux vous aider a creer des taches, evenements, objectifs, et plus encore.
                </p>
                <div className="space-y-2 w-full max-w-xs">
                  <p className="text-xs text-gray-400 dark:text-gray-500 mb-1">
                    Exemples de commandes:
                  </p>
                  {EXAMPLE_COMMANDS.map((cmd, i) => (
                    <button
                      key={i}
                      onClick={() => handleExampleClick(cmd)}
                      className="w-full text-left text-xs px-3 py-2 rounded-lg bg-light-base dark:bg-dark-base text-gray-600 dark:text-gray-400 hover:bg-light-hover dark:hover:bg-dark-hover border border-gray-200 dark:border-white/5 transition-colors"
                    >
                      "{cmd}"
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              <div className="py-2">
                {messages.map((msg) => (
                  <ChatMessage
                    key={msg.id}
                    message={msg}
                    onQuickAction={handleQuickAction}
                  />
                ))}
                {isLoading && (
                  <div className="flex gap-3 p-3">
                    <div className="w-8 h-8 rounded-full bg-purple-500/20 flex items-center justify-center">
                      <Bot className="h-4 w-4 text-purple-500" />
                    </div>
                    <div className="flex items-center gap-2 text-gray-500 dark:text-gray-400">
                      <Loader2 className="h-4 w-4 animate-spin" />
                      <span className="text-sm">En train d'ecrire...</span>
                    </div>
                  </div>
                )}
                <div ref={messagesEndRef} />
              </div>
            )}
          </div>

          {/* Input */}
          <div className="p-3 border-t border-gray-200 dark:border-white/10">
            <div className="flex items-center gap-2">
              <input
                ref={inputRef}
                type="text"
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Tapez votre message..."
                className="flex-1 bg-light-base dark:bg-dark-base border border-gray-200 dark:border-white/10 rounded-xl px-4 py-2.5 text-sm text-gray-900 dark:text-gray-100 placeholder-gray-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500/50 focus:border-purple-500"
                disabled={isLoading}
              />
              <button
                onClick={handleSend}
                disabled={!inputValue.trim() || isLoading}
                className={cn(
                  'w-10 h-10 rounded-xl flex items-center justify-center transition-colors',
                  inputValue.trim() && !isLoading
                    ? 'bg-purple-500 text-white hover:bg-purple-600'
                    : 'bg-gray-200 dark:bg-dark-hover text-gray-400 cursor-not-allowed'
                )}
              >
                {isLoading ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Send className="h-4 w-4" />
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
