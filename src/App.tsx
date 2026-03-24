import { useReducer, useEffect } from 'react';
import { HomeScreen } from './screens/HomeScreen';
import { TelecomScreen } from './screens/TelecomScreen';
import { TransactionScreen } from './screens/TransactionScreen';
import { SettingsScreen } from './screens/SettingsScreen';
import { BottomNav } from './components/BottomNav';
import { Bell, User } from 'lucide-react';
import { reducer, initialState } from './store';
import { cn } from './lib/utils';
import { persistenceService } from './services/persistenceService';

export default function App() {
  const savedState = persistenceService.loadState();
  const mergedState = savedState ? { ...initialState, ...savedState } : initialState;
  
  // Deduplicate transactions to fix existing key errors in saved state
  if (mergedState.transactions) {
    const seenIds = new Set();
    mergedState.transactions = mergedState.transactions.filter(t => {
      if (seenIds.has(t.id)) return false;
      seenIds.add(t.id);
      return true;
    });
  }
  
  const [state, dispatch] = useReducer(reducer, mergedState);

  // Persistence
  useEffect(() => {
    persistenceService.saveState(state);
  }, [state]);

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
          <button className={cn(
            "w-10 h-10 flex items-center justify-center rounded-xl transition-colors",
            state.theme === 'dark' ? "bg-slate-800 text-slate-400 hover:text-white" : 
            state.theme === 'vibrant' ? "bg-indigo-500 text-indigo-200 hover:text-white" : 
            state.theme === 'midnight' ? "bg-slate-900 text-slate-400 hover:text-white" :
            state.theme === 'forest' ? "bg-emerald-800 text-emerald-200 hover:text-white" : "bg-slate-50 text-slate-400 hover:text-slate-600"
          )}>
            <User size={20} />
          </button>
        </div>
      </nav>

      {/* Main Content */}
      <main className="pt-28 px-6 max-w-lg mx-auto">
        {state.activeTab === 'home' && (
          <HomeScreen 
            packages={state.telecomPackages} 
            transactions={state.transactions} 
            telecomBalance={state.telecomBalance} 
            language={state.language}
          />
        )}
        {state.activeTab === 'telecom' && (
          <TelecomScreen 
            packages={state.telecomPackages} 
            recommendedBundles={state.recommendedBundles}
            balance={state.telecomBalance} 
            language={state.language}
            giftRequests={state.giftRequests}
            dispatch={dispatch}
          />
        )}
        {state.activeTab === 'transactions' && (
          <TransactionScreen 
            transactions={state.transactions} 
            language={state.language}
            sources={state.transactionSources}
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
        pendingGiftsCount={(state.giftRequests || []).filter(r => r.status === 'pending').length}
        language={state.language}
      />
    </div>
  );
}
