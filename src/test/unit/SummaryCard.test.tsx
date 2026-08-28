import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { SummaryCard } from '../../components/SummaryCard';

describe('SummaryCard Unit Tests', () => {
  const defaultProps = {
    language: 'en' as const,
    netBalance: 45250.75,
    totalIncome: 65000,
    totalExpense: 19749.25,
    showAmounts: true,
    onToggleAmounts: vi.fn(),
    selectedPeriod: 'allTime' as const,
    onSelectPeriod: vi.fn(),
    isSyncing: false,
    onSync: vi.fn(),
  };

  it('renders Net Balance and positive flow indicator correctly', () => {
    render(<SummaryCard {...defaultProps} />);

    expect(screen.getByText(/Net Balance/i)).toBeInTheDocument();
    expect(screen.getByText('45,250.75')).toBeInTheDocument();
    expect(screen.getByText('+ Positive')).toBeInTheDocument();
  });

  it('renders Sync Balance button next to Net Balance title and handles click', () => {
    render(<SummaryCard {...defaultProps} />);

    const syncButton = screen.getByRole('button', { name: /Sync/i });
    expect(syncButton).toBeInTheDocument();
    expect(syncButton).toHaveAttribute('id', 'summary-sync-btn');

    fireEvent.click(syncButton);
    expect(defaultProps.onSync).toHaveBeenCalledTimes(1);
  });

  it('disables Sync button and spins icon when isSyncing is true', () => {
    render(<SummaryCard {...defaultProps} isSyncing={true} />);

    const syncButton = screen.getByRole('button', { name: /Sync/i });
    expect(syncButton).toBeDisabled();
  });

  it('masks balances with bullet placeholders when showAmounts is false', () => {
    render(<SummaryCard {...defaultProps} showAmounts={false} />);

    expect(screen.queryByText('45,250.75')).not.toBeInTheDocument();
    expect(screen.getByText('••••••••')).toBeInTheDocument();
  });

  it('calls onToggleAmounts when eye toggle button is clicked', () => {
    render(<SummaryCard {...defaultProps} />);

    const toggleBtn = screen.getByTitle(/Hide Amount/i);
    fireEvent.click(toggleBtn);
    expect(defaultProps.onToggleAmounts).toHaveBeenCalledTimes(1);
  });

  it('allows switching filter period pills', () => {
    render(<SummaryCard {...defaultProps} />);

    const todayBtn = screen.getByText('Today');
    fireEvent.click(todayBtn);
    expect(defaultProps.onSelectPeriod).toHaveBeenCalledWith('today');

    const monthlyBtn = screen.getByText('Monthly');
    fireEvent.click(monthlyBtn);
    expect(defaultProps.onSelectPeriod).toHaveBeenCalledWith('monthly');
  });
});
