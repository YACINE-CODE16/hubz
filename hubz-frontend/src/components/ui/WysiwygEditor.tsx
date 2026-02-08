import { useEditor, EditorContent, type Editor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Placeholder from '@tiptap/extension-placeholder';
import Link from '@tiptap/extension-link';
import Image from '@tiptap/extension-image';
import Underline from '@tiptap/extension-underline';
import CodeBlockLowlight from '@tiptap/extension-code-block-lowlight';
import { common, createLowlight } from 'lowlight';
import { useEffect, useCallback, useState } from 'react';
import {
  Bold,
  Italic,
  Underline as UnderlineIcon,
  Strikethrough,
  Heading1,
  Heading2,
  Heading3,
  List,
  ListOrdered,
  Code,
  Quote,
  Link as LinkIcon,
  Image as ImageIcon,
  Eye,
  Edit3,
  Undo,
  Redo,
  Minus,
} from 'lucide-react';
import { cn } from '../../lib/utils';

const lowlight = createLowlight(common);

interface WysiwygEditorProps {
  content: string;
  onChange: (content: string) => void;
  placeholder?: string;
  className?: string;
  minHeight?: string;
}

interface ToolbarButtonProps {
  onClick: () => void;
  isActive?: boolean;
  disabled?: boolean;
  title: string;
  children: React.ReactNode;
}

function ToolbarButton({ onClick, isActive, disabled, title, children }: ToolbarButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      title={title}
      className={cn(
        'p-1.5 rounded transition-colors',
        isActive
          ? 'bg-accent text-white'
          : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700',
        disabled && 'opacity-50 cursor-not-allowed'
      )}
    >
      {children}
    </button>
  );
}

function ToolbarDivider() {
  return <div className="w-px h-6 bg-gray-300 dark:bg-gray-600 mx-1" />;
}

interface MenuBarProps {
  editor: Editor | null;
  isPreviewMode: boolean;
  onTogglePreview: () => void;
}

