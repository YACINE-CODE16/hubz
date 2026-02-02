import api from './api';

export type ReportFormat = 'csv' | 'excel' | 'pdf';
export type ReportType = 'tasks' | 'goals' | 'habits';

const formatExtensions: Record<ReportFormat, string> = {
  csv: '.csv',
  excel: '.xlsx',
  pdf: '.pdf',
};

const formatMimeTypes: Record<ReportFormat, string> = {
  csv: 'text/csv',
  excel: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  pdf: 'application/pdf',
};

export const reportService = {
  async downloadOrganizationTasks(organizationId: string, format: ReportFormat): Promise<void> {
    const endpoint = `/reports/organizations/${organizationId}/tasks/${format}`;
    await downloadFile(endpoint, `tasks${formatExtensions[format]}`, formatMimeTypes[format]);
  },

  async downloadOrganizationGoals(organizationId: string, format: ReportFormat): Promise<void> {
    const endpoint = `/reports/organizations/${organizationId}/goals/${format}`;
    await downloadFile(endpoint, `goals${formatExtensions[format]}`, formatMimeTypes[format]);
  },

  async downloadPersonalGoals(format: ReportFormat): Promise<void> {
    const endpoint = `/reports/users/me/goals/${format}`;
    await downloadFile(endpoint, `personal_goals${formatExtensions[format]}`, formatMimeTypes[format]);
  },

  async downloadHabits(format: ReportFormat): Promise<void> {
    const endpoint = `/reports/users/me/habits/${format}`;
    await downloadFile(endpoint, `habits${formatExtensions[format]}`, formatMimeTypes[format]);
  },
};

async function downloadFile(endpoint: string, filename: string, mimeType: string): Promise<void> {
  const response = await api.get(endpoint, {
    responseType: 'blob',
  });

  const blob = new Blob([response.data], { type: mimeType });
  const url = window.URL.createObjectURL(blob);

  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();

  // Cleanup
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}
