import { useRef, useState } from 'react';
import { UploadCloud } from 'lucide-react';

/**
 * Drag & drop file upload zone. Validates type/size before accepting.
 */
export default function FileDropzone({
  onFile,
  accept = '.pdf,.docx',
  maxSizeMB = 10,
  label = 'Drop your file here or click to browse',
  sublabel = 'PDF or DOCX, up to 10 MB',
}) {
  const [dragging, setDragging] = useState(false);
  const inputRef = useRef(null);

  const validate = (file) => {
    const ext = (file?.name?.split('.').pop() || '').toLowerCase();
    const allowed = accept.split(',').map((a) => a.trim().replace('.', '').toLowerCase());
    if (!allowed.includes(ext)) {
      onFile({ error: `Unsupported file type ".${ext}". Allowed: ${accept}` });
      return;
    }
    if (file.size > maxSizeMB * 1024 * 1024) {
      onFile({ error: `File exceeds ${maxSizeMB} MB limit` });
      return;
    }
    onFile(file);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setDragging(false);
    const file = e.dataTransfer?.files?.[0];
    if (file) validate(file);
  };

  return (
    <div
      className={`flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed px-6 py-12 text-center transition-colors ${
        dragging
          ? 'border-brand-500 bg-brand-50 dark:bg-brand-900/20'
          : 'border-slate-300 dark:border-slate-700 hover:border-brand-400'
      }`}
      onDragOver={(e) => {
        e.preventDefault();
        setDragging(true);
      }}
      onDragLeave={() => setDragging(false)}
      onDrop={handleDrop}
      onClick={() => inputRef.current?.click()}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => e.key === 'Enter' && inputRef.current?.click()}
    >
      <UploadCloud className="mb-3 h-10 w-10 text-brand-600 dark:text-brand-400" />
      <p className="text-sm font-medium">{label}</p>
      <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{sublabel}</p>
      <input
        ref={inputRef}
        type="file"
        accept={accept}
        className="hidden"
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (file) validate(file);
          e.target.value = '';
        }}
      />
    </div>
  );
}
