export interface VideoJob {
  jobId: number
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  progress: number
  errorMessage: string | null
  downloadUrl: string | null
}
