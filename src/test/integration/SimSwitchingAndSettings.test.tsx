import React from 'react';
import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { SecurityProvider } from '../../components/BiometricProvider';
import App from '../../App';

describe('Integration Test: Dual-SIM Switching & Settings Flow', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('switches between SIM 1 and SIM 2 using top navigation selector', async () => {
    render(
      <SecurityProvider>
        <App />
      </SecurityProvider>
    );

    const sim1Btn = screen.getByRole('button', { name: /SIM 1/i });
    const sim2Btn = screen.getByRole('button', { name: /SIM 2/i });

    expect(sim1Btn).toBeInTheDocument();
    expect(sim2Btn).toBeInTheDocument();

    fireEvent.click(sim2Btn);
    await waitFor(() => {
      expect(sim2Btn.className).toContain('bg-slate-700');
    });

    fireEvent.click(sim1Btn);
    await waitFor(() => {
      expect(sim1Btn.className).toContain('bg-slate-700');
    });
  });

  it('allows language change to Amharic and Afaan Oromoo in Settings', async () => {
    render(
      <SecurityProvider>
        <App />
      </SecurityProvider>
    );

    // Go to Settings
    const settingsTab = screen.getByRole('button', { name: /Settings/i });
    fireEvent.click(settingsTab);

    // Switch to Amharic
    const amharicBtn = screen.getByText('አማርኛ');
    fireEvent.click(amharicBtn);

    // Navigate to Home tab
    const homeTab = screen.getByRole('button', { name: /መነሻ/i });
    fireEvent.click(homeTab);

    // Check Amharic label for Net Balance
    await waitFor(() => {
      expect(screen.getByText(/የተጣራ ቀሪ ሂሳብ/i)).toBeInTheDocument();
    });
  });
});
