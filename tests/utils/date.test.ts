import { describe, expect, it } from 'vitest'
import {
  getMonthStart,
  getMonthStartFromKey,
  getMonthKey,
  formatMonthLabel,
  shiftMonth,
  normalizeDateInput,
  formatDateForApi,
} from '@/utils/date'

describe('getMonthStart', () => {
  it('returns first day of the month', () => {
    const date = new Date(2026, 2, 15) // March 15, 2026
    const result = getMonthStart(date)
    expect(result.getDate()).toBe(1)
    expect(result.getMonth()).toBe(2)
    expect(result.getFullYear()).toBe(2026)
  })

  it('returns same date if already first day', () => {
    const date = new Date(2026, 0, 1) // Jan 1, 2026
    const result = getMonthStart(date)
    expect(result.getDate()).toBe(1)
    expect(result.getMonth()).toBe(0)
  })

  it('handles last day of month', () => {
    const date = new Date(2026, 0, 31) // Jan 31, 2026
    const result = getMonthStart(date)
    expect(result.getDate()).toBe(1)
    expect(result.getMonth()).toBe(0)
  })
})

describe('getMonthStartFromKey', () => {
  it('parses standard month key', () => {
    const result = getMonthStartFromKey('2026-03')
    expect(result.getUTCFullYear()).toBe(2026)
    expect(result.getUTCMonth()).toBe(2) // March = 2
    expect(result.getUTCDate()).toBe(1)
  })

  it('parses January correctly', () => {
    const result = getMonthStartFromKey('2026-01')
    expect(result.getUTCMonth()).toBe(0)
    expect(result.getUTCDate()).toBe(1)
  })

  it('parses December correctly', () => {
    const result = getMonthStartFromKey('2026-12')
    expect(result.getUTCMonth()).toBe(11)
  })

  it('returns UTC date', () => {
    const result = getMonthStartFromKey('2026-06')
    expect(result.getUTCHours()).toBe(0)
    expect(result.getUTCMinutes()).toBe(0)
    expect(result.getUTCSeconds()).toBe(0)
  })
})

describe('getMonthKey', () => {
  it('formats date to yyyy-MM', () => {
    const date = new Date(2026, 2, 15) // March 15, 2026
    expect(getMonthKey(date)).toBe('2026-03')
  })

  it('pads single-digit months', () => {
    const date = new Date(2026, 0, 1) // January
    expect(getMonthKey(date)).toBe('2026-01')
  })

  it('handles December', () => {
    const date = new Date(2026, 11, 31) // December 31
    expect(getMonthKey(date)).toBe('2026-12')
  })
})

describe('formatMonthLabel', () => {
  it('formats month key to human-readable label', () => {
    expect(formatMonthLabel('2026-03')).toBe('March 2026')
  })

  it('formats January correctly', () => {
    expect(formatMonthLabel('2026-01')).toBe('January 2026')
  })

  it('formats December correctly', () => {
    expect(formatMonthLabel('2026-12')).toBe('December 2026')
  })
})

describe('shiftMonth', () => {
  it('shifts forward by one month', () => {
    expect(shiftMonth('2026-03', 1)).toBe('2026-04')
  })

  it('shifts backward by one month', () => {
    expect(shiftMonth('2026-03', -1)).toBe('2026-02')
  })

  it('wraps from December to January (year boundary)', () => {
    expect(shiftMonth('2026-12', 1)).toBe('2027-01')
  })

  it('wraps from January to December (year boundary backward)', () => {
    expect(shiftMonth('2026-01', -1)).toBe('2025-12')
  })

  it('handles multi-month shifts', () => {
    expect(shiftMonth('2026-01', 13)).toBe('2027-02')
  })

  it('handles large backward shifts', () => {
    expect(shiftMonth('2026-03', -15)).toBe('2024-12')
  })

  it('returns same month with offset 0', () => {
    expect(shiftMonth('2026-06', 0)).toBe('2026-06')
  })
})

describe('normalizeDateInput', () => {
  it('parses valid date string', () => {
    const result = normalizeDateInput('2026-03-15')
    expect(result).not.toBeNull()
    expect(result!.getUTCFullYear()).toBe(2026)
    expect(result!.getUTCMonth()).toBe(2)
    expect(result!.getUTCDate()).toBe(15)
  })

  it('returns null for null input', () => {
    expect(normalizeDateInput(null)).toBeNull()
  })

  it('returns null for empty string', () => {
    expect(normalizeDateInput('')).toBeNull()
  })

  it('returns null for whitespace-only string', () => {
    expect(normalizeDateInput('   ')).toBeNull()
  })

  it('returns null for non-string input', () => {
    // FormDataEntryValue can be File
    expect(normalizeDateInput(new File([], 'test.txt'))).toBeNull()
  })

  it('returns null for incomplete date (missing day)', () => {
    expect(normalizeDateInput('2026-03')).toBeNull()
  })

  it('returns null for invalid month (0)', () => {
    expect(normalizeDateInput('2026-00-15')).toBeNull()
  })

  it('returns null for invalid month (13)', () => {
    expect(normalizeDateInput('2026-13-15')).toBeNull()
  })

  it('returns null for invalid day (0)', () => {
    expect(normalizeDateInput('2026-03-00')).toBeNull()
  })

  it('returns null for invalid day (32)', () => {
    expect(normalizeDateInput('2026-03-32')).toBeNull()
  })

  it('returns null for Feb 30 (date rollover)', () => {
    expect(normalizeDateInput('2026-02-30')).toBeNull()
  })

  it('returns null for Feb 29 in non-leap year', () => {
    expect(normalizeDateInput('2025-02-29')).toBeNull()
  })

  it('accepts Feb 29 in leap year', () => {
    const result = normalizeDateInput('2024-02-29')
    expect(result).not.toBeNull()
    expect(result!.getUTCDate()).toBe(29)
  })

  it('returns null for non-numeric parts', () => {
    expect(normalizeDateInput('abc-03-15')).toBeNull()
  })

  it('returns null for floating point numbers', () => {
    expect(normalizeDateInput('2026-3.5-15')).toBeNull()
  })

  it('handles first day of year', () => {
    const result = normalizeDateInput('2026-01-01')
    expect(result).not.toBeNull()
    expect(result!.getUTCMonth()).toBe(0)
    expect(result!.getUTCDate()).toBe(1)
  })

  it('handles last day of year', () => {
    const result = normalizeDateInput('2026-12-31')
    expect(result).not.toBeNull()
    expect(result!.getUTCMonth()).toBe(11)
    expect(result!.getUTCDate()).toBe(31)
  })
})

describe('formatDateForApi', () => {
  it('formats date to YYYY-MM-DD', () => {
    const date = new Date(2026, 2, 15) // March 15, 2026
    const result = formatDateForApi(date)
    expect(result).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })

  it('pads single-digit month and day', () => {
    const date = new Date(2026, 0, 5) // Jan 5
    const result = formatDateForApi(date)
    expect(result).toContain('-01-')
    expect(result).toContain('-05')
  })
})
