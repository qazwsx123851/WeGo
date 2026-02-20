/**
 * WeGo Common Utilities
 * Shared across all pages to eliminate code duplication.
 *
 * @module WeGo
 */
(function() {
    'use strict';

    const WeGo = {};

    /**
     * Escapes HTML entities to prevent XSS.
     *
     * Handles: & < > " ' (all 5 critical HTML entities)
     *
     * @param {string} text - The text to escape
     * @returns {string} The escaped text
     */
    WeGo.escapeHtml = function(text) {
        if (text == null) return '';
        var div = document.createElement('div');
        div.textContent = String(text);
        return div.innerHTML;
    };

    /**
     * Gets CSRF token from meta tag.
     *
     * Reads from: <meta name="_csrf" content="..."/>
     *
     * @returns {string} CSRF token value
     * @throws {Error} if CSRF meta tag is not found
     */
    WeGo.getCsrfToken = function() {
        var meta = document.querySelector('meta[name="_csrf"]');
        if (!meta || !meta.getAttribute('content')) {
            throw new Error('CSRF token not found');
        }
        return meta.getAttribute('content');
    };

    /**
     * Gets CSRF header name from meta tag.
     *
     * Reads from: <meta name="_csrf_header" content="..."/>
     *
     * @returns {string} CSRF header name (default: 'X-CSRF-TOKEN')
     */
    WeGo.getCsrfHeader = function() {
        var meta = document.querySelector('meta[name="_csrf_header"]');
        return meta ? meta.getAttribute('content') : 'X-CSRF-TOKEN';
    };

    /**
     * Fetch wrapper with timeout support.
     *
     * @param {string} url - The URL to fetch
     * @param {object} [options={}] - Standard fetch options
     * @param {number} [timeoutMs=30000] - Timeout in milliseconds
     * @returns {Promise<Response>} The fetch response
     * @throws {Error} 'Request timed out' if timeout exceeded
     */
    WeGo.fetchWithTimeout = function(url, options, timeoutMs) {
        var timeout = (typeof timeoutMs === 'number' && timeoutMs > 0) ? timeoutMs : 30000;

        var controller = new AbortController();
        var merged = Object.assign({}, options || {}, { signal: controller.signal });

        var timeoutId = setTimeout(function() {
            controller.abort();
        }, timeout);

        return fetch(url, merged).then(function(response) {
            clearTimeout(timeoutId);
            return response;
        }).catch(function(error) {
            clearTimeout(timeoutId);
            if (error.name === 'AbortError') {
                throw new Error('Request timed out');
            }
            throw error;
        });
    };

    /**
     * Prevents double form submission by disabling the submit button.
     * Automatically attaches to all forms matching the selector.
     * Re-enables the button after 5 seconds as a safety net.
     *
     * @param {string} [selector='form[data-prevent-double-submit]'] CSS selector for forms
     */
    WeGo.preventDoubleSubmit = function(selector) {
        var sel = selector || 'form[data-prevent-double-submit]';
        document.querySelectorAll(sel).forEach(function(form) {
            form.addEventListener('submit', function(e) {
                var btn = form.querySelector('button[type="submit"], input[type="submit"]');
                if (!btn) return;
                if (btn.dataset.submitting === 'true') {
                    e.preventDefault();
                    return false;
                }
                btn.dataset.submitting = 'true';
                btn.disabled = true;
                var originalText = btn.innerHTML;
                btn.innerHTML = '<svg class="animate-spin -ml-1 mr-2 h-4 w-4 inline" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg> 處理中...';
                // Safety net: re-enable after 5 seconds
                setTimeout(function() {
                    btn.disabled = false;
                    btn.dataset.submitting = 'false';
                    btn.innerHTML = originalText;
                }, 5000);
            });
        });
    };

    /**
     * Shared category labels (Traditional Chinese).
     * Used by personal-expense.js, expense-statistics.js, expense-list.js.
     */
    WeGo.CATEGORY_LABELS = {
        FOOD: '餐飲', TRANSPORT: '交通', ACCOMMODATION: '住宿',
        SHOPPING: '購物', ENTERTAINMENT: '娛樂', HEALTH: '健康', OTHER: '其他'
    };

    /**
     * Shared category colors for charts.
     * Used by personal-expense.js, expense-statistics.js.
     */
    WeGo.CATEGORY_COLORS = {
        FOOD: '#F97316', TRANSPORT: '#3B82F6', ACCOMMODATION: '#8B5CF6',
        SHOPPING: '#EC4899', ENTERTAINMENT: '#F43F5E', HEALTH: '#10B981', OTHER: '#6B7280'
    };

    window.WeGo = WeGo;
})();

// Auto-initialize on DOMContentLoaded
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
        WeGo.preventDoubleSubmit();
    });
} else {
    WeGo.preventDoubleSubmit();
}
