import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { initializePWA } from './utils/pwa'

// Initialize PWA functionality (service worker, install prompt, offline detection)
initializePWA()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
