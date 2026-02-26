'use client'

import { useCallback } from 'react'
import { usePathname, useRouter, useSearchParams } from 'next/navigation'
import { shiftMonth } from '@/utils/date'

export function useDashboardNavigation(monthKey: string) {
  const router = useRouter()
  const pathname = usePathname()
  const searchParams = useSearchParams()

  const handleParamUpdate = useCallback((key: string, value?: string) => {
    const params = new URLSearchParams(searchParams.toString())
    if (value) {
      params.set(key, value)
    } else {
      params.delete(key)
    }
    const query = params.toString()
    router.push(query ? `${pathname}?${query}` : pathname)
  }, [pathname, router, searchParams])

  const handleMonthChange = useCallback((direction: number) => {
    const nextKey = shiftMonth(monthKey, direction)
    handleParamUpdate('month', nextKey)
  }, [monthKey, handleParamUpdate])

  return {
    router,
    pathname,
    searchParams,
    handleParamUpdate,
    handleMonthChange,
  }
}
