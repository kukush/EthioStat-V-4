import React from 'react';
import { ArrowRight, ChevronRight, Clock, PlusCircle, RefreshCw, Smartphone, TrendingUp, Wallet } from 'lucide-react';
import { BalancePackageEntity, BankInfo, FilterPeriod, Language, TelecomAssets, TransactionEntity } from '../types';
import { formatCurrency, t } from '../constants/translations';
import { SummaryCard } from '../components/SummaryCard';
import { TelecomAssetCard } from '../components/TelecomAssetCard';
import { TransactionItem } from '../components/TransactionItem';

interface HomeScreenProps {
  language: Language;
  transactions: TransactionEntity[];
  packages: BalancePackageEntity[];
  telecomAssets: TelecomAssets;
  banks: BankInfo[];
  showAmounts: boolean;
  onToggleAmounts: () => void;
  selectedPeriod: FilterPeriod;
  onSelectPeriod: (p: FilterPeriod) => void;
  onNavigateTab: (tab: 'home' | 'telecom' | 'transactions' | 'settings') => void;
  onOpenUssd: (action?: 'balance' | 'recharge' | 'transfer' | 'gift') => void;
  onOpenAddTx: () => void;
  isSyncing?: boolean;
  onSync?: () => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({
  language,
  transactions,
  packages,
  telecomAssets,
  banks,
  showAmounts,
  onToggleAmounts,
  selectedPeriod,
  onSelectPeriod,
  onNavigateTab,
  onOpenUssd,
  onOpenAddTx,
  isSyncing = false,
  onSync,
}) => {
  // Compute financial totals based on selected period
  const now = Date.now();
  const filteredTxs = transactions.filter((tx) => {
    if (selectedPeriod === 'allTime') return true;
    if (selectedPeriod === 'today') {
      const startOfDay = new Date();
      startOfDay.setHours(0, 0, 0, 0);
      return tx.timestamp >= startOfDay.getTime();
    }
    if (selectedPeriod === 'weekly') {
      return tx.timestamp >= now - 7 * 24 * 60 * 60 * 1000;
    }
    if (selectedPeriod === 'monthly') {
      return tx.timestamp >= now - 30 * 24 * 60 * 60 * 1000;
    }
    if (selectedPeriod === 'yearly') {
      return tx.timestamp >= now - 365 * 24 * 60 * 60 * 1000;
    }
    return true;
  });

  const totalIncome = filteredTxs
    .filter((tx) => tx.type === 'INCOME')
    .reduce((acc, tx) => acc + tx.amount, 0);

  const totalExpense = filteredTxs
    .filter((tx) => tx.type === 'EXPENSE' || tx.type === 'TRANSFER')
    .reduce((acc, tx) => acc + tx.amount, 0);

  // Calculate Net balance across active bank accounts
  const netBalance =
    banks
      .filter((b) => b.enabled)
      .reduce((acc, b) => acc + (b.currentBalance || 0), 0) + telecomAssets.airtimeBalance;

  // Aggregate Telecom quotas
  const dataVolGb = packages
    .filter((p) => p.type === 'internet' && p.isActive)
    .reduce((acc, p) => acc + (p.unit === 'GB' ? p.remainingAmount : p.remainingAmount / 1024), 0);

  const voiceMinutes = packages
    .filter((p) => p.type === 'voice' && p.isActive)
    .reduce((acc, p) => acc + p.remainingAmount, 0);

  const smsCount = packages
    .filter((p) => p.type === 'sms' && p.isActive)
    .reduce((acc, p) => acc + p.remainingAmount, 0);

  const recentTransactions = transactions.slice(0, 5);
  const activeBanks = banks.filter((b) => b.enabled);

  return (
    <div className="space-y-4 animate-fadeIn pb-16">
      {/* Financial Summary Card */}
      <SummaryCard
        language={language}
        netBalance={netBalance}
        totalIncome={totalIncome}
        totalExpense={totalExpense}
        showAmounts={showAmounts}
        onToggleAmounts={onToggleAmounts}
        selectedPeriod={selectedPeriod}
        onSelectPeriod={onSelectPeriod}
        isSyncing={isSyncing}
        onSync={onSync}
      />

      {/* Telecom Assets Overview */}
      <TelecomAssetCard
        language={language}
        dataVolGb={dataVolGb}
        voiceMinutes={voiceMinutes}
        smsCount={smsCount}
        airtimeBalance={telecomAssets.airtimeBalance}
        isCompact={true}
        onOpenUssd={onOpenUssd}
      />

      {/* Source Summaries: Bank & Mobile Money Balances */}
      <div className="rounded-3xl bg-slate-900 border border-slate-800 p-4 shadow-xl">
        <div className="flex items-center justify-between gap-2 mb-3">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-purple-500/10 border border-purple-500/20 text-purple-400">
              <Wallet className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-xs font-bold uppercase tracking-widest text-slate-400">
                {t(language, 'sourceSummaries')}
              </h3>
              <span className="text-[10px] text-slate-500 font-semibold">Active Ethiopian Accounts</span>
            </div>
          </div>
          <button
            onClick={() => onNavigateTab('settings')}
            className="text-xs font-bold text-emerald-400 hover:text-emerald-300 flex items-center gap-0.5"
          >
            <span>{t(language, 'manage')}</span>
            <ChevronRight className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="grid grid-cols-2 gap-2">
          {activeBanks.map((bank) => (
            <div
              key={bank.id}
              className="p-3 rounded-2xl bg-slate-950/70 border border-slate-800 flex items-center justify-between"
            >
              <div className="flex items-center gap-2 min-w-0">
                <div
                  className={`w-7 h-7 rounded-xl bg-gradient-to-tr ${bank.color} flex items-center justify-center text-white font-extrabold text-[10px] shrink-0 shadow-sm`}
                >
                  {bank.abbreviation.slice(0, 3)}
                </div>
                <div className="min-w-0">
                  <span className="font-bold text-[11px] text-white block truncate">{bank.displayName}</span>
                  <span className="text-[9px] text-slate-400 font-mono">{bank.ussd}</span>
                </div>
              </div>
              <div className="text-right shrink-0">
                <span className="font-black text-xs text-slate-200 block">
                  {showAmounts ? `${formatCurrency(bank.currentBalance)}` : '••••'}
                </span>
                <span className="text-[9px] font-bold text-slate-500">ETB</span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Recent Activity Section */}
      <div className="rounded-3xl bg-slate-900 border border-slate-800 p-4 shadow-xl">
        <div className="flex items-center justify-between gap-2 mb-3">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-cyan-500/10 border border-cyan-500/20 text-cyan-400">
              <Clock className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-xs font-bold uppercase tracking-widest text-slate-400">
                {t(language, 'recentActivity')}
              </h3>
              <span className="text-[10px] text-slate-500 font-semibold">Latest reconciled transactions</span>
            </div>
          </div>

          <div className="flex items-center gap-1.5">
            <button
              id="home-add-tx-btn"
              onClick={onOpenAddTx}
              className="flex items-center gap-1 px-2.5 py-1 rounded-xl bg-emerald-600/20 hover:bg-emerald-600/30 border border-emerald-500/30 text-emerald-300 font-bold text-xs transition-colors"
            >
              <PlusCircle className="w-3 h-3" />
              <span>Record</span>
            </button>
            <button
              onClick={() => onNavigateTab('transactions')}
              className="text-xs font-bold text-emerald-400 hover:text-emerald-300 flex items-center gap-0.5"
            >
              <span>{t(language, 'viewAll')}</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {recentTransactions.length > 0 ? (
          <div className="space-y-2">
            {recentTransactions.map((tx) => (
              <TransactionItem key={tx.id} transaction={tx} language={language} />
            ))}
          </div>
        ) : (
          <div className="py-6 text-center text-slate-500 text-xs">
            No transactions found for this period.
          </div>
        )}
      </div>
    </div>
  );
};
