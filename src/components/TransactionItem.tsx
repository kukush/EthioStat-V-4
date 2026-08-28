import React, { useState } from 'react';
import { ArrowDownLeft, ArrowUpRight, ChevronDown, ChevronUp, Copy, ExternalLink, HelpCircle, Layers, MessageSquare, ShoppingCart, Utensils, Zap } from 'lucide-react';
import { Language, TransactionCategory, TransactionEntity } from '../types';
import { formatCurrency, formatDate, t } from '../constants/translations';

interface TransactionItemProps {
  transaction: TransactionEntity;
  language: Language;
}

const getCategoryIcon = (category: TransactionCategory) => {
  switch (category) {
    case 'UTILITY':
      return <Zap className="w-4 h-4 text-amber-400" />;
    case 'GROCERY':
    case 'SHOPPING':
      return <ShoppingCart className="w-4 h-4 text-blue-400" />;
    case 'DINING':
      return <Utensils className="w-4 h-4 text-orange-400" />;
    case 'TELECOM':
    case 'RECHARGE':
      return <Zap className="w-4 h-4 text-cyan-400" />;
    case 'SALARY':
      return <ArrowDownLeft className="w-4 h-4 text-emerald-400" />;
    case 'TRANSFER':
    default:
      return <Layers className="w-4 h-4 text-indigo-400" />;
  }
};

const getSourceBadgeColor = (source: string) => {
  const s = source.toUpperCase();
  if (s.includes('CBEBIRR')) return 'bg-fuchsia-500/20 text-fuchsia-300 border-fuchsia-500/30';
  if (s.includes('CBE')) return 'bg-purple-500/20 text-purple-300 border-purple-500/30';
  if (s.includes('TELEBIRR')) return 'bg-yellow-500/20 text-yellow-300 border-yellow-500/30';
  if (s.includes('AWASH')) return 'bg-blue-500/20 text-blue-300 border-blue-500/30';
  if (s.includes('DASHEN')) return 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30';
  if (s.includes('BOA') || s.includes('ABYSSINIA')) return 'bg-orange-500/20 text-orange-300 border-orange-500/30';
  if (s.includes('COOP')) return 'bg-lime-500/20 text-lime-300 border-lime-500/30';
  if (s.includes('AIRTIME')) return 'bg-cyan-500/20 text-cyan-300 border-cyan-500/30';
  return 'bg-slate-700/40 text-slate-300 border-slate-600/40';
};

export const TransactionItem: React.FC<TransactionItemProps> = ({ transaction, language }) => {
  const [expanded, setExpanded] = useState(false);
  const [copied, setCopied] = useState(false);

  const isIncome = transaction.type === 'INCOME';
  const isTransfer = transaction.type === 'TRANSFER';

  const handleCopyRef = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (transaction.reference) {
      navigator.clipboard.writeText(transaction.reference);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div
      onClick={() => setExpanded(!expanded)}
      className="group rounded-2xl bg-slate-900/90 hover:bg-slate-800/80 border border-slate-800 hover:border-slate-700 p-4 transition-all cursor-pointer shadow-sm"
    >
      <div className="flex items-center justify-between gap-3">
        {/* Category icon & Party / description */}
        <div className="flex items-center gap-3 min-w-0">
          <div className="p-2.5 rounded-xl bg-slate-800 border border-slate-700/60 shrink-0 group-hover:scale-105 transition-transform">
            {getCategoryIcon(transaction.category)}
          </div>
          <div className="min-w-0">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="font-bold text-sm text-slate-100 truncate">
                {transaction.recipientOrSender || transaction.source}
              </span>
              <span
                className={`text-[10px] font-extrabold uppercase px-2 py-0.5 rounded-full border ${getSourceBadgeColor(
                  transaction.source
                )}`}
              >
                {transaction.source}
              </span>
            </div>
            <div className="flex items-center gap-2 text-xs text-slate-400 mt-0.5">
              <span>{formatDate(transaction.timestamp, language)}</span>
              {transaction.reference && (
                <>
                  <span>•</span>
                  <span className="font-mono text-[11px] text-slate-500 truncate max-w-[120px]">
                    {transaction.reference}
                  </span>
                </>
              )}
            </div>
          </div>
        </div>

        {/* Amount & Type Indicator */}
        <div className="text-right shrink-0">
          <div
            className={`text-base sm:text-lg font-black tracking-tight ${
              isIncome
                ? 'text-emerald-400'
                : isTransfer
                ? 'text-cyan-400'
                : 'text-slate-200'
            }`}
          >
            {isIncome ? '+' : '-'}{formatCurrency(transaction.amount)}{' '}
            <span className="text-xs font-bold text-slate-400">ETB</span>
          </div>
          <div className="flex items-center justify-end gap-1 text-[11px] text-slate-400 font-semibold mt-0.5">
            <span>
              {isIncome ? t(language, 'income') : isTransfer ? t(language, 'transfer') : t(language, 'expense')}
            </span>
            {expanded ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
          </div>
        </div>
      </div>

      {/* Expanded SMS Payload Details Drawer */}
      {expanded && (
        <div className="mt-3 pt-3 border-t border-slate-800 text-xs text-slate-300 space-y-2 animate-fadeIn">
          {transaction.rawSmsBody && (
            <div className="p-3 rounded-xl bg-slate-950/80 border border-slate-800">
              <div className="flex items-center justify-between text-[11px] text-slate-400 mb-1">
                <span className="flex items-center gap-1 font-bold uppercase tracking-wider text-slate-400">
                  <MessageSquare className="w-3 h-3 text-emerald-400" />
                  Parsed Raw SMS
                </span>
                <span className="font-mono text-[10px]">SIM {transaction.simSlot || 1}</span>
              </div>
              <p className="text-slate-300 font-mono text-[11px] leading-relaxed select-text">
                {transaction.rawSmsBody}
              </p>
            </div>
          )}

          <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 pt-1">
            {transaction.reference && (
              <div className="p-2 rounded-lg bg-slate-800/60 border border-slate-700/40 flex items-center justify-between">
                <div>
                  <span className="text-[10px] text-slate-500 uppercase font-bold block">Ref ID</span>
                  <span className="font-mono text-slate-300 text-xs">{transaction.reference}</span>
                </div>
                <button
                  onClick={handleCopyRef}
                  className="p-1 rounded hover:bg-slate-700 text-slate-400 hover:text-white"
                  title="Copy Reference"
                >
                  <Copy className="w-3.5 h-3.5" />
                </button>
              </div>
            )}

            {transaction.fee !== undefined && (
              <div className="p-2 rounded-lg bg-slate-800/60 border border-slate-700/40">
                <span className="text-[10px] text-slate-500 uppercase font-bold block">Service Fee / VAT</span>
                <span className="text-slate-300 text-xs font-semibold">{formatCurrency(transaction.fee)} ETB</span>
              </div>
            )}

            {transaction.balanceAfter !== undefined && (
              <div className="p-2 rounded-lg bg-slate-800/60 border border-slate-700/40">
                <span className="text-[10px] text-slate-500 uppercase font-bold block">New Balance</span>
                <span className="text-emerald-400 text-xs font-bold">{formatCurrency(transaction.balanceAfter)} ETB</span>
              </div>
            )}
          </div>
          {copied && (
            <div className="text-[10px] text-emerald-400 font-bold text-center">Reference copied to clipboard!</div>
          )}
        </div>
      )}
    </div>
  );
};
