import { useState, useEffect } from 'react';
import { MessageSquare, Send, Edit2, Trash2, CornerDownRight, X, MoreVertical } from 'lucide-react';
import toast from 'react-hot-toast';
import { commentService } from '../../services/comment.service';
import { mentionService } from '../../services/mention.service';
import type { TaskComment } from '../../types/task';
import type { MentionableUser } from '../../types/mention';
import Button from '../ui/Button';
import MentionInput from '../ui/MentionInput';
import MentionText from '../ui/MentionText';
import { cn } from '../../lib/utils';

interface TaskCommentsProps {
  taskId: string;
  organizationId: string;
}

export default function TaskComments({ taskId, organizationId }: TaskCommentsProps) {
  const [comments, setComments] = useState<TaskComment[]>([]);
  const [loading, setLoading] = useState(true);
  const [newComment, setNewComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [replyingTo, setReplyingTo] = useState<string | null>(null);
  const [replyContent, setReplyContent] = useState('');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editContent, setEditContent] = useState('');
  const [mentionableUsers, setMentionableUsers] = useState<MentionableUser[]>([]);

  useEffect(() => {
    loadComments();
    loadMentionableUsers();
  }, [taskId, organizationId]);

  const loadComments = async () => {
    try {
      setLoading(true);
      const data = await commentService.getByTask(taskId);
      setComments(data);
    } catch (error) {
      console.error('Failed to load comments:', error);
      toast.error('Erreur lors du chargement des commentaires');
    } finally {
      setLoading(false);
    }
  };

  const loadMentionableUsers = async () => {
    try {
      const users = await mentionService.getMentionableUsers(organizationId);
      setMentionableUsers(users);
    } catch (error) {
      console.error('Failed to load mentionable users:', error);
      // Don't show error toast for this, it's not critical
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newComment.trim()) return;

    setSubmitting(true);
    try {
      await commentService.create(taskId, { content: newComment.trim() });
      setNewComment('');
      await loadComments();
      toast.success('Commentaire ajoute');
    } catch (error) {
      console.error('Failed to create comment:', error);
      toast.error('Erreur lors de la creation du commentaire');
    } finally {
      setSubmitting(false);
    }
  };

  const handleReply = async (parentCommentId: string) => {
    if (!replyContent.trim()) return;

    setSubmitting(true);
    try {
      await commentService.create(taskId, {
        content: replyContent.trim(),
        parentCommentId,
      });
      setReplyingTo(null);
      setReplyContent('');
      await loadComments();
      toast.success('Reponse ajoutee');
    } catch (error) {
      console.error('Failed to reply:', error);
      toast.error('Erreur lors de la reponse');
    } finally {
      setSubmitting(false);
    }
  };

  const handleEdit = async (commentId: string) => {
    if (!editContent.trim()) return;

    try {
      await commentService.update(taskId, commentId, { content: editContent.trim() });
      setEditingId(null);
      setEditContent('');
      await loadComments();
      toast.success('Commentaire modifie');
    } catch (error) {
      console.error('Failed to edit comment:', error);
      toast.error('Erreur lors de la modification');
    }
  };

  const handleDelete = async (commentId: string) => {
    if (!confirm('Supprimer ce commentaire ?')) return;

    try {
      await commentService.delete(taskId, commentId);
      await loadComments();
      toast.success('Commentaire supprime');
    } catch (error) {
      console.error('Failed to delete comment:', error);
      toast.error('Erreur lors de la suppression');
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'A l\'instant';
    if (diffMins < 60) return `Il y a ${diffMins} min`;
    if (diffHours < 24) return `Il y a ${diffHours}h`;
    if (diffDays < 7) return `Il y a ${diffDays}j`;
    return date.toLocaleDateString('fr-FR');
  };

  const CommentItem = ({ comment, depth = 0 }: { comment: TaskComment; depth?: number }) => {
    const [showActions, setShowActions] = useState(false);
    const isEditing = editingId === comment.id;
    const isReplying = replyingTo === comment.id;

    return (
      <div className={cn('group', depth > 0 && 'ml-6 mt-3')}>
        <div className="relative">
          {depth > 0 && (
            <div className="absolute -left-4 top-0 h-full w-px bg-gray-200 dark:bg-gray-700" />
          )}

          <div className="rounded-lg bg-gray-50 dark:bg-dark-hover p-3">
            {/* Header */}
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <div className="h-7 w-7 rounded-full bg-accent/20 flex items-center justify-center">
                  <span className="text-xs font-medium text-accent">
                    {comment.authorName.charAt(0).toUpperCase()}
                  </span>
                </div>
                <div>
                  <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
                    {comment.authorName}
                  </span>
                  <span className="ml-2 text-xs text-gray-500 dark:text-gray-400">
                    {formatDate(comment.createdAt)}
                    {comment.edited && ' (modifie)'}
                  </span>
                </div>
              </div>

              {/* Actions menu */}
              <div className="relative">
                <button
                  onClick={() => setShowActions(!showActions)}
                  className="p-1 rounded hover:bg-gray-200 dark:hover:bg-gray-600 opacity-0 group-hover:opacity-100 transition-opacity"
                >
                  <MoreVertical className="h-4 w-4 text-gray-500" />
                </button>

                {showActions && (
                  <>
                    <div
                      className="fixed inset-0 z-10"
                      onClick={() => setShowActions(false)}
                    />
                    <div className="absolute right-0 top-full mt-1 z-20 bg-white dark:bg-dark-card rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 py-1 min-w-[120px]">
                      <button
                        onClick={() => {
                          setEditingId(comment.id);
                          setEditContent(comment.content);
                          setShowActions(false);
                        }}
                        className="w-full flex items-center gap-2 px-3 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-dark-hover"
                      >
                        <Edit2 className="h-4 w-4" />
                        Modifier
                      </button>
                      <button
                        onClick={() => {
                          handleDelete(comment.id);
                          setShowActions(false);
                        }}
                        className="w-full flex items-center gap-2 px-3 py-2 text-sm text-error hover:bg-error/10"
                      >
                        <Trash2 className="h-4 w-4" />
                        Supprimer
                      </button>
                    </div>
                  </>
                )}
              </div>
            </div>

            {/* Content */}
            {isEditing ? (
              <div className="space-y-2">
                <MentionInput
                  value={editContent}
                  onChange={setEditContent}
                  mentionableUsers={mentionableUsers}
                  placeholder="Modifier le commentaire..."
                  rows={2}
                />
                <div className="flex gap-2">
                  <Button size="sm" onClick={() => handleEdit(comment.id)}>
                    Enregistrer
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => { setEditingId(null); setEditContent(''); }}
                  >
                    Annuler
                  </Button>
                </div>
              </div>
            ) : (
              <div className="text-sm text-gray-700 dark:text-gray-300">
                <MentionText content={comment.content} organizationId={organizationId} />
              </div>
            )}

            {/* Reply button */}
            {!isEditing && depth < 2 && (
              <button
                onClick={() => {
                  setReplyingTo(isReplying ? null : comment.id);
                  setReplyContent('');
                }}
                className="mt-2 flex items-center gap-1 text-xs text-gray-500 hover:text-accent transition-colors"
              >
                <CornerDownRight className="h-3 w-3" />
                Repondre
              </button>
            )}

            {/* Reply form */}
            {isReplying && (
              <div className="mt-3 space-y-2">
                <MentionInput
                  value={replyContent}
                  onChange={setReplyContent}
                  mentionableUsers={mentionableUsers}
                  placeholder="Ecrire une reponse... (tapez @ pour mentionner)"
                  rows={2}
                  autoFocus
                />
                <div className="flex gap-2">
                  <Button
                    size="sm"
                    onClick={() => handleReply(comment.id)}
                    loading={submitting}
                  >
                    <Send className="h-3 w-3" />
                    Repondre
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => { setReplyingTo(null); setReplyContent(''); }}
                  >
                    <X className="h-3 w-3" />
                    Annuler
                  </Button>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Nested replies */}
        {comment.replies && comment.replies.length > 0 && (
          <div className="space-y-2">
            {comment.replies.map((reply) => (
              <CommentItem key={reply.id} comment={reply} depth={depth + 1} />
            ))}
          </div>
        )}
      </div>
    );
  };

  const totalComments = comments.reduce((acc, c) => {
    const countReplies = (comment: TaskComment): number => {
      return 1 + (comment.replies?.reduce((sum, r) => sum + countReplies(r), 0) || 0);
    };
    return acc + countReplies(c);
  }, 0);

  return (
    <div className="border-t border-gray-200/50 dark:border-white/10 pt-4 mt-4">
      {/* Header */}
      <div className="flex items-center gap-2 mb-4">
        <MessageSquare className="h-4 w-4 text-gray-500" />
        <h3 className="text-sm font-medium text-gray-900 dark:text-gray-100">
          Commentaires
          {totalComments > 0 && (
            <span className="ml-2 text-xs text-gray-500">({totalComments})</span>
          )}
        </h3>
      </div>

      {/* Hint for @mentions */}
      <p className="text-xs text-gray-500 dark:text-gray-400 mb-2">
        Tapez @ pour mentionner un membre de l'organisation
      </p>

      {/* New comment form */}
      <form onSubmit={handleSubmit} className="mb-4">
        <div className="flex gap-2">
          <div className="flex-1">
            <MentionInput
              value={newComment}
              onChange={setNewComment}
              mentionableUsers={mentionableUsers}
              placeholder="Ecrire un commentaire..."
              rows={2}
            />
          </div>
          <Button
            type="submit"
            size="sm"
            loading={submitting}
            disabled={!newComment.trim()}
            className="self-end"
          >
            <Send className="h-4 w-4" />
          </Button>
        </div>
      </form>

      {/* Comments list */}
      {loading ? (
        <div className="flex justify-center py-4">
          <div className="h-6 w-6 animate-spin rounded-full border-2 border-accent border-t-transparent" />
        </div>
      ) : comments.length === 0 ? (
        <p className="text-center text-sm text-gray-500 dark:text-gray-400 py-4">
          Aucun commentaire pour le moment
        </p>
      ) : (
        <div className="space-y-3 max-h-80 overflow-y-auto">
          {comments.map((comment) => (
            <CommentItem key={comment.id} comment={comment} />
          ))}
        </div>
      )}
    </div>
  );
}
