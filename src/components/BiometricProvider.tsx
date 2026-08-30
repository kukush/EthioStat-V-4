import React, { createContext, useState, useContext, ReactNode } from 'react';
import { BiometricService } from '../services/BiometricService';

interface SecurityContextType {
  isLocked: boolean;
  unlockWithBiometric: () => Promise<void>;
  unlockWithPin: (pin: string) => boolean;
  lock: () => void;
  hasPin: boolean;
  setPin: (pin: string) => void;
  isBiometricEnabled: boolean;
  setIsBiometricEnabled: (enabled: boolean) => void;
  isPinEnabled: boolean;
  setIsPinEnabled: (enabled: boolean) => void;
  isAutoSyncEnabled: boolean;
  setIsAutoSyncEnabled: (enabled: boolean) => void;
}

const SecurityContext = createContext<SecurityContextType | undefined>(undefined);

export const SecurityProvider = ({ children }: { children: ReactNode }) => {
  const [isLocked, setIsLocked] = useState(true);
  const [storedPin, setStoredPin] = useState<string | null>(localStorage.getItem('app_pin'));
  const [isBiometricEnabled, setIsBiometricEnabled] = useState<boolean>(localStorage.getItem('biometric_enabled') === 'true');
  const [isPinEnabled, setIsPinEnabled] = useState<boolean>(localStorage.getItem('pin_enabled') === 'true');
  const [isAutoSyncEnabled, setIsAutoSyncEnabled] = useState<boolean>(localStorage.getItem('auto_sync_enabled') !== 'false');

  const unlockWithBiometric = async () => {
    const success = await BiometricService.authenticate();
    if (success) {
      setIsLocked(false);
    }
  };

  const unlockWithPin = (pin: string) => {
    if (storedPin === pin) {
      setIsLocked(false);
      return true;
    }
    return false;
  };

  const lock = () => {
    setIsLocked(true);
  };

  const setPin = (pin: string) => {
    localStorage.setItem('app_pin', pin);
    setStoredPin(pin);
  };
  
  const setBiometricEnabled = (enabled: boolean) => {
      localStorage.setItem('biometric_enabled', enabled.toString());
      setIsBiometricEnabled(enabled);
  };
  
  const setPinEnabled = (enabled: boolean) => {
      localStorage.setItem('pin_enabled', enabled.toString());
      setIsPinEnabled(enabled);
  };
  
  const setAutoSyncEnabled = (enabled: boolean) => {
      localStorage.setItem('auto_sync_enabled', enabled.toString());
      setIsAutoSyncEnabled(enabled);
  };

  return (
    <SecurityContext.Provider value={{ isLocked, unlockWithBiometric, unlockWithPin, lock, hasPin: !!storedPin, setPin, isBiometricEnabled, setIsBiometricEnabled: setBiometricEnabled, isPinEnabled, setIsPinEnabled: setPinEnabled, isAutoSyncEnabled, setIsAutoSyncEnabled: setAutoSyncEnabled }}>
      {children}
    </SecurityContext.Provider>
  );
};

export const useSecurity = () => {
  const context = useContext(SecurityContext);
  if (!context) {
    throw new Error('useSecurity must be used within a SecurityProvider');
  }
  return context;
};
