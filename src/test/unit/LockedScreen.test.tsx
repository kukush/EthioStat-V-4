import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { LockedScreen } from '../../screens/LockedScreen';
import { SecurityProvider } from '../../components/BiometricProvider';

describe('LockedScreen Unit Tests', () => {
  it('renders correctly', () => {
    render(
      <SecurityProvider>
        <LockedScreen language="en" />
      </SecurityProvider>
    );
    expect(screen.getByAltText(/EthioBalance Logo/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Set PIN/i)).toBeInTheDocument();
  });

  it('allows setting a PIN', () => {
    render(
      <SecurityProvider>
        <LockedScreen language="en" />
      </SecurityProvider>
    );
    const pinInput = screen.getByPlaceholderText(/Set PIN/i);
    fireEvent.change(pinInput, { target: { value: '1234' } });
    expect(pinInput).toHaveValue('1234');
    
    const unlockBtn = screen.getByRole('button', { name: /Set PIN/i });
    fireEvent.click(unlockBtn);
    // Should be unlocked now.
  });
});
