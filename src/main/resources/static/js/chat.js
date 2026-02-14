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
    var expandBtn = document.getElementById('chat-expand-btn');
    var expandIcon = document.getElementById('chat-expand-icon');
    var collapseIcon = document.getElementById('chat-collapse-icon');

    if (!toggleBtn || !chatWindow) return;

    var tripId = toggleBtn.getAttribute('data-trip-id');
    var isOpen = false;
    var isSending = false;
    var isExpanded = false;
    var MAX_LENGTH = 500;
    var TYPING_SPEED_MS = 30;
    var desktopQuery = window.matchMedia('(min-width: 640px)');

    // --- iOS/Mobile Keyboard Handling ---
    function setupKeyboardHandling() {
        if (!window.visualViewport) return;
        var vv = window.visualViewport;
        var rafId = 0;

        function adjustForKeyboard() {
            if (!isOpen || desktopQuery.matches) return;
            var keyboardHeight = Math.round(window.innerHeight - vv.height - vv.offsetTop);

            if (keyboardHeight > 100) {
                chatWindow.style.height = vv.height + 'px';
                chatWindow.style.bottom = keyboardHeight + 'px';
            } else {
                chatWindow.style.height = '';
                chatWindow.style.bottom = '';
            }
            scrollToBottom();
        }

        function scheduleAdjust() {
            if (rafId) return;
            rafId = requestAnimationFrame(function() {
                rafId = 0;
                adjustForKeyboard();
            });
        }

        vv.addEventListener('resize', scheduleAdjust);
        vv.addEventListener('scroll', scheduleAdjust);

        // Reset on blur with delay to avoid send-button click miss
        chatInput.addEventListener('blur', function() {
            if (!isOpen || desktopQuery.matches) return;
            setTimeout(function() {
                if (document.activeElement !== chatInput) {
                    chatWindow.style.height = '';
                    chatWindow.style.bottom = '';
                }
            }, 150);
        });
    }

    setupKeyboardHandling();

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
            // Mobile: slide-up + backdrop + lock body scroll
            chatWindow.classList.remove('translate-y-full');
            chatWindow.classList.add('translate-y-0');
            if (backdrop) {
                backdrop.classList.remove('hidden');
            }
            document.body.classList.add('overflow-hidden');
        }
        chatInput.focus();
    }

    function closeChat() {
        isOpen = false;
        chatWindow.style.height = '';
        chatWindow.style.bottom = '';
        // Reset expand state (desktop only)
        if (isExpanded) {
            isExpanded = false;
            chatWindow.style.width = '';
            chatWindow.style.height = '';
            if (expandIcon) expandIcon.classList.remove('hidden');
            if (collapseIcon) collapseIcon.classList.add('hidden');
        }
        toggleBtn.classList.remove('hidden');
        chatWindow.classList.add('opacity-0', 'pointer-events-none');
        chatWindow.classList.remove('opacity-100', 'pointer-events-auto');

        if (desktopQuery.matches) {
            // Desktop: scale animation
            chatWindow.classList.add('sm:scale-0');
            chatWindow.classList.remove('sm:scale-100');
        } else {
            // Mobile: slide-down + hide backdrop + unlock body scroll
            chatWindow.classList.add('translate-y-full');
            chatWindow.classList.remove('translate-y-0');
            if (backdrop) {
                backdrop.classList.add('hidden');
            }
            document.body.classList.remove('overflow-hidden');
        }
    }

    // --- Expand / Collapse (Desktop only) ---
    // Uses window.innerHeight for reliable viewport measurement
    // (CSS dvh/vh can overflow due to browser chrome)
    function toggleExpand() {
        if (!desktopQuery.matches) return;
        isExpanded = !isExpanded;
        if (isExpanded) {
            var vh = window.innerHeight;
            var vw = window.innerWidth;
            var expandH = Math.round(vh * 0.85);
            var expandW = Math.round(Math.min(Math.max(vw * 0.4, 416), 576));
            chatWindow.style.width = expandW + 'px';
            chatWindow.style.height = expandH + 'px';
            var bottomOffset = Math.round((vh - expandH) / 2);
            chatWindow.style.bottom = Math.max(bottomOffset, 24) + 'px';
        } else {
            chatWindow.style.width = '';
            chatWindow.style.height = '';
            chatWindow.style.bottom = '';
        }
        if (expandIcon) expandIcon.classList.toggle('hidden', isExpanded);
        if (collapseIcon) collapseIcon.classList.toggle('hidden', !isExpanded);
        if (expandBtn) expandBtn.setAttribute('aria-label', isExpanded ? '收合聊天視窗' : '展開聊天視窗');
        scrollToBottom();
    }

    if (expandBtn) {
        expandBtn.addEventListener('click', toggleExpand);
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

    // --- Focus: scroll to bottom after keyboard appears ---
    chatInput.addEventListener('focus', function() {
        setTimeout(scrollToBottom, 300);
    });

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
                body: JSON.stringify({
                message: message,
                timezone: Intl.DateTimeFormat().resolvedOptions().timeZone
            })
            },
            35000
        ).then(function(response) {
            return response.json().then(function(data) {
                return { status: response.status, data: data };
            });
        }).then(function(result) {
            removeLoadingDots(dotsEl);

            if (result.status === 200 && result.data.success) {
                typeMessage(result.data.data.reply, result.data.data.sources);
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
        bubble.className = 'bg-gray-100 dark:bg-gray-700 rounded-2xl rounded-tl-sm px-3 py-2 max-w-[88%]';

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
    function typeMessage(fullText, sources) {
        var wrapper = document.createElement('div');
        wrapper.className = 'flex gap-2';

        var avatar = createBotAvatar();
        wrapper.appendChild(avatar);

        var bubble = document.createElement('div');
        bubble.className = 'bg-gray-100 dark:bg-gray-700 rounded-2xl rounded-tl-sm px-3 py-2 max-w-[88%]';

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
                // Append search sources after reply
                if (sources && sources.length > 0) {
                    bubble.appendChild(buildSourcesElement(sources));
                }
                enableInput();
                scrollToBottom();
            }
        }, TYPING_SPEED_MS);
    }

    // --- Build Sources Element ---
    function buildSourcesElement(sources) {
        var container = document.createElement('div');
        container.className = 'mt-2 pt-2 border-t border-gray-200 dark:border-gray-600 flex flex-wrap gap-1.5';

        for (var i = 0; i < sources.length; i++) {
            var source = sources[i];
            var link = document.createElement('a');
            var safeUrl = source.url || '';
            if (!/^https?:\/\//i.test(safeUrl)) {
                safeUrl = '#';
            }
            link.href = safeUrl;
            link.target = '_blank';
            link.rel = 'noopener noreferrer';
            link.className = 'inline-flex items-center gap-1 text-[11px] text-primary-600 dark:text-primary-400 '
                + 'hover:underline bg-primary-50 dark:bg-primary-900/20 rounded-full px-2 py-0.5 '
                + 'max-w-[200px] truncate';

            var icon = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
            icon.setAttribute('class', 'w-3 h-3 flex-shrink-0');
            icon.setAttribute('fill', 'none');
            icon.setAttribute('stroke', 'currentColor');
            icon.setAttribute('viewBox', '0 0 24 24');
            var path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
            path.setAttribute('stroke-linecap', 'round');
            path.setAttribute('stroke-linejoin', 'round');
            path.setAttribute('stroke-width', '2');
            path.setAttribute('d', 'M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1');
            icon.appendChild(path);

            link.appendChild(icon);
            link.appendChild(document.createTextNode(WeGo.escapeHtml(source.title || source.url)));
            container.appendChild(link);
        }

        return container;
    }

    // --- Format Reply (Markdown rendering) ---
    function formatReply(text) {
        if (!text) return '';
        var escaped = WeGo.escapeHtml(text);
        var lines = escaped.split('\n');
        var result = [];
        var listType = null; // null, 'ul', or 'ol'

        for (var i = 0; i < lines.length; i++) {
            var line = lines[i];

            // ### Heading
            if (/^#{1,3}\s+/.test(line)) {
                if (listType) { result.push('</' + listType + '>'); listType = null; }
                var headingText = line.replace(/^#{1,3}\s+/, '');
                result.push('<strong class="block text-sm font-semibold mt-2 mb-1">' + inlineMd(headingText) + '</strong>');
                continue;
            }

            // Bullet list item: - or * at start
            if (/^[\-\*]\s+/.test(line)) {
                if (listType && listType !== 'ul') { result.push('</' + listType + '>'); listType = null; }
                if (!listType) { result.push('<ul class="list-disc list-inside space-y-0.5 my-1">'); listType = 'ul'; }
                result.push('<li>' + inlineMd(line.replace(/^[\-\*]\s+/, '')) + '</li>');
                continue;
            }

            // Numbered list item: 1. 2. etc
            if (/^\d+\.\s+/.test(line)) {
                if (listType && listType !== 'ol') { result.push('</' + listType + '>'); listType = null; }
                if (!listType) { result.push('<ol class="list-decimal list-inside space-y-0.5 my-1">'); listType = 'ol'; }
                result.push('<li>' + inlineMd(line.replace(/^\d+\.\s+/, '')) + '</li>');
                continue;
            }

            // Close open list
            if (listType) { result.push('</' + listType + '>'); listType = null; }

            // Empty line = paragraph break
            if (line.trim() === '') { result.push('<br>'); continue; }

            // Regular paragraph
            result.push('<span>' + inlineMd(line) + '</span><br>');
        }

        if (listType) { result.push('</' + listType + '>'); }
        return result.join('');
    }

    function inlineMd(text) {
        return text.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
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
