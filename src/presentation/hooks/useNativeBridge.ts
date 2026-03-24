import React, { useEffect } from 'react';
import SmsMonitor from '@/data/smsMonitorPlugin';
import { Intent } from '@/domain/types';

export const useNativeBridge = (dispatch: React.Dispatch<Intent>) => {
  useEffect(() => {
    // Start native monitoring
    SmsMonitor.startMonitoring();

    // Listen for USSD capture events
    const ussdListener = SmsMonitor.addListener('ussdReceived', (data) => {
      console.log('Native Bridge: USSD Received', data.text);
      dispatch({ type: 'PARSE_USSD', text: data.text });
    });

    // Listen for Historical SMS scan results
    const smsListener = SmsMonitor.addListener('smsFound', (data) => {
      console.log('Native Bridge: SMS Found', data.sender);
      dispatch({ type: 'PARSE_SMS', text: data.body, senderId: data.sender });
    });

    return () => {
      // Cleanup if needed
    };
  }, [dispatch]);
};
