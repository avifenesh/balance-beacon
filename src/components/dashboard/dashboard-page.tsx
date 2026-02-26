'use client'

import { Suspense, useMemo } from 'react'
import dynamic from 'next/dynamic'
import { Currency } from '@prisma/client'
import { ArrowUp } from 'lucide-react'

import { BalanceForm } from '@/components/dashboard/balance-form'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { DashboardData } from '@/lib/finance'
import { formatMonthLabel } from '@/utils/date'
import { useCsrfTokenWithState } from '@/hooks/useCsrfToken'
import { ChatWidget } from '@/components/ai/chat-widget'
import {
  BudgetsTab,
  CategoriesTab,
  OverviewTab,
  RecurringTab,
  TransactionsTab,
  SharingTab,
} from '@/components/dashboard/tabs'
import { SubscriptionBanner, type SubscriptionBannerData } from '@/components/subscription'
import { DeleteAccountDialog } from '@/components/settings/delete-account-dialog'
import { ExportDataDialog } from '@/components/settings/export-data-dialog'

import { useDashboardNavigation } from '@/components/dashboard/hooks/use-dashboard-navigation'
import { useDashboardState } from '@/components/dashboard/hooks/use-dashboard-state'
import { useDashboardActions } from '@/components/dashboard/hooks/use-dashboard-actions'
import { DashboardNav } from '@/components/dashboard/dashboard-nav'
import { DashboardHeader } from '@/components/dashboard/dashboard-header'

type DashboardPageProps = {
  data: DashboardData
  monthKey: string
  accountId: string
  subscription: SubscriptionBannerData | null
  userEmail: string
}

const HoldingsTab = dynamic(() => import('./holdings-tab'), {
  ssr: false,
  loading: () => <HoldingsFallback />,
})

function HoldingsFallback() {
  return (
    <div className="grid gap-6 lg:grid-cols-[400px_1fr]">
      <Card className="border-white/15 bg-white/10 h-fit">
        <CardHeader className="gap-1">
          <CardTitle className="text-lg font-semibold text-white">Loading holdings…</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="h-32 animate-pulse rounded-xl bg-white/5" />
        </CardContent>
      </Card>
      <Card className="border-white/15 bg-white/10">
        <CardHeader>
          <CardTitle className="text-lg font-semibold text-white">Preparing data…</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="h-48 animate-pulse rounded-2xl bg-white/5" />
        </CardContent>
      </Card>
    </div>
  )
}

