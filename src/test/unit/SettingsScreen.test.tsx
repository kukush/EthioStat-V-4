import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { SettingsScreen } from '../../screens/SettingsScreen';
import { BankInfo, TelecomAssets } from '../../types';

describe('SettingsScreen Unit Tests', () => {
  const telecomAssets: TelecomAssets = {
    activeSim: 1,
    sim1Number: '0911223344',
    sim2Number: '0711223344',
    sim1Carrier: 'Ethio Telecom',
    sim2Carrier: 'Safaricom',
    airtimeBalance: 45.5,
    lastSyncedAt: Date.now(),
  };

  const defaultProps = {
    language: 'en' as const,
    setLanguage: vi.fn(),
    theme: 'dark' as const,
    setTheme: vi.fn(),
    banks: [] as BankInfo[],
    onToggleBank: vi.fn(),
    onAddBank: vi.fn(),
    onEditBank: vi.fn(),
    onDeleteBank: vi.fn(),
    telecomAssets,
    onUpdateTelecomAssets: vi.fn(),
    onOpenSmsSimulator: vi.fn(),
    onOpenOnboarding: vi.fn(),
    onResetData: vi.fn(),
    isBiometricEnabled: false,
    setIsBiometricEnabled: vi.fn(),
    isPinEnabled: false,
    setIsPinEnabled: vi.fn(),
    isAutoSyncEnabled: true,
    setIsAutoSyncEnabled: vi.fn(),
  };

  it('renders correctly', () => {
    render(<SettingsScreen {...defaultProps} />);
    expect(screen.getByText(/Security/i)).toBeInTheDocument();
  });

  it('handles SIM phone updates', () => {
    render(<SettingsScreen {...defaultProps} />);
    const sim1Input = screen.getByPlaceholderText('0911223344');
    fireEvent.change(sim1Input, { target: { value: '0987654321' } });
    const saveBtn = screen.getByText(/Save SIM Info/i);
    fireEvent.click(saveBtn);
    expect(defaultProps.onUpdateTelecomAssets).toHaveBeenCalled();
  });
});
