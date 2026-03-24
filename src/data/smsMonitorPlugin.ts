import { registerPlugin } from '@capacitor/core';

export interface SmsMonitorPlugin {
  startMonitoring(): Promise<void>;
  scanHistory(options: { senderId: string; days?: number }): Promise<void>;
  addListener(eventName: 'ussdReceived', listenerFunc: (data: { text: string }) => void): void;
  addListener(eventName: 'smsFound', listenerFunc: (data: { sender: string; body: string; timestamp: number }) => void): void;
}

const SmsMonitor = registerPlugin<SmsMonitorPlugin>('SmsMonitor');

export default SmsMonitor;
