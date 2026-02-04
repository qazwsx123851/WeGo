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
     */
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
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
     * Show error message for an input field
     */
    showError(input, message) {
        const container = input.closest('.form-group') || input.parentNode;
        this.clearError(input);

        input.classList.add('border-error', 'focus:ring-error');
        input.classList.remove('border-gray-200', 'focus:ring-primary-500');

        const errorEl = document.createElement('p');
        errorEl.className = 'text-caption text-error mt-1 form-error';
        errorEl.textContent = message;
        container.appendChild(errorEl);
    },

    /**
     * Clear error message for an input field
     */
    clearError(input) {
        const container = input.closest('.form-group') || input.parentNode;
        const errorEl = container.querySelector('.form-error');
        if (errorEl) errorEl.remove();

        input.classList.remove('border-error', 'focus:ring-error');
        input.classList.add('border-gray-200', 'focus:ring-primary-500');
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
};

// Modal Management
const Modal = {
    /**
     * Open a modal by ID
     */
    open(modalId) {
        const modal = document.getElementById(modalId);
        if (!modal) return;

        modal.classList.remove('hidden');
        modal.classList.add('flex');
        document.body.classList.add('overflow-hidden');

        // Focus trap
        const focusable = modal.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])');
        if (focusable.length) focusable[0].focus();
    },

    /**
     * Close a modal by ID
     */
    close(modalId) {
        const modal = document.getElementById(modalId);
        if (!modal) return;

        modal.classList.add('hidden');
        modal.classList.remove('flex');
        document.body.classList.remove('overflow-hidden');
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

        // Close on Escape key
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                const openModal = document.querySelector('[data-modal]:not(.hidden)');
                if (openModal) this.close(openModal.id);
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
        console.log('[Weather] Starting geolocation request...');
        console.log('[Weather] Fallback coordinates:', this.fallbackLat, this.fallbackLng);

        // Check if geolocation is supported
        if (!navigator.geolocation) {
            console.log('[Weather] Geolocation not supported, using fallback');
            this.useFallbackLocation();
            return;
        }

        // Try to get current position
        navigator.geolocation.getCurrentPosition(
            // Success callback
            (position) => {
                this.lat = position.coords.latitude;
                this.lng = position.coords.longitude;
                console.log('[Weather] ✅ User allowed geolocation');
                console.log('[Weather] Using USER location:', this.lat, this.lng);
                this.loadWeather();
            },
            // Error callback (denied or failed)
            (error) => {
                console.log('[Weather] ❌ Geolocation denied or failed:', error.code, error.message);
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
            console.log('[Weather] Using FALLBACK location:', this.lat, this.lng);
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
            console.error('Weather load error:', error);
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

        // Date validation
        const startDate = document.getElementById('startDate');
        const endDate = document.getElementById('endDate');
        if (startDate && endDate) {
            startDate.addEventListener('change', () => {
                endDate.min = startDate.value;
                if (endDate.value && endDate.value < startDate.value) {
                    endDate.value = startDate.value;
                }
            });

            // Set min date to today
            const today = new Date().toISOString().split('T')[0];
            startDate.min = today;
        }
    }
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

    // Expose to global scope for inline handlers
    window.Toast = Toast;
    window.DarkMode = DarkMode;
    window.Loading = Loading;
    window.FormValidation = FormValidation;
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
