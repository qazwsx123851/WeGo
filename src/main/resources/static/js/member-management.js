/**
 * WeGo - Member Management JavaScript Module
 *
 * Handles member management AJAX operations:
 * - Invite link generation and copying
 * - Role changes (EDITOR/VIEWER)
 * - Member removal
 * - Leave trip
 *
 * Integrates with global Toast utility from app.js
 * and WeGo shared utilities from common.js.
 *
 * @module MemberManagement
 */
const MemberManagement = (() => {
    'use strict';

    let selectedMemberId = null;
    let selectedMemberName = null;

    /**
     * Get trip ID from the page container data attribute.
     * @returns {string} Trip ID
     */
    function getTripId() {
        const el = document.getElementById('member-management-container');
        return el ? el.dataset.tripId : '';
    }

    /**
     * Build AJAX headers with CSRF token and optional Content-Type.
     * @param {boolean} [withJson=false] - Whether to include JSON Content-Type
     * @returns {Object} Headers object
     */
    function buildHeaders(withJson) {
        const headers = {
            [WeGo.getCsrfHeader()]: WeGo.getCsrfToken()
        };
        if (withJson) {
            headers['Content-Type'] = 'application/json';
        }
        return headers;
    }

    /**
     * Disable a button and show a spinner during AJAX.
     * @param {HTMLElement} btn - Button to disable
     * @returns {string} Original innerHTML for restoration
     */
    function disableButton(btn) {
        const originalHTML = btn.innerHTML;
        btn.disabled = true;
        btn.classList.add('opacity-60');
        btn.innerHTML = '<svg class="w-4 h-4 animate-spin inline-block" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/></svg><span class="ml-1">處理中...</span>';
        return originalHTML;
    }

    /**
     * Re-enable a button and restore its original content.
     * @param {HTMLElement} btn - Button to enable
     * @param {string} originalHTML - Original innerHTML
     */
    function enableButton(btn, originalHTML) {
        btn.disabled = false;
        btn.classList.remove('opacity-60');
        btn.innerHTML = originalHTML;
    }

    /**
     * Change a member's role via AJAX.
     * @param {HTMLElement} btn - The role button with data attributes
     */
    function changeRole(btn) {
        const tripId = btn.dataset.tripId;
        const userId = btn.dataset.userId;
        const newRole = btn.dataset.role;
        const originalHTML = disableButton(btn);

        WeGo.fetchWithTimeout('/api/trips/' + tripId + '/members/' + userId + '/role', {
            method: 'PUT',
            headers: buildHeaders(true),
            body: JSON.stringify({ role: newRole })
        })
        .then(function(response) {
            if (response.ok) {
                Toast.success('成員角色已更新');
                setTimeout(function() { window.location.reload(); }, 1000);
            } else {
                return response.json().then(function(error) {
                    Toast.error(error.message || '更新失敗');
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
     * Show the remove member confirmation dialog.
     * @param {HTMLElement} btn - Button with data-member-id and data-member-name
     */
    function confirmRemoveMember(btn) {
        selectedMemberId = btn.dataset.memberId;
        selectedMemberName = btn.dataset.memberName;

        const nameEl = document.getElementById('memberName');
        if (nameEl) {
            nameEl.textContent = selectedMemberName;
        }
        document.getElementById('removeMemberDialog').classList.remove('hidden');
    }

    /**
     * Close the remove member confirmation dialog.
     */
    function closeRemoveDialog() {
        document.getElementById('removeMemberDialog').classList.add('hidden');
        selectedMemberId = null;
        selectedMemberName = null;
    }

    /**
     * Execute member removal via AJAX.
     * @param {HTMLElement} btn - The confirm remove button
     */
    function removeMember(btn) {
        if (!selectedMemberId) return;

        const tripId = getTripId();
        const originalHTML = disableButton(btn);

        WeGo.fetchWithTimeout('/api/trips/' + tripId + '/members/' + selectedMemberId, {
            method: 'DELETE',
            headers: buildHeaders(false)
        })
        .then(function(response) {
            if (response.ok) {
                Toast.success('成員已移除');
                closeRemoveDialog();
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
     * Generate a new invite link via AJAX.
     * @param {HTMLElement} btn - The generate link button
     */
    function generateInviteLink(btn) {
        const tripId = getTripId();
        const originalHTML = disableButton(btn);

        WeGo.fetchWithTimeout('/api/trips/' + tripId + '/invites', {
            method: 'POST',
            headers: buildHeaders(true),
            body: JSON.stringify({
                role: 'EDITOR',
                expiresInHours: 168
            })
        })
        .then(function(response) {
            if (response.ok) {
                return response.json().then(function(data) {
                    document.getElementById('inviteLink').value = data.data.inviteUrl;
                    const expiryEl = document.getElementById('inviteLinkExpiry');
                    if (expiryEl) expiryEl.classList.remove('hidden');
                    Toast.success('已產生新的邀請連結');
                    enableButton(btn, originalHTML);
                });
            } else {
                return response.json().then(function(error) {
                    Toast.error(error.message || '產生連結失敗');
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
     * Copy invite link to clipboard.
     * @param {HTMLElement} btn - The copy button
     */
    function copyInviteLink(btn) {
        const linkInput = document.getElementById('inviteLink');
        if (!linkInput || !linkInput.value) {
            Toast.error('請先產生邀請連結');
            return;
        }

        const spanEl = btn.querySelector('span');
        const originalText = spanEl ? spanEl.textContent : '';

        navigator.clipboard.writeText(linkInput.value)
            .then(function() {
                if (spanEl) {
                    spanEl.textContent = '已複製';
                    setTimeout(function() { spanEl.textContent = originalText; }, 2000);
                }
            })
            .catch(function() {
                // Fallback for older browsers
                linkInput.select();
                document.execCommand('copy');
                if (spanEl) {
                    spanEl.textContent = '已複製';
                    setTimeout(function() { spanEl.textContent = originalText; }, 2000);
                }
            });
    }

    /**
     * Show the leave trip confirmation dialog.
     */
    function confirmLeaveTrip() {
        document.getElementById('leaveTripDialog').classList.remove('hidden');
    }

    /**
     * Close the leave trip confirmation dialog.
     */
    function closeLeaveDialog() {
        document.getElementById('leaveTripDialog').classList.add('hidden');
    }

    /**
     * Execute leave trip via AJAX.
     * @param {HTMLElement} btn - The confirm leave button
     */
    function leaveTrip(btn) {
        const tripId = getTripId();
        const originalHTML = disableButton(btn);

        WeGo.fetchWithTimeout('/api/trips/' + tripId + '/members/me', {
            method: 'DELETE',
            headers: buildHeaders(false)
        })
        .then(function(response) {
            if (response.ok) {
                Toast.success('已離開行程');
                setTimeout(function() { window.location.href = '/dashboard'; }, 1000);
            } else {
                return response.json().then(function(error) {
                    Toast.error(error.message || '離開行程失敗');
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
     * Toggle role dropdown visibility.
     * @param {HTMLElement} btn - The gear button element
     */
    function toggleRoleDropdown(btn) {
        const dropdown = btn.parentElement.querySelector('.role-dropdown');
        // Close all other dropdowns first
        document.querySelectorAll('.role-dropdown').forEach(function(d) {
            if (d !== dropdown) d.classList.add('hidden');
        });
        dropdown.classList.toggle('hidden');
    }

    /**
     * Close all role dropdowns.
     */
    function closeAllDropdowns() {
        document.querySelectorAll('.role-dropdown').forEach(function(d) {
            d.classList.add('hidden');
        });
    }

    /**
     * Initialize event delegation and global listeners.
     */
    function init() {
        // Event delegation for all data-action buttons
        document.addEventListener('click', function(e) {
            const actionEl = e.target.closest('[data-action]');
            if (!actionEl) return;

            const action = actionEl.dataset.action;

            switch (action) {
                case 'generate-invite-link':
                    generateInviteLink(actionEl);
                    break;
                case 'copy-invite-link':
                    copyInviteLink(actionEl);
                    break;
                case 'toggle-role-dropdown':
                    e.stopPropagation();
                    toggleRoleDropdown(actionEl);
                    break;
                case 'change-role':
                    changeRole(actionEl);
                    break;
                case 'confirm-remove-member':
                    confirmRemoveMember(actionEl);
                    break;
                case 'remove-member':
                    removeMember(actionEl);
                    break;
                case 'close-remove-dialog':
                    closeRemoveDialog();
                    break;
                case 'confirm-leave-trip':
                    confirmLeaveTrip();
                    break;
                case 'leave-trip':
                    leaveTrip(actionEl);
                    break;
                case 'close-leave-dialog':
                    closeLeaveDialog();
                    break;
            }
        });

        // Close role dropdowns when clicking outside
        document.addEventListener('click', function(e) {
            if (!e.target.closest('.relative')) {
                closeAllDropdowns();
            }
        });

        // Close dialogs on escape key
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') {
                closeRemoveDialog();
                closeLeaveDialog();
                closeAllDropdowns();
            }
        });

        // Close dialogs on backdrop click
        const removeDialog = document.getElementById('removeMemberDialog');
        if (removeDialog) {
            removeDialog.addEventListener('click', function(e) {
                if (e.target === removeDialog) closeRemoveDialog();
            });
        }

        const leaveDialog = document.getElementById('leaveTripDialog');
        if (leaveDialog) {
            leaveDialog.addEventListener('click', function(e) {
                if (e.target === leaveDialog) closeLeaveDialog();
            });
        }
    }

    return { init };
})();

document.addEventListener('DOMContentLoaded', MemberManagement.init);
