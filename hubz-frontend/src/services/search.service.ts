import api from './api';
import type { SearchResultResponse } from '../types/search';

export const searchService = {
  async search(query: string): Promise<SearchResultResponse> {
    const response = await api.get<SearchResultResponse>('/search', {
      params: { q: query },
    });
    return response.data;
  },
};
