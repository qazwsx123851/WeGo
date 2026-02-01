/**
 * WeGo - Drag and Drop Reorder Module
 *
 * @contract
 *   - Handles drag-and-drop reordering of activity cards
 *   - Supports both mouse and touch interactions
 *   - Calls API to persist order changes
 *   - calledBy: activity/list.html
 *   - calls: PUT /api/trips/{tripId}/activities/reorder
 */

const DragReorder = {
    // Configuration constants
    TOUCH_HOLD_DELAY: 150,         // Delay before starting touch drag (prevents accidental drags)
    TOUCH_CANCEL_THRESHOLD: 10,    // Pixels moved to cancel touch-hold
    TOUCH_THROTTLE_MS: 50,         // Throttle interval for touch move events

    // State
    draggedElement: null,
    draggedIndex: null,
    placeholder: null,
    touchStartY: null,
    touchCurrentY: null,
    isDragging: false,
    lastTouchMoveTime: 0,
    touchTimeout: null,
    originalOrder: [],             // Store for rollback on failure
    initializedContainers: new WeakSet(),

    /**
     * Initialize drag-and-drop functionality.
     *
     * @contract
     *   - pre: DOM is ready
     *   - post: Event listeners attached to sortable containers
     */
    init() {
        const sortableContainers = document.querySelectorAll('.sortable-activities');
        if (sortableContainers.length === 0) return;

        sortableContainers.forEach(container => {
            // Prevent duplicate initialization (memory leak prevention)
            if (!this.initializedContainers.has(container)) {
                this.initContainer(container);
                this.initializedContainers.add(container);
            }
        });
    },

    /**
     * Initialize a single sortable container.
     *
     * @param {HTMLElement} container - The sortable container element
     */
    initContainer(container) {
        const cards = container.querySelectorAll('.activity-card');

        cards.forEach((card, index) => {
            // Mouse drag events
            card.addEventListener('dragstart', (e) => this.handleDragStart(e, card, container));
            card.addEventListener('dragend', (e) => this.handleDragEnd(e, container));
            card.addEventListener('dragover', (e) => this.handleDragOver(e, card));
            card.addEventListener('dragleave', (e) => this.handleDragLeave(e, card));
            card.addEventListener('drop', (e) => this.handleDrop(e, card, container));

            // Touch events for mobile
            const dragHandle = card.querySelector('.drag-handle');
            if (dragHandle) {
                dragHandle.addEventListener('touchstart', (e) => this.handleTouchStart(e, card, container), { passive: false });
                dragHandle.addEventListener('touchmove', (e) => this.handleTouchMove(e, card, container), { passive: false });
                dragHandle.addEventListener('touchend', (e) => this.handleTouchEnd(e, container), { passive: false });
            }
        });

        // Container events
        container.addEventListener('dragover', (e) => e.preventDefault());
    },

    /**
     * Handle drag start event.
     *
     * @param {DragEvent} e - The drag event
     * @param {HTMLElement} card - The activity card
     * @param {HTMLElement} container - The sortable container
     */
    handleDragStart(e, card, container) {
        // Only allow drag from drag handle
        if (!e.target.closest('.drag-handle')) {
            e.preventDefault();
            return;
        }

        this.draggedElement = card;
        this.draggedIndex = this.getCardIndex(card, container);
        this.isDragging = true;

        // Store original order for potential rollback
        this.originalOrder = Array.from(container.querySelectorAll('.activity-card'));

        // Set drag image
        e.dataTransfer.effectAllowed = 'move';
        e.dataTransfer.setData('text/plain', card.dataset.activityId);

        // Add dragging styles after a short delay
        requestAnimationFrame(() => {
            card.classList.add('dragging');
        });
    },

    /**
     * Handle drag end event.
     *
     * @param {DragEvent} e - The drag event
     * @param {HTMLElement} container - The sortable container
     */
    handleDragEnd(e, container) {
        if (!this.draggedElement) return;

        this.draggedElement.classList.remove('dragging');
        this.removePlaceholder();
        this.removeDropIndicators(container);

        this.draggedElement = null;
        this.draggedIndex = null;
        this.isDragging = false;
    },

    /**
     * Handle drag over event.
     *
     * @param {DragEvent} e - The drag event
     * @param {HTMLElement} card - The activity card being dragged over
     */
    handleDragOver(e, card) {
        e.preventDefault();
        if (!this.draggedElement || card === this.draggedElement) return;

        e.dataTransfer.dropEffect = 'move';
        card.classList.add('drag-over');
    },

    /**
     * Handle drag leave event.
     *
     * @param {DragEvent} e - The drag event
     * @param {HTMLElement} card - The activity card
     */
    handleDragLeave(e, card) {
        card.classList.remove('drag-over');
    },

    /**
     * Handle drop event.
     *
     * @param {DragEvent} e - The drag event
     * @param {HTMLElement} targetCard - The drop target card
     * @param {HTMLElement} container - The sortable container
     */
    handleDrop(e, targetCard, container) {
        e.preventDefault();
        if (!this.draggedElement || targetCard === this.draggedElement) return;

        targetCard.classList.remove('drag-over');

        // Determine insert position
        const targetRect = targetCard.getBoundingClientRect();
        const mouseY = e.clientY;
        const insertBefore = mouseY < targetRect.top + targetRect.height / 2;

        // Reorder in DOM
        if (insertBefore) {
            container.insertBefore(this.draggedElement, targetCard);
        } else {
            container.insertBefore(this.draggedElement, targetCard.nextSibling);
        }

        // Save new order to server
        this.saveOrder(container);
    },

    /**
     * Handle touch start event for mobile drag.
     *
     * @param {TouchEvent} e - The touch event
     * @param {HTMLElement} card - The activity card
     * @param {HTMLElement} container - The sortable container
     */
    handleTouchStart(e, card, container) {
        if (this.isDragging) return;

        // Clear any existing timeout to prevent race conditions
        if (this.touchTimeout) {
            clearTimeout(this.touchTimeout);
            this.touchTimeout = null;
        }

        this.touchStartY = e.touches[0].clientY;
        this.draggedElement = card;
        this.draggedIndex = this.getCardIndex(card, container);

        // Store original order for potential rollback
        this.originalOrder = Array.from(container.querySelectorAll('.activity-card'));

        // Add a small delay before starting drag
        this.touchTimeout = setTimeout(() => {
            this.isDragging = true;
            card.classList.add('dragging', 'touch-dragging');
            document.body.classList.add('dragging-active');

            // Create placeholder
            this.createPlaceholder(card, container);
        }, this.TOUCH_HOLD_DELAY);
    },

    /**
     * Handle touch move event for mobile drag.
     *
     * @param {TouchEvent} e - The touch event
     * @param {HTMLElement} card - The activity card
     * @param {HTMLElement} container - The sortable container
     */
    handleTouchMove(e, card, container) {
        if (!this.isDragging) {
            // Check if moved enough to cancel timeout
            if (this.touchTimeout) {
                const deltaY = Math.abs(e.touches[0].clientY - this.touchStartY);
                if (deltaY > this.TOUCH_CANCEL_THRESHOLD) {
                    clearTimeout(this.touchTimeout);
                    this.touchTimeout = null;
                }
            }
            return;
        }

        e.preventDefault();
        this.touchCurrentY = e.touches[0].clientY;

        // Move the card visually
        const deltaY = this.touchCurrentY - this.touchStartY;
        card.style.transform = `translateY(${deltaY}px)`;
        card.style.zIndex = '1000';

        // Throttle touch move handling
        const now = Date.now();
        if (now - this.lastTouchMoveTime < this.TOUCH_THROTTLE_MS) return;
        this.lastTouchMoveTime = now;

        // Find the element under the touch point
        const touchX = e.touches[0].clientX;
        const touchY = e.touches[0].clientY;

        // Temporarily hide dragged element to find what's underneath
        card.style.pointerEvents = 'none';
        const elementAtPoint = document.elementFromPoint(touchX, touchY);
        card.style.pointerEvents = '';

        if (elementAtPoint) {
            const targetCard = elementAtPoint.closest('.activity-card');
            if (targetCard && targetCard !== card && container.contains(targetCard)) {
                this.highlightDropTarget(targetCard, touchY, container);
            }
        }
    },

    /**
     * Handle touch end event for mobile drag.
     *
     * @param {TouchEvent} e - The touch event
     * @param {HTMLElement} container - The sortable container
     */
    handleTouchEnd(e, container) {
        if (this.touchTimeout) {
            clearTimeout(this.touchTimeout);
            this.touchTimeout = null;
        }

        if (!this.isDragging || !this.draggedElement) {
            this.isDragging = false;
            return;
        }

        const card = this.draggedElement;

        // Reset styles
        card.classList.remove('dragging', 'touch-dragging');
        card.style.transform = '';
        card.style.zIndex = '';
        document.body.classList.remove('dragging-active');

        // Find where to insert
        const touchY = this.touchCurrentY || this.touchStartY;
        const cards = Array.from(container.querySelectorAll('.activity-card:not(.dragging)'));

        let insertBeforeCard = null;
        for (const targetCard of cards) {
            const rect = targetCard.getBoundingClientRect();
            if (touchY < rect.top + rect.height / 2) {
                insertBeforeCard = targetCard;
                break;
            }
        }

        // Remove placeholder first
        this.removePlaceholder();
        this.removeDropIndicators(container);

        // Reorder in DOM
        if (insertBeforeCard) {
            container.insertBefore(card, insertBeforeCard);
        } else {
            container.appendChild(card);
        }

        // Save new order
        this.saveOrder(container);

        // Reset state
        this.draggedElement = null;
        this.draggedIndex = null;
        this.touchStartY = null;
        this.touchCurrentY = null;
        this.isDragging = false;
    },

    /**
     * Create a placeholder element for touch drag.
     *
     * @param {HTMLElement} card - The dragged card
     * @param {HTMLElement} container - The sortable container
     */
    createPlaceholder(card, container) {
        this.placeholder = document.createElement('div');
        this.placeholder.className = 'drag-placeholder';
        this.placeholder.style.height = `${card.offsetHeight}px`;
        container.insertBefore(this.placeholder, card);
    },

    /**
     * Remove the placeholder element.
     */
    removePlaceholder() {
        if (this.placeholder && this.placeholder.parentNode) {
            this.placeholder.parentNode.removeChild(this.placeholder);
            this.placeholder = null;
        }
    },

    /**
     * Highlight the drop target during touch drag.
     *
     * @param {HTMLElement} targetCard - The potential drop target
     * @param {number} touchY - Current touch Y position
     * @param {HTMLElement} container - The sortable container
     */
    highlightDropTarget(targetCard, touchY, container) {
        this.removeDropIndicators(container);
        targetCard.classList.add('drag-over');
    },

    /**
     * Remove all drop indicators.
     *
     * @param {HTMLElement} container - The sortable container
     */
    removeDropIndicators(container) {
        container.querySelectorAll('.drag-over').forEach(el => {
            el.classList.remove('drag-over');
        });
    },

    /**
     * Get the index of a card in the container.
     *
     * @param {HTMLElement} card - The activity card
     * @param {HTMLElement} container - The sortable container
     * @returns {number} The card index
     */
    getCardIndex(card, container) {
        const cards = Array.from(container.querySelectorAll('.activity-card'));
        return cards.indexOf(card);
    },

    /**
     * Get CSRF token from meta tag.
     *
     * @returns {string|null} The CSRF token or null if not found
     */
    getCsrfToken() {
        const csrfMeta = document.querySelector('meta[name="_csrf"]');
        return csrfMeta ? csrfMeta.getAttribute('content') : null;
    },

    /**
     * Get CSRF header name from meta tag.
     *
     * @returns {string} The CSRF header name (defaults to X-CSRF-TOKEN)
     */
    getCsrfHeader() {
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');
        return headerMeta ? headerMeta.getAttribute('content') : 'X-CSRF-TOKEN';
    },

    /**
     * Rollback to original order after failed save.
     *
     * @param {HTMLElement} container - The sortable container
     */
    rollbackOrder(container) {
        if (this.originalOrder.length > 0) {
            this.originalOrder.forEach(card => container.appendChild(card));
        }
    },

    /**
     * Save the new order to the server.
     *
     * @contract
     *   - pre: Container has data-trip-id and data-day attributes
     *   - post: API called to persist order, UI updated on success/failure
     *   - calls: PUT /api/trips/{tripId}/activities/reorder
     *
     * @param {HTMLElement} container - The sortable container
     */
    async saveOrder(container) {
        const tripId = container.dataset.tripId;
        const day = parseInt(container.dataset.day, 10);
        const cards = container.querySelectorAll('.activity-card');
        const activityIds = Array.from(cards).map(card => card.dataset.activityId);

        if (!tripId || !day || activityIds.length === 0) {
            return;
        }

        // Add saving indicator
        container.classList.add('saving');

        try {
            const csrfToken = this.getCsrfToken();
            const csrfHeader = this.getCsrfHeader();
            const headers = {
                'Content-Type': 'application/json',
            };

            // Add CSRF token if available
            if (csrfToken) {
                headers[csrfHeader] = csrfToken;
            }

            const response = await fetch(`/api/trips/${tripId}/activities/reorder`, {
                method: 'PUT',
                headers: headers,
                body: JSON.stringify({
                    day: day,
                    activityIds: activityIds
                })
            });

            const data = await response.json();

            if (response.ok && data.success) {
                if (window.Toast) {
                    Toast.success('景點順序已更新');
                }
                // Clear original order after successful save
                this.originalOrder = [];
            } else {
                throw new Error(data.message || '更新失敗');
            }
        } catch (error) {
            // Rollback to original order on failure
            this.rollbackOrder(container);

            if (window.Toast) {
                Toast.error('無法更新景點順序，請稍後再試');
            }
        } finally {
            container.classList.remove('saving');
        }
    }
};

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    DragReorder.init();

    // Expose to global scope
    window.DragReorder = DragReorder;
});
