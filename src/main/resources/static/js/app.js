/**
 * WeGo - Main Application JavaScript
 *
 * @contract
 *   - Initializes toast notifications, dark mode toggle, and UI interactions
 *   - Provides global utility functions for components
 */

// Toast Notification System
const Toast = {
    container: null,

    /**
     * Initialize toast container
     */
    init() {
        this.container = document.getElementById('toast-container');
        if (!this.container) {
            this.container = document.createElement('div');
            this.container.id = 'toast-container';
            this.container.className = 'fixed top-4 right-4 z-50 flex flex-col gap-3 pointer-events-none';
            document.body.appendChild(this.container);
        }
    },

    /**
     * Show a toast notification
     * @param {string} message - The message to display
     * @param {string} type - Toast type: 'success' | 'error' | 'warning' | 'info'
     * @param {number} duration - Duration in milliseconds (default: 4000)
     */
    show(message, type = 'info', duration = 4000) {
        if (!this.container) this.init();

        const toast = document.createElement('div');
        toast.className = this.getToastClasses(type);
        toast.innerHTML = this.getToastContent(message, type);

        // Add close button handler
        const closeBtn = toast.querySelector('[data-toast-close]');
        if (closeBtn) {
            closeBtn.addEventListener('click', () => this.dismiss(toast));
        }

        this.container.appendChild(toast);

        // Use anime.js spring entrance if available, otherwise CSS handles it
        const canAnimate = typeof anime !== 'undefined' && !(typeof WeGo !== 'undefined' && WeGo._reducedMotion);
        if (canAnimate) {
            toast.classList.remove('animate-slide-in');
            toast.style.transform = 'translateX(100%)';
            toast.style.opacity = '0';
            var staggerDelay = Math.min((this.container.children.length - 1) * 50, 200);
            anime.animate(toast, {
                translateX: ['100%', 0],
                opacity: [0, 1],
                delay: staggerDelay,
                ease: anime.createSpring({ stiffness: 300, damping: 20, mass: 1 })
            });
        }

        // Auto dismiss
        if (duration > 0) {
            setTimeout(() => this.dismiss(toast), duration);
        }

        return toast;
    },

    /**
     * Get CSS classes for toast type
     */
    getToastClasses(type) {
        const baseClasses = 'pointer-events-auto flex items-center gap-3 px-4 py-3 rounded-xl shadow-lg animate-slide-in max-w-sm';
        const typeClasses = {
            success: 'bg-success-light text-success-dark border border-success/20',
            error: 'bg-error-light text-error-dark border border-error/20',
            warning: 'bg-warning-light text-warning-dark border border-warning/20',
            info: 'bg-info-light text-info-dark border border-info/20',
        };
        return `${baseClasses} ${typeClasses[type] || typeClasses.info}`;
    },

    /**
     * Get HTML content for toast
     */
    getToastContent(message, type) {
        const icons = {
            success: '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>',
            error: '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>',
            warning: '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>',
            info: '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>',
        };

        return `
            <svg class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                ${icons[type] || icons.info}
            </svg>
            <span class="text-body-sm font-medium flex-1">${this.escapeHtml(message)}</span>
            <button data-toast-close class="p-1 hover:bg-black/10 rounded-full transition-colors cursor-pointer" aria-label="關閉">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                </svg>
            </button>
        `;
    },

    /**
     * Dismiss a toast with animation
     */
    dismiss(toast) {
        if (!toast || !toast.parentNode) return;

        toast.classList.remove('animate-slide-in');
        toast.classList.add('animate-slide-out');

        toast.addEventListener('animationend', () => {
            toast.remove();
        }, { once: true });
    },

    /**
     * Helper: Escape HTML to prevent XSS
     * Delegates to WeGo.escapeHtml shared utility.
     */
    escapeHtml(text) {
        return WeGo.escapeHtml(text);
    },

    // Convenience methods
    success(message, duration) { return this.show(message, 'success', duration); },
    error(message, duration) { return this.show(message, 'error', duration); },
    warning(message, duration) { return this.show(message, 'warning', duration); },
    info(message, duration) { return this.show(message, 'info', duration); },
};

// Dark Mode Toggle
const DarkMode = {
    /**
     * Initialize dark mode based on system preference or saved preference
     */
    init() {
        const savedTheme = localStorage.getItem('theme');
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;

        if (savedTheme === 'dark' || (!savedTheme && prefersDark)) {
            document.documentElement.classList.add('dark');
        } else {
            document.documentElement.classList.remove('dark');
        }

        // Listen for system preference changes
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
            if (!localStorage.getItem('theme')) {
                document.documentElement.classList.toggle('dark', e.matches);
            }
        });
    },

    /**
     * Toggle dark mode
     */
    toggle() {
        const isDark = document.documentElement.classList.toggle('dark');
        localStorage.setItem('theme', isDark ? 'dark' : 'light');

        // Dispatch custom event for components (e.g., Chart.js) to update
        window.dispatchEvent(new CustomEvent('themechange', { detail: { isDark } }));

        return isDark;
    },

    /**
     * Get current mode
     */
    isDark() {
        return document.documentElement.classList.contains('dark');
    },
};

// Loading State Management
const Loading = {
    /**
     * Show loading state on a button
     */
    start(button) {
        if (!button) return;

        button.disabled = true;
        button.dataset.originalContent = button.innerHTML;
        button.innerHTML = `
            <svg class="animate-spin w-5 h-5 mr-2" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            處理中...
        `;
    },

    /**
     * Reset button to original state
     */
    stop(button) {
        if (!button || !button.dataset.originalContent) return;

        button.disabled = false;
        button.innerHTML = button.dataset.originalContent;
        delete button.dataset.originalContent;
    },
};

