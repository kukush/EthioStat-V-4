import React, { useState, useEffect } from 'react';
import { X, Wallet } from 'lucide-react';
import { BankInfo, Language } from '../types';
import { t } from '../constants/translations';

interface BankEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  language: Language;
  bankToEdit?: BankInfo | null;
  onSaveBank: (bank: BankInfo) => void;
}

export const BankEditModal: React.FC<BankEditModalProps> = ({
  isOpen,
  onClose,
  language,
  bankToEdit,
  onSaveBank,
}) => {
  const [displayName, setDisplayName] = useState('');
  const [abbreviation, setAbbreviation] = useState('');
  const [fullName, setFullName] = useState('');
  const [ussd, setUssd] = useState('*999#');
  const [senderIds, setSenderIds] = useState('');
  const [color, setColor] = useState('from-emerald-600 to-teal-700');
  const [error, setError] = useState('');

  useEffect(() => {
    if (bankToEdit) {
      setDisplayName(bankToEdit.displayName);
      setAbbreviation(bankToEdit.abbreviation);
      setFullName(bankToEdit.fullName);
      setUssd(bankToEdit.ussd);
      setSenderIds(bankToEdit.senderIds.join(', '));
      setColor(bankToEdit.color);
    } else {
      setDisplayName('');
      setAbbreviation('');
      setFullName('');
      setUssd('*999#');
      setSenderIds('');
      setColor('from-emerald-600 to-teal-700');
    }
    setError('');
  }, [bankToEdit, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!displayName.trim() || !abbreviation.trim()) {
      setError('Display name and abbreviation are required.');
      return;
    }

    const bankId = bankToEdit ? bankToEdit.id : `custom-${abbreviation.toLowerCase().replace(/\s+/g, '-')}-${Date.now()}`;
    const sendersArray = senderIds.split(',').map((s) => s.trim()).filter(Boolean);

    const bankInfo: BankInfo = {
      id: bankId,
      abbreviation: abbreviation.toUpperCase().trim(),
      displayName: displayName.trim(),
      fullName: fullName.trim() || displayName.trim(),
      senderIds: sendersArray.length > 0 ? sendersArray : [abbreviation.toUpperCase().trim()],
      ussd: ussd.trim() || '*999#',
      color,
      accentColor: '#059669',
      enabled: bankToEdit ? bankToEdit.enabled : true,
      currentBalance: bankToEdit ? bankToEdit.currentBalance : 0,
    };

    onSaveBank(bankInfo);
    onClose();
  };

  const colorOptions = [
    { label: 'Emerald', value: 'from-emerald-600 to-teal-700' },
    { label: 'Purple', value: 'from-purple-600 to-indigo-700' },
    { label: 'Blue', value: 'from-blue-600 to-cyan-700' },
    { label: 'Amber', value: 'from-amber-600 to-orange-700' },
    { label: 'Rose', value: 'from-rose-600 to-red-700' },
    { label: 'Slate', value: 'from-slate-600 to-slate-800' },
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-fadeIn">
      <div className="relative w-full max-w-md rounded-3xl bg-slate-900 border border-slate-700 shadow-2xl overflow-hidden flex flex-col max-h-[92vh]">
        <div className="flex items-center justify-between p-5 border-b border-slate-800 bg-slate-950/70">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-amber-500/20 text-amber-400 border border-amber-500/30">
              <Wallet className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-extrabold text-base text-white">
                {bankToEdit ? 'Edit Transaction Source' : 'Add New Transaction Source'}
              </h3>
              <p className="text-xs text-slate-400">Configure bank or digital wallet source</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-5 overflow-y-auto space-y-4">
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1.5">
              Display Name *
            </label>
            <input
              type="text"
              placeholder="e.g. Awash Bank, CBE, Telebirr"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              className="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1.5">
              Abbreviation / Code *
            </label>
            <input
              type="text"
              placeholder="e.g. AWASH, CBE, TELEBIRR"
              value={abbreviation}
              onChange={(e) => setAbbreviation(e.target.value)}
              className="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white font-mono text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1.5">
              Full Institution Name
            </label>
            <input
              type="text"
              placeholder="e.g. Awash International Bank S.C."
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              className="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1.5">
                USSD Code
              </label>
              <input
                type="text"
                placeholder="e.g. *901#"
                value={ussd}
                onChange={(e) => setUssd(e.target.value)}
                className="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white font-mono text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1.5">
                SMS Sender IDs (comma-separated)
              </label>
              <input
                type="text"
                placeholder="e.g. 901, Awash"
                value={senderIds}
                onChange={(e) => setSenderIds(e.target.value)}
                className="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white font-mono text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1.5">
              Theme Color
            </label>
            <div className="grid grid-cols-3 gap-2">
              {colorOptions.map((opt) => (
                <button
                  key={opt.value}
                  type="button"
                  onClick={() => setColor(opt.value)}
                  className={`p-2 rounded-xl border text-xs font-bold flex items-center gap-2 transition-all ${
                    color === opt.value
                      ? 'border-emerald-500 bg-emerald-950/30 text-white'
                      : 'border-slate-800 bg-slate-950 text-slate-400 hover:text-slate-200'
                  }`}
                >
                  <div className={`w-4 h-4 rounded-full bg-gradient-to-tr ${opt.value}`} />
                  <span>{opt.label}</span>
                </button>
              ))}
            </div>
          </div>

          {error && <p className="text-rose-400 text-xs font-bold">{error}</p>}

          <div className="pt-2 flex items-center justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2.5 rounded-xl hover:bg-slate-800 text-slate-400 hover:text-white font-semibold text-xs transition-colors"
            >
              {t(language, 'cancel')}
            </button>
            <button
              type="submit"
              className="px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 font-bold text-xs text-white shadow-lg shadow-emerald-950/40 transition-all active:scale-95"
            >
              {bankToEdit ? 'Save Changes' : 'Add Source'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
