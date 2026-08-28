import React, { useState } from 'react';
import {
  BalancePackageEntity,
  BankInfo,
  FilterPeriod,
  Language,
  ParsedSmsResult,
  RecommendedBundle,
  TelecomAssets,
  ThemeMode,
  TransactionEntity,
} from './types';
import {
  hasCompletedOnboarding,
  loadBanks,
  loadLanguage,
  loadPackages,
  loadTelecomAssets,
  loadTheme,
  loadTransactions,
  resetAllData,
  saveBanks,
  saveLanguage,
  savePackages,
  saveTelecomAssets,
  saveTheme,
  saveTransactions,
  setCompletedOnboarding,
} from './services/storage';
import { INITIAL_BANKS } from './constants/banks';
import { INITIAL_PACKAGES, INITIAL_TELECOM_ASSETS, INITIAL_TRANSACTIONS } from './services/mockData';
import { Navbar, BottomNavBar } from './components/Navbar';
import { HomeScreen } from './screens/HomeScreen';
import { TelecomScreen } from './screens/TelecomScreen';
import { TransactionScreen } from './screens/TransactionScreen';
import { SettingsScreen } from './screens/SettingsScreen';
import { UssdModal } from './components/UssdModal';
import { SmsSimulatorModal } from './components/SmsSimulatorModal';
import { AddTransactionModal } from './components/AddTransactionModal';
import { OnboardingModal } from './components/OnboardingModal';

