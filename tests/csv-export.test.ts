import { describe, it, expect } from 'vitest'
import { TransactionType } from '@prisma/client'
import { buildTransactionCsvRows, formatCsvContent, type TransactionForCsv } from '@/lib/csv-export'

// ---------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------

function makeTransaction(overrides: Partial<TransactionForCsv> = {}): TransactionForCsv {
  return {
    date: new Date('2024-03-15T00:00:00.000Z'),
    type: TransactionType.EXPENSE,
    category: { name: 'Groceries' },
    account: { name: 'Personal' },
    amount: 50.25,
    currency: 'USD',
    description: null,
    ...overrides,
  }
}

// ---------------------------------------------------------------------------
// buildTransactionCsvRows
// ---------------------------------------------------------------------------

describe('buildTransactionCsvRows()', () => {
  describe('amount sign', () => {
    it('negates amount for EXPENSE transactions', () => {
      const rows = buildTransactionCsvRows([makeTransaction({ type: TransactionType.EXPENSE, amount: 100 })])
      const amountCell = rows[0][4]
      expect(amountCell).toBe(-100)
    })

    it('keeps amount positive for INCOME transactions', () => {
      const rows = buildTransactionCsvRows([makeTransaction({ type: TransactionType.INCOME, amount: 200 })])
      const amountCell = rows[0][4]
      expect(amountCell).toBe(200)
    })

    it('handles zero amounts without sign change', () => {
      const expenseRows = buildTransactionCsvRows([makeTransaction({ type: TransactionType.EXPENSE, amount: 0 })])
      expect(expenseRows[0][4]).toBe(-0)

      const incomeRows = buildTransactionCsvRows([makeTransaction({ type: TransactionType.INCOME, amount: 0 })])
      expect(incomeRows[0][4]).toBe(0)
    })

    it('handles fractional amounts correctly', () => {
      const rows = buildTransactionCsvRows([makeTransaction({ type: TransactionType.EXPENSE, amount: 9.99 })])
      expect(rows[0][4]).toBe(-9.99)
    })
  })

  describe('date formatting', () => {
    it('formats date as YYYY-MM-DD', () => {
      const rows = buildTransactionCsvRows([makeTransaction({ date: new Date('2024-03-15T14:30:00.000Z') })])
      expect(rows[0][0]).toBe('2024-03-15')
    })

    it('formats first-of-month dates correctly', () => {
      const rows = buildTransactionCsvRows([makeTransaction({ date: new Date('2024-01-01T00:00:00.000Z') })])
      expect(rows[0][0]).toBe('2024-01-01')
    })

    it('formats last-of-month dates correctly', () => {
      const rows = buildTransactionCsvRows([makeTransaction({ date: new Date('2024-12-31T23:59:59.000Z') })])
      expect(rows[0][0]).toBe('2024-12-31')
    })

    it('accepts a date string as well as a Date object', () => {
      const rows = buildTransactionCsvRows([makeTransaction({ date: '2025-07-04T00:00:00.000Z' })])
      expect(rows[0][0]).toBe('2025-07-04')
    })
  })

  describe('description handling', () => {
    it('passes description through without pre-escaping (escaping done by formatCsvContent)', () => {
      const rows = buildTransactionCsvRows([makeTransaction({ description: 'He said "hello"' })])
      expect(rows[0][6]).toBe('He said "hello"')
    })

    it('leaves descriptions without quotes unchanged', () => {
      const rows = buildTransactionCsvRows([makeTransaction({ description: 'Weekly groceries' })])
      expect(rows[0][6]).toBe('Weekly groceries')
    })

    it('uses empty string when description is null', () => {
      const rows = buildTransactionCsvRows([makeTransaction({ description: null })])
      expect(rows[0][6]).toBe('')
    })

    it('uses empty string when description is an empty string', () => {
      const rows = buildTransactionCsvRows([makeTransaction({ description: '' })])
      expect(rows[0][6]).toBe('')
    })
  })

  describe('row structure', () => {
    it('produces a row with 7 cells in the correct column order', () => {
      const t = makeTransaction({
        date: new Date('2024-06-01T00:00:00.000Z'),
        type: TransactionType.INCOME,
        category: { name: 'Salary' },
        account: { name: 'Main' },
        amount: 3000,
        currency: 'EUR',
        description: 'June paycheck',
      })
      const rows = buildTransactionCsvRows([t])
      expect(rows).toHaveLength(1)
      const [date, type, category, account, amount, currency, description] = rows[0]
      expect(date).toBe('2024-06-01')
      expect(type).toBe(TransactionType.INCOME)
      expect(category).toBe('Salary')
      expect(account).toBe('Main')
      expect(amount).toBe(3000)
      expect(currency).toBe('EUR')
      expect(description).toBe('June paycheck')
    })

    it('returns an empty array for an empty transaction list', () => {
      expect(buildTransactionCsvRows([])).toEqual([])
    })

    it('returns one row per transaction', () => {
      const transactions = [
        makeTransaction({ amount: 10 }),
        makeTransaction({ amount: 20 }),
        makeTransaction({ amount: 30 }),
      ]
      expect(buildTransactionCsvRows(transactions)).toHaveLength(3)
    })
  })
})

