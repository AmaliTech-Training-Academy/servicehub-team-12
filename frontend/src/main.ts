import './style.css';
import 'preline';

// Declare global types for Preline
declare global {
    interface Window {
        HSStaticMethods: any;
    }
}

// Ensure Preline is initialized even if the window 'load' event has already fired
const initPreline = () => {
    if (window.HSStaticMethods) {
        window.HSStaticMethods.autoInit();
    }
};

if (document.readyState === 'complete' || document.readyState === 'interactive') {
    initPreline();
} else {
    window.addEventListener('DOMContentLoaded', initPreline);
}

// Global JS entry point.
// Import and initialise npm packages here (e.g. Preline UI components).
// This file is compiled by Vite and served as /assets/main.js to all Thymeleaf pages.
