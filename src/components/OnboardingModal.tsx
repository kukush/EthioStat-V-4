import React, { useState } from 'react';
import { Check, ChevronRight, Cpu, EyeOff, Hash, Layers, ShieldCheck, Smartphone, X } from 'lucide-react';
import { Language } from '../types';
import { t } from '../constants/translations';
import { APP_NAME, APP_DESCRIPTION } from '../constants/app';

interface OnboardingModalProps {
  isOpen: boolean;
  onClose: () => void;
  language: Language;
}

export const OnboardingModal: React.FC<OnboardingModalProps> = ({ isOpen, onClose, language }) => {
  const [step, setStep] = useState(0);

  if (!isOpen) return null;

  const steps = [
    {
      title: '100% Offline & Private Financial Manager',
      description: APP_DESCRIPTION,
      icon: <img src="/app-icon-512.png" alt={APP_NAME} className="w-12 h-12 rounded-2xl object-cover shadow-lg shadow-emerald-500/20" />,
    },
    {
      title: 'Smart SMS Parsing & Reconciliation',
      description:
        'Automatically extracts transaction amounts, fees, references, and ledger balances from Telebirr, CBE, Awash, Dashen, and 20+ Ethiopian banks.',
      icon: <Cpu className="w-10 h-10 text-purple-400" />,
    },
    {
      title: 'Ethio Telecom Resource Tracking',
      description:
        'Keep track of your Internet Data (GB), Voice Minutes, and SMS packs. Check balances with *804#, recharge scratch cards with *805*, or transfer airtime via *806*.',
      icon: <Smartphone className="w-10 h-10 text-blue-400" />,
    },
    {
      title: 'Multi-Lingual Experience',
      description:
        'Fully localized in English, Amharic (አማርኛ), and Afaan Oromoo with Ethiopian calendar dates and native banking terms.',
      icon: <Layers className="w-10 h-10 text-amber-400" />,
    },
  ];

  const handleNext = () => {
    if (step < steps.length - 1) {
      setStep(step + 1);
    } else {
      onClose();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-fadeIn">
      <div className="relative w-full max-w-md rounded-3xl bg-slate-900 border border-slate-700 shadow-2xl p-6 flex flex-col text-center">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-2 rounded-xl hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="mx-auto mb-4 p-4 rounded-3xl bg-slate-800/80 border border-slate-700">
          {steps[step].icon}
        </div>

        <h3 className="font-extrabold text-lg text-white mb-2">{steps[step].title}</h3>
        <p className="text-xs text-slate-300 leading-relaxed mb-6 px-2">{steps[step].description}</p>

        {/* Step Indicator dots */}
        <div className="flex items-center justify-center gap-1.5 mb-6">
          {steps.map((_, i) => (
            <span
              key={i}
              className={`h-2 rounded-full transition-all ${
                step === i ? 'w-6 bg-emerald-400' : 'w-2 bg-slate-700'
              }`}
            />
          ))}
        </div>

        <div className="flex items-center justify-between gap-3">
          {step > 0 ? (
            <button
              onClick={() => setStep(step - 1)}
              className="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 font-bold text-xs"
            >
              {t(language, 'back')}
            </button>
          ) : (
            <div />
          )}

          <button
            id="onboarding-next-btn"
            onClick={handleNext}
            className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 font-bold text-xs text-white shadow-lg shadow-emerald-950/50 flex items-center gap-2 ml-auto"
          >
            <span>{step === steps.length - 1 ? 'Get Started' : t(language, 'next')}</span>
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
};
