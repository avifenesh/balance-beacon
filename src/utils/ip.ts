import { NextRequest } from 'next/server'

/**
 * Get client IP address from request headers.
 *
 * Prioritizes 'x-forwarded-for' header (standard for proxies/load balancers),
 * falls back to 'x-real-ip', then request.ip.
 *
 * @param request NextRequest object
 * @returns Client IP address string
 */
export function getClientIp(request: NextRequest): string {
  // Check x-forwarded-for header (standard for proxies/load balancers)
  const forwardedFor = request.headers.get('x-forwarded-for')
  if (forwardedFor) {
    // The header can contain multiple IPs (client, proxy1, proxy2...).
    // The first one is the original client IP.
    return forwardedFor.split(',')[0].trim()
  }

  // Check x-real-ip header (common in Nginx)
  const realIp = request.headers.get('x-real-ip')
  if (realIp) {
    return realIp.trim()
  }

  // Fallback to Next.js ip property (available on some platforms like Vercel)
  // Type assertion needed as TS might not see .ip on some NextRequest definitions
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  if ((request as any).ip) {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return (request as any).ip
  }

  // Fallback for local development or unknown source
  return '127.0.0.1'
}
