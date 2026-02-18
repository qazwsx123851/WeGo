// Tab switching (C1: dynamic header CTA href)
function switchTab(tab) {
    var isPersonal = tab === 'personal';
    var teamContent = document.getElementById('content-team');
    var personalContent = document.getElementById('content-personal');
    var tabTeam = document.getElementById('tab-team');
    var tabPersonal = document.getElementById('tab-personal');

    if (teamContent) teamContent.classList.toggle('hidden', isPersonal);
    if (personalContent) personalContent.classList.toggle('hidden', !isPersonal);

    if (tabTeam) {
        tabTeam.classList.toggle('bg-white', !isPersonal);
        tabTeam.classList.toggle('dark:bg-gray-700', !isPersonal);
        tabTeam.classList.toggle('text-gray-800', !isPersonal);
        tabTeam.classList.toggle('dark:text-gray-100', !isPersonal);
        tabTeam.classList.toggle('shadow-sm', !isPersonal);
        tabTeam.classList.toggle('text-gray-500', isPersonal);
        tabTeam.classList.toggle('dark:text-gray-400', isPersonal);
    }

    if (tabPersonal) {
        tabPersonal.classList.toggle('bg-white', isPersonal);
        tabPersonal.classList.toggle('dark:bg-gray-700', isPersonal);
        tabPersonal.classList.toggle('text-gray-800', isPersonal);
        tabPersonal.classList.toggle('dark:text-gray-100', isPersonal);
        tabPersonal.classList.toggle('shadow-sm', isPersonal);
        tabPersonal.classList.toggle('text-gray-500', !isPersonal);
        tabPersonal.classList.toggle('dark:text-gray-400', !isPersonal);
    }

    // C1: Update header CTA href based on active tab
    var headerAddBtn = document.getElementById('header-add-btn');
    if (headerAddBtn) {
        headerAddBtn.href = isPersonal
            ? (headerAddBtn.dataset.personalHref || headerAddBtn.href)
            : (headerAddBtn.dataset.teamHref || headerAddBtn.href);
    }

    // Update URL without reload
    var url = new URL(window.location);
    if (isPersonal) {
        url.searchParams.set('tab', 'personal');
    } else {
        url.searchParams.delete('tab');
    }
    window.history.replaceState({}, '', url);
}

// M5: Smooth toggle for AUTO item details
function toggleAutoItem(tripExpenseId) {
    var detail = document.getElementById('auto-detail-' + tripExpenseId);
    var chevron = document.getElementById('auto-chevron-' + tripExpenseId);
    if (!detail) return;

    var isOpen = detail.style.maxHeight && detail.style.maxHeight !== '0px';
    if (isOpen) {
        detail.style.maxHeight = '0px';
        if (chevron) chevron.classList.remove('rotate-180');
    } else {
        detail.style.maxHeight = detail.scrollHeight + 'px';
        if (chevron) chevron.classList.add('rotate-180');
    }
}

