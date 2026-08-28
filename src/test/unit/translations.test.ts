import { describe, it, expect } from 'vitest';
import { t, formatCurrency, formatNumber } from '../../constants/translations';
import { Language } from '../../types';

describe('Translations & Localization Unit Tests', () => {
  const languages: Language[] = ['en', 'am', 'om'];

  it('provides translations for core keys in English, Amharic, and Afaan Oromoo', () => {
    languages.forEach((lang) => {
      expect(t(lang, 'netBalance')).toBeTruthy();
      expect(t(lang, 'sync')).toBeTruthy();
      expect(t(lang, 'income')).toBeTruthy();
      expect(t(lang, 'expense')).toBeTruthy();
      expect(t(lang, 'telecom')).toBeTruthy();
      expect(t(lang, 'transactions')).toBeTruthy();
      expect(t(lang, 'settings')).toBeTruthy();
    });
  });

  it('formats currency numbers with commas and decimals correctly', () => {
    expect(formatCurrency(12500.5)).toBe('12,500.50');
    expect(formatCurrency(0)).toBe('0.00');
    expect(formatCurrency(1000000)).toBe('1,000,000.00');
  });

  it('formats whole numbers properly', () => {
    expect(formatNumber(1500)).toBe('1,500');
    expect(formatNumber(0)).toBe('0');
  });
});
