import { AppState, Language, Theme, SimCard, UserProfile, Intent } from './domain/types';
import { FALLBACK_AVATAR } from './constants/avatars';

const mockSimPhone = import.meta.env.VITE_MOCK_SIM_PHONE || '+251911223344';
const defaultSources = (import.meta.env.VITE_DEFAULT_SOURCES || 'CBE,Telebirr,Awash,Dashen').split(',').map((s: string) => s.trim());

export const initialState: AppState = {
  language: (import.meta.env.VITE_DEFAULT_LANGUAGE as Language) || 'en',
  theme: (import.meta.env.VITE_DEFAULT_THEME as Theme) || 'light',
  activeTab: 'home',
  userProfile: {
    name: import.meta.env.VITE_MOCK_USER_NAME || 'User',
    avatarUrl: import.meta.env.VITE_MOCK_USER_AVATAR || FALLBACK_AVATAR,
    phoneNumber: import.meta.env.VITE_MOCK_USER_PHONE || mockSimPhone
  },
  simCards: [
    { id: 'sim1', phoneNumber: mockSimPhone, label: 'Primary SIM', isPrimary: true, provider: 'Ethio Telecom' }
  ],
  transactionSources: defaultSources,
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
    case 'ADD_SIM': {
      const newSimCards = [...state.simCards, intent.sim];
      const newState = { ...state, simCards: newSimCards };
      if (intent.sim.isPrimary && state.userProfile) {
        newState.userProfile = { ...state.userProfile, phoneNumber: intent.sim.phoneNumber };
      }
      return newState;
    }
    case 'REMOVE_SIM':
      return { ...state, simCards: state.simCards.filter(s => s.id !== intent.id) };
    case 'SET_USER_PROFILE': {
      const newState = { ...state, userProfile: intent.profile };
      // Sync profile phone number back to primary SIM if valid
      if (intent.profile.phoneNumber && intent.profile.phoneNumber !== 'Unknown') {
        newState.simCards = state.simCards.map(s => 
          s.isPrimary ? { ...s, phoneNumber: intent.profile.phoneNumber } : s
        );
      }
      return newState;
    }
    case 'SET_PRIMARY_SIM': {
      const primarySim = state.simCards.find(s => s.id === intent.id);
      return { 
        ...state, 
        simCards: state.simCards.map(s => ({ ...s, isPrimary: s.id === intent.id })),
        userProfile: (state.userProfile && primarySim && primarySim.phoneNumber !== 'Unknown') 
          ? { ...state.userProfile, phoneNumber: primarySim.phoneNumber } 
          : state.userProfile
      };
    }
    case 'SET_SIMS': {
      const primarySim = intent.sims.find(s => s.isPrimary);
      const newState = { ...state, simCards: intent.sims };
      if (primarySim && state.userProfile && primarySim.phoneNumber && primarySim.phoneNumber !== 'Unknown') {
        newState.userProfile = { ...state.userProfile, phoneNumber: primarySim.phoneNumber };
      }
      return newState;
    }
    case 'SET_TRANSACTION_SOURCES':
      return { ...state, transactionSources: intent.sources };
    default:
      return state;
  }
};
