import { config } from 'dotenv'
import { PrismaPg } from '@prisma/adapter-pg'
import { Currency, Prisma, PrismaClient, SubscriptionStatus, TransactionType } from '@prisma/client'

const Decimal = Prisma.Decimal
import { Pool } from 'pg'

config()

const DATABASE_URL = process.env.DATABASE_URL
if (!DATABASE_URL) throw new Error('DATABASE_URL is not set')

const pool = new Pool({ connectionString: DATABASE_URL })
const prisma = new PrismaClient({ adapter: new PrismaPg(pool) })

const USER_ID = 'cmkrc73bd0000c8qujiaylxg6'
const ACCOUNT_ID = 'cmkrc73cp0003c8quhsz54atr'

// Category IDs from DB
const CAT = {
  groceries: 'cmkrc73ct0004c8qu0fvrnqur',
  diningOut: 'cmkrc73cu0005c8qungcye9t3',
  transportation: 'cmkrc73cu0006c8quuuyi29yx',
  utilities: 'cmkrc73cv0007c8que7klluo1',
  entertainment: 'cmkrc73cv0008c8qu13ytc1cp',
  shopping: 'cmkrc73cw0009c8queizw3yo0',
  health: 'cmkrc73cx000ac8qud3yxw10k',
  housing: 'cmkrc73cx000bc8quem4beg0s',
  insurance: 'cmkrc73cy000cc8qu9815xilv',
  subscriptions: 'cmkrc73cy000dc8quq78gdkwj',
  salary: 'cmkrc73cy000ec8quymbcjl6f',
  freelance: 'cmkrc73cz000fc8qug69cnit2',
  investments: 'cmkrc73cz000gc8qugy3q5kww',
  otherIncome: 'cmkrc73d0000hc8quxtw1fx3j',
  savings: 'cmkrc73d0000ic8qunlvjfuzr',
  stocks: 'cmkrc73d1000jc8qu9xry47ba',
  etf: 'cmkrc73d1000kc8qu7zqzcgm0',
  bonds: 'cmkrc73d2000lc8quojqxbjhm',
  crypto: 'cmkrc73d2000mc8qubulkq0b5',
}

function monthStart(year: number, month: number): Date {
  return new Date(Date.UTC(year, month - 1, 1))
}

function date(year: number, month: number, day: number): Date {
  return new Date(Date.UTC(year, month - 1, day))
}

interface TxSeed {
  type: TransactionType
  amount: number
  currency: Currency
  categoryId: string
  description: string
  date: Date
  month: Date
}

function tx(type: TransactionType, amount: number, categoryId: string, description: string, d: Date): TxSeed {
  return {
    type,
    amount,
    currency: Currency.ILS,
    categoryId,
    description,
    date: d,
    month: monthStart(d.getUTCFullYear(), d.getUTCMonth() + 1),
  }
}

const E = TransactionType.EXPENSE
const I = TransactionType.INCOME

