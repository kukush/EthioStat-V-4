import React from 'react';
import { cn } from '@/lib/utils';
import { Smartphone, ArrowLeftRight, Settings, Home, Signal, Gift } from 'lucide-react';
import { motion } from 'framer-motion';
import { useTranslation } from '@/translations';
import { Language } from '@/types';

interface BottomNavProps {
  activeTab: 'home' | 'telecom' | 'transactions' | 'settings';
  onTabChange: (tab: 'home' | 'telecom' | 'transactions' | 'settings') => void;
  pendingGiftsCount?: number;
  language: Language;
}

export const BottomNav: React.FC<BottomNavProps> = ({ activeTab, onTabChange, pendingGiftsCount = 0, language }) => {
  const t = useTranslation(language);
  const tabs = [
    { id: 'home', label: t('home'), icon: Home, activeColor: 'text-slate-900 bg-slate-100' },
    { id: 'telecom', label: t('telecom'), icon: Gift, activeColor: 'text-blue-600 bg-blue-50 border-blue-100' },
    { id: 'transactions', label: t('historyTab'), icon: ArrowLeftRight, activeColor: 'text-green-600 bg-green-50 border-green-100' },
    { id: 'settings', label: t('settingsTab'), icon: Settings, activeColor: 'text-slate-900 bg-slate-100' },
  ] as const;

  return (
    <nav className="fixed bottom-0 w-full z-50 bg-white/90 backdrop-blur-xl border-t border-slate-100 px-4 pb-8 pt-3 flex justify-around items-center shadow-[0_-10px_40px_rgba(0,0,0,0.05)]">
      {tabs.map((tab) => {
        const Icon = tab.icon;
        const isActive = activeTab === tab.id;
        const hasBadge = tab.id === 'telecom' && pendingGiftsCount > 0;
        
        return (
          <button
            key={tab.id}
            onClick={() => onTabChange(tab.id)}
            className={cn(
              'flex flex-col items-center gap-1 transition-all duration-300 relative px-4 py-2 rounded-2xl border border-transparent',
              isActive ? cn('scale-110 shadow-sm', tab.activeColor) : 'text-slate-400 hover:text-slate-600'
            )}
          >
            <div className="relative">
              <Icon size={22} strokeWidth={isActive ? 2.5 : 2} />
              {hasBadge && (
                <span className="absolute -top-1.5 -right-1.5 w-4 h-4 bg-red-500 text-white text-[8px] font-black rounded-full flex items-center justify-center border-2 border-white shadow-sm">
                  {pendingGiftsCount}
                </span>
              )}
            </div>
            <span className="text-[9px] font-black uppercase tracking-widest">
              {tab.label}
            </span>
            {isActive && (
              <motion.div 
                layoutId="activeTabIndicator"
                className="absolute -top-3 w-1 h-1 bg-current rounded-full"
              />
            )}
          </button>
        );
      })}
    </nav>
  );
};
