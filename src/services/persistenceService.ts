import { AppState } from '../types';

const STORAGE_KEY = 'ethio_balance_state';

export const persistenceService = {
  saveState: (state: AppState) => {
    try {
      const serializedState = JSON.stringify(state);
      localStorage.setItem(STORAGE_KEY, serializedState);
    } catch (err) {
      console.error('Could not save state', err);
    }
  },

  loadState: (): AppState | undefined => {
    try {
      const serializedState = localStorage.getItem(STORAGE_KEY);
      if (serializedState === null) {
        return undefined;
      }
      return JSON.parse(serializedState);
    } catch (err) {
      console.error('Could not load state', err);
      return undefined;
    }
  },

  clearState: () => {
    localStorage.removeItem(STORAGE_KEY);
  }
};
