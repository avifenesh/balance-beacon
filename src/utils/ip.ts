import { NextRequest } from 'next/server'

/**
 * Get client IP address from request headers
 * Prioritizes request.ip (Next.js trusted proxy) then x-forwarded-for
 */
export function getClientIp(request: NextRequest): string {
  // In Next.js, request.ip is available if trusted proxy is configured
  // This is the most reliable source when deployed on Vercel or similar platforms
  const ip = (request as unknown as { ip?: string }).ip
  if (ip) return ip

  // Fallback to x-forwarded-for header
  // This header can contain multiple IPs (client, proxy1, proxy2...)
  // We want the first one (the client)
  const forwardedFor = request.headers.get('x-forwarded-for')
  if (forwardedFor) {
    return forwardedFor.split(',')[0].trim()
  }

  // Fallback for local development or direct connection
  return '127.0.0.1'
}
