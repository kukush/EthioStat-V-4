import { describe, it, expect, beforeEach } from 'vitest';
import {
  loadTransactions,
  saveTransactions,
  loadPackages,
  savePackages,
  loadTelecomAssets,
  saveTelecomAssets,
  loadBanks,
  saveBanks,
  loadLanguage,
  saveLanguage,
  loadTheme,
  saveTheme,
  loadUserName,
  saveUserName,
  hasCompletedOnboarding,
  setCompletedOnboarding,
  resetAllData,
} from '../../services/storage';
import { TransactionEntity, BalancePackageEntity, TelecomAssets, BankInfo } from '../../types';

describe('Storage Service Unit Tests', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('loads default transactions when storage is empty', () => {
    const txs = loadTransactions();
    expect(txs.length).toBeGreaterThan(0);
    expect(txs[0]).toHaveProperty('id');
    expect(txs[0]).toHaveProperty('amount');
  });

  it('saves and loads custom transactions accurately', () => {
    const customTx: TransactionEntity[] = [
      {
        id: 'test-tx-101',
        type: 'INCOME',
        category: 'SALARY',
        amount: 25000,
        currency: 'ETB',
        source: 'CBE',
        recipientOrSender: 'Ethiopian Airlines',
        timestamp: 1724000000000,
        reference: 'FT99220011',
        simSlot: 1,
      },
    ];

    saveTransactions(customTx);
    const loaded = loadTransactions();
    expect(loaded).toEqual(customTx);
    expect(loaded[0].amount).toBe(25000);
  });

  it('saves and loads packages', () => {
    const customPackages: BalancePackageEntity[] = [
      {
        id: 'pkg-data-1',
        type: 'internet',
        subType: 'Monthly',
        totalAmount: 10,
        remainingAmount: 7.5,
        unit: 'GB',
        expiryDate: Date.now() + 864000000,
        isActive: true,
        source: 'Ethio Telecom',
        simId: 'sim1',
      },
    ];

    savePackages(customPackages);
    const loaded = loadPackages();
    expect(loaded).toEqual(customPackages);
    expect(loaded[0].remainingAmount).toBe(7.5);
  });

  it('saves and loads telecom assets', () => {
    const customAssets: TelecomAssets = {
      activeSim: 2,
      sim1Number: '0911000000',
      sim2Number: '0711000000',
      sim1Carrier: 'Ethio Telecom',
      sim2Carrier: 'Safaricom',
      airtimeBalance: 120.75,
      lastSyncedAt: 1725000000000,
    };

    saveTelecomAssets(customAssets);
    const loaded = loadTelecomAssets();
    expect(loaded).toEqual(customAssets);
    expect(loaded.airtimeBalance).toBe(120.75);
    expect(loaded.activeSim).toBe(2);
  });

  it('handles language preferences persistence', () => {
    expect(loadLanguage()).toBe('en');
    saveLanguage('am');
    expect(loadLanguage()).toBe('am');
    saveLanguage('om');
    expect(loadLanguage()).toBe('om');
  });

  it('handles theme preferences persistence', () => {
    expect(loadTheme()).toBe('dark');
    saveTheme('light');
    expect(loadTheme()).toBe('light');
    saveTheme('vibrant');
    expect(loadTheme()).toBe('vibrant');
  });

  it('handles user name and onboarding status', () => {
    expect(loadUserName()).toBe('Abebe Bikila');
    saveUserName('Kenenisa Bekele');
    expect(loadUserName()).toBe('Kenenisa Bekele');

    expect(hasCompletedOnboarding()).toBe(false);
    setCompletedOnboarding(true);
    expect(hasCompletedOnboarding()).toBe(true);
  });

  it('clears core financial data on resetAllData', () => {
    saveTransactions([
      {
        id: 'tx-1',
        type: 'EXPENSE',
        category: 'DINING',
        amount: 50,
        currency: 'ETB',
        source: 'CASH',
        recipientOrSender: 'Lunch Cafe',
        timestamp: Date.now(),
        simSlot: 1,
      },
    ]);
    expect(localStorage.getItem('ethiobalance_transactions')).toBeTruthy();

    resetAllData();
    expect(localStorage.getItem('ethiobalance_transactions')).toBeNull();
  });
});
