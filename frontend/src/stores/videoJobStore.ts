import { defineStore } from 'pinia'
import { videoJobApi } from '../api/videoJobApi'
import type { VideoJob } from '../types/videoJob'

export const useVideoJobStore = defineStore('videoJob', {
  state: () => ({
    current: null as VideoJob | null,
  }),
  actions: {
    async start(projectId: string): Promise<VideoJob> {
      const job = await videoJobApi.start(projectId)
      this.current = job
      return job
    },
    async refreshStatus(): Promise<VideoJob | null> {
      if (!this.current) {
        return null
      }
      const job = await videoJobApi.getStatus(this.current.jobId)
      this.current = job
      return job
    },
  },
})
