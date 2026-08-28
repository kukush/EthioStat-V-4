import React from 'react';
import { ArrowRightLeft, Gift, Globe, MessageSquare, PhoneCall, PlusCircle, Radio, Sparkles } from 'lucide-react';
import { Language } from '../types';
import { formatCurrency, t } from '../constants/translations';

interface TelecomAssetCardProps {
  language: Language;
  dataVolGb: number;
  voiceMinutes: number;
  smsCount: number;
  airtimeBalance: number;
  onOpenUssd: (action?: 'balance' | 'recharge' | 'transfer' | 'gift') => void;
}

export const TelecomAssetCard: React.FC<TelecomAssetCardProps> = ({
  language,
  dataVolGb,
  voiceMinutes,
  smsCount,
  airtimeBalance,
  onOpenUssd,
}) => {
  return (
    <div className="rounded-3xl bg-slate-900 border border-slate-800 p-5 shadow-xl">
      <div className="flex items-center justify-between gap-2 mb-4">
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-xl bg-blue-500/10 border border-blue-500/20 text-blue-400">
            <Radio className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-xs font-bold uppercase tracking-widest text-slate-400">
              {t(language, 'telecomAssets')}
            </h3>
            <span className="text-[10px] text-slate-500 font-semibold">Ethio Telecom / *804#</span>
          </div>
        </div>

        {/* Quick USSD Action Buttons */}
        <div className="flex items-center gap-1.5">
          <button
            id="quick-recharge-btn"
            onClick={() => onOpenUssd('recharge')}
            className="flex items-center gap-1 px-2.5 py-1 rounded-xl bg-emerald-600/20 hover:bg-emerald-600/30 border border-emerald-500/30 text-emerald-300 font-bold text-[11px] transition-all active:scale-95"
            title="Recharge Airtime via Voucher *805*"
          >
            <PlusCircle className="w-3.5 h-3.5" />
            <span>{t(language, 'recharge')}</span>
          </button>
          <button
            id="quick-transfer-btn"
            onClick={() => onOpenUssd('transfer')}
            className="flex items-center gap-1 px-2.5 py-1 rounded-xl bg-cyan-600/20 hover:bg-cyan-600/30 border border-cyan-500/30 text-cyan-300 font-bold text-[11px] transition-all active:scale-95"
            title="Transfer Airtime *806*"
          >
            <ArrowRightLeft className="w-3.5 h-3.5" />
            <span>{t(language, 'transfer')}</span>
          </button>
        </div>
      </div>

      {/* 4-Item Metric Grid: Data, Voice, SMS, Airtime */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {/* Data / Internet */}
        <div className="p-3.5 rounded-2xl bg-gradient-to-b from-blue-950/40 to-slate-900 border border-blue-500/20 flex flex-col justify-between">
          <div className="flex items-center justify-between mb-2">
            <span className="text-[11px] font-bold text-blue-400 tracking-wider uppercase flex items-center gap-1">
              <Globe className="w-3.5 h-3.5" />
              {t(language, 'data')}
            </span>
            <span className="text-[10px] font-extrabold px-1.5 py-0.5 rounded bg-blue-500/20 text-blue-300">
              GB
            </span>
          </div>
          <div>
            <div className="text-2xl font-black text-white tracking-tight">
              {dataVolGb.toFixed(2)}
            </div>
            <div className="text-[10px] text-slate-400 font-medium mt-0.5">
              Available Internet
            </div>
          </div>
        </div>

        {/* Voice / Audio */}
        <div className="p-3.5 rounded-2xl bg-gradient-to-b from-purple-950/40 to-slate-900 border border-purple-500/20 flex flex-col justify-between">
          <div className="flex items-center justify-between mb-2">
            <span className="text-[11px] font-bold text-purple-400 tracking-wider uppercase flex items-center gap-1">
              <PhoneCall className="w-3.5 h-3.5" />
              {t(language, 'voice')}
            </span>
            <span className="text-[10px] font-extrabold px-1.5 py-0.5 rounded bg-purple-500/20 text-purple-300">
              MIN
            </span>
          </div>
          <div>
            <div className="text-2xl font-black text-white tracking-tight">
              {Math.round(voiceMinutes)}
            </div>
            <div className="text-[10px] text-slate-400 font-medium mt-0.5">
              Voice Minutes
            </div>
          </div>
        </div>

        {/* SMS Messages */}
        <div className="p-3.5 rounded-2xl bg-gradient-to-b from-amber-950/40 to-slate-900 border border-amber-500/20 flex flex-col justify-between">
          <div className="flex items-center justify-between mb-2">
            <span className="text-[11px] font-bold text-amber-400 tracking-wider uppercase flex items-center gap-1">
              <MessageSquare className="w-3.5 h-3.5" />
              {t(language, 'sms')}
            </span>
            <span className="text-[10px] font-extrabold px-1.5 py-0.5 rounded bg-amber-500/20 text-amber-300">
              SMS
            </span>
          </div>
          <div>
            <div className="text-2xl font-black text-white tracking-tight">
              {smsCount}
            </div>
            <div className="text-[10px] text-slate-400 font-medium mt-0.5">
              SMS Left
            </div>
          </div>
        </div>

        {/* Available Airtime */}
        <div className="p-3.5 rounded-2xl bg-gradient-to-b from-emerald-950/40 to-slate-900 border border-emerald-500/20 flex flex-col justify-between">
          <div className="flex items-center justify-between mb-2">
            <span className="text-[11px] font-bold text-emerald-400 tracking-wider uppercase flex items-center gap-1">
              <Sparkles className="w-3.5 h-3.5" />
              Airtime
            </span>
            <span className="text-[10px] font-extrabold px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-300">
              ETB
            </span>
          </div>
          <div>
            <div className="text-2xl font-black text-white tracking-tight">
              {formatCurrency(airtimeBalance)}
            </div>
            <div className="text-[10px] text-slate-400 font-medium mt-0.5">
              Prepaid Credit
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
