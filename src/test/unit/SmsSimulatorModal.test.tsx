import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { SmsSimulatorModal } from '../../components/SmsSimulatorModal';

describe('SmsSimulatorModal Unit Tests', () => {
  const defaultProps = {
    isOpen: true,
    onClose: vi.fn(),
    language: 'en' as const,
    onApplyParsedSms: vi.fn(),
  };

  it('renders simulator modal and sample preset buttons', () => {
    render(<SmsSimulatorModal {...defaultProps} />);

    expect(screen.getByText('SMS Simulation & Parsing Sandbox')).toBeInTheDocument();
    expect(screen.getByText(/Preset Ethiopian Bank & Telecom Samples/i)).toBeInTheDocument();
  });

  it('allows clicking sample presets and running regex parser test', () => {
    render(<SmsSimulatorModal {...defaultProps} />);

    const parseBtn = screen.getByRole('button', { name: /Execute Regex Parser/i });
    fireEvent.click(parseBtn);

    expect(screen.getByText(/Parser Extraction Output/i)).toBeInTheDocument();
    expect(screen.getByText(/Confidence/i)).toBeInTheDocument();
  });

  it('commits parsed SMS and reconciles ledger', () => {
    render(<SmsSimulatorModal {...defaultProps} />);

    const reconcileBtn = screen.getByRole('button', { name: /Process & Reconcile Ledger/i });
    fireEvent.click(reconcileBtn);

    expect(defaultProps.onApplyParsedSms).toHaveBeenCalledTimes(1);
    expect(defaultProps.onClose).toHaveBeenCalledTimes(1);
  });
});
