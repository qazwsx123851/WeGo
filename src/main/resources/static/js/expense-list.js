/**
 * WeGo - Expense List JavaScript Module
 *
 * Handles expense list interactions: date group accordion,
 * expense detail modal, edit/delete operations.
 * Integrates with global Toast utility from app.js
 * and WeGo shared utilities from common.js.
 */
const ExpenseList = (() => {
    'use strict';

    // =====================
    // State
    // =====================
    let currentExpenseId = null;
    let currentExpenseData = null;

    // =====================
    // DOM Helpers
    // =====================
    function getTripId() {
        return document.getElementById('trip-id').value;
    }

    // =====================
    // Date Group Accordion
    // =====================
    function toggleDateGroup(header) {
        const content = header.nextElementSibling;
        if (!content || !content.classList.contains('expense-group-content')) return;

        const chevron = header.querySelector('.chevron-icon');
        const isCollapsed = content.classList.contains('max-h-0');
        const prefersReducedMotion = WeGo._reducedMotion;

        if (isCollapsed) {
            // Expand
            content.classList.remove('max-h-0');
            content.style.maxHeight = content.scrollHeight + 'px';
            if (chevron) chevron.classList.add('rotate-180');

            const cleanup = () => {
                content.style.maxHeight = '';
                content.removeEventListener('transitionend', cleanup);
            };
            if (prefersReducedMotion) {
                content.style.maxHeight = '';
            } else {
                content.addEventListener('transitionend', cleanup);
            }
        } else {
            // Collapse: set explicit height first, then animate to 0
            content.style.maxHeight = content.scrollHeight + 'px';
            if (chevron) chevron.classList.remove('rotate-180');

            if (prefersReducedMotion) {
                content.classList.add('max-h-0');
                content.style.maxHeight = '0px';
            } else {
                requestAnimationFrame(() => {
                    content.classList.add('max-h-0');
                    content.style.maxHeight = '0px';
                });
            }
        }
    }

    // =====================
    // Split Detail Toggle
    // =====================
    function toggleSplitDetail(expenseId) {
        const detail = document.getElementById('split-detail-' + expenseId);
        const chevron = document.getElementById('split-chevron-' + expenseId);
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
    // Expense Detail Modal
    // =====================
    function openExpenseModal(cardElement) {
        currentExpenseId = cardElement.dataset.expenseId;
        currentExpenseData = {
            id: cardElement.dataset.expenseId,
            description: cardElement.dataset.expenseDescription,
            amount: parseFloat(cardElement.dataset.expenseAmount),
            currency: cardElement.dataset.expenseCurrency || 'TWD',
            category: cardElement.dataset.expenseCategory,
            payerName: cardElement.dataset.expensePayerName,
            splitType: cardElement.dataset.expenseSplitType,
            note: cardElement.dataset.expenseNote
        };

        const modal = document.getElementById('expense-detail-modal');
        const panel = document.getElementById('expense-modal-panel');
        modal.classList.remove('hidden');
        document.body.style.overflow = 'hidden';

        // Hide pill bar to prevent overlap with modal footer
        const pillBar = document.querySelector('#global-pill-bar, #dynamic-pill-bar');
        if (pillBar) pillBar.classList.add('hidden');

        // Animate in
        requestAnimationFrame(() => {
            panel.classList.remove('translate-y-full');
            panel.classList.add('translate-y-0');
            panel.classList.remove('md:scale-95', 'md:opacity-0');
            panel.classList.add('md:scale-100', 'md:opacity-100');
        });

        // Show loading, hide content
        document.getElementById('expense-modal-loading').classList.remove('hidden');
        document.getElementById('expense-modal-content').classList.add('hidden');

        fetchExpenseDetail(currentExpenseId);
    }

    function fetchExpenseDetail(expenseId) {
        const tripId = getTripId();
        WeGo.fetchWithTimeout('/api/trips/' + tripId + '/expenses/' + expenseId)
            .then(function(response) {
                if (response.ok) {
                    return response.json().then(function(result) {
                        if (result.success && result.data) {
                            currentExpenseData = result.data;
                            renderExpenseDetail(result.data);
                        } else {
                            Toast.error('找不到支出記錄');
                            closeExpenseModal();
                        }
                    });
                } else {
                    // Use cached data from card
                    renderExpenseDetail(currentExpenseData);
                }
            })
            .catch(function() {
                // Use cached data from card
                renderExpenseDetail(currentExpenseData);
            });
    }

    function renderExpenseDetail(expense) {
        const content = document.getElementById('expense-modal-content');
        content.innerHTML = '';

        // Amount Section
        const amountSection = document.createElement('div');
        amountSection.className = 'text-center mb-6';

        const amountEl = document.createElement('p');
        amountEl.className = 'text-4xl font-bold text-gray-800 dark:text-gray-100 font-mono';
        amountEl.textContent = '$' + formatNumber(expense.amount);
        amountSection.appendChild(amountEl);

        const currencyEl = document.createElement('p');
        currencyEl.className = 'text-sm text-gray-500 dark:text-gray-400 mt-1';
        currencyEl.textContent = expense.currency || 'TWD';
        amountSection.appendChild(currencyEl);

        content.appendChild(amountSection);

        // Description
        const descSection = document.createElement('div');
        descSection.className = 'bg-gray-50 dark:bg-gray-800 rounded-xl p-4 mb-4';

        const descLabel = document.createElement('p');
        descLabel.className = 'text-xs text-gray-500 dark:text-gray-400 mb-1';
        descLabel.textContent = '描述';
        descSection.appendChild(descLabel);

        const descValue = document.createElement('p');
        descValue.className = 'text-gray-800 dark:text-gray-100 font-medium';
        descValue.textContent = expense.description || '-';
        descSection.appendChild(descValue);

        content.appendChild(descSection);

        // Info Grid
        const infoGrid = document.createElement('div');
        infoGrid.className = 'grid grid-cols-2 gap-4 mb-4';

        infoGrid.appendChild(createInfoItem('付款人', expense.paidByName || expense.payerName || '-'));
        infoGrid.appendChild(createInfoItem('類別', getCategoryLabel(expense.category)));
        infoGrid.appendChild(createInfoItem('分攤方式', getSplitTypeLabel(expense.splitType)));
        infoGrid.appendChild(createInfoItem('日期', expense.expenseDate || formatDate(expense.createdAt)));

        content.appendChild(infoGrid);

        // Note
        if (expense.note) {
            const noteSection = document.createElement('div');
            noteSection.className = 'bg-gray-50 dark:bg-gray-800 rounded-xl p-4 mb-4';

            const noteLabel = document.createElement('p');
            noteLabel.className = 'text-xs text-gray-500 dark:text-gray-400 mb-1';
            noteLabel.textContent = '備註';
            noteSection.appendChild(noteLabel);

            const noteValue = document.createElement('p');
            noteValue.className = 'text-gray-800 dark:text-gray-100';
            noteValue.textContent = expense.note;
            noteSection.appendChild(noteValue);

            content.appendChild(noteSection);
        }

        // Splits Section
        if (expense.splits && expense.splits.length > 0) {
            const splitsSection = document.createElement('div');
            splitsSection.className = 'mt-4';

            const splitsLabel = document.createElement('p');
            splitsLabel.className = 'text-sm font-medium text-gray-700 dark:text-gray-300 mb-3';
            splitsLabel.textContent = '分攤明細';
            splitsSection.appendChild(splitsLabel);

            const splitsList = document.createElement('div');
            splitsList.className = 'space-y-2';

            expense.splits.forEach(function(split) {
                const splitItem = document.createElement('div');
                splitItem.className = 'flex items-center justify-between bg-gray-50 dark:bg-gray-800 rounded-lg px-3 py-2';

                const userInfo = document.createElement('div');
                userInfo.className = 'flex items-center gap-2';

                const avatar = document.createElement('div');
                avatar.className = 'w-7 h-7 rounded-full bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center';
                const avatarText = document.createElement('span');
                avatarText.className = 'text-xs font-medium text-primary-600 dark:text-primary-400';
                avatarText.textContent = (split.userNickname || '?').charAt(0);
                avatar.appendChild(avatarText);
                userInfo.appendChild(avatar);

                const userName = document.createElement('span');
                userName.className = 'text-sm text-gray-700 dark:text-gray-300';
                userName.textContent = split.userNickname || '未知使用者';
                userInfo.appendChild(userName);

                splitItem.appendChild(userInfo);

                const amountInfo = document.createElement('div');
                amountInfo.className = 'flex items-center gap-2';

                const splitAmount = document.createElement('span');
                splitAmount.className = 'text-sm font-medium text-gray-800 dark:text-gray-100 font-mono';
                splitAmount.textContent = '$' + formatNumber(split.amount);
                amountInfo.appendChild(splitAmount);

                if (split.isSettled) {
                    const settledBadge = document.createElement('span');
                    settledBadge.className = 'text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400';
                    settledBadge.textContent = '已結清';
                    amountInfo.appendChild(settledBadge);
                }

                splitItem.appendChild(amountInfo);
                splitsList.appendChild(splitItem);
            });

            splitsSection.appendChild(splitsList);
            content.appendChild(splitsSection);
        }

        // Hide loading, show content
        document.getElementById('expense-modal-loading').classList.add('hidden');
        content.classList.remove('hidden');
    }

    function createInfoItem(label, value) {
        const div = document.createElement('div');
        div.className = 'bg-gray-50 dark:bg-gray-800 rounded-xl p-3';

        const labelEl = document.createElement('p');
        labelEl.className = 'text-xs text-gray-500 dark:text-gray-400 mb-1';
        labelEl.textContent = label;
        div.appendChild(labelEl);

        const valueEl = document.createElement('p');
        valueEl.className = 'text-sm text-gray-800 dark:text-gray-100 font-medium';
        valueEl.textContent = value;
        div.appendChild(valueEl);

        return div;
    }

    function closeExpenseModal() {
        const modal = document.getElementById('expense-detail-modal');
        const panel = document.getElementById('expense-modal-panel');

        // Animate out
        panel.classList.add('translate-y-full');
        panel.classList.remove('translate-y-0');
        panel.classList.add('md:scale-95', 'md:opacity-0');
        panel.classList.remove('md:scale-100', 'md:opacity-100');

        setTimeout(function() {
            modal.classList.add('hidden');
            document.body.style.overflow = '';
            currentExpenseId = null;
            currentExpenseData = null;

            // Restore pill bar
            const pillBar = document.querySelector('#global-pill-bar, #dynamic-pill-bar');
            if (pillBar) pillBar.classList.remove('hidden');
        }, 200);
    }

    // =====================
    // Edit Expense
    // =====================
    function editExpense() {
        if (currentExpenseId) {
            window.location.href = '/trips/' + getTripId() + '/expenses/' + currentExpenseId + '/edit';
        }
    }

    // =====================
    // Delete Expense
    // =====================
    function confirmDeleteExpense() {
        document.getElementById('delete-expense-modal').classList.remove('hidden');
    }

    function closeDeleteModal() {
        document.getElementById('delete-expense-modal').classList.add('hidden');
    }

    function deleteExpense() {
        if (!currentExpenseId) return;

        const deleteBtn = document.getElementById('confirm-delete-btn');
        deleteBtn.disabled = true;
        const originalText = deleteBtn.textContent;
        deleteBtn.textContent = '刪除中...';

        const headers = {
            [WeGo.getCsrfHeader()]: WeGo.getCsrfToken()
        };

        WeGo.fetchWithTimeout('/api/expenses/' + currentExpenseId, {
            method: 'DELETE',
            headers: headers
        })
        .then(function(response) {
            if (response.ok) {
                Toast.success('支出已刪除');
                closeDeleteModal();
                closeExpenseModal();

                // Remove the card from DOM
                const card = document.querySelector('[data-expense-id="' + currentExpenseId + '"]');
                if (card) {
                    const parentGroup = card.closest('.mb-4');
                    card.remove();

                    // Check if the date group is now empty
                    const remainingCards = parentGroup ? parentGroup.querySelectorAll('.expense-card') : [];
                    if (remainingCards.length === 0 && parentGroup) {
                        parentGroup.remove();
                    }
                }

                // Check if all expenses are deleted
                const allCards = document.querySelectorAll('.expense-card');
                if (allCards.length === 0) {
                    setTimeout(function() { window.location.reload(); }, 500);
                }
            } else {
                return response.json().then(function(error) {
                    Toast.error(error.message || '刪除失敗');
                }).catch(function() {
                    Toast.error('刪除失敗');
                });
            }
        })
        .catch(function() {
            Toast.error('網路錯誤，請稍後再試');
        })
        .finally(function() {
            deleteBtn.disabled = false;
            deleteBtn.textContent = originalText;
        });
    }

    // =====================
    // Utility Functions
    // =====================
    function formatNumber(num) {
        if (num === null || num === undefined) return '0';
        return parseFloat(num).toLocaleString('zh-TW', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
    }

    function formatDate(isoString) {
        if (!isoString) return '-';
        try {
            const date = new Date(isoString);
            return date.toLocaleDateString('zh-TW', { month: '2-digit', day: '2-digit' });
        } catch (e) {
            return '-';
        }
    }

    function getCategoryLabel(category) {
        return (WeGo.CATEGORY_LABELS || {})[category] || '其他';
    }

    function getSplitTypeLabel(splitType) {
        const labels = {
            'EQUAL': '均分',
            'PERCENTAGE': '百分比',
            'CUSTOM': '自訂金額',
            'SHARES': '比例'
        };
        return labels[splitType] || '均分';
    }

    // =====================
    // Event Delegation
    // =====================
    function handleAction(action, target) {
        switch (action) {
            case 'toggle-date-group':
                toggleDateGroup(target.closest('[data-action="toggle-date-group"]'));
                break;
            case 'toggle-split-detail':
                toggleSplitDetail(target.closest('[data-action="toggle-split-detail"]').dataset.expenseId);
                break;
            case 'close-expense-modal':
                closeExpenseModal();
                break;
            case 'edit-expense':
                editExpense();
                break;
            case 'confirm-delete-expense':
                confirmDeleteExpense();
                break;
            case 'close-delete-modal':
                closeDeleteModal();
                break;
            case 'delete-expense':
                deleteExpense();
                break;
            case 'switch-tab':
                if (typeof PersonalExpense !== 'undefined' && PersonalExpense.switchTab) {
                    PersonalExpense.switchTab(target.closest('[data-tab]').dataset.tab);
                }
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
            if (actionEl) {
                const action = actionEl.dataset.action;
                handleAction(action, e.target);
                return;
            }

            // Expense card click (open modal)
            const card = e.target.closest('.expense-card');
            if (card) {
                openExpenseModal(card);
                return;
            }
        });

        // Keyboard navigation
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') {
                const deleteModal = document.getElementById('delete-expense-modal');
                const detailModal = document.getElementById('expense-detail-modal');

                if (!deleteModal.classList.contains('hidden')) {
                    closeDeleteModal();
                } else if (!detailModal.classList.contains('hidden')) {
                    closeExpenseModal();
                }
            }
        });

        // Initialize expanded groups on load
        document.querySelectorAll('.expense-group-content:not(.max-h-0)').forEach(function(content) {
            content.style.maxHeight = '';
        });
    }

    return { init };
})();

document.addEventListener('DOMContentLoaded', ExpenseList.init);
