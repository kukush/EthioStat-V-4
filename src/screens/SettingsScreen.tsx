import React, { useState } from 'react';
import { Check, CheckCircle2, Cpu, Download, Globe, HelpCircle, Layers, Moon, Pencil, Phone, Plus, RefreshCw, RotateCcw, ShieldCheck, Smartphone, Sun, Terminal, Trash2, Wallet } from 'lucide-react';
import { BankInfo, Language, TelecomAssets, ThemeMode } from '../types';
import { t } from '../constants/translations';
import { APP_NAME, APP_VERSION, APP_TAGLINE } from '../constants/app';
import { BankEditModal } from '../components/BankEditModal';

interface SettingsScreenProps {
  language: Language;
  setLanguage: (lang: Language) => void;
  theme: ThemeMode;
  setTheme: (theme: ThemeMode) => void;
  banks: BankInfo[];
  onToggleBank: (bankId: string) => void;
  onAddBank: (bank: BankInfo) => void;
  onEditBank: (bank: BankInfo) => void;
  onDeleteBank: (bankId: string) => void;
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
  onAddBank,
  onEditBank,
  onDeleteBank,
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
  const [isBankModalOpen, setIsBankModalOpen] = useState(false);
  const [bankToEdit, setBankToEdit] = useState<BankInfo | null>(null);
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);
  const [resetConfirm, setResetConfirm] = useState(false);

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
              <span className="text-[10px] text-slate-500 font-semibold">Enable, add or edit financial sources</span>
            </div>
          </div>
          <button
            onClick={() => {
              setBankToEdit(null);
              setIsBankModalOpen(true);
            }}
            className="px-2.5 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-500 text-white font-bold text-xs flex items-center gap-1 shadow-md transition-all active:scale-95"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Add Source</span>
          </button>
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
              className={`p-2.5 rounded-2xl border transition-all flex items-center justify-between ${
                bank.enabled
                  ? 'bg-slate-950 border-emerald-500/40'
                  : 'bg-slate-950/40 border-slate-800 opacity-60'
              }`}
            >
              <div
                onClick={() => onToggleBank(bank.id)}
                className="flex items-center gap-2 min-w-0 flex-1 cursor-pointer"
              >
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

              <div className="flex items-center gap-1.5 shrink-0">
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setBankToEdit(bank);
                    setIsBankModalOpen(true);
                  }}
                  title="Edit Source"
                  className="p-1.5 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
                >
                  <Pencil className="w-3.5 h-3.5" />
                </button>
                {deleteConfirmId === bank.id ? (
                  <div className="flex items-center gap-1">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onDeleteBank(bank.id);
                        setDeleteConfirmId(null);
                      }}
                      className="px-2 py-1.5 bg-rose-600 text-white text-[10px] font-bold rounded-lg"
                    >
                      Confirm
                    </button>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setDeleteConfirmId(null);
                      }}
                      className="px-2 py-1.5 bg-slate-800 text-slate-300 text-[10px] font-bold rounded-lg"
                    >
                      Cancel
                    </button>
                  </div>
                ) : (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setDeleteConfirmId(bank.id);
                    }}
                    title="Delete Source"
                    className="p-1.5 rounded-lg hover:bg-rose-950/40 text-slate-400 hover:text-rose-400 transition-colors"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                )}
                <div
                  onClick={() => onToggleBank(bank.id)}
                  className={`w-5 h-5 rounded-lg flex items-center justify-center cursor-pointer border transition-all ${
                    bank.enabled ? 'bg-emerald-600 border-emerald-500 text-white' : 'bg-slate-900 border-slate-700'
                  }`}
                >
                  {bank.enabled && <Check className="w-3 h-3" />}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      <BankEditModal
        isOpen={isBankModalOpen}
        onClose={() => setIsBankModalOpen(false)}
        language={language}
        bankToEdit={bankToEdit}
        onSaveBank={(savedBank) => {
          if (bankToEdit) {
            onEditBank(savedBank);
          } else {
            onAddBank(savedBank);
          }
        }}
      />

      {/* Android APK Build & Export Guide */}
      <div className="rounded-3xl bg-slate-900 border border-slate-800 p-4 shadow-xl space-y-3">
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
            <Download className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-xs font-bold uppercase tracking-widest text-slate-400">
              Android APK Build & Export
            </h3>
            <span className="text-[10px] text-slate-500 font-semibold">Generate native Android APK</span>
          </div>
        </div>

        <div className="p-3 rounded-2xl bg-slate-950/80 border border-slate-800 space-y-2 text-xs text-slate-300">
          <p className="text-[11px] text-slate-400">
            The native Android source code with Room DB, SMS Receiver, and Compose UI is located in <code className="text-emerald-400 font-mono text-[10px] bg-slate-900 px-1 py-0.5 rounded">/android</code>.
          </p>
          <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-800 font-mono text-[10px] text-emerald-400 select-all overflow-x-auto space-y-1">
            <div className="text-slate-500"># 1. Build fresh APK</div>
            <div>cd android && ./gradlew assembleDebug</div>
            <div className="text-slate-500 pt-1"># 2. Output location:</div>
            <div className="text-cyan-300">android/app/build/outputs/apk/debug/app-debug.apk</div>
          </div>
        </div>
      </div>

      {/* About App Info */}
      <div className="p-4 rounded-3xl bg-slate-900 border border-slate-800 flex items-center gap-3">
        <img
          src="/app-icon-512.png"
          alt={APP_NAME}
          className="w-10 h-10 rounded-2xl object-cover shadow-md shadow-emerald-500/20 border border-slate-700/60 shrink-0"
        />
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <span className="font-extrabold text-xs text-white">{APP_NAME}</span>
            <span className="text-[9px] px-1.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 font-bold border border-emerald-500/30">
              v{APP_VERSION} Offline
            </span>
          </div>
          <span className="text-[10px] text-slate-400 block mt-0.5">
            {APP_TAGLINE} for Ethiopia
          </span>
        </div>
      </div>

      {/* Reset Data */}
      <div className="p-3 rounded-2xl bg-slate-900 border border-slate-800 flex items-center justify-between">
        <span className="text-xs text-slate-400 font-semibold">Reset to defaults</span>
        {resetConfirm ? (
          <div className="flex items-center gap-1">
            <button
              onClick={() => {
                onResetData();
                setResetConfirm(false);
              }}
              className="px-2.5 py-1.5 rounded-xl bg-rose-600 text-white text-[10px] font-bold transition-colors"
            >
              Confirm
            </button>
            <button
              onClick={() => setResetConfirm(false)}
              className="px-2.5 py-1.5 rounded-xl bg-slate-800 text-slate-300 text-[10px] font-bold transition-colors"
            >
              Cancel
            </button>
          </div>
        ) : (
          <button
            onClick={() => setResetConfirm(true)}
            className="px-2.5 py-1.5 rounded-xl bg-rose-600/10 hover:bg-rose-600/20 border border-rose-500/30 text-rose-300 text-xs font-bold transition-colors flex items-center gap-1"
          >
            <RotateCcw className="w-3 h-3 text-rose-400" />
            <span>Reset</span>
          </button>
        )}
      </div>
    </div>
  );
};
