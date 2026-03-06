import { describe, expect, it } from 'vitest'
import {
  resolveStatIconName,
  buildTabParams,
  buildActivityFeed,
  type ActivityTransaction,
} from '@/lib/dashboard-helpers'

// ---------------------------------------------------------------------------
// resolveStatIconName
// ---------------------------------------------------------------------------

describe('resolveStatIconName', () => {
  describe('Wallet keywords', () => {
    it('returns Wallet for exact "net"', () => {
      expect(resolveStatIconName('net')).toBe('Wallet')
    })

    it('returns Wallet for "Net this month" (mixed case, substring)', () => {
      expect(resolveStatIconName('Net this month')).toBe('Wallet')
    })

    it('returns Wallet for "saved"', () => {
      expect(resolveStatIconName('saved')).toBe('Wallet')
    })

    it('returns Wallet for "Total Saved" (mixed case)', () => {
      expect(resolveStatIconName('Total Saved')).toBe('Wallet')
    })

    it('returns Wallet for "income"', () => {
      expect(resolveStatIconName('income')).toBe('Wallet')
    })

    it('returns Wallet for "Monthly Income" (mixed case, substring)', () => {
      expect(resolveStatIconName('Monthly Income')).toBe('Wallet')
    })

    it('returns Wallet for "inflow"', () => {
      expect(resolveStatIconName('inflow')).toBe('Wallet')
    })

    it('returns Wallet for "Cash Inflow"', () => {
      expect(resolveStatIconName('Cash Inflow')).toBe('Wallet')
    })
  })

  describe('PiggyBank keywords', () => {
    it('returns PiggyBank for "spend"', () => {
      expect(resolveStatIconName('spend')).toBe('PiggyBank')
    })

    it('returns PiggyBank for "Total Spending" (mixed case, substring)', () => {
      expect(resolveStatIconName('Total Spending')).toBe('PiggyBank')
    })

    it('returns PiggyBank for "expense"', () => {
      expect(resolveStatIconName('expense')).toBe('PiggyBank')
    })

    it('returns PiggyBank for "Expenses this month" (mixed case)', () => {
      expect(resolveStatIconName('Expenses this month')).toBe('PiggyBank')
    })

    it('returns PiggyBank for "outflow"', () => {
      expect(resolveStatIconName('outflow')).toBe('PiggyBank')
    })

    it('returns PiggyBank for "Cash Outflow"', () => {
      expect(resolveStatIconName('Cash Outflow')).toBe('PiggyBank')
    })
  })

  describe('Layers keywords', () => {
    it('returns Layers for "target"', () => {
      expect(resolveStatIconName('target')).toBe('Layers')
    })

    it('returns Layers for "Monthly Target" (mixed case, substring)', () => {
      expect(resolveStatIconName('Monthly Target')).toBe('Layers')
    })

    it('returns Layers for "goal"', () => {
      expect(resolveStatIconName('goal')).toBe('Layers')
    })

    it('returns Layers for "Savings Goal"', () => {
      expect(resolveStatIconName('Savings Goal')).toBe('Layers')
    })

    it('returns Layers for "budget"', () => {
      expect(resolveStatIconName('budget')).toBe('Layers')
    })

    it('returns Layers for "Budget vs Actual"', () => {
      expect(resolveStatIconName('Budget vs Actual')).toBe('Layers')
    })

    it('returns Layers for "track"', () => {
      expect(resolveStatIconName('track')).toBe('Layers')
    })

    it('returns Layers for "On Track"', () => {
      expect(resolveStatIconName('On Track')).toBe('Layers')
    })
  })

  describe('TrendingUp default', () => {
    it('returns TrendingUp for an unrecognised label', () => {
      expect(resolveStatIconName('growth')).toBe('TrendingUp')
    })

    it('returns TrendingUp for an empty string', () => {
      expect(resolveStatIconName('')).toBe('TrendingUp')
    })

    it('returns TrendingUp for a label with no matching keyword', () => {
      expect(resolveStatIconName('Portfolio value')).toBe('TrendingUp')
    })

    it('returns TrendingUp for a purely numeric-like label', () => {
      expect(resolveStatIconName('2025')).toBe('TrendingUp')
    })
  })

  describe('case-insensitivity', () => {
    it('is case-insensitive for UPPER CASE label', () => {
      expect(resolveStatIconName('INCOME')).toBe('Wallet')
    })

    it('is case-insensitive for ALL CAPS spend keyword', () => {
      expect(resolveStatIconName('SPEND')).toBe('PiggyBank')
    })

    it('is case-insensitive for Title Case budget keyword', () => {
      expect(resolveStatIconName('Budget')).toBe('Layers')
    })
  })

  describe('keyword priority (first match wins)', () => {
    // "net" is checked before "budget"; a label containing both resolves to Wallet
    it('resolves to Wallet when label contains both "net" and "budget"', () => {
      expect(resolveStatIconName('net budget')).toBe('Wallet')
    })

    // "income" takes priority over "expense" because Wallet group is checked first
    it('resolves to Wallet when label contains both "income" and "expense"', () => {
      expect(resolveStatIconName('income vs expense')).toBe('Wallet')
    })
  })
})

// ---------------------------------------------------------------------------
// buildTabParams
// ---------------------------------------------------------------------------

