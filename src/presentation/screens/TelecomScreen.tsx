import React, { useState, useEffect } from 'react';
import { PackageCard } from '@/presentation/components/PackageCard';
import { TelecomPackage, RecommendedBundle, Language, PackageType } from '@/types';
import { motion, AnimatePresence } from 'framer-motion';
import { RefreshCw, Phone, Globe, Zap, MessageSquare, X, ShieldCheck, Info, Plus, Check } from 'lucide-react';
import { useTranslation } from '@/translations';
import { cn } from '@/lib/utils';
import { useNativeBridge } from '@/presentation/hooks/useNativeBridge';

interface TelecomScreenProps {
  packages: TelecomPackage[];
  recommendedBundles: RecommendedBundle[];
  balance: number;
  language: Language;
  dispatch: any;
}

export const TelecomScreen: React.FC<TelecomScreenProps> = ({ 
  packages, recommendedBundles, balance, language, dispatch 
}) => {
  const [showRechargeModal, setShowRechargeModal] = useState(false);
  const { rechargeSelf, sendUssdRequest, getUssdCodes } = useNativeBridge();
  const [voucherNumber, setVoucherNumber] = useState('');
  const [ussdCodes, setUssdCodes] = useState({ BALANCE_CHECK: '*804#' });
  const t = useTranslation(language);

  useEffect(() => {
    // Fetch USSD codes from native AppConstants
    const loadUssdCodes = async () => {
      try {
        const codes = await getUssdCodes();
        setUssdCodes(codes);
      } catch (error) {
        console.error('Failed to load USSD codes:', error);
      }
    };
    loadUssdCodes();
  }, [getUssdCodes]);

  const handleSync = () => {
    // Dial USSD code to get main airtime balance
    sendUssdRequest(ussdCodes.BALANCE_CHECK, (response) => {
      if (response) {
        // Parse the USSD response for airtime balance
        dispatch({ type: 'PARSE_USSD_RESPONSE', response });
      }
    });
  };



  const handleRecharge = () => {
    const cleanVoucher = voucherNumber.replace(/[^0-9]/g, '');
    // EthioTelecom vouchers are 13 or 14 digits. We only dial if it looks like a valid voucher.
    if (cleanVoucher.length < 13) {
      alert(t('invalidVoucher') || "Voucher must be at least 13 digits.");
      return;
    }
    rechargeSelf(cleanVoucher);

    setShowRechargeModal(false);
    setVoucherNumber('');
  };

  const packageTypes: PackageType[] = ['internet', 'voice', 'sms', 'bonus'];

  const totals = {
    internet: packages.filter(p => p.type === 'internet').reduce((acc, p) => {
      const val = Number(p.value) || 0;
      const valueMB = (p.unit || '').toUpperCase() === 'GB' ? val * 1024 : val;
      return acc + valueMB;
    }, 0),
    voice: packages.filter(p => p.type === 'voice').reduce((acc, p) => acc + (Number(p.value) || 0), 0),
    sms: packages.filter(p => p.type === 'sms').reduce((acc, p) => acc + (Number(p.value) || 0), 0),
  };

  const totalCapacity = {
    internet: packages.filter(p => p.type === 'internet').reduce((acc, p) => {
      const tot = Number(p.total) || 0;
      const totMB = (p.unit || '').toUpperCase() === 'GB' ? tot * 1024 : tot;
      return acc + totMB;
    }, 0),
    voice: packages.filter(p => p.type === 'voice').reduce((acc, p) => acc + (Number(p.total) || 0), 0),
    sms: packages.filter(p => p.type === 'sms').reduce((acc, p) => acc + (Number(p.total) || 0), 0),
  };

  const summaryPct = {
    internet: totalCapacity.internet > 0 ? Math.min(100, (totals.internet / totalCapacity.internet) * 100) : 0,
    voice: totalCapacity.voice > 0 ? Math.min(100, (totals.voice / totalCapacity.voice) * 100) : 0,
    sms: totalCapacity.sms > 0 ? Math.min(100, (totals.sms / totalCapacity.sms) * 100) : 0,
  };

  const formattedInternet = totals.internet >= 1024 
    ? { value: (totals.internet / 1024).toFixed(1), unit: 'GB' }
    : { value: totals.internet.toFixed(0), unit: 'MB' };

  return (
    <div className="space-y-8 pb-32">
      <header className="flex justify-between items-center">
        <h1 className="text-4xl font-black tracking-tight text-slate-900">{t('telecom')}</h1>
        <div className="flex gap-2">
          <button 
            onClick={handleSync}
            className="p-4 bg-blue-600 text-white rounded-2xl shadow-xl shadow-blue-200 hover:bg-blue-700 transition-all active:scale-95"
          >
            <RefreshCw size={24} />
          </button>
        </div>
      </header>

      {/* Airtime Balance Card */}
      <section className="bg-slate-900 p-8 rounded-[3rem] text-white shadow-2xl relative overflow-hidden">
        <div className="relative z-10 space-y-10">
          <div className="flex justify-between items-center">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-full bg-white/10 flex items-center justify-center text-blue-400">
                <Zap size={24} />
              </div>
              <h3 className="text-[10px] font-bold uppercase tracking-[0.2em] opacity-60">{t('telecomAssets')}</h3>
            </div>
            <div className="px-4 py-1.5 bg-emerald-500/20 text-emerald-400 rounded-full flex items-center gap-1.5 border border-emerald-500/20">
              <ShieldCheck size={14} />
              <span className="text-[10px] font-black uppercase tracking-widest">Verified</span>
            </div>
          </div>

          <div className="space-y-2">
            <p className="text-xs font-bold opacity-60 uppercase tracking-widest">{t('availableAirtime')}</p>
            <div className="flex items-baseline gap-3">
              <span className="text-2xl font-bold opacity-40">ETB</span>
              <h2 className="text-6xl font-black tracking-tight">
                {balance.toFixed(2)}
              </h2>
            </div>
          </div>

          {/* Quick Summary Row */}
          <div className="grid grid-cols-3 gap-6 pt-6 border-t border-white/10">
            <div className="space-y-3">
              <p className="text-[9px] font-black opacity-40 uppercase tracking-widest">{t('data')}</p>
              <p className="text-sm font-black tracking-tight">
                {formattedInternet.value} <span className="text-[10px] opacity-60">{formattedInternet.unit}</span>
              </p>
              <div className="h-1.5 w-full bg-white/10 rounded-full overflow-hidden">
                <div className="h-full bg-blue-500 rounded-full" style={{ width: `${summaryPct.internet.toFixed(0)}%` }} />
              </div>
            </div>
            <div className="space-y-3">
              <p className="text-[9px] font-black opacity-40 uppercase tracking-widest">{t('audio')}</p>
              <p className="text-sm font-black tracking-tight">
                {totals.voice} <span className="text-[10px] opacity-60">Min</span>
              </p>
              <div className="h-1.5 w-full bg-white/10 rounded-full overflow-hidden">
                <div className="h-full bg-emerald-500 rounded-full" style={{ width: `${summaryPct.voice.toFixed(0)}%` }} />
              </div>
            </div>
            <div className="space-y-3">
              <p className="text-[9px] font-black opacity-40 uppercase tracking-widest">{t('sms')}</p>
              <p className="text-sm font-black tracking-tight">
                {totals.sms} <span className="text-[10px] opacity-60">SMS</span>
              </p>
              <div className="h-1.5 w-full bg-white/10 rounded-full overflow-hidden">
                <div className="h-full bg-purple-500 rounded-full" style={{ width: `${summaryPct.sms.toFixed(0)}%` }} />
              </div>
            </div>
          </div>

          <div className="pt-4">
            <button 
              onClick={() => setShowRechargeModal(true)}
              className="w-full py-5 bg-blue-600 hover:bg-blue-700 rounded-2xl text-xs font-black uppercase tracking-widest transition-all shadow-2xl shadow-blue-500/40"
            >
              {t('recharge')}
            </button>
          </div>
        </div>

        {/* Decorative elements */}
        <div className="absolute -right-20 -top-20 w-64 h-64 bg-blue-600/20 rounded-full blur-[100px]" />
        <div className="absolute -left-20 -bottom-20 w-64 h-64 bg-purple-600/20 rounded-full blur-[100px]" />
      </section>

      {/* Active Packages List */}
      <section className="space-y-6">
        <div className="flex justify-between items-center px-2">
          <h3 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400">{t('activePackages')}</h3>
          <button className="text-[10px] font-black text-blue-600 uppercase tracking-widest flex items-center gap-1">
            <Info size={12} />
            DETAILS
          </button>
        </div>
        
        <div className="grid gap-4">
          {(packages || []).filter(p => p.type !== 'airtime').length > 0 ? (
            (packages || []).filter(p => p.type !== 'airtime').map((pkg) => (
              <PackageCard
                key={pkg.id}
                type={pkg.type}
                value={pkg.value}
                total={pkg.total}
                unit={pkg.unit}
                label={pkg.label}
                expiry={pkg.expiryDate}
                daysLeft={pkg.daysLeft}
                totalDays={pkg.totalDays}
                language={language}
              />
            ))
          ) : (
            <div className="bg-white p-12 rounded-[2.5rem] border border-dashed border-slate-200 flex flex-col items-center justify-center text-center space-y-4">
              <div className="w-16 h-16 rounded-full bg-slate-50 flex items-center justify-center text-slate-300">
                <Zap size={32} />
              </div>
              <div>
                <p className="text-sm font-bold text-slate-900">{t('noActivePackages') || 'No active packages'}</p>
                <p className="text-xs text-slate-400 mt-1">{t('syncToSeePackages') || 'Sync your balance to see your packages'}</p>
              </div>
            </div>
          )}
        </div>
      </section>


      {/* Recharge Modal */}
      <AnimatePresence>
        {showRechargeModal && (
          <div className="fixed inset-0 z-[100] flex items-end sm:items-center justify-center p-4">
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setShowRechargeModal(false)}
              className="absolute inset-0 bg-slate-900/60 backdrop-blur-sm"
            />
            <motion.div 
              initial={{ y: '100%' }}
              animate={{ y: 0 }}
              exit={{ y: '100%' }}
              className="relative w-full max-w-lg bg-white rounded-t-[3rem] sm:rounded-[3rem] p-8 shadow-2xl space-y-6"
            >
              <div className="flex justify-between items-start">
                <div className="space-y-1">
                  <h3 className="text-2xl font-black text-slate-900">{t('rechargeBalance')}</h3>
                  <p className="text-sm text-slate-500">{t('chooseRechargeMethod')}</p>
                </div>
                <button 
                  onClick={() => setShowRechargeModal(false)}
                  className="p-2 bg-slate-100 rounded-full text-slate-400 hover:text-slate-600"
                >
                  <X size={20} />
                </button>
              </div>


              <div className="space-y-4">
                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest ml-2">{t('voucherNumber')}</label>
                  <input 
                    type="tel"
                    value={voucherNumber}
                    onChange={(e) => {
                      const val = e.target.value.replace(/[^0-9]/g, '');
                      if (val.length <= 15) setVoucherNumber(val);
                    }}
                    placeholder={t('enterVoucher')}
                    className="w-full px-6 py-5 bg-slate-50 border-none rounded-[2rem] text-sm font-bold focus:ring-2 focus:ring-blue-500/20 transition-all"
                  />
                </div>
                <div className="p-4 bg-blue-50 rounded-2xl flex gap-3">
                  <Info size={18} className="text-blue-600 shrink-0" />
                  <p className="text-[10px] text-blue-700 font-medium leading-relaxed">
                    {t('ussdRechargeInfo')}
                  </p>
                </div>
              </div>

              <button 
                onClick={handleRecharge}
                disabled={!voucherNumber.trim()}
                className="w-full py-5 bg-blue-600 text-white rounded-[2rem] font-black uppercase tracking-widest flex items-center justify-center gap-2 hover:bg-blue-700 transition-all disabled:opacity-50 shadow-lg shadow-blue-200"
              >
                {t('rechargeViaUSSD')}
              </button>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
      
    </div>
  );
};
