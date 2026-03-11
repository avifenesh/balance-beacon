import { NextRequest } from 'next/server'
import crypto, { randomBytes } from 'crypto'
import { z } from 'zod'
import { prisma } from '@/lib/prisma'
import { consumeRateLimit } from '@/lib/rate-limit'
import { rateLimitError, validationError, successResponse, serverError } from '@/lib/api-helpers'
import { serverLogger } from '@/lib/server-logger'

const requestResetSchema = z.object({
  email: z.string().email('Invalid email address').max(255),
})

export async function POST(request: NextRequest) {
  try {
    let body
    try {
      body = await request.json()
    } catch {
      return validationError({ body: ['Invalid JSON'] })
    }

    const parsed = requestResetSchema.safeParse(body)
    if (!parsed.success) {
      return validationError(parsed.error.flatten().fieldErrors as Record<string, string[]>)
    }

    const { email } = parsed.data
    const normalizedEmail = email.trim().toLowerCase()

    // Rate limit: 3 per hour per email
    const rateLimit = await consumeRateLimit(normalizedEmail, 'password_reset')
    if (!rateLimit.allowed) {
      return rateLimitError(rateLimit.resetAt)
    }

    // Find user by email
    const user = await prisma.user.findUnique({
      where: { email: normalizedEmail },
    })

    // Always return success to prevent email enumeration
    if (!user) {
      serverLogger.info('Password reset requested for non-existent email', { email: normalizedEmail })
      return successResponse({ message: 'If an account exists with this email, a password reset link will be sent.' })
    }

    // Generate reset token
    const resetToken = randomBytes(32).toString('hex')
    // Hash token before storing (security best practice - prevents token theft if DB is compromised)
    const hashedToken = crypto.createHash('sha256').update(resetToken).digest('hex')
    const resetExpires = new Date(Date.now() + 60 * 60 * 1000) // 1 hour

    // Update user with reset token
    await prisma.user.update({
      where: { id: user.id },
      data: {
        passwordResetToken: hashedToken,
        passwordResetExpires: resetExpires,
      },
    })

    if (process.env.NODE_ENV === 'development') {
      serverLogger.info('Password reset email requested', {
        email: normalizedEmail,
        token: resetToken,
        expires: resetExpires.toISOString(),
      })
    }

    return successResponse({ message: 'If an account exists with this email, a password reset link will be sent.' })
  } catch (error) {
    serverLogger.error('Password reset request failed', { error })
    return serverError('Failed to process request')
  }
}
