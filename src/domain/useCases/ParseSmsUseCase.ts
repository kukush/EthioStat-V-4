import { AppState, TelecomPackage, Transaction } from '../types';
import { parseEthioSMS } from '../../data/smsParser';

/**
 * UseCase for parsing incoming SMS messages and updating the app state.
 * Aligns with the 'ParseSmsUseCase' in the proposed MVI architecture.
 */
export const parseSmsUseCase = (state: AppState, text: string, senderId?: string): Partial<AppState> => {
  const parsed = parseEthioSMS(text, senderId);
  const primarySim = state.simCards.find(s => s.isPrimary) || state.simCards[0];
  const simId = primarySim?.id || 'unknown';
  
  return {
    telecomBalance: parsed.balance ?? state.telecomBalance,
    telecomPackages: parsed.packages 
      ? [...state.telecomPackages, ...parsed.packages.map(p => ({ ...p, simId } as TelecomPackage))] 
      : state.telecomPackages,
    transactions: parsed.transaction 
      ? [{ ...parsed.transaction, simId } as Transaction, ...state.transactions] 
      : state.transactions,
  };
};
