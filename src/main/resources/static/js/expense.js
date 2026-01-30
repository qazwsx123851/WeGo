/**
 * WeGo Expense Form Module
 * Handles expense form interactions, split calculations, and optional AJAX submission.
 *
 * @module ExpenseForm
 */

const ExpenseForm = {
    /** Form element */
    form: null,
    /** Amount input element */
    amountInput: null,
    /** Split method hidden input */
    splitMethodInput: null,
    /** CSRF token */
    csrfToken: '',
    /** CSRF header name */
    csrfHeader: 'X-CSRF-TOKEN',
    /** Trip ID */
    tripId: '',

    /**
     * Initialize the expense form module
     */
    init() {
        this.form = document.getElementById('expense-form');
        if (!this.form) return;

        this.amountInput = document.getElementById('amount');
        this.splitMethodInput = document.getElementById('splitMethod');

        // Get CSRF token
        this.csrfToken = document.querySelector('meta[name="_csrf"]')?.content || '';
        this.csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

        // Get trip ID from form action or data attribute
        const actionMatch = this.form.action?.match(/\/trips\/([^/]+)/);
        if (actionMatch) {
            this.tripId = actionMatch[1];
        }

        this.bindEvents();
        this.initializeSplitTabs();
        this.calculateSplitAmounts();
    },

    /**
     * Bind event listeners
     */
    bindEvents() {
        // Amount change triggers recalculation
        if (this.amountInput) {
            this.amountInput.addEventListener('input', () => this.calculateSplitAmounts());
        }

        // Split tab switching
        document.querySelectorAll('.split-tab').forEach(tab => {
            tab.addEventListener('click', (e) => this.handleTabSwitch(e.currentTarget));
        });

        // Participant checkbox changes (for equal split)
        document.querySelectorAll('.participant-checkbox').forEach(cb => {
            cb.addEventListener('change', () => this.calculateSplitAmounts());
        });

        // Percentage input changes
        document.querySelectorAll('.percentage-input').forEach(input => {
            input.addEventListener('input', () => this.calculateSplitAmounts());
        });

        // Custom amount input changes
        document.querySelectorAll('.custom-input').forEach(input => {
            input.addEventListener('input', () => this.calculateSplitAmounts());
        });

        // Form validation on submit
        this.form.addEventListener('submit', (e) => this.handleSubmit(e));
    },

    /**
     * Initialize split tab styling
     */
    initializeSplitTabs() {
        const activeTab = document.querySelector('.split-tab.active');
        if (activeTab) {
            activeTab.classList.add('bg-white', 'dark:bg-gray-700', 'shadow-sm', 'text-gray-800', 'dark:text-gray-100');
            activeTab.classList.remove('text-gray-500', 'dark:text-gray-400');
        }
    },

    /**
     * Handle split method tab switching
     * @param {HTMLElement} tab - The clicked tab element
     */
    handleTabSwitch(tab) {
        const method = tab.dataset.splitMethod;
        const splitTabs = document.querySelectorAll('.split-tab');
        const splitContents = document.querySelectorAll('.split-content');

        // Update active tab styling
        splitTabs.forEach(t => {
            t.classList.remove('active', 'bg-white', 'dark:bg-gray-700', 'shadow-sm', 'text-gray-800', 'dark:text-gray-100');
            t.classList.add('text-gray-500', 'dark:text-gray-400');
        });
        tab.classList.add('active', 'bg-white', 'dark:bg-gray-700', 'shadow-sm', 'text-gray-800', 'dark:text-gray-100');
        tab.classList.remove('text-gray-500', 'dark:text-gray-400');

        // Update hidden input
        if (this.splitMethodInput) {
            this.splitMethodInput.value = method;
        }

        // Show corresponding content
        splitContents.forEach(content => content.classList.add('hidden'));
        const targetContent = document.getElementById('split-' + method.toLowerCase());
        if (targetContent) {
            targetContent.classList.remove('hidden');
        }

        // Recalculate amounts
        this.calculateSplitAmounts();
    },

    /**
     * Calculate and display split amounts based on current method
     */
    calculateSplitAmounts() {
        const amount = parseFloat(this.amountInput?.value) || 0;
        const method = this.splitMethodInput?.value || 'EQUAL';

        if (method === 'EQUAL') {
            this.calculateEqualSplit(amount);
        } else if (method === 'PERCENTAGE') {
            this.calculatePercentageSplit(amount);
        } else if (method === 'CUSTOM') {
            this.calculateCustomSplit(amount);
        }
    },

    /**
     * Calculate equal split amounts
     * @param {number} amount - Total amount
     */
    calculateEqualSplit(amount) {
        const checkboxes = document.querySelectorAll('.participant-checkbox:checked');
        const count = checkboxes.length;
        const perPerson = count > 0 ? (amount / count).toFixed(2) : 0;

        document.querySelectorAll('.equal-amount').forEach(el => {
            const checkbox = el.closest('.flex').querySelector('.participant-checkbox');
            el.textContent = checkbox?.checked ? '$' + this.formatNumber(perPerson) : '$0';
        });
    },

    /**
     * Calculate percentage split amounts
     * @param {number} amount - Total amount
     */
    calculatePercentageSplit(amount) {
        let total = 0;
        document.querySelectorAll('.percentage-input').forEach(input => {
            const value = parseFloat(input.value) || 0;
            total += value;
            const calcAmount = (amount * value / 100).toFixed(2);
            const amountEl = input.closest('.flex')?.querySelector('.percentage-amount');
            if (amountEl) {
                amountEl.textContent = '$' + this.formatNumber(calcAmount);
            }
        });

        const totalEl = document.getElementById('percentage-total');
        if (totalEl) {
            totalEl.textContent = total + '%';
            totalEl.classList.remove('text-success', 'text-error');
            totalEl.classList.add(total === 100 ? 'text-success' : 'text-error');
        }
    },

    /**
     * Calculate custom split amounts
     * @param {number} amount - Total amount
     */
    calculateCustomSplit(amount) {
        let total = 0;
        document.querySelectorAll('.custom-input').forEach(input => {
            total += parseFloat(input.value) || 0;
        });

        const totalEl = document.getElementById('custom-total');
        if (totalEl) {
            totalEl.textContent = '$' + this.formatNumber(total.toFixed(2));
            totalEl.classList.remove('text-success', 'text-error', 'text-gray-800', 'dark:text-gray-100');
            totalEl.classList.add(Math.abs(total - amount) < 0.01 ? 'text-success' : 'text-error');
        }
    },

    /**
     * Format number with locale
     * @param {number|string} num - Number to format
     * @returns {string} Formatted number
     */
    formatNumber(num) {
        return parseFloat(num).toLocaleString('en-US', {
            minimumFractionDigits: 0,
            maximumFractionDigits: 2
        });
    },

    /**
     * Handle form submission with validation
     * @param {Event} e - Submit event
     */
    handleSubmit(e) {
        // Basic validation
        const amount = parseFloat(this.amountInput?.value) || 0;
        const description = document.getElementById('description')?.value?.trim();
        const payerId = document.getElementById('payerId')?.value;

        if (amount <= 0) {
            e.preventDefault();
            this.showError('請輸入有效金額');
            return;
        }

        if (!description) {
            e.preventDefault();
            this.showError('請輸入支出描述');
            return;
        }

        if (!payerId) {
            e.preventDefault();
            this.showError('請選擇付款人');
            return;
        }

        // Validate split method totals
        const method = this.splitMethodInput?.value || 'EQUAL';
        if (method === 'PERCENTAGE') {
            let total = 0;
            document.querySelectorAll('.percentage-input').forEach(input => {
                total += parseFloat(input.value) || 0;
            });
            if (Math.abs(total - 100) > 0.01) {
                e.preventDefault();
                this.showError('百分比總和必須等於 100%');
                return;
            }
        } else if (method === 'CUSTOM') {
            let total = 0;
            document.querySelectorAll('.custom-input').forEach(input => {
                total += parseFloat(input.value) || 0;
            });
            if (Math.abs(total - amount) > 0.01) {
                e.preventDefault();
                this.showError('自訂金額總和必須等於支出金額');
                return;
            }
        }

        // Show loading state on submit button
        const submitBtn = this.form.querySelector('button[type="submit"]');
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<span class="animate-spin">⏳</span> 處理中...';
        }
    },

    /**
     * Show error toast message
     * @param {string} message - Error message
     */
    showError(message) {
        // Use existing Toast if available
        if (typeof Toast !== 'undefined') {
            Toast.error(message);
            return;
        }

        // Fallback alert
        alert(message);
    }
};

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    ExpenseForm.init();
});