function MenuBar({ editor, isPreviewMode, onTogglePreview }: MenuBarProps) {
  const [linkUrl, setLinkUrl] = useState('');
  const [showLinkInput, setShowLinkInput] = useState(false);
  const [imageUrl, setImageUrl] = useState('');
  const [showImageInput, setShowImageInput] = useState(false);

  if (!editor) {
    return null;
  }

  const addLink = () => {
    if (linkUrl) {
      editor
        .chain()
        .focus()
        .extendMarkRange('link')
        .setLink({ href: linkUrl })
        .run();
      setLinkUrl('');
      setShowLinkInput(false);
    }
  };

  const addImage = () => {
    if (imageUrl) {
      editor.chain().focus().setImage({ src: imageUrl }).run();
      setImageUrl('');
      setShowImageInput(false);
    }
  };

  const removeLink = () => {
    editor.chain().focus().unsetLink().run();
  };

  return (
    <div className="border-b border-gray-200 dark:border-gray-700 p-2 flex flex-wrap items-center gap-0.5 bg-gray-50 dark:bg-gray-800/50 rounded-t-lg">
      {/* Undo/Redo */}
      <ToolbarButton
        onClick={() => editor.chain().focus().undo().run()}
        disabled={!editor.can().undo()}
        title="Annuler (Ctrl+Z)"
      >
        <Undo className="h-4 w-4" />
      </ToolbarButton>
      <ToolbarButton
        onClick={() => editor.chain().focus().redo().run()}
        disabled={!editor.can().redo()}
        title="Retablir (Ctrl+Y)"
      >
        <Redo className="h-4 w-4" />
      </ToolbarButton>

      <ToolbarDivider />

      {/* Text formatting */}
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleBold().run()}
        isActive={editor.isActive('bold')}
        title="Gras (Ctrl+B)"
      >
        <Bold className="h-4 w-4" />
      </ToolbarButton>
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleItalic().run()}
        isActive={editor.isActive('italic')}
        title="Italique (Ctrl+I)"
      >
        <Italic className="h-4 w-4" />
      </ToolbarButton>
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleUnderline().run()}
        isActive={editor.isActive('underline')}
        title="Souligne (Ctrl+U)"
      >
        <UnderlineIcon className="h-4 w-4" />
      </ToolbarButton>
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleStrike().run()}
        isActive={editor.isActive('strike')}
        title="Barre"
      >
        <Strikethrough className="h-4 w-4" />
      </ToolbarButton>

      <ToolbarDivider />

      {/* Headings */}
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleHeading({ level: 1 }).run()}
        isActive={editor.isActive('heading', { level: 1 })}
        title="Titre 1"
      >
        <Heading1 className="h-4 w-4" />
      </ToolbarButton>
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}
        isActive={editor.isActive('heading', { level: 2 })}
        title="Titre 2"
      >
        <Heading2 className="h-4 w-4" />
      </ToolbarButton>
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleHeading({ level: 3 }).run()}
        isActive={editor.isActive('heading', { level: 3 })}
        title="Titre 3"
      >
        <Heading3 className="h-4 w-4" />
      </ToolbarButton>

      <ToolbarDivider />

      {/* Lists */}
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleBulletList().run()}
        isActive={editor.isActive('bulletList')}
        title="Liste a puces"
      >
        <List className="h-4 w-4" />
      </ToolbarButton>
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleOrderedList().run()}
        isActive={editor.isActive('orderedList')}
        title="Liste numerotee"
      >
        <ListOrdered className="h-4 w-4" />
      </ToolbarButton>

      <ToolbarDivider />

      {/* Code & Quote */}
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleCode().run()}
        isActive={editor.isActive('code')}
        title="Code inline"
      >
        <Code className="h-4 w-4" />
      </ToolbarButton>
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleCodeBlock().run()}
        isActive={editor.isActive('codeBlock')}
        title="Bloc de code"
      >
        <div className="flex items-center">
          <Code className="h-3 w-3" />
          <Code className="h-3 w-3 -ml-1" />
        </div>
      </ToolbarButton>
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleBlockquote().run()}
        isActive={editor.isActive('blockquote')}
        title="Citation"
      >
        <Quote className="h-4 w-4" />
      </ToolbarButton>

      <ToolbarDivider />

      {/* Horizontal Rule */}
      <ToolbarButton
        onClick={() => editor.chain().focus().setHorizontalRule().run()}
        title="Ligne horizontale"
      >
        <Minus className="h-4 w-4" />
      </ToolbarButton>

      <ToolbarDivider />

      {/* Link */}
      <div className="relative">
        <ToolbarButton
          onClick={() => {
            if (editor.isActive('link')) {
              removeLink();
            } else {
              setShowLinkInput(!showLinkInput);
              setShowImageInput(false);
            }
          }}
          isActive={editor.isActive('link')}
          title="Lien"
        >
          <LinkIcon className="h-4 w-4" />
        </ToolbarButton>
        {showLinkInput && (
          <div className="absolute top-full left-0 mt-1 z-50 bg-white dark:bg-dark-card border border-gray-200 dark:border-gray-700 rounded-lg p-2 shadow-lg flex gap-2">
            <input
              type="url"
              value={linkUrl}
              onChange={(e) => setLinkUrl(e.target.value)}
              placeholder="https://..."
              className="text-sm px-2 py-1 border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-dark-card text-gray-900 dark:text-gray-100 w-48"
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  addLink();
                }
                if (e.key === 'Escape') {
                  setShowLinkInput(false);
                }
              }}
              autoFocus
            />
            <button
              type="button"
              onClick={addLink}
              className="text-sm px-2 py-1 bg-accent text-white rounded hover:bg-accent/90"
            >
              OK
            </button>
          </div>
        )}
      </div>

      {/* Image */}
      <div className="relative">
        <ToolbarButton
          onClick={() => {
            setShowImageInput(!showImageInput);
            setShowLinkInput(false);
          }}
          title="Image (URL)"
        >
          <ImageIcon className="h-4 w-4" />
        </ToolbarButton>
        {showImageInput && (
          <div className="absolute top-full left-0 mt-1 z-50 bg-white dark:bg-dark-card border border-gray-200 dark:border-gray-700 rounded-lg p-2 shadow-lg flex gap-2">
            <input
              type="url"
              value={imageUrl}
              onChange={(e) => setImageUrl(e.target.value)}
              placeholder="URL de l'image..."
              className="text-sm px-2 py-1 border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-dark-card text-gray-900 dark:text-gray-100 w-48"
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  addImage();
                }
                if (e.key === 'Escape') {
                  setShowImageInput(false);
                }
              }}
              autoFocus
            />
            <button
              type="button"
              onClick={addImage}
              className="text-sm px-2 py-1 bg-accent text-white rounded hover:bg-accent/90"
            >
              OK
            </button>
          </div>
        )}
      </div>

      {/* Preview Toggle */}
      <div className="ml-auto">
        <ToolbarButton
          onClick={onTogglePreview}
          isActive={isPreviewMode}
          title={isPreviewMode ? 'Mode edition' : 'Mode apercu'}
        >
          {isPreviewMode ? <Edit3 className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
        </ToolbarButton>
      </div>
    </div>
  );
}

