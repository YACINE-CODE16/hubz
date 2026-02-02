import { useState } from 'react';
import { Download, FileSpreadsheet, FileText, File, ChevronDown } from 'lucide-react';
import toast from 'react-hot-toast';
import Button from '../ui/Button';
import { reportService } from '../../services/report.service';
import type { ReportFormat } from '../../services/report.service';

interface Props {
  type: 'tasks' | 'goals' | 'habits';
  organizationId?: string;
  variant?: 'primary' | 'secondary' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
}

export default function ReportExportButton({ type, organizationId, variant = 'secondary', size = 'md' }: Props) {
  const [isOpen, setIsOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleExport = async (format: ReportFormat) => {
    setIsOpen(false);
    setLoading(true);

    try {
      switch (type) {
        case 'tasks':
          if (!organizationId) {
            toast.error('Organisation requise pour exporter les taches');
            return;
          }
          await reportService.downloadOrganizationTasks(organizationId, format);
          break;
        case 'goals':
          if (organizationId) {
            await reportService.downloadOrganizationGoals(organizationId, format);
          } else {
            await reportService.downloadPersonalGoals(format);
          }
          break;
        case 'habits':
          await reportService.downloadHabits(format);
          break;
      }
      toast.success(`Export ${format.toUpperCase()} telecharge avec succes`);
    } catch (error) {
      console.error('Export error:', error);
      toast.error(`Erreur lors de l'export ${format.toUpperCase()}`);
    } finally {
      setLoading(false);
    }
  };

  const formatOptions: { format: ReportFormat; label: string; icon: React.ReactNode }[] = [
    { format: 'csv', label: 'CSV', icon: <FileText className="h-4 w-4" /> },
    { format: 'excel', label: 'Excel', icon: <FileSpreadsheet className="h-4 w-4" /> },
    { format: 'pdf', label: 'PDF', icon: <File className="h-4 w-4" /> },
  ];

  return (
    <div className="relative">
      <Button
        variant={variant}
        size={size}
        onClick={() => setIsOpen(!isOpen)}
        disabled={loading}
      >
        {loading ? (
          <div className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
        ) : (
          <Download className="h-4 w-4" />
        )}
        Exporter
        <ChevronDown className={`h-4 w-4 transition-transform ${isOpen ? 'rotate-180' : ''}`} />
      </Button>

      {isOpen && (
        <>
          {/* Backdrop */}
          <div
            className="fixed inset-0 z-10"
            onClick={() => setIsOpen(false)}
          />

          {/* Dropdown */}
          <div className="absolute right-0 z-20 mt-2 w-48 rounded-lg border border-gray-200 bg-white py-1 shadow-lg dark:border-gray-700 dark:bg-dark-card">
            {formatOptions.map(({ format, label, icon }) => (
              <button
                key={format}
                onClick={() => handleExport(format)}
                className="flex w-full items-center gap-3 px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-dark-hover"
              >
                {icon}
                <span>Exporter en {label}</span>
              </button>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