// Form Validation Helpers
const FormValidation = {
    /**
     * Find the container element for an input (used for error message placement).
     * Matches the .space-y-1.5 / .space-y-1 wrappers used across all form templates.
     */
    _findContainer(input) {
        return input.closest('[class*="space-y-1"]') || input.closest('.relative')?.parentNode || input.parentNode;
    },

    /**
     * Show error message for an input field.
     * Adds red border, error text below input, and aria attributes for accessibility.
     */
    showError(input, message) {
        const container = this._findContainer(input);
        this.clearError(input);

        // Save original border classes before replacing (for clean restoration)
        if (!input.dataset._savedBorderClasses) {
            var saved = ['border-gray-200', 'dark:border-gray-700',
                         'focus:ring-primary-500/50', 'focus:border-primary-500',
                         'focus:ring-primary-500', 'focus:border-transparent']
                .filter(function(cls) { return input.classList.contains(cls); });
            input.dataset._savedBorderClasses = JSON.stringify(saved);
        }

        // Add error border (light + dark mode)
        input.classList.add('border-error', 'focus:ring-error/50', 'focus:border-error');
        var savedClasses = JSON.parse(input.dataset._savedBorderClasses || '[]');
        savedClasses.forEach(function(cls) { input.classList.remove(cls); });

        // Create error message element
        const errorId = 'error-' + (input.id || input.name || '') + '-' + Math.random().toString(36).slice(2);
        const errorEl = document.createElement('p');
        errorEl.className = 'field-error-message form-error';
        errorEl.id = errorId;
        errorEl.setAttribute('role', 'status');
        errorEl.setAttribute('aria-live', 'polite');
        errorEl.textContent = message;
        container.appendChild(errorEl);

        // Accessibility attributes on input
        input.setAttribute('aria-invalid', 'true');
        input.setAttribute('aria-describedby', errorId);
    },

    /**
     * Clear error message for an input field.
     * Restores normal border and removes aria attributes.
     */
    clearError(input) {
        const container = this._findContainer(input);
        const errorEl = container.querySelector('.form-error');
        if (errorEl) errorEl.remove();

        input.classList.remove('border-error', 'focus:ring-error/50', 'focus:border-error');
        // Restore original border classes saved by showError
        var saved = JSON.parse(input.dataset._savedBorderClasses || '["border-gray-200","dark:border-gray-700","focus:ring-primary-500/50","focus:border-primary-500"]');
        saved.forEach(function(cls) { input.classList.add(cls); });
        delete input.dataset._savedBorderClasses;

        input.removeAttribute('aria-invalid');
        input.removeAttribute('aria-describedby');
    },

    /**
     * Validate email format
     */
    isValidEmail(email) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    },

    /**
     * Validate required field
     */
    isRequired(value) {
        return value !== null && value !== undefined && value.toString().trim() !== '';
    },

    /**
     * Validate a single input based on its data-validate attributes.
     * @returns {{ valid: boolean, message: string }}
     */
    validateInput(input) {
        const value = input.value.trim();
        const rules = (input.dataset.validate || '').split(',').map(function(r) { return r.trim(); }).filter(Boolean);
        const customMsg = input.dataset.validateMessage;

        for (var i = 0; i < rules.length; i++) {
            var rule = rules[i];
            switch (rule) {
                case 'required':
                    if (!this.isRequired(value)) {
                        return { valid: false, message: customMsg || '此欄位為必填' };
                    }
                    break;
                case 'email':
                    if (value && !this.isValidEmail(value)) {
                        return { valid: false, message: customMsg || '請輸入有效的電子郵件' };
                    }
                    break;
                case 'min': {
                    var minVal = parseFloat(input.getAttribute('min'));
                    var numVal = parseFloat(value);
                    if (value !== '' && isNaN(numVal)) {
                        return { valid: false, message: customMsg || '請輸入有效數字' };
                    }
                    if (!isNaN(minVal) && value !== '' && numVal < minVal) {
                        return { valid: false, message: customMsg || '最小值為 ' + minVal };
                    }
                    break;
                }
                case 'max': {
                    var maxVal = parseFloat(input.getAttribute('max'));
                    var numVal2 = parseFloat(value);
                    if (value !== '' && isNaN(numVal2)) {
                        return { valid: false, message: customMsg || '請輸入有效數字' };
                    }
                    if (!isNaN(maxVal) && value !== '' && numVal2 > maxVal) {
                        return { valid: false, message: customMsg || '最大值為 ' + maxVal };
                    }
                    break;
                }
                case 'maxlength': {
                    var maxLen = parseInt(input.getAttribute('maxlength'), 10);
                    if (maxLen && value.length > maxLen) {
                        return { valid: false, message: customMsg || '最多 ' + maxLen + ' 個字' };
                    }
                    break;
                }
            }
        }
        return { valid: true, message: '' };
    },

    /**
     * Initialize validation on a form.
     * Attaches blur validation, input error clearing, and submit interception.
     */
    init(form) {
        if (!form || form.dataset.validationInitialized) return;
        form.dataset.validationInitialized = 'true';
        var self = this;

        var inputs = form.querySelectorAll('[data-validate]');

        // Blur: validate on focus out
        inputs.forEach(function(input) {
            input.addEventListener('blur', function() {
                var result = self.validateInput(input);
                if (!result.valid) {
                    self.showError(input, result.message);
                }
            });

            // Input: clear error when user starts typing
            input.addEventListener('input', function() {
                if (input.getAttribute('aria-invalid') === 'true') {
                    self.clearError(input);
                }
            });

            // For select elements, also listen to change
            if (input.tagName === 'SELECT') {
                input.addEventListener('change', function() {
                    if (input.getAttribute('aria-invalid') === 'true') {
                        self.clearError(input);
                    }
                });
            }
        });

        // Submit interception (capture phase — runs before preventDoubleSubmit)
        form.addEventListener('submit', function(e) {
            if (!self.validateForm(form)) {
                e.preventDefault();
                e.stopImmediatePropagation();
            }
        }, true);
    },

    /**
     * Validate all data-validate inputs in a form.
     * On failure: shows errors, shakes form, scrolls to first error.
     * @returns {boolean} true if all valid
     */
    validateForm(form) {
        var inputs = form.querySelectorAll('[data-validate]');
        var firstError = null;
        var allValid = true;
        var self = this;

        inputs.forEach(function(input) {
            var result = self.validateInput(input);
            if (!result.valid) {
                self.showError(input, result.message);
                if (!firstError) firstError = input;
                allValid = false;
            } else {
                self.clearError(input);
            }
        });

        if (!allValid) {
            self.shakeElement(firstError);
            self.scrollToFirstError(form);
        }

        return allValid;
    },

    /**
     * Shake animation using anime.js (respects prefers-reduced-motion).
     * Falls back to CSS animation class.
     */
    shakeElement(element) {
        if (!element) return;
        var reducedMotion = typeof WeGo !== 'undefined' && WeGo._reducedMotion;
        if (reducedMotion) return;

        if (typeof anime !== 'undefined' && anime.animate) {
            anime.animate(element, {
                translateX: [0, -6, 6, -4, 4, -2, 2, 0],
                duration: 400,
                easing: 'easeInOutQuad'
            });
        } else {
            element.classList.add('animate-shake');
            setTimeout(function() { element.classList.remove('animate-shake'); }, 400);
        }
    },

    /**
     * Scroll to the first invalid input in the form and focus it.
     */
    scrollToFirstError(form) {
        var firstError = form.querySelector('[aria-invalid="true"]');
        if (firstError) {
            firstError.scrollIntoView({ behavior: 'smooth', block: 'center' });
            setTimeout(function() { firstError.focus({ preventScroll: true }); }, 300);
        }
    },

    /**
     * Scan for server-rendered error messages and apply red border to corresponding inputs.
     * Detects <p role="alert"> and <p> with text-error class near form inputs.
     */
    initServerErrors() {
        var self = this;
        var errorEls = document.querySelectorAll('p[role="alert"], p.text-error, p.text-xs.text-error');
        errorEls.forEach(function(errorP) {
            if (!errorP.textContent || errorP.textContent.trim() === '') return;
            var container = errorP.closest('[class*="space-y-1"]') || errorP.parentNode;
            var input = container.querySelector('input, textarea, select');
            if (input) {
                input.classList.add('border-error', 'focus:ring-error/50', 'focus:border-error');
                input.classList.remove('border-gray-200', 'dark:border-gray-700',
                                       'focus:ring-primary-500/50', 'focus:border-primary-500');
                input.setAttribute('aria-invalid', 'true');
            }
        });

        // Scroll to first server error
        var firstError = document.querySelector('[aria-invalid="true"]');
        if (firstError) {
            setTimeout(function() { firstError.scrollIntoView({ behavior: 'smooth', block: 'center' }); }, 300);
        }
    },

    /**
     * Auto-initialize all forms with data-validate-form attribute.
     * Also processes server-rendered errors on page load.
     */
    autoInit() {
        var self = this;
        document.querySelectorAll('form[data-validate-form]').forEach(function(form) {
            self.init(form);
        });
        self.initServerErrors();
    }
};

