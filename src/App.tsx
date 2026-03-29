import { useReducer, useEffect, useMemo } from 'react';
import { Capacitor } from '@capacitor/core';
import { HomeScreen } from './presentation/screens/HomeScreen';
import { TelecomScreen } from './presentation/screens/TelecomScreen';
import { TransactionScreen } from './presentation/screens/TransactionScreen';
import { SettingsScreen } from './presentation/screens/SettingsScreen';
import { BottomNav } from './presentation/components/BottomNav';
import { Bell, User } from 'lucide-react';
import { reducer, initialState } from './store';
import { useNativeBridge } from '@/presentation/hooks/useNativeBridge';
import { useNativeData } from '@/presentation/hooks/useNativeData';
import { cn } from './lib/utils';
import { persistenceService } from './data/persistenceService';
import SimDetection from './data/simDetectionPlugin';

export default function App() {
  const savedState = persistenceService.loadState();
  const mergedState = useMemo(() => {
    if (!savedState) return initialState;
    return {
      ...initialState,
      ...savedState,
      // Deep merge userProfile to avoid losing fields like 'name' from previous versions
      userProfile: {
        ...initialState.userProfile,
        ...(savedState.userProfile || {})
      }
    };
  }, [savedState]);
  
  const [state, dispatch] = useReducer(reducer, mergedState);
  const { packages, transactions: rawTransactions, netBalance } = useNativeData();

  // Globally filter out AIRTIME transactions so they don't pollute any screen's UI or summaries
  const transactions = useMemo(() => 
    rawTransactions.filter(t => t?.source?.toUpperCase() !== 'AIRTIME'),
    [rawTransactions]
  );

  // Memoized sources to prevent unnecessary re-renders of TransactionScreen
  const sources = useMemo(() => 
    Array.from(new Set([...state.transactionSources, ...transactions.map(t => t.source)])),
    [state.transactionSources, transactions]
  );

  // Initialize Native SMS/USSD Bridge (dispatch not needed here as hook handles its own side effects or returns callbacks)
  useNativeBridge();

  // Persistence
  useEffect(() => {
    persistenceService.saveState(state);
  }, [state]);

  // Synchronization will now be handled inside the reducer via SET_SIMS/SET_PRIMARY_SIM cases.
  useEffect(() => {
    const detectSims = async () => {
      try {
        if (Capacitor.isNativePlatform()) {
          const { sims } = await SimDetection.getSimCards();
          if (sims && sims.length > 0) {
            // Map SimCardInfo to our domain SimCard type
            const domainSims = sims.map(s => ({
              id: s.id,
              phoneNumber: s.phoneNumber,
              label: s.carrierName,
              isPrimary: s.isPrimary,
              provider: s.carrierName
            }));
            dispatch({ type: 'SET_SIMS', sims: domainSims });
          }
        }
      } catch (err) {
        console.error("SIM detection failed:", err);
      }
    };
    detectSims();
  }, []); // Only on mount

  // Apply theme to body
  useEffect(() => {
    const root = window.document.documentElement;
    root.classList.remove('light', 'dark', 'vibrant');
    root.classList.add(state.theme);
    
    if (state.theme === 'dark') {
      root.style.backgroundColor = '#0f172a'; // slate-900
    } else if (state.theme === 'vibrant') {
      root.style.backgroundColor = '#4f46e5'; // indigo-600
    } else if (state.theme === 'midnight') {
      root.style.backgroundColor = '#020617'; // slate-950
    } else if (state.theme === 'forest') {
      root.style.backgroundColor = '#064e3b'; // emerald-900
    } else {
      root.style.backgroundColor = '#F9F9FB';
    }
  }, [state.theme]);

  const setActiveTab = (tab: any) => dispatch({ type: 'SET_TAB', tab });

  return (
    <div className={cn(
      "min-h-screen font-sans selection:bg-blue-100 transition-colors duration-500",
      state.theme === 'dark' ? "bg-slate-900 text-white" : 
      state.theme === 'vibrant' ? "bg-indigo-600 text-white" : 
      state.theme === 'midnight' ? "bg-slate-950 text-white" :
      state.theme === 'forest' ? "bg-emerald-900 text-white" : "bg-[#F9F9FB] text-slate-900"
    )}>
      {/* Top Bar */}
      <nav className={cn(
        "fixed top-0 w-full z-50 border-b flex justify-between items-center px-6 py-4 backdrop-blur-lg",
        state.theme === 'dark' ? "bg-slate-900/80 border-slate-800" : 
        state.theme === 'vibrant' ? "bg-indigo-600/80 border-indigo-500" : 
        state.theme === 'midnight' ? "bg-slate-950/80 border-slate-900" :
        state.theme === 'forest' ? "bg-emerald-900/80 border-emerald-800" : "bg-white/80 border-slate-100"
      )}>
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-blue-600 flex items-center justify-center text-white shadow-lg shadow-blue-100">
            <span className="font-black text-xl">E</span>
          </div>
          <span className={cn(
            "text-xl font-black tracking-tight",
            ['dark', 'vibrant', 'midnight', 'forest'].includes(state.theme) ? "text-white" : "text-slate-900"
          )}>EthioBalance</span>
        </div>
        <div className="flex items-center gap-3">
          <button className={cn(
            "w-10 h-10 flex items-center justify-center rounded-xl transition-colors",
            state.theme === 'dark' ? "bg-slate-800 text-slate-400 hover:text-white" : 
            state.theme === 'vibrant' ? "bg-indigo-500 text-indigo-200 hover:text-white" : 
            state.theme === 'midnight' ? "bg-slate-900 text-slate-400 hover:text-white" :
            state.theme === 'forest' ? "bg-emerald-800 text-emerald-200 hover:text-white" : "bg-slate-50 text-slate-400 hover:text-slate-600"
          )}>
            <Bell size={20} />
          </button>
          <button 
            onClick={() => setActiveTab('settings')}
            className={cn(
              "w-10 h-10 flex items-center justify-center rounded-xl transition-colors",
              state.theme === 'dark' ? "bg-slate-800 text-slate-400 hover:text-white" : 
              state.theme === 'vibrant' ? "bg-indigo-500 text-indigo-200 hover:text-white" : 
              state.theme === 'midnight' ? "bg-slate-900 text-slate-400 hover:text-white" :
              state.theme === 'forest' ? "bg-emerald-800 text-emerald-200 hover:text-white" : "bg-slate-50 text-slate-400 hover:text-slate-600"
            )}
          >
            <User size={20} />
          </button>
        </div>
      </nav>

      {/* Main Content */}
      <main className="pt-28 px-6 max-w-lg mx-auto">
        {state.activeTab === 'home' && (
          <HomeScreen 
            packages={packages} 
            transactions={transactions} 
            telecomBalance={state.telecomBalance ?? 0} 
            language={state.language}
            userName={state.userProfile?.name}
            userPhoneNumber={state.userProfile?.phoneNumber}
          />
        )}
        {state.activeTab === 'telecom' && (
          <TelecomScreen 
            packages={packages} 
            recommendedBundles={[]}
            balance={state.telecomBalance ?? 0} 
            language={state.language}
            giftRequests={[]}
            dispatch={dispatch}
          />
        )}
        {state.activeTab === 'transactions' && (
          <TransactionScreen 
            transactions={transactions} 
            language={state.language}
            sources={sources}
          />
        )}
        {state.activeTab === 'settings' && (
          <SettingsScreen state={state} dispatch={dispatch} />
        )}
      </main>

      {/* Navigation */}
      <BottomNav 
        activeTab={state.activeTab} 
        onTabChange={setActiveTab} 
        pendingGiftsCount={0}
        language={state.language}
      />
    </div>
  );
}
