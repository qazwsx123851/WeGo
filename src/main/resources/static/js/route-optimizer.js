/**
 * Route Optimization JavaScript Module
 *
 * Handles the route optimization UI for reordering activities
 * based on optimized route calculations.
 *
 * @contract
 *   - pre: window.activityData must be available with tripId and activities
 *   - pre: Toast utility from app.js must be initialized
 *   - post: Provides API integration and UI state management for route optimization
 *   - calls: /api/trips/{tripId}/activities/optimize, /api/trips/{tripId}/activities/apply-optimization
 */

/**
 * API wrapper for route optimization endpoints.
 */
const RouteOptimizationApi = {
    /**
     * Gets CSRF token from meta tag.
     * @returns {string} CSRF token
     * @throws {Error} if CSRF token is not found
     */
    getCsrfToken() {
        const token = document.querySelector('meta[name="_csrf"]');
        if (!token || !token.getAttribute('content')) {
            throw new Error('CSRF token not found');
        }
        return token.getAttribute('content');
    },

    /**
     * Gets CSRF header name from meta tag.
     * @returns {string} CSRF header name (default: X-CSRF-TOKEN)
     */
    getCsrfHeader() {
        const header = document.querySelector('meta[name="_csrf_header"]');
        return header ? header.getAttribute('content') : 'X-CSRF-TOKEN';
    },

    /**
     * Fetches route optimization suggestion from the server.
     *
     * @contract
     *   - pre: tripId is valid UUID
     *   - pre: day >= 1
     *   - post: returns { success, data, message } or throws error
     *   - calls: GET /api/trips/{tripId}/activities/optimize?day={day}
     *
     * @param {string} tripId - The trip ID
     * @param {number} day - The day number to optimize
     * @returns {Promise<Object>} The optimization result
     */
    async getOptimization(tripId, day) {
        const response = await fetch(`/api/trips/${tripId}/activities/optimize?day=${day}`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json',
            },
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `HTTP ${response.status}`);
        }

        return response.json();
    },

    /**
     * Applies the optimized route order to activities.
     *
     * @contract
     *   - pre: tripId is valid UUID
     *   - pre: day >= 1
     *   - pre: activityOrder is non-empty array of activity UUIDs
     *   - post: returns { success, data, message } or throws error
     *   - calls: POST /api/trips/{tripId}/activities/apply-optimization
     *
     * @param {string} tripId - The trip ID
     * @param {number} day - The day number
     * @param {string[]} activityOrder - Array of activity IDs in optimized order
     * @returns {Promise<Object>} The result with reordered activities
     */
    async applyOptimization(tripId, day, activityOrder) {
        const response = await fetch(`/api/trips/${tripId}/activities/apply-optimization`, {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
                [this.getCsrfHeader()]: this.getCsrfToken(),
            },
            credentials: 'same-origin',
            body: JSON.stringify({
                day: day,
                optimizedOrder: activityOrder,
            }),
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `HTTP ${response.status}`);
        }

        return response.json();
    },
};

/**
 * UI controller for the route optimization modal.
 */
