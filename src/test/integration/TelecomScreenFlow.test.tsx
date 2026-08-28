import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { TelecomScreen } from '../../screens/TelecomScreen';
import { BalancePackageEntity, TelecomAssets } from '../../types';

describe('TelecomScreen Flow Integration Tests', () => {
  const mockAssets: TelecomAssets = {
    activeSim: 1,
    sim1Number: '0912345678',
    sim2Number: '0712345678',
    sim1Carrier: 'Ethio Telecom',
    sim2Carrier: 'Safaricom',
    airtimeBalance: 88.5,
    lastSyncedAt: Date.now(),
  };

  const mockPackages: BalancePackageEntity[] = [
    {
      id: 'pkg-1',
      type: 'internet',
      subType: 'Monthly Data',
      totalAmount: 10,
      remainingAmount: 6.4,
      unit: 'GB',
      expiryDate: Date.now() + 864000000,
      source: 'Ethio Telecom',
      simId: 'sim1',
      isActive: true,
    },
    {
      id: 'pkg-2',
      type: 'voice',
      subType: 'Night Voice',
      totalAmount: 100,
      remainingAmount: 45,
      unit: 'MIN',
      expiryDate: Date.now() + 432000000,
      source: 'Ethio Telecom',
      simId: 'sim1',
      isActive: true,
    },
  ];

  it('renders telecom balance, SIM number, and active packages', () => {
    const handleOpenUssd = vi.fn();
    const handleBuyBundle = vi.fn();

    render(
      <TelecomScreen
        language="en"
        telecomAssets={mockAssets}
        packages={mockPackages}
        onOpenUssd={handleOpenUssd}
        onBuyBundle={handleBuyBundle}
      />
    );

    expect(screen.getByText('Ethio Telecom')).toBeInTheDocument();
    expect(screen.getByText('0912345678')).toBeInTheDocument();
    expect(screen.getByText('88.50')).toBeInTheDocument();
    expect(screen.getByText(/Monthly Data/i)).toBeInTheDocument();
    expect(screen.getByText(/Night Voice/i)).toBeInTheDocument();
  });

  it('filters active packages by category pills', () => {
    render(
      <TelecomScreen
        language="en"
        telecomAssets={mockAssets}
        packages={mockPackages}
        onOpenUssd={vi.fn()}
        onBuyBundle={vi.fn()}
      />
    );

    // Filter to Voice only
    const voiceFilterBtn = screen.getByRole('button', { name: /^voice$/i });
    fireEvent.click(voiceFilterBtn);

    expect(screen.getByText(/Night Voice/i)).toBeInTheDocument();
    expect(screen.queryByText(/Monthly Data/i)).not.toBeInTheDocument();

    // Filter back to All
    const allFilterBtn = screen.getByRole('button', { name: /^all$/i });
    fireEvent.click(allFilterBtn);

    expect(screen.getByText(/Monthly Data/i)).toBeInTheDocument();
    expect(screen.getByText(/Night Voice/i)).toBeInTheDocument();
  });

  it('triggers USSD action handlers on quick buttons', () => {
    const handleOpenUssd = vi.fn();
    const handleBuyBundle = vi.fn();

    render(
      <TelecomScreen
        language="en"
        telecomAssets={mockAssets}
        packages={mockPackages}
        onOpenUssd={handleOpenUssd}
        onBuyBundle={handleBuyBundle}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: /\*804# Sync/i }));
    expect(handleOpenUssd).toHaveBeenCalledWith('balance');

    fireEvent.click(screen.getByRole('button', { name: /\*805\* Recharge/i }));
    expect(handleOpenUssd).toHaveBeenCalledWith('recharge');

    fireEvent.click(screen.getByRole('button', { name: /\*806\* Transfer/i }));
    expect(handleOpenUssd).toHaveBeenCalledWith('transfer');
  });

  it('triggers bundle buy callback from recommended catalog', () => {
    const handleBuyBundle = vi.fn();

    render(
      <TelecomScreen
        language="en"
        telecomAssets={mockAssets}
        packages={mockPackages}
        onOpenUssd={vi.fn()}
        onBuyBundle={handleBuyBundle}
      />
    );

    const buyButtons = screen.getAllByRole('button', { name: /Buy/i });
    expect(buyButtons.length).toBeGreaterThan(0);

    fireEvent.click(buyButtons[0]);
    expect(handleBuyBundle).toHaveBeenCalledTimes(1);
  });
});
