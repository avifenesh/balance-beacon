/**
 * Pure helper functions extracted from dashboard-page.tsx for testability.
 * These functions contain no React or Next.js dependencies.
 */

/**
 * Maps a stat label string to one of four icon identifiers.
 * Matching is case-insensitive and uses substring inclusion.
 *
 * Wallet    - "net", "saved", "income", "inflow"
 * PiggyBank - "spend", "expense", "outflow"
 * Layers    - "target", "goal", "budget", "track"
 * TrendingUp - everything else (default)
 */
export type StatIconName = 'Wallet' | 'PiggyBank' | 'Layers' | 'TrendingUp'

export function resolveStatIconName(label: string): StatIconName {
  const normalized = label.toLowerCase()
  if (['net', 'saved', 'income', 'inflow'].some((kw) => normalized.includes(kw))) {
    return 'Wallet'
  }
  if (['spend', 'expense', 'outflow'].some((kw) => normalized.includes(kw))) {
    return 'PiggyBank'
  }
  if (['target', 'goal', 'budget', 'track'].some((kw) => normalized.includes(kw))) {
    return 'Layers'
  }
  return 'TrendingUp'
}

/**
 * Produces the URLSearchParams string for a tab change.
 * - 'overview' removes the tab param (clean URL).
 * - Any other tab sets tab=<value>.
 * Preserves all existing params.
 *
 * Returns the new query string (without leading "?"), or an empty string
 * when no params remain.
 */
export function buildTabParams(currentSearch: string, tab: string): string {
  const params = new URLSearchParams(currentSearch)
  if (tab === 'overview') {
    params.delete('tab')
  } else {
    params.set('tab', tab)
  }
  return params.toString()
}

export type ActivityTransaction = {
  id: string
  type: string
  amount: number
  description?: string | null
  category?: { name?: string | null; color?: string | null } | null
  date: Date | string
}

export type ActivityItem = {
  id: string
  label: string
  prefix: '+' | '-'
  amount: number
  color: string
  date: Date | string
}

/**
 * Converts a list of transactions into the activity feed items shown in the
 * dashboard overview:
 * - At most 5 items (first 5 of the supplied list).
 * - INCOME transactions get a "+" prefix.
 * - EXPENSE (or any non-INCOME) transactions get a "-" prefix.
 * - Falls back to category name, then "Transaction" when description is absent.
 * - Falls back to "#0ea5e9" when no category colour is present.
 */
export function buildActivityFeed(transactions: ActivityTransaction[]): ActivityItem[] {
  return transactions.slice(0, 5).map((tx) => ({
    id: tx.id,
    label: tx.description || tx.category?.name || 'Transaction',
    prefix: tx.type === 'INCOME' ? '+' : '-',
    amount: Math.abs(tx.amount),
    color: tx.category?.color || '#0ea5e9',
    date: tx.date,
  }))
}

/**
 * Calculates the percentage of an income goal that has been reached.
 *
 * Returns a value in the range [0, 100]:
 * - 0 when goalAmount is 0 or negative (no goal set).
 * - Clamped to 100 when actualIncome exceeds the goal.
 */
export function calculateIncomeProgress(actualIncome: number, goalAmount: number): number {
  if (goalAmount <= 0) return 0
  return Math.min((actualIncome / goalAmount) * 100, 100)
}
