'use client'

import {
  ArrowLeft,
  ArrowRight,
  CalendarRange,
  ChevronDown,
  Layers,
  PiggyBank,
  RefreshCcw,
  Sparkles,
  TrendingUp,
  Wallet,
} from 'lucide-react'
import { Currency } from '@prisma/client'
import { Button } from '@/components/ui/button'
import { Sparkline } from '@/components/dashboard/sparkline'
import { StatBreakdownPanel } from '@/components/dashboard/stat-breakdown'
import { formatCurrency, formatRelativeAmount } from '@/utils/format'
import { formatMonthLabel } from '@/utils/date'
import { cn } from '@/utils/cn'
import { DashboardData } from '@/lib/finance'

const STAT_VARIANT_STYLES: Record<
  NonNullable<DashboardData['stats'][number]['variant']>,
  {
    border: string
    chip: string
    chipText: string
    icon: string
  }
> = {
  positive: {
    border: 'border-emerald-400/40',
    chip: 'bg-emerald-400/20',
    chipText: 'text-emerald-200',
    icon: 'text-emerald-200',
  },
  negative: {
    border: 'border-rose-400/40',
    chip: 'bg-rose-400/20',
    chipText: 'text-rose-200',
    icon: 'text-rose-200',
  },
  neutral: {
    border: 'border-white/15',
    chip: 'bg-white/15',
    chipText: 'text-slate-200',
    icon: 'text-slate-200',
  },
}

function resolveStatIcon(label: string) {
  const normalized = label.toLowerCase()
  if (['net', 'saved', 'income', 'inflow'].some((kw) => normalized.includes(kw))) {
    return Wallet
  }
  if (['spend', 'expense', 'outflow'].some((kw) => normalized.includes(kw))) {
    return PiggyBank
  }
  if (['target', 'goal', 'budget', 'track'].some((kw) => normalized.includes(kw))) {
    return Layers
  }
  return TrendingUp
}

type DashboardHeaderProps = {
  monthKey: string
  onMonthChange: (direction: number) => void
  netStat: DashboardData['stats'][number] | undefined
  netHistory: number[]
  latestHistory: { income: number; expense: number } | undefined
  netDelta: number
  preferredCurrency: Currency
  stats: DashboardData['stats']
  expandedStat: string | null
  setExpandedStat: (label: string | null) => void
  exchangeRateLastUpdate: Date | null
  onRefreshRates: () => void
  isRefreshRatesDisabled: boolean
  isPendingRates: boolean
  refreshRatesLabel: string
}

