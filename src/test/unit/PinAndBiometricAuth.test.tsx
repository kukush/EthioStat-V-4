import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { LockedScreen } from '../../screens/LockedScreen';
import { SecurityProvider, useSecurity } from '../../components/BiometricProvider';
import { BiometricService } from '../../services/BiometricService';

interface SecurityContextType {
  isLocked: boolean;
  unlockWithBiometric: () => Promise<void>;
  unlockWithPin: (pin: string) => boolean;
  lock: () => void;
  hasPin: boolean;
  setPin: (pin: string) => void;
  isBiometricEnabled: boolean;
  setIsBiometricEnabled: (enabled: boolean) => void;
  isPinEnabled: boolean;
  setIsPinEnabled: (enabled: boolean) => void;
  isAutoSyncEnabled: boolean;
  setIsAutoSyncEnabled: (enabled: boolean) => void;
}

const TestSecurityController = ({ onMount }: { onMount: (ctx: SecurityContextType) => void }) => {
  const security = useSecurity();
  React.useEffect(() => {
    onMount(security);
  }, [security, onMount]);
  return null;
};

describe('PIN and Biometric Authentication Unit & Integration Tests', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('handles PIN setup and unlock flow correctly', async () => {
    localStorage.setItem('pin_enabled', 'true');
    let getContext: () => SecurityContextType = () => ({} as SecurityContextType);

    const Controller = () => {
      const ctx = useSecurity();
      getContext = () => ctx;
      return null;
    };

    render(
      <SecurityProvider>
        <Controller />
        <LockedScreen language="en" />
      </SecurityProvider>
    );

    const pinInput = screen.getByPlaceholderText(/••••/i);
    fireEvent.change(pinInput, { target: { value: '1234' } });
    
    const setPinBtn = screen.getByRole('button', { name: /Set PIN/i });
    fireEvent.click(setPinBtn);

    await waitFor(() => {
      expect(getContext().hasPin).toBe(true);
      expect(getContext().isLocked).toBe(false);
    });
  });

  it('shows both PIN and Biometric when both enabled', () => {
    localStorage.setItem('pin_enabled', 'true');
    localStorage.setItem('biometric_enabled', 'true');

    render(
      <SecurityProvider>
        <LockedScreen language="en" />
      </SecurityProvider>
    );

    expect(screen.getByPlaceholderText(/••••/i)).toBeInTheDocument();
    expect(screen.getByText(/Use Biometric/i)).toBeInTheDocument();
  });

  it('shows only Biometric when PIN is disabled and Biometric enabled', () => {
    localStorage.setItem('pin_enabled', 'false');
    localStorage.setItem('biometric_enabled', 'true');

    render(
      <SecurityProvider>
        <LockedScreen language="en" />
      </SecurityProvider>
    );

    expect(screen.queryByPlaceholderText(/••••/i)).not.toBeInTheDocument();
    expect(screen.getByText(/Use Biometric/i)).toBeInTheDocument();
  });

  it('shows only PIN when Biometric is disabled and PIN enabled', () => {
    localStorage.setItem('pin_enabled', 'true');
    localStorage.setItem('biometric_enabled', 'false');

    render(
      <SecurityProvider>
        <LockedScreen language="en" />
      </SecurityProvider>
    );

    expect(screen.getByPlaceholderText(/••••/i)).toBeInTheDocument();
    expect(screen.queryByText(/Use Biometric/i)).not.toBeInTheDocument();
  });

  it('triggers biometric authentication successfully', async () => {
    localStorage.setItem('biometric_enabled', 'true');
    vi.spyOn(BiometricService, 'authenticate').mockResolvedValueOnce(true);
    let getContext: () => SecurityContextType = () => ({} as SecurityContextType);

    const Controller = () => {
      const ctx = useSecurity();
      getContext = () => ctx;
      return null;
    };

    render(
      <SecurityProvider>
        <Controller />
        <LockedScreen language="en" />
      </SecurityProvider>
    );

    const bioBtn = screen.getByText(/Use Biometric/i);
    fireEvent.click(bioBtn);

    await waitFor(() => {
      expect(getContext().isLocked).toBe(false);
    });
  });
});
