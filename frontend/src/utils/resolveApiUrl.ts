export function resolveApiUrl(path: string): string {
  return `${import.meta.env.VITE_API_BASE_URL}${path}`
}
