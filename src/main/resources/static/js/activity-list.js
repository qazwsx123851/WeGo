/**
 * WeGo - Activity List JavaScript Module
 *
 * Handles activity group toggle, date tab navigation,
 * recalculate transport, and reorder loading overlay.
 *
 * Integrates with global Toast utility from app.js
 * and WeGo shared utilities from common.js.
 */
const ActivityList = (() => {
    'use strict';

    // --- Activity Group Toggle ---

    function toggleActivityGroup(header) {
        const content = header.nextElementSibling;
        if (!content || !content.classList.contains('activity-group-content')) return;

        const chevron = header.querySelector('.chevron-icon');
        const isCollapsed = content.classList.contains('max-h-0');
        const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

        if (content._cleanupTimer) clearTimeout(content._cleanupTimer);
        if (content._cleanup) {
            content.removeEventListener('transitionend', content._cleanup);
            content._cleanup = null;
        }

        header.setAttribute('aria-expanded', isCollapsed ? 'true' : 'false');

        if (isCollapsed) {
            content.classList.remove('max-h-0');
            content.style.maxHeight = content.scrollHeight + 'px';
            if (chevron) chevron.classList.add('rotate-180');

            const cleanup = function() {
                content.style.maxHeight = '';
                content.removeEventListener('transitionend', cleanup);
                content._cleanup = null;
                clearTimeout(content._cleanupTimer);
            };
            content._cleanup = cleanup;
            if (prefersReducedMotion) {
                content.style.maxHeight = '';
            } else {
                content.addEventListener('transitionend', cleanup);
                content._cleanupTimer = setTimeout(cleanup, 300);
            }
        } else {
            content.style.maxHeight = content.scrollHeight + 'px';
            if (chevron) chevron.classList.remove('rotate-180');

            if (prefersReducedMotion) {
                content.classList.add('max-h-0');
                content.style.maxHeight = '0px';
            } else {
                requestAnimationFrame(function() {
                    content.classList.add('max-h-0');
                    content.style.maxHeight = '0px';
                });
            }
        }
    }

    // --- Date Tab Navigation ---

    function initDateTabs() {
        const dateTabs = document.querySelectorAll('button[data-date]');
        const activeClasses = ['bg-primary-500', 'text-white'];
        const inactiveClasses = ['bg-white', 'dark:bg-gray-800', 'text-gray-700', 'dark:text-gray-200'];

        dateTabs.forEach(function(tab) {
            tab.addEventListener('click', function() {
                const date = this.getAttribute('data-date');
                const section = document.getElementById('day-' + date);
                if (!section) return;

                const header = section.querySelector('[data-action="toggle-group"]');
                if (header) {
                    const content = header.nextElementSibling;
                    if (content && content.classList.contains('activity-group-content')
                        && content.classList.contains('max-h-0')) {
                        toggleActivityGroup(header);
                    }
                }

                setTimeout(function() {
                    section.scrollIntoView({ behavior: 'smooth', block: 'start' });
                }, 50);

                dateTabs.forEach(function(t) {
                    activeClasses.forEach(function(c) { t.classList.remove(c); });
                    inactiveClasses.forEach(function(c) { t.classList.add(c); });
                });
                inactiveClasses.forEach(function(c) { this.classList.remove(c); }.bind(this));
                activeClasses.forEach(function(c) { this.classList.add(c); }.bind(this));
            });
        });
    }

    // --- Lottie Helper ---

    function initLottie(containerId, spinnerId, animationPath) {
        const container = document.getElementById(containerId);
        const spinner = document.getElementById(spinnerId);
        let animation = null;

        try {
            if (typeof lottie !== 'undefined' && container) {
                animation = lottie.loadAnimation({
                    container: container,
                    renderer: 'svg',
                    loop: true,
                    autoplay: false,
                    path: animationPath
                });
                animation.addEventListener('error', function() {
                    container.classList.add('hidden');
                    if (spinner) spinner.classList.remove('hidden');
                });
            } else {
                if (container) container.classList.add('hidden');
                if (spinner) spinner.classList.remove('hidden');
            }
        } catch (e) {
            if (container) container.classList.add('hidden');
            if (spinner) spinner.classList.remove('hidden');
        }

        return animation;
    }

    // --- Reorder Loading Overlay ---

    function initReorderLoading() {
        const reorderAnimation = initLottie('reorder-lottie', 'reorder-css-spinner', '/animations/route-loading.json');

        window.ReorderLoading = {
            show: function() {
                const overlay = document.getElementById('reorder-loading-overlay');
                if (overlay) {
                    overlay.classList.remove('opacity-0', 'pointer-events-none');
                    overlay.classList.add('opacity-100');
                    if (reorderAnimation) reorderAnimation.play();
                }
            },
            hide: function() {
                const overlay = document.getElementById('reorder-loading-overlay');
                if (overlay) {
                    overlay.classList.add('opacity-0', 'pointer-events-none');
                    overlay.classList.remove('opacity-100');
                    if (reorderAnimation) reorderAnimation.stop();
                }
            }
        };
    }

    // --- Recalculate Transport ---

    function initRecalculateTransport() {
        const recalculateBtn = document.getElementById('recalculate-transport-btn');
        const recalculateModal = document.getElementById('recalculate-modal');

        if (!recalculateBtn) return;

        const loadingAnimation = initLottie('lottie-loading', 'css-spinner', '/animations/route-loading.json');

        function hideLoading() {
            recalculateModal.classList.add('hidden');
            if (loadingAnimation) loadingAnimation.stop();
        }

        if (window.matchMedia('(prefers-reduced-motion: reduce)').matches && loadingAnimation) {
            loadingAnimation.setSpeed(0.5);
        }

        recalculateBtn.addEventListener('click', async function() {
            if (recalculateBtn.disabled) return;
            recalculateBtn.disabled = true;

            const tripId = this.dataset.tripId;

            recalculateModal.classList.remove('hidden');
            if (loadingAnimation) loadingAnimation.play();

            try {
                const response = await WeGo.fetchWithTimeout(
                    '/trips/' + tripId + '/recalculate-transport',
                    {
                        method: 'POST',
                        headers: {
                            [WeGo.getCsrfHeader()]: WeGo.getCsrfToken(),
                            'X-Requested-With': 'XMLHttpRequest',
                            'Accept': 'application/json'
                        },
                        credentials: 'same-origin'
                    },
                    60000
                );

                let data;
                try {
                    data = await response.json();
                } catch (_) {
                    hideLoading();
                    recalculateBtn.disabled = false;
                    Toast.error('伺服器回應格式錯誤，請稍後再試');
                    return;
                }

                hideLoading();

                if (response.ok && data.success) {
                    Toast.success(data.message);
                    setTimeout(function() { window.location.reload(); }, 500);
                } else {
                    recalculateBtn.disabled = false;
                    Toast.error(data.message || '重新計算失敗，請稍後再試');
                }
            } catch (error) {
                hideLoading();
                recalculateBtn.disabled = false;
                const msg = error.message === 'Request timed out'
                    ? '重新計算逾時，請稍後再試'
                    : '重新計算失敗，請稍後再試';
                Toast.error(msg);
            }
        });
    }

    // --- Event Delegation ---

    function initEventDelegation() {
        document.addEventListener('click', function(e) {
            const toggleTarget = e.target.closest('[data-action="toggle-group"]');
            if (toggleTarget) {
                toggleActivityGroup(toggleTarget);
                return;
            }
        });

        document.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' || e.key === ' ') {
                const toggleTarget = e.target.closest('[data-action="toggle-group"]');
                if (toggleTarget) {
                    e.preventDefault();
                    toggleActivityGroup(toggleTarget);
                }
            }
        });
    }

    // --- Init ---

    function init() {
        initEventDelegation();
        initDateTabs();
        initReorderLoading();
        initRecalculateTransport();
    }

    return { init };
})();

document.addEventListener('DOMContentLoaded', ActivityList.init);
