/**
 * WeGo Common Utilities
 * Shared across all pages to eliminate code duplication.
 *
 * @module WeGo
 */
(function() {
    'use strict';

    const WeGo = {};

    /**
     * Escapes HTML entities to prevent XSS.
     *
     * Handles: & < > " ' (all 5 critical HTML entities)
     *
     * @param {string} text - The text to escape
     * @returns {string} The escaped text
     */
    WeGo.escapeHtml = function(text) {
        if (text == null) return '';
        var div = document.createElement('div');
        div.textContent = String(text);
        return div.innerHTML;
    };

    /**
     * Gets CSRF token from meta tag.
     *
     * Reads from: <meta name="_csrf" content="..."/>
     *
     * @returns {string} CSRF token value
     * @throws {Error} if CSRF meta tag is not found
     */
    WeGo.getCsrfToken = function() {
        var meta = document.querySelector('meta[name="_csrf"]');
        if (!meta || !meta.getAttribute('content')) {
            throw new Error('CSRF token not found');
        }
        return meta.getAttribute('content');
    };

    /**
     * Gets CSRF header name from meta tag.
     *
     * Reads from: <meta name="_csrf_header" content="..."/>
     *
     * @returns {string} CSRF header name (default: 'X-CSRF-TOKEN')
     */
    WeGo.getCsrfHeader = function() {
        var meta = document.querySelector('meta[name="_csrf_header"]');
        return meta ? meta.getAttribute('content') : 'X-CSRF-TOKEN';
    };

    /**
     * Fetch wrapper with timeout support.
     *
     * @param {string} url - The URL to fetch
     * @param {object} [options={}] - Standard fetch options
     * @param {number} [timeoutMs=30000] - Timeout in milliseconds
     * @returns {Promise<Response>} The fetch response
     * @throws {Error} 'Request timed out' if timeout exceeded
     */
    WeGo.fetchWithTimeout = function(url, options, timeoutMs) {
        var timeout = (typeof timeoutMs === 'number' && timeoutMs > 0) ? timeoutMs : 30000;

        var controller = new AbortController();
        var merged = Object.assign({}, options || {}, { signal: controller.signal });

        var timeoutId = setTimeout(function() {
            controller.abort();
        }, timeout);

        return fetch(url, merged).then(function(response) {
            clearTimeout(timeoutId);
            return response;
        }).catch(function(error) {
            clearTimeout(timeoutId);
            if (error.name === 'AbortError') {
                throw new Error('Request timed out');
            }
            throw error;
        });
    };

    window.WeGo = WeGo;
})();
