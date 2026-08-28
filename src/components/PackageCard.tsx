import React from 'react';
import { Clock, Globe, MessageSquare, PhoneCall, Sparkles } from 'lucide-react';
import { BalancePackageEntity, Language } from '../types';
import { formatDate, t } from '../constants/translations';

interface PackageCardProps {
  pkg: BalancePackageEntity;
  language: Language;
}

export const PackageCard: React.FC<PackageCardProps> = ({ pkg, language }) => {
  const isInternet = pkg.type === 'internet';
  const isVoice = pkg.type === 'voice';
  const isSms = pkg.type === 'sms';

  const percentage = Math.min(100, Math.max(0, (pkg.remainingAmount / (pkg.totalAmount || 1)) * 100));

  const now = Date.now();
  const msRemaining = Math.max(0, pkg.expiryDate - now);
  const daysLeft = Math.ceil(msRemaining / (1000 * 60 * 60 * 24));

  const getCardIcon = () => {
    if (isInternet) return <Globe className="w-4 h-4 text-blue-400" />;
    if (isVoice) return <PhoneCall className="w-4 h-4 text-purple-400" />;
    if (isSms) return <MessageSquare className="w-4 h-4 text-amber-400" />;
    return <Sparkles className="w-4 h-4 text-emerald-400" />;
  };

  const getBorderTheme = () => {
    if (isInternet) return 'border-blue-500/30 bg-blue-950/20';
    if (isVoice) return 'border-purple-500/30 bg-purple-950/20';
    if (isSms) return 'border-amber-500/30 bg-amber-950/20';
    return 'border-emerald-500/30 bg-emerald-950/20';
  };

  const getProgressColor = () => {
    if (percentage < 20) return 'bg-rose-500';
    if (percentage < 50) return 'bg-amber-500';
    if (isInternet) return 'bg-blue-500';
    if (isVoice) return 'bg-purple-500';
    if (isSms) return 'bg-amber-500';
    return 'bg-emerald-500';
  };

  return (
    <div className={`rounded-2xl border p-4 shadow-md transition-all hover:scale-[1.01] ${getBorderTheme()}`}>
      <div className="flex items-center justify-between gap-2 mb-3">
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-xl bg-slate-800/80 border border-slate-700/60">
            {getCardIcon()}
          </div>
          <div>
            <h4 className="font-extrabold text-sm text-white capitalize flex items-center gap-1.5">
              {pkg.subType} {t(language, pkg.type)}
            </h4>
            <span className="text-[10px] text-slate-400 font-semibold uppercase tracking-wider">
              {pkg.unit === 'GB' ? `${pkg.totalAmount} GB Total` : pkg.unit === 'MIN' ? `${pkg.totalAmount} Mins Total` : `${pkg.totalAmount} SMS Total`}
            </span>
          </div>
        </div>

        {/* Days left badge */}
        <div className="text-right">
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-slate-800 border border-slate-700 text-[11px] font-bold text-slate-200">
            <Clock className="w-3 h-3 text-emerald-400" />
            {daysLeft} {t(language, 'daysLeft')}
          </span>
        </div>
      </div>

      {/* Progress Bar & Numeric Quota */}
      <div className="space-y-2">
        <div className="flex items-baseline justify-between text-xs">
          <span className="text-slate-400 font-semibold">{t(language, 'left')}</span>
          <span className="font-black text-white text-sm">
            {pkg.remainingAmount.toFixed(pkg.unit === 'GB' ? 2 : 0)}{' '}
            <span className="text-xs font-bold text-slate-400">{pkg.unit}</span>
          </span>
        </div>

        <div className="w-full h-2.5 rounded-full bg-slate-800 overflow-hidden border border-slate-700/50">
          <div
            className={`h-full rounded-full transition-all duration-500 ${getProgressColor()}`}
            style={{ width: `${percentage}%` }}
          />
        </div>

        <div className="flex items-center justify-between text-[10px] text-slate-500 font-semibold pt-1">
          <span>{t(language, 'expires')}: {formatDate(pkg.expiryDate, language)}</span>
          <span>{percentage.toFixed(0)}% Quota</span>
        </div>
      </div>
    </div>
  );
};
