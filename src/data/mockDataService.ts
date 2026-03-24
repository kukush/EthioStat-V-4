import { Transaction, TelecomPackage, SimCard, AppState } from '../types';

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
