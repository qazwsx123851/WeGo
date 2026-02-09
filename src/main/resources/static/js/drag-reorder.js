/**
 * WeGo - Drag and Drop Reorder Module
 *
 * @contract
 *   - Handles drag-and-drop reordering of activity cards
 *   - Supports both mouse and touch interactions
 *   - Uses FLIP animation for smooth reordering transitions
 *   - Shows insertion line indicators for precise drop targeting
 *   - Calls API to persist order changes
 *   - calledBy: activity/list.html
 *   - calls: PUT /api/trips/{tripId}/activities/reorder
 */

const DragReorder = {
    // Configuration constants
    TOUCH_HOLD_DELAY: 150,         // Delay before starting touch drag (prevents accidental drags)
    TOUCH_CANCEL_THRESHOLD: 10,    // Pixels moved to cancel touch-hold
    TOUCH_THROTTLE_MS: 50,         // Throttle interval for touch move events
    ANIMATION_DURATION_MS: 200,    // FLIP animation duration

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
    reducedMotion: false,
    _mouseDownTarget: null,        // Track mousedown target for drag handle check

    /**
     * Initialize drag-and-drop functionality.
     *
     * @contract
     *   - pre: DOM is ready
     *   - post: Event listeners attached to sortable containers
     */
    init() {
        this.reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

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
            // Track mousedown target so dragstart can verify it came from the drag handle
            card.addEventListener('mousedown', (e) => {
                this._mouseDownTarget = e.target;
            });

            // Mouse drag events
            card.addEventListener('dragstart', (e) => this.handleDragStart(e, card, container));
            card.addEventListener('dragend', (e) => this.handleDragEnd(e, container));
            card.addEventListener('dragover', (e) => this.handleDragOver(e, card, container));
            card.addEventListener('dragleave', (e) => this.handleDragLeave(e, card));
            card.addEventListener('drop', (e) => this.handleDrop(e, card, container));

            // Touch events for mobile
            const dragHandle = card.querySelector('.drag-handle');
            if (dragHandle) {
                dragHandle.addEventListener('touchstart', (e) => this.handleTouchStart(e, card, container), { passive: false });
                dragHandle.addEventListener('touchmove', (e) => this.handleTouchMove(e, card, container), { passive: false });
                dragHandle.addEventListener('touchend', (e) => this.handleTouchEnd(e, container), { passive: false });
                dragHandle.addEventListener('touchcancel', (e) => this.handleTouchCancel(e, container), { passive: false });
            }
        });

        // Container events
        container.addEventListener('dragover', (e) => e.preventDefault());
    },

    /**
     * Get only activity card elements from a container (excludes transit connectors).
     *
     * @param {HTMLElement} container - The sortable container
     * @returns {HTMLElement[]} Array of activity card elements
     */
    getActivityCards(container) {
        return Array.from(container.querySelectorAll('.activity-card'));
    },

    /**
     * Handle drag start event.
     *
     * @param {DragEvent} e - The drag event
     * @param {HTMLElement} card - The activity card
     * @param {HTMLElement} container - The sortable container
     */
    handleDragStart(e, card, container) {
        // Only allow drag from drag handle (e.target is always the draggable element,
        // so we check the mousedown target saved earlier)
        if (!this._mouseDownTarget || !this._mouseDownTarget.closest('.drag-handle')) {
            e.preventDefault();
            return;
        }

        this.draggedElement = card;
        this.draggedIndex = this.getCardIndex(card, container);
        this.isDragging = true;

        // Store original order for potential rollback
        this.originalOrder = this.getActivityCards(container);

        // Set drag image
        e.dataTransfer.effectAllowed = 'move';
        e.dataTransfer.setData('text/plain', card.dataset.activityId);

        // Add dragging styles after a short delay (so browser captures drag image first)
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
     * Handle drag over event - shows insertion line indicator.
     *
     * @param {DragEvent} e - The drag event
     * @param {HTMLElement} card - The activity card being dragged over
     * @param {HTMLElement} container - The sortable container
     */
    handleDragOver(e, card, container) {
        e.preventDefault();
        if (!this.draggedElement || card === this.draggedElement) return;

        e.dataTransfer.dropEffect = 'move';

        // Determine insertion position based on cursor Y relative to card midpoint
        const rect = card.getBoundingClientRect();
        const midY = rect.top + rect.height / 2;
        const insertAbove = e.clientY < midY;

        // Remove all existing indicators in the container
        this.removeDropIndicators(container);

        // Add directional indicator
        if (insertAbove) {
            card.classList.add('drag-insert-above');
        } else {
            card.classList.add('drag-insert-below');
        }
    },

    /**
     * Handle drag leave event.
     *
     * @param {DragEvent} e - The drag event
     * @param {HTMLElement} card - The activity card
     */
    handleDragLeave(e, card) {
        card.classList.remove('drag-insert-above', 'drag-insert-below');
    },

    /**
     * Handle drop event with FLIP animation.
     *
     * @param {DragEvent} e - The drag event
     * @param {HTMLElement} targetCard - The drop target card
     * @param {HTMLElement} container - The sortable container
     */
    handleDrop(e, targetCard, container) {
        e.preventDefault();
        if (!this.draggedElement || targetCard === this.draggedElement) return;

        this.removeDropIndicators(container);

        const targetRect = targetCard.getBoundingClientRect();
        const mouseY = e.clientY;
        const insertBefore = mouseY < targetRect.top + targetRect.height / 2;

        // FLIP: capture positions before DOM change
        const firstPositions = this.capturePositions(container);

        // Perform DOM reorder
        if (insertBefore) {
            container.insertBefore(this.draggedElement, targetCard);
        } else {
            container.insertBefore(this.draggedElement, targetCard.nextSibling);
        }

        // FLIP: animate to new positions
        this.animateFLIP(container, firstPositions);

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
        this.originalOrder = this.getActivityCards(container);

        // Add a small delay before starting drag
        this.touchTimeout = setTimeout(() => {
            this.isDragging = true;
            card.classList.add('dragging', 'touch-dragging');
            document.body.classList.add('dragging-active');

            // Animate the lift effect
            if (!this.reducedMotion) {
                card.style.transition = 'transform 150ms ease, box-shadow 150ms ease';
                card.style.transform = 'scale(1.03)';
            }

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
        card.style.transition = 'none';
        card.style.transform = `translateY(${deltaY}px) scale(1.03)`;
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
                // Calculate insertion position
                const targetRect = targetCard.getBoundingClientRect();
                const insertAbove = touchY < targetRect.top + targetRect.height / 2;

                this.removeDropIndicators(container);
                if (insertAbove) {
                    targetCard.classList.add('drag-insert-above');
                } else {
                    targetCard.classList.add('drag-insert-below');
                }

                // Move placeholder to indicate drop position
                if (this.placeholder) {
                    if (insertAbove) {
                        container.insertBefore(this.placeholder, targetCard);
                    } else if (targetCard.nextSibling) {
                        container.insertBefore(this.placeholder, targetCard.nextSibling);
                    } else {
                        container.appendChild(this.placeholder);
                    }
                }
            }
        }
    },

    /**
     * Handle touch end event for mobile drag with FLIP animation.
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

        // Capture final touch position (throttling may have skipped it)
        if (e.changedTouches && e.changedTouches[0]) {
            this.touchCurrentY = e.changedTouches[0].clientY;
        }

        const card = this.draggedElement;

        // Reset styles
        card.classList.remove('dragging', 'touch-dragging');
        card.style.transform = '';
        card.style.transition = '';
        card.style.zIndex = '';
        document.body.classList.remove('dragging-active');

        // Find where to insert
        const touchY = this.touchCurrentY || this.touchStartY;
        const cards = this.getActivityCards(container).filter(c => c !== card);

        let insertBeforeCard = null;
        for (const targetCard of cards) {
            const rect = targetCard.getBoundingClientRect();
            if (touchY < rect.top + rect.height / 2) {
                insertBeforeCard = targetCard;
                break;
            }
        }

        // Remove placeholder and indicators
        this.removePlaceholder();
        this.removeDropIndicators(container);

        // FLIP: capture positions before DOM change
        const firstPositions = this.capturePositions(container);

        // Reorder in DOM
        if (insertBeforeCard) {
            container.insertBefore(card, insertBeforeCard);
        } else {
            container.appendChild(card);
        }

        // FLIP: animate to new positions
        this.animateFLIP(container, firstPositions);

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
     * Handle touch cancel event (system interruption like incoming call).
     *
     * @param {TouchEvent} e - The touch event
     * @param {HTMLElement} container - The sortable container
     */
    handleTouchCancel(e, container) {
        if (this.touchTimeout) {
            clearTimeout(this.touchTimeout);
            this.touchTimeout = null;
        }

        if (this.draggedElement) {
            this.draggedElement.classList.remove('dragging', 'touch-dragging');
            this.draggedElement.style.transform = '';
            this.draggedElement.style.transition = '';
            this.draggedElement.style.zIndex = '';
        }

        document.body.classList.remove('dragging-active');
        this.removePlaceholder();
        this.removeDropIndicators(container);

        // Rollback to original order if we were mid-drag
        if (this.isDragging && this.originalOrder.length > 0) {
            this.rollbackOrder(container);
        }

        // Reset all state
        this.draggedElement = null;
        this.draggedIndex = null;
        this.touchStartY = null;
        this.touchCurrentY = null;
        this.isDragging = false;
    },

    /**
     * Capture current positions of all activity cards for FLIP animation.
     *
     * @param {HTMLElement} container - The sortable container
     * @returns {Map<HTMLElement, DOMRect>} Map of cards to their current positions
     */
    capturePositions(container) {
        const positions = new Map();
        this.getActivityCards(container).forEach(card => {
            positions.set(card, card.getBoundingClientRect());
        });
        return positions;
    },

    /**
     * Animate cards from their old positions to new positions using FLIP technique.
     * First, Last, Invert, Play.
     *
     * @param {HTMLElement} container - The sortable container
     * @param {Map<HTMLElement, DOMRect>} firstPositions - Positions captured before DOM change
     */
    animateFLIP(container, firstPositions) {
        if (this.reducedMotion) return;

        const cards = this.getActivityCards(container);
        cards.forEach(card => {
            const first = firstPositions.get(card);
            if (!first) return;

            const last = card.getBoundingClientRect();
            const deltaY = first.top - last.top;

            if (Math.abs(deltaY) < 1) return; // no movement needed

            // Invert: set card to old position
            card.style.transition = 'none';
            card.style.transform = `translateY(${deltaY}px)`;

            // Force reflow to ensure the inverted position is applied
            card.offsetHeight; // eslint-disable-line no-unused-expressions

            // Play: animate to new (correct) position
            card.style.transition = `transform ${this.ANIMATION_DURATION_MS}ms ease`;
            card.style.transform = '';

            // Cleanup after animation completes (with safety timeout)
            let safetyTimeout = null;
            const cleanup = () => {
                card.style.transition = '';
                card.style.transform = '';
                if (safetyTimeout) {
                    clearTimeout(safetyTimeout);
                    safetyTimeout = null;
                }
            };
            card.addEventListener('transitionend', cleanup, { once: true });

            // Safety cleanup in case transitionend doesn't fire
            safetyTimeout = setTimeout(() => {
                card.removeEventListener('transitionend', cleanup);
                card.style.transition = '';
                card.style.transform = '';
            }, this.ANIMATION_DURATION_MS + 50);
        });
    },

    /**
     * Create a placeholder element for touch drag with expand animation.
     *
     * @param {HTMLElement} card - The dragged card
     * @param {HTMLElement} container - The sortable container
     */
    createPlaceholder(card, container) {
        this.placeholder = document.createElement('div');
        this.placeholder.className = 'drag-placeholder';
        const cardHeight = card.offsetHeight;

        if (!this.reducedMotion) {
            // Start at 0 height and animate open
            this.placeholder.style.height = '0px';
            container.insertBefore(this.placeholder, card);
            requestAnimationFrame(() => {
                this.placeholder.style.height = `${cardHeight}px`;
            });
        } else {
            this.placeholder.style.height = `${cardHeight}px`;
            container.insertBefore(this.placeholder, card);
        }
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
     * Remove all drop indicators (insertion lines).
     *
     * @param {HTMLElement} container - The sortable container
     */
    removeDropIndicators(container) {
        container.querySelectorAll('.drag-insert-above, .drag-insert-below, .drag-over').forEach(el => {
            el.classList.remove('drag-insert-above', 'drag-insert-below', 'drag-over');
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
        return this.getActivityCards(container).indexOf(card);
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

        // Show loading overlay immediately
        if (window.ReorderLoading) {
            window.ReorderLoading.show();
        }

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
                // Clear original order after successful save
                this.originalOrder = [];
                // Reload page to reflect updated transit connectors and transport data
                window.location.reload();
            } else {
                throw new Error(data.message || '更新失敗');
            }
        } catch (error) {
            // Hide loading overlay on failure
            if (window.ReorderLoading) {
                window.ReorderLoading.hide();
            }
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
