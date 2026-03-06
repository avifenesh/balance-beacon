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
    t.description || '',
  ])
}

function escapeCsvCell(cell: string | number): string {
  if (typeof cell !== 'string') return `"${cell}"`
  // Mitigate spreadsheet formula injection (=, +, -, @)
  const safe = /^[=+\-@]/.test(cell) ? `'${cell}` : cell
  // Escape embedded double-quotes per RFC 4180
  return `"${safe.replace(/"/g, '""')}"`
}

/**
 * Serialises headers and rows into a CSV string.
 *
 * Every string cell is escaped (embedded `"` → `""`) and sanitized
 * against spreadsheet formula injection before quoting.
 */
export function formatCsvContent(headers: string[], rows: (string | number)[][]): string {
  const headerLine = headers.join(',')
  const dataLines = rows.map((row) => row.map(escapeCsvCell).join(','))
  return [headerLine, ...dataLines].join('\n')
}
