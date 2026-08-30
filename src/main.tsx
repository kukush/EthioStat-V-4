import React from 'react';
import ReactDOM from 'react-dom/client';
import { App } from './App';
import { SecurityProvider } from './components/BiometricProvider';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <SecurityProvider>
      <App />
    </SecurityProvider>
  </React.StrictMode>
);
