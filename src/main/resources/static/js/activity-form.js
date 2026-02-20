/**
 * WeGo - Activity Form JavaScript Module
 *
 * Handles activity create/edit form interactions:
 * - Duration display calculation
 * - Place search autocomplete
 * - Transport mode selection with manual input toggle
 * - Delete confirmation dialog
 * - Form validation
 *
 * Integrates with global Toast utility from app.js
 * and WeGo shared utilities from common.js.
 */
const ActivityForm = (() => {
    'use strict';

    // =====================================================
    // Duration Display
    // =====================================================

    function formatMinutes(minutes) {
        const hours = Math.floor(minutes / 60);
        const mins = minutes % 60;
        if (hours > 0 && mins > 0) {
            return hours + ' \u5c0f\u6642 ' + mins + ' \u5206\u9418';
        } else if (hours > 0) {
            return hours + ' \u5c0f\u6642';
        }
        return mins + ' \u5206\u9418';
    }

    function updateDurationDisplay() {
        const durationInput = document.getElementById('durationMinutes');
        const durationDisplay = document.getElementById('duration-display');
        if (!durationInput || !durationDisplay) return;

        const minutes = parseInt(durationInput.value) || 0;
        durationDisplay.textContent = minutes > 0 ? formatMinutes(minutes) : '';
    }

    function calculateDuration() {
        const startTimeInput = document.getElementById('startTime');
        const endTimeInput = document.getElementById('endTime');
        const durationInput = document.getElementById('durationMinutes');
        if (!startTimeInput || !endTimeInput || !durationInput) return;

        const start = startTimeInput.value;
        const end = endTimeInput.value;
        if (start && end) {
            const startParts = start.split(':').map(Number);
            const endParts = end.split(':').map(Number);
            const startMinutes = startParts[0] * 60 + startParts[1];
            const endMinutes = endParts[0] * 60 + endParts[1];
            const diff = endMinutes - startMinutes;
            if (diff > 0) {
                durationInput.value = diff;
                updateDurationDisplay();
            }
        }
    }

    function initDuration() {
        const durationInput = document.getElementById('durationMinutes');
        const startTimeInput = document.getElementById('startTime');
        const endTimeInput = document.getElementById('endTime');

        if (durationInput) {
            durationInput.addEventListener('input', updateDurationDisplay);
        }
        if (startTimeInput) {
            startTimeInput.addEventListener('change', calculateDuration);
        }
        if (endTimeInput) {
            endTimeInput.addEventListener('change', calculateDuration);
        }

        updateDurationDisplay();
    }

    // =====================================================
    // Delete Dialog
    // =====================================================

    function showDeleteDialog() {
        const deleteDialog = document.getElementById('delete-dialog');
        if (deleteDialog) {
            deleteDialog.classList.remove('hidden');
        }
    }

    function hideDeleteDialog() {
        const deleteDialog = document.getElementById('delete-dialog');
        if (deleteDialog) {
            deleteDialog.classList.add('hidden');
        }
    }

    function initDeleteDialog() {
        const deleteBtn = document.getElementById('delete-activity-btn');
        const deleteDialog = document.getElementById('delete-dialog');
        const cancelDeleteBtn = document.getElementById('cancel-delete-btn');

        if (deleteBtn) {
            deleteBtn.addEventListener('click', showDeleteDialog);
        }

        if (cancelDeleteBtn) {
            cancelDeleteBtn.addEventListener('click', hideDeleteDialog);
        }

        if (deleteDialog) {
            deleteDialog.addEventListener('click', function(e) {
                if (e.target === deleteDialog) {
                    hideDeleteDialog();
                }
            });
        }
    }

    // =====================================================
    // Place Search
    // =====================================================

    let searchTimeout = null;

    function getSearchConfig() {
        return window.tripSearchConfig || { lat: 25.0330, lng: 121.5654, radius: 50000 };
    }

    function getSearchResultsEl() {
        return document.getElementById('place-search-results');
    }

    function hideSearchResults() {
        const searchResults = getSearchResultsEl();
        if (searchResults) {
            searchResults.classList.add('hidden');
            searchResults.innerHTML = '';
        }
    }

    function showSearchLoading() {
        const searchResults = getSearchResultsEl();
        if (!searchResults) return;

        searchResults.innerHTML =
            '<div class="p-4 text-center text-gray-500 dark:text-gray-400">' +
            '<svg class="animate-spin h-5 w-5 mx-auto mb-2" fill="none" viewBox="0 0 24 24">' +
            '<circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>' +
            '<path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>' +
            '</svg>' +
            '\u641c\u5c0b\u4e2d...' +
            '</div>';
        searchResults.classList.remove('hidden');
    }

    function displaySearchError(message) {
        const searchResults = getSearchResultsEl();
        if (!searchResults) return;

        searchResults.innerHTML =
            '<div class="p-4 text-center text-error">' +
            '<p>' + WeGo.escapeHtml(message) + '</p>' +
            '</div>';
        searchResults.classList.remove('hidden');
    }

    function selectPlace(element) {
        const placeNameInput = document.getElementById('placeName');
        const addressInput = document.getElementById('address');
        const placeIdInput = document.getElementById('placeId');
        const latitudeInput = document.getElementById('latitude');
        const longitudeInput = document.getElementById('longitude');

        if (placeNameInput) placeNameInput.value = element.dataset.name;
        if (addressInput) addressInput.value = element.dataset.address;
        if (placeIdInput) placeIdInput.value = element.dataset.placeId;
        if (latitudeInput) latitudeInput.value = element.dataset.lat;
        if (longitudeInput) longitudeInput.value = element.dataset.lng;

        hideSearchResults();
    }

    function displaySearchResults(places) {
        const searchResults = getSearchResultsEl();
        if (!searchResults) return;

        if (!places || places.length === 0) {
            searchResults.innerHTML =
                '<div class="p-4 text-center text-gray-500 dark:text-gray-400">' +
                '<p>\u627e\u4e0d\u5230\u7b26\u5408\u7684\u5730\u9ede</p>' +
                '<p class="text-xs mt-1">\u8acb\u5617\u8a66\u5176\u4ed6\u95dc\u9375\u5b57</p>' +
                '</div>';
            searchResults.classList.remove('hidden');
            return;
        }

        const html = places.map(function(place) {
            const name = WeGo.escapeHtml(place.name || '');
            const address = WeGo.escapeHtml(place.address || place.formattedAddress || '');
            const placeId = WeGo.escapeHtml(place.placeId || place.googlePlaceId || '');
            const lat = place.latitude || place.lat || '';
            const lng = place.longitude || place.lng || '';

            return '<button type="button"' +
                ' class="w-full px-4 py-3 text-left hover:bg-gray-50 dark:hover:bg-gray-700' +
                ' border-b border-gray-100 dark:border-gray-700 last:border-b-0' +
                ' cursor-pointer transition-colors duration-150"' +
                ' data-action="select-place"' +
                ' data-name="' + name + '"' +
                ' data-address="' + address + '"' +
                ' data-place-id="' + placeId + '"' +
                ' data-lat="' + lat + '"' +
                ' data-lng="' + lng + '">' +
                '<div class="font-medium text-gray-800 dark:text-gray-100">' + name + '</div>' +
                '<div class="text-sm text-gray-500 dark:text-gray-400 truncate">' + address + '</div>' +
                '</button>';
        }).join('');

        searchResults.innerHTML = html;
        searchResults.classList.remove('hidden');
    }

    function searchPlaces(query) {
        if (!query || query.trim().length < 2) {
            hideSearchResults();
            return;
        }

        const config = getSearchConfig();

        showSearchLoading();
        WeGo.fetchWithTimeout(
            '/api/places/search?query=' + encodeURIComponent(query.trim()) +
            '&lat=' + config.lat + '&lng=' + config.lng + '&radius=' + config.radius
        )
        .then(function(response) {
            if (!response.ok) {
                throw new Error('Search failed');
            }
            return response.json();
        })
        .then(function(data) {
            displaySearchResults(data.data || data);
        })
        .catch(function() {
            displaySearchError('\u641c\u5c0b\u6642\u767c\u751f\u932f\u8aa4\uff0c\u8acb\u7a0d\u5f8c\u518d\u8a66');
        });
    }

    function initPlaceSearch() {
        const placeNameInput = document.getElementById('placeName');
        const searchBtn = document.getElementById('place-search-btn');
        const searchResults = getSearchResultsEl();

        if (searchBtn) {
            searchBtn.addEventListener('click', function() {
                const query = placeNameInput ? placeNameInput.value : '';
                searchPlaces(query);
            });
        }

        if (placeNameInput) {
            placeNameInput.addEventListener('input', function() {
                clearTimeout(searchTimeout);
                const input = this;
                searchTimeout = setTimeout(function() {
                    const query = input.value;
                    if (query.length >= 2) {
                        searchPlaces(query);
                    } else {
                        hideSearchResults();
                    }
                }, 300);
            });

            placeNameInput.addEventListener('keydown', function(e) {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    searchPlaces(this.value);
                }
            });
        }

        // Close search results when clicking outside
        document.addEventListener('click', function(e) {
            if (!searchResults) return;
            if (!searchResults.contains(e.target) &&
                e.target !== placeNameInput &&
                e.target !== searchBtn &&
                (searchBtn && !searchBtn.contains(e.target))) {
                hideSearchResults();
            }
        });
    }

    // =====================================================
    // Transport Mode
    // =====================================================

    const MANUAL_MODES = ['FLIGHT', 'HIGH_SPEED_RAIL'];
    const EXTENDED_MODES = ['FLIGHT', 'HIGH_SPEED_RAIL', 'NOT_CALCULATED'];

    function updateManualTransportDisplay() {
        const manualTransportInput = document.getElementById('manualTransportMinutes');
        const manualTransportDisplay = document.getElementById('manual-transport-display');
        if (!manualTransportInput || !manualTransportDisplay) return;

        const minutes = parseInt(manualTransportInput.value) || 0;
        manualTransportDisplay.textContent = minutes > 0 ? formatMinutes(minutes) : '';
    }

    function showManualInput() {
        const manualInputSection = document.getElementById('manual-transport-input');
        if (manualInputSection) {
            manualInputSection.classList.remove('hidden');
        }
    }

    function hideManualInput() {
        const manualInputSection = document.getElementById('manual-transport-input');
        const manualTransportInput = document.getElementById('manualTransportMinutes');
        if (manualInputSection) {
            manualInputSection.classList.add('hidden');
        }
        if (manualTransportInput) {
            manualTransportInput.value = '';
            updateManualTransportDisplay();
        }
    }

    function expandExtendedOptions() {
        const extendedOptions = document.getElementById('transport-mode-extended');
        const toggleMoreIcon = document.getElementById('toggle-more-icon');
        const toggleMoreText = document.getElementById('toggle-more-text');

        if (extendedOptions && extendedOptions.classList.contains('hidden')) {
            extendedOptions.classList.remove('hidden');
            if (toggleMoreIcon) toggleMoreIcon.classList.add('rotate-180');
            if (toggleMoreText) toggleMoreText.textContent = '\u6536\u8d77\u9078\u9805';
        }
    }

    function initTransportMode() {
        const toggleMoreBtn = document.getElementById('toggle-more-transport');
        const toggleMoreText = document.getElementById('toggle-more-text');
        const toggleMoreIcon = document.getElementById('toggle-more-icon');
        const extendedOptions = document.getElementById('transport-mode-extended');
        const manualTransportInput = document.getElementById('manualTransportMinutes');
        const transportModeInputs = document.querySelectorAll('input[name="transportMode"]');

        // Toggle extended options
        if (toggleMoreBtn && extendedOptions) {
            toggleMoreBtn.addEventListener('click', function() {
                const isHidden = extendedOptions.classList.contains('hidden');
                extendedOptions.classList.toggle('hidden');
                if (toggleMoreIcon) toggleMoreIcon.classList.toggle('rotate-180');
                if (toggleMoreText) toggleMoreText.textContent = isHidden ? '\u6536\u8d77\u9078\u9805' : '\u66f4\u591a\u9078\u9805';
            });
        }

        // Handle transport mode changes
        transportModeInputs.forEach(function(input) {
            input.addEventListener('change', function() {
                const selectedMode = this.value;
                const requiresManual = MANUAL_MODES.includes(selectedMode);

                if (requiresManual) {
                    showManualInput();
                } else {
                    hideManualInput();
                }

                if (EXTENDED_MODES.includes(selectedMode)) {
                    expandExtendedOptions();
                }
            });
        });

        if (manualTransportInput) {
            manualTransportInput.addEventListener('input', updateManualTransportDisplay);
            updateManualTransportDisplay();
        }

        // Initialize: Check if current mode requires manual input
        const currentMode = document.querySelector('input[name="transportMode"]:checked');
        if (currentMode && MANUAL_MODES.includes(currentMode.value)) {
            showManualInput();
            expandExtendedOptions();
        }
    }

    // =====================================================
    // Form Validation
    // =====================================================

    function initFormValidation() {
        const activityForm = document.getElementById('activity-form');
        if (!activityForm) return;

        activityForm.addEventListener('submit', function(e) {
            const selectedMode = document.querySelector('input[name="transportMode"]:checked');
            if (selectedMode && MANUAL_MODES.includes(selectedMode.value)) {
                const manualTransportInput = document.getElementById('manualTransportMinutes');
                const manualInputSection = document.getElementById('manual-transport-input');
                const manualMinutes = manualTransportInput ? parseInt(manualTransportInput.value) : 0;

                if (!manualMinutes || manualMinutes <= 0) {
                    e.preventDefault();

                    if (manualInputSection) {
                        manualInputSection.classList.remove('hidden');
                        manualInputSection.classList.add('ring-2', 'ring-red-500');
                        setTimeout(function() {
                            manualInputSection.classList.remove('ring-2', 'ring-red-500');
                        }, 3000);
                    }
                    if (manualTransportInput) {
                        manualTransportInput.focus();
                        manualTransportInput.classList.add('border-red-500');
                        setTimeout(function() {
                            manualTransportInput.classList.remove('border-red-500');
                        }, 3000);
                    }

                    Toast.error('\u9078\u64c7\u98db\u6a5f\u6216\u9ad8\u9435\u6642\uff0c\u5fc5\u9808\u8f38\u5165\u9810\u4f30\u4ea4\u901a\u6642\u9593');
                    return false;
                }
            }
        });
    }

    // =====================================================
    // Event Delegation
    // =====================================================

    function handleActions(e) {
        const target = e.target.closest('[data-action]');
        if (!target) return;

        const action = target.dataset.action;
        if (action === 'select-place') {
            selectPlace(target);
        }
    }

    // =====================================================
    // Init
    // =====================================================

    function init() {
        initDuration();
        initDeleteDialog();
        initPlaceSearch();
        initTransportMode();
        initFormValidation();

        document.addEventListener('click', handleActions);
    }

    return { init: init };
})();

document.addEventListener('DOMContentLoaded', ActivityForm.init);
