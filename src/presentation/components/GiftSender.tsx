import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Phone, Gift, ChevronRight, CheckCircle2, ArrowLeft, Zap, Globe, MessageSquare, HandHelping, X, Check } from 'lucide-react';
import { RecommendedBundle, Language, GiftRequest } from '@/types';
import { useTranslation } from '@/translations';
import { cn } from '@/lib/utils';
import { PhoneInput, formatEthiopianPhone } from '@/presentation/components/PhoneInput';

interface GiftSenderProps {
  recommendedBundles: RecommendedBundle[];
  language: Language;
  balance: number;
  giftRequests: GiftRequest[];
  dispatch: any;
}

type Step = 'idle' | 'start' | 'phone' | 'package' | 'confirm' | 'success';
type Mode = 'send' | 'request';

export const GiftSender: React.FC<GiftSenderProps> = ({ 
  recommendedBundles, language, balance, giftRequests, dispatch 
}) => {
  const t = useTranslation(language);
  const [step, setStep] = useState<Step>('idle');
  const [mode, setMode] = useState<Mode>('send');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [selectedBundle, setSelectedBundle] = useState<RecommendedBundle | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const handleNext = () => {
    if (step === 'phone' && phoneNumber.length >= 9) {
      setStep('package');
    } else if (step === 'package' && selectedBundle) {
      setStep('confirm');
    }
  };

  const handleBack = () => {
    if (step === 'start') setStep('idle');
    if (step === 'phone') setStep('start');
    if (step === 'package') setStep('phone');
    if (step === 'confirm') setStep('package');
  };

  const handleConfirm = () => {
    setIsProcessing(true);
    setTimeout(() => {
      if (mode === 'send') {
        dispatch({ type: 'UPDATE_BALANCE', balance: balance - (selectedBundle?.price || 0) });
      }
      setIsProcessing(false);
      setStep('success');
    }, 1500);
  };

  const handleAcceptRequest = (request: GiftRequest) => {
    const bundle = recommendedBundles.find(b => b.id === request.bundleId);
    if (bundle) {
      setMode('send');
      setPhoneNumber(request.senderNumber);
      setSelectedBundle(bundle);
      setStep('confirm');
      dispatch({ type: 'REMOVE_GIFT_REQUEST', id: request.id });
    }
  };

  const handleRejectRequest = (id: string) => {
    dispatch({ type: 'REMOVE_GIFT_REQUEST', id });
  };

  const reset = () => {
    setStep('idle');
    setPhoneNumber('');
    setSelectedBundle(null);
  };

  const startFlow = (m: Mode) => {
    setMode(m);
    setStep('phone');
  };

  const pendingRequests = (giftRequests || []).filter(r => r.status === 'pending');

  return (
    <div className="bg-white rounded-[2rem] border border-slate-100 shadow-sm overflow-hidden">
      <div className="px-5 py-3 border-b border-slate-50 flex items-center justify-between bg-slate-50/50">
        <div className="flex items-center gap-2">
          <div className="relative">
            <Gift size={14} className="text-purple-600" />
            {pendingRequests.length > 0 && step === 'idle' && (
              <span className="absolute -top-1.5 -right-1.5 w-3.5 h-3.5 bg-red-500 text-white text-[7px] font-black rounded-full flex items-center justify-center border border-white">
                {pendingRequests.length}
              </span>
            )}
          </div>
          <h3 className="text-[9px] font-black text-slate-900 uppercase tracking-widest">
            {step === 'idle' || step === 'start' ? t('gift') : (mode === 'send' ? t('sendGift') : t('requestGift'))}
          </h3>
        </div>
        {step !== 'idle' && step !== 'success' && (
          <button onClick={handleBack} className="p-1 text-slate-400 hover:text-slate-600">
            <ArrowLeft size={14} />
          </button>
        )}
      </div>

      <div className="p-5">
        <AnimatePresence mode="wait">
          {step === 'idle' && (
            <motion.div
              key="idle"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
            >
              <button
                onClick={() => setStep('start')}
                className={cn(
                  "w-full py-3 rounded-xl text-[9px] font-black uppercase tracking-widest flex items-center justify-center gap-2 transition-all group",
                  pendingRequests.length > 0 
                    ? "bg-purple-600 text-white shadow-lg shadow-purple-200 animate-pulse" 
                    : "bg-purple-50 text-purple-600 hover:bg-purple-100"
                )}
              >
                <Gift size={14} className={cn("transition-transform", pendingRequests.length > 0 ? "scale-110" : "group-hover:scale-110")} />
                {pendingRequests.length > 0 ? `${t('pendingRequests')} (${pendingRequests.length})` : t('manageGift')}
              </button>
            </motion.div>
          )}

          {step === 'start' && (
            <motion.div
              key="start"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="space-y-4"
            >
              <div className="flex items-center justify-between bg-slate-50 p-2 rounded-xl">
                <div className="flex gap-1">
                  <button
                    onClick={() => startFlow('send')}
                    className="px-4 py-2 bg-purple-600 text-white rounded-lg text-[9px] font-black uppercase tracking-widest hover:bg-purple-700 transition-all"
                  >
                    {t('sendGift')}
                  </button>
                  <button
                    onClick={() => startFlow('request')}
                    className="px-4 py-2 bg-white border border-slate-200 text-slate-600 rounded-lg text-[9px] font-black uppercase tracking-widest hover:bg-slate-50 transition-all"
                  >
                    {t('requestGift')}
                  </button>
                </div>
                <ChevronRight size={14} className="text-slate-300" />
              </div>

              {pendingRequests.length > 0 && (
                <div className="space-y-2 animate-in fade-in slide-in-from-top-2 duration-500">
                  <div className="flex items-center justify-between px-1">
                    <p className="text-[8px] font-black text-purple-600 uppercase tracking-widest flex items-center gap-1">
                      <span className="w-1 h-1 bg-purple-600 rounded-full animate-pulse" />
                      {t('pendingRequests')}
                    </p>
                    <span className="px-1.5 py-0.5 bg-purple-100 text-purple-700 rounded-full text-[7px] font-black">
                      {pendingRequests.length}
                    </span>
                  </div>
                  <div className="space-y-2">
                    {pendingRequests.map((request) => {
                      const bundle = recommendedBundles.find(b => b.id === request.bundleId);
                      return (
                        <div key={request.id} className="flex items-center justify-between p-3 bg-purple-50 rounded-xl border border-purple-100">
                          <div className="flex items-center gap-2">
                            <div className="w-8 h-8 rounded-lg bg-white flex items-center justify-center text-purple-600 shadow-sm">
                              <HandHelping size={14} />
                            </div>
                            <div>
                              <p className="text-[9px] font-black text-slate-900">{request.senderNumber}</p>
                              <p className="text-[8px] text-slate-500 font-bold">{bundle?.label}</p>
                            </div>
                          </div>
                          <div className="flex gap-1">
                            <button
                              onClick={() => handleRejectRequest(request.id)}
                              className="p-1.5 bg-white text-slate-400 rounded-lg hover:text-red-500 transition-colors shadow-sm"
                            >
                              <X size={14} />
                            </button>
                            <button
                              onClick={() => handleAcceptRequest(request)}
                              className="p-1.5 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors shadow-sm"
                            >
                              <Check size={14} />
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </motion.div>
          )}

          {step === 'phone' && (
            <motion.div
              key="phone"
              initial={{ opacity: 0, x: 10 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -10 }}
              className="space-y-3"
            >
              <PhoneInput
                value={phoneNumber}
                onChange={setPhoneNumber}
                placeholder={mode === 'send' ? '9XX XXX XXXX' : '9XX XXX XXXX'}
              />
              <button
                onClick={handleNext}
                disabled={phoneNumber.length < 9}
                className="w-full py-3 bg-slate-900 text-white rounded-xl font-black uppercase tracking-widest flex items-center justify-center gap-2 hover:bg-slate-800 transition-all disabled:opacity-50 text-[9px]"
              >
                {t('next')}
                <ChevronRight size={14} />
              </button>
            </motion.div>
          )}

          {step === 'package' && (
            <motion.div
              key="package"
              initial={{ opacity: 0, x: 10 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -10 }}
              className="space-y-3"
            >
              <div className="grid gap-2 max-h-[180px] overflow-y-auto pr-1 no-scrollbar">
                {recommendedBundles.map((bundle) => (
                  <button
                    key={bundle.id}
                    onClick={() => setSelectedBundle(bundle)}
                    className={cn(
                      "p-2.5 rounded-xl border text-left transition-all flex items-center justify-between",
                      selectedBundle?.id === bundle.id
                        ? "border-purple-500 bg-purple-50"
                        : "border-slate-50 hover:border-purple-100"
                    )}
                  >
                    <div className="flex items-center gap-2">
                      <div className={cn(
                        "w-7 h-7 rounded-lg flex items-center justify-center",
                        bundle.type === 'internet' ? "bg-blue-100 text-blue-600" :
                        bundle.type === 'voice' ? "bg-emerald-100 text-emerald-600" : "bg-purple-100 text-purple-600"
                      )}>
                        {bundle.type === 'internet' ? <Globe size={14} /> : bundle.type === 'voice' ? <Phone size={14} /> : <MessageSquare size={14} />}
                      </div>
                      <div>
                        <p className="text-[10px] font-black text-slate-900">{bundle.label}</p>
                        <p className="text-[8px] text-slate-500 font-bold">{bundle.price} ETB</p>
                      </div>
                    </div>
                    {selectedBundle?.id === bundle.id && (
                      <CheckCircle2 size={14} className="text-purple-500" />
                    )}
                  </button>
                ))}
              </div>
              <button
                onClick={handleNext}
                disabled={!selectedBundle}
                className="w-full py-3 bg-slate-900 text-white rounded-xl font-black uppercase tracking-widest flex items-center justify-center gap-2 hover:bg-slate-800 transition-all disabled:opacity-50 text-[9px]"
              >
                {t('next')}
                <ChevronRight size={14} />
              </button>
            </motion.div>
          )}

          {step === 'confirm' && selectedBundle && (
            <motion.div
              key="confirm"
              initial={{ opacity: 0, scale: 0.98 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.98 }}
              className="space-y-3"
            >
              <div className="p-3 bg-slate-50 rounded-xl space-y-2">
                <div className="flex justify-between items-center pb-2 border-b border-slate-200">
                  <span className="text-[8px] font-black text-slate-400 uppercase tracking-widest">
                    {mode === 'send' ? t('confirmGift') : t('requestGift')}
                  </span>
                  <span className="text-[10px] font-black text-slate-900">{selectedBundle.label}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-[8px] font-bold text-slate-400 uppercase tracking-widest">
                    {mode === 'send' ? t('recipientNumber') : t('senderNumber')}
                  </span>
                  <span className="text-[10px] font-black text-slate-900">{phoneNumber}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-[8px] font-bold text-slate-400 uppercase tracking-widest">{t('amount')}</span>
                  <span className="text-[10px] font-black text-slate-900">{selectedBundle.price} ETB</span>
                </div>
              </div>

              <div className="flex gap-2">
                <button
                  onClick={handleConfirm}
                  disabled={isProcessing || (mode === 'send' && balance < selectedBundle.price)}
                  className="w-full py-3 bg-purple-600 text-white rounded-xl font-black uppercase tracking-widest flex items-center justify-center gap-2 hover:bg-purple-700 transition-all disabled:opacity-50 text-[9px]"
                >
                  {isProcessing ? (
                    <div className="w-3 h-3 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  ) : (
                    <>
                      <Zap size={14} />
                      {t('confirm')}
                    </>
                  )}
                </button>
              </div>
              {mode === 'send' && balance < selectedBundle.price && (
                <p className="text-[8px] text-red-500 font-bold text-center uppercase tracking-widest">
                  Insufficient Balance
                </p>
              )}
            </motion.div>
          )}

          {step === 'success' && (
            <motion.div
              key="success"
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              className="text-center space-y-3 py-1"
            >
              <div className="w-12 h-12 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto">
                <CheckCircle2 size={24} />
              </div>
              <div className="space-y-1">
                <h4 className="text-xs font-black text-slate-900">
                  {mode === 'send' ? t('giftSentSuccess') : t('requestSentSuccess')}
                </h4>
                <p className="text-[10px] text-slate-500">
                  {mode === 'send' ? t('giftSentTo') : t('requestSentTo')} <span className="font-bold text-slate-900">{phoneNumber}</span>
                </p>
              </div>
              <button
                onClick={reset}
                className="w-full py-3 bg-slate-900 text-white rounded-xl font-black uppercase tracking-widest hover:bg-slate-800 transition-all text-[9px]"
              >
                Done
              </button>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
};
