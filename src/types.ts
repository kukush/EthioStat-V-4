export type TransactionType = 'INCOME' | 'EXPENSE' | 'TRANSFER';

export type TransactionCategory =
  | 'UTILITY'
  | 'GROCERY'
  | 'SHOPPING'
  | 'DINING'
  | 'TELECOM'
  | 'TRANSFER'
  | 'SALARY'
  | 'RECHARGE'
  | 'GENERAL';

export interface TransactionEntity {
  id: string;
  amount: number;
  currency: string;
  source: string; // e.g. "TELEBIRR", "CBE", "AWASH", "DASHEN", "BOA", "AIRTIME"
  type: TransactionType;
  category: TransactionCategory;
  recipientOrSender?: string;
  reference?: string;
  timestamp: number;
  rawSmsBody?: string;
  simSlot: number;
  fee?: number;
  balanceAfter?: number;
}

export type TelecomPackageType = 'internet' | 'voice' | 'sms' | 'bonus';

export interface BalancePackageEntity {
  id: string;
  type: TelecomPackageType;
  subType: string; // "Monthly", "Weekly", "Daily", "Night", "Free", "Recurring"
  totalAmount: number;
  remainingAmount: number;
  unit: 'GB' | 'MB' | 'MIN' | 'SMS';
  expiryDate: number; // epoch ms
  isActive: boolean;
  source: string;
  simId: string;
  lastUpdated?: number;
}

export interface BankInfo {
  id: string;
  abbreviation: string;
  displayName: string;
  fullName: string;
  senderIds: string[];
  ussd: string;
  color: string;
  accentColor: string;
  enabled: boolean;
  currentBalance: number;
}

export interface TelecomAssets {
  airtimeBalance: number;
  sim1Number: string;
  sim2Number: string;
  activeSim: 1 | 2;
  sim1Carrier: string;
  sim2Carrier: string;
  lastSyncedAt: number;
}

export type Language = 'en' | 'am' | 'om';
export type ThemeMode = 'dark' | 'light' | 'vibrant';
export type FilterPeriod = 'today' | 'weekly' | 'monthly' | 'yearly' | 'custom' | 'allTime';

export interface RecommendedBundle {
  id: string;
  title: string;
  type: TelecomPackageType;
  duration: 'daily' | 'weekly' | 'monthly' | 'night';
  amount: number;
  unit: 'GB' | 'MB' | 'MIN' | 'SMS';
  priceEtb: number;
  ussdCode: string;
  popular?: boolean;
}

export interface ParsedSmsResult {
  scenario: string;
  confidence: number;
  source: string;
  type?: TransactionType;
  amount?: number;
  fee?: number;
  balanceAfter?: number;
  reference?: string;
  partyName?: string;
  category?: TransactionCategory;
  airtimeBalance?: number;
  packages: BalancePackageEntity[];
  isRecharge?: boolean;
}
