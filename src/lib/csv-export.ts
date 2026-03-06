import { TransactionType } from '@prisma/client'

export type TransactionForCsv = {
  date: Date | string
  type: TransactionType
  category: { name: string }
  account: { name: string }
  amount: number
  currency: string
  description: string | null
}

/**
 * Builds raw cell arrays for each transaction row.
 *
 * Rules:
 * - EXPENSE amounts are negated; INCOME amounts are positive.
 * - Any `"` character in description is escaped as `""` (RFC 4180).
 * - Date is formatted as YYYY-MM-DD.
 */
export function buildTransactionCsvRows(transactions: TransactionForCsv[]): (string | number)[][] {
  return transactions.map((t) => [
    new Date(t.date).toISOString().slice(0, 10),
    t.type,
    t.category.name,
    t.account.name,
    t.type === TransactionType.EXPENSE ? -t.amount : t.amount,
    t.currency,
    (t.description || '').replace(/"/g, '""'),
  ])
}

/**
 * Serialises headers and rows into a CSV string.
 *
 * Every cell is wrapped in double-quotes so that commas, newlines, and
 * already-escaped `""` sequences are handled correctly.
 */
export function formatCsvContent(headers: string[], rows: (string | number)[][]): string {
  const headerLine = headers.join(',')
  const dataLines = rows.map((row) => row.map((cell) => `"${cell}"`).join(','))
  return [headerLine, ...dataLines].join('\n')
}