// Modal Management
const Modal = {
    _previousFocus: null,

    /**
     * Open a modal by ID.
     * Saves the previously focused element and traps focus inside the modal.
     * Uses anime.js timeline for sequential entrance if available.
     */
    open(modalId) {
        const modal = document.getElementById(modalId);
        if (!modal) return;

        this._previousFocus = document.activeElement;

        modal.classList.remove('hidden');
        modal.classList.add('flex');
        document.body.classList.add('overflow-hidden');

        const backdrop = modal.querySelector('[data-modal-backdrop]');
        const content = modal.querySelector('.relative');
        const canAnimate = typeof anime !== 'undefined' && !(typeof WeGo !== 'undefined' && WeGo._reducedMotion);

        if (canAnimate && backdrop && content) {
            backdrop.style.opacity = '0';
            content.style.opacity = '0';
            content.style.transform = 'scale(0.95) translateY(10px)';

            var tl = anime.createTimeline({ defaults: { ease: 'outQuad' } });
            tl.add(backdrop, { opacity: [0, 1], duration: 120 }, 0);
            tl.add(content, {
                opacity: [0, 1],
                scale: [0.95, 1],
                translateY: [10, 0],
                duration: 250,
                ease: 'outQuint'
            }, 80);
        }

        // Focus first focusable element
        const focusable = this._getFocusable(modal);
        if (focusable.length) focusable[0].focus();
    },

    /**
     * Close a modal by ID.
     * Restores focus to the element that was focused before the modal opened.
     * Uses anime.js for exit animation if available.
     */
    close(modalId) {
        const modal = document.getElementById(modalId);
        if (!modal) return;

        const backdrop = modal.querySelector('[data-modal-backdrop]');
        const content = modal.querySelector('.relative');
        const canAnimate = typeof anime !== 'undefined' && !(typeof WeGo !== 'undefined' && WeGo._reducedMotion);

        const finalize = () => {
            modal.classList.add('hidden');
            modal.classList.remove('flex');
            document.body.classList.remove('overflow-hidden');
            // Reset inline styles from animation
            if (backdrop) { backdrop.style.opacity = ''; }
            if (content) { content.style.opacity = ''; content.style.transform = ''; }

            if (this._previousFocus && typeof this._previousFocus.focus === 'function') {
                this._previousFocus.focus();
                this._previousFocus = null;
            }
        };

        if (canAnimate && backdrop && content) {
            var tl = anime.createTimeline({
                defaults: { ease: 'inQuad' },
                onComplete: finalize
            });
            tl.add(content, { opacity: [1, 0], scale: [1, 0.95], duration: 150 }, 0);
            tl.add(backdrop, { opacity: [1, 0], duration: 150 }, 30);
        } else {
            finalize();
        }
    },

    /**
     * Get all focusable elements within a container.
     */
    _getFocusable(container) {
        return Array.from(container.querySelectorAll(
            'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
        )).filter(el => el.offsetParent !== null);
    },

    /**
     * Initialize modal close handlers
     */
    init() {
        // Close on backdrop click
        document.querySelectorAll('[data-modal-backdrop]').forEach(backdrop => {
            backdrop.addEventListener('click', (e) => {
                if (e.target === backdrop) {
                    const modalId = backdrop.closest('[data-modal]')?.id;
                    if (modalId) this.close(modalId);
                }
            });
        });

        // Close on Escape key + focus trap (Tab/Shift+Tab)
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                const openModal = document.querySelector('[data-modal]:not(.hidden)');
                if (openModal) this.close(openModal.id);
                return;
            }

            if (e.key === 'Tab') {
                const openModal = document.querySelector('[data-modal]:not(.hidden)');
                if (!openModal) return;

                const focusable = this._getFocusable(openModal);
                if (focusable.length === 0) return;

                const first = focusable[0];
                const last = focusable[focusable.length - 1];

                if (e.shiftKey) {
                    if (document.activeElement === first) {
                        e.preventDefault();
                        last.focus();
                    }
                } else {
                    if (document.activeElement === last) {
                        e.preventDefault();
                        first.focus();
                    }
                }
            }
        });

        // Close button handlers
        document.querySelectorAll('[data-modal-close]').forEach(btn => {
            btn.addEventListener('click', () => {
                const modalId = btn.closest('[data-modal]')?.id;
                if (modalId) this.close(modalId);
            });
        });
    },
};