export function DashboardPage({ data, monthKey, accountId, subscription, userEmail }: DashboardPageProps) {
  const { token: csrfToken, isLoading: isCsrfLoading } = useCsrfTokenWithState()

  // Get user's preferred currency for formatting
  const preferredCurrency = data.preferredCurrency || Currency.USD
  const initialAccountId = accountId ?? data.accounts[0]?.id ?? ''

  // Hooks
  const {
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
  } = useDashboardState(initialAccountId)

  const { handleParamUpdate, handleMonthChange } = useDashboardNavigation(monthKey)

  const accountsOptions = useMemo(
    () => data.accounts.map((account) => ({ label: account.name, value: account.id })),
    [data.accounts],
  )

  const { handleAccountSelect, handleLogout, handleRefreshRates, isPendingLogout, isPendingRates } =
    useDashboardActions(csrfToken, handleParamUpdate, setActiveAccount, accountsOptions)

  const historyWithLabels = useMemo(
    () =>
      data.history.map((point) => ({
        ...point,
        label: formatMonthLabel(point.month),
      })),
    [data.history],
  )

  const { netHistory, latestHistory, netDelta, netStat } = useMemo(() => {
    const history = historyWithLabels.map((point) => point.net)
    const latest = historyWithLabels.at(-1)
    const previous = historyWithLabels.at(-2)
    const delta = latest && previous ? latest.net - previous.net : 0
    const stat = data.stats.find((s) => s.label.toLowerCase().includes('net'))
    return { netHistory: history, latestHistory: latest, netDelta: delta, netStat: stat }
  }, [historyWithLabels, data.stats])

  const isRefreshRatesDisabled = isPendingRates || isCsrfLoading || !csrfToken
  const refreshRatesLabel = isCsrfLoading
    ? 'Loading security token…'
    : !csrfToken
      ? 'Security token unavailable'
      : 'Refresh exchange rates'

  return (
    <div className="mx-auto flex w-full max-w-7xl flex-col gap-4 px-4 pt-14 lg:gap-6 lg:px-6 lg:pt-16">
      {/* Subscription banner */}
      {subscription && (
        <SubscriptionBanner
          subscription={subscription}
          onUpgrade={() => {
            window.open('/upgrade', '_blank')
          }}
        />
      )}

      <DashboardNav
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        onToggleBalanceForm={() => setShowBalanceForm((prev) => !prev)}
        onExport={() => setShowExportDialog(true)}
        onLogout={handleLogout}
        onDelete={() => setShowDeleteDialog(true)}
        isPendingLogout={isPendingLogout}
      />

      <DashboardHeader
        monthKey={monthKey}
        onMonthChange={handleMonthChange}
        netStat={netStat}
        netHistory={netHistory}
        latestHistory={latestHistory}
        netDelta={netDelta}
        preferredCurrency={preferredCurrency}
        stats={data.stats}
        expandedStat={expandedStat}
        setExpandedStat={setExpandedStat}
        exchangeRateLastUpdate={data.exchangeRateLastUpdate}
        onRefreshRates={handleRefreshRates}
        isRefreshRatesDisabled={isRefreshRatesDisabled}
        isPendingRates={isPendingRates}
        refreshRatesLabel={refreshRatesLabel}
      />

      {/* Balance form - shown when Balance button clicked */}
      {showBalanceForm && (
        <BalanceForm
          activeAccount={activeAccount}
          monthKey={monthKey}
          preferredCurrency={preferredCurrency}
          currentNet={data.stats.find((s) => s.label === 'Net this month')?.amount ?? 0}
          onClose={() => setShowBalanceForm(false)}
        />
      )}

      <section className="space-y-6">
        {activeTab === 'overview' && (
          <OverviewTab
            history={data.history}
            comparison={data.comparison}
            budgets={data.budgets}
            transactionRequests={data.transactionRequests}
            activeAccount={activeAccount}
            preferredCurrency={preferredCurrency}
            onNavigateToBudgets={() => setActiveTab('budgets')}
          />
        )}

        {activeTab === 'budgets' && (
          <BudgetsTab
            budgets={data.budgets}
            accounts={data.accounts}
            categories={data.categories}
            activeAccount={activeAccount}
            monthKey={monthKey}
            preferredCurrency={preferredCurrency}
            monthlyIncomeGoal={data.monthlyIncomeGoal}
            actualIncome={data.actualIncome}
          />
        )}

        {activeTab === 'transactions' && (
          <TransactionsTab
            transactions={data.transactions}
            transactionRequests={data.transactionRequests}
            accounts={data.accounts}
            categories={data.categories}
            activeAccount={activeAccount}
            monthKey={monthKey}
            preferredCurrency={preferredCurrency}
          />
        )}
        {activeTab === 'recurring' && (
          <RecurringTab
            recurringTemplates={data.recurringTemplates}
            accounts={data.accounts}
            categories={data.categories}
            activeAccount={activeAccount}
            monthKey={monthKey}
            preferredCurrency={preferredCurrency}
          />
        )}

        {activeTab === 'categories' && <CategoriesTab categories={data.allCategories} />}

        {/* Holdings Tab */}
        {activeTab === 'holdings' && (
          <Suspense fallback={<HoldingsFallback />}>
            <HoldingsTab
              activeAccount={activeAccount}
              accountsOptions={accountsOptions}
              categories={data.categories}
              preferredCurrency={preferredCurrency}
              onSelectAccount={handleAccountSelect}
            />
          </Suspense>
        )}

        {/* Sharing Tab */}
        {activeTab === 'sharing' && (
          <SharingTab
            sharedExpenses={data.sharedExpenses || []}
            expensesSharedWithMe={data.expensesSharedWithMe || []}
            settlementBalances={data.settlementBalances || []}
            paymentHistory={data.paymentHistory || []}
          />
        )}
      </section>

      {/* AI Chat Widget */}
      {process.env.NEXT_PUBLIC_AI_ENABLED !== 'false' && (
        <ChatWidget accountId={accountId} monthKey={monthKey} preferredCurrency={preferredCurrency} />
      )}

      {/* Floating scroll-to-top button */}
      <Button
        type="button"
        variant="secondary"
        className="fixed bottom-6 left-6 h-10 w-10 rounded-full shadow-lg"
        onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
        title="Scroll to top"
      >
        <ArrowUp className="h-5 w-5" />
      </Button>

      {/* Export Data Dialog */}
      {showExportDialog && <ExportDataDialog onClose={() => setShowExportDialog(false)} />}

      {/* Delete Account Dialog */}
      {showDeleteDialog && <DeleteAccountDialog userEmail={userEmail} onClose={() => setShowDeleteDialog(false)} />}
    </div>
  )
}
