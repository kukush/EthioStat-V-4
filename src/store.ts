import { AppState, Language, Theme, Transaction, TelecomPackage, RecommendedBundle, SimCard, GiftRequest, UserProfile, PackageType, Intent } from './domain/types';
import { parseSmsUseCase } from './domain/useCases/ParseSmsUseCase';
import { syncBalanceUseCase } from './domain/useCases/SyncBalanceUseCase';
import { transferBalanceUseCase } from './domain/useCases/TransferBalanceUseCase';
import { FALLBACK_AVATAR } from './constants/avatars';
import { getMockData } from './data/mockDataService';

const mockSimPhone = import.meta.env.VITE_MOCK_SIM_PHONE || '+251911223344';
const defaultSources = (import.meta.env.VITE_DEFAULT_SOURCES || 'CBE,Telebirr,Awash,Dashen').split(',').map((s: string) => s.trim());
const mock = getMockData();

export const initialState: AppState = {
  language: (import.meta.env.VITE_DEFAULT_LANGUAGE as Language) || 'en',
  theme: (import.meta.env.VITE_DEFAULT_THEME as Theme) || 'light',
  activeTab: 'home',
  telecomBalance: Number(import.meta.env.VITE_MOCK_TELECOM_BALANCE) || 0,
  telebirrBalance: Number(import.meta.env.VITE_MOCK_TELEBIRR_BALANCE) || 0,
  userProfile: {
    name: import.meta.env.VITE_MOCK_USER_NAME || 'User',
    avatarUrl: import.meta.env.VITE_MOCK_USER_AVATAR || FALLBACK_AVATAR,
    phoneNumber: import.meta.env.VITE_MOCK_USER_PHONE || mockSimPhone
  },
  simCards: [
    { id: 'sim1', phoneNumber: mockSimPhone, label: 'Primary SIM', isPrimary: true, provider: 'Ethio Telecom' }
  ],
  transactionSources: defaultSources,
  telecomPackages: mock.telecomPackages,
  recommendedBundles: mock.recommendedBundles,
  transactions: mock.transactions,
  giftRequests: mock.giftRequests,
};

export const reducer = (state: AppState, intent: Intent): AppState => {
  switch (intent.type) {
    case 'SET_TAB':
      return { ...state, activeTab: intent.tab };
    case 'SET_LANGUAGE':
      return { ...state, language: intent.lang };
    case 'SET_THEME':
      return { ...state, theme: intent.theme };
    case 'ADD_TRANSACTION_SOURCE':
      return { ...state, transactionSources: [...state.transactionSources, intent.source] };
    case 'REMOVE_TRANSACTION_SOURCE':
      return { ...state, transactionSources: state.transactionSources.filter(s => s !== intent.source) };
    case 'ADD_TRANSACTION':
      if (state.transactions.some(t => t.id === intent.transaction.id)) {
        return state;
      }
      return { ...state, transactions: [intent.transaction, ...state.transactions] };
    case 'UPDATE_PACKAGES':
      return { ...state, telecomPackages: intent.packages };
    case 'UPDATE_BALANCE':
      return { ...state, telecomBalance: intent.balance };
    case 'ADD_SIM': {
      const newSimCards = [...state.simCards, intent.sim];
      const newState = { ...state, simCards: newSimCards };
      // When the new SIM is primary, sync phone to userProfile
      if (intent.sim.isPrimary && state.userProfile) {
        newState.userProfile = { ...state.userProfile, phoneNumber: intent.sim.phoneNumber };
      }
      return newState;
    }
    case 'REMOVE_SIM':
      return { ...state, simCards: state.simCards.filter(s => s.id !== intent.id) };

    case 'PARSE_SMS': 
      return { ...state, ...parseSmsUseCase(state, intent.text, intent.senderId) };
    case 'PARSE_USSD': 
      return { ...state, ...parseSmsUseCase(state, intent.text, 'USSD') };

    case 'ADD_GIFT_REQUEST':
      return { ...state, giftRequests: [intent.request, ...state.giftRequests] };
    case 'UPDATE_GIFT_REQUEST_STATUS':
      return { 
        ...state, 
        giftRequests: state.giftRequests.map(r => r.id === intent.id ? { ...r, status: intent.status } : r) 
      };
    case 'REMOVE_GIFT_REQUEST':
      return { ...state, giftRequests: state.giftRequests.filter(r => r.id !== intent.id) };
    case 'SET_USER_PROFILE':
      return { ...state, userProfile: intent.profile };
    case 'RECHARGE': 
      return { ...state, ...syncBalanceUseCase(state, intent.amount, intent.method) };
    
    case 'DIAL_USSD':
      // Handle USSD transfer - extract amount and recipient from code
      if (intent.code.startsWith('*806*')) {
        const parts = intent.code.replace('*', '').replace('#', '').split('*');
        if (parts.length >= 3) {
          const recipientNumber = parts[1];
          const amount = parseFloat(parts[2]);
          if (!isNaN(amount)) {
            return { ...state, ...transferBalanceUseCase(state, amount, recipientNumber, 'ussd') };
          }
        }
      }
      return state;

    case 'SET_PRIMARY_SIM': {
      const primarySim = state.simCards.find(s => s.id === intent.id);
      return { 
        ...state, 
        simCards: state.simCards.map(s => ({ ...s, isPrimary: s.id === intent.id })),
        userProfile: (state.userProfile && primarySim) ? { ...state.userProfile, phoneNumber: primarySim.phoneNumber } : state.userProfile
      };
    }
    default:
      return state;
  }
};
