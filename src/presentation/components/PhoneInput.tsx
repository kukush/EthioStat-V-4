import React, { useCallback } from 'react';
import { cn } from '@/lib/utils';

interface PhoneInputProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  className?: string;
  error?: string;
}

/**
 * Ethiopian phone number input with 🇪🇹 +251 prefix.
 * - Strips leading '0' or '+251' from user input
 * - Validates: must be 9 digits after prefix
 * - Always stores the raw 9-digit local number (e.g., '911223344')
 */
export const PhoneInput: React.FC<PhoneInputProps> = ({
  value,
  onChange,
  placeholder = '9XX XXX XXXX',
  className,
  error,
}) => {
  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      let raw = e.target.value.replace(/\D/g, '');

      // Strip country code if user pastes full number
      if (raw.startsWith('251') && raw.length > 9) {
        raw = raw.slice(3);
      }
      // Strip leading 0
      if (raw.startsWith('0') && raw.length > 1) {
        raw = raw.slice(1);
      }

      // Max 9 digits
      if (raw.length > 9) {
        raw = raw.slice(0, 9);
      }

      onChange(raw);
    },
    [onChange]
  );

  const isValid = value.length === 0 || value.length === 9;
  const hasError = error || (!isValid && value.length > 0);

  return (
    <div className={cn('space-y-1', className)}>
      <div
        className={cn(
          'flex items-center bg-slate-50 rounded-xl border transition-all overflow-hidden',
          hasError
            ? 'border-red-300 focus-within:ring-2 focus-within:ring-red-500/20'
            : 'border-slate-100 focus-within:ring-2 focus-within:ring-blue-500/20'
        )}
      >
        {/* Flag + prefix */}
        <div className="flex items-center gap-1.5 pl-3 pr-2 py-3 bg-slate-100/50 border-r border-slate-200 select-none">
          <span className="text-base leading-none">🇪🇹</span>
          <span className="text-xs font-black text-slate-600 tracking-wide">+251</span>
        </div>

        {/* Input */}
        <input
          type="tel"
          inputMode="numeric"
          value={value}
          onChange={handleChange}
          placeholder={placeholder}
          className="flex-1 px-3 py-3 bg-transparent text-sm font-bold text-slate-900 placeholder:text-slate-400 focus:outline-none"
          maxLength={12}
        />
      </div>

      {/* Error message */}
      {hasError && (
        <p className="text-[10px] font-bold text-red-500 px-1">
          {error || 'Phone number must be 9 digits'}
        </p>
      )}
    </div>
  );
};

/**
 * Format a raw phone number for display with +251 prefix.
 * Input: '911223344' → Output: '+251 911 223 344'
 * Input: '+251911223344' → Output: '+251 911 223 344'
 */
export function formatEthiopianPhone(phone: string): string {
  let digits = phone.replace(/\D/g, '');

  // Strip country code
  if (digits.startsWith('251') && digits.length > 9) {
    digits = digits.slice(3);
  }
  // Strip leading 0
  if (digits.startsWith('0')) {
    digits = digits.slice(1);
  }

  if (digits.length === 9) {
    return `+251 ${digits.slice(0, 3)} ${digits.slice(3, 6)} ${digits.slice(6)}`;
  }

  return phone; // Return as-is if can't parse
}

/**
 * Normalize phone to standard +251XXXXXXXXX format.
 */
export function normalizePhone(phone: string): string {
  let digits = phone.replace(/\D/g, '');

  if (digits.startsWith('251') && digits.length === 12) {
    return `+${digits}`;
  }
  if (digits.startsWith('0') && digits.length === 10) {
    return `+251${digits.slice(1)}`;
  }
  if (digits.length === 9) {
    return `+251${digits}`;
  }

  return phone;
}
