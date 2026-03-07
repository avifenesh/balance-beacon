const fs = require('fs')
const path = require('path')

const testFiles = [
  'tests/api/accounts/update.test.ts',
  'tests/api/v1/accounts-activate.test.ts',
  'tests/api/v1/accounts-set-balance.test.ts',
  'tests/api/v1/auth-login.test.ts',
  'tests/api/v1/auth-password-reset.test.ts',
  'tests/api/v1/auth-resend-verification.test.ts',
  'tests/api/v1/budgets-income-goal.test.ts',
  'tests/api/v1/budgets-quick.test.ts',
  'tests/api/v1/budgets.test.ts',
  'tests/api/v1/categories.test.ts',
  'tests/api/v1/expenses-share.test.ts',
  'tests/api/v1/expenses-shared-by-me.test.ts',
  'tests/api/v1/expenses-shared-with-me.test.ts',
  'tests/api/v1/expenses-shares-delete.test.ts',
  'tests/api/v1/expenses-shares-paid.test.ts',
  'tests/api/v1/expenses-shares-remind.test.ts',
  'tests/api/v1/holdings.test.ts',
  'tests/api/v1/rate-limiting.test.ts',
  'tests/api/v1/recurring.test.ts',
  'tests/api/v1/seed-data.test.ts',
  'tests/api/v1/sharing.test.ts',
  'tests/api/v1/subscription-enforcement.test.ts',
  'tests/api/v1/transactions.test.ts',
  'tests/api/v1/users-currency.test.ts',
]

testFiles.forEach((file) => {
  const filePath = path.join(process.cwd(), file)
  if (!fs.existsSync(filePath)) {
    console.log(`Skipping ${file} - not found`)
    return
  }

  let content = fs.readFileSync(filePath, 'utf8')

  // Skip if already has reset or mock
  if (content.includes('resetAllRateLimits') || content.includes("vi.mock('@/lib/rate-limit'")) {
    console.log(`Skipping ${file} - already has reset or mock`)
    return
  }

  // Add import after vitest import
  content = content.replace(
    /(import\s+{[^}]+}\s+from\s+['"]vitest['"])/,
    "$1\nimport { resetAllRateLimits } from '@/lib/rate-limit'",
  )

  // Add reset call in beforeEach after resetEnvCache
  content = content.replace(/(\s+resetEnvCache\(\))/g, '$1\n    await resetAllRateLimits()')

  // Also update existing resetAllRateLimits() calls to await them
  content = content.replace(/(\s+)resetAllRateLimits\(\)/g, '$1await resetAllRateLimits()')

  fs.writeFileSync(filePath, content, 'utf8')
  console.log(`Updated ${file}`)
})

console.log('\nDone!')