// Dropdown Management
const Dropdown = {
    /**
     * Initialize dropdown click handlers
     */
    init() {
        document.querySelectorAll('[data-dropdown-toggle]').forEach(toggle => {
            toggle.addEventListener('click', (e) => {
                e.stopPropagation();
                const targetId = toggle.dataset.dropdownToggle;
                const dropdown = document.getElementById(targetId);
                if (dropdown) {
                    dropdown.classList.toggle('hidden');
                }
            });
        });

        // Close dropdowns on outside click
        document.addEventListener('click', () => {
            document.querySelectorAll('[data-dropdown]:not(.hidden)').forEach(dropdown => {
                dropdown.classList.add('hidden');
            });
        });
    },
};

// Smooth Scroll
const SmoothScroll = {
    /**
     * Initialize smooth scroll for anchor links
     */
    init() {
        document.querySelectorAll('a[href^="#"]').forEach(anchor => {
            anchor.addEventListener('click', (e) => {
                const targetId = anchor.getAttribute('href');
                if (targetId === '#') return;

                const target = document.querySelector(targetId);
                if (target) {
                    e.preventDefault();
                    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
                }
            });
        });
    },
};

// Weather UI Module
const WeatherUI = {
    container: null,
    loadingEl: null,
    contentEl: null,
    errorEl: null,
    unavailableEl: null,
    lat: null,
    lng: null,
    fallbackLat: null,
    fallbackLng: null,

    /**
     * Initialize weather UI component.
     *
     * Location priority:
     * 1. User's current location (if permitted)
     * 2. Today's first activity coordinates
     * 3. Any activity with coordinates
     * 4. Default: Taipei 101
     *
     * @contract
     *   - pre: weatherSection element exists in DOM
     *   - post: Weather data is loaded and displayed
     *   - calls: WeatherUI#tryGeolocation, WeatherUI#loadWeather
     */
    init() {
        const weatherSection = document.getElementById('weather-section');
        if (!weatherSection) return;

        this.container = weatherSection;
        this.loadingEl = document.getElementById('weather-loading');
        this.contentEl = document.getElementById('weather-content');
        this.errorEl = document.getElementById('weather-error');
        this.unavailableEl = document.getElementById('weather-unavailable');

        // Store fallback coordinates from server
        this.fallbackLat = weatherSection.dataset.fallbackLat;
        this.fallbackLng = weatherSection.dataset.fallbackLng;

        // Try to get user's current location first
        this.tryGeolocation();
    },

    /**
     * Try to get user's current geolocation.
     * Falls back to server-provided coordinates if denied or failed.
     *
     * @contract
     *   - post: Sets lat/lng and calls loadWeather
     */
    tryGeolocation() {
        // Check if geolocation is supported
        if (!navigator.geolocation) {
            this.useFallbackLocation();
            return;
        }

        // Try to get current position
        navigator.geolocation.getCurrentPosition(
            // Success callback
            (position) => {
                this.lat = position.coords.latitude;
                this.lng = position.coords.longitude;
                this.loadWeather();
            },
            // Error callback (denied or failed)
            (error) => {
                this.useFallbackLocation();
            },
            // Options
            {
                enableHighAccuracy: false,
                timeout: 5000,
                maximumAge: 300000 // 5 minutes cache
            }
        );
    },

    /**
     * Use fallback location from server.
     */
    useFallbackLocation() {
        this.lat = this.fallbackLat;
        this.lng = this.fallbackLng;

        if (this.lat && this.lng) {
            this.loadWeather();
        } else {
            // No fallback available, show error
            this.renderError('無法取得位置資訊');
        }
    },

    /**
     * Load weather forecast from API.
     *
     * @contract
     *   - pre: lat and lng are set
     *   - post: Weather data is fetched and rendered or error is shown
     *   - calls: /api/weather/forecast
     */
    async loadWeather() {
        try {
            const response = await fetch(`/api/weather/forecast?lat=${this.lat}&lng=${this.lng}`);
            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.message || 'Failed to load weather');
            }

            if (data.success && data.data && data.data.forecasts && data.data.forecasts.length > 0) {
                this.renderForecast(data.data.forecasts);
            } else if (data.data && !data.data.available) {
                this.showUnavailable();
            } else {
                this.renderError('無法取得天氣資料');
            }
        } catch (error) {
            this.renderError('天氣資訊暫時無法取得');
        }
    },

    /**
     * Render weather forecast cards.
     *
     * @contract
     *   - pre: forecasts is an array of forecast objects
     *   - post: Forecast cards are rendered in the content container
     *
     * @param {Array} forecasts - Array of forecast objects
     */
    renderForecast(forecasts) {
        this.hideLoading();

        // Build forecast cards HTML
        const cardsHtml = forecasts.map(forecast => this.buildForecastCard(forecast)).join('');

        // Horizontal scroll on mobile, auto-fit on desktop
        this.contentEl.innerHTML = `
            <div class="flex gap-3 overflow-x-auto sm:overflow-visible pb-2 sm:pb-0 scrollbar-hide"
                 style="-webkit-overflow-scrolling: touch;">
                ${cardsHtml}
            </div>
        `;

        this.contentEl.classList.remove('hidden');
    },

    /**
     * Build HTML for a single forecast card.
     *
     * @contract
     *   - pre: forecast object with date, icon, tempHigh, tempLow, rainProbability
     *   - post: Returns HTML string for the forecast card
     *
     * @param {Object} forecast - Single forecast object
     * @returns {string} HTML string
     */
    buildForecastCard(forecast) {
        const date = new Date(forecast.date);
        const weekday = this.getWeekdayName(date);
        const monthDay = `${date.getMonth() + 1}/${date.getDate()}`;
        const iconUrl = this.getWeatherIconUrl(forecast.icon);
        const tempHigh = Math.round(forecast.tempHigh);
        const tempLow = Math.round(forecast.tempLow);
        const rainPercent = Math.round(forecast.rainProbability * 100);
        const showRain = rainPercent >= 30;

        // Responsive card: fixed width on mobile, equal-width fill on desktop
        // Uses .weather-card component class defined in input.css
        return `
            <div class="weather-card">
                <div class="text-xs text-gray-500 dark:text-gray-400 mb-0.5">${monthDay}</div>
                <div class="text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">${weekday}</div>
                <img src="${iconUrl}"
                     alt="${forecast.description || forecast.condition}"
                     class="w-10 h-10 mx-auto"
                     loading="lazy"/>
                <div class="mt-1">
                    <span class="text-sm font-semibold text-gray-800 dark:text-gray-100">${tempHigh}°</span>
                    <span class="text-xs text-gray-400 dark:text-gray-500"> / </span>
                    <span class="text-xs text-gray-500 dark:text-gray-400">${tempLow}°</span>
                </div>
                ${showRain ? `
                    <div class="mt-1 flex items-center justify-center gap-0.5 text-xs text-info dark:text-info">
                        <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20" aria-hidden="true">
                            <path fill-rule="evenodd"
                                  d="M5.5 17a4.5 4.5 0 01-1.44-8.765 4.5 4.5 0 018.302-3.046 3.5 3.5 0 014.504 4.272A4 4 0 0115 17H5.5zm3.75-2.75a.75.75 0 001.5 0V9.66l1.95 2.1a.75.75 0 101.1-1.02l-3.25-3.5a.75.75 0 00-1.1 0l-3.25 3.5a.75.75 0 101.1 1.02l1.95-2.1v4.59z"
                                  clip-rule="evenodd"/>
                        </svg>
                        <span>${rainPercent}%</span>
                    </div>
                ` : ''}
            </div>
        `;
    },

    /**
     * Get Chinese weekday name from date.
     *
     * @param {Date} date - Date object
     * @returns {string} Chinese weekday name
     */
    getWeekdayName(date) {
        const weekdays = ['日', '一', '二', '三', '四', '五', '六'];
        return `週${weekdays[date.getDay()]}`;
    },

    /**
     * Get OpenWeatherMap icon URL.
     *
     * @param {string} iconCode - Weather icon code (e.g., "01d", "10n")
     * @returns {string} Full URL to weather icon
     */
    getWeatherIconUrl(iconCode) {
        if (!iconCode) return '/images/weather-unknown.png';
        return `https://openweathermap.org/img/wn/${iconCode}@2x.png`;
    },

    /**
     * Hide loading skeleton.
     */
    hideLoading() {
        if (this.loadingEl) {
            this.loadingEl.classList.add('hidden');
        }
    },

    /**
     * Show error state.
     *
     * @param {string} message - Error message to display
     */
    renderError(message) {
        this.hideLoading();

        const errorMessageEl = document.getElementById('weather-error-message');
        if (errorMessageEl) {
            errorMessageEl.textContent = message;
        }

        if (this.errorEl) {
            this.errorEl.classList.remove('hidden');
        }
    },

    /**
     * Show unavailable state (for dates > 5 days).
     */
    showUnavailable() {
        this.hideLoading();

        if (this.unavailableEl) {
            this.unavailableEl.classList.remove('hidden');
        }
    }
};