// ---------------------------------------------------------------------------
// formatCsvContent
// ---------------------------------------------------------------------------

describe('formatCsvContent()', () => {
  const headers = ['Date', 'Type', 'Category', 'Account', 'Amount', 'Currency', 'Description']

  describe('header line', () => {
    it('joins headers with commas and no quotes', () => {
      const csv = formatCsvContent(headers, [])
      const firstLine = csv.split('\n')[0]
      expect(firstLine).toBe('Date,Type,Category,Account,Amount,Currency,Description')
    })
  })

  describe('data rows', () => {
    it('wraps each cell in double-quotes', () => {
      const rows = [['2024-01-15', 'EXPENSE', 'Groceries', 'Personal', -50.25, 'USD', '']]
      const csv = formatCsvContent(headers, rows)
      const dataLine = csv.split('\n')[1]
      expect(dataLine).toBe('"2024-01-15","EXPENSE","Groceries","Personal","-50.25","USD",""')
    })

    it('separates rows with newlines', () => {
      const rows = [
        ['2024-01-15', 'EXPENSE', 'Groceries', 'Personal', -50, 'USD', ''],
        ['2024-01-16', 'INCOME', 'Salary', 'Main', 3000, 'EUR', ''],
      ]
      const csv = formatCsvContent(headers, rows)
      const lines = csv.split('\n')
      expect(lines).toHaveLength(3) // 1 header + 2 data rows
    })

    it('produces only the header line when rows array is empty', () => {
      const csv = formatCsvContent(headers, [])
      expect(csv).toBe('Date,Type,Category,Account,Amount,Currency,Description')
    })
  })

  describe('quote escaping', () => {
    it('escapes embedded double-quotes as "" per RFC 4180', () => {
      const rows = [['He said "hello"']]
      const csv = formatCsvContent(['Col'], rows)
      expect(csv.split('\n')[1]).toBe('"He said ""hello"""')
    })

    it('escapes quotes in all string cells, not just description', () => {
      const rows = [['Category "A"', 'Account "B"', 100, 'Desc "C"']]
      const csv = formatCsvContent(['C', 'A', 'Amt', 'D'], rows)
      const line = csv.split('\n')[1]
      expect(line).toContain('"Category ""A"""')
      expect(line).toContain('"Account ""B"""')
      expect(line).toContain('"Desc ""C"""')
    })
  })

  describe('formula injection mitigation', () => {
    it.each(['=SUM(A1)', '+cmd|', '-1+1', '@import'])('prefixes risky string "%s" with apostrophe', (val) => {
      const rows = [[val]]
      const csv = formatCsvContent(['Col'], rows)
      const cell = csv.split('\n')[1]
      expect(cell.startsWith(`"'`)).toBe(true)
    })

    it('does not prefix safe strings', () => {
      const rows = [['Normal text']]
      const csv = formatCsvContent(['Col'], rows)
      expect(csv.split('\n')[1]).toBe('"Normal text"')
    })

    it('does not prefix numeric cells', () => {
      const rows = [[-100]]
      const csv = formatCsvContent(['Col'], rows)
      expect(csv.split('\n')[1]).toBe('"-100"')
    })
  })

  describe('integration: buildTransactionCsvRows + formatCsvContent', () => {
    it('produces a valid two-line CSV for a single EXPENSE', () => {
      const transactions = [
        makeTransaction({
          date: new Date('2024-03-01T00:00:00.000Z'),
          type: TransactionType.EXPENSE,
          category: { name: 'Rent' },
          account: { name: 'Checking' },
          amount: 1200,
          currency: 'USD',
          description: null,
        }),
      ]
      const rows = buildTransactionCsvRows(transactions)
      const csv = formatCsvContent(headers, rows)
      const lines = csv.split('\n')

      expect(lines[0]).toBe('Date,Type,Category,Account,Amount,Currency,Description')
      expect(lines[1]).toBe('"2024-03-01","EXPENSE","Rent","Checking","-1200","USD",""')
    })

    it('escapes description quotes in final CSV output', () => {
      const transactions = [makeTransaction({ description: 'Bought "organic" milk' })]
      const rows = buildTransactionCsvRows(transactions)
      const csv = formatCsvContent(headers, rows)
      // formatCsvContent escapes embedded " as "" per RFC 4180
      expect(csv).toContain('"Bought ""organic"" milk"')
    })

    it('produces correct CSV for an INCOME transaction', () => {
      const transactions = [
        makeTransaction({
          date: new Date('2024-05-31T00:00:00.000Z'),
          type: TransactionType.INCOME,
          category: { name: 'Freelance' },
          account: { name: 'Business' },
          amount: 750.5,
          currency: 'ILS',
          description: 'Project payment',
        }),
      ]
      const rows = buildTransactionCsvRows(transactions)
      const csv = formatCsvContent(headers, rows)
      const dataLine = csv.split('\n')[1]
      expect(dataLine).toBe('"2024-05-31","INCOME","Freelance","Business","750.5","ILS","Project payment"')
    })
  })
})
