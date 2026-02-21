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

    // ── Anime.js Utilities ──────────────────────────────────────────
    // Requires anime.js v4 UMD loaded via CDN (defer).
    // All animation helpers respect prefers-reduced-motion.

    var _reducedMotionQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
    var _reducedMotion = _reducedMotionQuery.matches;
    _reducedMotionQuery.addEventListener('change', function(e) {
        _reducedMotion = e.matches;
        WeGo._reducedMotion = e.matches;
    });
    WeGo._reducedMotion = _reducedMotion;

    WeGo.anime = {};

    /**
     * Stagger-in animation for a group of elements.
     * Elements should start with CSS: opacity: 0; transform: translateY(20px)
     *
     * @param {string} selector - CSS selector for target elements
     * @param {object} [opts] - Options
     * @param {number} [opts.delay=80] - Stagger delay between items (ms)
     * @param {number} [opts.duration=200] - Per-item animation duration (ms)
     * @param {string} [opts.from='first'] - Stagger origin
     * @param {number} [opts.translateY=20] - Initial translateY offset (px)
     */
    WeGo.anime.staggerIn = function(selector, opts) {
        var els = document.querySelectorAll(selector);
        if (!els.length) return;

        var o = opts || {};
        var delay = o.delay != null ? o.delay : 80;
        var duration = o.duration != null ? o.duration : 200;
        var from = o.from || 'first';
        var translateYVal = o.translateY != null ? o.translateY : 20;

        if (_reducedMotion || typeof anime === 'undefined') {
            els.forEach(function(el) {
                el.style.opacity = '1';
                el.style.transform = 'translateY(0)';
            });
            return;
        }

        anime.animate(els, {
            opacity: [0, 1],
            translateY: [translateYVal, 0],
            duration: duration,
            delay: anime.stagger(delay, { from: from }),
            ease: 'outQuad'
        });
    };

    /**
     * Count-up animation for a numeric value displayed in an element.
     *
     * @param {HTMLElement} el - Target element to update textContent
     * @param {number} targetValue - Final numeric value
     * @param {object} [opts] - Options
     * @param {string} [opts.format='number'] - 'number' or 'currency'
     * @param {string} [opts.currency='TWD'] - Currency code for Intl formatting
     * @param {number} [opts.duration=800] - Animation duration (ms)
     * @param {number} [opts.decimals=0] - Decimal places
     */
    WeGo.anime.countUp = function(el, targetValue, opts) {
        if (!el) return;

        var o = opts || {};
        var duration = o.duration != null ? o.duration : 800;
        var decimals = o.decimals != null ? o.decimals : 0;
        var format = o.format || 'number';
        var currency = o.currency || 'TWD';

        var numberFormatter = new Intl.NumberFormat('en-US', {
            minimumFractionDigits: decimals, maximumFractionDigits: decimals
        });
        var prefix = format === 'currency' ? '$' : '';
        var formatter = {
            format: function(val) { return prefix + numberFormatter.format(val); }
        };

        if (_reducedMotion || typeof anime === 'undefined') {
            el.textContent = formatter.format(targetValue);
            return;
        }

        var obj = { value: 0 };
        anime.animate(obj, {
            value: targetValue,
            duration: duration,
            ease: 'outExpo',
            onUpdate: function() {
                el.textContent = formatter.format(Math.round(obj.value * Math.pow(10, decimals)) / Math.pow(10, decimals));
            }
        });
    };

    /**
     * Triggers a callback when element scrolls into the viewport.
     * Uses IntersectionObserver (no anime.js dependency for this).
     *
     * @param {string} selector - CSS selector for observed elements
     * @param {function} callback - Called with (element) when visible
     * @param {object} [opts] - Options
     * @param {number} [opts.threshold=0.2] - Visibility threshold (0-1)
     * @param {boolean} [opts.once=true] - Only trigger once per element
     */
    WeGo.anime.onVisible = function(selector, callback, opts) {
        var els = document.querySelectorAll(selector);
        if (!els.length) return;

        var o = opts || {};
        var threshold = o.threshold != null ? o.threshold : 0.2;
        var once = o.once !== false;

        var observer = new IntersectionObserver(function(entries) {
            entries.forEach(function(entry) {
                if (entry.isIntersecting) {
                    callback(entry.target);
                    if (once) {
                        observer.unobserve(entry.target);
                    }
                }
            });
        }, { threshold: threshold });

        els.forEach(function(el) { observer.observe(el); });
    };

    /**
     * Auto-initializes countUp animations on SSR elements with data-count-up attribute.
     * Usage: <p data-count-up="currency" data-value="12345">$12,345</p>
     *
     * Supported types: 'currency' (with $ prefix) and 'number' (plain integer).
     */
    WeGo.anime.initCountUpElements = function() {
        var els = document.querySelectorAll('[data-count-up]');
        if (!els.length) return;

        els.forEach(function(el) {
            var value = parseFloat(el.dataset.value);
            if (isNaN(value) || value === 0) return;

            var type = el.getAttribute('data-count-up');
            var opts = type === 'currency'
                ? { format: 'currency', currency: 'TWD' }
                : {};
            WeGo.anime.countUp(el, value, opts);
        });
    };

    // ── Top Loading Bar ────────────────────────────────────────────
    // NProgress-style 2px bar at top of viewport during fetch requests.

    var _loadingBar = null;
    var _loadingTimer = null;

    WeGo.loadingBar = {
        start: function() {
            if (_reducedMotion) return;
            if (!_loadingBar) {
                _loadingBar = document.createElement('div');
                _loadingBar.id = 'wego-loading-bar';
                document.body.appendChild(_loadingBar);
            }
            _loadingBar.className = '';
            _loadingBar.style.width = '0';
            _loadingBar.style.opacity = '1';
            clearTimeout(_loadingTimer);
            // Force reflow before adding active class
            void _loadingBar.offsetWidth;
            _loadingBar.classList.add('active');
        },
        done: function() {
            if (!_loadingBar) return;
            _loadingBar.classList.remove('active');
            _loadingBar.classList.add('done');
            _loadingTimer = setTimeout(function() {
                if (_loadingBar) {
                    _loadingBar.className = '';
                    _loadingBar.style.width = '0';
                }
            }, 600);
        }
    };

    /**
     * Initialize character counters for textareas with data-char-counter attribute.
     * The attribute value should be the ID of the counter display element.
     */
    WeGo.initCharCounters = function() {
        document.querySelectorAll('[data-char-counter]').forEach(function(textarea) {
            var counterId = textarea.dataset.charCounter;
            var counterEl = document.getElementById(counterId);
            if (!counterEl) return;
            counterEl.textContent = (textarea.value || '').length;
            textarea.addEventListener('input', function() {
                counterEl.textContent = this.value.length;
            });
        });
    };

    window.WeGo = WeGo;
})();

// Auto-initialize on DOMContentLoaded
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
        WeGo.preventDoubleSubmit();
        WeGo.initCharCounters();
        WeGo.anime.initCountUpElements();
    });
} else {
    WeGo.preventDoubleSubmit();
    WeGo.initCharCounters();
    WeGo.anime.initCountUpElements();
}
