/**
 * WeGo - Demo Preview JavaScript Module
 *
 * Handles demo page interactions: tab switching, CTA modal,
 * timeline day-group toggle, expense sub-tabs, and Chart.js rendering.
 *
 * Dependencies: Chart.js (optional, for expense charts)
 */
(function() {
    'use strict';

    // ========================================
    // CTA Messages Map
    // ========================================
    var CTA_MESSAGES = {
        'add-activity': '想要新增自己的景點？',
        'edit-activity': '想要編輯行程安排？',
        'delete-activity': '想要管理行程安排？',
        'add-expense': '想要記錄旅費？',
        'edit-expense': '想要編輯費用紀錄？',
        'delete-expense': '想要管理費用紀錄？',
        'add-todo': '想要新增待辦事項？',
        'edit-todo': '想要管理待辦事項？',
        'invite-member': '想要邀請朋友一起規劃？',
        'upload-document': '想要上傳旅遊文件？',
        'edit-trip': '想要編輯行程資訊？',
        'edit-budget': '想要設定個人預算？',
        'chat-exhausted': '想要無限暢聊 AI 助手？',
        'default': '想要開始規劃自己的旅程？'
    };

    // ========================================
    // Tab Switching
    // ========================================
    function initTabSwitching() {
        document.addEventListener('click', function(e) {
            var tabBtn = e.target.closest('[data-tab]');
            if (!tabBtn) return;

            e.preventDefault();
            var targetTab = tabBtn.getAttribute('data-tab');
            switchTab(targetTab);
        });
    }

    function switchTab(tabName) {
        // Hide all tab contents
        document.querySelectorAll('.tab-content').forEach(function(el) {
            el.classList.add('hidden');
        });

        // Show target tab
        var target = document.getElementById('tab-' + tabName);
        if (target) {
            target.classList.remove('hidden');
        }

        // Update pill bar active state: toggle background on the active button
        // Each tab keeps its own text color; we only switch the background highlight
        document.querySelectorAll('#demo-pill-bar .demo-tab-link').forEach(function(btn) {
            btn.classList.remove('bg-primary-100/60', 'dark:bg-primary-900/40');
        });
        var activeBtn = document.querySelector('#demo-pill-bar [data-tab="' + tabName + '"]');
        if (activeBtn) {
            activeBtn.classList.add('bg-primary-100/60', 'dark:bg-primary-900/40');
        }

        // Initialize charts if switching to expenses tab
        if (tabName === 'expenses') {
            initChartsIfNeeded();
        }

        // Scroll to top of content
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    // ========================================
    // CTA Modal
    // ========================================
    function initCtaModal() {
        var backdrop = document.getElementById('cta-modal-backdrop');
        var modal = document.getElementById('cta-modal');
        var content = document.getElementById('cta-modal-content');
        var dismiss = document.getElementById('cta-dismiss');

        if (!modal) return;

        // Intercept all CTA-triggering clicks
        document.addEventListener('click', function(e) {
            var ctaBtn = e.target.closest('[data-demo-cta]');
            if (!ctaBtn) return;

            e.preventDefault();
            e.stopPropagation();

            var ctaType = ctaBtn.getAttribute('data-demo-cta');
            var title = CTA_MESSAGES[ctaType] || CTA_MESSAGES['default'];
            showCtaModal(title);
        });

        // Dismiss handlers
        if (dismiss) {
            dismiss.addEventListener('click', hideCtaModal);
        }
        if (backdrop) {
            backdrop.addEventListener('click', hideCtaModal);
        }

        // ESC key
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') hideCtaModal();
        });
    }

    function showCtaModal(title) {
        var backdrop = document.getElementById('cta-modal-backdrop');
        var modal = document.getElementById('cta-modal');
        var content = document.getElementById('cta-modal-content');
        var titleEl = document.getElementById('cta-title');

        if (titleEl) titleEl.textContent = title;

        backdrop.classList.remove('hidden');
        modal.classList.remove('hidden');

        // Trigger animation
        requestAnimationFrame(function() {
            backdrop.classList.remove('opacity-0');
            content.classList.remove('scale-95', 'opacity-0');
            content.classList.add('scale-100', 'opacity-100');
        });
    }

    function hideCtaModal() {
        var backdrop = document.getElementById('cta-modal-backdrop');
        var modal = document.getElementById('cta-modal');
        var content = document.getElementById('cta-modal-content');

        backdrop.classList.add('opacity-0');
        content.classList.remove('scale-100', 'opacity-100');
        content.classList.add('scale-95', 'opacity-0');

        setTimeout(function() {
            backdrop.classList.add('hidden');
            modal.classList.add('hidden');
        }, 300);
    }

    // ========================================
    // Timeline Interactions (Day groups)
    // ========================================
    function initTimelineInteractions() {
        document.addEventListener('click', function(e) {
            // Day group expand/collapse
            var toggleBtn = e.target.closest('[data-action="toggle-group"]');
            if (toggleBtn) {
                e.preventDefault();
                var dayGroup = toggleBtn.closest('[data-day-group]');
                if (dayGroup) {
                    var content = dayGroup.querySelector('.day-group-content');
                    var chevron = toggleBtn.querySelector('.chevron-icon');
                    if (content) {
                        content.classList.toggle('hidden');
                    }
                    if (chevron) {
                        chevron.classList.toggle('rotate-180');
                    }
                }
                return;
            }

            // Date navigation buttons
            var dateBtn = e.target.closest('[data-date-nav]');
            if (dateBtn) {
                e.preventDefault();
                var targetDay = dateBtn.getAttribute('data-date-nav');
                var daySection = document.querySelector('[data-day-group="' + targetDay + '"]');
                if (daySection) {
                    daySection.scrollIntoView({ behavior: 'smooth', block: 'start' });
                }
                // Update active state
                document.querySelectorAll('[data-date-nav]').forEach(function(btn) {
                    btn.classList.remove('bg-primary-600', 'text-white');
                    btn.classList.add('bg-gray-100', 'dark:bg-gray-800', 'text-gray-600');
                });
                dateBtn.classList.add('bg-primary-600', 'text-white');
                dateBtn.classList.remove('bg-gray-100', 'dark:bg-gray-800', 'text-gray-600');
                return;
            }
        });
    }

    // ========================================
    // Expense Sub-Tab Switching (Team / Personal)
    // ========================================
    function initExpenseSubTabs() {
        document.addEventListener('click', function(e) {
            var subTabBtn = e.target.closest('[data-expense-tab]');
            if (!subTabBtn) return;

            e.preventDefault();
            var targetSubTab = subTabBtn.getAttribute('data-expense-tab');

            // Hide all sub-tab contents
            document.querySelectorAll('.expense-sub-tab').forEach(function(el) {
                el.classList.add('hidden');
            });

            // Show target
            var target = document.getElementById('expense-' + targetSubTab);
            if (target) target.classList.remove('hidden');

            // Update active state
            document.querySelectorAll('[data-expense-tab]').forEach(function(btn) {
                btn.classList.remove('bg-primary-600', 'text-white');
                btn.classList.add('bg-gray-200', 'dark:bg-gray-700', 'text-gray-600');
            });
            subTabBtn.classList.add('bg-primary-600', 'text-white');
            subTabBtn.classList.remove('bg-gray-200', 'dark:bg-gray-700', 'text-gray-600');

            // Init charts when switching to personal tab
            if (targetSubTab === 'personal') {
                initChartsIfNeeded();
            }
        });
    }

    // ========================================
    // Chart.js (Personal Expense Charts)
    // ========================================
    var chartsInitialized = false;

    function initChartsIfNeeded() {
        if (chartsInitialized) return;
        if (typeof Chart === 'undefined') return;

        var categoryDataEl = document.getElementById('demo-category-data');
        var dailyDataEl = document.getElementById('demo-daily-data');

        if (!categoryDataEl || !dailyDataEl) return;

        try {
            var categoryData = JSON.parse(categoryDataEl.textContent);
            var dailyData = JSON.parse(dailyDataEl.textContent);

            initCategoryChart(categoryData);
            initDailyChart(dailyData);
            chartsInitialized = true;
        } catch (e) {
            // Chart data parse error - skip silently
        }
    }

    var CATEGORY_COLORS = {
        'FOOD': '#f97316',
        'TRANSPORT': '#3b82f6',
        'ACCOMMODATION': '#8b5cf6',
        'SHOPPING': '#ec4899',
        'ENTERTAINMENT': '#f43f5e',
        'HEALTH': '#10b981',
        'OTHER': '#6b7280'
    };

    var CATEGORY_LABELS = {
        'FOOD': '美食',
        'TRANSPORT': '交通',
        'ACCOMMODATION': '住宿',
        'SHOPPING': '購物',
        'ENTERTAINMENT': '娛樂',
        'HEALTH': '健康',
        'OTHER': '其他'
    };

    function initCategoryChart(data) {
        var canvas = document.getElementById('demo-category-chart');
        if (!canvas) return;

        var labels = [];
        var values = [];
        var colors = [];

        for (var key in data) {
            labels.push(CATEGORY_LABELS[key] || key);
            values.push(data[key]);
            colors.push(CATEGORY_COLORS[key] || '#6b7280');
        }

        new Chart(canvas, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: values,
                    backgroundColor: colors,
                    borderWidth: 0,
                    hoverOffset: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '65%',
                plugins: {
                    legend: { display: false }
                }
            }
        });
    }

    function initDailyChart(data) {
        var canvas = document.getElementById('demo-daily-chart');
        if (!canvas) return;

        var labels = [];
        var values = [];

        for (var key in data) {
            // Format date label
            var parts = key.split('-');
            labels.push(parts[1] + '/' + parts[2]);
            values.push(data[key]);
        }

        new Chart(canvas, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    data: values,
                    backgroundColor: '#6366f1',
                    borderRadius: 8,
                    barPercentage: 0.6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true,
                        grid: { color: 'rgba(0,0,0,0.05)' },
                        ticks: { callback: function(v) { return '\u00a5' + v.toLocaleString(); } }
                    },
                    x: {
                        grid: { display: false }
                    }
                },
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: function(ctx) { return '\u00a5' + ctx.raw.toLocaleString(); }
                        }
                    }
                }
            }
        });
    }

    // ========================================
    // Personal Expense Chart Tab Switcher
    // ========================================
    function initChartTabSwitcher() {
        document.addEventListener('click', function(e) {
            var chartTabBtn = e.target.closest('[data-chart-tab]');
            if (!chartTabBtn) return;

            e.preventDefault();
            var targetChart = chartTabBtn.getAttribute('data-chart-tab');

            document.querySelectorAll('.chart-panel').forEach(function(el) {
                el.classList.add('hidden');
            });

            var target = document.getElementById('chart-' + targetChart);
            if (target) target.classList.remove('hidden');

            document.querySelectorAll('[data-chart-tab]').forEach(function(btn) {
                btn.classList.remove('bg-primary-600', 'text-white');
                btn.classList.add('bg-gray-200', 'text-gray-600');
            });
            chartTabBtn.classList.add('bg-primary-600', 'text-white');
            chartTabBtn.classList.remove('bg-gray-200', 'text-gray-600');
        });
    }

    // ========================================
    // Init
    // ========================================
    document.addEventListener('DOMContentLoaded', function() {
        initTabSwitching();
        initCtaModal();
        initTimelineInteractions();
        initExpenseSubTabs();
        initChartTabSwitcher();
    });

})();
