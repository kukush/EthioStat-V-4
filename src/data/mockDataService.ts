import { Transaction, TelecomPackage, RecommendedBundle, GiftRequest, SimCard, AppState } from '../types';

/**
 * Returns the default initial mock data for the app.
 * Previously hardcoded in store.ts — now centralized here.
 */
export function getMockData() {
  const telecomPackages: TelecomPackage[] = [
    { id: '1', simId: 'sim1', type: 'internet', value: 450, total: 1024, unit: 'MB', label: 'Daily Data', expiryDate: 'Today, 11:59 PM', daysLeft: 1, totalDays: 1 },
    { id: '2', simId: 'sim1', type: 'voice', value: 15, total: 20, unit: 'Min', label: 'Night Voice', expiryDate: 'Tomorrow, 6:00 AM', daysLeft: 1, totalDays: 1 },
    { id: '4', simId: 'sim1', type: 'sms', value: 85, total: 100, unit: 'SMS', label: 'Weekly SMS', expiryDate: 'Oct 28, 2026', daysLeft: 5, totalDays: 7 },
    { id: '3', simId: 'sim1', type: 'bonus', value: 12.50, total: 50, unit: 'ETB', label: 'Airtime Bonus', expiryDate: 'Oct 30, 2026', daysLeft: 30, totalDays: 30 },
  ];

  const recommendedBundles: RecommendedBundle[] = [
    { id: 'r1', type: 'internet', label: '1GB Monthly', price: 100, description: '1GB Data for 30 days' },
    { id: 'r2', type: 'combo', label: 'Weekly Combo', price: 50, description: '500MB + 50 Min + 50 SMS' },
    { id: 'r3', type: 'voice', label: 'Daily Voice', price: 10, description: '30 Min for 24 hours' },
    { id: 'r4', type: 'internet', label: '2GB Daily', price: 29, description: '2GB Data for 24 hours' },
    { id: 'r5', type: 'sms', label: 'Monthly SMS', price: 45, description: '500 SMS for 30 days' },
    { id: 'r6', type: 'combo', label: 'Monthly Premium', price: 450, description: '10GB + 500 Min + 500 SMS' },
  ];

  const transactions: Transaction[] = [
    { id: '1', simId: 'sim1', type: 'income', amount: 7500, source: 'CBE', description: 'Salary Credit', timestamp: '12:45 PM', category: 'Income' },
    { id: '2', simId: 'sim1', type: 'expense', amount: 1100, source: 'Telebirr', description: 'Utility Payment', timestamp: '10:30 AM', category: 'Utility' },
    { id: '3', simId: 'sim1', type: 'expense', amount: 2000, source: 'Awash', description: 'Transfer to Savings', timestamp: 'Yesterday', category: 'Transfer' },
    { id: '4', simId: 'sim1', type: 'expense', amount: 450, source: 'Telebirr', description: 'Ethio Telecom Recharge', timestamp: 'Oct 22', category: 'Telecom' },
  ];

  const giftRequests: GiftRequest[] = [
    { id: 'gr1', senderNumber: '0922334455', bundleId: 'r1', timestamp: '10:15 AM', status: 'pending' },
    { id: 'gr2', senderNumber: '0933445566', bundleId: 'r3', timestamp: 'Yesterday', status: 'pending' },
  ];

  return { telecomPackages, recommendedBundles, transactions, giftRequests };
}

export const generateMockData = (simId: string): Partial<AppState> => {
  const timestamp = Date.now();
  const transactions: Transaction[] = [
    {
      id: `mock-tx-1-${timestamp}`,
      simId,
      type: 'income',
      amount: 15000,
      source: 'CBE',
      description: 'Monthly Salary',
      timestamp: new Date().toISOString(),
      category: 'Income'
    },
    {
      id: `mock-tx-2-${timestamp}`,
      simId,
      type: 'expense',
      amount: 450,
      source: 'Telebirr',
      description: 'Ethio Telecom Recharge',
      timestamp: new Date(Date.now() - 86400000).toISOString(),
      category: 'Telecom'
    },
    {
      id: `mock-tx-3-${timestamp}`,
      simId,
      type: 'expense',
      amount: 1200,
      source: 'Telebirr',
      description: 'Utility Bill Payment',
      timestamp: new Date(Date.now() - 172800000).toISOString(),
      category: 'Utility'
    },
    {
      id: `mock-tx-4-${timestamp}`,
      simId,
      type: 'income',
      amount: 2500,
      source: 'Awash',
      description: 'Transfer from Friend',
      timestamp: new Date(Date.now() - 259200000).toISOString(),
      category: 'Transfer'
    }
  ];

  const packages: TelecomPackage[] = [
    {
      id: `mock-pkg-1-${timestamp}`,
      simId,
      type: 'internet',
      value: 1.5,
      total: 2,
      unit: 'GB',
      label: 'Monthly Data',
      expiryDate: new Date(Date.now() + 86400000 * 15).toISOString(),
      daysLeft: 15,
      totalDays: 30
    },
    {
      id: `mock-pkg-2-${timestamp}`,
      simId,
      type: 'voice',
      value: 45,
      total: 100,
      unit: 'Min',
      label: 'Weekly Voice',
      expiryDate: new Date(Date.now() + 86400000 * 3).toISOString(),
      daysLeft: 3,
      totalDays: 7
    }
  ];

  return {
    transactions,
    telecomPackages: packages,
    telecomBalance: 245.75,
    telebirrBalance: 5430.20
  };
};

export const injectMockData = (dispatch: any, simId: string) => {
  const mockData = generateMockData(simId);
  
  if (mockData.transactions) {
    mockData.transactions.forEach(tx => {
      dispatch({ type: 'ADD_TRANSACTION', transaction: tx });
    });
  }
  
  if (mockData.telecomPackages) {
    dispatch({ type: 'UPDATE_PACKAGES', packages: mockData.telecomPackages });
  }
  
  if (mockData.telecomBalance !== undefined) {
    dispatch({ type: 'UPDATE_BALANCE', balance: mockData.telecomBalance });
  }
};
