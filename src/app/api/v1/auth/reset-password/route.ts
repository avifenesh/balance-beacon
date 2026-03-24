import { NextRequest } from 'next/server'
import { createHash } from 'crypto'
import bcrypt from 'bcryptjs'
import { z } from 'zod'
import { prisma } from '@/lib/prisma'
import { validationError, successResponse, serverError, authError } from '@/lib/api-helpers'
import { serverLogger } from '@/lib/server-logger'

const resetPasswordSchema = z.object({
  token: z.string().min(1, 'Token is required').max(128, 'Invalid token'),
  newPassword: z
    .string()
    .min(8, 'Password must be at least 8 characters')
    .max(128, 'Password must be at most 128 characters')
    .regex(/[a-z]/, 'Password must contain at least one lowercase letter')
    .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
    .regex(/[0-9]/, 'Password must contain at least one number'),
})

export async function POST(request: NextRequest) {
  try {
    let body
    try {
      body = await request.json()
    } catch {
      return validationError({ body: ['Invalid JSON'] })
    }

    const parsed = resetPasswordSchema.safeParse(body)
    if (!parsed.success) {
      return validationError(parsed.error.flatten().fieldErrors as Record<string, string[]>)
    }

    const { token, newPassword } = parsed.data

    // Security: Hash the incoming token to match the stored hash
    const hashedToken = createHash('sha256').update(token).digest('hex')

    // Find user with this reset token (try hashed lookup first)
    let user = await prisma.user.findUnique({
      where: { passwordResetToken: hashedToken },
    })

    // Migration: Fallback to plaintext for in-flight tokens
    // This allows tokens created before the hashing change to still work
    if (!user) {
      user = await prisma.user.findUnique({
        where: { passwordResetToken: token },
      })
      // If found via plaintext, upgrade to hashed storage immediately
      if (user) {
        await prisma.user.update({
          where: { id: user.id },
          data: { passwordResetToken: hashedToken },
        })
      }
    }

    // Check if token is valid (must have expiry that is not null and not expired)
    if (!user || !user.passwordResetExpires || user.passwordResetExpires < new Date()) {
      return authError('Invalid or expired reset token')
    }

    // Hash new password with bcrypt (cost factor 12)
    const passwordHash = await bcrypt.hash(newPassword, 12)

    // Update user password and clear reset token
    await prisma.user.update({
      where: { id: user.id },
      data: {
        passwordHash,
        passwordResetToken: null,
        passwordResetExpires: null,
      },
    })

    // Invalidate all existing refresh tokens for this user
    await prisma.refreshToken.deleteMany({
      where: { userId: user.id },
    })

    serverLogger.info('Password reset successfully', { userId: user.id, email: user.email })

    return successResponse({ message: 'Password has been reset successfully. Please log in with your new password.' })
  } catch (error) {
    serverLogger.error('Password reset failed', { error })
    return serverError('Password reset failed')
  }
}
