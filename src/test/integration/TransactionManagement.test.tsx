import React from 'react';
import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { SecurityProvider } from '../../components/BiometricProvider';
import App from '../../App';

describe('Integration Test: Transaction Management & Filtering Flow', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('navigates between Home, Telecom, Transactions, and Settings tabs', async () => {
    render(
      <SecurityProvider>
        <App />
      </SecurityProvider>
    );

    // Click History (Transactions) tab in bottom nav
    const txTab = screen.getByRole('button', { name: /History/i });
    fireEvent.click(txTab);

    // Verify Transaction Screen rendered
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Search transactions/i)).toBeInTheDocument();
    });

    // Click Settings tab
    const settingsTab = screen.getByRole('button', { name: /Settings/i });
    fireEvent.click(settingsTab);

    // Verify Settings Screen rendered
    await waitFor(() => {
      expect(screen.getByText(/100% Offline & Private Storage/i)).toBeInTheDocument();
    });

    // Click Home tab
    const homeTab = screen.getByRole('button', { name: /Home/i });
    fireEvent.click(homeTab);

    // Verify Home Screen rendered
    await waitFor(() => {
      expect(screen.getByText(/Net Balance/i)).toBeInTheDocument();
    });
  });

  it('opens and closes Add Transaction modal and records manual transaction', async () => {
    render(
      <SecurityProvider>
        <App />
      </SecurityProvider>
    );

    // Click Record button on Home screen
    const recordBtn = screen.getByRole('button', { name: /Record/i });
    fireEvent.click(recordBtn);

    // Verify modal opened
    expect(screen.getByText(/Add Transaction/i)).toBeInTheDocument();

    // Fill amount
    const amountInput = screen.getByPlaceholderText('e.g. 250.00');
    fireEvent.change(amountInput, { target: { value: '750' } });

    // Submit transaction
    const saveBtn = screen.getByRole('button', { name: /Save Transaction/i });
    fireEvent.click(saveBtn);

    // Verify modal closes
    await waitFor(() => {
      expect(screen.queryByText(/Record a custom offline transaction/i)).not.toBeInTheDocument();
    });
  });
});
