/**
 * Application Constants
 *
 * Centralized application configuration, branding, and environment variables.
 * Used across the web preview & mobile simulation environment.
 */

export const APP_NAME: string = import.meta.env.VITE_APP_NAME || 'EthioStat';
export const APP_VERSION: string = import.meta.env.VITE_APP_VERSION || '1.1.0';
export const DEFAULT_LANGUAGE: string = import.meta.env.VITE_DEFAULT_LANGUAGE || 'en';
export const APP_TAGLINE: string = 'Telecom & Financial Asset Tracker';
export const APP_DESCRIPTION: string =
  'EthioStat is engineered specifically for Ethiopia. It runs completely offline on your device — no bank passwords, no cloud uploads, and zero telemetry.';
export const APP_CSV_PREFIX: string = `${APP_NAME}_Ledger`;
