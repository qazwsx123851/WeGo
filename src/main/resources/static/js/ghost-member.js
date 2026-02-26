/**
 * WeGo - Ghost Member Management JavaScript Module
 *
 * Handles ghost (virtual) member AJAX operations:
 * - Create ghost member
 * - Remove ghost member
 * - Merge ghost member into real member
 *
 * @module GhostMember
 */
const GhostMember = (() => {
    'use strict';

    let selectedGhostId = null;
    let selectedGhostName = null;

    function getTripId() {
        const el = document.getElementById('member-management-container');
        return el ? el.dataset.tripId : '';
    }

    function buildHeaders(withJson) {
        const headers = {
            [WeGo.getCsrfHeader()]: WeGo.getCsrfToken()
        };
        if (withJson) {
            headers['Content-Type'] = 'application/json';
        }
        return headers;
    }

    function disableButton(btn) {
        const originalHTML = btn.innerHTML;
        btn.disabled = true;
        btn.classList.add('opacity-60');
        btn.innerHTML = '<svg class="w-4 h-4 animate-spin inline-block" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/></svg><span class="ml-1">處理中...</span>';
        return originalHTML;
    }

    function enableButton(btn, originalHTML) {
        btn.disabled = false;
        btn.classList.remove('opacity-60');
        btn.innerHTML = originalHTML;
    }

    /**
     * Create a new ghost member.
     */
    function createGhost(btn) {
        var nameInput = document.getElementById('ghostName');
        var noteInput = document.getElementById('ghostNote');
        var name = nameInput ? nameInput.value.trim() : '';
        var note = noteInput ? noteInput.value.trim() : '';

        if (!name) {
            Toast.error('請輸入虛擬成員名稱');
            if (nameInput) nameInput.focus();
            return;
        }

        var tripId = getTripId();
        var originalHTML = disableButton(btn);

        WeGo.fetchWithTimeout('/api/trips/' + tripId + '/ghost-members', {
            method: 'POST',
            headers: buildHeaders(true),
            body: JSON.stringify({ displayName: name, note: note || null })
        })
        .then(function(response) {
            if (response.ok) {
                Toast.success('已新增虛擬成員「' + WeGo.escapeHtml(name) + '」');
                setTimeout(function() { window.location.reload(); }, 1000);
            } else {
                return response.json().then(function(error) {
                    Toast.error(error.message || '新增失敗');
                    enableButton(btn, originalHTML);
                });
            }
        })
        .catch(function() {
            Toast.error('網路錯誤，請稍後再試');
            enableButton(btn, originalHTML);
        });
    }

    /**
     * Show ghost removal confirmation dialog.
     */
    function confirmRemoveGhost(btn) {
        selectedGhostId = btn.dataset.ghostId;
        selectedGhostName = btn.dataset.ghostName;

        var nameEl = document.getElementById('ghostRemoveName');
        if (nameEl) {
            nameEl.textContent = selectedGhostName;
        }
        document.getElementById('removeGhostDialog').classList.remove('hidden');
    }

    function closeGhostRemoveDialog() {
        document.getElementById('removeGhostDialog').classList.add('hidden');
        selectedGhostId = null;
        selectedGhostName = null;
    }

    /**
     * Execute ghost removal.
     */
    function removeGhost(btn) {
        if (!selectedGhostId) return;

        var tripId = getTripId();
        var originalHTML = disableButton(btn);

        WeGo.fetchWithTimeout('/api/trips/' + tripId + '/ghost-members/' + selectedGhostId, {
            method: 'DELETE',
            headers: buildHeaders(false)
        })
        .then(function(response) {
            if (response.ok) {
                Toast.success('已移除虛擬成員');
                closeGhostRemoveDialog();
                setTimeout(function() { window.location.reload(); }, 1000);
            } else {
                return response.json().then(function(error) {
                    Toast.error(error.message || '移除失敗');
                    enableButton(btn, originalHTML);
                });
            }
        })
        .catch(function() {
            Toast.error('網路錯誤，請稍後再試');
            enableButton(btn, originalHTML);
        });
    }

    /**
     * Open merge modal for a ghost member.
     */
    function openMergeModal(btn) {
        selectedGhostId = btn.dataset.ghostId;
        selectedGhostName = btn.dataset.ghostName;

        var nameEl = document.getElementById('mergeGhostName');
        if (nameEl) {
            nameEl.textContent = selectedGhostName;
        }

        var select = document.getElementById('mergeTargetUser');
        if (select) select.value = '';

        document.getElementById('mergeGhostModal').classList.remove('hidden');
    }

    function closeMergeModal() {
        document.getElementById('mergeGhostModal').classList.add('hidden');
        selectedGhostId = null;
        selectedGhostName = null;
    }

    /**
     * Execute merge operation.
     */
    function confirmMerge(btn) {
        if (!selectedGhostId) return;

        var select = document.getElementById('mergeTargetUser');
        var targetUserId = select ? select.value : '';

        if (!targetUserId) {
            Toast.error('請選擇要合併的真實成員');
            return;
        }

        var tripId = getTripId();
        var originalHTML = disableButton(btn);

        WeGo.fetchWithTimeout('/api/trips/' + tripId + '/ghost-members/' + selectedGhostId + '/merge', {
            method: 'POST',
            headers: buildHeaders(true),
            body: JSON.stringify({ targetUserId: targetUserId })
        })
        .then(function(response) {
            if (response.ok) {
                Toast.success('已合併虛擬成員「' + WeGo.escapeHtml(selectedGhostName) + '」');
                closeMergeModal();
                setTimeout(function() { window.location.reload(); }, 1000);
            } else {
                return response.json().then(function(error) {
                    Toast.error(error.message || '合併失敗');
                    enableButton(btn, originalHTML);
                });
            }
        })
        .catch(function() {
            Toast.error('網路錯誤，請稍後再試');
            enableButton(btn, originalHTML);
        });
    }

    function init() {
        document.addEventListener('click', function(e) {
            var actionEl = e.target.closest('[data-action]');
            if (!actionEl) return;

            var action = actionEl.dataset.action;

            switch (action) {
                case 'create-ghost':
                    createGhost(actionEl);
                    break;
                case 'confirm-remove-ghost':
                    confirmRemoveGhost(actionEl);
                    break;
                case 'remove-ghost':
                    removeGhost(actionEl);
                    break;
                case 'close-ghost-remove-dialog':
                    closeGhostRemoveDialog();
                    break;
                case 'open-merge-modal':
                    openMergeModal(actionEl);
                    break;
                case 'close-merge-modal':
                    closeMergeModal();
                    break;
                case 'confirm-merge':
                    confirmMerge(actionEl);
                    break;
            }
        });

        // Close modals on escape key
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') {
                closeGhostRemoveDialog();
                closeMergeModal();
            }
        });

        // Close modals on backdrop click
        var removeDialog = document.getElementById('removeGhostDialog');
        if (removeDialog) {
            removeDialog.addEventListener('click', function(e) {
                if (e.target === removeDialog) closeGhostRemoveDialog();
            });
        }

        var mergeModal = document.getElementById('mergeGhostModal');
        if (mergeModal) {
            mergeModal.addEventListener('click', function(e) {
                if (e.target === mergeModal) closeMergeModal();
            });
        }

        // Allow Enter key to submit ghost name
        var ghostNameInput = document.getElementById('ghostName');
        if (ghostNameInput) {
            ghostNameInput.addEventListener('keydown', function(e) {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    var btn = document.querySelector('[data-action="create-ghost"]');
                    if (btn) createGhost(btn);
                }
            });
        }
    }

    return { init: init };
})();

document.addEventListener('DOMContentLoaded', GhostMember.init);
