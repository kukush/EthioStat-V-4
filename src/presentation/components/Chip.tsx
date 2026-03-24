import React from 'react';
import { cn } from '@/lib/utils';

interface ChipProps {
  label: string;
  variant?: 'internet' | 'voice' | 'sms' | 'bonus' | 'default';
  className?: string;
}

export const Chip: React.FC<ChipProps> = ({ label, variant = 'default', className }) => {
  const variants = {
    internet: 'bg-blue-100 text-blue-700 border-blue-200',
    voice: 'bg-green-100 text-green-700 border-green-200',
    sms: 'bg-purple-100 text-purple-700 border-purple-200',
    bonus: 'bg-amber-100 text-amber-700 border-amber-200',
    default: 'bg-slate-100 text-slate-700 border-slate-200',
  };

  return (
    <span className={cn(
      'px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider border',
      variants[variant],
      className
    )}>
      {label}
    </span>
  );
};
