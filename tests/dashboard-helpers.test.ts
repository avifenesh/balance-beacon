import { describe, it, expect } from 'vitest'
import {
  resolveStatIconName,
  buildTabParams,
  buildActivityFeed,
  calculateIncomeProgress,
  type ActivityTransaction,
} from '@/lib/dashboard-helpers'

// ---------------------------------------------------------------------------
// resolveStatIconName
// ---------------------------------------------------------------------------

describe('resolveStatIconName()', () => {
  it('returns Wallet for labels containing "net"', () => {
    expect(resolveStatIconName('Net savings')).toBe('Wallet')
    expect(resolveStatIconName('NET')).toBe('Wallet')
  })

  it('returns Wallet for labels containing "income"', () => {
    expect(resolveStatIconName('Total Income')).toBe('Wallet')
  })

  it('returns Wallet for labels containing "saved" or "inflow"', () => {
    expect(resolveStatIconName('Money saved')).toBe('Wallet')
    expect(resolveStatIconName('Inflow this month')).toBe('Wallet')
  })

  it('returns PiggyBank for labels containing "expense"', () => {
    expect(resolveStatIconName('Total Expenses')).toBe('PiggyBank')
    expect(resolveStatIconName('expense')).toBe('PiggyBank')
  })

  it('returns PiggyBank for labels containing "spend" or "outflow"', () => {
    expect(resolveStatIconName('Spend limit')).toBe('PiggyBank')
    expect(resolveStatIconName('Outflow')).toBe('PiggyBank')
  })

  it('returns Layers for labels containing "budget"', () => {
    expect(resolveStatIconName('Budget overview')).toBe('Layers')
  })

  it('returns Layers for labels containing "goal" or "target" or "track"', () => {
    expect(resolveStatIconName('Monthly goal')).toBe('Layers')
    expect(resolveStatIconName('Target amount')).toBe('Layers')
    expect(resolveStatIconName('On track')).toBe('Layers')
  })

  it('returns TrendingUp as default for unknown labels', () => {
    expect(resolveStatIconName('Portfolio value')).toBe('TrendingUp')
    expect(resolveStatIconName('')).toBe('TrendingUp')
    expect(resolveStatIconName('Random metric')).toBe('TrendingUp')
  })

  it('matching is case-insensitive', () => {
    expect(resolveStatIconName('EXPENSE')).toBe('PiggyBank')
    expect(resolveStatIconName('INCOME')).toBe('Wallet')
    expect(resolveStatIconName('Budget')).toBe('Layers')
  })
})

// ---------------------------------------------------------------------------
// buildTabParams
// ---------------------------------------------------------------------------

describe('buildTabParams()', () => {
  it('removes the tab param when switching to "overview"', () => {
    expect(buildTabParams('tab=transactions', 'overview')).toBe('')
  })

  it('sets tab param for non-overview tabs', () => {
    expect(buildTabParams('', 'budgets')).toBe('tab=budgets')
    expect(buildTabParams('', 'transactions')).toBe('tab=transactions')
  })

  it('replaces an existing tab param', () => {
    expect(buildTabParams('tab=budgets', 'transactions')).toBe('tab=transactions')
  })

  it('preserves unrelated query params', () => {
    const result = buildTabParams('month=2024-03&tab=overview', 'budgets')
    expect(result).toContain('month=2024-03')
    expect(result).toContain('tab=budgets')
  })

  it('removes tab param while preserving other params when switching to overview', () => {
    const result = buildTabParams('month=2024-03&tab=budgets', 'overview')
    expect(result).toBe('month=2024-03')
    expect(result).not.toContain('tab=')
  })
})

// ---------------------------------------------------------------------------
// buildActivityFeed
// ---------------------------------------------------------------------------