// === Focus trap utility for modals ===
function trapFocus(modal) {
    var focusable = modal.querySelectorAll(
        'button:not([disabled]), input:not([disabled]), a[href], [tabindex]:not([tabindex="-1"])'
    );
    if (focusable.length === 0) return;
    var first = focusable[0];
    var last = focusable[focusable.length - 1];

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

// === Delete Modal (C3: custom modal replacing native confirm) ===
var pendingDeleteBtn = null;
var deleteTriggerEl = null;

function showDeleteModal(btn) {
    pendingDeleteBtn = btn;
    deleteTriggerEl = document.activeElement;
    var modal = document.getElementById('personal-delete-modal');
    var desc = document.getElementById('personal-delete-desc');
    var expenseDesc = btn.dataset.expenseDesc || '';
    if (desc) {
        desc.textContent = expenseDesc
            ? '確定要刪除「' + expenseDesc + '」嗎？刪除後無法復原。'
            : '刪除後無法復原。';
    }
    if (modal) {
        modal.classList.remove('hidden');
        trapFocus(modal);
        // Focus the cancel button for accessibility
        var cancelBtn = modal.querySelector('button');
        if (cancelBtn) cancelBtn.focus();
    }
}

function closePersonalDeleteModal() {
    var modal = document.getElementById('personal-delete-modal');
    if (modal) {
        releaseFocusTrap(modal);
        modal.classList.add('hidden');
    }
    pendingDeleteBtn = null;
    // Restore focus to trigger element
    if (deleteTriggerEl) { deleteTriggerEl.focus(); deleteTriggerEl = null; }
}

function confirmDeletePersonalExpense() {
    if (!pendingDeleteBtn) return;
    var btn = pendingDeleteBtn;
    var expenseId = btn.dataset.expenseId;
    var tripId = btn.dataset.tripId;
    var csrfToken = WeGo.getCsrfToken();

    var confirmBtn = document.getElementById('personal-delete-confirm-btn');
    var spinner = document.getElementById('personal-delete-spinner');
    if (confirmBtn) confirmBtn.disabled = true;
    if (spinner) spinner.classList.remove('hidden');

    WeGo.fetchWithTimeout('/api/trips/' + tripId + '/personal-expenses/' + expenseId, {
        method: 'DELETE',
        headers: { 'X-XSRF-TOKEN': csrfToken }
    }).then(function(res) {
        if (res.ok) {
            closePersonalDeleteModal();
            // C4: Fade out animation + toast, then reload for fresh summary/charts
            var card = btn.closest('.expense-item-card');
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
            if (typeof Toast !== 'undefined') {
                Toast.success('已刪除費用');
            }
        } else {
            closePersonalDeleteModal();
            if (typeof Toast !== 'undefined') {
                Toast.error('刪除失敗，請重試');
            }
        }
    }).catch(function() {
        closePersonalDeleteModal();
        if (typeof Toast !== 'undefined') {
            Toast.error('刪除失敗，請重試');
        }
    }).finally(function() {
        if (confirmBtn) confirmBtn.disabled = false;
        if (spinner) spinner.classList.add('hidden');
    });
}

// ESC key handler for modals
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        var deleteModal = document.getElementById('personal-delete-modal');
        if (deleteModal && !deleteModal.classList.contains('hidden')) {
            closePersonalDeleteModal();
            return;
        }
        var budgetModal = document.getElementById('budget-modal');
        if (budgetModal && !budgetModal.classList.contains('hidden')) {
            closeBudgetModal();
        }
    }
});

// === Budget modal (C2: accessibility, H6: loading, M4: prefill) ===
var budgetTriggerEl = null;

function showBudgetModal() {
    budgetTriggerEl = document.activeElement;
    var modal = document.getElementById('budget-modal');
    var input = document.getElementById('budget-input');
    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
        trapFocus(modal);
    }
    // M4: Prefill existing budget value
    var budgetDisplay = document.getElementById('budget-display');
    if (budgetDisplay && budgetDisplay.dataset.budget && input) {
        input.value = budgetDisplay.dataset.budget;
    }
    // Focus input for accessibility
    if (input) {
        setTimeout(function() { input.focus(); }, 100);
    }
}

function closeBudgetModal() {
    var modal = document.getElementById('budget-modal');
    if (modal) {
        releaseFocusTrap(modal);
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    }
    // Restore focus to trigger element
    if (budgetTriggerEl) { budgetTriggerEl.focus(); budgetTriggerEl = null; }
}

function submitBudget() {
    var budgetInput = document.getElementById('budget-input');
    var budget = budgetInput ? budgetInput.value : null;
    if (!budget || parseFloat(budget) <= 0) return;

    // Get tripId from URL
    var pathParts = window.location.pathname.split('/');
    var tripsIndex = pathParts.indexOf('trips');
    var tripId = tripsIndex !== -1 ? pathParts[tripsIndex + 1] : null;
    if (!tripId) return;

    // H6: Loading state
    var submitBtn = document.getElementById('budget-submit-btn');
    var spinner = document.getElementById('budget-spinner');
    var submitText = document.getElementById('budget-submit-text');
    if (submitBtn) submitBtn.disabled = true;
    if (spinner) spinner.classList.remove('hidden');
    if (submitText) submitText.textContent = '設定中...';

    var csrfToken = WeGo.getCsrfToken();
    WeGo.fetchWithTimeout('/api/trips/' + tripId + '/personal-expenses/budget', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': csrfToken },
        body: JSON.stringify({ budget: parseFloat(budget) })
    }).then(function(res) {
        if (res.ok) window.location.reload();
        else {
            if (typeof Toast !== 'undefined') {
                Toast.error('設定失敗，請重試');
            }
        }
    }).catch(function() {
        if (typeof Toast !== 'undefined') {
            Toast.error('設定失敗，請重試');
        }
    }).finally(function() {
        if (submitBtn) submitBtn.disabled = false;
        if (spinner) spinner.classList.add('hidden');
        if (submitText) submitText.textContent = '確定';
    });
}

