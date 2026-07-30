import { httpClient } from './httpClient'
import type { VideoJob } from '../types/videoJob'

export const videoJobApi = {
  async start(projectId: string): Promise<VideoJob> {
    const response = await httpClient.post<VideoJob>(`/api/projects/${projectId}/video-jobs`)
    return response.data
  },

  async getStatus(jobId: number): Promise<VideoJob> {
    const response = await httpClient.get<VideoJob>(`/api/video-jobs/${jobId}`)
    return response.data
  },
}
