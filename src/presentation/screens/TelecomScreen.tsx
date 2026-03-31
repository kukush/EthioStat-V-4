import React, { useState } from 'react';
import { PackageCard } from '@/presentation/components/PackageCard';
import { GiftSender } from '@/presentation/components/GiftSender';
import { TelecomPackage, RecommendedBundle, Language, GiftRequest, PackageType } from '@/types';
import { motion, AnimatePresence } from 'framer-motion';
import { RefreshCw, Phone, Globe, Zap, MessageSquare, X, Send, ShieldCheck, Info, Plus, Check } from 'lucide-react';
import { useTranslation } from '@/translations';
import { cn } from '@/lib/utils';
import { PhoneInput, normalizePhone } from '@/presentation/components/PhoneInput';
import { useNativeBridge } from '@/presentation/hooks/useNativeBridge';

interface TelecomScreenProps {
  packages: TelecomPackage[];
  recommendedBundles: RecommendedBundle[];
  balance: number;
  language: Language;
  giftRequests: GiftRequest[];
  dispatch: any;
}

export const TelecomScreen: React.FC<TelecomScreenProps> = ({ 
  packages, recommendedBundles, balance, language, giftRequests, dispatch 
}) => {
  const [showSyncModal, setShowSyncModal] = useState(false);
  const [showRechargeModal, setShowRechargeModal] = useState(false);
  const [showTransferModal, setShowTransferModal] = useState(false);
  const [syncMethod, setSyncMethod] = useState<'ussd' | 'sms'>('ussd');
  const [transferMethod, setTransferMethod] = useState<'ussd' | 'telebirr'>('ussd');
  const { rechargeSelf, transferAirtime } = useNativeBridge();
  const [recipientNumber, setRecipientNumber] = useState('');
  const [transferAmount, setTransferAmount] = useState('');
  const [smsText, setSmsText] = useState('');
  const [senderId, setSenderId] = useState('');
  const [voucherNumber, setVoucherNumber] = useState('');
  const [rechargeMethod, setRechargeMethod] = useState<'ussd' | 'telebirr'>('ussd');
  const t = useTranslation(language);

  const handleSync = () => {
    if (syncMethod === 'sms' && smsText.trim()) {
      dispatch({ type: 'PARSE_SMS', text: smsText, senderId: senderId || undefined });
      setSmsText('');
      setSenderId('');
      setShowSyncModal(false);
    } else if (syncMethod === 'ussd') {
      // Simulate USSD sync
      dispatch({ type: 'SYNC_USSD' });
      setShowSyncModal(false);
    }
  };

  const handleBalanceTransfer = () => {
    if (transferMethod === 'ussd') {
      const amount = parseFloat(transferAmount);
      if (isNaN(amount) || amount < 5 || amount > 1000) {
        alert(t('invalidTransferAmount'));
        return;
      }

      if (balance - amount < 5) {
        alert(t('insufficientBalanceForTransfer'));
        return;
      }

      // Dial *806*RecipientNumber*Amount# (normalize phone number)
      const normalizedPhone = normalizePhone(recipientNumber).replace('+251', '');
      const ussdCode = `*806*${normalizedPhone}*${amount}#`;
      dispatch({ type: 'DIAL_USSD', code: ussdCode });
    } else {
      // Telebirr transfer - try to open app with fallback
      handleTelebirrRedirect();
    }

    setShowTransferModal(false);
    setRecipientNumber('');
    setTransferAmount('');
  };

  const handleTelebirrRedirect = () => {
    // Try to open Telebirr app
    const telebirrUrl = 'telebirr://';
    const fallbackUrl = 'https://play.google.com/store/apps/details?id=com.ethiotelecom.telebirr';
    
    // Create a hidden iframe to test if the app opens
    const iframe = document.createElement('iframe');
    iframe.style.display = 'none';
    iframe.src = telebirrUrl;
    document.body.appendChild(iframe);
    
    // Set a timeout to show fallback message if app doesn't open
    const timeout = setTimeout(() => {
      document.body.removeChild(iframe);
      alert('Telebirr app not found on your device. Please install Telebirr from the app store to continue with mobile transfers.');
    }, 2000);
    
    // Clean up if user returns quickly (app opened successfully)
    window.addEventListener('blur', () => {
      clearTimeout(timeout);
      setTimeout(() => {
        if (document.body.contains(iframe)) {
          document.body.removeChild(iframe);
        }
      }, 1000);
    }, { once: true });
  };

  const handleRecharge = () => {
    if (rechargeMethod === 'ussd') {
      const cleanVoucher = voucherNumber.replace(/[^0-9]/g, '');
      // EthioTelecom vouchers are 13 or 14 digits. We only dial if it looks like a valid voucher.
      if (cleanVoucher.length < 13) {
        alert(t('invalidVoucher') || "Voucher must be at least 13 digits.");
        return;
      }
      rechargeSelf(cleanVoucher);
    } else if (rechargeMethod === 'telebirr') {
      handleTelebirrRedirect();
    }

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
            onClick={() => setShowSyncModal(true)}
            className="p-4 bg-white text-slate-900 rounded-2xl shadow-xl shadow-slate-200 hover:bg-slate-50 transition-all active:scale-95 border border-slate-100"
          >
            <RefreshCw size={24} />
          </button>
          <button 
            onClick={() => setShowSyncModal(true)}
            className="p-4 bg-blue-600 text-white rounded-2xl shadow-xl shadow-blue-200 hover:bg-blue-700 transition-all active:scale-95"
          >
            <MessageSquare size={24} />
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

          <div className="grid grid-cols-2 gap-4 pt-4">
            <button 
              onClick={() => setShowTransferModal(true)}
              className="flex-1 py-5 bg-white/10 hover:bg-white/20 rounded-2xl text-xs font-bold uppercase tracking-widest transition-all flex items-center justify-center gap-3"
            >
              <Send size={18} />
              {t('transfer')}
            </button>
            <button 
              onClick={() => setShowRechargeModal(true)}
              className="flex-1 py-5 bg-blue-600 hover:bg-blue-700 rounded-2xl text-xs font-black uppercase tracking-widest transition-all shadow-2xl shadow-blue-500/40"
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

      {/* Gift Sender */}
      <GiftSender 
        recommendedBundles={recommendedBundles} 
        language={language} 
        balance={balance} 
        giftRequests={giftRequests}
        dispatch={dispatch}
      />

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

              <div className="flex gap-2">
                <button 
                  onClick={() => setRechargeMethod('ussd')}
                  className={cn(
                    "flex-1 py-4 rounded-2xl text-xs font-bold transition-all border",
                    rechargeMethod === 'ussd' ? "bg-slate-900 text-white border-slate-900 shadow-lg" : "bg-white text-slate-600 border-slate-100"
                  )}
                >
                  {t('ussdVoucher')}
                </button>
                <button 
                  onClick={() => setRechargeMethod('telebirr')}
                  className={cn(
                    "flex-1 py-4 rounded-2xl text-xs font-bold transition-all border",
                    rechargeMethod === 'telebirr' ? "bg-slate-900 text-white border-slate-900 shadow-lg" : "bg-white text-slate-600 border-slate-100"
                  )}
                >
                  Telebirr
                </button>
              </div>

              {rechargeMethod === 'ussd' ? (
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
              ) : (
                <div className="space-y-4">
                  <div className="p-4 bg-blue-50 rounded-2xl flex gap-3">
                    <Zap size={24} className="text-blue-600 shrink-0" />
                    <p className="text-[10px] text-blue-700 font-medium leading-relaxed">
                      {t('telebirrRechargeInfo')}
                    </p>
                  </div>
                </div>
              )}

              <button 
                onClick={handleRecharge}
                disabled={rechargeMethod === 'ussd' && !voucherNumber.trim()}
                className="w-full py-5 bg-blue-600 text-white rounded-[2rem] font-black uppercase tracking-widest flex items-center justify-center gap-2 hover:bg-blue-700 transition-all disabled:opacity-50 shadow-lg shadow-blue-200"
              >
                {rechargeMethod === 'ussd' ? t('rechargeViaUSSD') : t('openTelebirr')}
              </button>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
      
      {/* Sync Modal */}
      <AnimatePresence>
        {showSyncModal && (
          <div className="fixed inset-0 z-[100] flex items-end sm:items-center justify-center p-4">
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setShowSyncModal(false)}
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
                  <h3 className="text-2xl font-black text-slate-900">{t('syncBalance')}</h3>
                  <p className="text-sm text-slate-500">Choose your preferred sync method</p>
                </div>
                <button 
                  onClick={() => setShowSyncModal(false)}
                  className="p-2 bg-slate-100 rounded-full text-slate-400 hover:text-slate-600"
                >
                  <X size={20} />
                </button>
              </div>

              <div className="flex gap-2">
                <button 
                  onClick={() => setSyncMethod('ussd')}
                  className={cn(
                    "flex-1 py-4 rounded-2xl text-xs font-bold transition-all border",
                    syncMethod === 'ussd' ? "bg-slate-900 text-white border-slate-900 shadow-lg" : "bg-white text-slate-600 border-slate-100"
                  )}
                >
                  {t('ussdDefault')}
                </button>
                <button 
                  onClick={() => setSyncMethod('sms')}
                  className={cn(
                    "flex-1 py-4 rounded-2xl text-xs font-bold transition-all border",
                    syncMethod === 'sms' ? "bg-slate-900 text-white border-slate-900 shadow-lg" : "bg-white text-slate-600 border-slate-100"
                  )}
                >
                  {t('smsOption')}
                </button>
              </div>
              
              {syncMethod === 'ussd' ? (
                <div className="space-y-4">
                  <div className="p-4 bg-blue-50 rounded-2xl flex gap-3">
                    <Zap size={24} className="text-blue-600 shrink-0" />
                    <p className="text-[10px] text-blue-700 font-medium leading-relaxed">
                      {t('ussdSyncInfo')}
                    </p>
                  </div>
                </div>
              ) : (
                <div className="space-y-4">
                  <div className="space-y-2">
                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest ml-2">{t('senderIdLabel')}</label>
                    <input 
                      type="text"
                      value={senderId}
                      onChange={(e) => setSenderId(e.target.value)}
                      placeholder="e.g., CBE, Telebirr, *847#"
                      className="w-full px-6 py-4 bg-slate-50 border-none rounded-2xl text-sm font-bold focus:ring-2 focus:ring-blue-500/20 transition-all"
                    />
                  </div>
                  <div className="space-y-2">
                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest ml-2">{t('smsContentLabel')}</label>
                    <textarea 
                      value={smsText}
                      onChange={(e) => setSmsText(e.target.value)}
                      placeholder={t('smsContentPlaceholder')}
                      className="w-full h-32 p-6 bg-slate-50 border-none rounded-[2rem] text-sm font-medium focus:ring-2 focus:ring-blue-500/20 transition-all resize-none"
                    />
                  </div>
                </div>
              )}
              
              <button 
                onClick={handleSync}
                disabled={syncMethod === 'sms' && !smsText.trim()}
                className="w-full py-5 bg-blue-600 text-white rounded-[2rem] font-black uppercase tracking-widest flex items-center justify-center gap-2 hover:bg-blue-700 transition-all disabled:opacity-50 disabled:grayscale shadow-lg shadow-blue-200"
              >
                {syncMethod === 'ussd' ? <Zap size={18} /> : <Send size={18} />}
                {syncMethod === 'ussd' ? t('syncViaUSSD') : t('processSMS')}
              </button>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
      {/* Balance Transfer Modal */}
      <AnimatePresence>
        {showTransferModal && (
          <div className="fixed inset-0 z-[100] flex items-end sm:items-center justify-center p-4">
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setShowTransferModal(false)}
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
                  <h3 className="text-2xl font-black text-slate-900">{t('sendAirtime')}</h3>
                  <p className="text-sm text-slate-500">{t('transferAirtimeInfo')}</p>
                </div>
                <button 
                  onClick={() => setShowTransferModal(false)}
                  className="p-2 bg-slate-100 rounded-full text-slate-400 hover:text-slate-600"
                >
                  <X size={20} />
                </button>
              </div>

              <div className="flex gap-2">
                <button 
                  onClick={() => setTransferMethod('ussd')}
                  className={cn(
                    "flex-1 py-4 rounded-2xl text-xs font-bold transition-all border",
                    transferMethod === 'ussd' ? "bg-slate-900 text-white border-slate-900 shadow-lg" : "bg-white text-slate-600 border-slate-100"
                  )}
                >
                  {t('ussdCode')}
                </button>
                <button 
                  onClick={() => setTransferMethod('telebirr')}
                  className={cn(
                    "flex-1 py-4 rounded-2xl text-xs font-bold transition-all border",
                    transferMethod === 'telebirr' ? "bg-slate-900 text-white border-slate-900 shadow-lg" : "bg-white text-slate-600 border-slate-100"
                  )}
                >
                  Telebirr
                </button>
              </div>

              {transferMethod === 'ussd' ? (
                <div className="space-y-4">
                  <div className="space-y-2">
                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest ml-2">{t('recipientNumber')}</label>
                    <PhoneInput
                      value={recipientNumber}
                      onChange={setRecipientNumber}
                      placeholder="9XX XXX XXXX"
                    />
                  </div>
                  <div className="space-y-2">
                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest ml-2">{t('amountEtb')}</label>
                    <input 
                      type="number"
                      value={transferAmount}
                      onChange={(e) => setTransferAmount(e.target.value)}
                      placeholder="5 - 1000"
                      className="w-full px-6 py-4 bg-slate-50 border-none rounded-2xl text-sm font-bold focus:ring-2 focus:ring-blue-500/20 transition-all"
                    />
                  </div>
                  <div className="p-4 bg-slate-50 rounded-2xl space-y-2">
                    <div className="flex justify-between text-[10px] font-bold uppercase tracking-widest">
                      <span className="text-slate-400">{t('minTransfer')}</span>
                      <span className="text-slate-900">5 ETB</span>
                    </div>
                    <div className="flex justify-between text-[10px] font-bold uppercase tracking-widest">
                      <span className="text-slate-400">{t('maxTransfer')}</span>
                      <span className="text-slate-900">1000 ETB</span>
                    </div>
                    <div className="flex justify-between text-[10px] font-bold uppercase tracking-widest">
                      <span className="text-slate-400">{t('minBalanceAfter')}</span>
                      <span className="text-slate-900">5 ETB</span>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="p-6 bg-blue-50 rounded-[2rem] border border-blue-100 space-y-4">
                  <div className="w-12 h-12 rounded-2xl bg-blue-600 flex items-center justify-center text-white shadow-lg shadow-blue-100">
                    <Info size={24} />
                  </div>
                  <p className="text-sm font-bold text-slate-900 leading-relaxed">
                    {t('telebirrRechargeInfo')}
                  </p>
                </div>
              )}

              <button 
                onClick={handleBalanceTransfer}
                disabled={transferMethod === 'ussd' && (!recipientNumber.trim() || !transferAmount.trim())}
                className="w-full py-5 bg-blue-600 text-white rounded-[2rem] font-black uppercase tracking-widest flex items-center justify-center gap-2 hover:bg-blue-700 transition-all disabled:opacity-50 shadow-lg shadow-blue-200"
              >
                {transferMethod === 'ussd' ? t('transferViaUSSD') : t('openTelebirr')}
              </button>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
};
