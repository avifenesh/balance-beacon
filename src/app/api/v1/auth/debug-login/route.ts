import { NextRequest, NextResponse } from 'next/server'
import { AccountType, Currency, SubscriptionStatus } from '@prisma/client'
import { generateAccessToken, generateRefreshToken } from '@/lib/jwt'
import { prisma } from '@/lib/prisma'
import { TRIAL_DURATION_DAYS } from '@/lib/subscription-constants'

const DEMO_EMAIL = 'android-demo@balancebeacon.local'
const DEMO_DISPLAY_NAME = 'Android Demo'
const DEMO_ACCOUNT_NAME = 'Android Demo'

export async function POST(_request: NextRequest) {
  if (process.env.NODE_ENV === 'production') {
    return NextResponse.json({ error: 'Not found' }, { status: 404 })
  }

  const trialEndsAt = new Date()
  trialEndsAt.setDate(trialEndsAt.getDate() + TRIAL_DURATION_DAYS)

  const user = await prisma.user.upsert({
    where: { email: DEMO_EMAIL },
    update: {
      displayName: DEMO_DISPLAY_NAME,
      emailVerified: true,
      preferredCurrency: Currency.USD,
      hasCompletedOnboarding: true,
      deletedAt: null,
    },
    create: {
      email: DEMO_EMAIL,
      displayName: DEMO_DISPLAY_NAME,
      passwordHash: 'debug-login-unavailable',
      emailVerified: true,
      preferredCurrency: Currency.USD,
      hasCompletedOnboarding: true,
    },
    select: {
      id: true,
      displayName: true,
      preferredCurrency: true,
      hasCompletedOnboarding: true,
    },
  })

  await prisma.account.upsert({
    where: {
      userId_name: {
        userId: user.id,
        name: DEMO_ACCOUNT_NAME,
      },
    },
    update: {
      type: AccountType.SELF,
      preferredCurrency: Currency.USD,
      color: '#0ea5e9',
      icon: 'Smartphone',
      deletedAt: null,
    },
    create: {
      userId: user.id,
      name: DEMO_ACCOUNT_NAME,
      type: AccountType.SELF,
      preferredCurrency: Currency.USD,
      color: '#0ea5e9',
      icon: 'Smartphone',
    },
  })

  await prisma.subscription.upsert({
    where: { userId: user.id },
    update: {
      status: SubscriptionStatus.TRIALING,
      trialEndsAt,
    },
    create: {
      userId: user.id,
      status: SubscriptionStatus.TRIALING,
      trialEndsAt,
    },
  })

  const accessToken = generateAccessToken(user.id, DEMO_EMAIL)
  const { token: refreshToken, jti, expiresAt } = generateRefreshToken(user.id, DEMO_EMAIL)

  await prisma.refreshToken.create({
    data: {
      jti,
      userId: user.id,
      email: DEMO_EMAIL,
      expiresAt,
    },
  })

  return NextResponse.json({
    success: true,
    data: {
      accessToken,
      refreshToken,
      expiresIn: 900,
      user: {
        id: user.id,
        email: DEMO_EMAIL,
        displayName: user.displayName,
        preferredCurrency: user.preferredCurrency,
        hasCompletedOnboarding: user.hasCompletedOnboarding,
      },
    },
  })
}
