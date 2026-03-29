import React from 'react';
import { motion } from 'framer-motion';
import { cn } from '@/lib/utils';
import { useTranslation } from '@/translations';
import { Language } from '@/types';

interface PackageCardProps {
  type: 'internet' | 'voice' | 'sms' | 'bonus';
  value: number;
  total: number;
  unit: string;
  label: string;
  expiry: string;
  daysLeft?: number;
  totalDays?: number;
  language: Language;
  className?: string;
}

export const PackageCard: React.FC<PackageCardProps> = ({
  type, value, total, unit, label, expiry, daysLeft = 1, totalDays = 1, language, className
}) => {
  const t = useTranslation(language);
  const percentage = total > 0 ? Math.min(100, (value / total) * 100) : 0;
  const expiryPercentage = (totalDays || 1) > 0 ? Math.min(100, ((daysLeft || 0) / (totalDays || 1)) * 100) : 0;
  const radius = 36;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (percentage / 100) * circumference;
  
  const themes = {
    internet: {
      bg: 'bg-blue-600',
      text: 'text-white',
      progressBg: 'stroke-blue-400/30',
      progressFg: 'stroke-white',
      accent: 'text-blue-100',
      barBg: 'bg-blue-400/30',
      barFg: 'bg-white',
    },
    voice: {
      bg: 'bg-green-600',
      text: 'text-white',
      progressBg: 'stroke-green-400/30',
      progressFg: 'stroke-white',
      accent: 'text-green-100',
      barBg: 'bg-green-400/30',
      barFg: 'bg-white',
    },
    sms: {
      bg: 'bg-purple-600',
      text: 'text-white',
      progressBg: 'stroke-purple-400/30',
      progressFg: 'stroke-white',
      accent: 'text-purple-100',
      barBg: 'bg-purple-400/30',
      barFg: 'bg-white',
    },
    bonus: {
      bg: 'bg-amber-500',
      text: 'text-white',
      progressBg: 'stroke-amber-300/30',
      progressFg: 'stroke-white',
      accent: 'text-amber-50',
      barBg: 'bg-amber-300/30',
      barFg: 'bg-white',
    },
  };

  const theme = themes[type as keyof typeof themes] || themes.internet;

  // Try to translate the label if it matches a translation key
  const translatedLabel = label === 'Daily Data' ? t('daily') + ' ' + t('data') :
                         label === 'Night Voice' ? t('daily') + ' ' + t('voice') :
                         label === 'Weekly SMS' ? t('weekly') + ' ' + t('sms') :
                         label === 'Monthly Data' ? t('monthlyData') :
                         label === 'Weekly Voice' ? t('weeklyVoice') : label;

  return (
    <motion.div 
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className={cn(
        'p-5 rounded-[2rem] shadow-xl relative overflow-hidden flex items-center justify-between',
        theme.bg,
        theme.text,
        className
      )}
    >
      <div className="space-y-1 z-10 flex-1">
        <p className={cn("text-[10px] font-bold uppercase tracking-[0.2em] opacity-80", theme.accent)}>
          {t(type as any)}
        </p>
        <h3 className="text-xl font-black">{translatedLabel}</h3>
        <div className="flex items-baseline gap-1 mt-2">
          <span className="text-3xl font-black">{value}</span>
          <span className="text-sm font-bold opacity-80">{unit}</span>
        </div>
        
        {/* Expiry Progress Bar */}
        <div className="mt-4 space-y-2">
          <div className="flex justify-between items-end">
            <span className="text-[9px] font-black uppercase tracking-[0.2em] opacity-60">{t('validity') || 'Validity'}</span>
            <span className="text-[10px] font-black">{daysLeft} / {totalDays} {t('daysLeft')}</span>
          </div>
          <div className={cn("h-2 w-full rounded-full overflow-hidden p-0.5", theme.barBg)}>
            <motion.div 
              initial={{ width: 0 }}
              animate={{ width: `${expiryPercentage}%` }}
              transition={{ duration: 1, ease: "easeOut" }}
              className={cn("h-full rounded-full shadow-sm", theme.barFg)}
            />
          </div>
          <div className="flex justify-between items-center">
            <p className="text-[8px] font-bold uppercase tracking-widest opacity-60">
              {t('expires')}: {
                !isNaN(Number(expiry)) 
                  ? new Date(Number(expiry)).toLocaleDateString(language === 'en' ? 'en-US' : language === 'am' ? 'am-ET' : 'om-ET', { month: 'short', day: 'numeric', year: 'numeric' })
                  : expiry
              }
            </p>
            <div className="flex gap-1">
              {[...Array(5)].map((_, i) => (
                <div 
                  key={i} 
                  className={cn(
                    "w-1 h-1 rounded-full", 
                    i < Math.ceil((expiryPercentage / 100) * 5) ? theme.barFg : "bg-white/20"
                  )} 
                />
              ))}
            </div>
          </div>
        </div>
      </div>

      <div className="relative flex items-center justify-center z-10 ml-4">
        <svg className="w-24 h-24 transform -rotate-90">
          <circle
            cx="48"
            cy="48"
            r={radius}
            className={cn("fill-none stroke-[8]", theme.progressBg)}
          />
          <motion.circle
            cx="48"
            cy="48"
            r={radius}
            className={cn("fill-none stroke-[8] stroke-round", theme.progressFg)}
            initial={{ strokeDashoffset: circumference }}
            animate={{ strokeDashoffset: offset }}
            transition={{ duration: 1.5, ease: "easeOut" }}
            style={{
              strokeDasharray: circumference,
            }}
          />
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-sm font-black">{Math.round(percentage)}%</span>
          <span className="text-[8px] font-bold uppercase opacity-60">{t('left') || 'Left'}</span>
        </div>
      </div>

      {/* Decorative background element */}
      <div className="absolute -right-4 -top-4 w-24 h-24 bg-white/10 rounded-full blur-2xl" />
    </motion.div>
  );
};
