import { registerPlugin } from '@capacitor/core';

export interface SimCardInfo {
  id: string;
  phoneNumber: string;
  carrierName: string;
  slotIndex: number;
  isPrimary: boolean;
}

export interface SimDetectionPlugin {
  getSimCards(): Promise<{ sims: SimCardInfo[] }>;
  setPrimarySim(options: { id: string }): Promise<void>;
}

const SimDetection = registerPlugin<SimDetectionPlugin>('SimDetection');

export default SimDetection;
