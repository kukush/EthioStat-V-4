import { AppState, Language, Theme, Transaction, TelecomPackage, RecommendedBundle, SimCard, GiftRequest, UserProfile, PackageType } from './types';
import { parseEthioSMS } from './services/smsParser';

export type Intent =
  | { type: 'SET_TAB'; tab: AppState['activeTab'] }
  | { type: 'SET_LANGUAGE'; lang: Language }
  | { type: 'SET_THEME'; theme: Theme }
  | { type: 'ADD_TRANSACTION_SOURCE'; source: string }
  | { type: 'REMOVE_TRANSACTION_SOURCE'; source: string }
  | { type: 'ADD_TRANSACTION'; transaction: Transaction }
  | { type: 'UPDATE_PACKAGES'; packages: TelecomPackage[] }
  | { type: 'UPDATE_BALANCE'; balance: number }
  | { type: 'PARSE_SMS'; text: string; senderId?: string }
  | { type: 'ADD_SIM'; sim: SimCard }
  | { type: 'REMOVE_SIM'; id: string }
  | { type: 'SET_PRIMARY_SIM'; id: string }
  | { type: 'ADD_GIFT_REQUEST'; request: GiftRequest }
  | { type: 'UPDATE_GIFT_REQUEST_STATUS'; id: string; status: GiftRequest['status'] }
  | { type: 'REMOVE_GIFT_REQUEST'; id: string }
  | { type: 'SET_USER_PROFILE'; profile: UserProfile }
  | { type: 'RECHARGE'; amount: number; method: 'ussd' | 'telebirr'; packageType?: PackageType }
;

export const initialState: AppState = {
  language: 'en',
  theme: 'light',
  activeTab: 'home',
  telecomBalance: 145.50,
  telebirrBalance: 2500.00,
  userProfile: {
    name: 'Abebe Kebede',
    avatarUrl: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&q=80&w=200',
    phoneNumber: '+251911223344'
  },
  simCards: [
    { id: 'sim1', phoneNumber: '+251911223344', label: 'Primary SIM', isPrimary: true, provider: 'Ethio Telecom' }
  ],
  transactionSources: ['CBE', 'Telebirr', 'Awash', 'Dashen'],
  telecomPackages: [
    { id: '1', simId: 'sim1', type: 'internet', value: 450, total: 1024, unit: 'MB', label: 'Daily Data', expiryDate: 'Today, 11:59 PM', daysLeft: 1, totalDays: 1 },
    { id: '2', simId: 'sim1', type: 'voice', value: 15, total: 20, unit: 'Min', label: 'Night Voice', expiryDate: 'Tomorrow, 6:00 AM', daysLeft: 1, totalDays: 1 },
    { id: '4', simId: 'sim1', type: 'sms', value: 85, total: 100, unit: 'SMS', label: 'Weekly SMS', expiryDate: 'Oct 28, 2026', daysLeft: 5, totalDays: 7 },
    { id: '3', simId: 'sim1', type: 'bonus', value: 12.50, total: 50, unit: 'ETB', label: 'Airtime Bonus', expiryDate: 'Oct 30, 2026', daysLeft: 30, totalDays: 30 },
  ],
  recommendedBundles: [
    { id: 'r1', type: 'internet', label: '1GB Monthly', price: 100, description: '1GB Data for 30 days' },
    { id: 'r2', type: 'combo', label: 'Weekly Combo', price: 50, description: '500MB + 50 Min + 50 SMS' },
    { id: 'r3', type: 'voice', label: 'Daily Voice', price: 10, description: '30 Min for 24 hours' },
    { id: 'r4', type: 'internet', label: '2GB Daily', price: 29, description: '2GB Data for 24 hours' },
    { id: 'r5', type: 'sms', label: 'Monthly SMS', price: 45, description: '500 SMS for 30 days' },
    { id: 'r6', type: 'combo', label: 'Monthly Premium', price: 450, description: '10GB + 500 Min + 500 SMS' },
  ],
  transactions: [
    { id: '1', simId: 'sim1', type: 'income', amount: 7500, source: 'CBE', description: 'Salary Credit', timestamp: '12:45 PM', category: 'Income' },
    { id: '2', simId: 'sim1', type: 'expense', amount: 1100, source: 'Telebirr', description: 'Utility Payment', timestamp: '10:30 AM', category: 'Utility' },
    { id: '3', simId: 'sim1', type: 'expense', amount: 2000, source: 'Awash', description: 'Transfer to Savings', timestamp: 'Yesterday', category: 'Transfer' },
    { id: '4', simId: 'sim1', type: 'expense', amount: 450, source: 'Telebirr', description: 'Ethio Telecom Recharge', timestamp: 'Oct 22', category: 'Telecom' },
  ],
  giftRequests: [
    { id: 'gr1', senderNumber: '0922334455', bundleId: 'r1', timestamp: '10:15 AM', status: 'pending' },
    { id: 'gr2', senderNumber: '0933445566', bundleId: 'r3', timestamp: 'Yesterday', status: 'pending' },
  ],
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
    case 'ADD_SIM':
      return { ...state, simCards: [...state.simCards, intent.sim] };
    case 'REMOVE_SIM':
      return { ...state, simCards: state.simCards.filter(s => s.id !== intent.id) };
    case 'SET_PRIMARY_SIM':
      return { 
        ...state, 
        simCards: state.simCards.map(s => ({ ...s, isPrimary: s.id === intent.id })) 
      };
    case 'PARSE_SMS': {
      const parsed = parseEthioSMS(intent.text, intent.senderId);
      const primarySim = state.simCards.find(s => s.isPrimary) || state.simCards[0];
      const simId = primarySim?.id || 'unknown';
      
      return {
        ...state,
        telecomBalance: parsed.balance ?? state.telecomBalance,
        telecomPackages: parsed.packages 
          ? [...state.telecomPackages, ...parsed.packages.map(p => ({ ...p, simId } as TelecomPackage))] 
          : state.telecomPackages,
        transactions: parsed.transaction 
          ? [{ ...parsed.transaction, simId } as Transaction, ...state.transactions] 
          : state.transactions,
      };
    }
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
    case 'RECHARGE': {
      const isExpense = intent.method === 'telebirr';
      const newTransaction: Transaction = {
        id: `tx-${Date.now()}`,
        simId: state.simCards.find(s => s.isPrimary)?.id || 'unknown',
        type: isExpense ? 'expense' : 'income',
        amount: intent.amount,
        source: intent.method === 'telebirr' ? 'Telebirr' : 'USSD',
        description: `Recharge via ${intent.method.toUpperCase()}`,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        category: 'Telecom'
      };

      return {
        ...state,
        telecomBalance: state.telecomBalance + (intent.method === 'ussd' ? intent.amount : 0),
        telebirrBalance: state.telebirrBalance - (intent.method === 'telebirr' ? intent.amount : 0),
        transactions: [newTransaction, ...state.transactions]
      };
    }
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
