import React from 'react';
import { Activity, Clock, RefreshCw, Settings, ShieldCheck, Smartphone } from 'lucide-react';
import { Language, TelecomAssets } from '../types';
import { t } from '../constants/translations';

interface NavbarProps {
  activeTab: 'home' | 'telecom' | 'transactions' | 'settings';
  setActiveTab: (tab: 'home' | 'telecom' | 'transactions' | 'settings') => void;
  language: Language;
  telecomAssets: TelecomAssets;
  onSwitchSim: (sim: 1 | 2) => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  activeTab,
  setActiveTab,
  language,
  telecomAssets,
  onSwitchSim,
}) => {
  return (
    <header className="sticky top-0 z-30 bg-slate-900/95 backdrop-blur-md border-b border-slate-800">
      <div className="max-w-md mx-auto px-4 py-2.5 flex items-center justify-between gap-2">
        {/* Brand Logo & Title */}
        <div
          className="flex items-center gap-2 cursor-pointer select-none"
          onClick={() => setActiveTab('home')}
        >
          <div className="relative flex items-center justify-center w-8 h-8 rounded-xl bg-gradient-to-tr from-emerald-500 via-teal-500 to-cyan-500 shadow-md shadow-emerald-500/20 text-white font-black text-sm">
            EB
            <span className="absolute -bottom-0.5 -right-0.5 w-2 h-2 bg-emerald-400 rounded-full border-2 border-slate-900" />
          </div>
          <div>
            <div className="flex items-center gap-1">
              <h1 className="font-extrabold text-sm tracking-tight text-white">
                EthioBalance
              </h1>
              <span className="text-[8px] uppercase font-bold tracking-widest px-1 py-0.2 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                v1.1
              </span>
            </div>
            <div className="flex items-center gap-1 text-[10px] text-slate-400">
              <ShieldCheck className="w-2.5 h-2.5 text-emerald-400" />
              <span>Offline Ledger</span>
            </div>
          </div>
        </div>

        {/* Action Controls: Dual-SIM Selector */}
        <div className="flex items-center gap-1.5">
          <div className="flex items-center bg-slate-950/70 p-0.5 rounded-xl border border-slate-800 text-[11px] font-bold">
            <button
              id="sim-1-selector-btn"
              onClick={() => onSwitchSim(1)}
              className={`flex items-center gap-0.5 px-2 py-1 rounded-lg transition-all ${
                telecomAssets.activeSim === 1
                  ? 'bg-slate-700 text-white shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Smartphone className="w-3 h-3 text-emerald-400" />
              <span>SIM 1</span>
            </button>
            <button
              id="sim-2-selector-btn"
              onClick={() => onSwitchSim(2)}
              className={`flex items-center gap-0.5 px-2 py-1 rounded-lg transition-all ${
                telecomAssets.activeSim === 2
                  ? 'bg-slate-700 text-white shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Smartphone className="w-3 h-3 text-teal-400" />
              <span>SIM 2</span>
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};

export const BottomNavBar: React.FC<{
  activeTab: 'home' | 'telecom' | 'transactions' | 'settings';
  setActiveTab: (tab: 'home' | 'telecom' | 'transactions' | 'settings') => void;
  language: Language;
}> = ({ activeTab, setActiveTab, language }) => {
  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 bg-slate-900/95 backdrop-blur-lg border-t border-slate-800 px-2 py-1.5 flex items-center justify-around max-w-md mx-auto shadow-2xl">
      <button
        id="bottom-nav-home"
        onClick={() => setActiveTab('home')}
        className={`flex flex-col items-center gap-0.5 py-1 px-3 rounded-xl transition-all ${
          activeTab === 'home' ? 'text-emerald-400 font-bold' : 'text-slate-400 hover:text-slate-200'
        }`}
      >
        <Activity className="w-5 h-5" />
        <span className="text-[10px] tracking-tight">{t(language, 'home')}</span>
      </button>

      <button
        id="bottom-nav-telecom"
        onClick={() => setActiveTab('telecom')}
        className={`flex flex-col items-center gap-0.5 py-1 px-3 rounded-xl transition-all ${
          activeTab === 'telecom' ? 'text-emerald-400 font-bold' : 'text-slate-400 hover:text-slate-200'
        }`}
      >
        <Smartphone className="w-5 h-5" />
        <span className="text-[10px] tracking-tight">{t(language, 'telecom')}</span>
      </button>

      <button
        id="bottom-nav-history"
        onClick={() => setActiveTab('transactions')}
        className={`flex flex-col items-center gap-0.5 py-1 px-3 rounded-xl transition-all ${
          activeTab === 'transactions' ? 'text-emerald-400 font-bold' : 'text-slate-400 hover:text-slate-200'
        }`}
      >
        <Clock className="w-5 h-5" />
        <span className="text-[10px] tracking-tight">{t(language, 'transactions')}</span>
      </button>

      <button
        id="bottom-nav-settings"
        onClick={() => setActiveTab('settings')}
        className={`flex flex-col items-center gap-0.5 py-1 px-3 rounded-xl transition-all ${
          activeTab === 'settings' ? 'text-emerald-400 font-bold' : 'text-slate-400 hover:text-slate-200'
        }`}
      >
        <Settings className="w-5 h-5" />
        <span className="text-[10px] tracking-tight">{t(language, 'settings')}</span>
      </button>
    </nav>
  );
};
