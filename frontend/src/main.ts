import './style.css';
import $ from 'jquery';
import DataTable from 'datatables.net-dt';
import 'preline';

// Declare global types for Preline
declare global {
    interface Window {
        HSStaticMethods: any;
        HSDataTable: any;
        $: typeof $;
        jQuery: typeof $;
        DataTable: typeof DataTable;
    }
}

window.$ = $;
window.jQuery = $;
window.DataTable = DataTable;

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

window.addEventListener('load', () => {
    const requestTableRoot = document.querySelector('#hs-datatable-filter');
    const priorityEl = document.querySelector<HTMLSelectElement>('#hs-request-filter-priority');
    const statusEl = document.querySelector<HTMLSelectElement>('#hs-request-filter-status');

    if (requestTableRoot && window.HSDataTable) {
        const { dataTable } = new window.HSDataTable('#hs-datatable-filter');

        dataTable.search.fixed('request-priority', (_searchStr: string, data: string[]) => {
            const priority = priorityEl?.value ?? '';
            const rowPriority = (data[3] ?? '').trim();

            return !priority || rowPriority === priority;
        });

        dataTable.search.fixed('request-status', (_searchStr: string, data: string[]) => {
            const status = statusEl?.value ?? '';
            const rowStatus = (data[4] ?? '').trim();

            return !status || rowStatus === status;
        });

        priorityEl?.addEventListener('change', () => dataTable.draw());
        statusEl?.addEventListener('change', () => dataTable.draw());
    }

    const inputs = document.querySelectorAll<HTMLInputElement>('.dt-container thead input');

    inputs.forEach((input) => {
        input.addEventListener('keydown', function (evt) {
            if ((evt.metaKey || evt.ctrlKey) && evt.key === 'a') {
                this.select();
            }
        });
    });
});

// Global JS entry point.
// Import and initialise npm packages here (e.g. Preline UI components).
// This file is compiled by Vite and served as /assets/main.js to all Thymeleaf pages.
