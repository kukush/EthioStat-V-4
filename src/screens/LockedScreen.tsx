import React, { useState } from 'react';
import { Lock, Eye, EyeOff, Fingerprint } from 'lucide-react';
import { useSecurity } from '../components/BiometricProvider';

export const LockedScreen = ({ language }: { language: string }) => {
  const { unlockWithBiometric, unlockWithPin, hasPin, setPin, isPinEnabled, isBiometricEnabled } = useSecurity();
  const [pin, setPinInput] = useState('');
  const [showPin, setShowPin] = useState(false);
  const [error, setError] = useState('');

  const t = (key: string): string => {
    const strings: Record<string, Record<string, string>> = {
      en: {
        pinSetupTitle: "Set 4-Digit PIN",
        pinEnterTitle: "Enter 4-Digit PIN",
        pinRequirement: "PIN must be exactly 4 digits (numbers only).",
        invalidPinError: "Invalid PIN. Please try again.",
        unlock: "Unlock",
        setPin: "Set PIN",
        useBiometric: "Use Biometric",
      },
      am: {
        pinSetupTitle: "የ4-አሃዝ PIN ያዘጋጁ",
        pinEnterTitle: "የ4-አሃዝ PIN ያስገቡ",
        pinRequirement: "PIN በትክክል 4 አሃዝ መሆን አለበት (ቁጥሮች ብቻ)።",
        invalidPinError: "ልክ ያልሆነ PIN። እባክዎ እንደገና ይሞክሩ።",
        unlock: "ክፈት",
        setPin: "PIN አዘጋጅ",
        useBiometric: "ጣት አሻራ ተጠቀም",
      },
      om: {
        pinSetupTitle: "PIN Dijiitii 4 Haaressi",
        pinEnterTitle: "PIN Dijiitii 4 Galchi",
        pinRequirement: "PIN dijiitii 4 qofa ta'uu qaba (lakkoofsa qofa).",
        invalidPinError: "PIN dogoggora. Maaloo irra deebi'ii yaali.",
        unlock: "Bani",
        setPin: "PIN Haaressi",
        useBiometric: "Biometric Fayyadami",
      }
    };
    const lang = strings[language] ? language : 'en';
    return strings[lang][key] || strings['en'][key] || key;
  };

  const handleUnlock = () => {
    const digitsOnly = pin.replace(/\D/g, '');
    if (digitsOnly.length !== 4) {
      setError(t('pinRequirement'));
      return;
    }

    if (hasPin) {
      if (unlockWithPin(digitsOnly)) {
        setError('');
      } else {
        setError(t('invalidPinError'));
      }
    } else {
      setPin(digitsOnly);
      unlockWithPin(digitsOnly);
    }
  };

  return (
    <div className="flex flex-col items-center justify-center min-h-screen p-4 text-center bg-slate-950 text-white">
      <div className="mb-6 relative flex items-center justify-center">
        <img 
          src="/app-icon-512.png" 
          alt="EthioBalance Logo" 
          className="w-24 h-24 rounded-2xl object-cover shadow-lg border border-slate-800"
          referrerPolicy="no-referrer"
        />
        <div className="absolute -bottom-2 -right-2 bg-slate-900 border border-slate-700 rounded-full p-1.5 shadow-md">
          <Lock className="w-5 h-5 text-slate-300" />
        </div>
      </div>

      {isPinEnabled && !hasPin && (
        <>
          <h2 className="text-xl font-bold mb-1">
            {t('pinSetupTitle')}
          </h2>
          <p className="text-xs text-slate-400 mb-4 max-w-xs">
            {t('pinRequirement')}
          </p>
        </>
      )}

      {isPinEnabled && (
        <>
          <div className="relative w-full max-w-xs mb-4">
            <input
              type={showPin ? "text" : "password"}
              maxLength={4}
              value={pin}
              onChange={(e) => {
                const filtered = e.target.value.replace(/\D/g, '');
                setPinInput(filtered);
                setError('');
              }}
              placeholder="••••"
              className="w-full p-3.5 bg-slate-900 border border-slate-800 rounded-2xl text-center text-xl tracking-widest font-mono text-white focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition-all"
            />
            <button
              onClick={() => setShowPin(!showPin)}
              className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300 transition-colors"
            >
              {showPin ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
            </button>
          </div>

          {error && (
            <p className="text-xs text-rose-400 mb-4 font-semibold px-4 max-w-xs">
              {error}
            </p>
          )}

          <button
            onClick={handleUnlock}
            className="w-full max-w-xs py-3.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-2xl font-bold transition-all shadow-lg shadow-emerald-950/20 active:scale-95"
          >
            {hasPin ? t('unlock') : t('setPin')}
          </button>
        </>
      )}

      {isBiometricEnabled && (
        <button
          onClick={unlockWithBiometric}
          className={`${isPinEnabled ? 'mt-6' : ''} flex items-center gap-2 text-sm font-bold text-emerald-400 hover:text-emerald-300 transition-colors bg-emerald-500/10 px-4 py-2 rounded-full border border-emerald-500/20 active:scale-95`}
        >
          <Fingerprint className="w-4 h-4" />
          <span>{t('useBiometric')}</span>
        </button>
      )}
    </div>
  );
};
