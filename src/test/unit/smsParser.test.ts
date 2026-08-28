import { describe, it, expect } from 'vitest';
import { parseIncomingSms } from '../../services/smsParser';

describe('SMS Parser Unit Tests', () => {
  describe('Ethio Telecom *804# and Airtime parsing', () => {
    it('parses airtime balance and multi-packages correctly from 804 SMS', () => {
      const smsText =
        'Your balance is 84.50 ETB. Valid until 15/10/2026; from 5GB Monthly Internet package is 3.5GB, expiry date on 10/10/2026; from 100 Min Voice package is 45 Min, expiry date on 05/10/2026; 25 SMS remaining.';
      const result = parseIncomingSms('804', smsText);

      expect(result.airtimeBalance).toBe(84.5);
      expect(result.packages.length).toBeGreaterThanOrEqual(2);
      
      const internetPkg = result.packages.find((p) => p.type === 'internet');
      expect(internetPkg).toBeDefined();
      expect(internetPkg?.remainingAmount).toBe(3.5);

      const voicePkg = result.packages.find((p) => p.type === 'voice');
      expect(voicePkg).toBeDefined();
      expect(voicePkg?.remainingAmount).toBe(45);
    });

    it('parses airtime recharge notification from 805', () => {
      const smsText = 'You have successfully recharged 50.00 ETB. Your new balance is 125.50 ETB.';
      const result = parseIncomingSms('805', smsText);

      expect(result.isRecharge).toBe(true);
      expect(result.amount).toBe(50);
      expect(result.balanceAfter).toBe(125.5);
    });
  });

  describe('CBE (Commercial Bank of Ethiopia) parsing', () => {
    it('parses credit (income) transactions with reference and account', () => {
      const smsText =
        'Dear Customer, your account 100012345678 has been credited with ETB 3,500.00 on 2026-08-20 from ABEBE KEBEDE. Ref: FT2623348910. Current balance is ETB 18,250.75.';
      const result = parseIncomingSms('CBE', smsText);

      expect(result.type).toBe('INCOME');
      expect(result.amount).toBe(3500);
      expect(result.balanceAfter).toBe(18250.75);
      expect(result.reference).toBe('FT2623348910');
      expect(result.source).toBe('CBE');
      expect(result.partyName).toBe('ABEBE KEBEDE');
    });

    it('parses debit (expense/transfer) transactions with fee', () => {
      const smsText =
        'Dear Customer, your account 100012345678 has been debited with ETB 1,200.00 for payment to ETHIO ELECTRIC. Service fee: ETB 2.50. Ref: TX9981244. New balance is ETB 17,048.25.';
      const result = parseIncomingSms('889', smsText);

      expect(result.type).toBe('EXPENSE');
      expect(result.amount).toBe(1200);
      expect(result.fee).toBe(2.5);
      expect(result.balanceAfter).toBe(17048.25);
      expect(result.reference).toBe('TX9981244');
      expect(result.source).toBe('CBE');
    });
  });

  describe('Telebirr transaction parsing', () => {
    it('parses Telebirr received money (Income)', () => {
      const smsText =
        'You have received ETB 650.00 from CHALA TULU (0911002233) on 27/08/2026 14:30:12. Transaction number: TB26284910. Your current balance is ETB 2,450.00.';
      const result = parseIncomingSms('127', smsText);

      expect(result.type).toBe('INCOME');
      expect(result.amount).toBe(650);
      expect(result.balanceAfter).toBe(2450);
      expect(result.reference).toBe('TB26284910');
      expect(result.source).toBe('TELEBIRR');
      expect(result.partyName).toBe('CHALA TULU');
    });

    it('parses Telebirr payment/transfer (Expense)', () => {
      const smsText =
        'You have transferred ETB 400.00 to TIGIST HAILE (0922334455). Service fee ETB 0.00. Trans No: TB77889900. Your new account balance is ETB 2,050.00.';
      const result = parseIncomingSms('TELEBIRR', smsText);

      expect(result.type).toBe('EXPENSE');
      expect(result.amount).toBe(400);
      expect(result.balanceAfter).toBe(2050);
      expect(result.reference).toBe('TB77889900');
    });
  });

  describe('Other Ethiopian Private Banks parsing', () => {
    it('parses Awash Bank transactions', () => {
      const smsText =
        'Awash Bank: Your account 013200998811 has been credited with ETB 8,000.00. Ref: AWB202688. Balance ETB 45,000.00.';
      const result = parseIncomingSms('AWASH', smsText);

      expect(result.source).toBe('AWASH');
      expect(result.amount).toBe(8000);
      expect(result.balanceAfter).toBe(45000);
    });

    it('parses Dashen Bank transactions', () => {
      const smsText =
        'Dashen Bank: Transfer of ETB 2,150.00 completed successfully. Ref: DSH984123. Current balance is ETB 12,300.50.';
      const result = parseIncomingSms('DASHEN', smsText);

      expect(result.source).toBe('DASHEN');
      expect(result.amount).toBe(2150);
      expect(result.balanceAfter).toBe(12300.5);
    });

    it('parses Bank of Abyssinia (BOA) transactions', () => {
      const smsText =
        'Bank of Abyssinia: You have received ETB 5,000.00 from SOLOMON BEKELE. Ref: BOA771234. Balance is ETB 31,400.00.';
      const result = parseIncomingSms('BOA', smsText);

      expect(result.source).toBe('BOA');
      expect(result.amount).toBe(5000);
      expect(result.balanceAfter).toBe(31400);
    });
  });

  describe('Edge cases and error resiliency', () => {
    it('handles unstructured or empty SMS without crashing', () => {
      const result = parseIncomingSms('UNKNOWN', 'Hello world, special discount today only!');
      expect(result.source).toBe('UNKNOWN');
      expect(result.packages).toEqual([]);
    });
  });
});
