export type Language = 'en' | 'am' | 'om';
export type Theme = 'light' | 'dark' | 'vibrant' | 'midnight' | 'forest';

export type PackageType = 'internet' | 'voice' | 'sms' | 'bonus';
export type TransactionType = 'income' | 'expense';

export interface SimCard {
  id: string;
  phoneNumber: string;
  label: string;
  isPrimary: boolean;
  provider: 'Ethio Telecom' | 'Safaricom' | 'Other';
}

export interface TelecomPackage {
  id: string;
  simId: string;
  type: PackageType;
  value: number;
  unit: string;
  total: number;
  expiryDate: string;
  label: string;
  daysLeft: number;
  totalDays: number;
}

export interface RecommendedBundle {
  id: string;
  type: 'internet' | 'voice' | 'sms' | 'combo';
  label: string;
  price: number;
  description: string;
}

export interface Transaction {
  id: string;
  simId: string;
  type: TransactionType;
  amount: number;
  source: string;
  description: string;
  timestamp: string;
  category: string;
}

export interface SourceSummary {
  source: string;
  income: number;
  expense: number;
  netBalance: number;
  transactionCount: number;
  lastTransaction: string;
}

export interface GiftRequest {
  id: string;
  senderNumber: string;
  bundleId: string;
  timestamp: string;
  status: 'pending' | 'accepted' | 'rejected';
}

export interface UserProfile {
  name: string;
  avatarUrl: string;
  phoneNumber: string;
}

export interface AppState {
  language: Language;
  theme: Theme;
  simCards: SimCard[];
  telecomPackages: TelecomPackage[];
  recommendedBundles: RecommendedBundle[];
  transactions: Transaction[];
  transactionSources: string[];
  telecomBalance: number;
  telebirrBalance: number;
  activeTab: 'home' | 'telecom' | 'transactions' | 'settings';
  giftRequests: GiftRequest[];
  userProfile?: UserProfile;
}
