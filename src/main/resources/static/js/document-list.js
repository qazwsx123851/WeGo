/**
 * WeGo - Document List JavaScript Module
 *
 * Handles document upload, preview, delete, filtering, and sorting.
 * Integrates with global Toast utility from app.js
 * and WeGo shared utilities from common.js.
 */
const DocumentList = (() => {
    'use strict';

    // State
    let currentDocumentId = null;
    let isUploading = false;

    // DOM references (cached on init)
    let tripId = null;

    // ── Helpers ──────────────────────────────────────────────

    function formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
    }

    function setButtonDisabled(btn, disabled, loadingText) {
        if (!btn) return;
        if (disabled) {
            btn.disabled = true;
            if (loadingText) {
                btn.dataset.originalContent = btn.innerHTML;
                btn.innerHTML = '<svg class="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">' +
                    '<circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>' +
                    '<path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>' +
                    '</svg> ' + loadingText;
                btn.classList.add('flex', 'items-center', 'justify-center', 'gap-2');
            }
        } else {
            btn.disabled = false;
            if (btn.dataset.originalContent) {
                btn.innerHTML = btn.dataset.originalContent;
                delete btn.dataset.originalContent;
                btn.classList.remove('flex', 'items-center', 'justify-center', 'gap-2');
            }
        }
    }

    // ── Upload Modal ─────────────────────────────────────────

    function openUploadModal() {
        const modal = document.getElementById('upload-modal');
        if (modal) {
            modal.classList.remove('hidden');
            document.body.style.overflow = 'hidden';
        }
    }

    function closeUploadModal() {
        if (isUploading) return;
        const modal = document.getElementById('upload-modal');
        if (modal) {
            modal.classList.add('hidden');
            document.body.style.overflow = '';
        }
        clearFileSelection();
        resetUploadUI();
    }

    // ── File Selection ───────────────────────────────────────

    function handleFileSelect(input) {
        const file = input.files[0];
        if (!file) return;

        if (file.size > 10 * 1024 * 1024) {
            Toast.error('檔案大小超過 10MB 限制');
            input.value = '';
            return;
        }

        const preview = document.getElementById('file-preview');
        const previewIcon = document.getElementById('preview-icon');
        const previewName = document.getElementById('preview-name');
        const previewSize = document.getElementById('preview-size');

        if (previewName) previewName.textContent = file.name;
        if (previewSize) previewSize.textContent = formatFileSize(file.size);

        if (previewIcon) {
            if (file.type.startsWith('image/')) {
                previewIcon.innerHTML = '<svg class="w-6 h-6 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>';
            } else if (file.type === 'application/pdf') {
                previewIcon.innerHTML = '<svg class="w-6 h-6 text-red-500" fill="currentColor" viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6zm-1 2l5 5h-5V4z"/></svg>';
            } else {
                previewIcon.innerHTML = '<svg class="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>';
            }
        }

        if (preview) preview.classList.remove('hidden');
        const dropZone = document.getElementById('drop-zone');
        if (dropZone) dropZone.classList.add('hidden');
        const submitBtn = document.getElementById('submit-btn');
        if (submitBtn) submitBtn.disabled = false;
    }

    function clearFileSelection() {
        const fileInput = document.getElementById('file-input');
        const filePreview = document.getElementById('file-preview');
        const dropZone = document.getElementById('drop-zone');
        const submitBtn = document.getElementById('submit-btn');
        const category = document.getElementById('category');
        const description = document.getElementById('description');

        if (fileInput) fileInput.value = '';
        if (filePreview) filePreview.classList.add('hidden');
        if (dropZone) dropZone.classList.remove('hidden');
        if (submitBtn) submitBtn.disabled = true;
        if (category) category.value = 'other';
        if (description) description.value = '';
    }

    // ── Upload UI Reset ──────────────────────────────────────

    function resetUploadUI() {
        isUploading = false;
        const progressEl = document.getElementById('upload-progress');
        const progressBar = document.getElementById('progress-bar');
        const progressText = document.getElementById('progress-text');
        const submitBtn = document.getElementById('submit-btn');
        const cancelBtn = document.getElementById('cancel-btn');
        const category = document.getElementById('category');
        const description = document.getElementById('description');

        if (progressEl) progressEl.classList.add('hidden');
        if (progressBar) progressBar.style.width = '0%';
        if (progressText) progressText.textContent = '0%';

        setButtonDisabled(submitBtn, false);

        if (cancelBtn) {
            cancelBtn.classList.remove('opacity-50', 'pointer-events-none');
        }
        if (category) category.disabled = false;
        if (description) description.disabled = false;
    }

    // ── Upload Success ───────────────────────────────────────

    function showUploadSuccess() {
        isUploading = false;
        closeUploadModal();

        const overlay = document.getElementById('upload-success-overlay');
        if (!overlay) return;

        overlay.classList.remove('opacity-0', 'pointer-events-none');
        overlay.classList.add('opacity-100');

        const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

        if (prefersReducedMotion) {
            const lottieEl = document.getElementById('success-lottie');
            const fallbackEl = document.getElementById('success-fallback');
            if (lottieEl) lottieEl.classList.add('hidden');
            if (fallbackEl) fallbackEl.classList.remove('hidden');
            setTimeout(function() { window.location.reload(); }, 1500);
            return;
        }

        const container = document.getElementById('success-lottie');
        try {
            if (typeof lottie !== 'undefined') {
                const anim = lottie.loadAnimation({
                    container: container,
                    renderer: 'svg',
                    loop: false,
                    autoplay: true,
                    path: '/animations/Success.json'
                });
                anim.addEventListener('complete', function() {
                    setTimeout(function() { window.location.reload(); }, 500);
                });
                anim.addEventListener('error', function() {
                    container.classList.add('hidden');
                    document.getElementById('success-fallback').classList.remove('hidden');
                    setTimeout(function() { window.location.reload(); }, 1500);
                });
                setTimeout(function() { window.location.reload(); }, 5000);
            } else {
                throw new Error('lottie not loaded');
            }
        } catch (e) {
            container.classList.add('hidden');
            document.getElementById('success-fallback').classList.remove('hidden');
            setTimeout(function() { window.location.reload(); }, 1500);
        }
    }

    // ── Upload Document (XHR for progress) ───────────────────

    function uploadDocument(file, category, description) {
        const formData = new FormData();
        formData.append('file', file);

        const metadata = { category: category, description: description };
        formData.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));

        isUploading = true;
        const progressEl = document.getElementById('upload-progress');
        const filePreview = document.getElementById('file-preview');
        const submitBtn = document.getElementById('submit-btn');
        const cancelBtn = document.getElementById('cancel-btn');
        const categoryEl = document.getElementById('category');
        const descriptionEl = document.getElementById('description');

        if (progressEl) progressEl.classList.remove('hidden');
        if (filePreview) filePreview.classList.add('hidden');

        setButtonDisabled(submitBtn, true, '上傳中...');

        if (cancelBtn) cancelBtn.classList.add('opacity-50', 'pointer-events-none');
        if (categoryEl) categoryEl.disabled = true;
        if (descriptionEl) descriptionEl.disabled = true;

        try {
            const xhr = new XMLHttpRequest();

            xhr.upload.addEventListener('progress', function(e) {
                if (e.lengthComputable) {
                    const percent = Math.round((e.loaded / e.total) * 100);
                    const bar = document.getElementById('progress-bar');
                    const text = document.getElementById('progress-text');
                    if (bar) bar.style.width = percent + '%';
                    if (text) text.textContent = percent + '%';
                }
            });

            xhr.onload = function() {
                if (xhr.status >= 200 && xhr.status < 300) {
                    showUploadSuccess();
                } else {
                    try {
                        const error = JSON.parse(xhr.responseText);
                        Toast.error(error.message || '上傳失敗');
                    } catch (e) {
                        Toast.error('上傳失敗，請稍後再試');
                    }
                    resetUploadUI();
                    if (filePreview) filePreview.classList.remove('hidden');
                }
            };

            xhr.onerror = function() {
                Toast.error('網路錯誤，請稍後再試');
                resetUploadUI();
                if (filePreview) filePreview.classList.remove('hidden');
            };

            xhr.open('POST', '/api/trips/' + tripId + '/documents');
            xhr.setRequestHeader(WeGo.getCsrfHeader(), WeGo.getCsrfToken());
            xhr.send(formData);

        } catch (error) {
            Toast.error('上傳失敗，請稍後再試');
            resetUploadUI();
            if (filePreview) filePreview.classList.remove('hidden');
        }
    }

    // ── Drag and Drop ────────────────────────────────────────

    function initDragAndDrop() {
        const dropZone = document.getElementById('drop-zone');
        if (!dropZone) return;

        function preventDefaults(e) {
            e.preventDefault();
            e.stopPropagation();
        }

        ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(function(eventName) {
            dropZone.addEventListener(eventName, preventDefaults, false);
        });

        ['dragenter', 'dragover'].forEach(function(eventName) {
            dropZone.addEventListener(eventName, function() {
                dropZone.classList.add('border-primary-500', 'bg-primary-50', 'dark:bg-primary-900/20');
            }, false);
        });

        ['dragleave', 'drop'].forEach(function(eventName) {
            dropZone.addEventListener(eventName, function() {
                dropZone.classList.remove('border-primary-500', 'bg-primary-50', 'dark:bg-primary-900/20');
            }, false);
        });

        dropZone.addEventListener('drop', function(e) {
            const files = e.dataTransfer.files;
            if (files.length > 0) {
                const fileInput = document.getElementById('file-input');
                if (fileInput) {
                    fileInput.files = files;
                    handleFileSelect(fileInput);
                }
            }
        }, false);
    }

    // ── Category Filter ──────────────────────────────────────

    function filterByCategory(category) {
        const buttons = document.querySelectorAll('[data-category]');
        const cards = document.querySelectorAll('.document-card');

        buttons.forEach(function(btn) {
            if (btn.dataset.category === category) {
                btn.classList.add('bg-primary-500', 'text-white');
                btn.classList.remove('bg-white', 'dark:bg-gray-800', 'text-gray-700', 'dark:text-gray-300', 'border');
            } else {
                btn.classList.remove('bg-primary-500', 'text-white');
                btn.classList.add('bg-white', 'dark:bg-gray-800', 'text-gray-700', 'dark:text-gray-300', 'border');
            }
        });

        cards.forEach(function(card) {
            if (category === 'all' || card.dataset.category === category) {
                card.classList.remove('hidden');
            } else {
                card.classList.add('hidden');
            }
        });
    }

    // ── Document Detail Modal ────────────────────────────────

    function openDocumentDetail(card) {
        currentDocumentId = card.dataset.id;
        const mimeType = card.dataset.mime || '';

        const fileName = card.querySelector('h3').textContent;
        const fileSize = card.querySelector('.text-xs span:first-child').textContent;
        const uploadDate = card.querySelector('.text-xs span:last-child').textContent;
        const isPdf = mimeType === 'application/pdf';
        const isImage = mimeType.startsWith('image/');

        const titleEl = document.getElementById('detail-title');
        const infoEl = document.getElementById('detail-info');
        if (titleEl) titleEl.textContent = fileName;
        if (infoEl) infoEl.textContent = fileSize + ' - 上傳於 ' + uploadDate;

        const previewArea = document.getElementById('detail-preview');
        if (previewArea) previewArea.innerHTML = '';

        const modal = document.getElementById('detail-modal');
        if (modal) {
            modal.classList.remove('hidden');
            document.body.style.overflow = 'hidden';
        }

        const signedUrl = card.dataset.signedUrl || '';

        if (isImage && previewArea) {
            const img = document.createElement('img');
            img.src = signedUrl || '/api/trips/' + tripId + '/documents/' + currentDocumentId + '/preview';
            img.alt = fileName;
            img.className = 'max-w-full max-h-full object-contain rounded-lg';
            previewArea.appendChild(img);
        } else if (isPdf && previewArea) {
            const previewUrl = signedUrl || '/api/trips/' + tripId + '/documents/' + currentDocumentId + '/preview';
            renderPdfPreview(previewArea, previewUrl);
        } else if (previewArea) {
            renderGenericPreview(previewArea);
        }
    }

    function renderPdfPreview(container, url) {
        container.innerHTML = '';
        const iframe = document.createElement('iframe');
        iframe.src = url;
        iframe.className = 'w-full h-full rounded-xl';
        iframe.style.border = 'none';
        iframe.style.minHeight = '400px';
        iframe.title = 'PDF Preview';

        let loaded = false;

        iframe.onload = function() {
            loaded = true;
        };

        iframe.onerror = function() {
            renderPreviewError(container);
        };

        setTimeout(function() {
            if (!loaded) {
                renderPreviewError(container);
            }
        }, 15000);

        container.appendChild(iframe);
    }

    function renderPreviewError(container) {
        container.innerHTML = '';
        const wrapper = document.createElement('div');
        wrapper.className = 'text-center';
        wrapper.setAttribute('role', 'alert');
        wrapper.innerHTML = '<svg class="w-20 h-20 mx-auto text-gray-400 dark:text-gray-500 mb-4" fill="currentColor" viewBox="0 0 24 24">' +
            '<path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6zm-1 2l5 5h-5V4z"/>' +
            '</svg>';
        const label = document.createElement('p');
        label.className = 'text-gray-500 dark:text-gray-400 mb-4';
        label.textContent = '無法預覽此檔案';
        wrapper.appendChild(label);
        container.appendChild(wrapper);
    }

    function renderGenericPreview(container) {
        container.innerHTML = '';
        const wrapper = document.createElement('div');
        wrapper.className = 'text-center';
        wrapper.innerHTML = '<svg class="w-24 h-24 mx-auto text-gray-400 dark:text-gray-500 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">' +
            '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>' +
            '</svg>';
        const label = document.createElement('p');
        label.className = 'text-gray-600 dark:text-gray-400';
        label.textContent = '此檔案類型不支援預覽';
        wrapper.appendChild(label);
        container.appendChild(wrapper);
    }

    function closeDetailModal() {
        const modal = document.getElementById('detail-modal');
        if (modal) modal.classList.add('hidden');
        document.body.style.overflow = '';
        currentDocumentId = null;
    }

    // ── Delete Document ──────────────────────────────────────

    function confirmDelete() {
        if (!currentDocumentId) return;
        const modal = document.getElementById('delete-confirm-modal');
        if (modal) modal.classList.remove('hidden');
    }

    function closeDeleteConfirmModal() {
        const modal = document.getElementById('delete-confirm-modal');
        if (modal) modal.classList.add('hidden');
        const btn = document.getElementById('confirm-delete-btn');
        if (btn) {
            btn.disabled = false;
            btn.textContent = '刪除';
        }
    }

    function executeDelete() {
        const deleteBtn = document.getElementById('confirm-delete-btn');
        if (!deleteBtn || deleteBtn.disabled) return;

        deleteBtn.disabled = true;
        deleteBtn.textContent = '刪除中...';

        const headers = {};
        headers[WeGo.getCsrfHeader()] = WeGo.getCsrfToken();

        WeGo.fetchWithTimeout('/api/trips/' + tripId + '/documents/' + currentDocumentId, {
            method: 'DELETE',
            headers: headers
        })
        .then(function(response) {
            if (response.ok) {
                const deletedId = currentDocumentId;
                closeDeleteConfirmModal();
                closeDetailModal();
                Toast.success('檔案已刪除');

                const card = document.querySelector('[data-id="' + deletedId + '"]');
                if (card) {
                    card.style.transition = 'opacity 200ms ease-out, transform 200ms ease-out';
                    card.style.opacity = '0';
                    card.style.transform = 'scale(0.95)';
                }
                setTimeout(function() { window.location.reload(); }, 500);
            } else {
                return response.json().then(function(error) {
                    Toast.error(error.message || '刪除失敗');
                }).catch(function() {
                    Toast.error('刪除失敗，請稍後再試');
                }).finally(function() {
                    deleteBtn.disabled = false;
                    deleteBtn.textContent = '刪除';
                });
            }
        })
        .catch(function() {
            deleteBtn.disabled = false;
            deleteBtn.textContent = '刪除';
            Toast.error('刪除失敗，請稍後再試');
        });
    }

    // ── Sort ─────────────────────────────────────────────────

    function sortDocuments(sortBy) {
        const grid = document.querySelector('.grid');
        if (!grid) return;
        const cards = Array.from(grid.querySelectorAll('.document-card'));

        cards.sort(function(a, b) {
            switch (sortBy) {
                case 'date-desc': return b.dataset.date.localeCompare(a.dataset.date);
                case 'date-asc':  return a.dataset.date.localeCompare(b.dataset.date);
                case 'name-asc':  return a.dataset.name.localeCompare(b.dataset.name, 'zh-TW');
                case 'name-desc': return b.dataset.name.localeCompare(a.dataset.name, 'zh-TW');
                case 'size-desc': return Number(b.dataset.size) - Number(a.dataset.size);
                default: return 0;
            }
        });

        cards.forEach(function(card) { grid.appendChild(card); });
    }

    // ── Event Delegation ─────────────────────────────────────

    function handleAction(actionName, target) {
        switch (actionName) {
            case 'open-upload-modal':
                openUploadModal();
                break;
            case 'close-upload-modal':
                closeUploadModal();
                break;
            case 'clear-file-selection':
                clearFileSelection();
                break;
            case 'filter-category':
                filterByCategory(target.dataset.category);
                break;
            case 'open-document-detail':
                openDocumentDetail(target.closest('.document-card'));
                break;
            case 'close-detail-modal':
                closeDetailModal();
                break;
            case 'confirm-delete':
                confirmDelete();
                break;
            case 'close-delete-confirm':
                closeDeleteConfirmModal();
                break;
            case 'execute-delete':
                executeDelete();
                break;
            default:
                break;
        }
    }

    // ── Initialization ───────────────────────────────────────

    function init() {
        // Read tripId from data attribute on main content
        const mainContent = document.getElementById('main-content');
        tripId = mainContent ? mainContent.dataset.tripId : null;

        // If tripId not on main, try hidden input fallback
        if (!tripId) {
            const hiddenInput = document.querySelector('input[name="tripId"]');
            tripId = hiddenInput ? hiddenInput.value : null;
        }

        // Event delegation for all data-action clicks
        document.addEventListener('click', function(e) {
            const actionEl = e.target.closest('[data-action]');
            if (actionEl) {
                handleAction(actionEl.dataset.action, actionEl);
            }
        });

        // File input change handler
        const fileInput = document.getElementById('file-input');
        if (fileInput) {
            fileInput.addEventListener('change', function() {
                handleFileSelect(this);
            });
        }

        // Upload form submit handler
        const uploadForm = document.getElementById('upload-form');
        if (uploadForm) {
            uploadForm.addEventListener('submit', function(e) {
                e.preventDefault();

                const fi = document.getElementById('file-input');
                const file = fi ? fi.files[0] : null;
                if (!file) {
                    Toast.error('請選擇要上傳的檔案');
                    return;
                }

                const category = document.getElementById('category').value;
                const description = document.getElementById('description').value;
                uploadDocument(file, category, description);
            });
        }

        // Sort select handler
        const sortSelect = document.getElementById('sort-select');
        if (sortSelect) {
            sortSelect.addEventListener('change', function() {
                sortDocuments(this.value);
            });
        }

        // Escape key handler
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') {
                closeDeleteConfirmModal();
                closeUploadModal();
                closeDetailModal();
            }
        });

        // Drag and drop
        initDragAndDrop();
    }

    return { init: init };
})();

document.addEventListener('DOMContentLoaded', DocumentList.init);
