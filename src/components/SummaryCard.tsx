import React from 'react';
import { ArrowDownLeft, ArrowUpRight, Eye, EyeOff, RefreshCw, TrendingUp, Wallet } from 'lucide-react';
import { FilterPeriod, Language } from '../types';
import { formatCurrency, t } from '../constants/translations';

interface SummaryCardProps {
  language: Language;
  netBalance: number;
  totalIncome: number;
  totalExpense: number;
  showAmounts: boolean;
  onToggleAmounts: () => void;
  selectedPeriod: FilterPeriod;
  onSelectPeriod: (p: FilterPeriod) => void;
  isSyncing?: boolean;
  onSync?: () => void;
}

export const SummaryCard: React.FC<SummaryCardProps> = ({
  language,
  netBalance,
  totalIncome,
  totalExpense,
  showAmounts,
  onToggleAmounts,
  selectedPeriod,
  onSelectPeriod,
  isSyncing = false,
  onSync,
}) => {
  return (
    <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 p-5 border border-slate-700/80 shadow-2xl shadow-emerald-950/20">
      {/* Decorative backdrop glow */}
      <div className="absolute top-0 right-0 -mr-16 -mt-16 w-48 h-48 rounded-full bg-emerald-500/10 blur-3xl pointer-events-none" />
      <div className="absolute bottom-0 left-0 -ml-16 -mb-16 w-48 h-48 rounded-full bg-cyan-500/10 blur-3xl pointer-events-none" />

      {/* Top Header with Net Balance label, Sync button, and Eye toggle */}
      <div className="flex items-center justify-between gap-2 mb-4">
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 shrink-0">
            <Wallet className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-bold uppercase tracking-widest text-slate-400">
                {t(language, 'netBalance')}
              </span>
              {onSync && (
                <button
                  id="summary-sync-btn"
                  onClick={onSync}
                  disabled={isSyncing}
                  className="flex items-center gap-1 px-2 py-0.5 rounded-lg bg-emerald-600/20 hover:bg-emerald-600/30 border border-emerald-500/40 text-emerald-400 font-bold text-[10px] transition-all active:scale-95 disabled:opacity-50"
                  title="Sync Balance via USSD *804#"
                >
                  <RefreshCw className={`w-3 h-3 ${isSyncing ? 'animate-spin' : ''}`} />
                  <span>{t(language, 'sync')}</span>
                </button>
              )}
            </div>
            <div className="text-[10px] text-slate-500 font-semibold tracking-wide">
              {t(language, 'overallSummary')}
            </div>
          </div>
        </div>

        <button
          id="toggle-amount-visibility-btn"
          onClick={onToggleAmounts}
          className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors border border-slate-700 shrink-0"
          title={showAmounts ? t(language, 'hideAmount') : t(language, 'showAmount')}
        >
          {showAmounts ? <Eye className="w-4 h-4" /> : <EyeOff className="w-4 h-4" />}
        </button>
      </div>

      {/* Period selector pills */}
      <div className="mb-4">
        <div className="flex items-center bg-slate-950/60 p-1 rounded-xl border border-slate-800 text-[11px] font-bold overflow-x-auto no-scrollbar gap-1">
          {[
            { id: 'allTime', label: t(language, 'allTime') },
            { id: 'today', label: t(language, 'today') },
            { id: 'weekly', label: t(language, 'thisWeek') },
            { id: 'monthly', label: t(language, 'thisMonth') },
            { id: 'yearly', label: t(language, 'thisYear') },
          ].map((item) => (
            <button
              key={item.id}
              onClick={() => onSelectPeriod(item.id as FilterPeriod)}
              className={`px-2.5 py-1 rounded-lg transition-colors whitespace-nowrap ${
                selectedPeriod === item.id ? 'bg-slate-700 text-white' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>

      {/* Main Net Balance Display */}
      <div className="mb-5">
        <div className="flex items-baseline gap-2">
          <span className="text-3xl font-extrabold tracking-tight text-white">
            {showAmounts ? `${formatCurrency(netBalance)}` : '••••••••'}
          </span>
          <span className="text-sm font-bold text-emerald-400">ETB</span>
        </div>
        <div className="flex items-center gap-1.5 mt-1 text-xs text-slate-400 font-medium">
          <TrendingUp className="w-3.5 h-3.5 text-emerald-400" />
          <span>{t(language, 'netCashFlow')}</span>
          <span className={`font-bold ${netBalance >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
            {netBalance >= 0 ? '+ Positive' : '- Deficit'}
          </span>
        </div>
      </div>

      {/* Income & Expense Breakdown Grid */}
      <div className="grid grid-cols-2 gap-2.5 pt-3.5 border-t border-slate-700/60">
        <div className="p-3 rounded-2xl bg-emerald-950/30 border border-emerald-500/20">
          <div className="flex items-center gap-1.5 text-xs font-semibold text-emerald-400 mb-1">
            <div className="p-1 rounded-md bg-emerald-500/20">
              <ArrowDownLeft className="w-3.5 h-3.5" />
            </div>
            <span>{t(language, 'income')}</span>
          </div>
          <div className="text-base font-black text-white">
            {showAmounts ? `+${formatCurrency(totalIncome)}` : '••••••'}
          </div>
          <span className="text-[9px] text-emerald-300/70 font-semibold uppercase tracking-wider">ETB Received</span>
        </div>

        <div className="p-3 rounded-2xl bg-rose-950/30 border border-rose-500/20">
          <div className="flex items-center gap-1.5 text-xs font-semibold text-rose-400 mb-1">
            <div className="p-1 rounded-md bg-rose-500/20">
              <ArrowUpRight className="w-3.5 h-3.5" />
            </div>
            <span>{t(language, 'expense')}</span>
          </div>
          <div className="text-base font-black text-white">
            {showAmounts ? `-${formatCurrency(totalExpense)}` : '••••••'}
          </div>
          <span className="text-[9px] text-rose-300/70 font-semibold uppercase tracking-wider">ETB Spent</span>
        </div>
      </div>
    </div>
  );
};
