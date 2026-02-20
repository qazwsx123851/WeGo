/**
 * WeGo - SortableJS-based Drag Reorder Module
 *
 * @contract
 *   - Handles drag-and-drop reordering of activity cards using SortableJS
 *   - Supports both desktop and mobile via SortableJS fallback mode
 *   - Respects reduced motion preference
 *   - Persists order changes via API with rollback on failure
 *   - calledBy: activity/list.html
 *   - calls: PUT /api/trips/{tripId}/activities/reorder
 */
const SortableReorder = {
    /** @type {Sortable[]} Active SortableJS instances */
    instances: [],

    /** @type {boolean} User prefers reduced motion */
    reducedMotion: false,

    /** @type {RegExp} UUID v4 format validation */
    UUID_REGEX: /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,

    /**
     * Initialize SortableJS on all sortable containers.
     *
     * @contract
     *   - pre: DOM is ready, SortableJS loaded globally
     *   - post: SortableJS instances attached to .sortable-activities containers
     */
    init() {
        if (typeof Sortable === 'undefined') {
            return;
        }

        this.reducedMotion = window.matchMedia(
            '(prefers-reduced-motion: reduce)'
        ).matches;

        const containers = document.querySelectorAll('.sortable-activities');
        containers.forEach(container => this.initContainer(container));
    },

    /**
     * Initialize a single sortable container.
     *
     * @param {HTMLElement} container - The sortable container element
     */
    initContainer(container) {
        const self = this;
        let originalOrder = [];

        const instance = Sortable.create(container, {
            handle: '.drag-handle',
            draggable: '.activity-card',
            dataIdAttr: 'data-activity-id',
            animation: self.reducedMotion ? 0 : 200,
            easing: 'cubic-bezier(0.25, 1, 0.5, 1)',
            forceFallback: true,
            fallbackOnBody: true,
            fallbackTolerance: 3,
            delay: 100,
            delayOnTouchOnly: true,
            touchStartThreshold: 5,
            ghostClass: 'sortable-ghost',
            chosenClass: 'sortable-chosen',
            fallbackClass: 'sortable-fallback',

            onStart() {
                originalOrder = instance.toArray();
                document.body.classList.add('dragging-active');
            },

            onEnd() {
                document.body.classList.remove('dragging-active');

                const newOrder = instance.toArray();

                // Skip API call if order unchanged
                const orderChanged = newOrder.some(
                    (id, i) => id !== originalOrder[i]
                );
                if (!orderChanged) return;

                self.saveOrder(container, instance, newOrder, originalOrder);
            }
        });

        this.instances.push(instance);
    },

    /**
     * Persist the new activity order to the server.
     *
     * @contract
     *   - pre: container has data-trip-id, data-day attributes
     *   - post: API called; on success page reloads; on failure order is rolled back
     *   - calls: PUT /api/trips/{tripId}/activities/reorder
     *
     * @param {HTMLElement} container
     * @param {Sortable} instance - The SortableJS instance
     * @param {string[]} newOrder - Activity IDs in new order
     * @param {string[]} originalOrder - Activity IDs in original order
     */
    async saveOrder(container, instance, newOrder, originalOrder) {
        const tripId = container.dataset.tripId;
        const day = parseInt(container.dataset.day, 10);

        // Validate tripId (UUID format) and day (positive integer)
        if (!tripId || !this.UUID_REGEX.test(tripId)) return;
        if (!Number.isInteger(day) || day < 1) return;
        if (newOrder.length === 0) return;

        // Validate all activity IDs are valid UUIDs
        if (!newOrder.every(id => this.UUID_REGEX.test(id))) return;

        container.classList.add('saving');

        if (window.ReorderLoading) {
            window.ReorderLoading.show();
        }

        try {
            const response = await fetch(
                `/api/trips/${tripId}/activities/reorder`,
                {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                        [WeGo.getCsrfHeader()]: WeGo.getCsrfToken()
                    },
                    body: JSON.stringify({ day, activityIds: newOrder })
                }
            );

            let data;
            try {
                data = await response.json();
            } catch {
                throw new Error('伺服器回應異常');
            }

            if (response.ok && data.success) {
                // Reload to refresh server-rendered transit connectors between activities
                window.location.reload();
            } else {
                throw new Error(data.message || '更新失敗');
            }
        } catch (error) {
            if (window.ReorderLoading) {
                window.ReorderLoading.hide();
            }

            // Rollback using SortableJS built-in sort
            instance.sort(originalOrder, true);

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
    SortableReorder.init();
    window.SortableReorder = SortableReorder;
});