const RouteOptimizationUI = {
    modal: null,
    tripId: null,
    day: null,
    result: null,
    activities: [],

    // DOM element references
    elements: {
        loading: null,
        result: null,
        noImprovement: null,
        error: null,
        footerLoading: null,
        footerResult: null,
        footerClose: null,
        footerRetry: null,
        distanceSaved: null,
        savingsPercentage: null,
        suggestedOrderList: null,
        applyBtn: null,
        applyBtnText: null,
        applyBtnSpinner: null,
        retryBtn: null,
        errorMessage: null,
    },

    /**
     * Initializes the route optimization UI.
     *
     * @contract
     *   - pre: DOM is fully loaded
     *   - post: All element references are set
     *   - post: Event listeners are attached
     *
     * @param {string} tripId - The trip ID
     * @param {number} day - The day number
     */
    init(tripId, day) {
        this.tripId = tripId;
        this.day = day;
        this.modal = document.getElementById('optimization-modal');

        if (!this.modal) {
            return;
        }

        // Cache DOM elements
        this.elements = {
            loading: document.getElementById('optimization-loading'),
            result: document.getElementById('optimization-result'),
            noImprovement: document.getElementById('optimization-no-improvement'),
            error: document.getElementById('optimization-error'),
            footerLoading: document.getElementById('footer-loading'),
            footerResult: document.getElementById('footer-result'),
            footerClose: document.getElementById('footer-close'),
            footerRetry: document.getElementById('footer-retry'),
            distanceSaved: document.getElementById('distance-saved'),
            savingsPercentage: document.getElementById('savings-percentage'),
            suggestedOrderList: document.getElementById('suggested-order-list'),
            applyBtn: document.getElementById('apply-optimization-btn'),
            applyBtnText: document.getElementById('apply-btn-text'),
            applyBtnSpinner: document.getElementById('apply-btn-spinner'),
            retryBtn: document.getElementById('retry-optimization-btn'),
            errorMessage: document.getElementById('error-message'),
        };

        // Load activities from window.activityData
        if (window.activityData && window.activityData.activities) {
            this.activities = window.activityData.activities;
        }

        // Set up event listeners
        this.setupEventListeners();
    },

    /**
     * Sets up event listeners for modal interactions.
     */
    setupEventListeners() {
        // Apply optimization button
        if (this.elements.applyBtn) {
            this.elements.applyBtn.addEventListener('click', () => {
                this.applyOptimization();
            });
        }

        // Retry button
        if (this.elements.retryBtn) {
            this.elements.retryBtn.addEventListener('click', () => {
                this.showOptimization();
            });
        }

        // Close button handlers - handled by Modal utility in app.js
    },

    /**
     * Shows the optimization modal and fetches optimization data.
     */
    async showOptimization() {
        if (!this.modal) {
            return;
        }

        // Open modal and show loading state
        Modal.open('optimization-modal');
        this.showLoadingState();

        try {
            const response = await RouteOptimizationApi.getOptimization(this.tripId, this.day);

            if (response.success && response.data) {
                this.result = response.data;

                if (response.data.optimizationApplied && response.data.savingsPercentage > 0) {
                    this.showResultState(response.data);
                } else {
                    this.showNoImprovementState();
                }
            } else {
                this.showErrorState(response.message || '無法取得優化結果');
            }
        } catch (error) {
            this.showErrorState(error.message || '發生未預期的錯誤');
        }
    },

    /**
     * Displays the loading state in the modal.
     */
    showLoadingState() {
        this.hideAllStates();
        this.elements.loading.classList.remove('hidden');
        this.elements.footerLoading.classList.remove('hidden');
    },

    /**
     * Displays the optimization result in the modal.
     *
     * @param {Object} result - The optimization result data
     */
    showResultState(result) {
        this.hideAllStates();

        // Update distance saved
        this.elements.distanceSaved.textContent = result.distanceSavedFormatted || '0 m';

        // Update savings percentage
        const percentage = Math.round(result.savingsPercentage * 10) / 10;
        this.elements.savingsPercentage.textContent = percentage;

        // Build suggested order list
        this.renderSuggestedOrder(result.optimizedOrder);

        // Show result state
        this.elements.result.classList.remove('hidden');
        this.elements.footerResult.classList.remove('hidden');
        this.elements.footerResult.classList.add('flex');
    },

    /**
     * Renders the suggested activity order as a numbered list.
     *
     * @param {string[]} optimizedOrder - Array of activity IDs in optimized order
     */
    renderSuggestedOrder(optimizedOrder) {
        const list = this.elements.suggestedOrderList;
        list.innerHTML = '';

        optimizedOrder.forEach((activityId, index) => {
            const activity = this.findActivityById(activityId);
            // Access place.name from ActivityResponse structure
            const placeName = activity?.place?.name || '未知景點';

            const li = document.createElement('li');
            li.className = 'flex items-center gap-3 p-2 bg-gray-50 dark:bg-gray-700/50 rounded-lg';
            li.innerHTML = `
                <span class="w-6 h-6 rounded-full bg-primary-100 dark:bg-primary-900/30
                             text-primary-600 dark:text-primary-400
                             flex items-center justify-center text-xs font-semibold flex-shrink-0">
                    ${index + 1}
                </span>
                <span class="text-sm text-gray-700 dark:text-gray-200 truncate">${this.escapeHtml(placeName)}</span>
            `;
            list.appendChild(li);
        });
    },

    /**
     * Finds an activity by its ID from the loaded activities.
     *
     * @param {string} activityId - The activity ID to find
     * @returns {Object|null} The activity object or null
     */
    findActivityById(activityId) {
        // Handle both string and object ID comparison
        return this.activities.find(a => {
            const id = a.id?.toString() || a.id;
            return id === activityId || id === activityId?.toString();
        });
    },

    /**
     * Displays the no improvement state in the modal.
     */
    showNoImprovementState() {
        this.hideAllStates();
        this.elements.noImprovement.classList.remove('hidden');
        this.elements.footerClose.classList.remove('hidden');
    },

    /**
     * Displays the error state in the modal.
     *
     * @param {string} message - The error message to display
     */
    showErrorState(message) {
        this.hideAllStates();
        this.elements.errorMessage.textContent = message || '請稍後再試';
        this.elements.error.classList.remove('hidden');
        this.elements.footerRetry.classList.remove('hidden');
        this.elements.footerRetry.classList.add('flex');
    },

    /**
     * Hides all modal body and footer states.
     */
    hideAllStates() {
        // Hide body states
        this.elements.loading.classList.add('hidden');
        this.elements.result.classList.add('hidden');
        this.elements.noImprovement.classList.add('hidden');
        this.elements.error.classList.add('hidden');

        // Hide footer states
        this.elements.footerLoading.classList.add('hidden');
        this.elements.footerResult.classList.add('hidden');
        this.elements.footerResult.classList.remove('flex');
        this.elements.footerClose.classList.add('hidden');
        this.elements.footerRetry.classList.add('hidden');
        this.elements.footerRetry.classList.remove('flex');
    },

    /**
     * Applies the optimized route order.
     */
    async applyOptimization() {
        if (!this.result || !this.result.optimizedOrder) {
            Toast.error('沒有可套用的優化結果');
            return;
        }

        // Disable button and show spinner
        this.elements.applyBtn.disabled = true;
        this.elements.applyBtnText.textContent = '套用中...';
        this.elements.applyBtnSpinner.classList.remove('hidden');

        try {
            const response = await RouteOptimizationApi.applyOptimization(
                this.tripId,
                this.day,
                this.result.optimizedOrder
            );

            if (response.success) {
                Toast.success('路線已優化！');
                Modal.close('optimization-modal');

                // Reload page to show new order
                setTimeout(() => {
                    window.location.reload();
                }, 500);
            } else {
                Toast.error(response.message || '套用優化失敗');
                this.resetApplyButton();
            }
        } catch (error) {
            Toast.error(error.message || '套用優化時發生錯誤');
            this.resetApplyButton();
        }
    },

    /**
     * Resets the apply button to its default state.
     */
    resetApplyButton() {
        this.elements.applyBtn.disabled = false;
        this.elements.applyBtnText.textContent = '套用優化';
        this.elements.applyBtnSpinner.classList.add('hidden');
    },

    /**
     * Closes the optimization modal.
     */
    closeModal() {
        Modal.close('optimization-modal');
    },

    /**
     * Escapes HTML to prevent XSS.
     *
     * @param {string} text - The text to escape
     * @returns {string} The escaped text
     */
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    },
};

// Initialize on DOMContentLoaded
document.addEventListener('DOMContentLoaded', () => {
    const optimizeBtn = document.getElementById('optimize-route-btn');

    if (optimizeBtn) {
        const tripId = optimizeBtn.dataset.tripId;
        const day = parseInt(optimizeBtn.dataset.day, 10) || 1;

        RouteOptimizationUI.init(tripId, day);

        optimizeBtn.addEventListener('click', () => {
            RouteOptimizationUI.showOptimization();
        });
    }
});

// Expose to global scope for debugging
window.RouteOptimizationApi = RouteOptimizationApi;
window.RouteOptimizationUI = RouteOptimizationUI;