export function DashboardHeader({
  monthKey,
  onMonthChange,
  netStat,
  netHistory,
  latestHistory,
  netDelta,
  preferredCurrency,
  stats,
  expandedStat,
  setExpandedStat,
  exchangeRateLastUpdate,
  onRefreshRates,
  isRefreshRatesDisabled,
  isPendingRates,
  refreshRatesLabel,
}: DashboardHeaderProps) {
  const netDeltaVariant = netDelta >= 0 ? 'text-emerald-300' : 'text-rose-300'

  return (
    <header className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 p-5 text-white shadow-xl lg:p-6">
      <div
        className="absolute inset-0 bg-[radial-gradient(circle_at_top,_rgba(148,163,184,0.18),_transparent_55%)]"
        aria-hidden
      />
      <div className="relative z-10 flex flex-col gap-8 lg:flex-row lg:items-center lg:justify-between">
        <div className="max-w-xl space-y-5">
          <div className="flex flex-wrap items-center gap-2">
            {/* Month selector */}
            <div className="inline-flex items-center gap-0.5 rounded-full border border-white/15 bg-white/10 px-1.5 py-0.5 backdrop-blur">
              <Button
                type="button"
                variant="ghost"
                className="h-6 w-6 rounded-full text-white/90 transition hover:bg-white/20"
                onClick={() => onMonthChange(-1)}
                aria-label="Previous month"
              >
                <ArrowLeft className="h-3 w-3" />
              </Button>
              <div
                className="flex items-center gap-1 px-1.5 text-xs font-medium text-white"
                data-testid="month-label"
              >
                <CalendarRange className="h-3 w-3" />
                {formatMonthLabel(monthKey)}
              </div>
              <Button
                type="button"
                variant="ghost"
                className="h-6 w-6 rounded-full text-white/90 transition hover:bg-white/20"
                onClick={() => onMonthChange(1)}
                aria-label="Next month"
              >
                <ArrowRight className="h-3 w-3" />
              </Button>
            </div>
            <span className="inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1 text-xs uppercase tracking-wide text-slate-200 backdrop-blur">
              <Sparkles className="h-3.5 w-3.5" />
              Financial clarity
            </span>
          </div>
          <div className="space-y-2">
            <h1 className="text-4xl font-semibold tracking-tight text-white md:text-5xl">Balance Beacon</h1>
            <p className="text-sm leading-relaxed text-slate-200/80">
              Track personal spending and shared plans with insights that highlight what changed and where to focus
              your next dollar.
            </p>
          </div>
          {netStat && (
            <span className="inline-flex items-center gap-2 rounded-full bg-emerald-500/10 px-3 py-1 text-xs font-medium text-emerald-200">
              <TrendingUp className="h-3.5 w-3.5" />
              {formatRelativeAmount(netStat.amount)} net flow
            </span>
          )}
        </div>

        <div className="flex w-full max-w-md flex-col gap-6 rounded-2xl bg-white/10 p-5 backdrop-blur lg:max-w-sm">
          <div className="flex items-center justify-between text-xs uppercase tracking-wide text-slate-200/80">
            <span>Cashflow snapshot</span>
            <RefreshCcw className="h-4 w-4 opacity-70" />
          </div>
          <div className="h-28 w-full">
            <Sparkline
              values={netHistory}
              strokeClassName="stroke-white"
              fillClassName="fill-white/15"
              ariaLabel="Net cashflow snapshot"
            />
          </div>
          <div className="grid grid-cols-2 gap-4 text-sm text-slate-100/80">
            <div>
              <p className="text-xs uppercase tracking-wide text-slate-200/70">Income this month</p>
              <p className="text-lg font-semibold text-white">
                {formatCurrency(latestHistory?.income ?? 0, preferredCurrency)}
              </p>
            </div>
            <div>
              <p className="text-xs uppercase tracking-wide text-slate-200/70">Spending this month</p>
              <p className="text-lg font-semibold text-white">
                {formatCurrency(latestHistory?.expense ?? 0, preferredCurrency)}
              </p>
            </div>
            <div>
              <p className="text-xs uppercase tracking-wide text-slate-200/70">Change vs last month</p>
              <p className={cn('text-lg font-semibold', netDeltaVariant)}>
                {formatRelativeAmount(netDelta, preferredCurrency)}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Compact stat cards at the bottom of header */}
      <div className="relative z-10 mt-5 grid grid-cols-1 gap-2 min-[375px]:grid-cols-2 md:grid-cols-4">
        {stats.map((stat) => {
          const variantKey = stat.variant ?? 'neutral'
          const styles = STAT_VARIANT_STYLES[variantKey]
          const Icon = resolveStatIcon(stat.label)
          const isExpanded = expandedStat === stat.label
          const hasBreakdown = !!stat.breakdown

          const cardContent = (
            <>
              <span className={cn('inline-flex shrink-0 items-center justify-center rounded-lg p-1.5', styles.chip)}>
                <Icon className={cn('h-3.5 w-3.5', styles.icon)} />
              </span>
              <div className="min-w-0 flex-1">
                <p className="truncate text-[10px] font-medium uppercase tracking-wide text-slate-300">
                  {stat.label}
                </p>
                <p className="truncate text-sm font-semibold text-white">
                  {formatCurrency(stat.amount, preferredCurrency)}
                </p>
              </div>
              {hasBreakdown && (
                <ChevronDown
                  className={cn(
                    'h-3.5 w-3.5 shrink-0 text-slate-400 transition-transform',
                    isExpanded && 'rotate-180',
                  )}
                />
              )}
            </>
          )

          const cardClassName = cn(
            'flex w-full items-center gap-3 rounded-xl border bg-white/5 px-3 py-2 backdrop-blur transition',
            styles.border,
            isExpanded && 'ring-1 ring-white/30',
          )

          // Use button for interactive cards, div for non-interactive
          return hasBreakdown ? (
            <button
              key={stat.label}
              type="button"
              onClick={() => setExpandedStat(isExpanded ? null : stat.label)}
              className={cn(cardClassName, 'cursor-pointer hover:bg-white/10 text-left')}
              aria-expanded={isExpanded}
              aria-label={`View ${stat.label} breakdown`}
              data-testid="stat-card"
            >
              {cardContent}
            </button>
          ) : (
            <div key={stat.label} className={cardClassName} data-testid="stat-card">
              {cardContent}
            </div>
          )
        })}
      </div>

      {/* Stat breakdown panel */}
      {expandedStat &&
        (() => {
          const stat = stats.find((s) => s.label === expandedStat)
          if (!stat?.breakdown) return null

          return (
            <div className="relative z-10 mt-2 rounded-xl border border-white/15 bg-white/5 p-4 backdrop-blur">
              <div className="flex items-center justify-between mb-3">
                <p className="text-xs font-medium uppercase tracking-wide text-slate-300">{expandedStat} breakdown</p>
                <button
                  type="button"
                  onClick={() => setExpandedStat(null)}
                  className="text-xs text-slate-400 hover:text-white transition"
                  aria-label="Close breakdown"
                >
                  Close
                </button>
              </div>
              <StatBreakdownPanel breakdown={stat.breakdown} currency={preferredCurrency} />
            </div>
          )
        })()}

      {/* Exchange rate refresh - compact */}
      {exchangeRateLastUpdate && (
        <div className="relative z-10 mt-3 flex items-center justify-end gap-2 text-xs text-slate-400">
          <span>
            Rates:{' '}
            {new Date(exchangeRateLastUpdate).toLocaleDateString('en-US', {
              month: 'short',
              day: 'numeric',
            })}
          </span>
          <Button
            type="button"
            variant="ghost"
            className="h-6 px-2 text-xs text-slate-300 hover:bg-white/10"
            onClick={onRefreshRates}
            disabled={isRefreshRatesDisabled}
            title={refreshRatesLabel}
            aria-label={refreshRatesLabel}
          >
            <RefreshCcw className={cn('h-3 w-3', isPendingRates && 'animate-spin')} />
          </Button>
        </div>
      )}
    </header>
  )
}
