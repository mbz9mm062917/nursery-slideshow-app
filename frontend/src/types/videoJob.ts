export interface VideoJob {
  jobId: string
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  progress: number
  errorMessage: string | null
  downloadUrl: string | null
}