// Cover Image Preview Module
const CoverImagePreview = {
    /**
     * Initialize cover image preview functionality.
     *
     * @contract
     *   - pre: Elements with IDs 'coverImage', 'preview-image', 'upload-placeholder' may exist
     *   - post: Event listener attached if elements found
     */
    init() {
        const coverInput = document.getElementById('coverImage');
        const previewImage = document.getElementById('preview-image');
        const placeholder = document.getElementById('upload-placeholder');

        if (!coverInput || !previewImage || !placeholder) {
            return; // Not on a page with cover image upload
        }

        coverInput.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (!file) return;

            // Validate file size (max 5MB)
            const maxSize = 5 * 1024 * 1024;
            if (file.size > maxSize) {
                Toast.error('封面圖片大小不可超過 5MB');
                coverInput.value = '';
                return;
            }

            // Validate file type
            const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
            if (!allowedTypes.includes(file.type)) {
                Toast.error('僅支援 JPEG、PNG、WebP 格式');
                coverInput.value = '';
                return;
            }

            // Revoke previous object URL to prevent memory leak
            if (previewImage.src && previewImage.src.startsWith('blob:')) {
                URL.revokeObjectURL(previewImage.src);
            }

            // Create preview using URL.createObjectURL
            const objectUrl = URL.createObjectURL(file);
            previewImage.src = objectUrl;
            previewImage.style.display = 'block';
            previewImage.classList.remove('hidden');
            placeholder.style.display = 'none';
            placeholder.classList.add('hidden');
        });
    }
};

