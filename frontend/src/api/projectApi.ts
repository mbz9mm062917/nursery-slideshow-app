import { httpClient } from './httpClient'
import type { Project, ProjectPatchInput } from '../types/project'

export const projectApi = {
  async create(): Promise<Project> {
    const response = await httpClient.post<Project>('/api/projects')
    return response.data
  },

  async get(projectId: string): Promise<Project> {
    const response = await httpClient.get<Project>(`/api/projects/${projectId}`)
    return response.data
  },

  async patch(projectId: string, input: ProjectPatchInput): Promise<Project> {
    const response = await httpClient.patch<Project>(`/api/projects/${projectId}`, input)
    return response.data
  },
}
