export interface NoteAttachment {
  id: string;
  noteId: string;
  fileName: string;
  originalFileName: string;
  fileSize: number;
  contentType: string;
  uploadedBy: string;
  uploadedAt: string;
}
