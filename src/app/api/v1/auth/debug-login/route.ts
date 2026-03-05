import { NextRequest, NextResponse } from 'next/server'
import {
  AccountType,
  Currency,
  PaymentStatus,
  RequestStatus,
  SplitType,
  SubscriptionStatus,
  TransactionType,
} from '@prisma/client'
import { generateAccessToken, generateRefreshToken } from '@/lib/jwt'
import { prisma } from '@/lib/prisma'
import { TRIAL_DURATION_DAYS } from '@/lib/subscription-constants'
import { invalidateDashboardCache } from '@/lib/dashboard-cache'
import {
  DEFAULT_EXPENSE_CATEGORIES,
  DEFAULT_HOLDING_CATEGORIES,
  DEFAULT_INCOME_CATEGORIES,
} from '@/lib/default-categories'
import { getMonthKey, getMonthStartFromKey, shiftMonth } from '@/utils/date'

const DEMO_EMAIL = 'android-demo@balancebeacon.local'
const DEMO_DISPLAY_NAME = 'Android Demo'
const DEMO_ACCOUNT_NAME = 'Android Demo'
const DEMO_PRICE_SOURCE = 'debug-seed'
const DEMO_MONTHLY_INCOME_GOAL = 6200
const DEMO_PEERS = [
  {
    key: 'maya',
    email: 'android-maya@balancebeacon.local',
    displayName: 'Maya Chen',
    accountName: 'Maya Chen',
    color: '#14b8a6',
  },
  {
    key: 'liam',
    email: 'android-liam@balancebeacon.local',
    displayName: 'Liam Ortiz',
    accountName: 'Liam Ortiz',
    color: '#f97316',
  },
] as const

const DEMO_BUDGETS = [
  { categoryName: 'Housing', planned: 1700 },
  { categoryName: 'Groceries', planned: 650 },
  { categoryName: 'Dining Out', planned: 260 },
  { categoryName: 'Transportation', planned: 180 },
  { categoryName: 'Entertainment', planned: 140 },
] as const

const DEMO_HOLDINGS = [
  { categoryName: 'ETF', symbol: 'VTI', quantity: 14, averageCost: 245, notes: 'Core index position' },
  { categoryName: 'Stocks', symbol: 'AAPL', quantity: 10, averageCost: 182, notes: 'Long-term compounder' },
  { categoryName: 'Crypto', symbol: 'BTC', quantity: 0.18, averageCost: 42000, notes: 'High-volatility satellite' },
] as const

const DEMO_STOCK_PRICES = [
  { symbol: 'VTI', price: 289.12, changePercent: 1.25 },
  { symbol: 'AAPL', price: 194.38, changePercent: 0.85 },
  { symbol: 'BTC', price: 63850.0, changePercent: 2.15 },
] as const

const DEMO_MONTH_PLANS = [
  {
    monthsAgo: 5,
    salary: 5200,
    freelance: 350,
    investments: 60,
    housing: 1650,
    groceries: 420,
    dining: 140,
    transportation: 125,
    entertainment: 90,
  },
  {
    monthsAgo: 4,
    salary: 5300,
    freelance: 0,
    investments: 75,
    housing: 1650,
    groceries: 435,
    dining: 155,
    transportation: 130,
    entertainment: 95,
  },
  {
    monthsAgo: 3,
    salary: 5400,
    freelance: 420,
    investments: 80,
    housing: 1650,
    groceries: 460,
    dining: 180,
    transportation: 135,
    entertainment: 110,
  },
  {
    monthsAgo: 2,
    salary: 5550,
    freelance: 350,
    investments: 85,
    housing: 1650,
    groceries: 475,
    dining: 190,
    transportation: 145,
    entertainment: 120,
  },
  {
    monthsAgo: 1,
    salary: 5750,
    freelance: 500,
    investments: 95,
    housing: 1650,
    groceries: 490,
    dining: 210,
    transportation: 155,
    entertainment: 145,
  },
  {
    monthsAgo: 0,
    salary: 5900,
    freelance: 650,
    investments: 110,
    housing: 1650,
    groceries: 430,
    dining: 175,
    transportation: 125,
    entertainment: 96,
  },
] as const

type DemoCategoryMaps = {
  expense: Map<string, string>
  income: Map<string, string>
  holding: Map<string, string>
}

