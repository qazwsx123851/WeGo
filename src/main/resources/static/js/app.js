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

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    Toast.init();
    DarkMode.init();
    Modal.init();
    Dropdown.init();
    SmoothScroll.init();

    // Expose to global scope for inline handlers
    window.Toast = Toast;
    window.DarkMode = DarkMode;
    window.Loading = Loading;
    window.FormValidation = FormValidation;
    window.Modal = Modal;
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
