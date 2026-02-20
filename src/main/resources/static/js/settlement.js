/**
 * WeGo - Settlement JavaScript Module
 *
 * Handles settlement confirmation AJAX operations.
 * Integrates with global Toast utility from app.js
 * and WeGo shared utilities from common.js.
 */
const Settlement = (() => {
    'use strict';

    function settleItem(btn) {
        const tripId = btn.dataset.trip;
        const fromUserId = btn.dataset.from;
        const toUserId = btn.dataset.to;
        const nameFrom = btn.dataset.nameFrom;
        const nameTo = btn.dataset.nameTo;

        btn.disabled = true;
        const originalHTML = btn.innerHTML;
        btn.innerHTML = '<svg class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/></svg><span>處理中...</span>';
        btn.classList.add('opacity-60');

        const headers = {
            'Content-Type': 'application/json',
            [WeGo.getCsrfHeader()]: WeGo.getCsrfToken()
        };

        WeGo.fetchWithTimeout('/api/trips/' + tripId + '/settlement/settle', {
            method: 'PUT',
            headers: headers,
            body: JSON.stringify({ fromUserId: fromUserId, toUserId: toUserId })
        })
        .then(function(response) {
            if (response.ok) {
                Toast.success(nameFrom + ' → ' + nameTo + ' 已結清');
                setTimeout(function() { window.location.reload(); }, 800);
            } else {
                throw new Error('API error: ' + response.status);
            }
        })
        .catch(function() {
            Toast.error('結清失敗，請稍後再試');
            btn.disabled = false;
            btn.innerHTML = originalHTML;
            btn.classList.remove('opacity-60');
        });
    }

    function init() {
        document.addEventListener('click', function(e) {
            const btn = e.target.closest('[data-action="settle"]');
            if (btn) {
                settleItem(btn);
            }
        });
    }

    return { init };
})();

document.addEventListener('DOMContentLoaded', Settlement.init);