describe('buildTabParams', () => {
  it('removes tab param when switching to overview', () => {
    const result = buildTabParams('tab=transactions', 'overview')
    expect(result).toBe('')
  })

  it('sets tab param for non-overview tabs', () => {
    const result = buildTabParams('', 'transactions')
    expect(result).toBe('tab=transactions')
  })

  it('replaces existing tab param when switching tabs', () => {
    const result = buildTabParams('tab=budgets', 'recurring')
    expect(result).toBe('tab=recurring')
  })

  it('preserves other search params when setting a tab', () => {
    const result = buildTabParams('month=2025-01&account=acc-1', 'transactions')
    const params = new URLSearchParams(result)
    expect(params.get('tab')).toBe('transactions')
    expect(params.get('month')).toBe('2025-01')
    expect(params.get('account')).toBe('acc-1')
  })

  it('preserves other search params when switching to overview', () => {
    const result = buildTabParams('tab=budgets&month=2025-01', 'overview')
    const params = new URLSearchParams(result)
    expect(params.has('tab')).toBe(false)
    expect(params.get('month')).toBe('2025-01')
  })

  it('returns empty string when switching to overview with no other params', () => {
    expect(buildTabParams('tab=holdings', 'overview')).toBe('')
  })

  it('sets tab param for all known tabs', () => {
    const tabs = ['transactions', 'budgets', 'recurring', 'categories', 'holdings', 'sharing']
    for (const tab of tabs) {
      const result = buildTabParams('', tab)
      expect(new URLSearchParams(result).get('tab')).toBe(tab)
    }
  })

  it('does not set a tab param when starting with empty search and switching to overview', () => {
    const result = buildTabParams('', 'overview')
    expect(result).toBe('')
  })
})

// ---------------------------------------------------------------------------
// buildActivityFeed
// ---------------------------------------------------------------------------

describe('buildActivityFeed', () => {
  const makeTransaction = (overrides: Partial<ActivityTransaction> = {}): ActivityTransaction => ({
    id: 'tx-1',
    type: 'EXPENSE',
    amount: 50,
    description: 'Coffee',
    category: { name: 'Food', color: '#f00' },
    date: new Date('2025-01-15'),
    ...overrides,
  })

  it('returns at most 5 items', () => {
    const transactions = Array.from({ length: 10 }, (_, i) => makeTransaction({ id: `tx-${i}` }))
    expect(buildActivityFeed(transactions)).toHaveLength(5)
  })

  it('returns fewer items when fewer than 5 transactions provided', () => {
    const transactions = [makeTransaction(), makeTransaction({ id: 'tx-2' })]
    expect(buildActivityFeed(transactions)).toHaveLength(2)
  })

  it('returns an empty array for an empty input', () => {
    expect(buildActivityFeed([])).toHaveLength(0)
  })

  it('gives INCOME transactions a "+" prefix', () => {
    const item = buildActivityFeed([makeTransaction({ type: 'INCOME' })])[0]
    expect(item.prefix).toBe('+')
  })

  it('gives EXPENSE transactions a "-" prefix', () => {
    const item = buildActivityFeed([makeTransaction({ type: 'EXPENSE' })])[0]
    expect(item.prefix).toBe('-')
  })

  it('gives non-INCOME unknown type a "-" prefix', () => {
    const item = buildActivityFeed([makeTransaction({ type: 'TRANSFER' })])[0]
    expect(item.prefix).toBe('-')
  })

  it('uses description as label when present', () => {
    const item = buildActivityFeed([makeTransaction({ description: 'My description' })])[0]
    expect(item.label).toBe('My description')
  })

  it('falls back to category name when description is absent', () => {
    const item = buildActivityFeed([
      makeTransaction({ description: null, category: { name: 'Groceries', color: '#0f0' } }),
    ])[0]
    expect(item.label).toBe('Groceries')
  })

  it('falls back to "Transaction" when description and category name are both absent', () => {
    const item = buildActivityFeed([makeTransaction({ description: null, category: { name: null, color: null } })])[0]
    expect(item.label).toBe('Transaction')
  })

  it('falls back to "Transaction" when description is absent and category is null', () => {
    const item = buildActivityFeed([makeTransaction({ description: null, category: null })])[0]
    expect(item.label).toBe('Transaction')
  })

  it('uses absolute value of amount regardless of sign', () => {
    const item = buildActivityFeed([makeTransaction({ amount: -75.5 })])[0]
    expect(item.amount).toBe(75.5)
  })

  it('uses absolute value for positive amounts too', () => {
    const item = buildActivityFeed([makeTransaction({ amount: 100 })])[0]
    expect(item.amount).toBe(100)
  })

  it('uses category color when present', () => {
    const item = buildActivityFeed([makeTransaction({ category: { name: 'Food', color: '#abc123' } })])[0]
    expect(item.color).toBe('#abc123')
  })

  it('falls back to default color "#0ea5e9" when category color is absent', () => {
    const item = buildActivityFeed([makeTransaction({ category: { name: 'Food', color: null } })])[0]
    expect(item.color).toBe('#0ea5e9')
  })

  it('falls back to default color "#0ea5e9" when category is null', () => {
    const item = buildActivityFeed([makeTransaction({ category: null })])[0]
    expect(item.color).toBe('#0ea5e9')
  })

  it('preserves id and date on output items', () => {
    const date = new Date('2025-06-01')
    const item = buildActivityFeed([makeTransaction({ id: 'tx-abc', date })])[0]
    expect(item.id).toBe('tx-abc')
    expect(item.date).toBe(date)
  })

  it('takes only the first 5 items, preserving order', () => {
    const transactions = Array.from({ length: 8 }, (_, i) =>
      makeTransaction({ id: `tx-${i}`, description: `Item ${i}` }),
    )
    const feed = buildActivityFeed(transactions)
    expect(feed.map((f) => f.id)).toEqual(['tx-0', 'tx-1', 'tx-2', 'tx-3', 'tx-4'])
  })

  it('handles a description that is an empty string by falling back to category name', () => {
    const item = buildActivityFeed([
      makeTransaction({ description: '', category: { name: 'Utilities', color: '#111' } }),
    ])[0]
    // Empty string is falsy, so should fall back
    expect(item.label).toBe('Utilities')
  })
})
