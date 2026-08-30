import React from 'react';
import { X, FileText } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { formatCurrency, formatDate } from '../constants/translations';
import { TransactionEntity } from '../types';

interface ExportPreviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  transactions: TransactionEntity[];
  totalIncome: number;
  totalExpense: number;
  userName: string;
  userPhone: string;
}

export const ExportPreviewModal: React.FC<ExportPreviewModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
  transactions,
  totalIncome,
  totalExpense,
  userName,
  userPhone,
}) => {
  if (!isOpen) return null;

  const data = [
    { name: 'Income', value: totalIncome, color: '#10b981' }, // Emerald-500
    { name: 'Expense', value: totalExpense, color: '#f43f5e' }, // Rose-500
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fadeIn">
      <div className="w-full max-w-2xl max-h-[90vh] flex flex-col rounded-3xl bg-slate-900 border border-slate-800 shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <FileText className="w-5 h-5 text-emerald-400" />
            <h2 className="text-sm font-bold text-white uppercase tracking-wider">Report Preview</h2>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {/* Summary Info & Chart */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="p-4 rounded-2xl bg-slate-950 border border-slate-800 space-y-3">
                <div className='flex justify-between text-xs'>
                  <span className="text-slate-500 font-semibold">Account Holder</span>
                  <span className="text-white font-bold">{userName}</span>
                </div>
                <div className='flex justify-between text-xs'>
                  <span className="text-slate-500 font-semibold">Phone Number</span>
                  <span className="text-white font-bold">{userPhone}</span>
                </div>
                <div className='flex justify-between text-xs'>
                  <span className="text-slate-500 font-semibold">Total Income</span>
                  <span className="text-emerald-400 font-bold">+{formatCurrency(totalIncome)}</span>
                </div>
                <div className='flex justify-between text-xs'>
                  <span className="text-slate-500 font-semibold">Total Expense</span>
                  <span className="text-rose-400 font-bold">-{formatCurrency(totalExpense)}</span>
                </div>
            </div>
            
            <div className="p-4 rounded-2xl bg-slate-950 border border-slate-800 h-40">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={data} layout="vertical" margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                  <XAxis type="number" hide />
                  <YAxis dataKey="name" type="category" stroke="#94a3b8" fontSize={12} />
                  <Tooltip contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '12px' }} />
                  <Bar dataKey="value" radius={[0, 4, 4, 0]}>
                    {data.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* Table */}
          <div className="border border-slate-800 rounded-2xl overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-xs text-left">
                <thead className="bg-slate-950 text-slate-400 uppercase">
                  <tr>
                    <th className="px-3 py-2">Name</th>
                    <th className="px-3 py-2">Date</th>
                    <th className="px-3 py-2 text-right">Amount</th>
                    <th className="px-3 py-2">Category</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800 text-slate-300">
                  {transactions.slice(0, 10).map((tx) => (
                    <tr key={tx.id}>
                      <td className="px-3 py-2 truncate max-w-[150px]">{tx.recipientOrSender || 'N/A'}</td>
                      <td className="px-3 py-2">{new Date(tx.timestamp).toLocaleDateString()}</td>
                      <td className="px-3 py-2 text-right font-mono">{formatCurrency(tx.amount)}</td>
                      <td className="px-3 py-2">{tx.category}</td>
                    </tr>
                  ))}
                  {transactions.length > 10 && (
                    <tr>
                      <td colSpan={4} className="px-3 py-2 text-center text-slate-500 italic">... and {transactions.length - 10} more</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-slate-800 flex justify-end gap-3">
          <button onClick={onClose} className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 font-bold text-xs hover:bg-slate-700">
            Cancel
          </button>
          <button onClick={onConfirm} className="px-4 py-2 rounded-xl bg-emerald-600 text-white font-bold text-xs hover:bg-emerald-500 shadow-lg shadow-emerald-900/50">
            Confirm & Download
          </button>
        </div>
      </div>
    </div>
  );
};
