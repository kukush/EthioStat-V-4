import { AppState, Transaction } from '../types';

/**
 * UseCase for handling airtime transfer with balance deduction.
 * Deducts amount from telecomBalance and creates expense transaction.
 */
export const transferBalanceUseCase = (
  state: AppState, 
  amount: number, 
  recipientNumber: string,
  method: 'ussd' | 'telebirr'
): Partial<AppState> => {
  const newTransaction: Transaction = {
    id: `tx-${Date.now()}`,
    simId: state.simCards.find(s => s.isPrimary)?.id || 'unknown',
    type: 'expense',
    amount: amount,
    source: method === 'telebirr' ? 'Telebirr' : 'Ethio Telecom',
    description: `Airtime transfer to ${recipientNumber}`,
    timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    category: 'Transfer'
  };

  return {
    telecomBalance: state.telecomBalance - amount,
    transactions: [newTransaction, ...state.transactions]
  };
};
