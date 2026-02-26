'use client'

import { useRef, useState } from 'react'
import {
  CreditCard,
  ChevronDown,
  FileSpreadsheet,
  Gauge,
  Repeat,
  Scale,
  Settings,
  Tags,
  TrendingUp,
  Users,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/utils/cn'
import { TabValue } from './dashboard-types'
import { DashboardSettingsMenu } from './dashboard-settings-menu'

const TABS: Array<{
  value: TabValue
  label: string
  icon: React.ComponentType<{ className?: string }>
}> = [
  { value: 'overview', label: 'Overview', icon: Gauge },
  { value: 'transactions', label: 'Transactions', icon: CreditCard },
  { value: 'budgets', label: 'Budgets', icon: FileSpreadsheet },
  { value: 'recurring', label: 'Auto-repeat', icon: Repeat },
  { value: 'categories', label: 'Labels', icon: Tags },
  { value: 'holdings', label: 'Investments', icon: TrendingUp },
  { value: 'sharing', label: 'Sharing', icon: Users },
]

type DashboardNavProps = {
  activeTab: TabValue
  setActiveTab: (tab: TabValue) => void
  onToggleBalanceForm: () => void
  onExport: () => void
  onLogout: () => void
  onDelete: () => void
  isPendingLogout: boolean
}

export function DashboardNav({
  activeTab,
  setActiveTab,
  onToggleBalanceForm,
  onExport,
  onLogout,
  onDelete,
  isPendingLogout,
}: DashboardNavProps) {
  const [showSettingsMenu, setShowSettingsMenu] = useState(false)
  const settingsButtonRef = useRef<HTMLButtonElement>(null)

  return (
    <div className="fixed left-0 right-0 top-0 z-50 border-b border-white/10 bg-slate-900/95 px-4 py-2.5 backdrop-blur-md lg:px-6 lg:py-3">
      <div className="mx-auto flex max-w-7xl items-center justify-between gap-2">
        {/* Tab navigation */}
        <div role="tablist" aria-label="Dashboard sections" className="flex items-center gap-1 overflow-x-auto">
          {TABS.map(({ value, label, icon: Icon }) => (
            <Button
              key={value}
              type="button"
              variant="ghost"
              role="tab"
              id={`tab-${value}`}
              aria-selected={activeTab === value}
              aria-controls={`panel-${value}`}
              className={cn(
                'h-9 gap-1.5 rounded-full px-3.5 py-2 text-xs font-medium transition',
                activeTab === value ? 'bg-white/20 text-white' : 'text-white/70 hover:bg-white/10 hover:text-white',
              )}
              onClick={() => setActiveTab(value)}
              title={label}
            >
              <Icon className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">{label}</span>
            </Button>
          ))}
          <div className="mx-1 h-5 w-px bg-white/20" />
          <Button
            type="button"
            variant="ghost"
            className="h-9 gap-1.5 rounded-full px-3.5 py-2 text-xs font-medium text-white/70 hover:bg-white/10 hover:text-white"
            onClick={onToggleBalanceForm}
            title="Set balance"
          >
            <Scale className="h-3.5 w-3.5" />
            <span className="hidden lg:inline">Balance</span>
          </Button>
        </div>

        {/* Right side actions - Settings dropdown */}
        <div className="relative">
          <Button
            ref={settingsButtonRef}
            type="button"
            variant="ghost"
            className="h-8 gap-1.5 rounded-full px-3 text-xs font-medium text-white/70 hover:bg-white/10 hover:text-white"
            onClick={() => setShowSettingsMenu((prev) => !prev)}
            aria-haspopup="menu"
            aria-expanded={showSettingsMenu}
            aria-controls="settings-menu"
          >
            <Settings className="h-3.5 w-3.5" />
            <span className="hidden sm:inline">Account</span>
            <ChevronDown className="h-3 w-3" />
          </Button>
          {showSettingsMenu && (
            <DashboardSettingsMenu
              anchorRef={settingsButtonRef}
              onClose={() => setShowSettingsMenu(false)}
              onExport={onExport}
              onLogout={onLogout}
              onDelete={onDelete}
              isPendingLogout={isPendingLogout}
            />
          )}
        </div>
      </div>
    </div>
  )
}
