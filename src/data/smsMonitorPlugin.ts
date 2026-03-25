import { registerPlugin } from '@capacitor/core';

import { TelecomPackage, Transaction } from '../domain/types';

export interface SmsMonitorPlugin {
  startMonitoring(): Promise<void>;
  scanHistory(options: { senderId: string; days?: number }): Promise<void>;
  addListener(eventName: 'ussdReceived', listenerFunc: (data: { text: string }) => void): void;
  addListener(eventName: 'smsFound', listenerFunc: (data: { sender: string; body: string; timestamp: number }) => void): void;
  getBalances(): Promise<{ packages: TelecomPackage[], netBalance: number }>;
  getTransactions(): Promise<{ transactions: Transaction[] }>;
}

const SmsMonitor = registerPlugin<SmsMonitorPlugin>('SmsMonitor');

export default SmsMonitor;
