import { httpClient } from './httpClient'
import type { Bgm } from '../types/bgm'

export const bgmApi = {
  async list(): Promise<Bgm[]> {
    const response = await httpClient.get<Bgm[]>('/api/bgms')
    return response.data
  },
}
