import { BalancePackageEntity, BankInfo, Language, TelecomAssets, ThemeMode, TransactionEntity } from '../types';
import { INITIAL_BANKS } from '../constants/banks';
import { INITIAL_PACKAGES, INITIAL_TELECOM_ASSETS, INITIAL_TRANSACTIONS } from './mockData';

const STORAGE_KEYS = {
  TRANSACTIONS: 'ethiobalance_transactions',
  PACKAGES: 'ethiobalance_packages',
  TELECOM_ASSETS: 'ethiobalance_telecom_assets',
  BANKS: 'ethiobalance_banks',
  LANGUAGE: 'ethiobalance_language',
  THEME: 'ethiobalance_theme',
  USER_NAME: 'ethiobalance_user_name',
  ONBOARDING_COMPLETED: 'ethiobalance_onboarding_completed',
};

export function loadTransactions(): TransactionEntity[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.TRANSACTIONS);
    if (raw) return JSON.parse(raw);
  } catch (e) {
    console.error('Failed to load transactions from localStorage', e);
  }
  return INITIAL_TRANSACTIONS;
}

export function saveTransactions(transactions: TransactionEntity[]) {
  localStorage.setItem(STORAGE_KEYS.TRANSACTIONS, JSON.stringify(transactions));
}

export function loadPackages(): BalancePackageEntity[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.PACKAGES);
    if (raw) return JSON.parse(raw);
  } catch (e) {
    console.error('Failed to load packages from localStorage', e);
  }
  return INITIAL_PACKAGES;
}

export function savePackages(packages: BalancePackageEntity[]) {
  localStorage.setItem(STORAGE_KEYS.PACKAGES, JSON.stringify(packages));
}

export function loadTelecomAssets(): TelecomAssets {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.TELECOM_ASSETS);
    if (raw) return JSON.parse(raw);
  } catch (e) {
    console.error('Failed to load telecom assets from localStorage', e);
  }
  return INITIAL_TELECOM_ASSETS;
}

export function saveTelecomAssets(assets: TelecomAssets) {
  localStorage.setItem(STORAGE_KEYS.TELECOM_ASSETS, JSON.stringify(assets));
}

export function loadBanks(): BankInfo[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.BANKS);
    if (raw) return JSON.parse(raw);
  } catch (e) {
    console.error('Failed to load banks from localStorage', e);
  }
  return INITIAL_BANKS;
}

export function saveBanks(banks: BankInfo[]) {
  localStorage.setItem(STORAGE_KEYS.BANKS, JSON.stringify(banks));
}

export function loadLanguage(): Language {
  const raw = localStorage.getItem(STORAGE_KEYS.LANGUAGE) as Language;
  return raw === 'am' || raw === 'om' || raw === 'en' ? raw : 'en';
}

export function saveLanguage(lang: Language) {
  localStorage.setItem(STORAGE_KEYS.LANGUAGE, lang);
}

export function loadTheme(): ThemeMode {
  const raw = localStorage.getItem(STORAGE_KEYS.THEME) as ThemeMode;
  return raw === 'light' || raw === 'vibrant' || raw === 'dark' ? raw : 'dark';
}

export function saveTheme(theme: ThemeMode) {
  localStorage.setItem(STORAGE_KEYS.THEME, theme);
}

export function loadUserName(): string {
  return localStorage.getItem(STORAGE_KEYS.USER_NAME) || 'Abebe Bikila';
}

export function saveUserName(name: string) {
  localStorage.setItem(STORAGE_KEYS.USER_NAME, name);
}

export function hasCompletedOnboarding(): boolean {
  return localStorage.getItem(STORAGE_KEYS.ONBOARDING_COMPLETED) === 'true';
}

export function setCompletedOnboarding(completed: boolean) {
  localStorage.setItem(STORAGE_KEYS.ONBOARDING_COMPLETED, completed ? 'true' : 'false');
}

export function resetAllData() {
  localStorage.removeItem(STORAGE_KEYS.TRANSACTIONS);
  localStorage.removeItem(STORAGE_KEYS.PACKAGES);
  localStorage.removeItem(STORAGE_KEYS.TELECOM_ASSETS);
  localStorage.removeItem(STORAGE_KEYS.BANKS);
}
