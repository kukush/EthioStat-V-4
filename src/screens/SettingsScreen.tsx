import React, { useState } from 'react';
import { Check, CheckCircle2, Cpu, Globe, HelpCircle, Layers, Moon, Phone, RefreshCw, RotateCcw, ShieldCheck, Smartphone, Sun, Trash2, Wallet } from 'lucide-react';
import { BankInfo, Language, TelecomAssets, ThemeMode } from '../types';
import { t } from '../constants/translations';

interface SettingsScreenProps {
  language: Language;
  setLanguage: (lang: Language) => void;
  theme: ThemeMode;
  setTheme: (theme: ThemeMode) => void;
  banks: BankInfo[];
  onToggleBank: (bankId: string) => void;
  telecomAssets: TelecomAssets;
  onUpdateTelecomAssets: (assets: TelecomAssets) => void;
  onOpenSmsSimulator: () => void;
  onOpenOnboarding: () => void;
  onResetData: () => void;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({
  language,
  setLanguage,
  theme,
  setTheme,
  banks,
  onToggleBank,
  telecomAssets,
  onUpdateTelecomAssets,
  onOpenSmsSimulator,
  onOpenOnboarding,
  onResetData,
}) => {
  const [sim1Phone, setSim1Phone] = useState(telecomAssets.sim1Number);
  const [sim2Phone, setSim2Phone] = useState(telecomAssets.sim2Number);
  const [savedPhoneNotice, setSavedPhoneNotice] = useState(false);
  const [searchBank, setSearchBank] = useState('');

  const handleSavePhones = (e: React.FormEvent) => {
    e.preventDefault();
    onUpdateTelecomAssets({
      ...telecomAssets,
      sim1Number: sim1Phone,
      sim2Number: sim2Phone,
    });
    setSavedPhoneNotice(true);
    setTimeout(() => setSavedPhoneNotice(false), 2500);
  };

  const filteredBanks = banks.filter(
    (b) =>
      b.displayName.toLowerCase().includes(searchBank.toLowerCase()) ||
      b.fullName.toLowerCase().includes(searchBank.toLowerCase()) ||
      b.abbreviation.toLowerCase().includes(searchBank.toLowerCase())
  );

  return (
    <div className="space-y-4 animate-fadeIn pb-16">
      {/* Top Banner: Offline Privacy Guarantee */}
      <div className="p-4 rounded-3xl bg-slate-900 border border-slate-800 shadow-xl flex items-start gap-3">
        <div className="p-2 rounded-2xl bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 shrink-0">
          <ShieldCheck className="w-5 h-5" />
        </div>
        <div className="space-y-0.5">
          <h3 className="font-extrabold text-xs text-white">100% Offline & Private Storage</h3>
          <p className="text-[11px] text-slate-400 leading-relaxed">
            {t(language, 'offlineNotice')} All financial transactions and SMS reconciliations occur strictly on this device.
          </p>
        </div>
      </div>

      {/* Developer Sandbox & Guide */}
      <div className="rounded-3xl bg-slate-900 border border-slate-800 p-4 shadow-xl space-y-3">
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-xl bg-cyan-500/10 border border-cyan-500/20 text-cyan-400">
            <Cpu className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-xs font-bold uppercase tracking-widest text-slate-400">
              Simulation Sandbox & Guide
            </h3>
            <span className="text-[10px] text-slate-500 font-semibold">SMS parser sandbox & application tour</span>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-2">
          <button
            id="settings-sms-simulator-btn"
            onClick={onOpenSmsSimulator}
            className="p-3 rounded-2xl bg-purple-600/10 hover:bg-purple-600/20 border border-purple-500/30 text-left transition-colors flex flex-col justify-between"
          >
            <Cpu className="w-4 h-4 text-purple-400 mb-1.5" />
            <div>
              <span className="font-bold text-xs text-white block">SMS Simulator</span>
              <span className="text-[9px] text-slate-400 block mt-0.5">Test regex parser</span>
            </div>
          </button>

          <button
            id="settings-walkthrough-btn"
            onClick={onOpenOnboarding}
            className="p-3 rounded-2xl bg-blue-600/10 hover:bg-blue-600/20 border border-blue-500/30 text-left transition-colors flex flex-col justify-between"
          >
            <HelpCircle className="w-4 h-4 text-blue-400 mb-1.5" />
            <div>
              <span className="font-bold text-xs text-white block">App Walkthrough</span>
              <span className="text-[9px] text-slate-400 block mt-0.5">Architecture tour</span>
            </div>
          </button>
        </div>
      </div>

      {/* Language & Appearance */}
      <div className="rounded-3xl bg-slate-900 border border-slate-800 p-4 shadow-xl space-y-3">
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-xl bg-purple-500/10 border border-purple-500/20 text-purple-400">
            <Globe className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-xs font-bold uppercase tracking-widest text-slate-400">
              {t(language, 'language')} & {t(language, 'theme')}
            </h3>
            <span className="text-[10px] text-slate-500 font-semibold">Customization Preferences</span>
          </div>
        </div>

        {/* Language Options */}
        <div className="space-y-1.5">
          <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider">
            Select Language
          </label>
          <div className="grid grid-cols-3 gap-1.5">
            {[
              { id: 'en', label: 'English', desc: 'US' },
              { id: 'am', label: 'አማርኛ', desc: 'ET' },
              { id: 'om', label: 'Oromoo', desc: 'ET' },
            ].map((item) => (
              <button
                key={item.id}
                onClick={() => setLanguage(item.id as Language)}
                className={`p-2.5 rounded-xl border text-center transition-all ${
                  language === item.id
                    ? 'bg-emerald-600/20 border-emerald-500 text-white shadow-md'
                    : 'bg-slate-950/60 border-slate-800 text-slate-400 hover:border-slate-700 hover:text-slate-200'
                }`}
              >
                <div className="font-extrabold text-xs text-white">{item.label}</div>
                <span className="text-[9px] text-slate-500 block mt-0.5">{item.desc}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Theme Options */}
        <div className="pt-2 border-t border-slate-800 space-y-1.5">
          <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider">
            Theme Mode
          </label>
          <div className="grid grid-cols-3 gap-1.5">
            <button
              onClick={() => setTheme('dark')}
              className={`p-2 rounded-xl border font-bold text-xs flex items-center justify-center gap-1 transition-all ${
                theme === 'dark'
                  ? 'bg-slate-800 border-slate-600 text-white'
                  : 'bg-slate-950/60 border-slate-800 text-slate-400'
              }`}
            >
              <Moon className="w-3 h-3 text-slate-300" />
              <span>{t(language, 'dark')}</span>
            </button>
            <button
              onClick={() => setTheme('light')}
              className={`p-2 rounded-xl border font-bold text-xs flex items-center justify-center gap-1 transition-all ${
                theme === 'light'
                  ? 'bg-slate-800 border-slate-600 text-white'
                  : 'bg-slate-950/60 border-slate-800 text-slate-400'
              }`}
            >
              <Sun className="w-3 h-3 text-amber-400" />
              <span>{t(language, 'light')}</span>
            </button>
            <button
              onClick={() => setTheme('vibrant')}
              className={`p-2 rounded-xl border font-bold text-xs flex items-center justify-center gap-1 transition-all ${
                theme === 'vibrant'
                  ? 'bg-purple-900/60 border-purple-500 text-white'
                  : 'bg-slate-950/60 border-slate-800 text-slate-400'
              }`}
            >
              <Layers className="w-3 h-3 text-purple-400" />
              <span>{t(language, 'vibrant')}</span>
            </button>
          </div>
        </div>
      </div>

      {/* SIM Configuration */}
      <div className="rounded-3xl bg-slate-900 border border-slate-800 p-4 shadow-xl space-y-3">
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
            <Smartphone className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-xs font-bold uppercase tracking-widest text-slate-400">
              SIM Slot Configuration
            </h3>
            <span className="text-[10px] text-slate-500 font-semibold">Dual-SIM MSISDN bindings</span>
          </div>
        </div>

        <form onSubmit={handleSavePhones} className="space-y-2.5">
          <div className="space-y-2">
            <div>
              <label className="block text-[10px] font-bold uppercase text-slate-400 mb-1">
                SIM 1 (Ethio Telecom)
              </label>
              <input
                id="sim1-phone-input"
                type="text"
                value={sim1Phone}
                onChange={(e) => setSim1Phone(e.target.value)}
                placeholder="0911223344"
                className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs font-mono focus:outline-none focus:ring-1 focus:ring-emerald-500"
              />
            </div>

            <div>
              <label className="block text-[10px] font-bold uppercase text-slate-400 mb-1">
                SIM 2 (Safaricom / Alternative)
              </label>
              <input
                id="sim2-phone-input"
                type="text"
                value={sim2Phone}
                onChange={(e) => setSim2Phone(e.target.value)}
                placeholder="0711223344"
                className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs font-mono focus:outline-none focus:ring-1 focus:ring-emerald-500"
              />
            </div>
          </div>

          <div className="flex items-center justify-between pt-1">
            {savedPhoneNotice ? (
              <span className="text-[11px] font-bold text-emerald-400 flex items-center gap-1">
                <Check className="w-3 h-3" />
                Updated!
              </span>
            ) : (
              <span />
            )}
            <button
              id="save-sim-phones-btn"
              type="submit"
              className="px-3 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 font-bold text-xs text-white shadow-md transition-all active:scale-95"
            >
              Save SIM Info
            </button>
          </div>
        </form>
      </div>

      {/* 25+ Ethiopian Bank Sources Manager */}
      <div className="rounded-3xl bg-slate-900 border border-slate-800 p-4 shadow-xl space-y-3">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-400">
              <Wallet className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-xs font-bold uppercase tracking-widest text-slate-400">
                {t(language, 'transactionSources')} ({banks.filter((b) => b.enabled).length}/{banks.length})
              </h3>
              <span className="text-[10px] text-slate-500 font-semibold">Enable or disable bank tracking</span>
            </div>
          </div>
        </div>

        <input
          type="text"
          placeholder="Search banks (CBE, Awash, Telebirr...)"
          value={searchBank}
          onChange={(e) => setSearchBank(e.target.value)}
          className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs placeholder:text-slate-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
        />

        <div className="space-y-1.5 max-h-72 overflow-y-auto pr-1">
          {filteredBanks.map((bank) => (
            <div
              key={bank.id}
              onClick={() => onToggleBank(bank.id)}
              className={`p-2.5 rounded-2xl border transition-all cursor-pointer flex items-center justify-between ${
                bank.enabled
                  ? 'bg-slate-950 border-emerald-500/40 hover:border-emerald-500'
                  : 'bg-slate-950/40 border-slate-800 opacity-60'
              }`}
            >
              <div className="flex items-center gap-2 min-w-0">
                <div
                  className={`w-7 h-7 rounded-xl bg-gradient-to-tr ${bank.color} flex items-center justify-center text-white font-extrabold text-[9px] shrink-0`}
                >
                  {bank.abbreviation.slice(0, 3)}
                </div>
                <div className="min-w-0">
                  <span className="font-bold text-xs text-white block truncate">{bank.displayName}</span>
                  <span className="text-[9px] text-slate-400 font-mono block truncate">{bank.fullName}</span>
                </div>
              </div>

              <div
                className={`w-5 h-5 rounded-lg flex items-center justify-center shrink-0 border transition-all ${
                  bank.enabled ? 'bg-emerald-600 border-emerald-500 text-white' : 'bg-slate-900 border-slate-700'
                }`}
              >
                {bank.enabled && <Check className="w-3 h-3" />}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Reset Data */}
      <div className="p-3 rounded-2xl bg-slate-900 border border-slate-800 flex items-center justify-between">
        <span className="text-xs text-slate-400 font-semibold">Reset to defaults</span>
        <button
          onClick={() => {
            if (window.confirm('Reset all transactions, packages, and custom bank balances to default?')) {
              onResetData();
            }
          }}
          className="px-2.5 py-1.5 rounded-xl bg-rose-600/10 hover:bg-rose-600/20 border border-rose-500/30 text-rose-300 text-xs font-bold transition-colors flex items-center gap-1"
        >
          <RotateCcw className="w-3 h-3 text-rose-400" />
          <span>Reset</span>
        </button>
      </div>
    </div>
  );
};
