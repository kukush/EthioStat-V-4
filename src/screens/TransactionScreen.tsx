import React, { useMemo, useState } from 'react';
import {
  BarChart3,
  Calendar,
  CalendarDays,
  Download,
  Filter,
  Layers,
  PlusCircle,
  RotateCcw,
  Search,
  SlidersHorizontal,
  X,
} from 'lucide-react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { BankInfo, FilterPeriod, Language, TransactionCategory, TransactionEntity, TransactionType } from '../types';
import { formatCurrency, formatDate, t } from '../constants/translations';
import { TransactionItem } from '../components/TransactionItem';

interface TransactionScreenProps {
  language: Language;
  transactions: TransactionEntity[];
  banks: BankInfo[];
  onOpenAddTx: () => void;
  onClearTransactions: () => void;
}

export const TransactionScreen: React.FC<TransactionScreenProps> = ({
  language,
  transactions,
  banks,
  onOpenAddTx,
  onClearTransactions,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedSource, setSelectedSource] = useState('ALL');
  const [selectedType, setSelectedType] = useState<'ALL' | TransactionType>('ALL');
  const [selectedCategory, setSelectedCategory] = useState<'ALL' | TransactionCategory>('ALL');
  const [selectedPeriod, setSelectedPeriod] = useState<FilterPeriod>('allTime');
  const [customStartDate, setCustomStartDate] = useState('');
  const [customEndDate, setCustomEndDate] = useState('');
  const [showChart, setShowChart] = useState(false);
  const [exportNotice, setExportNotice] = useState(false);

  // Date filtering logic
  const isWithinPeriod = (timestamp: number, period: FilterPeriod): boolean => {
    const now = Date.now();
    if (period === 'allTime') return true;

    if (period === 'today') {
      const startOfDay = new Date();
      startOfDay.setHours(0, 0, 0, 0);
      const endOfDay = new Date();
      endOfDay.setHours(23, 59, 59, 999);
      return timestamp >= startOfDay.getTime() && timestamp <= endOfDay.getTime();
    }

    if (period === 'weekly') {
      const sevenDaysAgo = now - 7 * 24 * 60 * 60 * 1000;
      return timestamp >= sevenDaysAgo;
    }

    if (period === 'monthly') {
      const thirtyDaysAgo = now - 30 * 24 * 60 * 60 * 1000;
      return timestamp >= thirtyDaysAgo;
    }

    if (period === 'yearly') {
      const oneYearAgo = now - 365 * 24 * 60 * 60 * 1000;
      return timestamp >= oneYearAgo;
    }

    if (period === 'custom') {
      let valid = true;
      if (customStartDate) {
        const start = new Date(`${customStartDate}T00:00:00`).getTime();
        if (!isNaN(start) && timestamp < start) valid = false;
      }
      if (customEndDate) {
        const end = new Date(`${customEndDate}T23:59:59.999`).getTime();
        if (!isNaN(end) && timestamp > end) valid = false;
      }
      return valid;
    }

    return true;
  };

  // Filter transactions
  const filteredTransactions = useMemo(() => {
    return transactions.filter((tx) => {
      // Date period filter
      if (!isWithinPeriod(tx.timestamp, selectedPeriod)) {
        return false;
      }

      // Source filter
      if (selectedSource !== 'ALL' && tx.source.toUpperCase() !== selectedSource.toUpperCase()) {
        return false;
      }
      // Type filter
      if (selectedType !== 'ALL' && tx.type !== selectedType) {
        return false;
      }
      // Category filter
      if (selectedCategory !== 'ALL' && tx.category !== selectedCategory) {
        return false;
      }
      // Search filter
      if (searchTerm.trim()) {
        const query = searchTerm.toLowerCase();
        const matchesParty = tx.recipientOrSender?.toLowerCase().includes(query);
        const matchesRef = tx.reference?.toLowerCase().includes(query);
        const matchesSource = tx.source.toLowerCase().includes(query);
        const matchesRaw = tx.rawSmsBody?.toLowerCase().includes(query);
        if (!matchesParty && !matchesRef && !matchesSource && !matchesRaw) {
          return false;
        }
      }
      return true;
    });
  }, [transactions, selectedPeriod, customStartDate, customEndDate, selectedSource, selectedType, selectedCategory, searchTerm]);

  // Aggregate metrics
  const totalIncome = filteredTransactions
    .filter((tx) => tx.type === 'INCOME')
    .reduce((acc, tx) => acc + tx.amount, 0);

  const totalExpense = filteredTransactions
    .filter((tx) => tx.type === 'EXPENSE' || tx.type === 'TRANSFER')
    .reduce((acc, tx) => acc + tx.amount, 0);

  const netCashFlow = totalIncome - totalExpense;

  // Chart data: monthly/source aggregation
  const chartData = useMemo(() => {
    const map: Record<string, { name: string; income: number; expense: number }> = {};
    filteredTransactions.forEach((tx) => {
      const src = tx.source;
      if (!map[src]) {
        map[src] = { name: src, income: 0, expense: 0 };
      }
      if (tx.type === 'INCOME') {
        map[src].income += tx.amount;
      } else {
        map[src].expense += tx.amount;
      }
    });
    return Object.values(map);
  }, [filteredTransactions]);

  // CSV Export
  const handleExportCsv = () => {
    const headers = ['ID', 'Date', 'Source', 'Type', 'Category', 'Party', 'Amount (ETB)', 'Fee', 'Reference', 'Raw SMS'];
    const rows = filteredTransactions.map((tx) => [
      tx.id,
      new Date(tx.timestamp).toISOString(),
      tx.source,
      tx.type,
      tx.category,
      `"${(tx.recipientOrSender || '').replace(/"/g, '""')}"`,
      tx.amount,
      tx.fee || 0,
      `"${(tx.reference || '').replace(/"/g, '""')}"`,
      `"${(tx.rawSmsBody || '').replace(/"/g, '""')}"`,
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map((r) => r.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `EthioBalance_Ledger_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    setExportNotice(true);
    setTimeout(() => setExportNotice(false), 3000);
  };

  const handleResetFilters = () => {
    setSelectedPeriod('allTime');
    setCustomStartDate('');
    setCustomEndDate('');
    setSelectedSource('ALL');
    setSelectedType('ALL');
    setSelectedCategory('ALL');
    setSearchTerm('');
  };

  const hasActiveFilters =
    selectedPeriod !== 'allTime' ||
    selectedSource !== 'ALL' ||
    selectedType !== 'ALL' ||
    selectedCategory !== 'ALL' ||
    searchTerm.trim() !== '' ||
    customStartDate !== '' ||
    customEndDate !== '';

  return (
    <div className="space-y-4 animate-fadeIn pb-16">
      {/* Top Header Card */}
      <div className="rounded-3xl bg-slate-900 border border-slate-800 p-4 shadow-xl">
        <div className="flex items-center justify-between gap-2 mb-3">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
              <Layers className="w-5 h-5" />
            </div>
            <div>
              <span className="text-xs font-bold uppercase tracking-widest text-slate-400">
                {t(language, 'transactionHistory')}
              </span>
              <div className="text-[10px] text-slate-500 font-semibold tracking-wide">
                {filteredTransactions.length} records • Offline Ledger
              </div>
            </div>
          </div>

          <div className="flex items-center gap-1.5">
            <button
              onClick={() => setShowChart(!showChart)}
              className={`p-2 rounded-xl border transition-colors ${
                showChart
                  ? 'bg-purple-600 border-purple-500 text-white shadow-md'
                  : 'bg-slate-800 hover:bg-slate-700 border-slate-700 text-slate-400 hover:text-white'
              }`}
              title={showChart ? 'Hide Analytics' : 'Show Analytics'}
            >
              <BarChart3 className="w-4 h-4" />
            </button>

            <button
              id="export-csv-btn"
              onClick={handleExportCsv}
              className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-400 hover:text-white transition-colors"
              title="Download CSV Ledger"
            >
              <Download className="w-4 h-4 text-emerald-400" />
            </button>

            <button
              id="add-tx-history-btn"
              onClick={onOpenAddTx}
              className="flex items-center gap-1 px-3 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 font-bold text-xs text-white shadow-md shadow-emerald-950/40 transition-all active:scale-95"
            >
              <PlusCircle className="w-3.5 h-3.5" />
              <span>Record</span>
            </button>
          </div>
        </div>

        {/* Date Filter: Identical to Home Page SummaryCard Style */}
        <div className="pt-3 border-t border-slate-800">
          <div className="flex items-center justify-between gap-2 mb-2">
            <div className="flex items-center gap-1.5">
              <CalendarDays className="w-3.5 h-3.5 text-emerald-400" />
              <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                {t(language, 'filterByDate')}
              </span>
            </div>

            {hasActiveFilters && (
              <button
                onClick={handleResetFilters}
                className="flex items-center gap-1 text-[10px] font-bold text-amber-400 hover:text-amber-300 transition-colors"
              >
                <RotateCcw className="w-3 h-3" />
                <span>Reset</span>
              </button>
            )}
          </div>

          {/* Period selector pills container - matching Home page */}
          <div className="flex items-center bg-slate-950/60 p-1 rounded-xl border border-slate-800 text-[11px] font-bold overflow-x-auto no-scrollbar gap-1">
            {[
              { id: 'allTime', label: t(language, 'allTime') },
              { id: 'today', label: t(language, 'today') },
              { id: 'weekly', label: t(language, 'thisWeek') },
              { id: 'monthly', label: t(language, 'thisMonth') },
              { id: 'yearly', label: t(language, 'thisYear') },
              { id: 'custom', label: t(language, 'custom') },
            ].map((item) => (
              <button
                key={item.id}
                onClick={() => setSelectedPeriod(item.id as FilterPeriod)}
                className={`px-2.5 py-1 rounded-lg transition-colors whitespace-nowrap ${
                  selectedPeriod === item.id ? 'bg-slate-700 text-white' : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {item.label}
              </button>
            ))}
          </div>

          {/* Custom Date Range Picker when Custom is chosen */}
          {selectedPeriod === 'custom' && (
            <div className="mt-3 p-3 rounded-2xl bg-slate-950/60 border border-slate-800 space-y-2 animate-fadeIn">
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-[10px] font-bold uppercase text-slate-400 mb-1">
                    {t(language, 'startDate')}
                  </label>
                  <input
                    id="filter-custom-start-date"
                    type="date"
                    value={customStartDate}
                    onChange={(e) => setCustomStartDate(e.target.value)}
                    className="w-full px-2.5 py-1.5 rounded-xl bg-slate-900 border border-slate-700 text-slate-200 text-[11px] font-mono focus:outline-none focus:ring-1 focus:ring-emerald-500"
                  />
                </div>
                <div>
                  <label className="block text-[10px] font-bold uppercase text-slate-400 mb-1">
                    {t(language, 'endDate')}
                  </label>
                  <input
                    id="filter-custom-end-date"
                    type="date"
                    value={customEndDate}
                    onChange={(e) => setCustomEndDate(e.target.value)}
                    className="w-full px-2.5 py-1.5 rounded-xl bg-slate-900 border border-slate-700 text-slate-200 text-[11px] font-mono focus:outline-none focus:ring-1 focus:ring-emerald-500"
                  />
                </div>
              </div>

              {(customStartDate || customEndDate) && (
                <button
                  onClick={() => {
                    setCustomStartDate('');
                    setCustomEndDate('');
                  }}
                  className="w-full py-1 rounded-lg bg-slate-900 border border-slate-700 text-slate-400 hover:text-white text-[10px] font-semibold flex items-center justify-center gap-1 transition-colors"
                >
                  <X className="w-3 h-3" />
                  <span>{t(language, 'clearDateFilter')}</span>
                </button>
              )}
            </div>
          )}
        </div>
      </div>

      {exportNotice && (
        <div className="p-3 rounded-2xl bg-emerald-950/50 border border-emerald-500/40 text-emerald-300 text-xs font-semibold text-center animate-fadeIn">
          {t(language, 'exportSuccess')}
        </div>
      )}

      {/* Analytics Chart */}
      {showChart && (
        <div className="p-4 rounded-3xl bg-slate-900 border border-slate-800 shadow-xl space-y-2 animate-fadeIn">
          <div className="flex items-center justify-between">
            <span className="text-xs font-extrabold uppercase tracking-widest text-slate-400">
              Source Flow (ETB)
            </span>
            <span className="text-[10px] text-slate-500 font-semibold">Income vs Expense</span>
          </div>

          <div className="h-48 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                <XAxis dataKey="name" stroke="#94a3b8" fontSize={10} />
                <YAxis stroke="#94a3b8" fontSize={10} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#0f172a',
                    borderColor: '#334155',
                    borderRadius: '0.75rem',
                    color: '#f8fafc',
                    fontSize: '11px',
                  }}
                />
                <Bar dataKey="income" fill="#10b981" name="Income (ETB)" radius={[4, 4, 0, 0]} />
                <Bar dataKey="expense" fill="#f43f5e" name="Expense (ETB)" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      {/* Search & Secondary Filter Dropdowns */}
      <div className="p-4 rounded-3xl bg-slate-900 border border-slate-800 shadow-xl space-y-2.5">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input
            id="tx-search-input"
            type="text"
            placeholder={t(language, 'searchTransactions')}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-7 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs placeholder:text-slate-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm('')}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 p-1 text-slate-400 hover:text-white"
            >
              <X className="w-3 h-3" />
            </button>
          )}
        </div>

        <div className="grid grid-cols-3 gap-1.5 text-xs">
          <div>
            <select
              value={selectedSource}
              onChange={(e) => setSelectedSource(e.target.value)}
              className="w-full px-2 py-1.5 rounded-xl bg-slate-950 border border-slate-700 text-slate-200 text-[11px] font-semibold focus:outline-none focus:ring-1 focus:ring-emerald-500 truncate"
            >
              <option value="ALL">All Sources</option>
              {banks.map((b) => (
                <option key={b.id} value={b.abbreviation}>
                  {b.abbreviation}
                </option>
              ))}
              <option value="AIRTIME">Airtime</option>
            </select>
          </div>

          <div>
            <select
              value={selectedType}
              onChange={(e) => setSelectedType(e.target.value as any)}
              className="w-full px-2 py-1.5 rounded-xl bg-slate-950 border border-slate-700 text-slate-200 text-[11px] font-semibold focus:outline-none focus:ring-1 focus:ring-emerald-500 truncate"
            >
              <option value="ALL">{t(language, 'allTypes')}</option>
              <option value="INCOME">{t(language, 'incomesOnly')}</option>
              <option value="EXPENSE">{t(language, 'expensesOnly')}</option>
              <option value="TRANSFER">{t(language, 'transfersOnly')}</option>
            </select>
          </div>

          <div>
            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value as any)}
              className="w-full px-2 py-1.5 rounded-xl bg-slate-950 border border-slate-700 text-slate-200 text-[11px] font-semibold focus:outline-none focus:ring-1 focus:ring-emerald-500 truncate"
            >
              <option value="ALL">All Categories</option>
              <option value="SALARY">Salary</option>
              <option value="TELECOM">Telecom</option>
              <option value="RECHARGE">Recharge</option>
              <option value="SHOPPING">Shopping</option>
              <option value="DINING">Dining</option>
              <option value="UTILITY">Utility</option>
              <option value="TRANSFER">Transfer</option>
              <option value="GENERAL">General</option>
            </select>
          </div>
        </div>
      </div>

      {/* Filtered Totals & Period Summary Card */}
      <div className="flex items-center justify-between text-xs px-3 py-2.5 rounded-2xl bg-slate-900 border border-slate-800 text-slate-400">
        <div>
          <span className="font-bold text-white">{filteredTransactions.length}</span> items
        </div>
        <div className="flex items-center gap-2">
          <span className="text-emerald-400 font-bold">+{formatCurrency(totalIncome)}</span>
          <span className="text-rose-400 font-bold">-{formatCurrency(totalExpense)}</span>
          <span className="text-slate-600 font-bold">|</span>
          <span className={`font-bold ${netCashFlow >= 0 ? 'text-teal-400' : 'text-rose-400'}`}>
            {formatCurrency(netCashFlow)} ETB
          </span>
        </div>
      </div>

      {/* Transactions List */}
      {filteredTransactions.length > 0 ? (
        <div className="space-y-2">
          {filteredTransactions.map((tx) => (
            <TransactionItem key={tx.id} transaction={tx} language={language} />
          ))}
        </div>
      ) : (
        <div className="p-8 text-center rounded-3xl bg-slate-900 border border-slate-800 text-slate-500 text-xs space-y-2">
          <p>No transactions match the selected filters or date range.</p>
          <button
            onClick={handleResetFilters}
            className="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 font-bold text-[11px] transition-colors"
          >
            Clear Filters
          </button>
        </div>
      )}
    </div>
  );
};