type DemoPeerSeed = {
  key: (typeof DEMO_PEERS)[number]['key']
  userId: string
  accountId: string
  categoryMaps: DemoCategoryMaps
}

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

  const account = await prisma.account.upsert({
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
      description: 'Auto-provisioned local Android demo account',
      defaultIncomeGoal: DEMO_MONTHLY_INCOME_GOAL,
      defaultIncomeGoalCurrency: Currency.USD,
      deletedAt: null,
    },
    create: {
      userId: user.id,
      name: DEMO_ACCOUNT_NAME,
      type: AccountType.SELF,
      preferredCurrency: Currency.USD,
      color: '#0ea5e9',
      icon: 'Smartphone',
      description: 'Auto-provisioned local Android demo account',
      defaultIncomeGoal: DEMO_MONTHLY_INCOME_GOAL,
      defaultIncomeGoalCurrency: Currency.USD,
    },
    select: {
      id: true,
    },
  })

  await prisma.user.update({
    where: { id: user.id },
    data: { activeAccountId: account.id },
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

  await prisma.refreshToken.deleteMany({
    where: { userId: user.id },
  })

  const categoryMaps = await ensureDemoCategories(user.id)
  const peers = await ensureDemoPeers()
  await seedDemoAccountData({
    demoUserId: user.id,
    accountId: account.id,
    categoryMaps,
    peers,
  })
  await invalidateDashboardCache({ accountId: account.id })

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

async function ensureDemoCategories(userId: string): Promise<DemoCategoryMaps> {
  const expense = new Map<string, string>()
  const income = new Map<string, string>()
  const holding = new Map<string, string>()

  const expenseCategories = await Promise.all(
    DEFAULT_EXPENSE_CATEGORIES.map((category) =>
      prisma.category.upsert({
        where: {
          userId_name_type: {
            userId,
            name: category.name,
            type: TransactionType.EXPENSE,
          },
        },
        update: {
          color: category.color,
          isHolding: false,
          isArchived: false,
        },
        create: {
          userId,
          name: category.name,
          type: TransactionType.EXPENSE,
          color: category.color,
          isHolding: false,
          isArchived: false,
        },
        select: {
          id: true,
          name: true,
        },
      }),
    ),
  )

  const incomeCategories = await Promise.all(
    DEFAULT_INCOME_CATEGORIES.map((category) =>
      prisma.category.upsert({
        where: {
          userId_name_type: {
            userId,
            name: category.name,
            type: TransactionType.INCOME,
          },
        },
        update: {
          color: category.color,
          isHolding: false,
          isArchived: false,
        },
        create: {
          userId,
          name: category.name,
          type: TransactionType.INCOME,
          color: category.color,
          isHolding: false,
          isArchived: false,
        },
        select: {
          id: true,
          name: true,
        },
      }),
    ),
  )

  const holdingCategories = await Promise.all(
    DEFAULT_HOLDING_CATEGORIES.map((category) =>
      prisma.category.upsert({
        where: {
          userId_name_type: {
            userId,
            name: category.name,
            type: TransactionType.EXPENSE,
          },
        },
        update: {
          color: category.color,
          isHolding: true,
          isArchived: false,
        },
        create: {
          userId,
          name: category.name,
          type: TransactionType.EXPENSE,
          color: category.color,
          isHolding: true,
          isArchived: false,
        },
        select: {
          id: true,
          name: true,
        },
      }),
    ),
  )

  expenseCategories.forEach((category) => expense.set(category.name, category.id))
  incomeCategories.forEach((category) => income.set(category.name, category.id))
  holdingCategories.forEach((category) => holding.set(category.name, category.id))

  return { expense, income, holding }
}

async function ensureDemoPeers(): Promise<DemoPeerSeed[]> {
  return Promise.all(
    DEMO_PEERS.map(async (peer) => {
      const user = await prisma.user.upsert({
        where: { email: peer.email },
        update: {
          displayName: peer.displayName,
          emailVerified: true,
          preferredCurrency: Currency.USD,
          hasCompletedOnboarding: true,
          deletedAt: null,
        },
        create: {
          email: peer.email,
          displayName: peer.displayName,
          passwordHash: 'debug-login-unavailable',
          emailVerified: true,
          preferredCurrency: Currency.USD,
          hasCompletedOnboarding: true,
        },
        select: {
          id: true,
        },
      })

      const account = await prisma.account.upsert({
        where: {
          userId_name: {
            userId: user.id,
            name: peer.accountName,
          },
        },
        update: {
          type: AccountType.SELF,
          preferredCurrency: Currency.USD,
          color: peer.color,
          icon: 'Users',
          description: 'Auto-provisioned local Android debug counterparty',
          deletedAt: null,
        },
        create: {
          userId: user.id,
          name: peer.accountName,
          type: AccountType.SELF,
          preferredCurrency: Currency.USD,
          color: peer.color,
          icon: 'Users',
          description: 'Auto-provisioned local Android debug counterparty',
        },
        select: {
          id: true,
        },
      })

      await prisma.user.update({
        where: { id: user.id },
        data: { activeAccountId: account.id },
      })

      return {
        key: peer.key,
        userId: user.id,
        accountId: account.id,
        categoryMaps: await ensureDemoCategories(user.id),
      }
    }),
  )
}

async function seedDemoAccountData({
  demoUserId,
  accountId,
  categoryMaps,
  peers,
}: {
  demoUserId: string
  accountId: string
  categoryMaps: DemoCategoryMaps
  peers: DemoPeerSeed[]
}) {
  const currentMonthKey = getMonthKey(new Date())
  const currentMonthStart = getMonthStartFromKey(currentMonthKey)
  const recurringStartMonth = getMonthStartFromKey(shiftMonth(currentMonthKey, -5))
  const seededAccountIds = [accountId, ...peers.map((peer) => peer.accountId)]

  await Promise.all([
    prisma.transactionRequest.deleteMany({
      where: {
        OR: [{ toId: { in: seededAccountIds } }, { fromId: { in: seededAccountIds } }],
      },
    }),
    prisma.transaction.deleteMany({
      where: {
        accountId: {
          in: seededAccountIds,
        },
      },
    }),
    prisma.budget.deleteMany({ where: { accountId } }),
    prisma.monthlyIncomeGoal.deleteMany({ where: { accountId } }),
    prisma.holding.deleteMany({ where: { accountId } }),
    prisma.recurringTemplate.deleteMany({ where: { accountId } }),
    prisma.stockPrice.deleteMany({
      where: {
        source: DEMO_PRICE_SOURCE,
        symbol: {
          in: DEMO_STOCK_PRICES.map((price) => price.symbol),
        },
      },
    }),
  ])

  await prisma.transaction.createMany({
    data: buildDemoTransactions(accountId, categoryMaps, currentMonthKey),
  })

  await prisma.budget.createMany({
    data: DEMO_BUDGETS.map((budget) => ({
      accountId,
      categoryId: requireCategoryId(categoryMaps.expense, budget.categoryName),
      month: currentMonthStart,
      planned: budget.planned,
      currency: Currency.USD,
    })),
  })

  await prisma.monthlyIncomeGoal.create({
    data: {
      accountId,
      month: currentMonthStart,
      amount: DEMO_MONTHLY_INCOME_GOAL,
      currency: Currency.USD,
      notes: 'Local Android debug goal',
    },
  })

  await prisma.recurringTemplate.createMany({
    data: [
      {
        accountId,
        categoryId: requireCategoryId(categoryMaps.income, 'Salary'),
        type: TransactionType.INCOME,
        amount: 5900,
        currency: Currency.USD,
        dayOfMonth: 1,
        description: 'Monthly salary',
        isActive: true,
        startMonth: recurringStartMonth,
      },
      {
        accountId,
        categoryId: requireCategoryId(categoryMaps.expense, 'Housing'),
        type: TransactionType.EXPENSE,
        amount: 1650,
        currency: Currency.USD,
        dayOfMonth: 2,
        description: 'Apartment rent',
        isActive: true,
        startMonth: recurringStartMonth,
      },
      {
        accountId,
        categoryId: requireCategoryId(categoryMaps.expense, 'Subscriptions'),
        type: TransactionType.EXPENSE,
        amount: 29,
        currency: Currency.USD,
        dayOfMonth: 3,
        description: 'Apps and streaming',
        isActive: true,
        startMonth: recurringStartMonth,
      },
    ],
  })

  await prisma.holding.createMany({
    data: DEMO_HOLDINGS.map((holding) => ({
      accountId,
      categoryId: requireCategoryId(categoryMaps.holding, holding.categoryName),
      symbol: holding.symbol,
      quantity: holding.quantity,
      averageCost: holding.averageCost,
      currency: Currency.USD,
      notes: holding.notes,
    })),
  })

  await prisma.stockPrice.createMany({
    data: DEMO_STOCK_PRICES.map((price) => ({
      symbol: price.symbol,
      price: price.price,
      currency: Currency.USD,
      changePercent: price.changePercent,
      source: DEMO_PRICE_SOURCE,
      fetchedAt: new Date(),
    })),
  })

  await seedDemoSharingData({
    demoUserId,
    demoAccountId: accountId,
    demoCategoryMaps: categoryMaps,
    peers,
    currentMonthStart,
  })
}

async function seedDemoSharingData({
  demoUserId,
  demoAccountId,
  demoCategoryMaps,
  peers,
  currentMonthStart,
}: {
  demoUserId: string
  demoAccountId: string
  demoCategoryMaps: DemoCategoryMaps
  peers: DemoPeerSeed[]
  currentMonthStart: Date
}) {
  const maya = requireDemoPeer(peers, 'maya')
  const liam = requireDemoPeer(peers, 'liam')

  const dinnerTransaction = await prisma.transaction.create({
    data: {
      accountId: demoAccountId,
      categoryId: requireCategoryId(demoCategoryMaps.expense, 'Dining Out'),
      type: TransactionType.EXPENSE,
      amount: 108,
      currency: Currency.USD,
      date: createUtcMonthDate(currentMonthStart, resolveDemoDay(0, 6)),
      month: currentMonthStart,
      description: 'Team dinner split',
    },
    select: {
      id: true,
    },
  })

  await prisma.sharedExpense.create({
    data: {
      transactionId: dinnerTransaction.id,
      ownerId: demoUserId,
      splitType: SplitType.EQUAL,
      totalAmount: 108,
      currency: Currency.USD,
      description: 'Team dinner split',
      participants: {
        create: [
          {
            userId: liam.userId,
            shareAmount: 54,
            status: PaymentStatus.PAID,
            paidAt: createUtcMonthDate(currentMonthStart, resolveDemoDay(0, 5)),
          },
        ],
      },
    },
  })

  const groceriesTransaction = await prisma.transaction.create({
    data: {
      accountId: maya.accountId,
      categoryId: requireCategoryId(maya.categoryMaps.expense, 'Groceries'),
      type: TransactionType.EXPENSE,
      amount: 84,
      currency: Currency.USD,
      date: createUtcMonthDate(currentMonthStart, resolveDemoDay(0, 4)),
      month: currentMonthStart,
      description: 'Weekend groceries split',
    },
    select: {
      id: true,
    },
  })

  await prisma.sharedExpense.create({
    data: {
      transactionId: groceriesTransaction.id,
      ownerId: maya.userId,
      splitType: SplitType.EQUAL,
      totalAmount: 84,
      currency: Currency.USD,
      description: 'Weekend groceries split',
      participants: {
        create: [
          {
            userId: demoUserId,
            shareAmount: 42,
            status: PaymentStatus.PAID,
            paidAt: createUtcMonthDate(currentMonthStart, resolveDemoDay(0, 4)),
          },
        ],
      },
    },
  })

  const ticketsTransaction = await prisma.transaction.create({
    data: {
      accountId: liam.accountId,
      categoryId: requireCategoryId(liam.categoryMaps.expense, 'Entertainment'),
      type: TransactionType.EXPENSE,
      amount: 64,
      currency: Currency.USD,
      date: createUtcMonthDate(currentMonthStart, resolveDemoDay(0, 5)),
      month: currentMonthStart,
      description: 'Concert tickets',
    },
    select: {
      id: true,
    },
  })

  await prisma.sharedExpense.create({
    data: {
      transactionId: ticketsTransaction.id,
      ownerId: liam.userId,
      splitType: SplitType.EQUAL,
      totalAmount: 64,
      currency: Currency.USD,
      description: 'Concert tickets',
      participants: {
        create: [
          {
            userId: demoUserId,
            shareAmount: 32,
            status: PaymentStatus.PENDING,
          },
        ],
      },
    },
  })

  await prisma.transactionRequest.createMany({
    data: [
      {
        fromId: maya.accountId,
        toId: demoAccountId,
        categoryId: requireCategoryId(demoCategoryMaps.expense, 'Dining Out'),
        amount: 28,
        currency: Currency.USD,
        date: createUtcMonthDate(currentMonthStart, resolveDemoDay(0, 5)),
        description: 'Add my half of the ramen run',
        status: RequestStatus.PENDING,
      },
      {
        fromId: liam.accountId,
        toId: demoAccountId,
        categoryId: requireCategoryId(demoCategoryMaps.expense, 'Transportation'),
        amount: 18,
        currency: Currency.USD,
        date: createUtcMonthDate(currentMonthStart, resolveDemoDay(0, 4)),
        description: 'Parking split from the airport pickup',
        status: RequestStatus.PENDING,
      },
    ],
  })
}

function buildDemoTransactions(accountId: string, categoryMaps: DemoCategoryMaps, currentMonthKey: string) {
  return DEMO_MONTH_PLANS.flatMap((plan) => {
    const monthKey = shiftMonth(currentMonthKey, -plan.monthsAgo)
    const monthStart = getMonthStartFromKey(monthKey)

    return [
      buildTransaction({
        accountId,
        categoryId: requireCategoryId(categoryMaps.income, 'Salary'),
        type: TransactionType.INCOME,
        amount: plan.salary,
        monthStart,
        day: resolveDemoDay(plan.monthsAgo, 1),
        description: 'Primary salary deposit',
      }),
      buildTransaction({
        accountId,
        categoryId: requireCategoryId(categoryMaps.income, 'Freelance'),
        type: TransactionType.INCOME,
        amount: plan.freelance,
        monthStart,
        day: resolveDemoDay(plan.monthsAgo, 2),
        description: 'Freelance invoice payout',
      }),
      buildTransaction({
        accountId,
        categoryId: requireCategoryId(categoryMaps.income, 'Investments'),
        type: TransactionType.INCOME,
        amount: plan.investments,
        monthStart,
        day: resolveDemoDay(plan.monthsAgo, 3),
        description: 'Dividend and yield income',
      }),
      buildTransaction({
        accountId,
        categoryId: requireCategoryId(categoryMaps.expense, 'Housing'),
        type: TransactionType.EXPENSE,
        amount: plan.housing,
        monthStart,
        day: resolveDemoDay(plan.monthsAgo, 2),
        description: 'Apartment rent',
      }),
      buildTransaction({
        accountId,
        categoryId: requireCategoryId(categoryMaps.expense, 'Groceries'),
        type: TransactionType.EXPENSE,
        amount: plan.groceries,
        monthStart,
        day: resolveDemoDay(plan.monthsAgo, 4),
        description: 'Groceries and pantry restock',
      }),
      buildTransaction({
        accountId,
        categoryId: requireCategoryId(categoryMaps.expense, 'Dining Out'),
        type: TransactionType.EXPENSE,
        amount: plan.dining,
        monthStart,
        day: resolveDemoDay(plan.monthsAgo, 5),
        description: 'Coffee and dinner out',
      }),
      buildTransaction({
        accountId,
        categoryId: requireCategoryId(categoryMaps.expense, 'Transportation'),
        type: TransactionType.EXPENSE,
        amount: plan.transportation,
        monthStart,
        day: resolveDemoDay(plan.monthsAgo, 6),
        description: 'Transit pass and rideshare',
      }),
      buildTransaction({
        accountId,
        categoryId: requireCategoryId(categoryMaps.expense, 'Entertainment'),
        type: TransactionType.EXPENSE,
        amount: plan.entertainment,
        monthStart,
        day: resolveDemoDay(plan.monthsAgo, 7),
        description: 'Streaming, books, and cinema',
      }),
    ].filter((transaction) => transaction.amount > 0)
  })
}

function buildTransaction({
  accountId,
  categoryId,
  type,
  amount,
  monthStart,
  day,
  description,
}: {
  accountId: string
  categoryId: string
  type: TransactionType
  amount: number
  monthStart: Date
  day: number
  description: string
}) {
  return {
    accountId,
    categoryId,
    type,
    amount,
    currency: Currency.USD,
    date: createUtcMonthDate(monthStart, day),
    month: monthStart,
    description,
  }
}

function createUtcMonthDate(monthStart: Date, day: number): Date {
  return new Date(Date.UTC(monthStart.getUTCFullYear(), monthStart.getUTCMonth(), day, 12, 0, 0))
}

function resolveDemoDay(monthsAgo: number, day: number): number {
  return monthsAgo === 0 ? Math.min(day, new Date().getUTCDate()) : day
}

function requireDemoPeer(peers: DemoPeerSeed[], key: DemoPeerSeed['key']): DemoPeerSeed {
  const peer = peers.find((entry) => entry.key === key)
  if (!peer) {
    throw new Error(`Missing demo peer seed: ${key}`)
  }
  return peer
}

function requireCategoryId(categoryMap: Map<string, string>, categoryName: string): string {
  const categoryId = categoryMap.get(categoryName)
  if (!categoryId) {
    throw new Error(`Missing demo category: ${categoryName}`)
  }
  return categoryId
}
