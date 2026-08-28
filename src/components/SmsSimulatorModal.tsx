import React, { useState } from 'react';
import confetti from 'canvas-confetti';
import { Check, CheckCircle2, ChevronRight, Cpu, FileText, Play, Sparkles, X } from 'lucide-react';
import { Language, ParsedSmsResult } from '../types';
import { formatCurrency, t } from '../constants/translations';
import { SAMPLE_SMS_PRESETS } from '../services/mockData';
import { parseIncomingSms } from '../services/smsParser';

interface SmsSimulatorModalProps {
  isOpen: boolean;
  onClose: () => void;
  language: Language;
  onApplyParsedSms: (parsed: ParsedSmsResult, rawBody: string, sender: string) => void;
}

export const SmsSimulatorModal: React.FC<SmsSimulatorModalProps> = ({
  isOpen,
  onClose,
  language,
  onApplyParsedSms,
}) => {
  const [sender, setSender] = useState('127');
  const [body, setBody] = useState(
    'You have successfully paid 100.00 ETB for 5GB Monthly Internet package via Telebirr. Your new balance is 4,150.75 ETB. Transaction ID: TB992817290.'
  );
  const [parsedPreview, setParsedPreview] = useState<ParsedSmsResult | null>(null);

  if (!isOpen) return null;

  const handleTestParse = () => {
    const res = parseIncomingSms(sender, body);
    setParsedPreview(res);
  };

  const handleSelectPreset = (preset: (typeof SAMPLE_SMS_PRESETS)[0]) => {
    setSender(preset.sender);
    setBody(preset.body);
    const res = parseIncomingSms(preset.sender, preset.body);
    setParsedPreview(res);
  };

  const handleCommit = () => {
    const res = parsedPreview || parseIncomingSms(sender, body);
    onApplyParsedSms(res, body, sender);
    confetti({ particleCount: 70, spread: 80, origin: { y: 0.6 } });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-fadeIn">
      <div className="relative w-full max-w-2xl rounded-3xl bg-slate-900 border border-slate-700 shadow-2xl overflow-hidden flex flex-col max-h-[92vh]">
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-slate-800 bg-slate-950/70">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-2xl bg-purple-500/20 text-purple-400 border border-purple-500/30">
              <Cpu className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-extrabold text-base text-white">SMS Simulation & Parsing Sandbox</h3>
              <p className="text-xs text-slate-400">Test regex parsing & live balance reconciliation</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-5 overflow-y-auto space-y-4">
          {/* Preset Sample Quick Selector */}
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-2">
              Preset Ethiopian Bank & Telecom Samples:
            </label>
            <div className="flex items-center gap-2 overflow-x-auto pb-1">
              {SAMPLE_SMS_PRESETS.map((preset, idx) => (
                <button
                  key={idx}
                  onClick={() => handleSelectPreset(preset)}
                  className="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-xs font-semibold text-slate-200 shrink-0 transition-colors flex items-center gap-1.5"
                >
                  <Sparkles className="w-3 h-3 text-purple-400" />
                  <span>{preset.title}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Sender & Body Inputs */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div className="sm:col-span-1">
              <label className="block text-xs font-bold text-slate-300 uppercase tracking-wider mb-1">
                Sender / Shortcode
              </label>
              <input
                id="sms-sender-input"
                type="text"
                value={sender}
                onChange={(e) => setSender(e.target.value)}
                placeholder="127, 889, 804..."
                className="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white font-mono text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
              />
            </div>
            <div className="sm:col-span-2">
              <label className="block text-xs font-bold text-slate-300 uppercase tracking-wider mb-1">
                SMS Message Text
              </label>
              <textarea
                id="sms-body-input"
                rows={3}
                value={body}
                onChange={(e) => setBody(e.target.value)}
                placeholder="Paste incoming SMS text here..."
                className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white font-mono text-xs focus:outline-none focus:ring-2 focus:ring-purple-500 leading-relaxed"
              />
            </div>
          </div>

          <div className="flex justify-end">
            <button
              id="test-parse-btn"
              onClick={handleTestParse}
              className="px-4 py-2 rounded-xl bg-purple-600/20 hover:bg-purple-600/30 border border-purple-500/40 text-purple-300 font-bold text-xs flex items-center gap-2 transition-all"
            >
              <Play className="w-3.5 h-3.5" />
              <span>Execute Regex Parser</span>
            </button>
          </div>

          {/* Parsed Result Card */}
          {parsedPreview && (
            <div className="p-4 rounded-2xl bg-slate-950 border border-purple-500/30 space-y-3 animate-fadeIn">
              <div className="flex items-center justify-between">
                <span className="text-xs font-extrabold uppercase tracking-widest text-purple-400 flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                  Parser Extraction Output
                </span>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-purple-500/20 text-purple-300 border border-purple-500/40 font-mono font-bold">
                  {(parsedPreview.confidence * 100).toFixed(0)}% Confidence
                </span>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
                <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-800">
                  <span className="text-[10px] text-slate-500 font-bold uppercase block">Source</span>
                  <span className="font-extrabold text-white">{parsedPreview.source}</span>
                </div>
                <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-800">
                  <span className="text-[10px] text-slate-500 font-bold uppercase block">Type</span>
                  <span className="font-extrabold text-emerald-400">{parsedPreview.type || 'N/A'}</span>
                </div>
                <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-800">
                  <span className="text-[10px] text-slate-500 font-bold uppercase block">Amount</span>
                  <span className="font-extrabold text-white">{formatCurrency(parsedPreview.amount || 0)} ETB</span>
                </div>
                <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-800">
                  <span className="text-[10px] text-slate-500 font-bold uppercase block">Ref ID</span>
                  <span className="font-mono text-slate-300 truncate block">{parsedPreview.reference || 'N/A'}</span>
                </div>
              </div>

              {parsedPreview.packages.length > 0 && (
                <div className="p-3 rounded-xl bg-blue-950/30 border border-blue-500/20 text-xs">
                  <span className="text-[11px] font-bold text-blue-400 uppercase tracking-wider block mb-1">
                    Detected Telecom Packages ({parsedPreview.packages.length}):
                  </span>
                  <div className="space-y-1">
                    {parsedPreview.packages.map((pkg, i) => (
                      <div key={i} className="text-slate-300 font-mono text-[11px] flex justify-between">
                        <span>• {pkg.subType} {pkg.type.toUpperCase()}</span>
                        <span className="text-emerald-400 font-bold">{pkg.remainingAmount} {pkg.unit}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {parsedPreview.airtimeBalance !== undefined && (
                <div className="p-2.5 rounded-xl bg-emerald-950/30 border border-emerald-500/20 text-xs flex justify-between items-center">
                  <span className="text-emerald-400 font-bold">New Airtime Balance</span>
                  <span className="text-white font-mono font-black">{formatCurrency(parsedPreview.airtimeBalance)} ETB</span>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer Actions */}
        <div className="p-4 border-t border-slate-800 bg-slate-950/80 flex items-center justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2.5 rounded-xl hover:bg-slate-800 text-slate-400 hover:text-white font-semibold text-xs transition-colors"
          >
            {t(language, 'cancel')}
          </button>
          <button
            id="apply-parsed-sms-btn"
            onClick={handleCommit}
            className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 font-bold text-xs text-white shadow-lg shadow-purple-950/40 flex items-center gap-2 transition-all active:scale-95"
          >
            <Check className="w-4 h-4" />
            <span>Process & Reconcile Ledger</span>
          </button>
        </div>
      </div>
    </div>
  );
};
