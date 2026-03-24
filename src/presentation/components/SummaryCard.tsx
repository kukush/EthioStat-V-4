import React from 'react';
import { motion } from 'framer-motion';
import { cn } from '@/lib/utils';
import { TrendingUp, TrendingDown } from 'lucide-react';

interface SummaryCardProps {
  type: 'income' | 'expense';
  amount: number;
  percentageChange: number;
  currency?: string;
  className?: string;
}

export const SummaryCard: React.FC<SummaryCardProps> = ({
  type, amount, percentageChange, currency = 'ETB', className
}) => {
  const isIncome = type === 'income';
  
  return (
    <div className={cn(
      'p-5 rounded-2xl shadow-sm border-l-4 bg-white',
      isIncome ? 'border-green-500' : 'border-red-500',
      className
    )}>
      <div className="flex items-center gap-2 mb-3">
        <div className={cn(
          'w-8 h-8 rounded-lg flex items-center justify-center',
          isIncome ? 'bg-green-50 text-green-600' : 'bg-red-50 text-red-600'
        )}>
          {isIncome ? <TrendingUp size={18} /> : <TrendingDown size={18} />}
        </div>
        <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">
          {isIncome ? 'Total Income' : 'Total Expenses'}
        </span>
      </div>
      
      <div className="space-y-1">
        <p className="text-xl font-black text-slate-900">
          {currency} {amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}
        </p>
        <p className={cn(
          'text-[10px] font-bold flex items-center gap-1',
          isIncome ? 'text-green-500' : 'text-red-500'
        )}>
          {percentageChange > 0 ? '+' : ''}{percentageChange}% VS LAST MONTH
        </p>
      </div>
    </div>
  );
};
