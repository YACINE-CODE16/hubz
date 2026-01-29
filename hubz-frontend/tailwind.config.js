/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        dark: {
          base: '#0A0A0F',
          card: '#12121A',
          hover: '#1A1A24',
        },
        light: {
          base: '#F8FAFC',
          card: '#FFFFFF',
          hover: '#F1F5F9',
        },
        accent: '#3B82F6',
        success: '#22C55E',
        warning: '#F59E0B',
        error: '#EF4444',
      },
    },
  },
  plugins: [],
};
