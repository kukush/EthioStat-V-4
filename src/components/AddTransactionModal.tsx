import React, { useState } from 'react';
import { Layers, PlusCircle, X } from 'lucide-react';
import { BankInfo, Language, TransactionCategory, TransactionEntity, TransactionType } from '../types';
import { t } from '../constants/translations';

interface AddTransactionModalProps {
  isOpen: boolean;
  onClose: () => void;
  language: Language;
  banks: BankInfo[];
  onAddTransaction: (transaction: TransactionEntity) => void;
}

export const AddTransactionModal: React.FC<AddTransactionModalProps> = ({
  isOpen,
  onClose,
  language,
  banks,
  onAddTransaction,
}) => {
  const [source, setSource] = useState('TELEBIRR');
  const [type, setType] = useState<TransactionType>('EXPENSE');
  const [category, setCategory] = useState<TransactionCategory>('SHOPPING');
  const [amount, setAmount] = useState('');
  const [party, setParty] = useState('');
  const [reference, setReference] = useState('');
  const [error, setError] = useState('');

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const numAmt = parseFloat(amount);
    if (isNaN(numAmt) || numAmt <= 0) {
      setError('Please enter a valid amount greater than 0');
      return;
    }

    const newTx: TransactionEntity = {
      id: `tx-manual-${Date.now()}`,
      amount: numAmt,
      currency: 'ETB',
      source,
      type,
      category,
      recipientOrSender: party.trim() || source,
      reference: reference.trim() || `REF-${Math.floor(100000 + Math.random() * 900000)}`,
      timestamp: Date.now(),
      simSlot: 1,
      rawSmsBody: `[Manual Entry] ${type} of ${numAmt} ETB via ${source}. Note: ${party}`,
    };

    onAddTransaction(newTx);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-fadeIn">
      <div className="relative w-full max-w-lg rounded-3xl bg-slate-900 border border-slate-700 shadow-2xl overflow-hidden flex flex-col max-h-[92vh]">
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-slate-800 bg-slate-950/70">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
              <PlusCircle className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-extrabold text-base text-white">{t(language, 'addTransactionManually')}</h3>
              <p className="text-xs text-slate-400">Record a custom offline transaction</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-5 overflow-y-auto space-y-4">
          {/* Type Selector (Income, Expense, Transfer) */}
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1.5">
              Transaction Type
            </label>
            <div className="grid grid-cols-3 gap-2">
              {(['EXPENSE', 'INCOME', 'TRANSFER'] as TransactionType[]).map((tType) => (
                <button
                  key={tType}
                  type="button"
                  onClick={() => setType(tType)}
                  className={`py-2 px-3 rounded-xl font-bold text-xs capitalize transition-all ${
                    type === tType
                      ? tType === 'INCOME'
                        ? 'bg-emerald-600 text-white shadow-sm'
                        : tType === 'EXPENSE'
                        ? 'bg-rose-600 text-white shadow-sm'
                        : 'bg-cyan-600 text-white shadow-sm'
                      : 'bg-slate-800 text-slate-400 hover:text-white'
                  }`}
                >
                  {tType === 'INCOME' ? t(language, 'income') : tType === 'EXPENSE' ? t(language, 'expense') : t(language, 'transfer')}
                </button>
              ))}
            </div>
          </div>

          {/* Source Bank / Wallet */}
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1.5">
              Source Bank / Wallet
            </label>
            <select
              id="modal-source-select"
              value={source}
              onChange={(e) => setSource(e.target.value)}
              className="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
            >
              {banks.map((b) => (
                <option key={b.id} value={b.abbreviation}>
                  {b.displayName} ({b.abbreviation})
                </option>
              ))}
              <option value="AIRTIME">Airtime Credit</option>
              <option value="CASH">Cash in Hand</option>
            </select>
          </div>

          {/* Amount (ETB) */}
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1.5">
              {t(language, 'amountEtb')} *
            </label>
            <input
              id="modal-amount-input"
              type="number"
              step="any"
              placeholder="e.g. 250.00"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-700 text-white font-mono text-base focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
            {error && <p className="text-rose-400 text-xs font-bold mt-1">{error}</p>}
          </div>

          {/* Category */}
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1.5">
              Category
            </label>
            <select
              id="modal-category-select"
              value={category}
              onChange={(e) => setCategory(e.target.value as TransactionCategory)}
              className="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
            >
              <option value="SHOPPING">Shopping / Grocery</option>
              <option value="DINING">Food & Dining</option>
              <option value="UTILITY">Electricity / Water / Utility</option>
              <option value="TELECOM">Telecom / Internet Package</option>
              <option value="RECHARGE">Airtime Recharge</option>
              <option value="SALARY">Salary / Income</option>
              <option value="TRANSFER">Transfer</option>
              <option value="GENERAL">General</option>
            </select>
          </div>

          {/* Recipient / Merchant */}
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1.5">
              Recipient, Sender, or Merchant
            </label>
            <input
              id="modal-party-input"
              type="text"
              placeholder="e.g. Friendship Supermarket, Dawit, etc."
              value={party}
              onChange={(e) => setParty(e.target.value)}
              className="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>

          {/* Reference */}
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1.5">
              Reference / Transaction ID (Optional)
            </label>
            <input
              id="modal-ref-input"
              type="text"
              placeholder="e.g. FT2608129..."
              value={reference}
              onChange={(e) => setReference(e.target.value)}
              className="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white font-mono text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>

          {/* Buttons */}
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
              Save Transaction
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
