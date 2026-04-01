import { useEffect, useState } from 'react';
import { Capacitor } from '@capacitor/core';
import SmsMonitor from '../../data/smsMonitorPlugin';
import { ETHIOPIAN_BANKS } from '../../constants/banks';

// EthioTelecom & Telebirr short-codes that deliver telecom package/balance SMS.
// These are scanned on every startup regardless of which banks the user has configured.
// We'll fetch these from native AppConstants to avoid hardcoding
let ALWAYS_SCAN_SENDERS: string[] = [];

export const useNativeBridge = (sources: string[] = []) => {
  const [ussdCodes, setUssdCodes] = useState<any>({});

  useEffect(() => {
    if (!Capacitor.isNativePlatform()) return;

    // Use Capacitor's built-in checkPermissions/requestPermissions so scans only
    // fire after Android has definitively confirmed READ_SMS is granted.
    (async () => {
      try {
        let { sms } = await SmsMonitor.checkPermissions();
        if (sms !== 'granted') {
          const result = await SmsMonitor.requestPermissions();
          sms = result.sms;
        }
        if (sms !== 'granted') {
          console.warn('[EthioStat] READ_SMS permission denied — SMS scanning disabled');
          return;
        }

        await SmsMonitor.startMonitoring();

        // Get USSD codes and sender list from native AppConstants
        try {
          const [ussdResult, sendersResult] = await Promise.all([
            SmsMonitor.getUssdCodes(),
            SmsMonitor.getSmsSenders()
          ]);
          setUssdCodes(ussdResult);
          ALWAYS_SCAN_SENDERS = sendersResult.senders || ['127', '251994'];
        } catch (error) {
          console.error('Failed to get constants from native:', error);
          // Fallback values
          setUssdCodes({
            BALANCE_CHECK: '*804#',
            RECHARGE_SELF: '*805#',
            TRANSFER_AIRTIME: '*806*',
            GIFT_PACKAGE: '*999#'
          });
          ALWAYS_SCAN_SENDERS = ['127', '251994'];
        }

        // Always scan EthioTelecom/Telebirr senders for telecom asset messages
        ALWAYS_SCAN_SENDERS.forEach(senderId => {
          SmsMonitor.scanHistory({ senderId, days: 7 });
        });

        // Trigger a 7-day historical scan for every user-configured source on startup
        sources.forEach(abbreviation => {
          const bank = ETHIOPIAN_BANKS.find(b => b.abbreviation === abbreviation);
          const senderId = bank?.senderId ?? abbreviation;
          // Skip if already covered by ALWAYS_SCAN_SENDERS
          if (!ALWAYS_SCAN_SENDERS.includes(senderId)) {
            SmsMonitor.scanHistory({ senderId, days: 7 });
          }
        });
      } catch (err) {
        console.warn('[EthioStat] Permission or scan init failed:', err);
      }
    })();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const transferAirtime = (recipient: string, amount: number) => {
    const code = `${ussdCodes.TRANSFER_AIRTIME || '*806*'}${recipient}*${amount}#`;
    if (Capacitor.isNativePlatform()) {
      SmsMonitor.dialUssd({ code });
    } else {
      window.location.href = `tel:${encodeURIComponent(code)}`;
    }
  };

  const rechargeForOther = (voucher: string, recipient: string) => {
    const code = `${ussdCodes.RECHARGE_OTHER || '*805*'}${voucher}*${recipient}#`;
    if (Capacitor.isNativePlatform()) {
      SmsMonitor.dialUssd({ code });
    } else {
      window.location.href = `tel:${encodeURIComponent(code)}`;
    }
  };

  const rechargeSelf = (voucher: string) => {
    const code = `${ussdCodes.RECHARGE_SELF || '*805#'}${voucher}#`;
    if (Capacitor.isNativePlatform()) {
      SmsMonitor.dialUssd({ code });
    } else {
      window.location.href = `tel:${encodeURIComponent(code)}`;
    }
  };

  const giftPackage = (sequence: string, recipient: string) => {
    const code = ussdCodes.GIFT_PACKAGE || '*999#'; 
    if (Capacitor.isNativePlatform()) {
      SmsMonitor.dialUssd({ code });
    } else {
      window.location.href = `tel:${encodeURIComponent(code)}`;
    }
  };

  const sendUssdRequest = (code: string, callback: (response: string) => void) => {
    if (Capacitor.isNativePlatform()) {
      // Use TelephonyManager.UssdResponseCallback for direct USSD response
      SmsMonitor.sendUssdRequest({ code }).then((result) => {
        if (result.response) {
          callback(result.response);
        }
      }).catch((error) => {
        console.error('USSD request failed:', error);
      });
    } else {
      // Fallback for web - just dial the code
      window.location.href = `tel:${encodeURIComponent(code)}`;
    }
  };

  const getUssdCodes = async () => {
    if (Capacitor.isNativePlatform()) {
      try {
        const result = await SmsMonitor.getUssdCodes();
        return result;
      } catch (error) {
        console.error('Failed to get USSD codes:', error);
        // Return empty object as fallback - let the calling component handle it
        return {};
      }
    } else {
      // Web fallback - return empty object
      return {};
    }
  };

  return { transferAirtime, rechargeForOther, rechargeSelf, giftPackage, sendUssdRequest, getUssdCodes };
};
