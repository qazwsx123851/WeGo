/**
 * WeGo Expense Statistics Module
 * Handles chart rendering and data fetching for expense statistics.
 *
 * @module ExpenseStatistics
 */

const CATEGORY_LABELS = {
    FOOD: '餐飲', TRANSPORT: '交通', ACCOMMODATION: '住宿',
    SHOPPING: '購物', ENTERTAINMENT: '娛樂', HEALTH: '健康', OTHER: '其他'
};

const ExpenseStatistics = {
    /** Trip ID */
    tripId: '',
    /** Chart instances */
    charts: {
        category: null,
        trend: null
    },

    /**
     * Escapes HTML to prevent XSS attacks.
     * Delegates to WeGo.escapeHtml shared utility.
     * @param {string} str - String to escape
     * @returns {string} Escaped string
     */
    escapeHtml(str) {
        return WeGo.escapeHtml(str);
    },

    /** Cached data for chart recreation on theme change */
    cachedData: {
        categories: null,
        trend: null
    },

    /**
     * Initialize the statistics module
     */
    init() {
        const tripIdEl = document.getElementById('trip-id');
        if (!tripIdEl) return;

        this.tripId = tripIdEl.value;

        // Configure Chart.js defaults for dark mode support
        this.configureChartDefaults();

        // Listen for theme changes to update charts
        window.addEventListener('themechange', () => this.onThemeChange());

        // Load all statistics
        this.loadCategoryBreakdown();
        this.loadTrend();
        this.loadMemberStatistics();
    },

    /**
     * Check if dark mode is active (localStorage preference first, then system)
     * @returns {boolean}
     */
    isDarkMode() {
        const savedTheme = localStorage.getItem('theme');
        if (savedTheme) {
            return savedTheme === 'dark';
        }
        return window.matchMedia('(prefers-color-scheme: dark)').matches;
    },

    /**
     * Configure Chart.js defaults
     */
    configureChartDefaults() {
        const isDarkMode = this.isDarkMode();
        const textColor = isDarkMode ? '#E5E7EB' : '#374151';
        const gridColor = isDarkMode ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.1)';

        Chart.defaults.color = textColor;
        Chart.defaults.borderColor = gridColor;
    },

    /**
     * Handle theme change - recreate charts with new colors
     */
    onThemeChange() {
        this.configureChartDefaults();

        // Recreate charts with cached data if available
        if (this.cachedData.categories) {
            this.renderCategoryChart(this.cachedData.categories);
        }
        if (this.cachedData.trend) {
            this.renderTrendChart(this.cachedData.trend);
        }
    },

    /**
     * Load category breakdown data and render chart
     */
    async loadCategoryBreakdown() {
        const loadingEl = document.getElementById('category-loading');
        const emptyEl = document.getElementById('category-empty');
        const containerEl = document.getElementById('category-chart-container');

        try {
            const response = await fetch(`/api/trips/${this.tripId}/statistics/category`);
            const result = await response.json();

            loadingEl.classList.add('hidden');

            if (!result.success || !result.data?.categories?.length) {
                emptyEl.classList.remove('hidden');
                return;
            }

            const data = result.data;
            containerEl.classList.remove('hidden');

            // Update summary
            document.getElementById('total-amount').textContent =
                '$' + this.formatNumber(data.totalAmount);
            document.getElementById('currency-label').textContent = data.currency || 'TWD';
            document.getElementById('total-count').textContent = data.totalCount;

            // Cache data for theme change recreation
            this.cachedData.categories = data.categories;

            // Render chart
            this.renderCategoryChart(data.categories);
            this.renderCategoryLegend(data.categories, data.currency);

        } catch (error) {
            loadingEl.classList.add('hidden');
            emptyEl.classList.remove('hidden');
            emptyEl.textContent = '載入失敗，請稍後再試';
        }
    },

    /**
     * Render category donut chart
     * @param {Array} categories - Category data
     */
    renderCategoryChart(categories) {
        const ctx = document.getElementById('category-chart');
        if (!ctx) return;

        // Destroy existing chart
        if (this.charts.category) {
            this.charts.category.destroy();
        }

        const labels = categories.map(c => CATEGORY_LABELS[c.category] || c.category);
        const data = categories.map(c => c.amount);
        const colors = categories.map(c => c.color);

        this.charts.category = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: data,
                    backgroundColor: colors,
                    borderWidth: 0,
                    hoverOffset: 8
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '60%',
                plugins: {
                    legend: {
                        display: false
                    },
                    tooltip: {
                        callbacks: {
                            label: (context) => {
                                const value = context.raw;
                                const percentage = categories[context.dataIndex].percentage;
                                return `$${this.formatNumber(value)} (${percentage.toFixed(1)}%)`;
                            }
                        }
                    }
                }
            }
        });
    },

    /**
     * Render category legend
     * @param {Array} categories - Category data
     * @param {string} currency - Currency code
     */
    renderCategoryLegend(categories, currency) {
        const legendEl = document.getElementById('category-legend');
        if (!legendEl) return;

        legendEl.innerHTML = categories.map(cat => `
            <div class="flex items-center gap-2 px-2 py-1 rounded-lg bg-gray-50 dark:bg-gray-800/50">
                <div class="w-3 h-3 rounded-full flex-shrink-0" style="background-color: ${this.escapeHtml(cat.color)}"></div>
                <span class="text-gray-700 dark:text-gray-300 truncate">${this.escapeHtml(CATEGORY_LABELS[cat.category] || cat.category)}</span>
                <span class="text-gray-500 dark:text-gray-400 font-mono ml-auto">${Number(cat.percentage).toFixed(1)}%</span>
            </div>
        `).join('');
    },

    /**
     * Load trend data and render chart
     */
    async loadTrend() {
        const loadingEl = document.getElementById('trend-loading');
        const emptyEl = document.getElementById('trend-empty');
        const containerEl = document.getElementById('trend-chart-container');

        try {
            const response = await fetch(`/api/trips/${this.tripId}/statistics/trend`);
            const result = await response.json();

            loadingEl.classList.add('hidden');

            if (!result.success || !result.data?.dataPoints?.length) {
                emptyEl.classList.remove('hidden');
                return;
            }

            const data = result.data;
            containerEl.classList.remove('hidden');

            // Update average per day
            document.getElementById('average-per-day').textContent =
                '$' + this.formatNumber(data.averagePerDay);

            // Cache data for theme change recreation
            this.cachedData.trend = data.dataPoints;

            // Render chart
            this.renderTrendChart(data.dataPoints);

        } catch (error) {
            loadingEl.classList.add('hidden');
            emptyEl.classList.remove('hidden');
            emptyEl.textContent = '載入失敗，請稍後再試';
        }
    },

    /**
     * Render trend line chart
     * @param {Array} dataPoints - Trend data points
     */
    renderTrendChart(dataPoints) {
        const ctx = document.getElementById('trend-chart');
        if (!ctx) return;

        // Destroy existing chart
        if (this.charts.trend) {
            this.charts.trend.destroy();
        }

        const labels = dataPoints.map(d => d.dateLabel);
        const data = dataPoints.map(d => d.amount);

        const isDarkMode = this.isDarkMode();
        const primaryColor = '#0EA5E9';
        const fillColor = isDarkMode ? 'rgba(14, 165, 233, 0.1)' : 'rgba(14, 165, 233, 0.2)';

        this.charts.trend = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: '支出金額',
                    data: data,
                    borderColor: primaryColor,
                    backgroundColor: fillColor,
                    fill: true,
                    tension: 0.3,
                    pointBackgroundColor: primaryColor,
                    pointBorderColor: '#fff',
                    pointBorderWidth: 2,
                    pointRadius: 4,
                    pointHoverRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false
                    },
                    tooltip: {
                        callbacks: {
                            label: (context) => {
                                const point = dataPoints[context.dataIndex];
                                return `$${this.formatNumber(context.raw)} (${point.count} 筆)`;
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        grid: {
                            display: false
                        }
                    },
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: (value) => '$' + this.formatNumber(value)
                        }
                    }
                }
            }
        });
    },

    /**
     * Load member statistics
     */
    async loadMemberStatistics() {
        const loadingEl = document.getElementById('members-loading');
        const emptyEl = document.getElementById('members-empty');
        const listEl = document.getElementById('members-list');

        try {
            const response = await fetch(`/api/trips/${this.tripId}/statistics/members`);
            const result = await response.json();

            loadingEl.classList.add('hidden');

            if (!result.success || !result.data?.members?.length) {
                emptyEl.classList.remove('hidden');
                return;
            }

            const data = result.data;
            listEl.classList.remove('hidden');

            // Render member list
            this.renderMemberList(data.members, data.currency);

        } catch (error) {
            loadingEl.classList.add('hidden');
            emptyEl.classList.remove('hidden');
            emptyEl.textContent = '載入失敗，請稍後再試';
        }
    },

    /**
     * Render member statistics list
     * @param {Array} members - Member statistics
     * @param {string} currency - Currency code
     */
    renderMemberList(members, currency) {
        const listEl = document.getElementById('members-list');
        if (!listEl) return;

        listEl.innerHTML = members.map(member => {
            // Use unsettledBalance for display (reflects settlement status)
            const unsettled = Number(member.unsettledBalance) || 0;
            const historical = Number(member.balance) || 0;

            let balanceHtml;
            if (unsettled === 0 && historical !== 0) {
                // Fully settled: green checkmark badge
                balanceHtml = `
                    <p class="font-semibold text-green-600 dark:text-green-400 flex items-center justify-end gap-1">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                        </svg>
                        已結清
                    </p>
                    <p class="text-xs text-gray-400 dark:text-gray-500 font-mono">
                        原 ${historical >= 0 ? '+' : ''}$${this.formatNumber(Math.abs(historical))}
                    </p>`;
            } else if (unsettled === 0) {
                // Even split, no balance
                balanceHtml = `
                    <p class="font-semibold text-gray-400 dark:text-gray-500">$0</p>
                    <p class="text-xs text-gray-400 dark:text-gray-500">均分</p>`;
            } else {
                // Outstanding balance
                const isPositive = unsettled >= 0;
                const balanceClass = isPositive
                    ? 'text-green-600 dark:text-green-400'
                    : 'text-red-600 dark:text-red-400';
                const balancePrefix = isPositive ? '+' : '';
                const statusText = isPositive ? '可收回' : '需支付';
                balanceHtml = `
                    <p class="font-semibold font-mono ${balanceClass}">
                        ${balancePrefix}$${this.formatNumber(Math.abs(unsettled))}
                    </p>
                    <p class="text-xs ${balanceClass}">${statusText}</p>`;
            }

            // Escape user-controlled data to prevent XSS
            const safeNickname = this.escapeHtml(member.nickname);
            const safeAvatarUrl = this.escapeHtml(member.avatarUrl);
            const firstChar = safeNickname.charAt(0) || '?';

            const avatarHtml = member.avatarUrl
                ? `<img src="${safeAvatarUrl}" alt="${safeNickname}" class="w-10 h-10 rounded-full object-cover" referrerpolicy="no-referrer">`
                : `<div class="w-10 h-10 rounded-full bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center">
                       <span class="text-sm font-semibold text-primary-600 dark:text-primary-400">${firstChar}</span>
                   </div>`;

            return `
                <div class="bg-white dark:bg-gray-800 rounded-xl p-4 shadow-card">
                    <div class="flex items-center gap-3">
                        ${avatarHtml}
                        <div class="flex-1 min-w-0">
                            <p class="font-medium text-gray-800 dark:text-gray-100 truncate">${safeNickname}</p>
                            <p class="text-xs text-gray-500 dark:text-gray-400">${Number(member.expenseCount)} 筆支出</p>
                        </div>
                        <div class="text-right">
                            ${balanceHtml}
                        </div>
                    </div>
                    <div class="mt-3 pt-3 border-t border-gray-100 dark:border-gray-700 grid grid-cols-2 gap-4 text-sm">
                        <div>
                            <p class="text-gray-500 dark:text-gray-400">已付</p>
                            <p class="font-mono text-gray-800 dark:text-gray-100">$${this.formatNumber(member.totalPaid)}</p>
                        </div>
                        <div>
                            <p class="text-gray-500 dark:text-gray-400">應分攤</p>
                            <p class="font-mono text-gray-800 dark:text-gray-100">$${this.formatNumber(member.totalOwed)}</p>
                        </div>
                    </div>
                    <div class="mt-2">
                        <div class="flex justify-between text-xs text-gray-500 dark:text-gray-400 mb-1">
                            <span>支付佔比</span>
                            <span>${Number(member.paidPercentage).toFixed(1)}%</span>
                        </div>
                        <div class="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                            <div class="bg-primary-500 h-2 rounded-full transition-all duration-300"
                                 style="width: ${Math.min(Number(member.paidPercentage), 100)}%"></div>
                        </div>
                    </div>
                </div>
            `;
        }).join('');
    },

    /**
     * Format number with locale
     * @param {number|string} num - Number to format
     * @returns {string} Formatted number
     */
    formatNumber(num) {
        return parseFloat(num).toLocaleString('en-US', {
            minimumFractionDigits: 0,
            maximumFractionDigits: 0
        });
    }
};

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    ExpenseStatistics.init();
});
