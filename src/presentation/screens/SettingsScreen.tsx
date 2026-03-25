import React, { useState, useMemo } from 'react';
import { Globe, Shield, Bell, Smartphone, HelpCircle, LogOut, Palette, Database, Plus, X, Check, Trash2, AlertCircle, Cloud, Zap, Moon, Sun, CreditCard, FlaskConical, Search, Landmark } from 'lucide-react';
import { Language, Theme, AppState, TelecomPackage, Transaction, Intent } from '@/domain/types';
import { useTranslation } from '@/translations';
import { cn } from '@/lib/utils';
import { motion, AnimatePresence } from 'framer-motion';
import { injectMockData } from '@/data/mockDataService';
import { ETHIOPIAN_BANKS } from '@/constants/banks';
import { DEFAULT_AVATARS, FALLBACK_AVATAR } from '@/constants/avatars';
import { getBankIcon } from '@/constants/bankIcons';
import { PhoneInput, formatEthiopianPhone, normalizePhone } from '@/presentation/components/PhoneInput';
import SmsMonitor from '@/data/smsMonitorPlugin';

interface SettingsScreenProps {
  state: AppState;
  dispatch: React.Dispatch<Intent>;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({ state, dispatch }) => {
  const t = useTranslation(state.language);
  const [newSource, setNewSource] = useState('');
  const [searchBank, setSearchBank] = useState('');
  const [showAddSource, setShowAddSource] = useState(false);

  const filteredBanks = useMemo(() => {
    return ETHIOPIAN_BANKS.filter(bank => {
      // Filter by search query
      const matchesSearch = bank.name.toLowerCase().includes(searchBank.toLowerCase()) || 
                           bank.abbreviation.toLowerCase().includes(searchBank.toLowerCase());
      
      // Filter out banks already added as transaction sources
      const notAlreadyAdded = !state.transactionSources.includes(bank.abbreviation);
      
      return matchesSearch && notAlreadyAdded;
    });
  }, [searchBank, state.transactionSources]);

  const handleAddSource = (source: string) => {
    if (source.trim()) {
      const cleanSource = source.trim();
      dispatch({ type: 'ADD_TRANSACTION_SOURCE', source: cleanSource });
      
      // Trigger 7-day historical scan for this new source
      SmsMonitor.scanHistory({ senderId: cleanSource, days: 7 });
      
      setNewSource('');
      setSearchBank('');
      setShowAddSource(false);
    }
  };

  const handleInjectMockData = () => {
    const primarySim = state.simCards.find(s => s.isPrimary) || state.simCards[0];
    if (primarySim) {
      injectMockData(dispatch, primarySim.id);
    }
  };

  const themes: { id: Theme; label: string; icon: any; color: string }[] = [
    { id: 'light', label: 'Light', icon: Sun, color: 'bg-white' },
    { id: 'dark', label: 'Dark', icon: Moon, color: 'bg-slate-900' },
    { id: 'midnight', label: 'Midnight', icon: Cloud, color: 'bg-blue-950' },
    { id: 'forest', label: 'Forest', icon: Zap, color: 'bg-emerald-950' },
  ];

  const [showAddSim, setShowAddSim] = useState(false);
  const [showEditProfile, setShowEditProfile] = useState(false);
  const [newSim, setNewSim] = useState({ phoneNumber: '', label: '', provider: 'Ethio Telecom' as any });
  const [profileName, setProfileName] = useState(state.userProfile?.name || '');
  const [selectedAvatar, setSelectedAvatar] = useState(state.userProfile?.avatarUrl || '');
  const [showSaveSuccess, setShowSaveSuccess] = useState(false);

  const handleUpdateProfile = () => {
    dispatch({ 
      type: 'SET_USER_PROFILE', 
      profile: { 
        name: profileName, 
        avatarUrl: selectedAvatar, 
        phoneNumber: state.userProfile?.phoneNumber || '' 
      } 
    });
    setShowSaveSuccess(true);
    setTimeout(() => {
      setShowSaveSuccess(false);
      setShowEditProfile(false);
    }, 800);
  };

  const openEditProfile = () => {
    setProfileName(state.userProfile?.name || '');
    setSelectedAvatar(state.userProfile?.avatarUrl || '');
    setShowEditProfile(true);
  };

  const handleAddSim = () => {
    if (newSim.phoneNumber.trim()) {
      const normalizedPhone = normalizePhone(newSim.phoneNumber);
      dispatch({ 
        type: 'ADD_SIM', 
        sim: { 
          id: `sim-${Date.now()}`, 
          ...newSim, 
          phoneNumber: normalizedPhone,
          isPrimary: state.simCards.length === 0 
        } 
      });
      setNewSim({ phoneNumber: '', label: '', provider: 'Ethio Telecom' });
      setShowAddSim(false);
    }
  };

  const getTranslatedSource = (source: string) => {
    const key = source.toLowerCase().replace(' ', '_');
    const translated = t(key as any);
    return translated !== key ? translated : source;
  };

  return (
    <div className="space-y-8 pb-32">
      <header>
        <h1 className="text-3xl font-black tracking-tight text-slate-900">{t('settings')}</h1>
      </header>

      <div className="flex flex-col items-center py-6">
        <div className="relative group">
          <div className="w-24 h-24 rounded-full bg-slate-200 border-4 border-white shadow-xl overflow-hidden">
            <img 
              src={state.userProfile?.avatarUrl || FALLBACK_AVATAR} 
              alt="Profile"
              className="w-full h-full object-cover"
            />
          </div>
          <button 
            onClick={openEditProfile}
            className="absolute bottom-0 right-0 p-2 bg-blue-600 text-white rounded-full shadow-lg border-2 border-white hover:bg-blue-700 transition-all active:scale-95"
          >
            <Plus size={14} />
          </button>
        </div>
        <h2 className="text-xl font-black text-slate-900 mt-4">{state.userProfile?.name || 'User'}</h2>
        <p className="text-sm font-bold text-slate-400">
          {formatEthiopianPhone(state.userProfile?.phoneNumber || state.simCards.find(s => s.isPrimary)?.phoneNumber || '')  || 'No Primary Number'}
        </p>
      </div>

      {/* SIM Cards Management */}
      <section className="space-y-4">
        <div className="flex justify-between items-center px-2">
          <div className="flex items-center gap-2">
            <Smartphone size={16} className="text-slate-400" />
            <h3 className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">SIM Cards & Numbers</h3>
          </div>
          <button 
            onClick={() => setShowAddSim(true)}
            className="text-[10px] font-bold text-blue-600 uppercase tracking-widest flex items-center gap-1"
          >
            <Plus size={12} />
            Add SIM
          </button>
        </div>
        
        <div className="bg-white rounded-[2.5rem] border border-slate-100 overflow-hidden shadow-sm">
          {state.simCards.map((sim, idx) => (
            <div
              key={sim.id}
              className={cn(
                "px-6 py-5 flex justify-between items-center",
                idx !== 0 && "border-t border-slate-50"
              )}
            >
              <div className="flex items-center gap-4">
                <div className={cn(
                  "w-10 h-10 rounded-2xl flex items-center justify-center text-white font-black text-xs",
                  sim.provider === 'Ethio Telecom' ? 'bg-blue-600' : 
                  sim.provider === 'Safaricom' ? 'bg-rose-600' : 'bg-slate-400'
                )}>
                  {sim.provider.substring(0, 1)}
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <p className="text-sm font-bold text-slate-900">{sim.phoneNumber}</p>
                    {sim.isPrimary && (
                      <span className="px-2 py-0.5 bg-blue-50 text-blue-600 text-[8px] font-black uppercase tracking-widest rounded-full">
                        Primary
                      </span>
                    )}
                  </div>
                  <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">{sim.label || sim.provider}</p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                {!sim.isPrimary && (
                  <button 
                    onClick={() => dispatch({ type: 'SET_PRIMARY_SIM', id: sim.id })}
                    className="p-2 text-slate-300 hover:text-blue-600 transition-colors"
                    title="Set as Primary"
                  >
                    <Check size={18} />
                  </button>
                )}
                <button 
                  onClick={() => dispatch({ type: 'REMOVE_SIM', id: sim.id })}
                  className="p-2 text-slate-300 hover:text-rose-500 transition-colors"
                >
                  <Trash2 size={18} />
                </button>
              </div>
            </div>
          ))}
          {state.simCards.length === 0 && (
            <div className="p-8 text-center">
              <p className="text-sm text-slate-400 font-bold">No SIM cards added</p>
            </div>
          )}
        </div>
      </section>

