import React, { useState } from 'react';
import confetti from 'canvas-confetti';
import { ArrowRightLeft, CheckCircle2, ChevronRight, Gift, Hash, PhoneCall, PlusCircle, RefreshCw, Send, Smartphone, X } from 'lucide-react';
import { Language, RecommendedBundle } from '../types';
import { formatCurrency, t } from '../constants/translations';
import { RECOMMENDED_BUNDLES } from '../constants/banks';

interface UssdModalProps {
  isOpen: boolean;
  onClose: () => void;
  language: Language;
  initialAction?: 'balance' | 'recharge' | 'transfer' | 'gift';
  onExecuteRecharge: (voucher: string) => void;
  onExecuteTransfer: (phone: string, amount: number) => void;
  onExecuteSync: () => void;
  onExecuteBuyBundle: (bundle: RecommendedBundle) => void;
}

export const UssdModal: React.FC<UssdModalProps> = ({
  isOpen,
  onClose,
  language,
  initialAction = 'balance',
  onExecuteRecharge,
  onExecuteTransfer,
  onExecuteSync,
  onExecuteBuyBundle,
}) => {
  const [activeTab, setActiveTab] = useState<'balance' | 'recharge' | 'transfer' | 'gift'>(initialAction);
  
  // Voucher Recharge state
  const [voucher, setVoucher] = useState('');
  const [voucherError, setVoucherError] = useState('');

  // Airtime Transfer state
  const [transferPhone, setTransferPhone] = useState('');
  const [transferAmount, setTransferAmount] = useState('25');
  const [transferError, setTransferError] = useState('');

  // USSD Output state
  const [isExecuting, setIsExecuting] = useState(false);
  const [executionResult, setExecutionResult] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleRunSync = () => {
    setIsExecuting(true);
    setTimeout(() => {
      onExecuteSync();
      setIsExecuting(false);
      setExecutionResult('USSD *804# request complete. Your balance and resources have been synced.');
      confetti({ particleCount: 40, spread: 60, origin: { y: 0.7 } });
    }, 1200);
  };

  const handleRunRecharge = (e: React.FormEvent) => {
    e.preventDefault();
    const clean = voucher.trim().replace(/\D/g, '');
    if (clean.length < 13 || clean.length > 16) {
      setVoucherError(t(language, 'invalidVoucher'));
      return;
    }
    setVoucherError('');
    setIsExecuting(true);
    setTimeout(() => {
      onExecuteRecharge(clean);
      setIsExecuting(false);
      setExecutionResult(`USSD *805*${clean}# completed! Account recharged.`);
      setVoucher('');
      confetti({ particleCount: 80, spread: 70, origin: { y: 0.6 } });
    }, 1200);
  };

  const handleRunTransfer = (e: React.FormEvent) => {
    e.preventDefault();
    const cleanPhone = transferPhone.trim().replace(/\s+/g, '');
    const amt = parseFloat(transferAmount);

    if (!cleanPhone.match(/^(?:09|07|\+2519|\+2517)\d{8}$/)) {
      setTransferError(t(language, 'invalidPhone') + ' (e.g. 0911223344)');
      return;
    }
    if (isNaN(amt) || amt < 5 || amt > 1000) {
      setTransferError('Amount must be between 5 and 1,000 ETB');
      return;
    }
    setTransferError('');
    setIsExecuting(true);
    setTimeout(() => {
      onExecuteTransfer(cleanPhone, amt);
      setIsExecuting(false);
      setExecutionResult(`Transferred ${amt} ETB to ${cleanPhone} via *806*.`);
      setTransferPhone('');
      confetti({ particleCount: 50, spread: 60, origin: { y: 0.6 } });
    }, 1200);
  };

  const handleBuyBundle = (bundle: RecommendedBundle) => {
    setIsExecuting(true);
    setTimeout(() => {
      onExecuteBuyBundle(bundle);
      setIsExecuting(false);
      setExecutionResult(`Successfully subscribed to ${bundle.title} for ${bundle.priceEtb} ETB!`);
      confetti({ particleCount: 70, spread: 80, origin: { y: 0.6 } });
    }, 1000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
      <div className="relative w-full max-w-lg rounded-3xl bg-slate-900 border border-slate-700 shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-slate-800 bg-slate-950/60">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
              <Hash className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-extrabold text-base text-white">USSD & Telecom Center</h3>
              <p className="text-xs text-slate-400">Ethio Telecom Quick USSD Services</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab Navigation */}
        <div className="flex items-center p-2 bg-slate-950/80 border-b border-slate-800 gap-1 overflow-x-auto text-xs font-bold">
          <button
            onClick={() => { setActiveTab('balance'); setExecutionResult(null); }}
            className={`flex items-center gap-1.5 px-3 py-2 rounded-xl transition-all shrink-0 ${
              activeTab === 'balance' ? 'bg-emerald-600 text-white' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>*804# Sync</span>
          </button>

          <button
            onClick={() => { setActiveTab('recharge'); setExecutionResult(null); }}
            className={`flex items-center gap-1.5 px-3 py-2 rounded-xl transition-all shrink-0 ${
              activeTab === 'recharge' ? 'bg-emerald-600 text-white' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <PlusCircle className="w-3.5 h-3.5" />
            <span>*805* Recharge</span>
          </button>

          <button
            onClick={() => { setActiveTab('transfer'); setExecutionResult(null); }}
            className={`flex items-center gap-1.5 px-3 py-2 rounded-xl transition-all shrink-0 ${
              activeTab === 'transfer' ? 'bg-emerald-600 text-white' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <ArrowRightLeft className="w-3.5 h-3.5" />
            <span>*806* Transfer</span>
          </button>

          <button
            onClick={() => { setActiveTab('gift'); setExecutionResult(null); }}
            className={`flex items-center gap-1.5 px-3 py-2 rounded-xl transition-all shrink-0 ${
              activeTab === 'gift' ? 'bg-emerald-600 text-white' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Gift className="w-3.5 h-3.5" />
            <span>*999# Bundles</span>
          </button>
        </div>

        {/* Content Body */}
        <div className="p-5 overflow-y-auto space-y-4">
          {/* Result Alert if any */}
          {executionResult && (
            <div className="p-3.5 rounded-2xl bg-emerald-950/40 border border-emerald-500/40 text-emerald-300 text-xs font-semibold flex items-center gap-2 animate-fadeIn">
              <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
              <span>{executionResult}</span>
            </div>
          )}

          {/* TAB 1: *804# Sync */}
          {activeTab === 'balance' && (
            <div className="space-y-4">
              <div className="p-4 rounded-2xl bg-slate-800/60 border border-slate-700/60 text-xs text-slate-300 space-y-2">
                <div className="font-bold text-slate-200 text-sm flex items-center gap-2">
                  <PhoneCall className="w-4 h-4 text-emerald-400" />
                  <span>Dial *804# (Balance & Package Query)</span>
                </div>
                <p className="text-slate-400 leading-relaxed">
                  {t(language, 'ussdSyncInfo')}
                </p>
              </div>

              <button
                id="btn-dial-804"
                onClick={handleRunSync}
                disabled={isExecuting}
                className="w-full py-3 px-4 rounded-2xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 font-bold text-sm text-white shadow-lg shadow-emerald-950/50 flex items-center justify-center gap-2 transition-all active:scale-[0.98] disabled:opacity-50"
              >
                <RefreshCw className={`w-4 h-4 ${isExecuting ? 'animate-spin' : ''}`} />
                <span>{isExecuting ? 'Dialing *804# & Parsing...' : 'Dial *804# Balance Sync'}</span>
              </button>
            </div>
          )}

          {/* TAB 2: *805* Recharge Voucher */}
          {activeTab === 'recharge' && (
            <form onSubmit={handleRunRecharge} className="space-y-4">
              <div className="p-4 rounded-2xl bg-slate-800/60 border border-slate-700/60 text-xs text-slate-300 space-y-2">
                <div className="font-bold text-slate-200 text-sm flex items-center gap-2">
                  <PlusCircle className="w-4 h-4 text-emerald-400" />
                  <span>Recharge via Scratch Card (*805*)</span>
                </div>
                <p className="text-slate-400">
                  {t(language, 'ussdRechargeInfo')}
                </p>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                  {t(language, 'voucherNumber')} (13-16 Digits)
                </label>
                <input
                  id="input-voucher-code"
                  type="text"
                  placeholder="e.g. 1234567890123"
                  value={voucher}
                  onChange={(e) => setVoucher(e.target.value)}
                  className="w-full px-4 py-3 rounded-2xl bg-slate-950 border border-slate-700 text-white font-mono text-base focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
                {voucherError && <p className="text-rose-400 text-xs font-bold mt-1">{voucherError}</p>}
              </div>

              {/* Sample Voucher Quick Fill */}
              <div className="flex items-center gap-2 flex-wrap text-xs text-slate-400">
                <span>Quick Test Vouchers:</span>
                <button
                  type="button"
                  onClick={() => setVoucher('9876543210123')}
                  className="px-2 py-0.5 rounded bg-slate-800 border border-slate-700 font-mono text-[11px] text-emerald-300 hover:bg-slate-700"
                >
                  50 ETB (9876...)
                </button>
                <button
                  type="button"
                  onClick={() => setVoucher('8765432109876')}
                  className="px-2 py-0.5 rounded bg-slate-800 border border-slate-700 font-mono text-[11px] text-emerald-300 hover:bg-slate-700"
                >
                  100 ETB (8765...)
                </button>
              </div>

              <button
                id="btn-dial-805"
                type="submit"
                disabled={isExecuting || !voucher}
                className="w-full py-3 px-4 rounded-2xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 font-bold text-sm text-white shadow-lg shadow-emerald-950/50 flex items-center justify-center gap-2 transition-all active:scale-[0.98] disabled:opacity-50"
              >
                <Smartphone className="w-4 h-4" />
                <span>{isExecuting ? 'Processing *805*...' : t(language, 'rechargeViaUSSD')}</span>
              </button>
            </form>
          )}

          {/* TAB 3: *806* Transfer Airtime */}
          {activeTab === 'transfer' && (
            <form onSubmit={handleRunTransfer} className="space-y-4">
              <div className="p-4 rounded-2xl bg-slate-800/60 border border-slate-700/60 text-xs text-slate-300 space-y-1">
                <div className="font-bold text-slate-200 text-sm flex items-center gap-2">
                  <ArrowRightLeft className="w-4 h-4 text-cyan-400" />
                  <span>Airtime P2P Transfer (*806*)</span>
                </div>
                <p className="text-slate-400">
                  {t(language, 'transferAirtimeInfo')}
                </p>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                  {t(language, 'recipientNumber')}
                </label>
                <input
                  id="input-transfer-phone"
                  type="text"
                  placeholder="0911223344"
                  value={transferPhone}
                  onChange={(e) => setTransferPhone(e.target.value)}
                  className="w-full px-4 py-3 rounded-2xl bg-slate-950 border border-slate-700 text-white font-mono text-base focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                  {t(language, 'amountEtb')}
                </label>
                <input
                  id="input-transfer-amount"
                  type="number"
                  placeholder="25"
                  value={transferAmount}
                  onChange={(e) => setTransferAmount(e.target.value)}
                  className="w-full px-4 py-3 rounded-2xl bg-slate-950 border border-slate-700 text-white font-mono text-base focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
                {transferError && <p className="text-rose-400 text-xs font-bold mt-1">{transferError}</p>}
              </div>

              <div className="flex items-center gap-2 flex-wrap">
                {[10, 25, 50, 100].map((preset) => (
                  <button
                    key={preset}
                    type="button"
                    onClick={() => setTransferAmount(preset.toString())}
                    className="px-3 py-1 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-xs font-bold text-slate-200"
                  >
                    {preset} ETB
                  </button>
                ))}
              </div>

              <button
                id="btn-dial-806"
                type="submit"
                disabled={isExecuting || !transferPhone}
                className="w-full py-3 px-4 rounded-2xl bg-gradient-to-r from-cyan-600 to-blue-600 hover:from-cyan-500 hover:to-blue-500 font-bold text-sm text-white shadow-lg shadow-cyan-950/50 flex items-center justify-center gap-2 transition-all active:scale-[0.98] disabled:opacity-50"
              >
                <Send className="w-4 h-4" />
                <span>{isExecuting ? 'Transferring...' : t(language, 'transferViaUSSD')}</span>
              </button>
            </form>
          )}

          {/* TAB 4: *999# Bundles & Gifting */}
          {activeTab === 'gift' && (
            <div className="space-y-3">
              <div className="text-xs text-slate-400 font-medium">
                Subscribe to top-rated Ethio Telecom packages or send them as gifts directly:
              </div>

              <div className="space-y-2">
                {RECOMMENDED_BUNDLES.map((bundle) => (
                  <div
                    key={bundle.id}
                    className="p-3.5 rounded-2xl bg-slate-800/80 border border-slate-700 flex items-center justify-between gap-3 hover:border-slate-600 transition-colors"
                  >
                    <div>
                      <div className="flex items-center gap-1.5">
                        <span className="font-bold text-sm text-white">{bundle.title}</span>
                        {bundle.popular && (
                          <span className="text-[10px] uppercase font-extrabold px-1.5 py-0.2 rounded bg-amber-500/20 text-amber-300 border border-amber-500/30">
                            Popular
                          </span>
                        )}
                      </div>
                      <div className="text-xs text-slate-400 font-mono mt-0.5">
                        Code: {bundle.ussdCode}
                      </div>
                    </div>

                    <button
                      onClick={() => handleBuyBundle(bundle)}
                      disabled={isExecuting}
                      className="px-3.5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 font-bold text-xs text-white shrink-0 shadow-md shadow-emerald-950/40 active:scale-95 transition-all"
                    >
                      {bundle.priceEtb} ETB
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
