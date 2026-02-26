'use client'

import { useState, useTransition, useCallback, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { logoutAction, persistActiveAccountAction, refreshExchangeRatesAction } from '@/app/actions'
import { Feedback } from '@/components/dashboard/dashboard-types'

export function useDashboardActions(
  csrfToken: string | null,
  updateParam: (key: string, value: string) => void,
  setActiveAccount: (accountId: string) => void,
  accountsOptions: Array<{ label: string; value: string }>
) {
  const router = useRouter()
  const [accountFeedback, setAccountFeedback] = useState<Feedback | null>(null)
  const [, startPersistAccount] = useTransition()
  const [isPendingLogout, startLogout] = useTransition()
  const [isPendingRates, startRates] = useTransition()

  const handleAccountSelect = useCallback((value: string) => {
    setActiveAccount(value)
    updateParam('account', value)
    const accountLabel = accountsOptions.find((option) => option.value === value)?.label ?? 'Account'

    if (!csrfToken) {
        setAccountFeedback({ type: 'error', message: 'Security token missing. Please refresh.' })
        return
    }

    startPersistAccount(async () => {
      const result = await persistActiveAccountAction({ accountId: value, csrfToken })
      if ('error' in result && result.error) {
        const firstErrorSet = Object.values(result.error)[0]
        const message: string =
          Array.isArray(firstErrorSet) && firstErrorSet.length > 0
            ? (firstErrorSet[0] ?? 'Unable to remember selection.')
            : 'Unable to remember selection.'
        setAccountFeedback({ type: 'error', message })
        return
      }
      setAccountFeedback({ type: 'success', message: `${accountLabel} will open by default next time.` })
    })
  }, [csrfToken, updateParam, setActiveAccount, accountsOptions])

  const handleLogout = useCallback(() => {
    startLogout(async () => {
      await logoutAction()
      router.push('/login')
      router.refresh()
    })
  }, [router])

  const handleRefreshRates = useCallback(() => {
    if (!csrfToken) {
      return
    }
    startRates(async () => {
      await refreshExchangeRatesAction({ csrfToken })
      router.refresh()
    })
  }, [csrfToken, router])

  useEffect(() => {
    if (!accountFeedback) return
    const timer = window.setTimeout(() => setAccountFeedback(null), 4000)
    return () => window.clearTimeout(timer)
  }, [accountFeedback])

  return {
    handleAccountSelect,
    handleLogout,
    handleRefreshRates,
    isPendingLogout,
    isPendingRates,
    accountFeedback,
  }
}
