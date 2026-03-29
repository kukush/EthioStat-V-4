import React, { useState } from 'react';
import { Transaction, Language, SourceSummary } from '@/types';
import { TransactionItem } from '@/presentation/components/TransactionItem';
import { motion, AnimatePresence } from 'framer-motion';
import { Search, Filter, TrendingUp, TrendingDown, Calendar, ArrowRight, History, Wallet, Download } from 'lucide-react';
import { cn, exportToCSV } from '@/lib/utils';
import { useTranslation } from '@/translations';
import { getBankIcon } from '@/constants/bankIcons';

interface TransactionScreenProps {
  transactions: Transaction[];
  language: Language;
  sources: string[];
}

type TimeFilter = 'all' | 'day' | 'week' | 'month';

export const TransactionScreen: React.FC<TransactionScreenProps> = ({ transactions, language, sources }) => {
  // Use local state instead of nuqs for filters
  const [activeSource, setActiveSource] = useState('all');
  const [timeFilter, setTimeFilter] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [showAmounts, setShowAmounts] = useState(true);
  const t = useTranslation(language);

  const filteredTransactions = transactions.filter(tr => {
    const matchesSource = activeSource === 'all' || tr.source.toLowerCase() === activeSource.toLowerCase();
    const matchesSearch = (tr.description?.toLowerCase() || '').includes(searchQuery.toLowerCase()) ||
      (tr.source?.toLowerCase() || '').includes(searchQuery.toLowerCase());

    // Time filtering logic
    let matchesTime = true;
    if (timeFilter !== 'all') {
      const txDate = new Date(tr.timestamp);
      const now = new Date();
      if (timeFilter === 'day') {
        matchesTime = txDate.toDateString() === now.toDateString();
      } else if (timeFilter === 'week') {
        const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
        matchesTime = txDate >= weekAgo;
      } else if (timeFilter === 'month') {
        matchesTime = txDate.getMonth() === now.getMonth() && txDate.getFullYear() === now.getFullYear();
      }
    }

    return matchesSource && matchesSearch && matchesTime;
  });

  const handleExport = () => {
    const exportData = filteredTransactions.map(t => ({
      Date: t.timestamp,
      Source: t.source,
      Description: t.description,
      Type: t.type,
      Amount: t.amount,
      Category: t.category
    }));
    exportToCSV(exportData, `transactions_${activeSource}_${new Date().toISOString().split('T')[0]}.csv`);
  };

  // Calculate Source Summary
  const sourceSummary: SourceSummary = {
    source: activeSource === 'all' ? 'Overall' : activeSource,
    income: filteredTransactions.filter(t => t.type === 'income').reduce((acc, t) => acc + t.amount, 0),
    expense: filteredTransactions.filter(t => t.type === 'expense').reduce((acc, t) => acc + t.amount, 0),
    netBalance: 0,
    transactionCount: filteredTransactions.length,
    lastTransaction: filteredTransactions[0]?.timestamp || 'N/A'
  };
  sourceSummary.netBalance = sourceSummary.income - sourceSummary.expense;

  const activeSourceLabel = activeSource === 'all'
    ? t('overallSummary')
    : (() => {
      const key = activeSource.toLowerCase().replace(/\s+/g, '_');
      // @ts-ignore
      const translated = t(key);
      return `${translated !== key ? translated : activeSource} ${t('summary')}`;
    })();

  return (
    <div className="space-y-8 pb-32">
      <header className="space-y-6">
        <div className="flex justify-between items-center">
          <h1 className="text-3xl font-black tracking-tight text-slate-900">{t('transactions')}</h1>
          <button className="p-2 bg-white rounded-full border border-slate-100 shadow-sm text-slate-400 hover:text-blue-600 transition-colors">
            <Filter size={20} />
          </button>
        </div>

        {/* Search Bar */}
        <div className="relative">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
          <input
            type="text"
            placeholder="Search transactions..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-12 pr-4 py-3 bg-white border border-slate-100 rounded-2xl text-sm font-medium focus:outline-none focus:ring-2 focus:ring-blue-500/20 transition-all shadow-sm"
          />
        </div>

        {/* Time Period Filter */}
        <div className="flex gap-2 px-1 overflow-x-auto no-scrollbar">
          {(['all', 'day', 'week', 'month'] as TimeFilter[]).map((f) => (
            <button
              key={f}
              onClick={() => setTimeFilter(f)}
              className={cn(
                "px-4 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all border whitespace-nowrap",
                timeFilter === f
                  ? "bg-slate-900 text-white border-slate-900 shadow-lg"
                  : "bg-white text-slate-400 border-slate-100 hover:border-slate-200"
              )}
            >
              {f === 'all' ? t('allTime') : f === 'day' ? t('today') : f === 'week' ? t('thisWeek') : t('thisMonth')}
            </button>
          ))}
        </div>

        {/* Source Filter Chips */}
        <div className="flex gap-4 overflow-x-auto no-scrollbar pb-2 -mx-2 px-2">
          {/* All Sources */}
          <button
            onClick={() => setActiveSource('all')}
            className="flex-shrink-0 flex flex-col items-center gap-2 group"
          >
            <div className={cn(
              "w-16 h-16 rounded-full flex items-center justify-center transition-all",
              activeSource === 'all' ? "bg-slate-900 ring-4 ring-slate-100" : "bg-slate-100"
            )}>
              <span className={cn(
                "text-xs font-black uppercase",
                activeSource === 'all' ? "text-white" : "text-slate-400"
              )}>{t('allTime').split(' ')[0]}</span>
            </div>
            <span className={cn(
              "text-[10px] font-bold uppercase tracking-widest",
              activeSource === 'all' ? "text-slate-900" : "text-slate-400"
            )}>{t('allTime').split(' ')[0]}</span>
          </button>
          {/* Dynamic Sources */}
          {sources.map((source) => {
            const key = source.toLowerCase().replace(/\s+/g, '_');
            // @ts-ignore
            const translated = t(key);
            const label = translated !== key ? translated : source;

            return (
              <button
                key={source}
                onClick={() => setActiveSource(source)}
                className="flex-shrink-0 flex flex-col items-center gap-2 group"
              >
                <div className={cn(
                  "w-16 h-16 rounded-full flex items-center justify-center transition-all overflow-hidden bg-white border border-slate-100",
                  activeSource === source ? "ring-4 ring-slate-200" : "hover:ring-4 hover:ring-slate-100",
                )}>
                  {getBankIcon(source, 40)}
                </div>
                <span className={cn(
                  "text-[10px] font-bold uppercase tracking-widest",
                  activeSource === source ? "text-slate-900" : "text-slate-400"
                )}>{label}</span>
              </button>
            );
          })}
        </div>
      </header>

      {/* Source Summary Card */}
      <AnimatePresence mode="wait">
        <motion.div
          key={activeSource}
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -10 }}
          className="bg-white p-6 rounded-[2.5rem] border border-slate-100 shadow-sm space-y-6"
        >
          <div className="flex justify-between items-center">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-full bg-blue-50 flex items-center justify-center text-blue-600">
                <History size={16} />
              </div>
              <h3 className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">
                {activeSourceLabel}
              </h3>
            </div>
            <div className="flex items-center gap-3">
              <button
                onClick={() => setShowAmounts(!showAmounts)}
                className="text-slate-400 hover:text-slate-600 transition-colors"
                title={showAmounts ? t('hideAmount') : t('showAmount')}
              >
                {showAmounts ? (
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" /><circle cx="12" cy="12" r="3" /></svg>
                ) : (
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M9.88 9.88 2 2m12.12 12.12L22 22M10.33 4.56C10.89 4.52 11.44 4.5 12 4.5c7 0 10 7 10 7a13.12 13.12 0 0 1-1.66 2.04m-3.17 1.16C15.5 15.82 13.86 16.5 12 16.5c-7 0-10-7-10-7a13.16 13.16 0 0 1 1.66-2.04m3.17-1.16C8.5 8.18 10.14 7.5 12 7.5c1.86 0 3.5.68 4.83 1.81" /><path d="M1 1l22 22" /></svg>
                )}
              </button>
              <div className="flex items-center gap-1 text-slate-400">
                <Calendar size={12} />
                <span className="text-[10px] font-bold uppercase tracking-widest">
                  {timeFilter === 'all' ? t('allTime') : timeFilter === 'day' ? t('today') : timeFilter === 'week' ? t('thisWeek') : t('thisMonth')}
                </span>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1">
              <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">{t('netBalance')}</p>
              <h2 className={cn(
                "text-2xl font-black",
                sourceSummary.netBalance >= 0 ? "text-emerald-600" : "text-rose-600"
              )}>
                {showAmounts ? sourceSummary.netBalance.toFixed(2) : '••••••'}
                <span className="text-xs font-bold ml-1">ETB</span>
              </h2>
            </div>
            <div className="space-y-1 text-right">
              <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">{t('transactions')}</p>
              <h2 className="text-2xl font-black text-slate-900">{sourceSummary.transactionCount}</h2>
            </div>
          </div>

          <div className="flex gap-4 pt-4 border-t border-slate-50">
            <div className="flex-1 flex items-center gap-3">
              <div className="w-10 h-10 rounded-2xl bg-emerald-50 flex items-center justify-center text-emerald-600">
                <TrendingUp size={20} />
              </div>
              <div>
                <p className="text-[8px] font-bold text-slate-400 uppercase tracking-widest">{t('income')}</p>
                <p className="text-sm font-black text-emerald-600">
                  {showAmounts ? `+${sourceSummary.income.toFixed(0)}` : '••••'}
                </p>
              </div>
            </div>
            <div className="flex-1 flex items-center gap-3">
              <div className="w-10 h-10 rounded-2xl bg-rose-50 flex items-center justify-center text-rose-600">
                <TrendingDown size={20} />
              </div>
              <div>
                <p className="text-[8px] font-bold text-slate-400 uppercase tracking-widest">{t('expense')}</p>
                <p className="text-sm font-black text-rose-600">
                  {showAmounts ? `-${sourceSummary.expense.toFixed(0)}` : '••••'}
                </p>
              </div>
            </div>
          </div>

          <div className="pt-2">
            <p className="text-[8px] font-bold text-slate-300 uppercase tracking-widest text-center italic">
              Last activity: {sourceSummary.lastTransaction}
            </p>
          </div>
        </motion.div>
      </AnimatePresence>

      <section className="space-y-4">
        <div className="flex justify-between items-end px-2">
          <h3 className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">{t('transactionHistory')}</h3>
          <button
            onClick={handleExport}
            className="flex items-center gap-1 text-blue-600 cursor-pointer hover:underline"
          >
            <span className="text-[10px] font-bold uppercase tracking-widest">{t('exportCsv')}</span>
            <Download size={12} />
          </button>
        </div>

        <div className="space-y-3">
          <AnimatePresence mode="popLayout">
            {filteredTransactions.length > 0 ? (
              filteredTransactions.map((t, index) => (
                <motion.div
                  key={t.id}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: index * 0.05 }}
                >
                  <TransactionItem transaction={t} language={language} />
                </motion.div>
              ))
            ) : (
              <div className="bg-white p-12 rounded-[2.5rem] border border-dashed border-slate-200 flex flex-col items-center justify-center text-center space-y-4">
                <div className="w-16 h-16 rounded-full bg-slate-50 flex items-center justify-center text-slate-300">
                  <Wallet size={32} />
                </div>
                <div>
                  <p className="text-sm font-bold text-slate-900">No transactions found</p>
                  <p className="text-xs text-slate-400 mt-1">Try adjusting your filters or search query</p>
                </div>
              </div>
            )}
          </AnimatePresence>
        </div>
      </section>
    </div>
  );
};
