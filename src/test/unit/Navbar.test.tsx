import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Navbar, BottomNavBar } from '../../components/Navbar';
import { TelecomAssets } from '../../types';

describe('Navbar Unit Tests', () => {
  const telecomAssets: TelecomAssets = {
    activeSim: 1,
    sim1Number: '0911223344',
    sim2Number: '0711223344',
    airtimeBalance: 45.5,
    lastSyncedAt: Date.now(),
  };

  const defaultProps = {
    activeTab: 'home' as const,
    setActiveTab: vi.fn(),
    language: 'en' as const,
    telecomAssets,
    onSwitchSim: vi.fn(),
  };

  it('renders brand title and offline security indicator', () => {
    render(<Navbar {...defaultProps} />);

    expect(screen.getByText('EthioBalance')).toBeInTheDocument();
    expect(screen.getByText('Offline Ledger')).toBeInTheDocument();
  });

  it('does NOT render the sync button in Navbar anymore', () => {
    render(<Navbar {...defaultProps} />);

    const quickSyncBtn = document.getElementById('quick-sync-btn');
    expect(quickSyncBtn).toBeNull();
  });

  it('switches active SIM card when SIM selector pill is clicked', () => {
    render(<Navbar {...defaultProps} />);

    const sim2Btn = screen.getByRole('button', { name: /SIM 2/i });
    fireEvent.click(sim2Btn);
    expect(defaultProps.onSwitchSim).toHaveBeenCalledWith(2);
  });

  it('BottomNavBar renders tab items and responds to tab switches', () => {
    const setActiveTab = vi.fn();
    render(
      <BottomNavBar
        activeTab="home"
        setActiveTab={setActiveTab}
        language="en"
      />
    );

    const telecomTab = screen.getByText('Telecom');
    fireEvent.click(telecomTab);
    expect(setActiveTab).toHaveBeenCalledWith('telecom');
  });
});
