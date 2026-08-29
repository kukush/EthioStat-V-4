import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { TransactionScreen } from '../../screens/TransactionScreen';
import { BankInfo } from '../../types';

describe('TransactionScreen Filter Tests', () => {
  it('filters source dropdown to show only enabled banks', () => {
    const mockBanks: BankInfo[] = [
      { id: '1', abbreviation: 'CBE', displayName: 'CBE', fullName: 'CBE', senderIds: [], ussd: '', color: '', accentColor: '', enabled: true, currentBalance: 0 },
      { id: '2', abbreviation: 'BOA', displayName: 'BOA', fullName: 'BOA', senderIds: [], ussd: '', color: '', accentColor: '', enabled: false, currentBalance: 0 },
      { id: '3', abbreviation: 'AWASH', displayName: 'AWASH', fullName: 'AWASH', senderIds: [], ussd: '', color: '', accentColor: '', enabled: true, currentBalance: 0 },
    ];

    render(
      <TransactionScreen
        language="en"
        transactions={[]}
        banks={mockBanks}
        onOpenAddTx={vi.fn()}
        onClearTransactions={vi.fn()}
      />
    );

    // CBE should be present
    expect(screen.getByText('CBE')).toBeInTheDocument();
    // AWASH should be present
    expect(screen.getByText('AWASH')).toBeInTheDocument();
    // BOA should NOT be present
    expect(screen.queryByText('BOA')).not.toBeInTheDocument();
  });
});
