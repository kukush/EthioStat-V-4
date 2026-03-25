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
    if (Capacitor.isNativePlatform()) {
      SmsMonitor.dialUssd({ code });
    } else {
      window.location.href = `tel:${encodeURIComponent(code)}`;
    }
  };

  const rechargeForOther = (voucher: string, recipient: string) => {
    const code = `*805*${voucher}*${recipient}#`;
    if (Capacitor.isNativePlatform()) {
      SmsMonitor.dialUssd({ code });
    } else {
      window.location.href = `tel:${encodeURIComponent(code)}`;
    }
  };

  const rechargeSelf = (voucher: string) => {
    const code = `*805*${voucher}#`;
    if (Capacitor.isNativePlatform()) {
      SmsMonitor.dialUssd({ code });
    } else {
      window.location.href = `tel:${encodeURIComponent(code)}`;
    }
  };

  const giftPackage = (sequence: string, recipient: string) => {
    const code = `*999#`; 
    if (Capacitor.isNativePlatform()) {
      SmsMonitor.dialUssd({ code });
    } else {
      window.location.href = `tel:${encodeURIComponent(code)}`;
    }
  };

  return { transferAirtime, rechargeForOther, rechargeSelf, giftPackage };
};
