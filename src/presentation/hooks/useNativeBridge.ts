import { useEffect } from 'react';
import { Capacitor } from '@capacitor/core';
import SmsMonitor from '../../data/smsMonitorPlugin';

export const useNativeBridge = () => {
  useEffect(() => {
    // Start native monitoring without JS parsing fallback
    if (Capacitor.isNativePlatform()) {
      SmsMonitor.startMonitoring();
    }
  }, []);

  const transferAirtime = (recipient: string, amount: number) => {
    const code = `*806*${recipient}*${amount}#`;
    window.location.href = `tel:${encodeURIComponent(code)}`;
  };

  const rechargeForOther = (voucher: string, recipient: string) => {
    const code = `*805*${voucher}*${recipient}#`;
    window.location.href = `tel:${encodeURIComponent(code)}`;
  };

  const rechargeSelf = (voucher: string) => {
    const code = `*805*${voucher}#`;
    window.location.href = `tel:${encodeURIComponent(code)}`;
  };

  const giftPackage = (sequence: string, recipient: string) => {
    // Android MMI sequence format isn't universally identical, but standard USSD can be dialed.
    // e.g., *999*1*1*2*recipient# (simulated if telecom supports direct nesting)
    // Or standard Ethio Gebeta *999# which requires accessibility service to automate the sequence.
    // Here we provide a direct string if supported or just the entry point
    const code = `*999#`; // Fallback manual entry
    window.location.href = `tel:${encodeURIComponent(code)}`;
  };

  return { transferAirtime, rechargeForOther, rechargeSelf, giftPackage };
};
