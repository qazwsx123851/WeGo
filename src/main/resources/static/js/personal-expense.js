/**
 * WeGo - Personal Expense Module
 *
 * Handles personal expense list interactions: tab switching,
 * budget modal, delete modal, chart rendering, date headers,
 * and exchange rate auto-fetch.
 *
 * @module PersonalExpense
 */
const PersonalExpense = (() => {
    'use strict';

    // =====================
    // Private State
    // =====================
    let pendingDeleteBtn = null;
    let deleteTriggerEl = null;
    let budgetTriggerEl = null;
    let categoryChart = null;
    let dailyChart = null;
    const prefersReducedMotion = WeGo._reducedMotion;

    // =====================
    // Tab Switching (C1: dynamic header CTA href)
    // =====================
    function switchTab(tab) {
        const isPersonal = tab === 'personal';
        const teamContent = document.getElementById('content-team');
        const personalContent = document.getElementById('content-personal');
        const tabTeam = document.getElementById('tab-team');
        const tabPersonal = document.getElementById('tab-personal');

        if (teamContent) teamContent.classList.toggle('hidden', isPersonal);
        if (personalContent) personalContent.classList.toggle('hidden', !isPersonal);

        // Slide indicator to active tab
        const mainIndicator = document.getElementById('main-tab-indicator');
        if (mainIndicator) {
            mainIndicator.style.left = isPersonal ? 'calc(50% + 2px)' : '4px';
        }

        if (tabTeam) {
            tabTeam.classList.toggle('text-gray-800', !isPersonal);
            tabTeam.classList.toggle('dark:text-gray-100', !isPersonal);
            tabTeam.classList.toggle('text-gray-500', isPersonal);
            tabTeam.classList.toggle('dark:text-gray-400', isPersonal);
        }

        if (tabPersonal) {
            tabPersonal.classList.toggle('text-gray-800', isPersonal);
            tabPersonal.classList.toggle('dark:text-gray-100', isPersonal);
            tabPersonal.classList.toggle('text-gray-500', !isPersonal);
            tabPersonal.classList.toggle('dark:text-gray-400', !isPersonal);
        }

        // C1: Update header CTA href based on active tab
        const headerAddBtn = document.getElementById('header-add-btn');
        if (headerAddBtn) {
            headerAddBtn.href = isPersonal
                ? (headerAddBtn.dataset.personalHref || headerAddBtn.href)
                : (headerAddBtn.dataset.teamHref || headerAddBtn.href);
        }

        // Update URL without reload
        const url = new URL(window.location);
        if (isPersonal) {
            url.searchParams.set('tab', 'personal');
        } else {
            url.searchParams.delete('tab');
        }
        window.history.replaceState({}, '', url);
    }

    // =====================
    // M5: Smooth toggle for AUTO item details
    // =====================
    function toggleAutoItem(tripExpenseId) {
        const detail = document.getElementById('auto-detail-' + tripExpenseId);
        const chevron = document.getElementById('auto-chevron-' + tripExpenseId);
        if (!detail) return;

        const isOpen = detail.style.maxHeight && detail.style.maxHeight !== '0px';
        if (isOpen) {
            detail.style.maxHeight = '0px';
            if (chevron) chevron.classList.remove('rotate-180');
        } else {
            detail.style.maxHeight = detail.scrollHeight + 'px';
            if (chevron) chevron.classList.add('rotate-180');
        }
    }

    // =====================
    // Focus trap utility for modals
    // =====================
    function trapFocus(modal) {
        const focusable = modal.querySelectorAll(
            'button:not([disabled]), input:not([disabled]), a[href], [tabindex]:not([tabindex="-1"])'
        );
        if (focusable.length === 0) return;
        const first = focusable[0];
        const last = focusable[focusable.length - 1];

        modal._focusTrapHandler = function(e) {
            if (e.key !== 'Tab') return;
            if (e.shiftKey) {
                if (document.activeElement === first) { e.preventDefault(); last.focus(); }
            } else {
                if (document.activeElement === last) { e.preventDefault(); first.focus(); }
            }
        };
        modal.addEventListener('keydown', modal._focusTrapHandler);
    }

    function releaseFocusTrap(modal) {
        if (modal && modal._focusTrapHandler) {
            modal.removeEventListener('keydown', modal._focusTrapHandler);
            modal._focusTrapHandler = null;
        }
    }

    // =====================
    // Delete Modal (C3: custom modal replacing native confirm)
    // =====================
    function showDeleteModal(btn) {
        pendingDeleteBtn = btn;
        deleteTriggerEl = document.activeElement;
        const modal = document.getElementById('personal-delete-modal');
        const desc = document.getElementById('personal-delete-desc');
        const expenseDesc = btn.dataset.expenseDesc || '';
        if (desc) {
            desc.textContent = expenseDesc
                ? '確定要刪除「' + expenseDesc + '」嗎？刪除後無法復原。'
                : '刪除後無法復原。';
        }
        if (modal) {
            modal.classList.remove('hidden');
            trapFocus(modal);
            const cancelBtn = modal.querySelector('button');
            if (cancelBtn) cancelBtn.focus();
        }
    }

    function closeDeleteModal() {
        const modal = document.getElementById('personal-delete-modal');
        if (modal) {
            releaseFocusTrap(modal);
            modal.classList.add('hidden');
        }
        pendingDeleteBtn = null;
        if (deleteTriggerEl) { deleteTriggerEl.focus(); deleteTriggerEl = null; }
    }

    function confirmDelete() {
        if (!pendingDeleteBtn) return;
        const btn = pendingDeleteBtn;
        const expenseId = btn.dataset.expenseId;
        const tripId = btn.dataset.tripId;
        const csrfToken = WeGo.getCsrfToken();

        const confirmBtn = document.getElementById('personal-delete-confirm-btn');
        const spinner = document.getElementById('personal-delete-spinner');
        if (confirmBtn) confirmBtn.disabled = true;
        if (spinner) spinner.classList.remove('hidden');

        WeGo.fetchWithTimeout('/api/trips/' + tripId + '/personal-expenses/' + expenseId, {
            method: 'DELETE',
            headers: { 'X-XSRF-TOKEN': csrfToken }
        }).then(function(res) {
            if (res.ok) {
                closeDeleteModal();
                const card = btn.closest('.expense-item-card');
                if (card) {
                    card.style.transition = 'opacity 300ms ease-out, transform 300ms ease-out';
                    card.style.opacity = '0';
                    card.style.transform = 'translateX(-16px)';
                    setTimeout(function() {
                        card.remove();
                        window.location.reload();
                    }, 350);
                } else {
                    window.location.reload();
                }
                if (typeof Toast !== 'undefined') Toast.success('已刪除費用');
            } else {
                closeDeleteModal();
                if (typeof Toast !== 'undefined') Toast.error('刪除失敗，請重試');
            }
        }).catch(function() {
            closeDeleteModal();
            if (typeof Toast !== 'undefined') Toast.error('刪除失敗，請重試');
        }).finally(function() {
            if (confirmBtn) confirmBtn.disabled = false;
            if (spinner) spinner.classList.add('hidden');
        });
    }

    // =====================
    // Budget modal (C2: accessibility, H6: loading, M4: prefill)
    // =====================
    function showBudgetModal() {
        budgetTriggerEl = document.activeElement;
        const modal = document.getElementById('budget-modal');
        const input = document.getElementById('budget-input');
        if (modal) {
            modal.classList.remove('hidden');
            modal.classList.add('flex');
            trapFocus(modal);
        }
        const budgetDisplay = document.getElementById('budget-display');
        if (budgetDisplay && budgetDisplay.dataset.budget && input) {
            input.value = budgetDisplay.dataset.budget;
        }
        if (input) {
            setTimeout(function() { input.focus(); }, 100);
        }
    }

    function closeBudgetModal() {
        const modal = document.getElementById('budget-modal');
        if (modal) {
            releaseFocusTrap(modal);
            modal.classList.add('hidden');
            modal.classList.remove('flex');
        }
        if (budgetTriggerEl) { budgetTriggerEl.focus(); budgetTriggerEl = null; }
    }

    function submitBudget() {
        const budgetInput = document.getElementById('budget-input');
        const budget = budgetInput ? budgetInput.value : null;
        if (!budget || parseFloat(budget) <= 0) return;

        const pathParts = window.location.pathname.split('/');
        const tripsIndex = pathParts.indexOf('trips');
        const tripId = tripsIndex !== -1 ? pathParts[tripsIndex + 1] : null;
        if (!tripId) return;

        const submitBtn = document.getElementById('budget-submit-btn');
        const spinner = document.getElementById('budget-spinner');
        const submitText = document.getElementById('budget-submit-text');
        if (submitBtn) submitBtn.disabled = true;
        if (spinner) spinner.classList.remove('hidden');
        if (submitText) submitText.textContent = '設定中...';

        const csrfToken = WeGo.getCsrfToken();
        WeGo.fetchWithTimeout('/api/trips/' + tripId + '/personal-expenses/budget', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': csrfToken },
            body: JSON.stringify({ budget: parseFloat(budget) })
        }).then(function(res) {
            if (res.ok) {
                window.location.reload();
            } else {
                if (typeof Toast !== 'undefined') Toast.error('設定失敗，請重試');
            }
        }).catch(function() {
            if (typeof Toast !== 'undefined') Toast.error('設定失敗，請重試');
        }).finally(function() {
            if (submitBtn) submitBtn.disabled = false;
            if (spinner) spinner.classList.add('hidden');
            if (submitText) submitText.textContent = '確定';
        });
    }

    // =====================
    // Chart tab switching & initialization
    // =====================
    function showChart(type) {
        const catDiv = document.getElementById('chart-category');
        const dailyDiv = document.getElementById('chart-daily');
        const catTab = document.getElementById('chart-tab-category');
        const dailyTab = document.getElementById('chart-tab-daily');

        if (type === 'category') {
            if (dailyDiv) { dailyDiv.classList.add('hidden'); dailyDiv.style.opacity = '0'; }
            if (catDiv) { catDiv.classList.remove('hidden'); catDiv.style.opacity = '0'; setTimeout(function() { catDiv.style.opacity = '1'; }, 10); }
        } else {
            if (catDiv) { catDiv.classList.add('hidden'); catDiv.style.opacity = '0'; }
            if (dailyDiv) { dailyDiv.classList.remove('hidden'); dailyDiv.style.opacity = '0'; setTimeout(function() { dailyDiv.style.opacity = '1'; }, 10); }
        }

        // Slide indicator to active tab
        const chartIndicator = document.getElementById('chart-tab-indicator');
        if (chartIndicator) {
            if (type === 'category') {
                chartIndicator.style.left = '4px';
            } else {
                chartIndicator.style.left = 'calc(50% + 2px)';
            }
        }

        if (catTab) {
            catTab.classList.toggle('text-gray-800', type === 'category');
            catTab.classList.toggle('dark:text-gray-100', type === 'category');
            catTab.classList.toggle('text-gray-500', type !== 'category');
            catTab.classList.toggle('dark:text-gray-400', type !== 'category');
        }
        if (dailyTab) {
            dailyTab.classList.toggle('text-gray-800', type === 'daily');
            dailyTab.classList.toggle('dark:text-gray-100', type === 'daily');
            dailyTab.classList.toggle('text-gray-500', type !== 'daily');
            dailyTab.classList.toggle('dark:text-gray-400', type !== 'daily');
        }

        if (type === 'daily' && dailyChart) {
            setTimeout(function() { dailyChart.resize(); }, 20);
            const wrapper = document.getElementById('daily-chart-wrapper');
            if (wrapper && wrapper.dataset.needsScroll === 'true') {
                wrapper.classList.add('overflow-x-auto', 'overscroll-x-contain');
            }
        }
    }

    function initCharts() {
        const dataEl = document.getElementById('personal-chart-data');
        if (!dataEl) return;
        if (typeof Chart === 'undefined') return;

        try {
            const categoryData = JSON.parse(dataEl.dataset.categoryBreakdown || '{}');
            const dailyData = JSON.parse(dataEl.dataset.dailyAmounts || '{}');
            const isDark = document.documentElement.classList.contains('dark');

            // Category doughnut
            const catCanvas = document.getElementById('personal-category-chart');
            if (catCanvas && Object.keys(categoryData).length > 0) {
                const labels = Object.keys(categoryData).map(function(k) { return WeGo.CATEGORY_LABELS[k] || k; });
                const data = Object.values(categoryData).map(function(v) { return parseFloat(v); });
                const colors = Object.keys(categoryData).map(function(k) { return WeGo.CATEGORY_COLORS[k] || '#6B7280'; });

                const personalCenterTextPlugin = {
                    id: 'personalCenterText',
                    afterDraw: function(chart) {
                        const tooltip = chart.tooltip;
                        if (tooltip && tooltip._active && tooltip._active.length > 0) return;

                        const ctx = chart.ctx;
                        const chartArea = chart.chartArea;
                        if (!chartArea) return;
                        const dataset = chart.data.datasets[0];
                        if (!dataset || !dataset.data.length) return;

                        const total = dataset.data.reduce(function(a, b) { return a + b; }, 0);
                        const centerX = (chartArea.left + chartArea.right) / 2;
                        const centerY = (chartArea.top + chartArea.bottom) / 2;
                        const dark = document.documentElement.classList.contains('dark');

                        ctx.save();
                        ctx.font = 'bold 16px system-ui, -apple-system, sans-serif';
                        ctx.fillStyle = dark ? '#E5E7EB' : '#1F2937';
                        ctx.textAlign = 'center';
                        ctx.textBaseline = 'middle';
                        ctx.fillText('$' + total.toLocaleString('en-US', { maximumFractionDigits: 0 }), centerX, centerY - 7);

                        ctx.font = '10px system-ui, -apple-system, sans-serif';
                        ctx.fillStyle = dark ? '#9CA3AF' : '#6B7280';
                        ctx.fillText('總計', centerX, centerY + 10);
                        ctx.restore();
                    }
                };

                categoryChart = new Chart(catCanvas, {
                    type: 'doughnut',
                    data: {
                        labels: labels,
                        datasets: [{
                            data: data,
                            backgroundColor: colors,
                            borderWidth: 2,
                            borderColor: isDark ? '#1F2937' : '#FFFFFF',
                            hoverOffset: 10,
                            hoverBorderWidth: 0,
                            spacing: 2
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        cutout: '65%',
                        animation: prefersReducedMotion ? false : {
                            animateRotate: true,
                            duration: 800,
                            easing: 'easeOutQuart'
                        },
                        plugins: {
                            legend: { display: false },
                            tooltip: {
                                usePointStyle: true,
                                cornerRadius: 8,
                                padding: 12,
                                backgroundColor: isDark ? 'rgba(31, 41, 55, 0.95)' : 'rgba(255, 255, 255, 0.95)',
                                titleColor: isDark ? '#F3F4F6' : '#1F2937',
                                bodyColor: isDark ? '#D1D5DB' : '#4B5563',
                                borderColor: isDark ? 'rgba(255, 255, 255, 0.1)' : '#E5E7EB',
                                borderWidth: 1,
                                titleFont: { weight: '600' },
                                callbacks: {
                                    label: function(context) {
                                        const value = context.raw;
                                        const total = context.dataset.data.reduce(function(a, b) { return a + b; }, 0);
                                        const pct = total > 0 ? (value / total * 100).toFixed(1) : '0.0';
                                        return ' $' + value.toLocaleString('en-US', { maximumFractionDigits: 0 }) + ' (' + pct + '%)';
                                    }
                                }
                            }
                        }
                    },
                    plugins: [personalCenterTextPlugin]
                });
                const total = data.reduce(function(sum, v) { return sum + v; }, 0);
                renderCategoryLegend(categoryData, total);
            }

            // Daily bar chart
            const dailyCanvas = document.getElementById('personal-daily-chart');
            if (dailyCanvas && Object.keys(dailyData).length > 0) {
                const gridColor = isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)';
                const textColor = isDark ? '#9CA3AF' : '#6B7280';
                const tooltipBg = isDark ? 'rgba(31,41,55,0.95)' : 'rgba(255,255,255,0.95)';
                const tooltipTitle = isDark ? '#F3F4F6' : '#1F2937';
                const tooltipBody = isDark ? '#D1D5DB' : '#4B5563';
                const tooltipBorder = isDark ? 'rgba(255,255,255,0.1)' : '#E5E7EB';

                const rawLabels = Object.keys(dailyData);
                const rawData = Object.values(dailyData).map(function(v) { return parseFloat(v); });

                // Trim future zero-value dates
                const now = new Date();
                const today = now.getFullYear() + '-'
                    + String(now.getMonth() + 1).padStart(2, '0') + '-'
                    + String(now.getDate()).padStart(2, '0');
                let dLabels = [];
                let dData = [];
                rawLabels.forEach(function(label, i) {
                    if (label <= today || rawData[i] > 0) {
                        dLabels.push(label);
                        dData.push(rawData[i]);
                    }
                });
                if (dLabels.length === 0) {
                    dLabels = rawLabels;
                    dData = rawData;
                }

                const needsScroll = dLabels.length > 14;
                const wrapper = document.getElementById('daily-chart-wrapper');
                if (needsScroll && wrapper) {
                    wrapper.dataset.needsScroll = 'true';
                    wrapper.classList.add('overflow-x-auto', 'overscroll-x-contain');
                    dailyCanvas.style.minWidth = (dLabels.length * 48) + 'px';
                }

                const zeroBarColor = isDark ? 'rgba(75,85,99,0.3)' : 'rgba(209,213,219,0.5)';
                const zeroBarHover = isDark ? 'rgba(75,85,99,0.5)' : 'rgba(209,213,219,0.7)';

                let barGradientCache = null;
                let barGradientHeight = 0;

                dailyChart = new Chart(dailyCanvas, {
                    type: 'bar',
                    data: {
                        labels: dLabels,
                        datasets: [{
                            label: '花費',
                            data: dData,
                            backgroundColor: function(context) {
                                const chart = context.chart;
                                const value = context.raw;
                                if (value === 0 || value === undefined) return zeroBarColor;
                                const chartArea = chart.chartArea;
                                if (!chartArea) return '#F97316';
                                const h = chartArea.bottom - chartArea.top;
                                if (barGradientCache && h === barGradientHeight) return barGradientCache;
                                barGradientHeight = h;
                                barGradientCache = chart.ctx.createLinearGradient(0, chartArea.bottom, 0, chartArea.top);
                                barGradientCache.addColorStop(0, 'rgba(249, 115, 22, 0.6)');
                                barGradientCache.addColorStop(1, 'rgba(249, 115, 22, 1)');
                                return barGradientCache;
                            },
                            hoverBackgroundColor: function(context) {
                                const value = context.raw;
                                if (value === 0 || value === undefined) return zeroBarHover;
                                return '#EA580C';
                            },
                            borderRadius: 8,
                            maxBarThickness: 40,
                            borderSkipped: false,
                            minBarLength: 2
                        }]
                    },
                    options: {
                        responsive: !needsScroll,
                        maintainAspectRatio: false,
                        interaction: {
                            mode: 'index',
                            intersect: false
                        },
                        animation: prefersReducedMotion ? false : {
                            delay: function(context) {
                                if (context.type === 'data' && context.mode === 'default') {
                                    return Math.min(context.dataIndex * 50, 500);
                                }
                                return 0;
                            },
                            duration: 600,
                            easing: 'easeOutQuart'
                        },
                        plugins: {
                            legend: { display: false },
                            tooltip: {
                                backgroundColor: tooltipBg,
                                titleColor: tooltipTitle,
                                bodyColor: tooltipBody,
                                borderColor: tooltipBorder,
                                borderWidth: 1,
                                cornerRadius: 8,
                                padding: 12,
                                usePointStyle: true,
                                titleFont: { weight: '600' },
                                displayColors: false,
                                callbacks: {
                                    title: function(items) {
                                        const label = items[0].label || '';
                                        const parts = label.split('-');
                                        if (parts.length === 3) {
                                            return parseInt(parts[1]) + '月' + parseInt(parts[2]) + '日';
                                        }
                                        return label;
                                    },
                                    label: function(item) {
                                        if (item.raw === 0) return '無花費';
                                        return '花費 $' + item.raw.toLocaleString('zh-TW', { maximumFractionDigits: 0 });
                                    }
                                }
                            }
                        },
                        scales: {
                            x: {
                                grid: { display: false },
                                ticks: {
                                    color: textColor,
                                    maxRotation: 0,
                                    autoSkip: true,
                                    maxTicksLimit: 8,
                                    font: { size: 11 },
                                    callback: function(val) {
                                        const label = this.getLabelForValue(val);
                                        const parts = label.split('-');
                                        if (parts.length === 3) {
                                            return parseInt(parts[1]) + '/' + parseInt(parts[2]);
                                        }
                                        return label;
                                    }
                                }
                            },
                            y: {
                                beginAtZero: true,
                                grid: {
                                    color: gridColor,
                                    drawTicks: false,
                                    borderDash: [4, 4]
                                },
                                border: {
                                    display: false
                                },
                                ticks: {
                                    color: textColor,
                                    font: { size: 11 },
                                    padding: 8,
                                    maxTicksLimit: 5,
                                    callback: function(value) {
                                        if (value >= 10000) return '$' + (value / 1000) + 'K';
                                        if (value >= 1000) {
                                            const k = value / 1000;
                                            return '$' + (k % 1 === 0 ? k : k.toFixed(1)) + 'K';
                                        }
                                        return '$' + value;
                                    }
                                }
                            }
                        }
                    }
                });
            }
        } catch (e) {
            if (typeof Toast !== 'undefined') Toast.error('圖表載入失敗');
        }
    }

    function renderCategoryLegend(data, total) {
        const legendEl = document.getElementById('personal-category-legend');
        if (!legendEl) return;
        const sorted = Object.entries(data).sort(function(a, b) { return b[1] - a[1]; });
        legendEl.replaceChildren();
        sorted.forEach(function(entry) {
            const cat = entry[0], amount = entry[1];
            const pct = total > 0 ? Math.round(amount / total * 100) : 0;
            const color = WeGo.CATEGORY_COLORS[cat] || '#6B7280';
            const label = WeGo.CATEGORY_LABELS[cat] || cat;

            const div = document.createElement('div');
            div.className = 'flex items-center gap-2 px-2 py-1.5 rounded-lg bg-gray-50 dark:bg-gray-800/50';

            const dot = document.createElement('div');
            dot.className = 'w-3 h-3 rounded-full flex-shrink-0';
            dot.style.background = color;

            const labelSpan = document.createElement('span');
            labelSpan.className = 'text-xs text-gray-700 dark:text-gray-300 truncate';
            labelSpan.textContent = label;

            const pctSpan = document.createElement('span');
            pctSpan.className = 'text-xs text-gray-500 dark:text-gray-400 tabular-nums ml-auto';
            pctSpan.textContent = pct + '%';

            div.appendChild(dot);
            div.appendChild(labelSpan);
            div.appendChild(pctSpan);
            legendEl.appendChild(div);
        });
    }

    // =====================
    // H4: Enhanced date headers with calendar icon
    // =====================
    function injectDateHeaders() {
        const items = document.querySelectorAll('.expense-item-card[data-date]');
        if (!items.length) return;
        const svgNS = 'http://www.w3.org/2000/svg';
        let lastDate = null;
        items.forEach(function(item) {
            const date = item.dataset.date;
            if (date && date !== lastDate) {
                const header = document.createElement('div');
                header.className = 'flex items-center gap-2 pt-2 pb-1 px-1';

                const icon = document.createElementNS(svgNS, 'svg');
                icon.setAttribute('class', 'w-3.5 h-3.5 text-gray-400 dark:text-gray-500');
                icon.setAttribute('fill', 'none');
                icon.setAttribute('stroke', 'currentColor');
                icon.setAttribute('viewBox', '0 0 24 24');
                const path = document.createElementNS(svgNS, 'path');
                path.setAttribute('stroke-linecap', 'round');
                path.setAttribute('stroke-linejoin', 'round');
                path.setAttribute('stroke-width', '2');
                path.setAttribute('d', 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z');
                icon.appendChild(path);

                const text = document.createElement('span');
                text.className = 'text-xs font-medium text-gray-400 dark:text-gray-500';
                text.textContent = date;

                header.appendChild(icon);
                header.appendChild(text);
                item.parentNode.insertBefore(header, item);
                lastDate = date;
            }
        });
    }

    // =====================
    // Exchange Rate auto-fetch for create/edit forms
    // =====================
    function initExchangeRate() {
        const currencySelect = document.getElementById('currency');
        const baseCurrencyInput = document.getElementById('baseCurrency');
        if (!currencySelect || !baseCurrencyInput) return;

        const baseCurrency = baseCurrencyInput.value;

        currencySelect.addEventListener('change', function() {
            onCurrencyChange(this.value, baseCurrency, false);
        });

        if (currencySelect.value && currencySelect.value !== baseCurrency) {
            onCurrencyChange(currencySelect.value, baseCurrency, true);
        }
    }

    function onCurrencyChange(currency, baseCurrency, isInit) {
        const row = document.getElementById('exchange-rate-row');
        const rateInput = document.getElementById('exchangeRate');
        const rateFrom = document.getElementById('rate-from');

        if (currency === baseCurrency) {
            if (row) row.classList.add('hidden');
            if (rateInput) rateInput.value = '';
            return;
        }

        if (row) row.classList.remove('hidden');
        if (rateFrom) rateFrom.textContent = currency;

        if (isInit && rateInput && rateInput.value) return;

        fetchExchangeRate(currency, baseCurrency);
    }

    function fetchExchangeRate(from, to) {
        const rateInput = document.getElementById('exchangeRate');
        const rateError = document.getElementById('rate-error');

        if (rateInput) rateInput.placeholder = '取得中...';
        if (rateError) rateError.classList.add('hidden');

        WeGo.fetchWithTimeout('/api/exchange-rates?from=' + encodeURIComponent(from) + '&to=' + encodeURIComponent(to))
            .then(function(res) {
                if (!res.ok) throw new Error('HTTP ' + res.status);
                return res.json();
            })
            .then(function(data) {
                if (data.success && data.data && data.data.rate) {
                    if (rateInput) {
                        rateInput.value = data.data.rate;
                        rateInput.placeholder = '匯率';
                    }
                } else {
                    throw new Error('No rate data');
                }
            })
            .catch(function() {
                if (rateInput) rateInput.placeholder = '請手動輸入';
                if (rateError) rateError.classList.remove('hidden');
            });
    }

    // =====================
    // Event Delegation
    // =====================
    function handleAction(action, actionEl) {
        switch (action) {
            case 'show-budget-modal':
                showBudgetModal();
                break;
            case 'close-budget-modal':
            case 'close-budget-modal-backdrop':
                closeBudgetModal();
                break;
            case 'submit-budget':
                submitBudget();
                break;
            case 'show-chart':
                showChart(actionEl.dataset.chartType);
                break;
            case 'toggle-auto-item':
                toggleAutoItem(actionEl.dataset.expenseId);
                break;
            case 'show-delete-modal':
                showDeleteModal(actionEl);
                break;
            case 'close-delete-modal':
            case 'close-delete-modal-backdrop':
                closeDeleteModal();
                break;
            case 'confirm-delete':
                confirmDelete();
                break;
        }
    }

    // =====================
    // Initialization
    // =====================
    function init() {
        // Event delegation for data-action clicks
        document.addEventListener('click', function(e) {
            const actionEl = e.target.closest('[data-action]');
            if (!actionEl) return;

            // Backdrop actions: only fire when clicking the backdrop itself, not inner content
            const action = actionEl.dataset.action;
            if (action === 'close-budget-modal-backdrop' || action === 'close-delete-modal-backdrop') {
                if (e.target !== actionEl) return;
            }

            handleAction(action, actionEl);
        });

        // ESC key handler for modals
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') {
                const deleteModal = document.getElementById('personal-delete-modal');
                if (deleteModal && !deleteModal.classList.contains('hidden')) {
                    closeDeleteModal();
                    return;
                }
                const budgetModal = document.getElementById('budget-modal');
                if (budgetModal && !budgetModal.classList.contains('hidden')) {
                    closeBudgetModal();
                }
            }
        });

        // Initialize
        initCharts();
        injectDateHeaders();
        initExchangeRate();

        // Re-init charts on theme change
        window.addEventListener('themechange', function() {
            if (categoryChart) { categoryChart.destroy(); categoryChart = null; }
            if (dailyChart) { dailyChart.destroy(); dailyChart = null; }
            initCharts();
        });

        // C1: Set initial header CTA href based on URL param
        const url = new URL(window.location);
        if (url.searchParams.get('tab') === 'personal') {
            const headerAddBtn = document.getElementById('header-add-btn');
            if (headerAddBtn && headerAddBtn.dataset.personalHref) {
                headerAddBtn.href = headerAddBtn.dataset.personalHref;
            }
        }
    }

    return { init, switchTab };
})();

document.addEventListener('DOMContentLoaded', PersonalExpense.init);