// Trip Create Form Module
const TripForm = {
    /**
     * Initialize trip create/edit form functionality.
     *
     * @contract
     *   - pre: Form elements may exist on page
     *   - post: Event listeners attached for form validation
     */
    init() {
        // Description character count
        const descTextarea = document.getElementById('description');
        const descCount = document.getElementById('desc-count');
        if (descTextarea && descCount) {
            descCount.textContent = descTextarea.value.length;
            descTextarea.addEventListener('input', () => {
                descCount.textContent = descTextarea.value.length;
            });
        }

    }
};

// Custom Time Picker
const TimePicker = {
    activePopup: null,
    activeInput: null,
    activeDisplay: null,

    init() {
        document.querySelectorAll('[data-timepicker]').forEach(el => {
            if (el.dataset.timepickerInit) return;
            el.dataset.timepickerInit = 'true';
            this.attachToInput(el);
        });
        document.addEventListener('click', (e) => {
            if (this.activePopup && !this.activePopup.contains(e.target) && e.target !== this.activeDisplay) {
                this.close();
            }
        });
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && this.activePopup) this.close();
        });
    },

    attachToInput(hiddenInput) {
        hiddenInput.type = 'hidden';

        const display = document.createElement('input');
        display.type = 'text';
        display.readOnly = true;
        display.placeholder = hiddenInput.placeholder || '選擇時間';
        // Copy classes but ensure cursor-pointer
        const classes = hiddenInput.className.replace('cursor-pointer', '').trim();
        display.className = classes + ' cursor-pointer';
        hiddenInput.parentNode.insertBefore(display, hiddenInput.nextSibling);

        if (hiddenInput.value) {
            display.value = this.format24to12(hiddenInput.value);
        }

        display.addEventListener('click', (e) => {
            e.stopPropagation();
            this.open(hiddenInput, display);
        });
    },

    open(hiddenInput, displayInput) {
        this.close();
        this.activeInput = hiddenInput;
        this.activeDisplay = displayInput;

        const { hour, minute, period } = hiddenInput.value
            ? this.parse24(hiddenInput.value)
            : { hour: 9, minute: 0, period: 'AM' };

        const popup = this.createPopup(hour, minute, period);
        document.body.appendChild(popup);
        this.positionPopup(popup, displayInput.getBoundingClientRect());
        this.activePopup = popup;
    },

    close() {
        if (this.activePopup) {
            this.activePopup.remove();
            this.activePopup = null;
        }
        this.activeInput = null;
        this.activeDisplay = null;
    },

    apply() {
        if (!this.activePopup || !this.activeInput) return;
        const h12 = parseInt(this.activePopup.querySelector('[data-hour]').value);
        const m = parseInt(this.activePopup.querySelector('[data-minute]').value);
        const activeBtn = this.activePopup.querySelector('[data-period-active]');
        const period = activeBtn ? activeBtn.dataset.period : 'AM';

        const h24 = this.to24(h12, period);
        this.activeInput.value = `${String(h24).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
        this.activeDisplay.value = this.format12(h12, m, period);
        this.activeInput.dispatchEvent(new Event('change', { bubbles: true }));
        this.close();
    },

    createPopup(hour, minute, period) {
        const popup = document.createElement('div');
        popup.className = 'timepicker-popup bg-white dark:bg-gray-800 rounded-2xl shadow-xl border border-gray-200 dark:border-gray-700 p-5 w-64 animate-scale-in';
        popup.style.position = 'fixed';
        popup.style.zIndex = '9999';

        // Title
        const title = document.createElement('div');
        title.className = 'text-sm font-semibold text-gray-800 dark:text-gray-100 mb-4';
        title.textContent = '選擇時間';
        popup.appendChild(title);

        // Hour : Minute row
        const timeRow = document.createElement('div');
        timeRow.className = 'flex items-center justify-center gap-2 mb-4';

        // Hour select
        const hourSelect = document.createElement('select');
        hourSelect.dataset.hour = '';
        hourSelect.className = 'timepicker-select bg-primary-50 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400 font-semibold text-lg rounded-xl px-3 py-2 border-none cursor-pointer w-16';
        for (let h = 1; h <= 12; h++) {
            const opt = document.createElement('option');
            opt.value = h;
            opt.textContent = h;
            if (h === hour) opt.selected = true;
            hourSelect.appendChild(opt);
        }
        timeRow.appendChild(hourSelect);

        // Colon
        const colon = document.createElement('span');
        colon.className = 'text-lg font-bold text-gray-400 dark:text-gray-500';
        colon.textContent = ':';
        timeRow.appendChild(colon);

        // Minute select
        const minuteSelect = document.createElement('select');
        minuteSelect.dataset.minute = '';
        minuteSelect.className = 'timepicker-select bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-100 font-semibold text-lg rounded-xl px-3 py-2 border-none cursor-pointer w-16';
        for (let m = 0; m < 60; m += 5) {
            const opt = document.createElement('option');
            opt.value = m;
            opt.textContent = String(m).padStart(2, '0');
            if (m === minute) opt.selected = true;
            minuteSelect.appendChild(opt);
        }
        // If minute is not a multiple of 5, add it as an option
        if (minute % 5 !== 0) {
            const opt = document.createElement('option');
            opt.value = minute;
            opt.textContent = String(minute).padStart(2, '0');
            opt.selected = true;
            minuteSelect.appendChild(opt);
        }
        timeRow.appendChild(minuteSelect);

        popup.appendChild(timeRow);

        // AM/PM toggle
        const periodRow = document.createElement('div');
        periodRow.className = 'flex justify-center gap-3 mb-5';

        const createPeriodBtn = (label, value, isActive) => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.dataset.period = value;
            if (isActive) btn.dataset.periodActive = '';
            btn.className = isActive
                ? 'text-sm font-bold text-primary-600 dark:text-primary-400 px-3 py-1.5 rounded-lg bg-primary-50 dark:bg-primary-900/20 cursor-pointer transition-colors'
                : 'text-sm font-normal text-gray-400 dark:text-gray-500 px-3 py-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 cursor-pointer transition-colors';
            btn.textContent = label;
            btn.addEventListener('click', () => {
                periodRow.querySelectorAll('[data-period]').forEach(b => {
                    const active = b === btn;
                    b.className = active
                        ? 'text-sm font-bold text-primary-600 dark:text-primary-400 px-3 py-1.5 rounded-lg bg-primary-50 dark:bg-primary-900/20 cursor-pointer transition-colors'
                        : 'text-sm font-normal text-gray-400 dark:text-gray-500 px-3 py-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 cursor-pointer transition-colors';
                    if (active) b.dataset.periodActive = '';
                    else delete b.dataset.periodActive;
                });
            });
            return btn;
        };

        periodRow.appendChild(createPeriodBtn('上午', 'AM', period === 'AM'));
        periodRow.appendChild(createPeriodBtn('下午', 'PM', period === 'PM'));
        popup.appendChild(periodRow);

        // Action buttons
        const actions = document.createElement('div');
        actions.className = 'flex justify-between items-center';

        const cancelBtn = document.createElement('button');
        cancelBtn.type = 'button';
        cancelBtn.className = 'text-sm text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 font-medium px-3 py-1.5 cursor-pointer transition-colors';
        cancelBtn.textContent = '取消';
        cancelBtn.addEventListener('click', () => this.close());
        actions.appendChild(cancelBtn);

        const applyBtn = document.createElement('button');
        applyBtn.type = 'button';
        applyBtn.className = 'text-sm text-white bg-primary-500 hover:bg-primary-600 font-semibold px-5 py-2 rounded-full cursor-pointer transition-colors';
        applyBtn.textContent = '套用';
        applyBtn.addEventListener('click', () => this.apply());
        actions.appendChild(applyBtn);

        popup.appendChild(actions);
        return popup;
    },

    positionPopup(popup, rect) {
        const popupH = popup.offsetHeight;
        const popupW = popup.offsetWidth;

        let top = rect.bottom + 8;
        let left = rect.left + (rect.width / 2) - (popupW / 2);

        if (top + popupH > window.innerHeight - 16) {
            top = rect.top - popupH - 8;
        }
        left = Math.max(8, Math.min(left, window.innerWidth - popupW - 8));

        popup.style.top = `${top}px`;
        popup.style.left = `${left}px`;
    },

    // === Conversion helpers ===
    parse24(val) {
        const [h, m] = val.split(':').map(Number);
        const period = h >= 12 ? 'PM' : 'AM';
        let hour12 = h % 12;
        if (hour12 === 0) hour12 = 12;
        return { hour: hour12, minute: m, period };
    },

    to24(h12, period) {
        if (period === 'AM') return h12 === 12 ? 0 : h12;
        return h12 === 12 ? 12 : h12 + 12;
    },

    format12(h12, m, period) {
        const label = period === 'AM' ? '上午' : '下午';
        return `${label} ${h12}:${String(m).padStart(2, '0')}`;
    },

    format24to12(val) {
        const { hour, minute, period } = this.parse24(val);
        return this.format12(hour, minute, period);
    },
};

// Flatpickr Date Picker Initialization
const DatePicker = {
    instances: [],

    init() {
        if (typeof flatpickr === 'undefined') return;

        // 全域設定繁體中文
        if (flatpickr.l10ns && flatpickr.l10ns.zh_tw) {
            flatpickr.localize(flatpickr.l10ns.zh_tw);
        }

        this.initLinkedDateRange();
        this.initDateInputs();
        this.initTimeInputs();
    },

    /** 單一日期選取：expense date, todo due date */
    initDateInputs() {
        document.querySelectorAll('[data-datepicker]:not([data-datepicker="trip-start"]):not([data-datepicker="trip-end"])').forEach(el => {
            if (el._flatpickr) return;

            const minDate = el.dataset.minDate || undefined;
            const maxDate = el.dataset.maxDate || undefined;

            const fp = flatpickr(el, {
                dateFormat: 'Y-m-d',
                altInput: true,
                altFormat: 'Y年m月d日',
                allowInput: false,
                disableMobile: true,
                defaultDate: el.value || null,
                minDate: minDate || undefined,
                maxDate: maxDate || undefined,
                onChange: () => this.syncDateChips(el),
            });
            this.instances.push(fp);
        });

        this.initDateChips();
    },

    /** 格式化本地日期為 YYYY-MM-DD（避免 toISOString 的 UTC 時區偏移問題） */
    formatLocalDate(d) {
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${y}-${m}-${day}`;
    },

    /** 快速日期選擇 chips */
    initDateChips() {
        const container = document.getElementById('date-chips');
        if (!container) return;

        const dateInput = document.getElementById('expenseDate');
        if (!dateInput) return;

        const minDate = dateInput.dataset.minDate || '';
        const maxDate = dateInput.dataset.maxDate || '';

        // Generate trip day chips if trip <= 14 days
        if (minDate && maxDate) {
            const start = new Date(minDate + 'T00:00:00');
            const end = new Date(maxDate + 'T00:00:00');
            const diffDays = Math.round((end - start) / (1000 * 60 * 60 * 24)) + 1;

            if (diffDays > 0 && diffDays <= 14) {
                for (let i = 0; i < diffDays; i++) {
                    const d = new Date(start);
                    d.setDate(d.getDate() + i);
                    const dateStr = this.formatLocalDate(d);
                    const label = `${d.getMonth() + 1}/${d.getDate()}`;

                    const chip = document.createElement('button');
                    chip.type = 'button';
                    chip.className = 'date-chip min-h-[44px] px-3 py-2 rounded-xl text-sm font-medium border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:border-primary-400 hover:bg-primary-50 dark:hover:bg-primary-900/20 transition-all duration-200 cursor-pointer';
                    chip.dataset.dateValue = dateStr;
                    chip.textContent = `Day ${i + 1} (${label})`;
                    container.appendChild(chip);
                }
            }
        }

        // Hide out-of-range offset chips (today/yesterday)
        container.querySelectorAll('[data-date-offset]').forEach(chip => {
            const offset = parseInt(chip.dataset.dateOffset, 10);
            if (isNaN(offset)) return;
            const d = new Date();
            d.setDate(d.getDate() + offset);
            const dateStr = this.formatLocalDate(d);
            chip.dataset.dateValue = dateStr;

            if ((minDate && dateStr < minDate) || (maxDate && dateStr > maxDate)) {
                chip.classList.add('hidden');
            }
        });

        // Click handler for all chips
        container.addEventListener('click', (e) => {
            const chip = e.target.closest('.date-chip');
            if (!chip || !chip.dataset.dateValue) return;

            const fp = dateInput._flatpickr;
            if (fp) {
                fp.setDate(chip.dataset.dateValue, true);
            }
            this.syncDateChips(dateInput);
        });

        // Initial sync
        this.syncDateChips(dateInput);
    },

    /** 同步 chip 高亮狀態 */
    syncDateChips(dateInput) {
        const container = document.getElementById('date-chips');
        if (!container) return;

        const currentVal = dateInput.value;
        container.querySelectorAll('.date-chip').forEach(chip => {
            const isActive = chip.dataset.dateValue === currentVal;
            chip.classList.toggle('border-primary-500', isActive);
            chip.classList.toggle('bg-primary-50', isActive);
            chip.classList.toggle('dark:bg-primary-900/30', isActive);
            chip.classList.toggle('text-primary-700', isActive);
            chip.classList.toggle('dark:text-primary-300', isActive);
            chip.classList.toggle('border-gray-200', !isActive);
            chip.classList.toggle('dark:border-gray-700', !isActive);
            chip.classList.toggle('bg-white', !isActive);
            chip.classList.toggle('dark:bg-gray-800', !isActive);
            chip.classList.toggle('text-gray-700', !isActive);
            chip.classList.toggle('dark:text-gray-300', !isActive);
        });
    },

    /** 時間選取：委派給 TimePicker 模組 */
    initTimeInputs() {
        TimePicker.init();
    },

    /** Trip 開始/結束日期連動 */
    initLinkedDateRange() {
        const startEl = document.querySelector('[data-datepicker="trip-start"]');
        const endEl = document.querySelector('[data-datepicker="trip-end"]');
        if (!startEl || !endEl) return;
        if (startEl._flatpickr) return;

        const today = new Date().toISOString().split('T')[0];

        const endPicker = flatpickr(endEl, {
            dateFormat: 'Y-m-d',
            altInput: true,
            altFormat: 'Y年m月d日',
            allowInput: true,
            disableMobile: true,
            minDate: startEl.value || today,
            defaultDate: endEl.value || null,
        });

        const startPicker = flatpickr(startEl, {
            dateFormat: 'Y-m-d',
            altInput: true,
            altFormat: 'Y年m月d日',
            allowInput: true,
            disableMobile: true,
            minDate: today,
            defaultDate: startEl.value || null,
            onChange(selectedDates) {
                if (selectedDates.length > 0) {
                    endPicker.set('minDate', selectedDates[0]);
                    const endDate = endPicker.selectedDates[0];
                    if (endDate && endDate < selectedDates[0]) {
                        endPicker.setDate(selectedDates[0]);
                    }
                }
            },
        });

        this.instances.push(startPicker, endPicker);
    },

    destroy() {
        this.instances.forEach(fp => fp.destroy());
        this.instances = [];
    },
};

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    Toast.init();
    DarkMode.init();
    Modal.init();
    Dropdown.init();
    SmoothScroll.init();
    WeatherUI.init();
    CoverImagePreview.init();
    TripForm.init();
    DatePicker.init();

    // Stagger-in animation for card grids
    if (typeof WeGo !== 'undefined' && WeGo.anime) {
        document.body.classList.add('js-animated');
        WeGo.anime.staggerIn('.stagger-item');
    }

    // Scroll-triggered reveal for sections
    if (typeof WeGo !== 'undefined' && WeGo.anime && WeGo.anime.onVisible) {
        WeGo.anime.onVisible('.scroll-reveal', function(el) {
            el.classList.add('is-visible');
        }, { threshold: 0.15 });
    }

    // Auto-init form validation (must run after DOM is ready)
    FormValidation.autoInit();

    // Expose to global scope for inline handlers
    window.Toast = Toast;
    window.DarkMode = DarkMode;
    window.Loading = Loading;
    window.FormValidation = FormValidation;
    window.DatePicker = DatePicker;
    window.TimePicker = TimePicker;
    window.Modal = Modal;
    window.WeatherUI = WeatherUI;
    window.CoverImagePreview = CoverImagePreview;
    window.TripForm = TripForm;
});

// Handle page visibility for animations
document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
        // Re-enable animations when page becomes visible
        document.body.style.setProperty('--animation-play-state', 'running');
    }
});

// Reduce motion for accessibility
if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    document.documentElement.style.setProperty('--animation-duration', '0.01ms');
}
