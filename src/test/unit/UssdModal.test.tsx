import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { UssdModal } from '../../components/UssdModal';

describe('UssdModal Unit Tests', () => {
  const defaultProps = {
    isOpen: true,
    onClose: vi.fn(),
    language: 'en' as const,
    initialAction: 'balance' as const,
    onExecuteRecharge: vi.fn(),
    onExecuteTransfer: vi.fn(),
    onExecuteSync: vi.fn(),
    onExecuteBuyBundle: vi.fn(),
  };

  it('renders USSD modal with tabs and close button', () => {
    render(<UssdModal {...defaultProps} />);

    expect(screen.getByText('USSD & Telecom Center')).toBeInTheDocument();
    expect(screen.getByText('*804# Sync')).toBeInTheDocument();
    expect(screen.getByText('*805* Recharge')).toBeInTheDocument();
    expect(screen.getByText('*806* Transfer')).toBeInTheDocument();
    expect(screen.getByText('*999# Bundles')).toBeInTheDocument();
  });

  it('handles *804# sync execution', async () => {
    render(<UssdModal {...defaultProps} />);

    const dialBtn = screen.getByRole('button', { name: /Dial \*804# Balance Sync/i });
    expect(dialBtn).toBeInTheDocument();

    fireEvent.click(dialBtn);

    await waitFor(
      () => {
        expect(defaultProps.onExecuteSync).toHaveBeenCalledTimes(1);
      },
      { timeout: 2000 }
    );
  });

  it('validates voucher code on *805* recharge tab', async () => {
    render(<UssdModal {...defaultProps} initialAction="recharge" />);

    const voucherInput = screen.getByPlaceholderText('e.g. 1234567890123');
    expect(voucherInput).toBeInTheDocument();

    // Fill valid voucher code
    fireEvent.change(voucherInput, { target: { value: '9876543210123' } });

    const submitBtn = screen.getByRole('button', { name: /Recharge via USSD/i });
    fireEvent.click(submitBtn);

    await waitFor(
      () => {
        expect(defaultProps.onExecuteRecharge).toHaveBeenCalledWith('9876543210123');
      },
      { timeout: 2000 }
    );
  });

  it('validates phone number on *806* airtime transfer tab', async () => {
    render(<UssdModal {...defaultProps} initialAction="transfer" />);

    const phoneInput = screen.getByPlaceholderText('0911223344');
    const amountInput = screen.getByPlaceholderText('25');

    fireEvent.change(phoneInput, { target: { value: '0911223344' } });
    fireEvent.change(amountInput, { target: { value: '50' } });

    const transferBtn = screen.getByRole('button', { name: /Transfer via USSD/i });
    fireEvent.click(transferBtn);

    await waitFor(
      () => {
        expect(defaultProps.onExecuteTransfer).toHaveBeenCalledWith('0911223344', 50);
      },
      { timeout: 2000 }
    );
  });

  it('executes bundle subscription from *999# Bundles tab', async () => {
    render(<UssdModal {...defaultProps} initialAction="gift" />);

    const buyButtons = screen.getAllByRole('button', { name: /ETB/i });
    expect(buyButtons.length).toBeGreaterThan(0);

    fireEvent.click(buyButtons[0]);

    await waitFor(
      () => {
        expect(defaultProps.onExecuteBuyBundle).toHaveBeenCalledTimes(1);
      },
      { timeout: 2000 }
    );
  });
});
