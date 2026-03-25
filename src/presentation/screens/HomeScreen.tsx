import React from 'react';
import { motion } from 'framer-motion';
import { TelecomPackage, Transaction, Language } from '@/domain/types';
import { TransactionItem } from '@/presentation/components/TransactionItem';
import { useTranslation } from '@/translations';
import { Globe, TrendingUp, TrendingDown, Wallet, Zap } from 'lucide-react';
import { cn } from '@/lib/utils';
import { SourceSummaryCard } from '@/presentation/components/SourceSummaryCard';

interface HomeScreenProps {
  packages: TelecomPackage[];
  transactions: Transaction[];
  telecomBalance: number;
  language: Language;
  userName?: string;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({ packages, transactions, telecomBalance, language, userName }) => {
  const t = useTranslation(language);

  // Extract unique sources from transactions
  const sources = Array.from(new Set(transactions.map(t => t.source))).filter(s => s !== 'Unknown');

  const totalIncome = transactions
    .filter(t => t.type === 'income')
    .reduce((acc, t) => acc + t.amount, 0);
    
  const totalExpense = transactions
    .filter(t => t.type === 'expense')
    .reduce((acc, t) => acc + t.amount, 0);

  const netBalance = totalIncome - totalExpense;

  return (
    <div className="space-y-8 pb-32">
      <header className="space-y-2">
        <h1 className="text-3xl font-black tracking-tight text-slate-900">{t('dashboard')}</h1>
        <p className="text-sm font-bold text-slate-400 uppercase tracking-widest">{t('welcome')}, {userName || 'User'}</p>
      </header>

      {/* Dual-Tracking Summary */}
      <section className="space-y-6">
        {/* Financial Summary Card */}
        <div className="bg-white p-6 rounded-[2.5rem] border border-slate-100 shadow-sm space-y-6">
          <div className="flex justify-between items-center">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-full bg-blue-50 flex items-center justify-center text-blue-600">
                <Wallet size={16} />
              </div>
              <h3 className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">{t('financialSummary')}</h3>
            </div>
            <span className="text-[10px] font-bold text-blue-600 uppercase tracking-widest">{t('history')}</span>
          </div>

          <div className="flex justify-between items-end">
            <div>
              <p className="text-xs font-bold text-slate-400 uppercase tracking-widest">Net Cash Flow</p>
              <h2 className={cn(
                "text-4xl font-black mt-1",
                netBalance >= 0 ? "text-emerald-600" : "text-rose-600"
              )}>
                <span className="text-lg font-bold opacity-60 mr-1">ETB</span>
                {netBalance.toLocaleString(undefined, { minimumFractionDigits: 2 })}
              </h2>
            </div>
            <div className="flex flex-col items-end gap-1">
              <div className="flex items-center gap-1 text-emerald-600">
                <TrendingUp size={14} />
                <span className="text-xs font-bold">+{totalIncome.toLocaleString()}</span>
              </div>
              <div className="flex items-center gap-1 text-rose-600">
                <TrendingDown size={14} />
                <span className="text-xs font-bold">-{totalExpense.toLocaleString()}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Telecom Assets Card */}
        <div className="bg-slate-900 p-6 rounded-[2.5rem] text-white shadow-2xl relative overflow-hidden">
          <div className="relative z-10 space-y-6">
            <div className="flex justify-between items-center">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center text-blue-400">
                  <Zap size={16} />
                </div>
                <h3 className="text-[10px] font-bold uppercase tracking-[0.2em] opacity-60">{t('telecomAssets')}</h3>
              </div>
              <div className="flex items-center gap-1 text-blue-400">
                <Globe size={14} />
                <span className="text-[10px] font-bold uppercase tracking-widest">Ethio Telecom</span>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-6">
              <div>
                <p className="text-[10px] font-bold uppercase tracking-[0.2em] opacity-60">{t('availableAirtime')}</p>
                <h2 className="text-3xl font-black mt-1">
                  <span className="text-sm font-bold opacity-60 mr-1">ETB</span>
                  {telecomBalance.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                </h2>
              </div>
              <div className="flex flex-col justify-center space-y-3">
                <div className="flex items-center gap-2">
                  <div className="w-1 h-6 bg-blue-500 rounded-full" />
                  <div>
                    <p className="text-[7px] font-bold uppercase tracking-widest opacity-60">{t('data')}</p>
                    <p className="text-xs font-black">
                      {packages.filter(p => p.type === 'internet').reduce((acc, p) => acc + (p.unit === 'GB' ? p.value : p.value / 1024), 0).toFixed(1)} 
                      <span className="text-[8px] opacity-60 ml-1">GB</span>
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-1 h-6 bg-emerald-500 rounded-full" />
                  <div>
                    <p className="text-[7px] font-bold uppercase tracking-widest opacity-60">{t('audio')}</p>
                    <p className="text-xs font-black">
                      {packages.filter(p => p.type === 'voice').reduce((acc, p) => acc + p.value, 0)} 
                      <span className="text-[8px] opacity-60 ml-1">Min</span>
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-1 h-6 bg-purple-500 rounded-full" />
                  <div>
                    <p className="text-[7px] font-bold uppercase tracking-widest opacity-60">{t('sms')}</p>
                    <p className="text-xs font-black">
                      {packages.filter(p => p.type === 'sms').reduce((acc, p) => acc + p.value, 0)} 
                      <span className="text-[8px] opacity-60 ml-1">SMS</span>
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          {/* Decorative elements */}
          <div className="absolute -right-10 -top-10 w-40 h-40 bg-blue-600/20 rounded-full blur-3xl" />
          <div className="absolute -left-10 -bottom-10 w-40 h-40 bg-purple-600/20 rounded-full blur-3xl" />
        </div>
      </section>

      {/* Per-Source Summaries (CBE, Telebirr, etc.) */}
      {sources.length > 0 && (
        <section className="space-y-4">
          <div className="flex justify-between items-end px-2">
            <h3 className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">Source Summaries</h3>
          </div>
          <div className="space-y-4">
            {sources.map(source => (
              <SourceSummaryCard key={source} source={source} transactions={transactions} />
            ))}
          </div>
        </section>
      )}

      <section className="space-y-4">
        <div className="flex justify-between items-end px-2">
          <h3 className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">{t('recentActivity')}</h3>
          <span className="text-[10px] font-bold text-blue-600 uppercase tracking-widest">{t('viewAll')}</span>
        </div>
        <div className="space-y-3">
          {transactions.slice(0, 4).map((t) => (
            <TransactionItem key={t.id} transaction={t} />
          ))}
        </div>
      </section>
    </div>
  );
};
