import React from 'react';
import { motion } from 'framer-motion';
import { TrendingUp, TrendingDown, Wallet } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Transaction } from '@/domain/types';

interface SourceSummaryCardProps {
  source: string;
  transactions: Transaction[];
}

export const SourceSummaryCard: React.FC<SourceSummaryCardProps> = ({ source, transactions }) => {
  const sourceTransactions = transactions.filter(t => 
    t.source.toLowerCase() === source.toLowerCase() || 
    t.description.toLowerCase().includes(source.toLowerCase())
  );

  const income = sourceTransactions
    .filter(t => t.type === 'income')
    .reduce((acc, t) => acc + t.amount, 0);

  const expense = sourceTransactions
    .filter(t => t.type === 'expense')
    .reduce((acc, t) => acc + t.amount, 0);

  const net = income - expense;
  const count = sourceTransactions.length;
  const lastTime = sourceTransactions[0]?.timestamp || 'Never';

  if (count === 0) return null;

  return (
    <motion.div 
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-white p-6 rounded-[2.5rem] border border-slate-100 shadow-sm space-y-4"
    >
      <div className="flex justify-between items-center">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-full bg-blue-50 flex items-center justify-center text-blue-600">
            <Wallet size={16} />
          </div>
          <h3 className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">{source} Summary</h3>
        </div>
        <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">{count} txns</span>
      </div>

      <div className="flex justify-between items-end">
        <div>
          <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest leading-none mb-1">Net Balance</p>
          <h2 className={cn(
            "text-3xl font-black",
            net >= 0 ? "text-emerald-600" : "text-rose-600"
          )}>
            <span className="text-sm font-bold opacity-60 mr-1">ETB</span>
            {net.toLocaleString()}
          </h2>
        </div>
        <div className="flex flex-col items-end gap-1">
          <div className="flex items-center gap-1 text-emerald-600">
            <TrendingUp size={14} />
            <span className="text-xs font-bold">+{income.toLocaleString()}</span>
          </div>
          <div className="flex items-center gap-1 text-rose-600">
            <TrendingDown size={14} />
            <span className="text-xs font-bold">-{expense.toLocaleString()}</span>
          </div>
        </div>
      </div>

      <div className="pt-2 border-t border-slate-50 flex justify-between items-center">
        <span className="text-[8px] font-bold text-slate-300 uppercase tracking-widest">Last Activity</span>
        <span className="text-[8px] font-black text-slate-400 uppercase tracking-widest">{lastTime}</span>
      </div>
    </motion.div>
  );
};