      {/* Theme Selection */}
      <section className="space-y-4">
        <div className="flex items-center gap-2 px-2">
          <Palette size={16} className="text-slate-400" />
          <h3 className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">Appearance</h3>
        </div>
        <div className="grid grid-cols-2 gap-3">
          {themes.map((th) => (
            <button
              key={th.id}
              onClick={() => dispatch({ type: 'SET_THEME', theme: th.id })}
              className={cn(
                "p-4 rounded-3xl border transition-all flex items-center gap-3",
                state.theme === th.id 
                  ? "bg-slate-900 text-white border-slate-900 shadow-lg" 
                  : "bg-white text-slate-600 border-slate-100 hover:border-slate-200"
              )}
            >
              <div className={cn(
                "w-8 h-8 rounded-xl flex items-center justify-center",
                state.theme === th.id ? "bg-white/20" : "bg-slate-100"
              )}>
                <th.icon size={18} />
              </div>
              <span className="text-xs font-bold">{th.label}</span>
              {state.theme === th.id && <Check size={14} className="ml-auto text-blue-400" />}
            </button>
          ))}
        </div>
      </section>

      {/* Language Selection */}
      <section className="space-y-4">
        <div className="flex items-center gap-2 px-2">
          <Globe size={16} className="text-slate-400" />
          <h3 className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">{t('language')}</h3>
        </div>
        <div className="bg-white rounded-[2.5rem] border border-slate-100 overflow-hidden shadow-sm">
          {(['en', 'am', 'om'] as Language[]).map((lang, idx) => (
            <button
              key={lang}
              onClick={() => dispatch({ type: 'SET_LANGUAGE', lang })}
              className={cn(
                "w-full px-6 py-5 flex justify-between items-center transition-colors",
                idx !== 0 && "border-t border-slate-50",
                state.language === lang ? "bg-blue-50/50" : "hover:bg-slate-50"
              )}
            >
              <span className={cn(
                "text-sm font-bold",
                state.language === lang ? "text-blue-600" : "text-slate-600"
              )}>
                {lang === 'en' ? 'English' : lang === 'am' ? 'አማርኛ' : 'Afaan Oromoo'}
              </span>
              {state.language === lang && <Check size={18} className="text-blue-600" />}
            </button>
          ))}
        </div>
      </section>