describe('buildActivityFeed()', () => {
  function makeTx(overrides: Partial<ActivityTransaction> = {}): ActivityTransaction {
    return {
      id: 'tx-1',
      type: 'EXPENSE',
      amount: 50,
      description: null,
      category: { name: 'Groceries', color: '#22c55e' },
      date: new Date('2024-03-15'),
      ...overrides,
    }
  }

  it('returns at most 5 items', () => {
    const txs = Array.from({ length: 10 }, (_, i) => makeTx({ id: `tx-${i}` }))
    expect(buildActivityFeed(txs)).toHaveLength(5)
  })

  it('returns all items when fewer than 5 are provided', () => {
    const txs = [makeTx({ id: 'a' }), makeTx({ id: 'b' })]
    expect(buildActivityFeed(txs)).toHaveLength(2)
  })

  it('returns an empty array for an empty list', () => {
    expect(buildActivityFeed([])).toEqual([])
  })

  it('gives INCOME transactions a "+" prefix', () => {
    const items = buildActivityFeed([makeTx({ type: 'INCOME' })])
    expect(items[0].prefix).toBe('+')
  })

  it('gives EXPENSE transactions a "-" prefix', () => {
    const items = buildActivityFeed([makeTx({ type: 'EXPENSE' })])
    expect(items[0].prefix).toBe('-')
  })

  it('gives non-INCOME transactions a "-" prefix', () => {
    const items = buildActivityFeed([makeTx({ type: 'UNKNOWN' })])
    expect(items[0].prefix).toBe('-')
  })

  it('uses description when present', () => {
    const items = buildActivityFeed([makeTx({ description: 'Coffee run' })])
    expect(items[0].label).toBe('Coffee run')
  })

  it('falls back to category name when description is absent', () => {
    const items = buildActivityFeed([makeTx({ description: null, category: { name: 'Transport', color: null } })])
    expect(items[0].label).toBe('Transport')
  })

  it('falls back to "Transaction" when both description and category name are absent', () => {
    const items = buildActivityFeed([makeTx({ description: null, category: null })])
    expect(items[0].label).toBe('Transaction')
  })

  it('uses category color when present', () => {
    const items = buildActivityFeed([makeTx({ category: { name: 'Food', color: '#ff0000' } })])
    expect(items[0].color).toBe('#ff0000')
  })

  it('falls back to default color when category color is absent', () => {
    const items = buildActivityFeed([makeTx({ category: { name: 'Food', color: null } })])
    expect(items[0].color).toBe('#0ea5e9')
  })

  it('preserves absolute amount (no sign applied to amount field)', () => {
    const items = buildActivityFeed([makeTx({ type: 'EXPENSE', amount: -150 })])
    expect(items[0].amount).toBe(150)
  })
})

// ---------------------------------------------------------------------------
// calculateIncomeProgress
// ---------------------------------------------------------------------------

describe('calculateIncomeProgress()', () => {
  describe('normal progress', () => {
    it('returns 0 when actual income is 0', () => {
      expect(calculateIncomeProgress(0, 5000)).toBe(0)
    })

    it('returns 50 when actual is half the goal', () => {
      expect(calculateIncomeProgress(2500, 5000)).toBe(50)
    })

    it('returns 100 when actual equals the goal', () => {
      expect(calculateIncomeProgress(5000, 5000)).toBe(100)
    })

    it('clamps to 100 when actual exceeds the goal', () => {
      expect(calculateIncomeProgress(6000, 5000)).toBe(100)
      expect(calculateIncomeProgress(10000, 5000)).toBe(100)
    })

    it('returns a fractional percentage for non-round values', () => {
      const result = calculateIncomeProgress(1, 3)
      expect(result).toBeCloseTo(33.333, 2)
    })
  })

  describe('zero or missing goal', () => {
    it('returns 0 when goal is 0', () => {
      expect(calculateIncomeProgress(5000, 0)).toBe(0)
    })

    it('returns 0 when goal is negative', () => {
      expect(calculateIncomeProgress(5000, -100)).toBe(0)
    })

    it('returns 0 when both actual and goal are 0', () => {
      expect(calculateIncomeProgress(0, 0)).toBe(0)
    })
  })

  describe('edge cases', () => {
    it('handles very small amounts', () => {
      expect(calculateIncomeProgress(0.01, 0.01)).toBe(100)
    })

    it('handles large amounts without overflow', () => {
      expect(calculateIncomeProgress(1_000_000, 2_000_000)).toBe(50)
    })

    it('returns 100 for large overshoot', () => {
      expect(calculateIncomeProgress(999_999_999, 1)).toBe(100)
    })
  })
})
