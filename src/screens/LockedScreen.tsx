import React, { useState } from 'react';
import { Lock } from 'lucide-react';
import { useSecurity } from '../components/BiometricProvider';

export const LockedScreen = ({ language }: { language: string }) => {
  const { unlockWithBiometric, unlockWithPin, hasPin, setPin } = useSecurity();
  const [pin, setPinInput] = useState('');
  const [error, setError] = useState(false);

  const handleUnlock = () => {
    if (hasPin) {
      if (!unlockWithPin(pin)) {
        setError(true);
      }
    } else {
      setPin(pin);
      // Immediately unlock after setting PIN for the first time
      unlockWithPin(pin);
    }
  };

  return (
    <div className="flex flex-col items-center justify-center h-screen p-4 text-center">
      <div className="mb-6 relative flex items-center justify-center">
        <img 
          src="/app-icon-512.png" 
          alt="EthioBalance Logo" 
          className="w-24 h-24 rounded-2xl object-cover"
          referrerPolicy="no-referrer"
        />
        <div className="absolute -bottom-2 -right-2 bg-white rounded-full p-1 border border-slate-300">
          <Lock className="w-6 h-6 text-slate-700" />
        </div>
      </div>
      
      <input
        type="password"
        maxLength={4}
        value={pin}
        onChange={(e) => {setPinInput(e.target.value); setError(false);}}
        placeholder={hasPin ? "Enter PIN" : "Set PIN"}
        className="mb-4 p-3 border rounded-lg text-center"
      />

      <button
        onClick={handleUnlock}
        className="px-6 py-3 bg-blue-600 text-white rounded-lg font-semibold hover:bg-blue-700 transition mb-4"
      >
        {hasPin ? (language === 'am' ? 'ክፈት' : 'Unlock') : (language === 'am' ? 'PIN አዘጋጅ' : 'Set PIN')}
      </button>

      {error && <p className="text-red-500 mb-4">Invalid PIN</p>}
      
      <button
        onClick={unlockWithBiometric}
        className="text-blue-600 underline"
      >
        {language === 'am' ? 'ጣት አሻራ ተጠቀም' : 'Use Biometric'}
      </button>
    </div>
  );
};
