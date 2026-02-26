/**
 * WeGo Expense Statistics Module
 * Handles chart rendering and data fetching for expense statistics.
 *
 * @module ExpenseStatistics
 */

const ExpenseStatistics = {
    /** Trip ID */
    tripId: '',
    /** Respect prefers-reduced-motion */
    prefersReducedMotion: (typeof WeGo !== 'undefined') ? WeGo._reducedMotion : false,
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

    /** Track which sections have been revealed by scroll */
    _revealed: { category: false, trend: false, members: false },

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

        // Scroll-triggered chart rendering: observe chart containers
        this.initScrollTriggers();
    },

    /**
     * Set up IntersectionObserver to trigger chart animations when visible.
     */
    initScrollTriggers() {
        if (typeof WeGo === 'undefined' || !WeGo.anime || !WeGo.anime.onVisible) return;

        const self = this;
        WeGo.anime.onVisible('#category-chart-container', function() {
            self._revealed.category = true;
            if (self.cachedData.categories && !self.charts.category) {
                self.renderCategoryChart(self.cachedData.categories);
            }
        }, { threshold: 0.1 });

        WeGo.anime.onVisible('#trend-chart-container', function() {
            self._revealed.trend = true;
            if (self.cachedData.trend && !self.charts.trend) {
                self.renderTrendChart(self.cachedData.trend);
            }
        }, { threshold: 0.1 });
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

            // Update summary with countUp animation
            var totalAmountEl = document.getElementById('total-amount');
            var totalCountEl = document.getElementById('total-count');
            document.getElementById('currency-label').textContent = data.currency || 'TWD';

            if (typeof WeGo !== 'undefined' && WeGo.anime && WeGo.anime.countUp) {
                WeGo.anime.countUp(totalAmountEl, data.totalAmount, { format: 'currency', currency: data.currency || 'TWD' });
                WeGo.anime.countUp(totalCountEl, data.totalCount);
            } else {
                totalAmountEl.textContent = '$' + this.formatNumber(data.totalAmount);
                totalCountEl.textContent = data.totalCount;
            }

            // Cache data for theme change recreation and scroll-trigger deferred rendering
            this.cachedData.categories = data.categories;

            // Render legend immediately (text only, no animation needed)
            this.renderCategoryLegend(data.categories, data.currency);

            // Render chart: immediately if already visible, or wait for scroll trigger
            if (this._revealed.category || !WeGo.anime || !WeGo.anime.onVisible) {
                this.renderCategoryChart(data.categories);
            }

        } catch (error) {
            loadingEl.classList.add('hidden');
            emptyEl.classList.remove('hidden');
            emptyEl.textContent = '載入失敗，請稍後再試';
        }
    },

    /**
     * Create center text plugin for doughnut chart
     * @param {Function} formatFn - Number formatting function
     * @param {Function} darkModeFn - Dark mode detection function
     * @returns {Object} Chart.js plugin
     */
    createCenterTextPlugin(formatFn, darkModeFn) {
        return {
            id: 'centerText',
            afterDraw(chart) {
                // Hide center text when tooltip is active to avoid overlap
                var tooltip = chart.tooltip;
                if (tooltip && tooltip._active && tooltip._active.length > 0) return;

                const { ctx, chartArea } = chart;
                if (!chartArea) return;
                const dataset = chart.data.datasets[0];
                if (!dataset || !dataset.data.length) return;

                const total = dataset.data.reduce(function(a, b) { return a + b; }, 0);
                const centerX = (chartArea.left + chartArea.right) / 2;
                const centerY = (chartArea.top + chartArea.bottom) / 2;
                const isDark = darkModeFn();

                ctx.save();
                ctx.font = 'bold 18px system-ui, -apple-system, sans-serif';
                ctx.fillStyle = isDark ? '#E5E7EB' : '#1F2937';
                ctx.textAlign = 'center';
                ctx.textBaseline = 'middle';
                ctx.fillText('$' + formatFn(total), centerX, centerY - 8);

                ctx.font = '11px system-ui, -apple-system, sans-serif';
                ctx.fillStyle = isDark ? '#9CA3AF' : '#6B7280';
                ctx.fillText('總計', centerX, centerY + 12);
                ctx.restore();
            }
        };
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

        const isDarkMode = this.isDarkMode();
        const labels = categories.map(c => WeGo.CATEGORY_LABELS[c.category] || c.category);
        const data = categories.map(c => c.amount);
        const colors = categories.map(c => c.color);
        const self = this;

        this.charts.category = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: data,
                    backgroundColor: colors,
                    borderWidth: 2,
                    borderColor: isDarkMode ? '#1F2937' : '#FFFFFF',
                    hoverOffset: 10,
                    hoverBorderWidth: 0,
                    spacing: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '65%',
                animation: this.prefersReducedMotion ? false : {
                    animateRotate: true,
                    duration: 800,
                    easing: 'easeOutQuart'
                },
                plugins: {
                    legend: {
                        display: false
                    },
                    tooltip: {
                        usePointStyle: true,
                        cornerRadius: 8,
                        padding: 12,
                        backgroundColor: isDarkMode ? 'rgba(31, 41, 55, 0.95)' : 'rgba(255, 255, 255, 0.95)',
                        titleColor: isDarkMode ? '#F3F4F6' : '#1F2937',
                        bodyColor: isDarkMode ? '#D1D5DB' : '#4B5563',
                        borderColor: isDarkMode ? 'rgba(255, 255, 255, 0.1)' : '#E5E7EB',
                        borderWidth: 1,
                        titleFont: { weight: '600' },
                        callbacks: {
                            label: function(context) {
                                var value = context.raw;
                                var percentage = categories[context.dataIndex].percentage;
                                return ' $' + self.formatNumber(value) + ' (' + percentage.toFixed(1) + '%)';
                            }
                        }
                    }
                }
            },
            plugins: [this.createCenterTextPlugin(this.formatNumber, this.isDarkMode.bind(this))]
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
                <span class="text-gray-700 dark:text-gray-300 truncate">${this.escapeHtml(WeGo.CATEGORY_LABELS[cat.category] || cat.category)}</span>
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

            // Update average per day with countUp animation
            var avgEl = document.getElementById('average-per-day');
            if (typeof WeGo !== 'undefined' && WeGo.anime && WeGo.anime.countUp) {
                WeGo.anime.countUp(avgEl, data.averagePerDay, { format: 'currency', currency: data.currency || 'TWD' });
            } else {
                avgEl.textContent = '$' + this.formatNumber(data.averagePerDay);
            }

            // Cache data for theme change recreation and scroll-trigger deferred rendering
            this.cachedData.trend = data.dataPoints;

            // Render chart: immediately if already visible, or wait for scroll trigger
            if (this._revealed.trend || !WeGo.anime || !WeGo.anime.onVisible) {
                this.renderTrendChart(data.dataPoints);
            }

        } catch (error) {
            loadingEl.classList.add('hidden');
            emptyEl.classList.remove('hidden');
            emptyEl.textContent = '載入失敗，請稍後再試';
        }
    },

    /**
     * Create crosshair plugin for line chart
     * @param {Function} darkModeFn - Dark mode detection function
     * @returns {Object} Chart.js plugin
     */
    createCrosshairPlugin(darkModeFn) {
        return {
            id: 'crosshair',
            afterDraw: function(chart) {
                var tooltip = chart.tooltip;
                if (tooltip && tooltip._active && tooltip._active.length) {
                    var x = tooltip._active[0].element.x;
                    var chartArea = chart.chartArea;
                    var ctx = chart.ctx;
                    var isDark = darkModeFn();

                    ctx.save();
                    ctx.setLineDash([4, 4]);
                    ctx.strokeStyle = isDark ? 'rgba(255, 255, 255, 0.15)' : 'rgba(0, 0, 0, 0.1)';
                    ctx.lineWidth = 1;
                    ctx.beginPath();
                    ctx.moveTo(x, chartArea.top);
                    ctx.lineTo(x, chartArea.bottom);
                    ctx.stroke();
                    ctx.restore();
                }
            }
        };
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
        const self = this;

        // Cache gradient to avoid re-creation on every animation frame
        var lineGradientCache = null;
        var lineGradientHeight = 0;

        this.charts.trend = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: '支出金額',
                    data: data,
                    borderColor: primaryColor,
                    backgroundColor: function(context) {
                        var chart = context.chart;
                        var chartArea = chart.chartArea;
                        if (!chartArea) return 'rgba(14, 165, 233, 0.1)';
                        var h = chartArea.bottom - chartArea.top;
                        if (lineGradientCache && h === lineGradientHeight) return lineGradientCache;
                        lineGradientHeight = h;
                        lineGradientCache = chart.ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
                        lineGradientCache.addColorStop(0, isDarkMode ? 'rgba(14, 165, 233, 0.2)' : 'rgba(14, 165, 233, 0.25)');
                        lineGradientCache.addColorStop(1, 'rgba(14, 165, 233, 0)');
                        return lineGradientCache;
                    },
                    fill: true,
                    tension: 0.4,
                    borderWidth: 2.5,
                    pointRadius: 0,
                    pointHoverRadius: 6,
                    pointBackgroundColor: primaryColor,
                    pointBorderColor: isDarkMode ? '#1F2937' : '#FFFFFF',
                    pointBorderWidth: 2,
                    pointHoverBackgroundColor: primaryColor,
                    pointHoverBorderColor: isDarkMode ? '#1F2937' : '#FFFFFF',
                    pointHoverBorderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: {
                    mode: 'index',
                    intersect: false
                },
                animation: this.prefersReducedMotion ? false : {
                    duration: 1200,
                    easing: 'easeOutQuart'
                },
                plugins: {
                    legend: {
                        display: false
                    },
                    tooltip: {
                        usePointStyle: true,
                        cornerRadius: 8,
                        padding: 12,
                        backgroundColor: isDarkMode ? 'rgba(31, 41, 55, 0.95)' : 'rgba(255, 255, 255, 0.95)',
                        titleColor: isDarkMode ? '#F3F4F6' : '#1F2937',
                        bodyColor: isDarkMode ? '#D1D5DB' : '#4B5563',
                        borderColor: isDarkMode ? 'rgba(255, 255, 255, 0.1)' : '#E5E7EB',
                        borderWidth: 1,
                        titleFont: { weight: '600' },
                        callbacks: {
                            label: function(context) {
                                var point = dataPoints[context.dataIndex];
                                return ' $' + self.formatNumber(context.raw) + ' (' + point.count + ' 筆)';
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
                        grid: {
                            color: isDarkMode ? 'rgba(255, 255, 255, 0.06)' : 'rgba(0, 0, 0, 0.06)',
                            drawTicks: false,
                            borderDash: [4, 4]
                        },
                        border: {
                            display: false
                        },
                        ticks: {
                            padding: 8,
                            callback: function(value) { return '$' + self.formatNumber(value); }
                        }
                    }
                }
            },
            plugins: [this.createCrosshairPlugin(this.isDarkMode.bind(this))]
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

            // Stagger-in member cards
            if (typeof WeGo !== 'undefined' && WeGo.anime && WeGo.anime.staggerIn) {
                WeGo.anime.staggerIn('#members-list > div', { delay: 60, duration: 200 });
            }

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

        var canStagger = typeof WeGo !== 'undefined' && WeGo.anime && WeGo.anime.staggerIn
            && typeof anime !== 'undefined' && !WeGo._reducedMotion;
        var staggerStyle = canStagger ? ' style="opacity:0;transform:translateY(20px)"' : '';

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
                <div class="bg-white dark:bg-gray-800 rounded-xl p-4 shadow-card"${staggerStyle}>
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
