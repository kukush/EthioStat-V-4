import { registerPlugin, PermissionState } from '@capacitor/core';

import { TelecomPackage, Transaction } from '../domain/types';

export interface SmsPermissionStatus {
  sms: PermissionState;
}

export interface SmsMonitorPlugin {
  checkPermissions(): Promise<SmsPermissionStatus>;
  requestPermissions(): Promise<SmsPermissionStatus>;
  startMonitoring(): Promise<void>;
  scanHistory(options: { senderId: string; days?: number }): Promise<void>;
  addListener(eventName: 'ussdReceived', listenerFunc: (data: { text: string }) => void): void;
  addListener(eventName: 'smsFound', listenerFunc: (data: { sender: string; body: string; timestamp: number }) => void): void;
  getBalances(): Promise<{ packages: TelecomPackage[], netBalance: number }>;
  getTransactions(): Promise<{ transactions: Transaction[] }>;
  dialUssd(options: { code: string }): Promise<void>;
  updateTransactionSources(options: { sources: { abbreviation: string; name: string; ussd: string; senderId?: string; isEnabled?: boolean }[] }): Promise<void>;
}

const SmsMonitor = registerPlugin<SmsMonitorPlugin>('SmsMonitor');

export default SmsMonitor;