export const App: React.FC = () => {
  // Navigation & View state
  const [activeTab, setActiveTab] = useState<'home' | 'telecom' | 'transactions' | 'settings'>('home');
  const [language, setLanguageState] = useState<Language>(loadLanguage());
  const [theme, setThemeState] = useState<ThemeMode>(loadTheme());
  const [showAmounts, setShowAmounts] = useState<boolean>(true);
  const [selectedPeriod, setSelectedPeriod] = useState<FilterPeriod>('allTime');

  // Core Data States
  const [transactions, setTransactions] = useState<TransactionEntity[]>(loadTransactions());
  const [packages, setPackages] = useState<BalancePackageEntity[]>(loadPackages());
  const [telecomAssets, setTelecomAssets] = useState<TelecomAssets>(loadTelecomAssets());
  const [banks, setBanks] = useState<BankInfo[]>(loadBanks());

  // Modal States
  const [isUssdOpen, setIsUssdOpen] = useState(false);
  const [ussdInitialAction, setUssdInitialAction] = useState<'balance' | 'recharge' | 'transfer' | 'gift'>('balance');
  const [isSmsSimOpen, setIsSmsSimOpen] = useState(false);
  const [isAddTxOpen, setIsAddTxOpen] = useState(false);
  const [isOnboardingOpen, setIsOnboardingOpen] = useState(!hasCompletedOnboarding());
  const [isSyncing, setIsSyncing] = useState(false);

  // Persistence hooks
  const setLanguage = (lang: Language) => {
    setLanguageState(lang);
    saveLanguage(lang);
  };

  const setTheme = (t: ThemeMode) => {
    setThemeState(t);
    saveTheme(t);
  };

  const updateTransactions = (newTxs: TransactionEntity[]) => {
    setTransactions(newTxs);
    saveTransactions(newTxs);
  };

  const updatePackages = (newPkgs: BalancePackageEntity[]) => {
    setPackages(newPkgs);
    savePackages(newPkgs);
  };

  const updateTelecomAssets = (newAssets: TelecomAssets) => {
    setTelecomAssets(newAssets);
    saveTelecomAssets(newAssets);
  };

  const updateBanks = (newBanks: BankInfo[]) => {
    setBanks(newBanks);
    saveBanks(newBanks);
  };

  // USSD Triggers & Reconciliations
  const handleOpenUssd = (action: 'balance' | 'recharge' | 'transfer' | 'gift' = 'balance') => {
    setUssdInitialAction(action);
    setIsUssdOpen(true);
  };

  const handleExecuteSync = () => {
    setIsSyncing(true);
    setTimeout(() => {
      // Refresh package quotas and update last synced timestamp
      const refreshedPkgs = packages.map((pkg) => ({
        ...pkg,
        lastUpdated: Date.now(),
      }));
      updatePackages(refreshedPkgs);
      updateTelecomAssets({
        ...telecomAssets,
        lastSyncedAt: Date.now(),
      });
      setIsSyncing(false);
    }, 1200);
  };

  const handleExecuteRecharge = (voucher: string) => {
    // Standard voucher value assumption (e.g. 50 or 100 ETB)
    const rechargeAmt = voucher.startsWith('8') ? 100 : 50;
    const newAirtime = telecomAssets.airtimeBalance + rechargeAmt;

    updateTelecomAssets({
      ...telecomAssets,
      airtimeBalance: newAirtime,
      lastSyncedAt: Date.now(),
    });

    // Record transaction
    const rechargeTx: TransactionEntity = {
      id: `tx-rech-${Date.now()}`,
      amount: rechargeAmt,
      currency: 'ETB',
      source: 'AIRTIME',
      type: 'INCOME',
      category: 'RECHARGE',
      recipientOrSender: 'Voucher Recharge (*805*)',
      reference: `REC-${voucher.slice(-6)}`,
      timestamp: Date.now(),
      simSlot: telecomAssets.activeSim,
      balanceAfter: newAirtime,
      rawSmsBody: `Your account has been recharged with ${rechargeAmt}.00 ETB. Your new airtime balance is ${newAirtime.toFixed(
        2
      )} ETB.`,
    };

    updateTransactions([rechargeTx, ...transactions]);
  };

  const handleExecuteTransfer = (recipientPhone: string, amount: number) => {
    const newAirtime = Math.max(0, telecomAssets.airtimeBalance - amount);
    updateTelecomAssets({
      ...telecomAssets,
      airtimeBalance: newAirtime,
      lastSyncedAt: Date.now(),
    });

    const transferTx: TransactionEntity = {
      id: `tx-trans-${Date.now()}`,
      amount: amount,
      currency: 'ETB',
      source: 'AIRTIME',
      type: 'TRANSFER',
      category: 'TRANSFER',
      recipientOrSender: recipientPhone,
      reference: `P2P-${Date.now().toString().slice(-6)}`,
      timestamp: Date.now(),
      simSlot: telecomAssets.activeSim,
      balanceAfter: newAirtime,
      rawSmsBody: `You transferred ${amount}.00 ETB to ${recipientPhone} via *806*. Available airtime balance is ${newAirtime.toFixed(
        2
      )} ETB.`,
    };

    updateTransactions([transferTx, ...transactions]);
  };

  const handleBuyBundle = (bundle: RecommendedBundle) => {
    // Deduct from airtime or telebirr if enough airtime
    const newAirtime = Math.max(0, telecomAssets.airtimeBalance - bundle.priceEtb);
    updateTelecomAssets({
      ...telecomAssets,
      airtimeBalance: newAirtime,
      lastSyncedAt: Date.now(),
    });

    // Add or boost active package
    const existingIdx = packages.findIndex((p) => p.type === bundle.type && p.subType.toLowerCase() === bundle.duration);
    let updatedPkgs = [...packages];

    const expiryDays = bundle.duration === 'daily' || bundle.duration === 'night' ? 1 : bundle.duration === 'weekly' ? 7 : 30;

    if (existingIdx >= 0) {
      updatedPkgs[existingIdx] = {
        ...updatedPkgs[existingIdx],
        remainingAmount: updatedPkgs[existingIdx].remainingAmount + bundle.amount,
        totalAmount: updatedPkgs[existingIdx].totalAmount + bundle.amount,
        expiryDate: Date.now() + expiryDays * 24 * 60 * 60 * 1000,
        isActive: true,
        lastUpdated: Date.now(),
      };
    } else {
      updatedPkgs.push({
        id: `pkg-${bundle.id}-${Date.now()}`,
        type: bundle.type,
        subType: bundle.duration.charAt(0).toUpperCase() + bundle.duration.slice(1),
        totalAmount: bundle.amount,
        remainingAmount: bundle.amount,
        unit: bundle.unit,
        expiryDate: Date.now() + expiryDays * 24 * 60 * 60 * 1000,
        isActive: true,
        source: 'SMS',
        simId: 'SIM1',
        lastUpdated: Date.now(),
      });
    }

    updatePackages(updatedPkgs);

    // Record telecom expense transaction
    const pkgTx: TransactionEntity = {
      id: `tx-bundle-${Date.now()}`,
      amount: bundle.priceEtb,
      currency: 'ETB',
      source: 'AIRTIME',
      type: 'EXPENSE',
      category: 'TELECOM',
      recipientOrSender: `Ethio Telecom (${bundle.title})`,
      reference: `PKG-${Date.now().toString().slice(-6)}`,
      timestamp: Date.now(),
      simSlot: telecomAssets.activeSim,
      balanceAfter: newAirtime,
      rawSmsBody: `You have successfully purchased ${bundle.title} for ${bundle.priceEtb}.00 ETB. Valid for ${expiryDays} days.`,
    };

    updateTransactions([pkgTx, ...transactions]);
  };

  // Live SMS Reconciliation Engine
  const handleApplyParsedSms = (parsed: ParsedSmsResult, rawBody: string, sender: string) => {
    // 1. If parsed contains packages, merge with package state
    if (parsed.packages && parsed.packages.length > 0) {
      const merged = [...packages];
      parsed.packages.forEach((newPkg) => {
        const idx = merged.findIndex((p) => p.type === newPkg.type && p.subType === newPkg.subType);
        if (idx >= 0) {
          merged[idx] = newPkg;
        } else {
          merged.push(newPkg);
        }
      });
      updatePackages(merged);
    }

    // 2. Update Airtime if detected
    if (parsed.airtimeBalance !== undefined) {
      updateTelecomAssets({
        ...telecomAssets,
        airtimeBalance: parsed.airtimeBalance,
        lastSyncedAt: Date.now(),
      });
    }

    // 3. Reconcile bank balances
    if (parsed.balanceAfter !== undefined && parsed.source) {
      const updatedBanks = banks.map((b) => {
        if (b.abbreviation.toUpperCase() === parsed.source.toUpperCase()) {
          return { ...b, currentBalance: parsed.balanceAfter! };
        }
        return b;
      });
      updateBanks(updatedBanks);
    }

    // 4. Record new transaction if amount exists
    if (parsed.amount && parsed.amount > 0 && parsed.type) {
      const newTx: TransactionEntity = {
        id: `tx-sms-${Date.now()}`,
        amount: parsed.amount,
        currency: 'ETB',
        source: parsed.source,
        type: parsed.type,
        category: parsed.category || 'GENERAL',
        recipientOrSender: parsed.partyName || parsed.source,
        reference: parsed.reference || `REF-${Date.now().toString().slice(-6)}`,
        timestamp: Date.now(),
        simSlot: telecomAssets.activeSim,
        fee: parsed.fee,
        balanceAfter: parsed.balanceAfter,
        rawSmsBody: rawBody,
      };
      updateTransactions([newTx, ...transactions]);
    }
  };

  const handleToggleBank = (bankId: string) => {
    const updated = banks.map((b) => (b.id === bankId ? { ...b, enabled: !b.enabled } : b));
    updateBanks(updated);
  };

  const handleResetData = () => {
    resetAllData();
    setTransactions(INITIAL_TRANSACTIONS);
    setPackages(INITIAL_PACKAGES);
    setTelecomAssets(INITIAL_TELECOM_ASSETS);
    setBanks(INITIAL_BANKS);
  };

  const handleCloseOnboarding = () => {
    setIsOnboardingOpen(false);
    setCompletedOnboarding(true);
  };

  return (
    <div className={`min-h-screen ${theme === 'light' ? 'bg-slate-100 text-slate-900' : 'bg-slate-950 text-slate-100'}`}>
      {/* Top Header & Navigation */}
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        language={language}
        telecomAssets={telecomAssets}
        onSwitchSim={(sim) => updateTelecomAssets({ ...telecomAssets, activeSim: sim })}
      />

      {/* Main Content Area: Constrained purely as a clean mobile screen */}
      <main className="max-w-md mx-auto px-3.5 pt-3.5 pb-20">
        {activeTab === 'home' && (
          <HomeScreen
            language={language}
            transactions={transactions}
            packages={packages}
            telecomAssets={telecomAssets}
            banks={banks}
            showAmounts={showAmounts}
            onToggleAmounts={() => setShowAmounts(!showAmounts)}
            selectedPeriod={selectedPeriod}
            onSelectPeriod={setSelectedPeriod}
            onNavigateTab={setActiveTab}
            onOpenUssd={handleOpenUssd}
            onOpenAddTx={() => setIsAddTxOpen(true)}
            isSyncing={isSyncing}
            onSync={handleExecuteSync}
          />
        )}

        {activeTab === 'telecom' && (
          <TelecomScreen
            language={language}
            telecomAssets={telecomAssets}
            packages={packages}
            onOpenUssd={handleOpenUssd}
            onBuyBundle={handleBuyBundle}
          />
        )}

        {activeTab === 'transactions' && (
          <TransactionScreen
            language={language}
            transactions={transactions}
            banks={banks}
            onOpenAddTx={() => setIsAddTxOpen(true)}
            onClearTransactions={() => updateTransactions([])}
          />
        )}

        {activeTab === 'settings' && (
          <SettingsScreen
            language={language}
            setLanguage={setLanguage}
            theme={theme}
            setTheme={setTheme}
            banks={banks}
            onToggleBank={handleToggleBank}
            telecomAssets={telecomAssets}
            onUpdateTelecomAssets={updateTelecomAssets}
            onOpenSmsSimulator={() => setIsSmsSimOpen(true)}
            onOpenOnboarding={() => setIsOnboardingOpen(true)}
            onResetData={handleResetData}
          />
        )}
      </main>

      {/* Mobile Bottom Navigation Bar */}
      <BottomNavBar activeTab={activeTab} setActiveTab={setActiveTab} language={language} />

      {/* Modals & Dialogs */}
      <UssdModal
        isOpen={isUssdOpen}
        onClose={() => setIsUssdOpen(false)}
        language={language}
        initialAction={ussdInitialAction}
        onExecuteRecharge={handleExecuteRecharge}
        onExecuteTransfer={handleExecuteTransfer}
        onExecuteSync={handleExecuteSync}
        onExecuteBuyBundle={handleBuyBundle}
      />

      <SmsSimulatorModal
        isOpen={isSmsSimOpen}
        onClose={() => setIsSmsSimOpen(false)}
        language={language}
        onApplyParsedSms={handleApplyParsedSms}
      />

      <AddTransactionModal
        isOpen={isAddTxOpen}
        onClose={() => setIsAddTxOpen(false)}
        language={language}
        banks={banks}
        onAddTransaction={(newTx) => updateTransactions([newTx, ...transactions])}
      />

      <OnboardingModal
        isOpen={isOnboardingOpen}
        onClose={handleCloseOnboarding}
        language={language}
      />
    </div>
  );
};

export default App;

