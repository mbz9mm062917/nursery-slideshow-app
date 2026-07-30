export interface Project {
  id: string
  title: string | null
  themeCode: string | null
  bgmCode: string | null
  slideDurationSec: number | null
  photoCount: number
  createdAt: string
}

export interface ProjectPatchInput {
  title?: string
  themeCode?: string
  bgmCode?: string
  slideDurationSec?: number
}
