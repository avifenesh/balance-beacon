'use client'

import { useState, useEffect } from 'react'
import { TabValue } from '@/components/dashboard/dashboard-types'

export function useDashboardState(initialActiveAccount: string) {
  const [activeAccount, setActiveAccount] = useState<string>(initialActiveAccount)
  const [activeTab, setActiveTab] = useState<TabValue>('overview')
  const [expandedStat, setExpandedStat] = useState<string | null>(null)
  const [showBalanceForm, setShowBalanceForm] = useState(false)
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const [showExportDialog, setShowExportDialog] = useState(false)

  // Handle ESC key to close breakdown panel
  useEffect(() => {
    if (!expandedStat) return

    const handleEscKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setExpandedStat(null)
      }
    }

    document.addEventListener('keydown', handleEscKey)
    return () => document.removeEventListener('keydown', handleEscKey)
  }, [expandedStat])

  return {
    activeAccount,
    setActiveAccount,
    activeTab,
    setActiveTab,
    expandedStat,
    setExpandedStat,
    showBalanceForm,
    setShowBalanceForm,
    showDeleteDialog,
    setShowDeleteDialog,
    showExportDialog,
    setShowExportDialog,
  }
}
