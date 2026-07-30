import { httpClient } from '../../api/httpClient'
import type { Project } from '../../types/project'

/**
 * プロジェクト一覧・削除は本番ウィザードUIでは使用しない開発・デバッグ専用機能のため、
 * 本番の api/projectApi.ts とは分離している。
 */
export const devProjectApi = {
  async list(): Promise<Project[]> {
    const response = await httpClient.get<Project[]>('/api/dev/projects')
    return response.data
  },

  async remove(projectId: string): Promise<void> {
    await httpClient.delete(`/api/dev/projects/${projectId}`)
  },
}
