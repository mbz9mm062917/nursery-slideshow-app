import { defineStore } from 'pinia'
import { projectApi } from '../api/projectApi'
import type { Project, ProjectPatchInput } from '../types/project'

export const useProjectStore = defineStore('project', {
  state: () => ({
    current: null as Project | null,
  }),
  actions: {
    async createProject(): Promise<Project> {
      const project = await projectApi.create()
      this.current = project
      return project
    },
    async loadProject(projectId: string): Promise<Project> {
      const project = await projectApi.get(projectId)
      this.current = project
      return project
    },
    async ensureLoaded(projectId: string): Promise<Project> {
      if (this.current?.id === projectId) {
        return this.current
      }
      return this.loadProject(projectId)
    },
    async patchProject(projectId: string, input: ProjectPatchInput): Promise<Project> {
      const project = await projectApi.patch(projectId, input)
      this.current = project
      return project
    },
  },
})
