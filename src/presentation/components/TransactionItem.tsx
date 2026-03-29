import React, { useState } from 'react';
import { cn } from '@/lib/utils';
import { Transaction, Language } from '@/types';
import { Smartphone, ShoppingBag, Utensils, Zap, ArrowUpRight, ArrowDownLeft, Calendar, Tag, Info, ChevronDown } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { useTranslation } from '@/translations';
import { getBankIcon } from '@/constants/bankIcons';
import { findBank } from '@/constants/banks';

interface TransactionItemProps {
  transaction: Transaction;
  language?: Language;
  className?: string;
}

export const TransactionItem: React.FC<TransactionItemProps> = ({ transaction, language = 'en' as Language, className }) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const t = useTranslation(language as Language);
  const isIncome = transaction.type === 'income';

  const getTranslatedSource = (source?: string) => {
    if (!source) return 'Unknown';
    const key = source.toLowerCase().replace(/\s+/g, '_');
    // @ts-ignore - dynamic key access
    const translated = t(key);
    return translated !== key ? translated : source;
  };

  const getIcon = () => {
    // Use bank icon if source is a known bank
    const bank = findBank(transaction.source);
    if (bank) {
      return getBankIcon(transaction.source, 32);
    }

    const category = (transaction.category || '').toLowerCase();
    switch (category) {
      case 'utility':
      case 'utilities':
      case 'bills':
        return <Zap size={20} className="text-blue-500" />;
      case 'grocery':
      case 'shopping':
      case 'market':
        return <ShoppingBag size={20} className="text-purple-500" />;
      case 'dining':
      case 'food':
      case 'restaurant':
        return <Utensils size={20} className="text-orange-500" />;
      case 'telecom':
      case 'recharge':
      case 'phone':
        return <Smartphone size={20} className="text-green-500" />;
      default:
        return isIncome ? (
          <ArrowUpRight size={20} className="text-emerald-500" />
        ) : (
          <ArrowDownLeft size={20} className="text-rose-500" />
        );
    }
  };

  return (
    <div 
      onClick={() => setIsExpanded(!isExpanded)}
      className={cn(
        'bg-white p-4 rounded-2xl transition-all duration-300 border border-transparent cursor-pointer overflow-hidden',
        isExpanded ? 'shadow-lg shadow-slate-200/50 border-slate-100 scale-[1.02]' : 'hover:bg-slate-50 hover:border-slate-100',
        className
      )}
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-slate-50 flex items-center justify-center">
            {getIcon()}
          </div>
          <div>
            <p className="font-bold text-slate-900">{transaction.description}</p>
            <p className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
              {getTranslatedSource(transaction.source)} • {transaction.timestamp}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <div className="text-right">
            <p className={cn(
              'font-black',
              isIncome ? 'text-green-600' : 'text-slate-900'
            )}>
              {isIncome ? '+' : '-'} ETB {Math.abs(transaction.amount).toLocaleString(undefined, { minimumFractionDigits: 2 })}
            </p>
            <p className="text-[9px] font-bold text-slate-400 uppercase tracking-widest">
              {transaction.category}
            </p>
          </div>
          <motion.div
            animate={{ rotate: isExpanded ? 180 : 0 }}
            className="text-slate-300"
          >
            <ChevronDown size={16} />
          </motion.div>
        </div>
      </div>

      <AnimatePresence>
        {isExpanded && (
          <motion.div
            initial={{ height: 0, opacity: 0, marginTop: 0 }}
            animate={{ height: 'auto', opacity: 1, marginTop: 16 }}
            exit={{ height: 0, opacity: 0, marginTop: 0 }}
            className="border-t border-slate-50 pt-4 space-y-3"
          >
            <div className="grid grid-cols-2 gap-3">
              <div className="flex items-center gap-2 text-slate-500">
                <Calendar size={14} />
                <div className="flex flex-col">
                  <span className="text-[8px] font-black uppercase tracking-widest text-slate-400">Full Date</span>
                  <span className="text-[10px] font-bold text-slate-700">{transaction.timestamp} (Full Timestamp)</span>
                </div>
              </div>
              <div className="flex items-center gap-2 text-slate-500">
                <Tag size={14} />
                <div className="flex flex-col">
                  <span className="text-[8px] font-black uppercase tracking-widest text-slate-400">Category</span>
                  <span className="text-[10px] font-bold text-slate-700">{transaction.category}</span>
                </div>
              </div>
            </div>
            <div className="flex items-center gap-2 text-slate-500 bg-slate-50 p-2 rounded-lg">
              <Info size={14} className="shrink-0" />
              <div className="flex flex-col">
                <span className="text-[8px] font-black uppercase tracking-widest text-slate-400">Transaction ID</span>
                <span className="text-[10px] font-bold text-slate-700 font-mono">{transaction.id}</span>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};
