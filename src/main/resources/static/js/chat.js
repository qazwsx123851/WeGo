/**
 * WeGo AI Chat Widget
 * Handles chat window interaction, message sending, typing animation.
 * Mobile: bottom sheet (slide-up, backdrop)
 * Desktop: floating card (scale animation)
 *
 * Dependencies: WeGo.escapeHtml(), WeGo.getCsrfToken(), WeGo.getCsrfHeader(), WeGo.fetchWithTimeout()
 */
(function() {
    'use strict';

    var toggleBtn = document.getElementById('chat-toggle-btn');
    var chatWindow = document.getElementById('chat-window');
    var messagesArea = document.getElementById('chat-messages');
    var chatInput = document.getElementById('chat-input');
    var sendBtn = document.getElementById('chat-send-btn');
    var charCount = document.getElementById('chat-char-count');
    var iconOpen = document.getElementById('chat-icon-open');
    var iconClose = document.getElementById('chat-icon-close');
    var backdrop = document.getElementById('chat-backdrop');
    var closeHeaderBtn = document.getElementById('chat-close-header');

    if (!toggleBtn || !chatWindow) return;

    var tripId = toggleBtn.getAttribute('data-trip-id');
    var isOpen = false;
    var isSending = false;
    var MAX_LENGTH = 500;
    var TYPING_SPEED_MS = 30;
    var desktopQuery = window.matchMedia('(min-width: 640px)');

    // --- Open / Close Chat ---
    function openChat() {
        isOpen = true;
        toggleBtn.classList.add('hidden');
        chatWindow.classList.remove('opacity-0', 'pointer-events-none');
        chatWindow.classList.add('opacity-100', 'pointer-events-auto');

        if (desktopQuery.matches) {
            // Desktop: scale animation
            chatWindow.classList.remove('sm:scale-0');
            chatWindow.classList.add('sm:scale-100');
        } else {
            // Mobile: slide-up + backdrop
            chatWindow.classList.remove('translate-y-full');
            chatWindow.classList.add('translate-y-0');
            if (backdrop) {
                backdrop.classList.remove('hidden');
            }
        }
        chatInput.focus();
    }

    function closeChat() {
        isOpen = false;
        toggleBtn.classList.remove('hidden');
        chatWindow.classList.add('opacity-0', 'pointer-events-none');
        chatWindow.classList.remove('opacity-100', 'pointer-events-auto');

        if (desktopQuery.matches) {
            // Desktop: scale animation
            chatWindow.classList.add('sm:scale-0');
            chatWindow.classList.remove('sm:scale-100');
        } else {
            // Mobile: slide-down + hide backdrop
            chatWindow.classList.add('translate-y-full');
            chatWindow.classList.remove('translate-y-0');
            if (backdrop) {
                backdrop.classList.add('hidden');
            }
        }
    }

    // --- Event Listeners ---
    toggleBtn.addEventListener('click', function() {
        if (isOpen) {
            closeChat();
        } else {
            openChat();
        }
    });

    if (backdrop) {
        backdrop.addEventListener('click', closeChat);
    }

    if (closeHeaderBtn) {
        closeHeaderBtn.addEventListener('click', closeChat);
    }

    // --- Character Counter ---
    chatInput.addEventListener('input', function() {
        var len = chatInput.value.length;
        charCount.textContent = len + '/' + MAX_LENGTH;

        if (len >= MAX_LENGTH * 0.9) {
            charCount.classList.add('text-red-500');
            charCount.classList.remove('text-amber-500', 'text-gray-400', 'dark:text-gray-500');
        } else if (len >= MAX_LENGTH * 0.7) {
            charCount.classList.add('text-amber-500');
            charCount.classList.remove('text-red-500', 'text-gray-400', 'dark:text-gray-500');
        } else {
            charCount.classList.remove('text-red-500', 'text-amber-500');
            charCount.classList.add('text-gray-400', 'dark:text-gray-500');
        }

        // Enable/disable send button
        sendBtn.disabled = chatInput.value.trim().length === 0 || isSending;

        // Auto-resize textarea
        chatInput.style.height = 'auto';
        chatInput.style.height = Math.min(chatInput.scrollHeight, 80) + 'px';
    });

    // --- Send on Enter (Shift+Enter for newline) ---
    chatInput.addEventListener('keydown', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            if (!sendBtn.disabled) {
                sendMessage();
            }
        }
    });

    sendBtn.addEventListener('click', function() {
        if (!sendBtn.disabled) {
            sendMessage();
        }
    });

    // --- Send Message ---
    function sendMessage() {
        var message = chatInput.value.trim();
        if (!message || isSending) return;

        isSending = true;
        sendBtn.disabled = true;
        chatInput.disabled = true;

        // Display user message
        appendUserMessage(message);

        // Clear input
        chatInput.value = '';
        charCount.textContent = '0/' + MAX_LENGTH;
        charCount.classList.remove('text-red-500', 'text-amber-500');
        charCount.classList.add('text-gray-400', 'dark:text-gray-500');
        chatInput.style.height = 'auto';

        // Show loading dots
        var dotsEl = appendLoadingDots();

        // Send to API
        var headers = {
            'Content-Type': 'application/json'
        };
        headers[WeGo.getCsrfHeader()] = WeGo.getCsrfToken();

        WeGo.fetchWithTimeout(
            '/api/trips/' + tripId + '/chat',
            {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({ message: message })
            },
            35000
        ).then(function(response) {
            return response.json().then(function(data) {
                return { status: response.status, data: data };
            });
        }).then(function(result) {
            removeLoadingDots(dotsEl);

            if (result.status === 200 && result.data.success) {
                typeMessage(result.data.data.reply);
            } else {
                var errorMsg = getErrorMessage(result.status, result.data);
                appendBotMessage(errorMsg);
                enableInput();
            }
        }).catch(function(error) {
            removeLoadingDots(dotsEl);
            var msg = error.message === 'Request timed out'
                ? '請求逾時，請稍後再試。'
                : '網路連線異常，請確認網路後再試。';
            appendBotMessage(msg);
            enableInput();
        });
    }

    // --- Error Messages ---
    function getErrorMessage(status, data) {
        if (status === 429 || (data && data.errorCode === 'RATE_LIMITED')) {
            return '請求太頻繁，請稍後再試。';
        }
        if (status === 403) {
            return '你沒有權限使用此行程的聊天功能。';
        }
        if (status === 400) {
            return data && data.message ? data.message : '輸入內容有誤，請重新輸入。';
        }
        return '系統忙碌中，請稍後再試。';
    }

    // --- DOM Helpers ---
    function appendUserMessage(text) {
        var wrapper = document.createElement('div');
        wrapper.className = 'flex justify-end';

        var bubble = document.createElement('div');
        bubble.className = 'bg-primary-500 text-white rounded-2xl rounded-tr-sm px-3 py-2 max-w-[80%]';

        var p = document.createElement('p');
        p.className = 'text-sm whitespace-pre-wrap break-words';
        p.textContent = text;

        bubble.appendChild(p);
        wrapper.appendChild(bubble);
        messagesArea.appendChild(wrapper);
        scrollToBottom();
    }

    function appendBotMessage(text) {
        var wrapper = document.createElement('div');
        wrapper.className = 'flex gap-2';

        var avatar = createBotAvatar();
        wrapper.appendChild(avatar);

        var bubble = document.createElement('div');
        bubble.className = 'bg-gray-100 dark:bg-gray-700 rounded-2xl rounded-tl-sm px-3 py-2 max-w-[80%]';

        var p = document.createElement('p');
        p.className = 'text-sm text-gray-800 dark:text-gray-200 whitespace-pre-wrap break-words';
        p.innerHTML = formatReply(text);

        bubble.appendChild(p);
        wrapper.appendChild(bubble);
        messagesArea.appendChild(wrapper);
        scrollToBottom();
    }

    function appendLoadingDots() {
        var wrapper = document.createElement('div');
        wrapper.className = 'flex gap-2';
        wrapper.id = 'chat-loading-dots';

        var avatar = createBotAvatar();
        wrapper.appendChild(avatar);

        var bubble = document.createElement('div');
        bubble.className = 'bg-gray-100 dark:bg-gray-700 rounded-2xl rounded-tl-sm px-4 py-3';

        var dotsContainer = document.createElement('div');
        dotsContainer.className = 'flex gap-1 items-center';

        for (var i = 0; i < 3; i++) {
            var dot = document.createElement('span');
            dot.className = 'w-2 h-2 rounded-full bg-gray-400 dark:bg-gray-500 animate-bounce';
            dot.style.animationDelay = (i * 0.15) + 's';
            dot.style.animationDuration = '0.6s';
            dotsContainer.appendChild(dot);
        }

        bubble.appendChild(dotsContainer);
        wrapper.appendChild(bubble);
        messagesArea.appendChild(wrapper);
        scrollToBottom();
        return wrapper;
    }

    function removeLoadingDots(el) {
        if (el && el.parentNode) {
            el.parentNode.removeChild(el);
        }
    }

    function createBotAvatar() {
        var avatar = document.createElement('div');
        avatar.className = 'w-7 h-7 rounded-full bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center flex-shrink-0 mt-0.5';
        avatar.innerHTML = '<svg class="w-4 h-4 text-primary-600 dark:text-primary-400" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">'
            + '<path d="M11.04 19.32Q12 21.51 12 24q0-2.49.93-4.68.96-2.19 2.58-3.81t3.81-2.55Q21.51 12 24 12q-2.49 0-4.68-.93a12.3 12.3 0 0 1-3.81-2.58 12.3 12.3 0 0 1-2.58-3.81Q12 2.49 12 0q0 2.49-.96 4.68-.93 2.19-2.55 3.81a12.3 12.3 0 0 1-3.81 2.58Q2.49 12 0 12q2.49 0 4.68.96 2.19.93 3.81 2.55t2.55 3.81"/>'
            + '</svg>';
        return avatar;
    }

    // --- Typing Animation ---
    function typeMessage(fullText) {
        var wrapper = document.createElement('div');
        wrapper.className = 'flex gap-2';

        var avatar = createBotAvatar();
        wrapper.appendChild(avatar);

        var bubble = document.createElement('div');
        bubble.className = 'bg-gray-100 dark:bg-gray-700 rounded-2xl rounded-tl-sm px-3 py-2 max-w-[80%]';

        var p = document.createElement('p');
        p.className = 'text-sm text-gray-800 dark:text-gray-200 whitespace-pre-wrap break-words';

        bubble.appendChild(p);
        wrapper.appendChild(bubble);
        messagesArea.appendChild(wrapper);

        var formatted = formatReply(fullText);
        // Parse HTML to extract text nodes and tags
        var temp = document.createElement('div');
        temp.innerHTML = formatted;
        var plainText = temp.textContent || '';
        var idx = 0;

        var timer = setInterval(function() {
            if (idx < plainText.length) {
                idx += 1;
                p.textContent = plainText.substring(0, idx);
                scrollToBottom();
            } else {
                clearInterval(timer);
                // Replace with formatted HTML after typing completes
                p.innerHTML = formatted;
                enableInput();
                scrollToBottom();
            }
        }, TYPING_SPEED_MS);
    }

    // --- Format Reply (bold markers) ---
    function formatReply(text) {
        if (!text) return '';
        // Escape HTML first
        var escaped = WeGo.escapeHtml(text);
        // Convert **bold** to <strong>
        escaped = escaped.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
        // Convert newlines to <br>
        escaped = escaped.replace(/\n/g, '<br>');
        return escaped;
    }

    function enableInput() {
        isSending = false;
        chatInput.disabled = false;
        sendBtn.disabled = chatInput.value.trim().length === 0;
        chatInput.focus();
    }

    function scrollToBottom() {
        messagesArea.scrollTop = messagesArea.scrollHeight;
    }

})();