// Chart initialization
var categoryChart = null;
var dailyChart = null;

var CATEGORY_COLORS = {
    FOOD: '#F97316',
    TRANSPORT: '#3B82F6',
    ACCOMMODATION: '#8B5CF6',
    SHOPPING: '#EC4899',
    ENTERTAINMENT: '#F43F5E',
    HEALTH: '#10B981',
    OTHER: '#6B7280'
};

var CATEGORY_LABELS = {
    FOOD: '餐飲',
    TRANSPORT: '交通',
    ACCOMMODATION: '住宿',
    SHOPPING: '購物',
    ENTERTAINMENT: '娛樂',
    HEALTH: '健康',
    OTHER: '其他'
};

// M6: Chart tab switching with opacity transition
var chartTransitionTimer = null;

function showChart(type) {
    if (chartTransitionTimer) clearTimeout(chartTransitionTimer);

    var catDiv = document.getElementById('chart-category');
    var dailyDiv = document.getElementById('chart-daily');
    var catTab = document.getElementById('chart-tab-category');
    var dailyTab = document.getElementById('chart-tab-daily');

    if (type === 'category') {
        if (dailyDiv) { dailyDiv.style.opacity = '0'; chartTransitionTimer = setTimeout(function() { dailyDiv.classList.add('hidden'); }, 150); }
        if (catDiv) { catDiv.classList.remove('hidden'); setTimeout(function() { catDiv.style.opacity = '1'; }, 10); }
    } else {
        if (catDiv) { catDiv.style.opacity = '0'; chartTransitionTimer = setTimeout(function() { catDiv.classList.add('hidden'); }, 150); }
        if (dailyDiv) { dailyDiv.classList.remove('hidden'); setTimeout(function() { dailyDiv.style.opacity = '1'; }, 10); }
    }

    if (catTab) {
        catTab.classList.toggle('bg-white', type === 'category');
        catTab.classList.toggle('dark:bg-gray-700', type === 'category');
        catTab.classList.toggle('shadow-sm', type === 'category');
        catTab.classList.toggle('text-gray-800', type === 'category');
        catTab.classList.toggle('dark:text-gray-100', type === 'category');
        catTab.classList.toggle('text-gray-500', type !== 'category');
        catTab.classList.toggle('dark:text-gray-400', type !== 'category');
    }
    if (dailyTab) {
        dailyTab.classList.toggle('bg-white', type === 'daily');
        dailyTab.classList.toggle('dark:bg-gray-700', type === 'daily');
        dailyTab.classList.toggle('shadow-sm', type === 'daily');
        dailyTab.classList.toggle('text-gray-800', type === 'daily');
        dailyTab.classList.toggle('dark:text-gray-100', type === 'daily');
        dailyTab.classList.toggle('text-gray-500', type !== 'daily');
        dailyTab.classList.toggle('dark:text-gray-400', type !== 'daily');
    }

    if (type === 'daily' && dailyChart) {
        var canvas = document.getElementById('personal-daily-chart');
        if (canvas) {
            var minWidth = canvas.dataset.minWidth || 200;
            canvas.style.minWidth = minWidth + 'px';
        }
    }
}

