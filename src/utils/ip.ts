import { NextRequest } from 'next/server'

/**
 * Extract the client IP address from the request.
 * Prioritizes request.ip as it is populated by Next.js based on trusted proxy configuration.
 * Falls back to x-forwarded-for header if request.ip is not available.
 */
export function getClientIp(request: NextRequest): string {
  // Use Next.js helper which handles trusted proxies securely
  const nextIp = (request as { ip?: string }).ip
  if (nextIp) {
    return nextIp
  }

  // Fallback for environments where request.ip is not populated (e.g. custom server without trust proxy)
  // Be aware this can be spoofed if not behind a trusted proxy that strips x-forwarded-for
  const forwardedFor = request.headers.get('x-forwarded-for')
  if (forwardedFor) {
    return forwardedFor.split(',')[0].trim()
  }

  return '127.0.0.1'
}