      {/* Transaction Sources */}
      <section className="space-y-4">
        <div className="flex justify-between items-center px-2">
          <div className="flex items-center gap-2">
            <CreditCard size={16} className="text-slate-400" />
            <h3 className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">{t('transactionSources')}</h3>
          </div>
          <button 
            onClick={() => setShowAddSource(true)}
            className="text-[10px] font-bold text-blue-600 uppercase tracking-widest flex items-center gap-1"
          >
            <Plus size={12} />
            Add New
          </button>
        </div>
        
        <div className="bg-white rounded-[2.5rem] border border-slate-100 overflow-hidden shadow-sm">
          {state.transactionSources.map((source, idx) => {
            return (
              <div
                key={source}
                className={cn(
                  "px-6 py-5 flex justify-between items-center",
                  idx !== 0 && "border-t border-slate-50"
                )}
              >
                <div className="flex items-center gap-4">
                  <div className="w-10 h-10 rounded-2xl flex items-center justify-center overflow-hidden">
                    {getBankIcon(source, 40)}
                  </div>
                  <div>
                    <p className="text-sm font-bold text-slate-900">{getTranslatedSource(source)}</p>
                    <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">Active Sync</p>
                  </div>
                </div>
                <button 
                  onClick={() => dispatch({ type: 'REMOVE_TRANSACTION_SOURCE', source })}
                  className="p-2 text-slate-300 hover:text-rose-500 transition-colors"
                >
                  <Trash2 size={18} />
                </button>
              </div>
            );
          })}
        </div>
      </section>

