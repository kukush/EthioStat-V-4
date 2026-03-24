import { AppState, Transaction } from '../types';

/**
 * UseCase for handling a recharge/sync balance intent.
 * Aligns with the 'SyncBalanceUseCase' in the proposed MVI architecture.
 */
export const syncBalanceUseCase = (
  state: AppState, 
  amount: number, 
  method: 'ussd' | 'telebirr'
): Partial<AppState> => {
  const isExpense = method === 'telebirr';
  const newTransaction: Transaction = {
    id: `tx-${Date.now()}`,
    simId: state.simCards.find(s => s.isPrimary)?.id || 'unknown',
    type: isExpense ? 'expense' : 'income',
    amount: amount,
    source: method === 'telebirr' ? 'Telebirr' : 'USSD',
    description: `Recharge via ${method.toUpperCase()}`,
    timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    category: 'Telecom'
  };

  return {
    telecomBalance: state.telecomBalance + (method === 'ussd' ? amount : 0),
    telebirrBalance: state.telebirrBalance - (method === 'telebirr' ? amount : 0),
    transactions: [newTransaction, ...state.transactions]
  };
};
