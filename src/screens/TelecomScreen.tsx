import React, { useState } from 'react';
import { ArrowRightLeft, Clock, Gift, Globe, MessageSquare, PhoneCall, PlusCircle, Radio, RefreshCw, Send, Sparkles, Zap } from 'lucide-react';
import { BalancePackageEntity, Language, RecommendedBundle, TelecomAssets } from '../types';
import { formatCurrency, t } from '../constants/translations';
import { PackageCard } from '../components/PackageCard';
import { RECOMMENDED_BUNDLES } from '../constants/banks';

interface TelecomScreenProps {
  language: Language;
  telecomAssets: TelecomAssets;
  packages: BalancePackageEntity[];
  onOpenUssd: (action?: 'balance' | 'recharge' | 'transfer' | 'gift') => void;
  onBuyBundle: (bundle: RecommendedBundle) => void;
}

export const TelecomScreen: React.FC<TelecomScreenProps> = ({
  language,
  telecomAssets,
  packages,
  onOpenUssd,
  onBuyBundle,
}) => {
  const [selectedFilter, setSelectedFilter] = useState<'all' | 'internet' | 'voice' | 'sms'>('all');

  const filteredPackages = packages.filter((p) => {
    if (!p.isActive) return false;
    if (selectedFilter === 'all') return true;
    return p.type === selectedFilter;
  });

  return (
    <div className="space-y-4 animate-fadeIn pb-16">
      {/* Airtime & Quick Action Banner */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-emerald-950 via-slate-900 to-slate-900 p-5 border border-emerald-500/30 shadow-2xl">
        <div className="space-y-3">
          <div>
            <div className="flex items-center gap-1.5 text-xs font-bold text-emerald-400 uppercase tracking-widest mb-1">
              <Radio className="w-3.5 h-3.5 animate-pulse" />
              <span>Ethio Telecom</span>
              <span className="px-1.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 font-mono text-[9px]">
                {telecomAssets.sim1Number}
              </span>
            </div>
            <div className="flex items-baseline gap-1.5">
              <span className="text-3xl font-extrabold text-white">
                {formatCurrency(telecomAssets.airtimeBalance)}
              </span>
              <span className="text-xs font-bold text-emerald-400">ETB Available</span>
            </div>
            <p className="text-[10px] text-slate-400 mt-0.5">
              Last synced: {new Date(telecomAssets.lastSyncedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
            </p>
          </div>

          <div className="grid grid-cols-3 gap-1.5">
            <button
              id="telecom-sync-btn"
              onClick={() => onOpenUssd('balance')}
              className="flex flex-col items-center justify-center py-2 px-1 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 font-bold text-[10px] text-slate-200 transition-all active:scale-95"
            >
              <RefreshCw className="w-3.5 h-3.5 text-emerald-400 mb-0.5" />
              <span>*804# Sync</span>
            </button>

            <button
              id="telecom-recharge-btn"
              onClick={() => onOpenUssd('recharge')}
              className="flex flex-col items-center justify-center py-2 px-1 rounded-xl bg-emerald-600 hover:bg-emerald-500 font-bold text-[10px] text-white shadow-md shadow-emerald-950/50 transition-all active:scale-95"
            >
              <PlusCircle className="w-3.5 h-3.5 mb-0.5" />
              <span>*805* Recharge</span>
            </button>

            <button
              id="telecom-transfer-btn"
              onClick={() => onOpenUssd('transfer')}
              className="flex flex-col items-center justify-center py-2 px-1 rounded-xl bg-cyan-600 hover:bg-cyan-500 font-bold text-[10px] text-white shadow-md shadow-cyan-950/50 transition-all active:scale-95"
            >
              <ArrowRightLeft className="w-3.5 h-3.5 mb-0.5" />
              <span>*806* Transfer</span>
            </button>
          </div>
        </div>
      </div>

      {/* Active Packages Section */}
      <div className="rounded-3xl bg-slate-900 border border-slate-800 p-4 shadow-xl">
        <div className="flex items-center justify-between gap-2 mb-3">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-blue-500/10 border border-blue-500/20 text-blue-400">
              <Sparkles className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-xs font-bold uppercase tracking-widest text-slate-400">
                {t(language, 'activePackages')}
              </h3>
              <span className="text-[10px] text-slate-500 font-semibold">Parsed from *804#</span>
            </div>
          </div>

          {/* Type filter pills */}
          <div className="flex items-center bg-slate-950/70 p-0.5 rounded-xl border border-slate-800 text-[10px] font-bold">
            {(['all', 'internet', 'voice', 'sms'] as const).map((filter) => (
              <button
                key={filter}
                onClick={() => setSelectedFilter(filter)}
                className={`px-2 py-1 rounded-lg transition-colors capitalize ${
                  selectedFilter === filter ? 'bg-slate-700 text-white' : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {filter === 'all' ? 'All' : filter}
              </button>
            ))}
          </div>
        </div>

        {filteredPackages.length > 0 ? (
          <div className="space-y-2.5">
            {filteredPackages.map((pkg) => (
              <PackageCard key={pkg.id} pkg={pkg} language={language} />
            ))}
          </div>
        ) : (
          <div className="py-6 text-center text-slate-500 text-xs space-y-1">
            <p>{t(language, 'noActivePackages')}</p>
            <p className="text-[10px] text-slate-600">{t(language, 'syncToSeePackages')}</p>
          </div>
        )}
      </div>

      {/* Recommended Bundles Catalog */}
      <div className="rounded-3xl bg-slate-900 border border-slate-800 p-4 shadow-xl">
        <div className="flex items-center justify-between gap-2 mb-3">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-400">
              <Gift className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-xs font-bold uppercase tracking-widest text-slate-400">
                {t(language, 'recommendedBundles')}
              </h3>
              <span className="text-[10px] text-slate-500 font-semibold">1-Click Subscriptions (*999#)</span>
            </div>
          </div>
          <button
            onClick={() => onOpenUssd('gift')}
            className="text-xs font-bold text-amber-400 hover:text-amber-300 flex items-center gap-0.5"
          >
            <Send className="w-3 h-3" />
            <span>{t(language, 'sendGift')}</span>
          </button>
        </div>

        <div className="space-y-2">
          {RECOMMENDED_BUNDLES.map((bundle) => (
            <div
              key={bundle.id}
              className="p-3 rounded-2xl bg-slate-950/70 border border-slate-800 flex items-center justify-between gap-2"
            >
              <div className="min-w-0">
                <div className="flex items-center gap-1.5 mb-0.5">
                  <span className="text-xs font-extrabold text-white truncate flex items-center gap-1">
                    {bundle.type === 'internet' ? (
                      <Globe className="w-3 h-3 text-blue-400 shrink-0" />
                    ) : bundle.type === 'voice' ? (
                      <PhoneCall className="w-3 h-3 text-purple-400 shrink-0" />
                    ) : (
                      <MessageSquare className="w-3 h-3 text-amber-400 shrink-0" />
                    )}
                    {bundle.title}
                  </span>
                  {bundle.popular && (
                    <span className="text-[8px] uppercase font-black px-1 py-0.2 rounded bg-amber-500/20 text-amber-400 border border-amber-500/30 shrink-0">
                      Popular
                    </span>
                  )}
                </div>
                <p className="text-[10px] text-slate-400 font-mono">
                  {bundle.ussdCode}
                </p>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                <span className="font-extrabold text-xs text-emerald-400">
                  {bundle.priceEtb} ETB
                </span>
                <button
                  onClick={() => onBuyBundle(bundle)}
                  className="px-2.5 py-1 rounded-xl bg-emerald-600/20 hover:bg-emerald-600 text-emerald-300 hover:text-white border border-emerald-500/30 text-xs font-bold transition-all active:scale-95"
                >
                  {t(language, 'buy')}
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