export default function WysiwygEditor({
  content,
  onChange,
  placeholder = 'Ecrivez votre contenu ici...',
  className,
  minHeight = '200px',
}: WysiwygEditorProps) {
  const [isPreviewMode, setIsPreviewMode] = useState(false);

  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        codeBlock: false,
      }),
      Underline,
      Placeholder.configure({
        placeholder,
      }),
      Link.configure({
        openOnClick: false,
        HTMLAttributes: {
          class: 'text-accent hover:underline cursor-pointer',
        },
      }),
      Image.configure({
        HTMLAttributes: {
          class: 'max-w-full h-auto rounded-lg my-4',
        },
      }),
      CodeBlockLowlight.configure({
        lowlight,
        HTMLAttributes: {
          class: 'bg-gray-900 text-gray-100 rounded-lg p-4 my-2 overflow-x-auto text-sm font-mono',
        },
      }),
    ],
    content,
    onUpdate: ({ editor }) => {
      onChange(editor.getHTML());
    },
    editorProps: {
      attributes: {
        class: 'prose prose-sm dark:prose-invert max-w-none focus:outline-none',
      },
    },
  });

  // Update editor content when prop changes (for edit mode)
  useEffect(() => {
    if (editor && content !== editor.getHTML()) {
      editor.commands.setContent(content);
    }
  }, [content, editor]);

  const togglePreview = useCallback(() => {
    setIsPreviewMode((prev) => !prev);
    if (editor) {
      editor.setEditable(isPreviewMode);
    }
  }, [editor, isPreviewMode]);

  return (
    <div className={cn('rounded-lg border border-gray-300 dark:border-gray-600 overflow-hidden', className)}>
      <MenuBar editor={editor} isPreviewMode={isPreviewMode} onTogglePreview={togglePreview} />
      <div
        className={cn(
          'bg-white dark:bg-dark-card p-4 overflow-y-auto',
          isPreviewMode && 'bg-gray-50 dark:bg-gray-800/50'
        )}
        style={{ minHeight }}
      >
        <EditorContent editor={editor} />
      </div>
      <style>{`
        .ProseMirror {
          outline: none;
        }
        .ProseMirror p.is-editor-empty:first-child::before {
          content: attr(data-placeholder);
          float: left;
          color: #9ca3af;
          pointer-events: none;
          height: 0;
        }
        .ProseMirror h1 {
          font-size: 1.75rem;
          font-weight: 700;
          margin-top: 1rem;
          margin-bottom: 0.5rem;
        }
        .ProseMirror h2 {
          font-size: 1.5rem;
          font-weight: 600;
          margin-top: 0.875rem;
          margin-bottom: 0.5rem;
        }
        .ProseMirror h3 {
          font-size: 1.25rem;
          font-weight: 600;
          margin-top: 0.75rem;
          margin-bottom: 0.5rem;
        }
        .ProseMirror p {
          margin: 0.5rem 0;
        }
        .ProseMirror ul,
        .ProseMirror ol {
          padding-left: 1.5rem;
          margin: 0.5rem 0;
        }
        .ProseMirror li {
          margin: 0.25rem 0;
        }
        .ProseMirror blockquote {
          border-left: 3px solid #3B82F6;
          padding-left: 1rem;
          margin: 0.5rem 0;
          font-style: italic;
          color: #6b7280;
        }
        .dark .ProseMirror blockquote {
          color: #9ca3af;
        }
        .ProseMirror code {
          background-color: #f3f4f6;
          padding: 0.125rem 0.25rem;
          border-radius: 0.25rem;
          font-family: ui-monospace, monospace;
          font-size: 0.875em;
        }
        .dark .ProseMirror code {
          background-color: #374151;
        }
        .ProseMirror pre {
          background-color: #111827;
          color: #f9fafb;
          padding: 1rem;
          border-radius: 0.5rem;
          margin: 0.5rem 0;
          overflow-x: auto;
        }
        .ProseMirror pre code {
          background: none;
          padding: 0;
          font-size: 0.875rem;
        }
        .ProseMirror hr {
          border: none;
          border-top: 1px solid #e5e7eb;
          margin: 1rem 0;
        }
        .dark .ProseMirror hr {
          border-top-color: #374151;
        }
        .ProseMirror img {
          max-width: 100%;
          height: auto;
          border-radius: 0.5rem;
          margin: 1rem 0;
        }
        .ProseMirror a {
          color: #3B82F6;
          text-decoration: none;
        }
        .ProseMirror a:hover {
          text-decoration: underline;
        }
      `}</style>
    </div>
  );
}

export { WysiwygEditor };