async function main() {
  console.log('Seeding data for user:', USER_ID)

  // 1. Fix subscription — extend trial to 30 days from now
  const trialEnd = new Date()
  trialEnd.setDate(trialEnd.getDate() + 30)
  await prisma.subscription.update({
    where: { userId: USER_ID },
    data: { status: SubscriptionStatus.TRIALING, trialEndsAt: trialEnd },
  })
  console.log('Subscription extended to:', trialEnd.toISOString())

  // 2. Seed transactions — 3 months of realistic Israeli expense data
  const transactions: TxSeed[] = [
    // === January 2026 ===
    tx(I, 18500, CAT.salary, 'January salary', date(2026, 1, 1)),
    tx(I, 3200, CAT.freelance, 'Logo design project', date(2026, 1, 8)),
    tx(E, 4800, CAT.housing, 'Rent - January', date(2026, 1, 1)),
    tx(E, 850, CAT.groceries, 'Rami Levy weekly shop', date(2026, 1, 3)),
    tx(E, 720, CAT.groceries, 'Shufersal online order', date(2026, 1, 10)),
    tx(E, 650, CAT.groceries, 'Osher Ad bulk buy', date(2026, 1, 17)),
    tx(E, 480, CAT.groceries, 'Rami Levy midweek', date(2026, 1, 24)),
    tx(E, 180, CAT.diningOut, 'Hummus at Abu Hassan', date(2026, 1, 5)),
    tx(E, 320, CAT.diningOut, 'Dinner at Rak Basar', date(2026, 1, 12)),
    tx(E, 95, CAT.diningOut, 'Aroma coffee & pastry', date(2026, 1, 19)),
    tx(E, 250, CAT.diningOut, 'Shabbat dinner out', date(2026, 1, 25)),
    tx(E, 350, CAT.transportation, 'Rav-Kav monthly pass', date(2026, 1, 1)),
    tx(E, 180, CAT.transportation, 'Gett rides', date(2026, 1, 14)),
    tx(E, 420, CAT.utilities, 'IEC electricity', date(2026, 1, 8)),
    tx(E, 180, CAT.utilities, 'Mekorot water', date(2026, 1, 10)),
    tx(E, 120, CAT.utilities, 'Partner internet', date(2026, 1, 15)),
    tx(E, 280, CAT.entertainment, 'Cinema City tickets', date(2026, 1, 11)),
    tx(E, 150, CAT.entertainment, 'Spotify + Netflix', date(2026, 1, 1)),
    tx(E, 450, CAT.shopping, 'Shoes at Castro', date(2026, 1, 9)),
    tx(E, 200, CAT.health, 'Maccabi copay', date(2026, 1, 6)),
    tx(E, 380, CAT.insurance, 'Car insurance', date(2026, 1, 1)),
    tx(E, 55, CAT.subscriptions, 'ChatGPT Plus', date(2026, 1, 1)),
    tx(E, 35, CAT.subscriptions, 'iCloud storage', date(2026, 1, 1)),

    // === February 2026 ===
    tx(I, 18500, CAT.salary, 'February salary', date(2026, 2, 1)),
    tx(I, 1800, CAT.freelance, 'Website maintenance', date(2026, 2, 15)),
    tx(I, 500, CAT.otherIncome, 'Sold old monitor', date(2026, 2, 20)),
    tx(E, 4800, CAT.housing, 'Rent - February', date(2026, 2, 1)),
    tx(E, 920, CAT.groceries, 'Rami Levy weekly', date(2026, 2, 2)),
    tx(E, 680, CAT.groceries, 'Shufersal', date(2026, 2, 9)),
    tx(E, 550, CAT.groceries, 'Rami Levy', date(2026, 2, 16)),
    tx(E, 780, CAT.groceries, 'Osher Ad + snacks', date(2026, 2, 23)),
    tx(E, 210, CAT.diningOut, 'Shakshuka at Dr Shakshuka', date(2026, 2, 4)),
    tx(E, 380, CAT.diningOut, 'Valentines dinner', date(2026, 2, 14)),
    tx(E, 85, CAT.diningOut, 'Falafel lunch', date(2026, 2, 18)),
    tx(E, 350, CAT.transportation, 'Rav-Kav monthly', date(2026, 2, 1)),
    tx(E, 240, CAT.transportation, 'Gett + Wolt deliveries', date(2026, 2, 11)),
    tx(E, 390, CAT.utilities, 'IEC electricity', date(2026, 2, 7)),
    tx(E, 160, CAT.utilities, 'Mekorot water', date(2026, 2, 10)),
    tx(E, 120, CAT.utilities, 'Partner internet', date(2026, 2, 15)),
    tx(E, 350, CAT.entertainment, 'Escape room + drinks', date(2026, 2, 8)),
    tx(E, 150, CAT.entertainment, 'Streaming subscriptions', date(2026, 2, 1)),
    tx(E, 1200, CAT.shopping, 'New jacket + jeans', date(2026, 2, 13)),
    tx(E, 320, CAT.health, 'Dentist visit', date(2026, 2, 5)),
    tx(E, 380, CAT.insurance, 'Car insurance', date(2026, 2, 1)),
    tx(E, 55, CAT.subscriptions, 'ChatGPT Plus', date(2026, 2, 1)),
    tx(E, 35, CAT.subscriptions, 'iCloud storage', date(2026, 2, 1)),

    // === March 2026 ===
    tx(I, 18500, CAT.salary, 'March salary', date(2026, 3, 1)),
    tx(I, 4500, CAT.freelance, 'Mobile app wireframes', date(2026, 3, 3)),
    tx(I, 800, CAT.investments, 'Dividend payment', date(2026, 3, 5)),
    tx(E, 4800, CAT.housing, 'Rent - March', date(2026, 3, 1)),
    tx(E, 780, CAT.groceries, 'Rami Levy weekly', date(2026, 3, 1)),
    tx(E, 650, CAT.groceries, 'Shufersal', date(2026, 3, 4)),
    tx(E, 150, CAT.diningOut, 'Cafe Landwer breakfast', date(2026, 3, 2)),
    tx(E, 290, CAT.diningOut, 'Sushi at Yakimono', date(2026, 3, 5)),
    tx(E, 350, CAT.transportation, 'Rav-Kav monthly', date(2026, 3, 1)),
    tx(E, 130, CAT.transportation, 'Gett to airport', date(2026, 3, 3)),
    tx(E, 450, CAT.utilities, 'IEC electricity', date(2026, 3, 5)),
    tx(E, 170, CAT.utilities, 'Mekorot water', date(2026, 3, 5)),
    tx(E, 120, CAT.utilities, 'Partner internet', date(2026, 3, 1)),
    tx(E, 200, CAT.entertainment, 'Board game cafe', date(2026, 3, 2)),
    tx(E, 150, CAT.entertainment, 'Streaming', date(2026, 3, 1)),
    tx(E, 680, CAT.shopping, 'Birthday gift', date(2026, 3, 4)),
    tx(E, 180, CAT.health, 'Pharmacy', date(2026, 3, 3)),
    tx(E, 380, CAT.insurance, 'Car insurance', date(2026, 3, 1)),
    tx(E, 55, CAT.subscriptions, 'ChatGPT Plus', date(2026, 3, 1)),
    tx(E, 35, CAT.subscriptions, 'iCloud storage', date(2026, 3, 1)),
  ]

  // Clear existing transactions for this account
  await prisma.transaction.deleteMany({ where: { accountId: ACCOUNT_ID } })

  // Insert all transactions
  for (const t of transactions) {
    await prisma.transaction.create({
      data: {
        accountId: ACCOUNT_ID,
        categoryId: t.categoryId,
        type: t.type,
        amount: new Decimal(t.amount),
        currency: t.currency,
        description: t.description,
        date: t.date,
        month: t.month,
      },
    })
  }
  console.log(`Created ${transactions.length} transactions`)

  // 3. Seed budgets — current and previous months
  await prisma.budget.deleteMany({ where: { accountId: ACCOUNT_ID } })

  const budgetData = [
    // March 2026
    { categoryId: CAT.groceries, planned: 3000, month: monthStart(2026, 3) },
    { categoryId: CAT.diningOut, planned: 800, month: monthStart(2026, 3) },
    { categoryId: CAT.transportation, planned: 600, month: monthStart(2026, 3) },
    { categoryId: CAT.utilities, planned: 800, month: monthStart(2026, 3) },
    { categoryId: CAT.entertainment, planned: 500, month: monthStart(2026, 3) },
    { categoryId: CAT.shopping, planned: 1000, month: monthStart(2026, 3) },
    { categoryId: CAT.health, planned: 400, month: monthStart(2026, 3) },
    // February 2026
    { categoryId: CAT.groceries, planned: 3000, month: monthStart(2026, 2) },
    { categoryId: CAT.diningOut, planned: 800, month: monthStart(2026, 2) },
    { categoryId: CAT.transportation, planned: 600, month: monthStart(2026, 2) },
    { categoryId: CAT.utilities, planned: 800, month: monthStart(2026, 2) },
    { categoryId: CAT.entertainment, planned: 500, month: monthStart(2026, 2) },
    { categoryId: CAT.shopping, planned: 1000, month: monthStart(2026, 2) },
    // January 2026
    { categoryId: CAT.groceries, planned: 2800, month: monthStart(2026, 1) },
    { categoryId: CAT.diningOut, planned: 700, month: monthStart(2026, 1) },
    { categoryId: CAT.transportation, planned: 500, month: monthStart(2026, 1) },
  ]

  for (const b of budgetData) {
    await prisma.budget.create({
      data: {
        accountId: ACCOUNT_ID,
        categoryId: b.categoryId,
        planned: new Decimal(b.planned),
        month: b.month,
      },
    })
  }
  console.log(`Created ${budgetData.length} budgets`)

  // 4. Seed recurring templates
  await prisma.recurringTemplate.deleteMany({ where: { accountId: ACCOUNT_ID } })

  const recurringData = [
    { categoryId: CAT.housing, type: E, amount: 4800, description: 'Monthly rent', dayOfMonth: 1 },
    { categoryId: CAT.salary, type: I, amount: 18500, description: 'Monthly salary', dayOfMonth: 1 },
    { categoryId: CAT.insurance, type: E, amount: 380, description: 'Car insurance', dayOfMonth: 1 },
    { categoryId: CAT.transportation, type: E, amount: 350, description: 'Rav-Kav monthly pass', dayOfMonth: 1 },
    { categoryId: CAT.subscriptions, type: E, amount: 55, description: 'ChatGPT Plus', dayOfMonth: 1 },
    { categoryId: CAT.subscriptions, type: E, amount: 35, description: 'iCloud storage', dayOfMonth: 1 },
    {
      categoryId: CAT.entertainment,
      type: E,
      amount: 150,
      description: 'Streaming (Spotify + Netflix)',
      dayOfMonth: 1,
    },
    { categoryId: CAT.utilities, type: E, amount: 120, description: 'Partner internet', dayOfMonth: 15 },
  ]

  for (const r of recurringData) {
    await prisma.recurringTemplate.create({
      data: {
        accountId: ACCOUNT_ID,
        categoryId: r.categoryId,
        type: r.type,
        amount: new Decimal(r.amount),
        currency: Currency.ILS,
        description: r.description,
        dayOfMonth: r.dayOfMonth,
        isActive: true,
        startMonth: monthStart(2026, 1),
      },
    })
  }
  console.log(`Created ${recurringData.length} recurring templates`)

  // 5. Seed holdings
  await prisma.holding.deleteMany({
    where: { category: { userId: USER_ID } },
  })

  const holdingsData = [
    { categoryId: CAT.stocks, symbol: 'AAPL', quantity: 15, averageCost: 175, currency: Currency.USD },
    { categoryId: CAT.stocks, symbol: 'MSFT', quantity: 8, averageCost: 390, currency: Currency.USD },
    { categoryId: CAT.etf, symbol: 'VOO', quantity: 10, averageCost: 480, currency: Currency.USD },
    { categoryId: CAT.crypto, symbol: 'BTC', quantity: 0.05, averageCost: 84000, currency: Currency.USD },
    { categoryId: CAT.crypto, symbol: 'ETH', quantity: 2, averageCost: 3200, currency: Currency.USD },
  ]

  for (const h of holdingsData) {
    await prisma.holding.create({
      data: {
        accountId: ACCOUNT_ID,
        categoryId: h.categoryId,
        symbol: h.symbol,
        quantity: new Decimal(h.quantity),
        averageCost: new Decimal(h.averageCost),
        currency: h.currency,
      },
    })
  }
  console.log(`Created ${holdingsData.length} holdings`)

  // 6. Seed exchange rates
  await prisma.exchangeRate.deleteMany({})

  const today = new Date(Date.UTC(2026, 2, 6)) // 2026-03-06
  const rates = [
    { base: Currency.USD, target: Currency.ILS, rate: 3.65 },
    { base: Currency.EUR, target: Currency.ILS, rate: 3.95 },
    { base: Currency.USD, target: Currency.EUR, rate: 0.92 },
    { base: Currency.ILS, target: Currency.USD, rate: 0.274 },
    { base: Currency.ILS, target: Currency.EUR, rate: 0.253 },
    { base: Currency.EUR, target: Currency.USD, rate: 1.087 },
  ]

  for (const r of rates) {
    await prisma.exchangeRate.create({
      data: {
        baseCurrency: r.base,
        targetCurrency: r.target,
        rate: new Decimal(r.rate),
        date: today,
      },
    })
  }
  console.log('Created exchange rates')

  console.log('\nDone! Summary:')
  console.log(`  Transactions: ${transactions.length}`)
  console.log(`  Budgets: ${budgetData.length}`)
  console.log(`  Recurring: ${recurringData.length}`)
  console.log(`  Holdings: ${holdingsData.length}`)
  console.log(`  Exchange rates: ${rates.length}`)
  console.log(`  Subscription: extended 30 days`)

  await prisma.$disconnect()
  pool.end()
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
