/**
 * WeGo - Todo JavaScript Module
 *
 * @contract
 *   - Provides API methods for todo CRUD operations
 *   - Provides UI methods for rendering and interactions
 *   - Integrates with global Toast, Modal, Loading utilities from app.js
 */

/**
 * Todo API client for backend communication.
 *
 * @contract
 *   - All methods return Promises
 *   - All methods use TRIP_ID from window context
 *   - Handles API response format: { success, data, message, errorCode }
 */
const TodoApi = {
    /**
     * Gets the base URL for todo API endpoints.
     * @returns {string} Base API URL
     */
    getBaseUrl() {
        return `/api/trips/${window.TRIP_ID}/todos`;
    },

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
     * Lists all todos for the current trip.
     *
     * @contract
     *   - pre: window.TRIP_ID is set
     *   - post: returns array of TodoResponse objects
     *   - calls: GET /api/trips/{tripId}/todos
     *
     * @returns {Promise<Array>} List of todos
     */
    async list() {
        try {
            const response = await fetch(this.getBaseUrl(), {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                },
                credentials: 'same-origin'
            });

            const result = await response.json();

            if (!response.ok || !result.success) {
                throw new Error(result.message || 'Failed to load todos');
            }

            return result.data;
        } catch (error) {
            throw error;
        }
    },

    /**
     * Creates a new todo.
     *
     * @contract
     *   - pre: data.title is required
     *   - post: returns created TodoResponse
     *   - calls: POST /api/trips/{tripId}/todos
     *
     * @param {Object} data - Todo creation data
     * @param {string} data.title - Todo title (required)
     * @param {string} [data.description] - Todo description
     * @param {string} [data.assigneeId] - Assignee user ID
     * @param {string} [data.dueDate] - Due date (YYYY-MM-DD)
     * @returns {Promise<Object>} Created todo
     */
    async create(data) {
        try {
            const response = await fetch(this.getBaseUrl(), {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json',
                    [this.getCsrfHeader()]: this.getCsrfToken()
                },
                credentials: 'same-origin',
                body: JSON.stringify(data)
            });

            const result = await response.json();

            if (!response.ok || !result.success) {
                throw new Error(result.message || 'Failed to create todo');
            }

            return result.data;
        } catch (error) {
            throw error;
        }
    },

    /**
     * Updates an existing todo.
     *
     * @contract
     *   - pre: todoId is valid UUID
     *   - post: returns updated TodoResponse
     *   - calls: PUT /api/trips/{tripId}/todos/{todoId}
     *
     * @param {string} todoId - Todo ID
     * @param {Object} data - Update data
     * @returns {Promise<Object>} Updated todo
     */
    async update(todoId, data) {
        try {
            const response = await fetch(`${this.getBaseUrl()}/${todoId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json',
                    [this.getCsrfHeader()]: this.getCsrfToken()
                },
                credentials: 'same-origin',
                body: JSON.stringify(data)
            });

            const result = await response.json();

            if (!response.ok || !result.success) {
                throw new Error(result.message || 'Failed to update todo');
            }

            return result.data;
        } catch (error) {
            throw error;
        }
    },

    /**
     * Deletes a todo.
     *
     * @contract
     *   - pre: todoId is valid UUID
     *   - post: todo is removed
     *   - calls: DELETE /api/trips/{tripId}/todos/{todoId}
     *
     * @param {string} todoId - Todo ID to delete
     * @returns {Promise<void>}
     */
    async delete(todoId) {
        try {
            const response = await fetch(`${this.getBaseUrl()}/${todoId}`, {
                method: 'DELETE',
                headers: {
                    'Accept': 'application/json',
                    [this.getCsrfHeader()]: this.getCsrfToken()
                },
                credentials: 'same-origin'
            });

            const result = await response.json();

            if (!response.ok || !result.success) {
                throw new Error(result.message || 'Failed to delete todo');
            }
        } catch (error) {
            throw error;
        }
    },

    /**
     * Gets a single todo by ID.
     *
     * @contract
     *   - pre: todoId is valid UUID
     *   - post: returns TodoResponse
     *   - calls: GET /api/trips/{tripId}/todos/{todoId}
     *
     * @param {string} todoId - Todo ID
     * @returns {Promise<Object>} Todo data
     */
    async get(todoId) {
        try {
            const response = await fetch(`${this.getBaseUrl()}/${todoId}`, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                },
                credentials: 'same-origin'
            });

            const result = await response.json();

            if (!response.ok || !result.success) {
                throw new Error(result.message || 'Failed to get todo');
            }

            return result.data;
        } catch (error) {
            throw error;
        }
    }
};

/**
 * Todo UI controller for DOM manipulation and user interactions.
 *
 * @contract
 *   - Uses TodoApi for data operations
 *   - Uses Toast, Modal, Loading from app.js
 *   - Manages filter state and todo list rendering
 */
const TodoUI = {
    currentFilter: 'all',
    editingTodoId: null,

    /**
     * Initializes the Todo UI module.
     *
     * @contract
     *   - post: Event listeners are set up
     *   - post: Modal is initialized
     *   - calledBy: DOMContentLoaded
     */
    init() {
        // Initialize modal close handlers
        const modal = document.getElementById('todo-modal');
        if (modal) {
            const backdrop = modal.querySelector('[data-modal-backdrop]');
            if (backdrop) {
                backdrop.addEventListener('click', () => {
                    Modal.close('todo-modal');
                });
            }
        }

        // Keyboard shortcut to close modal
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                const modal = document.getElementById('todo-modal');
                if (modal && !modal.classList.contains('hidden')) {
                    Modal.close('todo-modal');
                }
            }
        });
    },

    /**
     * Filters todos by status.
     *
     * @contract
     *   - pre: filter is 'all', 'PENDING', 'IN_PROGRESS', or 'COMPLETED'
     *   - post: Only matching todos are visible
     *   - post: Filter tabs are updated
     *
     * @param {string} filter - Status filter
     */
    filterTodos(filter) {
        this.currentFilter = filter;

        // Update filter tabs
        document.querySelectorAll('.filter-tab').forEach(tab => {
            if (tab.dataset.filter === filter) {
                tab.classList.add('active');
            } else {
                tab.classList.remove('active');
            }
        });

        // Filter todo items
        const todoItems = document.querySelectorAll('.todo-item');
        let visibleCount = 0;

        todoItems.forEach(item => {
            const status = item.dataset.status;
            const shouldShow = filter === 'all' || status === filter;

            if (shouldShow) {
                item.classList.remove('hidden');
                visibleCount++;
            } else {
                item.classList.add('hidden');
            }
        });

        // Show/hide empty state and no results state
        const emptyState = document.getElementById('empty-state');
        const noResultsState = document.getElementById('no-results-state');
        const todoList = document.getElementById('todo-list');

        if (todoItems.length === 0) {
            // Original empty state - no todos at all
            if (emptyState) emptyState.classList.remove('hidden');
            if (noResultsState) noResultsState.classList.add('hidden');
        } else if (visibleCount === 0) {
            // Filter resulted in no matches
            if (emptyState) emptyState.classList.add('hidden');
            if (noResultsState) noResultsState.classList.remove('hidden');
        } else {
            // Show todo list
            if (emptyState) emptyState.classList.add('hidden');
            if (noResultsState) noResultsState.classList.add('hidden');
        }
    },

    /**
     * Shows the create todo modal.
     *
     * @contract
     *   - post: Modal is open with empty form
     *   - post: editingTodoId is null
     */
    showCreateModal() {
        this.editingTodoId = null;

        // Reset form
        const form = document.getElementById('todo-form');
        if (form) {
            form.reset();
        }

        // Update modal title
        const title = document.getElementById('modal-title');
        if (title) {
            title.textContent = '新增待辦事項';
        }

        // Hide status field and delete button for create
        const statusField = document.getElementById('status-field');
        const deleteBtn = document.getElementById('delete-btn');
        if (statusField) statusField.classList.add('hidden');
        if (deleteBtn) deleteBtn.classList.add('hidden');

        // Clear hidden id field
        const idField = document.getElementById('todo-id');
        if (idField) idField.value = '';

        // Update submit button text
        const submitBtn = document.getElementById('submit-btn');
        if (submitBtn) {
            submitBtn.textContent = '新增';
        }

        // Clear flatpickr due date
        const dueDateInput = document.getElementById('todo-due-date');
        if (dueDateInput && dueDateInput._flatpickr) {
            dueDateInput._flatpickr.clear();
        }

        // Open modal
        Modal.open('todo-modal');

        // Focus on title input
        setTimeout(() => {
            const titleInput = document.getElementById('todo-title');
            if (titleInput) titleInput.focus();
        }, 100);
    },

    /**
     * Shows the edit todo modal.
     *
     * @contract
     *   - pre: todoId is valid
     *   - post: Modal is open with todo data populated
     *   - post: editingTodoId is set
     *
     * @param {string} todoId - Todo ID to edit
     */
    async showEditModal(todoId) {
        if (!window.CAN_EDIT) return;

        this.editingTodoId = todoId;

        try {
            const todo = await TodoApi.get(todoId);

            // Populate form
            const form = document.getElementById('todo-form');
            if (form) {
                document.getElementById('todo-id').value = todo.id;
                document.getElementById('todo-title').value = todo.title || '';
                document.getElementById('todo-description').value = todo.description || '';
                document.getElementById('todo-assignee').value = todo.assigneeId || '';
                const dueDateInput = document.getElementById('todo-due-date');
                if (dueDateInput._flatpickr) {
                    dueDateInput._flatpickr.setDate(todo.dueDate || '', false);
                } else {
                    dueDateInput.value = todo.dueDate || '';
                }
                document.getElementById('todo-status').value = todo.status || 'PENDING';
            }

            // Update modal title
            const title = document.getElementById('modal-title');
            if (title) {
                title.textContent = '編輯待辦事項';
            }

            // Show status field and delete button for edit
            const statusField = document.getElementById('status-field');
            const deleteBtn = document.getElementById('delete-btn');
            if (statusField) statusField.classList.remove('hidden');
            if (deleteBtn) deleteBtn.classList.remove('hidden');

            // Update submit button text
            const submitBtn = document.getElementById('submit-btn');
            if (submitBtn) {
                submitBtn.textContent = '儲存';
            }

            // Open modal
            Modal.open('todo-modal');

        } catch (error) {
            Toast.error('無法載入待辦事項');
        }
    },

    /**
     * Handles form submission for create/update.
     *
     * @contract
     *   - pre: form is valid
     *   - post: Todo is created or updated
     *   - post: Page reloads on success
     *
     * @param {Event} event - Form submit event
     */
    async handleSubmit(event) {
        event.preventDefault();

        const form = event.target;
        const submitBtn = document.getElementById('submit-btn');

        // Get form data
        const formData = new FormData(form);
        const data = {
            title: formData.get('title')?.trim(),
            description: formData.get('description')?.trim() || null,
            assigneeId: formData.get('assigneeId') || null,
            dueDate: formData.get('dueDate') || null
        };

        // Validate
        if (!data.title) {
            Toast.error('請輸入標題');
            document.getElementById('todo-title')?.focus();
            return;
        }

        // Start loading
        if (submitBtn) {
            Loading.start(submitBtn);
        }

        try {
            if (this.editingTodoId) {
                // Update existing
                data.status = formData.get('status') || 'PENDING';

                // Handle clearing assignee
                if (!data.assigneeId) {
                    data.clearAssignee = true;
                }

                // Handle clearing due date
                if (!data.dueDate) {
                    data.clearDueDate = true;
                }

                await TodoApi.update(this.editingTodoId, data);
                Toast.success('待辦事項已更新');
            } else {
                // Create new
                await TodoApi.create(data);
                Toast.success('待辦事項已建立');
            }

            // Close modal and reload
            Modal.close('todo-modal');

            // Reload page to show updated list
            setTimeout(() => {
                window.location.reload();
            }, 500);

        } catch (error) {
            Toast.error(error.message || '操作失敗');
        } finally {
            if (submitBtn) {
                Loading.stop(submitBtn);
            }
        }
    },

    /**
     * Toggles todo status.
     *
     * Cycle: PENDING -> IN_PROGRESS -> COMPLETED -> PENDING
     *
     * @contract
     *   - pre: todoId is valid
     *   - pre: currentStatus is valid TodoStatus
     *   - post: Status is updated to next in cycle
     *
     * @param {string} todoId - Todo ID
     * @param {string} currentStatus - Current status
     */
    async toggleStatus(todoId, currentStatus) {
        if (!window.CAN_EDIT) return;

        // Determine next status
        const statusCycle = {
            'PENDING': 'IN_PROGRESS',
            'IN_PROGRESS': 'COMPLETED',
            'COMPLETED': 'PENDING'
        };
        const newStatus = statusCycle[currentStatus] || 'PENDING';

        try {
            await TodoApi.update(todoId, { status: newStatus });

            // Show success message based on status
            const messages = {
                'PENDING': '已設為待處理',
                'IN_PROGRESS': '已設為進行中',
                'COMPLETED': '已完成'
            };
            Toast.success(messages[newStatus] || '狀態已更新');

            // Reload page to show updated status
            setTimeout(() => {
                window.location.reload();
            }, 300);

        } catch (error) {
            Toast.error('無法更新狀態');
        }
    },

    /**
     * Deletes the currently editing todo.
     *
     * @contract
     *   - pre: editingTodoId is set
     *   - post: Todo is deleted
     *   - post: Page reloads on success
     */
    async deleteTodo() {
        if (!this.editingTodoId || !window.CAN_EDIT) return;

        // Confirm deletion
        if (!confirm('確定要刪除這個待辦事項嗎？')) {
            return;
        }

        const deleteBtn = document.getElementById('delete-btn');
        if (deleteBtn) {
            Loading.start(deleteBtn);
        }

        try {
            await TodoApi.delete(this.editingTodoId);
            Toast.success('待辦事項已刪除');

            // Close modal and reload
            Modal.close('todo-modal');

            setTimeout(() => {
                window.location.reload();
            }, 500);

        } catch (error) {
            Toast.error('無法刪除待辦事項');
        } finally {
            if (deleteBtn) {
                Loading.stop(deleteBtn);
            }
        }
    },

    /**
     * Renders todos into the list container.
     * Used for dynamic updates without page reload.
     *
     * @contract
     *   - pre: todos is array of TodoResponse
     *   - post: todo-list container is updated
     *
     * @param {Array} todos - Array of todo objects
     */
    renderTodos(todos) {
        const container = document.getElementById('todo-list');
        if (!container) return;

        if (!todos || todos.length === 0) {
            container.innerHTML = '';
            this.filterTodos(this.currentFilter);
            return;
        }

        container.innerHTML = todos.map(todo => this.renderTodoItem(todo)).join('');
        this.filterTodos(this.currentFilter);
    },

    /**
     * Renders a single todo item HTML.
     *
     * @param {Object} todo - Todo object
     * @returns {string} HTML string
     */
    renderTodoItem(todo) {
        const statusClasses = {
            'PENDING': 'border-gray-300 dark:border-gray-600 hover:border-primary-500',
            'IN_PROGRESS': 'bg-primary-500 border-primary-500 text-white',
            'COMPLETED': 'bg-success border-success text-white'
        };

        const statusBadges = {
            'PENDING': '<span class="bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300">待處理</span>',
            'IN_PROGRESS': '<span class="bg-primary-100 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400">進行中</span>',
            'COMPLETED': '<span class="bg-success-light text-success-dark">已完成</span>'
        };

        const titleClass = todo.status === 'COMPLETED'
            ? 'text-gray-400 dark:text-gray-500 line-through'
            : 'text-gray-800 dark:text-gray-100';

        const checkboxContent = todo.status === 'COMPLETED'
            ? '<svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>'
            : todo.status === 'IN_PROGRESS'
                ? '<div class="w-2 h-2 bg-white rounded-full"></div>'
                : '';

        const dueDateHtml = todo.dueDate
            ? `<div class="flex items-center gap-1 ${todo.overdue ? 'text-error' : ''}">
                   <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                       <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                             d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
                   </svg>
                   <span>${this.formatDate(todo.dueDate)}</span>
                   ${todo.overdue ? '<span class="text-error font-medium">逾期</span>' : ''}
               </div>`
            : '';

        const assigneeHtml = todo.assigneeName
            ? `<div class="flex items-center gap-1">
                   ${todo.assigneeAvatarUrl
                       ? `<img src="${this.escapeHtml(todo.assigneeAvatarUrl)}" alt="${this.escapeHtml(todo.assigneeName)}" class="w-5 h-5 rounded-full object-cover"/>`
                       : `<span class="w-5 h-5 rounded-full bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center text-[10px] font-medium text-primary-600 dark:text-primary-400">${this.escapeHtml(todo.assigneeName.charAt(0))}</span>`
                   }
                   <span>${this.escapeHtml(todo.assigneeName)}</span>
               </div>`
            : '';

        const clickHandler = window.CAN_EDIT
            ? `onclick="TodoUI.showEditModal('${todo.id}')"`
            : '';

        const toggleHandler = window.CAN_EDIT
            ? `onclick="TodoUI.toggleStatus('${todo.id}', '${todo.status}')"`
            : '';

        return `
            <div data-todo-id="${todo.id}"
                 data-status="${todo.status}"
                 class="todo-item glass-card p-4 flex items-start gap-4
                        hover:shadow-md transition-all duration-200">
                <button type="button"
                        ${toggleHandler}
                        class="flex-shrink-0 w-6 h-6 mt-0.5 rounded-full border-2 flex items-center justify-center cursor-pointer transition-all duration-200 ${statusClasses[todo.status]}"
                        aria-label="切換狀態">
                    ${checkboxContent}
                </button>

                <div class="flex-1 min-w-0 ${window.CAN_EDIT ? 'cursor-pointer' : ''}" ${clickHandler}>
                    <h3 class="font-medium truncate ${titleClass}">${this.escapeHtml(todo.title)}</h3>
                    ${todo.description ? `<p class="text-sm text-gray-500 dark:text-gray-400 mt-1 line-clamp-2">${this.escapeHtml(todo.description)}</p>` : ''}
                    <div class="flex items-center gap-3 mt-2 text-xs text-gray-500 dark:text-gray-400">
                        ${assigneeHtml}
                        ${dueDateHtml}
                    </div>
                </div>

                <span class="flex-shrink-0 px-2 py-1 rounded-full text-xs font-medium">
                    ${statusBadges[todo.status]}
                </span>
            </div>
        `;
    },

    /**
     * Formats a date string for display.
     * @param {string} dateStr - Date string (YYYY-MM-DD)
     * @returns {string} Formatted date (MM/DD)
     */
    formatDate(dateStr) {
        if (!dateStr) return '';
        const date = new Date(dateStr);
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${month}/${day}`;
    },

    /**
     * Escapes HTML to prevent XSS.
     * @param {string} text - Text to escape
     * @returns {string} Escaped text
     */
    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
};

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    TodoUI.init();

    // Expose to global scope
    window.TodoApi = TodoApi;
    window.TodoUI = TodoUI;
});