      {/* Developer Tools */}
      <section className="space-y-4">
        <div className="flex items-center gap-2 px-2">
          <FlaskConical size={16} className="text-slate-400" />
          <h3 className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">Developer Tools</h3>
        </div>
        <div className="bg-white p-6 rounded-[2.5rem] border border-slate-100 shadow-sm space-y-4">
          <button 
            onClick={handleInjectMockData}
            className="w-full py-4 bg-blue-50 text-blue-600 rounded-2xl font-bold text-sm flex items-center justify-center gap-2 hover:bg-blue-100 transition-all"
          >
            <Zap size={18} />
            Inject Mock Data
          </button>
          <p className="text-[10px] text-slate-400 text-center">Use this to populate the app with sample transactions and packages for testing.</p>
        </div>
      </section>

      {/* Security & Privacy */}
      <section className="space-y-4">
        <div className="flex items-center gap-2 px-2">
          <Shield size={16} className="text-slate-400" />
          <h3 className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">Privacy</h3>
        </div>
        <div className="bg-white p-6 rounded-[2.5rem] border border-slate-100 shadow-sm space-y-4">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-2xl bg-emerald-50 flex items-center justify-center text-emerald-600">
              <Shield size={24} />
            </div>
            <div>
              <p className="text-sm font-bold text-slate-900">Offline-First Privacy</p>
              <p className="text-xs text-slate-400 mt-1 leading-relaxed">
                All your SMS data is processed locally on your device. No data ever leaves your phone.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Edit Profile Modal */}
      <AnimatePresence>
        {showEditProfile && (
          <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
            <motion.div
              initial={{ opacity: 0, y: 100 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 100 }}
              className="bg-white w-full max-w-md rounded-3xl overflow-hidden shadow-2xl"
            >
              <div className="p-6">
                <div className="flex justify-between items-center mb-6">
                  <h3 className="text-xl font-black text-slate-900">Edit Profile</h3>
                  <button 
                    onClick={() => setShowEditProfile(false)}
                    className="p-2 hover:bg-slate-100 rounded-full transition-colors"
                  >
                    <X size={20} />
                  </button>
                </div>

                <div className="space-y-6">
                  {/* Name Input */}
                  <div>
                    <label className="block text-[10px] font-black uppercase tracking-widest text-slate-400 mb-2">
                      Full Name
                    </label>
                    <input
                      type="text"
                      value={profileName}
                      onChange={(e) => setProfileName(e.target.value)}
                      placeholder="Enter your name"
                      className="w-full px-4 py-3 bg-slate-50 border-2 border-slate-100 rounded-2xl focus:border-blue-500 focus:outline-none font-bold text-slate-900 transition-all"
                    />
                  </div>

                  {/* Avatar Selection */}
                  <div>
                    <label className="block text-[10px] font-black uppercase tracking-widest text-slate-400 mb-4">
                      Choose Avatar
                    </label>
                    <div className="grid grid-cols-3 gap-4">
                      {DEFAULT_AVATARS.map((avatar) => (
                        <button
                          key={avatar.id}
                          onClick={() => setSelectedAvatar(avatar.url)}
                          className={cn(
                            "relative aspect-square rounded-2xl overflow-hidden border-4 transition-all",
                            selectedAvatar === avatar.url ? "border-blue-600 scale-105 shadow-lg" : "border-transparent opacity-60 hover:opacity-100"
                          )}
                        >
                          <img src={avatar.url} alt={avatar.label} className="w-full h-full object-cover" />
                          {selectedAvatar === avatar.url && (
                            <div className="absolute inset-0 bg-blue-600/20 flex items-center justify-center">
                              <div className="bg-blue-600 text-white rounded-full p-1">
                                <Check size={12} />
                              </div>
                            </div>
                          )}
                        </button>
                      ))}
                    </div>
                  </div>

                  <button
                    onClick={handleUpdateProfile}
                    disabled={showSaveSuccess}
                    className={cn(
                      "w-full py-4 rounded-2xl font-black uppercase tracking-widest shadow-xl transition-all active:scale-95",
                      showSaveSuccess
                        ? "bg-emerald-500 text-white"
                        : "bg-slate-900 text-white hover:bg-slate-800"
                    )}
                  >
                    {showSaveSuccess ? (
                      <span className="flex items-center justify-center gap-2">
                        <Check size={18} /> Saved!
                      </span>
                    ) : (
                      'Save Changes'
                    )}
                  </button>
                </div>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Add SIM Modal */}
      <AnimatePresence>
        {showAddSim && (
          <div className="fixed inset-0 z-[100] flex items-end sm:items-center justify-center p-4">
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setShowAddSim(false)}
              className="absolute inset-0 bg-slate-900/60 backdrop-blur-sm"
            />
            <motion.div 
              initial={{ y: '100%' }}
              animate={{ y: 0 }}
              exit={{ y: '100%' }}
              className="relative w-full max-w-lg bg-white rounded-t-[3rem] sm:rounded-[3rem] p-8 shadow-2xl space-y-6"
            >
              <div className="flex justify-between items-start">
                <div className="space-y-1">
                  <h3 className="text-2xl font-black text-slate-900">Add SIM Card</h3>
                  <p className="text-sm text-slate-500">Register a new phone number for tracking</p>
                </div>
                <button 
                  onClick={() => setShowAddSim(false)}
                  className="p-2 bg-slate-100 rounded-full text-slate-400 hover:text-slate-600"
                >
                  <X size={20} />
                </button>
              </div>
              
              <div className="space-y-4">
                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest ml-2">Phone Number</label>
                  <PhoneInput
                    value={newSim.phoneNumber}
                    onChange={(val) => setNewSim({ ...newSim, phoneNumber: val })}
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest ml-2">Label (Optional)</label>
                  <input 
                    type="text"
                    value={newSim.label}
                    onChange={(e) => setNewSim({ ...newSim, label: e.target.value })}
                    placeholder="e.g., Work SIM, Personal"
                    className="w-full px-6 py-5 bg-slate-50 border-none rounded-[2rem] text-sm font-bold focus:ring-2 focus:ring-blue-500/20 transition-all"
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest ml-2">Provider</label>
                  <div className="flex gap-2">
                    {['Ethio Telecom', 'Safaricom'].map((p) => (
                      <button
                        key={p}
                        onClick={() => setNewSim({ ...newSim, provider: p as any })}
                        className={cn(
                          "flex-1 py-4 rounded-2xl text-xs font-bold transition-all border",
                          newSim.provider === p 
                            ? "bg-slate-900 text-white border-slate-900 shadow-lg" 
                            : "bg-white text-slate-600 border-slate-100"
                        )}
                      >
                        {p}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
              
              <button 
                onClick={handleAddSim}
                disabled={!newSim.phoneNumber.trim()}
                className="w-full py-5 bg-slate-900 text-white rounded-[2rem] font-black uppercase tracking-widest flex items-center justify-center gap-2 hover:bg-slate-800 transition-all disabled:opacity-50 shadow-lg shadow-slate-200"
              >
                <Plus size={18} />
                Register SIM
              </button>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Add Source Modal */}
      <AnimatePresence>
        {showAddSource && (
          <div className="fixed inset-0 z-[100] flex items-end sm:items-center justify-center p-4">
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setShowAddSource(false)}
              className="absolute inset-0 bg-slate-900/60 backdrop-blur-sm"
            />
            <motion.div 
              initial={{ y: '100%' }}
              animate={{ y: 0 }}
              exit={{ y: '100%' }}
              className="relative w-full max-w-lg bg-white rounded-t-[3rem] sm:rounded-[3rem] p-8 shadow-2xl space-y-6 max-h-[90vh] flex flex-col"
            >
              <div className="flex justify-between items-start">
                <div className="space-y-1">
                  <h3 className="text-2xl font-black text-slate-900">Add Bank Source</h3>
                  <p className="text-sm text-slate-500">Select a bank or enter a custom sender ID</p>
                </div>
                <button 
                  onClick={() => setShowAddSource(false)}
                  className="p-2 bg-slate-100 rounded-full text-slate-400 hover:text-slate-600"
                >
                  <X size={20} />
                </button>
              </div>
              
              <div className="space-y-4 flex-1 overflow-hidden flex flex-col">
                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest ml-2">Custom Sender ID</label>
                  <div className="flex gap-2">
                    <input 
                      type="text"
                      value={newSource}
                      onChange={(e) => setNewSource(e.target.value)}
                      placeholder="e.g., CBE, Telebirr, 830"
                      className="flex-1 px-6 py-4 bg-slate-50 border-none rounded-2xl text-sm font-bold focus:ring-2 focus:ring-blue-500/20 transition-all"
                    />
                    <button 
                      onClick={() => handleAddSource(newSource)}
                      disabled={!newSource.trim()}
                      className="px-6 bg-slate-900 text-white rounded-2xl font-bold text-sm disabled:opacity-50"
                    >
                      Add
                    </button>
                  </div>
                </div>

                <div className="relative">
                  <div className="absolute inset-y-0 left-4 flex items-center pointer-events-none">
                    <Search size={16} className="text-slate-400" />
                  </div>
                  <input 
                    type="text"
                    value={searchBank}
                    onChange={(e) => setSearchBank(e.target.value)}
                    placeholder="Search Ethiopian Banks..."
                    className="w-full pl-12 pr-6 py-4 bg-slate-50 border-none rounded-2xl text-sm font-bold focus:ring-2 focus:ring-blue-500/20 transition-all"
                  />
                </div>

                <div className="flex-1 overflow-y-auto pr-2 space-y-2 custom-scrollbar">
                  {filteredBanks.map((bank) => (
                    <button
                      key={bank.abbreviation}
                      onClick={() => handleAddSource(bank.abbreviation)}
                      className="w-full flex items-center justify-between p-4 bg-slate-50 hover:bg-slate-100 rounded-2xl transition-all group"
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-xl bg-white flex items-center justify-center text-slate-400 group-hover:text-blue-600 transition-colors shadow-sm">
                          <Landmark size={20} />
                        </div>
                        <div className="text-left">
                          <div className="text-sm font-bold text-slate-900">{getTranslatedSource(bank.abbreviation)}</div>
                          <div className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">{bank.abbreviation} • {bank.ussd}</div>
                        </div>
                      </div>
                      <Plus size={18} className="text-slate-300 group-hover:text-slate-900" />
                    </button>
                  ))}
                  {filteredBanks.length === 0 && (
                    <div className="py-8 text-center space-y-2">
                      <div className="text-slate-300 flex justify-center">
                        <Search size={40} />
                      </div>
                      <p className="text-sm font-bold text-slate-400">No banks found matching "{searchBank}"</p>
                    </div>
                  )}
                </div>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
};
