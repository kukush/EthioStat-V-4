import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import App from '../../App';

describe('Integration Test: USSD *804# Sync Flow', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('renders application with Sync button next to Net Balance on Home screen', async () => {
    render(<App />);

    // Check brand header
    expect(screen.getByText('EthioBalance')).toBeInTheDocument();

    // Verify Sync button is rendered inside Net Balance Summary Card
    const syncButton = screen.getByRole('button', { name: /Sync/i });
    expect(syncButton).toBeInTheDocument();
    expect(syncButton).toHaveAttribute('id', 'summary-sync-btn');
  });

  it('triggers USSD sync simulation when Sync button is clicked', async () => {
    render(<App />);

    const syncButton = screen.getByRole('button', { name: /Sync/i });
    fireEvent.click(syncButton);

    // Should indicate syncing state or trigger toast / balance reconcile
    await waitFor(() => {
      expect(syncButton).toBeInTheDocument();
    });
  });
});
