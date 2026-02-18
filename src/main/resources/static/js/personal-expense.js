// Tab switching
function switchTab(tab) {
    const isPersonal = tab === 'personal';
    const teamContent = document.getElementById('content-team');
    const personalContent = document.getElementById('content-personal');
    const tabTeam = document.getElementById('tab-team');
    const tabPersonal = document.getElementById('tab-personal');

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

    // Update URL without reload
    const url = new URL(window.location);
    if (isPersonal) {
        url.searchParams.set('tab', 'personal');
    } else {
        url.searchParams.delete('tab');
    }
    window.history.replaceState({}, '', url);
}

function toggleAutoItem(tripExpenseId) {
    const detail = document.getElementById('auto-detail-' + tripExpenseId);
    if (detail) detail.classList.toggle('hidden');
}

function deleteManualExpense(btn) {
    if (!confirm('確定要刪除這筆費用嗎？')) return;
    const expenseId = btn.dataset.expenseId;
    const tripId = btn.dataset.tripId;
    const csrfToken = WeGo.getCsrfToken();
    WeGo.fetchWithTimeout('/api/trips/' + tripId + '/personal-expenses/' + expenseId, {
        method: 'DELETE',
        headers: { 'X-XSRF-TOKEN': csrfToken }
    }).then(function(res) {
        if (res.ok) {
            const card = btn.closest('.glass-card');
            if (card) card.remove();
        }
    }).catch(function(err) {
        console.error('Delete failed:', err);
    });
}

// Budget modal
function showBudgetModal() {
    const modal = document.getElementById('budget-modal');
    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }
}

function closeBudgetModal() {
    const modal = document.getElementById('budget-modal');
    if (modal) {
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    }
}

function submitBudget() {
    const budgetInput = document.getElementById('budget-input');
    const budget = budgetInput ? budgetInput.value : null;
    if (!budget || parseFloat(budget) <= 0) return;

    // Get tripId from URL
    const pathParts = window.location.pathname.split('/');
    const tripsIndex = pathParts.indexOf('trips');
    const tripId = tripsIndex !== -1 ? pathParts[tripsIndex + 1] : null;
    if (!tripId) return;

    const csrfToken = WeGo.getCsrfToken();
    WeGo.fetchWithTimeout('/api/trips/' + tripId + '/personal-expenses/budget', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': csrfToken },
        body: JSON.stringify({ budget: parseFloat(budget) })
    }).then(function(res) {
        if (res.ok) window.location.reload();
    }).catch(function(err) {
        console.error('Budget update failed:', err);
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

function showChart(type) {
    const catDiv = document.getElementById('chart-category');
    const dailyDiv = document.getElementById('chart-daily');
    const catTab = document.getElementById('chart-tab-category');
    const dailyTab = document.getElementById('chart-tab-daily');

    if (catDiv) catDiv.classList.toggle('hidden', type !== 'category');
    if (dailyDiv) dailyDiv.classList.toggle('hidden', type !== 'daily');

    if (catTab) {
        catTab.classList.toggle('bg-white', type === 'category');
        catTab.classList.toggle('dark:bg-gray-700', type === 'category');
        catTab.classList.toggle('shadow-sm', type === 'category');
        catTab.classList.toggle('text-gray-500', type !== 'category');
    }
    if (dailyTab) {
        dailyTab.classList.toggle('bg-white', type === 'daily');
        dailyTab.classList.toggle('dark:bg-gray-700', type === 'daily');
        dailyTab.classList.toggle('shadow-sm', type === 'daily');
        dailyTab.classList.toggle('text-gray-500', type !== 'daily');
    }

    if (type === 'daily' && dailyChart) {
        const canvas = document.getElementById('personal-daily-chart');
        if (canvas) {
            const minWidth = canvas.dataset.minWidth || 200;
            canvas.style.minWidth = minWidth + 'px';
        }
    }
}

function initPersonalCharts() {
    const dataEl = document.getElementById('personal-chart-data');
    if (!dataEl) return;

    try {
        const categoryData = JSON.parse(dataEl.dataset.categoryBreakdown || '{}');
        const dailyData = JSON.parse(dataEl.dataset.dailyAmounts || '{}');

        // Category doughnut
        const catCanvas = document.getElementById('personal-category-chart');
        if (catCanvas && Object.keys(categoryData).length > 0) {
            const labels = Object.keys(categoryData).map(function(k) { return CATEGORY_LABELS[k] || k; });
            const data = Object.values(categoryData).map(function(v) { return parseFloat(v); });
            const colors = Object.keys(categoryData).map(function(k) { return CATEGORY_COLORS[k] || '#6B7280'; });
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
                    maintainAspectRatio: true,
                    plugins: { legend: { position: 'bottom' } }
                }
            });
        }

        // Daily bar chart
        const dailyCanvas = document.getElementById('personal-daily-chart');
        if (dailyCanvas && Object.keys(dailyData).length > 0) {
            const labels = Object.keys(dailyData);
            const data = Object.values(dailyData).map(function(v) { return parseFloat(v); });
            dailyChart = new Chart(dailyCanvas, {
                type: 'bar',
                data: {
                    labels: labels,
                    datasets: [{
                        label: '花費',
                        data: data,
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
        console.error('Chart init failed:', e);
    }
}

document.addEventListener('DOMContentLoaded', initPersonalCharts);
