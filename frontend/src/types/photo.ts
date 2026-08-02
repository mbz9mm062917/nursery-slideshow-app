export interface Photo {
  id: number
  originalFileName: string
  displayOrder: number
  fileUrl: string
  pageBreakAfter: boolean
  layoutPattern: string | null
  cropShape: string | null
}
