import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { TelecomAssetCard } from '../../components/TelecomAssetCard';

describe('TelecomAssetCard', () => {
  const defaultProps = {
    language: 'en' as const,
    dataVolGb: 1.5,
    voiceMinutes: 120,
    smsCount: 50,
    airtimeBalance: 25.0,
    onOpenUssd: vi.fn(),
  };

  it('renders quick action buttons when isCompact is false (default)', () => {
    render(<TelecomAssetCard {...defaultProps} />);
    expect(screen.getByRole('button', { name: /Recharge/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Transfer/i })).toBeInTheDocument();
  });

  it('hides quick action buttons when isCompact is true', () => {
    render(<TelecomAssetCard {...defaultProps} isCompact={true} />);
    expect(screen.queryByRole('button', { name: /Recharge/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Transfer/i })).not.toBeInTheDocument();
  });
});
