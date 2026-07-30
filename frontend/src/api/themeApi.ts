import { httpClient } from './httpClient'
import type { Theme } from '../types/theme'

export const themeApi = {
  async list(): Promise<Theme[]> {
    const response = await httpClient.get<Theme[]>('/api/themes')
    return response.data
  },
}
