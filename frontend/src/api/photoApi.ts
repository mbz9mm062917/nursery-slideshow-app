import { httpClient } from './httpClient'
import type { Photo } from '../types/photo'

export const photoApi = {
  async list(projectId: string): Promise<Photo[]> {
    const response = await httpClient.get<Photo[]>(`/api/projects/${projectId}/photos`)
    return response.data
  },

  async upload(projectId: string, files: File[]): Promise<Photo[]> {
    const formData = new FormData()
    files.forEach((file) => formData.append('files', file))
    const response = await httpClient.post<Photo[]>(`/api/projects/${projectId}/photos`, formData)
    return response.data
  },

  async remove(photoId: number): Promise<void> {
    await httpClient.delete(`/api/photos/${photoId}`)
  },

  async reorder(projectId: string, photoIds: number[]): Promise<Photo[]> {
    const response = await httpClient.put<Photo[]>(`/api/projects/${projectId}/photos/order`, {
      photoIds,
    })
    return response.data
  },
}
