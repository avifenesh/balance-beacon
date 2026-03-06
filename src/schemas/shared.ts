import { z } from 'zod'

/** DB uses Decimal(12,2) — max representable value */
export const DECIMAL_12_2_MAX = 9999999999.99

/** YYYY-MM with valid month range 01-12 */
export const monthKey = z.string().regex(/^\d{4}-(0[1-9]|1[0-2])$/, 'Invalid month format (expected YYYY-MM)')
