import { useState, useEffect } from 'react';
import { Plus, Trash2, GripVertical, Check } from 'lucide-react';
import type { ChecklistItem, ChecklistProgress } from '../../types/task';
import { checklistService } from '../../services/checklist.service';
import Button from '../ui/Button';
import { cn } from '../../lib/utils';

interface TaskChecklistProps {
  taskId: string;
}

export default function TaskChecklist({ taskId }: TaskChecklistProps) {
  const [checklist, setChecklist] = useState<ChecklistProgress | null>(null);
  const [loading, setLoading] = useState(true);
  const [newItemContent, setNewItemContent] = useState('');
  const [addingItem, setAddingItem] = useState(false);
  const [editingItemId, setEditingItemId] = useState<string | null>(null);
  const [editContent, setEditContent] = useState('');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadChecklist();
  }, [taskId]);

  const loadChecklist = async () => {
    try {
      setLoading(true);
      const data = await checklistService.getChecklist(taskId);
      setChecklist(data);
      setError(null);
    } catch (err) {
      setError('Erreur lors du chargement de la checklist');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleAddItem = async () => {
    if (!newItemContent.trim()) return;

    try {
      setAddingItem(true);
      await checklistService.createItem(taskId, { content: newItemContent.trim() });
      setNewItemContent('');
      await loadChecklist();
    } catch (err) {
      setError('Erreur lors de l\'ajout de l\'element');
      console.error(err);
    } finally {
      setAddingItem(false);
    }
  };

  const handleToggleItem = async (itemId: string) => {
    try {
      await checklistService.toggleItem(taskId, itemId);
      await loadChecklist();
    } catch (err) {
      setError('Erreur lors de la mise a jour');
      console.error(err);
    }
  };

  const handleUpdateItem = async (itemId: string) => {
    if (!editContent.trim()) return;

    try {
      await checklistService.updateItem(taskId, itemId, { content: editContent.trim() });
      setEditingItemId(null);
      setEditContent('');
      await loadChecklist();
    } catch (err) {
      setError('Erreur lors de la mise a jour');
      console.error(err);
    }
  };

  const handleDeleteItem = async (itemId: string) => {
    try {
      await checklistService.deleteItem(taskId, itemId);
      await loadChecklist();
    } catch (err) {
      setError('Erreur lors de la suppression');
      console.error(err);
    }
  };

  const startEditing = (item: ChecklistItem) => {
    setEditingItemId(item.id);
    setEditContent(item.content);
  };

  const cancelEditing = () => {
    setEditingItemId(null);
    setEditContent('');
  };

  if (loading) {
    return (
      <div className="py-4">
        <div className="animate-pulse space-y-2">
          <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-1/4"></div>
          <div className="h-8 bg-gray-200 dark:bg-gray-700 rounded"></div>
          <div className="h-8 bg-gray-200 dark:bg-gray-700 rounded"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {/* Header with progress */}
      <div className="flex items-center justify-between">
        <label className="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">
          Checklist
        </label>
        {checklist && checklist.totalItems > 0 && (
          <span className="text-xs font-medium text-gray-500 dark:text-gray-400">
            {checklist.completedItems}/{checklist.totalItems}
          </span>
        )}
      </div>

      {/* Progress bar */}
      {checklist && checklist.totalItems > 0 && (
        <div className="h-1.5 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
          <div
            className="h-full bg-accent transition-all duration-300"
            style={{ width: `${checklist.completionPercentage}%` }}
          />
        </div>
      )}

      {/* Error message */}
      {error && (
        <div className="rounded-lg bg-error/10 border border-error/20 px-3 py-2 text-xs text-error">
          {error}
        </div>
      )}

      {/* Checklist items */}
      <div className="space-y-1">
        {checklist?.items.map((item) => (
          <div
            key={item.id}
            className={cn(
              'group flex items-center gap-2 rounded-lg px-2 py-1.5 transition-colors',
              'hover:bg-light-hover dark:hover:bg-dark-hover'
            )}
          >
            {/* Drag handle */}
            <GripVertical className="h-3.5 w-3.5 text-gray-400 opacity-0 group-hover:opacity-100 cursor-grab" />

            {/* Checkbox */}
            <button
              onClick={() => handleToggleItem(item.id)}
              className={cn(
                'flex-shrink-0 h-4 w-4 rounded border-2 flex items-center justify-center transition-colors',
                item.completed
                  ? 'bg-accent border-accent text-white'
                  : 'border-gray-300 dark:border-gray-600 hover:border-accent'
              )}
            >
              {item.completed && <Check className="h-3 w-3" />}
            </button>

            {/* Content */}
            {editingItemId === item.id ? (
              <div className="flex-1 flex items-center gap-2">
                <input
                  type="text"
                  value={editContent}
                  onChange={(e) => setEditContent(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') handleUpdateItem(item.id);
                    if (e.key === 'Escape') cancelEditing();
                  }}
                  className="flex-1 bg-transparent border-b border-accent focus:outline-none text-sm text-gray-900 dark:text-gray-100"
                  autoFocus
                />
                <Button size="sm" onClick={() => handleUpdateItem(item.id)}>
                  OK
                </Button>
                <Button size="sm" variant="ghost" onClick={cancelEditing}>
                  Annuler
                </Button>
              </div>
            ) : (
              <span
                onClick={() => startEditing(item)}
                className={cn(
                  'flex-1 text-sm cursor-pointer transition-colors',
                  item.completed
                    ? 'text-gray-400 dark:text-gray-500 line-through'
                    : 'text-gray-700 dark:text-gray-300'
                )}
              >
                {item.content}
              </span>
            )}

            {/* Delete button */}
            {!editingItemId && (
              <button
                onClick={() => handleDeleteItem(item.id)}
                className="opacity-0 group-hover:opacity-100 p-1 text-gray-400 hover:text-error transition-colors"
              >
                <Trash2 className="h-3.5 w-3.5" />
              </button>
            )}
          </div>
        ))}
      </div>

      {/* Add new item */}
      <div className="flex items-center gap-2">
        <input
          type="text"
          value={newItemContent}
          onChange={(e) => setNewItemContent(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') handleAddItem();
          }}
          placeholder="Ajouter un element..."
          className="flex-1 bg-transparent border-b border-gray-200 dark:border-gray-700 focus:border-accent focus:outline-none text-sm text-gray-900 dark:text-gray-100 placeholder:text-gray-400 py-1"
        />
        <Button
          size="sm"
          variant="ghost"
          onClick={handleAddItem}
          loading={addingItem}
          disabled={!newItemContent.trim()}
        >
          <Plus className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}
