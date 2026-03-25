import { useState, useEffect } from 'react';
import { Capacitor } from '@capacitor/core';
import SmsMonitor from '../../data/smsMonitorPlugin';
import { TelecomPackage, Transaction } from '../../domain/types';
import { getMockData } from '../../data/mockDataService';

export const useNativeData = () => {
  const [packages, setPackages] = useState<TelecomPackage[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [netBalance, setNetBalance] = useState<number>(0);
  const [isLoading, setIsLoading] = useState(true);

  const fetchData = async () => {
    try {
      if (!Capacitor.isNativePlatform()) {
        console.warn("Web Platform Detected. Bypassing native Room DB and loading mock data.");
        const mock = getMockData();
        setPackages(mock.telecomPackages);
        setTransactions(mock.transactions);
        return;
      }

      const balanceRes = await SmsMonitor.getBalances();
      const transactionRes = await SmsMonitor.getTransactions();
      
      setPackages(balanceRes.packages || []);
      setNetBalance(balanceRes.netBalance || 0);
      setTransactions(transactionRes.transactions || []);
    } catch (e) {
      console.error("Failed to load native data", e);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    // Poll every 5 seconds for updates (Alternative to WebSockets or broadcast events)
    const interval = setInterval(fetchData, 5000);
    return () => clearInterval(interval);
  }, []);

  return { packages, transactions, netBalance, isLoading, refetch: fetchData };
};
