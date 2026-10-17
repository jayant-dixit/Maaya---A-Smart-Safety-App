/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        maaya: {
          bg: '#090D16',
          card: '#131B2E',
          cardElevated: '#1E293B',
          red: '#E11D48',
          neonRed: '#FF2E56',
          green: '#10B981',
          amber: '#F59E0B',
          purple: '#8B5CF6',
        },
      },
    },
  },
  plugins: [],
};