function initPersonalCharts() {
    var dataEl = document.getElementById('personal-chart-data');
    if (!dataEl) return;
    if (typeof Chart === 'undefined') return;

    try {
        var categoryData = JSON.parse(dataEl.dataset.categoryBreakdown || '{}');
        var dailyData = JSON.parse(dataEl.dataset.dailyAmounts || '{}');

        // Category doughnut
        var catCanvas = document.getElementById('personal-category-chart');
        if (catCanvas && Object.keys(categoryData).length > 0) {
            var labels = Object.keys(categoryData).map(function(k) { return CATEGORY_LABELS[k] || k; });
            var data = Object.values(categoryData).map(function(v) { return parseFloat(v); });
            var colors = Object.keys(categoryData).map(function(k) { return CATEGORY_COLORS[k] || '#6B7280'; });
            categoryChart = new Chart(catCanvas, {
                type: 'doughnut',
                data: {
                    labels: labels,
                    datasets: [{
                        data: data,
                        backgroundColor: colors,
                        borderWidth: 2
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    cutout: '60%',
                    plugins: { legend: { display: false } }
                }
            });
            var total = data.reduce(function(sum, v) { return sum + v; }, 0);
            renderCategoryLegend(categoryData, total);
        }

        // Daily bar chart
        var dailyCanvas = document.getElementById('personal-daily-chart');
        if (dailyCanvas && Object.keys(dailyData).length > 0) {
            var dLabels = Object.keys(dailyData);
            var dData = Object.values(dailyData).map(function(v) { return parseFloat(v); });
            dailyChart = new Chart(dailyCanvas, {
                type: 'bar',
                data: {
                    labels: dLabels,
                    datasets: [{
                        label: '花費',
                        data: dData,
                        backgroundColor: '#F97316',
                        borderRadius: 4
                    }]
                },
                options: {
                    responsive: false,
                    maintainAspectRatio: false,
                    plugins: { legend: { display: false } },
                    scales: { y: { beginAtZero: true } }
                }
            });
        }
    } catch (e) {
        // Chart init failed silently
    }
}

function renderCategoryLegend(data, total) {
    var legendEl = document.getElementById('personal-category-legend');
    if (!legendEl) return;
    var sorted = Object.entries(data).sort(function(a, b) { return b[1] - a[1]; });
    legendEl.replaceChildren();
    sorted.forEach(function(entry) {
        var cat = entry[0], amount = entry[1];
        var pct = total > 0 ? Math.round(amount / total * 100) : 0;
        var color = CATEGORY_COLORS[cat] || '#6B7280';
        var label = CATEGORY_LABELS[cat] || cat;

        var div = document.createElement('div');
        div.className = 'flex items-center gap-2 px-2 py-1.5 rounded-lg bg-gray-50 dark:bg-gray-800/50';

        var dot = document.createElement('div');
        dot.className = 'w-3 h-3 rounded-full flex-shrink-0';
        dot.style.background = color;

        var labelSpan = document.createElement('span');
        labelSpan.className = 'text-xs text-gray-700 dark:text-gray-300 truncate';
        labelSpan.textContent = label;

        var pctSpan = document.createElement('span');
        pctSpan.className = 'text-xs text-gray-500 dark:text-gray-400 tabular-nums ml-auto';
        pctSpan.textContent = pct + '%';

        div.appendChild(dot);
        div.appendChild(labelSpan);
        div.appendChild(pctSpan);
        legendEl.appendChild(div);
    });
}

// H4: Enhanced date headers with calendar icon
function injectDateHeaders() {
    var items = document.querySelectorAll('.expense-item-card[data-date]');
    if (!items.length) return;
    var svgNS = 'http://www.w3.org/2000/svg';
    var lastDate = null;
    items.forEach(function(item) {
        var date = item.dataset.date;
        if (date && date !== lastDate) {
            var header = document.createElement('div');
            header.className = 'flex items-center gap-2 pt-2 pb-1 px-1';

            var icon = document.createElementNS(svgNS, 'svg');
            icon.setAttribute('class', 'w-3.5 h-3.5 text-gray-400 dark:text-gray-500');
            icon.setAttribute('fill', 'none');
            icon.setAttribute('stroke', 'currentColor');
            icon.setAttribute('viewBox', '0 0 24 24');
            var path = document.createElementNS(svgNS, 'path');
            path.setAttribute('stroke-linecap', 'round');
            path.setAttribute('stroke-linejoin', 'round');
            path.setAttribute('stroke-width', '2');
            path.setAttribute('d', 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z');
            icon.appendChild(path);

            var text = document.createElement('span');
            text.className = 'text-xs font-medium text-gray-400 dark:text-gray-500';
            text.textContent = date;

            header.appendChild(icon);
            header.appendChild(text);
            item.parentNode.insertBefore(header, item);
            lastDate = date;
        }
    });
}

// Consolidated DOMContentLoaded handler
document.addEventListener('DOMContentLoaded', function() {
    initPersonalCharts();
    injectDateHeaders();

    // C1: Set initial header CTA href based on URL param
    var url = new URL(window.location);
    if (url.searchParams.get('tab') === 'personal') {
        var headerAddBtn = document.getElementById('header-add-btn');
        if (headerAddBtn && headerAddBtn.dataset.personalHref) {
            headerAddBtn.href = headerAddBtn.dataset.personalHref;
        }
    }
});
