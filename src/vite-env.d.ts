/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_APP_NAME: string
  readonly VITE_STORAGE_KEY: string
  readonly VITE_DEFAULT_LANGUAGE: string
  readonly VITE_DEFAULT_THEME: string
  readonly VITE_MOCK_TELECOM_BALANCE: string
  readonly VITE_MOCK_TELEBIRR_BALANCE: string
  readonly VITE_MOCK_USER_NAME: string
  readonly VITE_MOCK_USER_PHONE: string
  readonly VITE_MOCK_USER_AVATAR: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
