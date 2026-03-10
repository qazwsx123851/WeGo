/**
 * WeGo - Demo Chat Widget
 *
 * Simplified chat for demo preview with limited messages.
 * Endpoint: /api/demo/chat (CSRF-exempt, no tripId required)
 * Frontend counter: starts at 3, shows CTA when exhausted.
 *
 * Key differences from chat.js:
 * - No CSRF headers (endpoint is CSRF-exempt)
 * - No search grounding toggle
 * - Frontend message counter with CTA on exhaustion
 */
(function() {
    'use strict';

    var MAX_DEMO_MESSAGES = 3;
    var remainingMessages = MAX_DEMO_MESSAGES;
    var isProcessing = false;
    var chatOpen = false;

    // DOM elements (populated on init)
    var chatToggle, chatWindow, chatMessages, chatInput, chatSendBtn;
    var chatClose, chatBackdrop, messagesContainer;

    function init() {
        chatToggle = document.getElementById('demo-chat-toggle-btn');
        chatWindow = document.getElementById('demo-chat-window');
        chatMessages = document.getElementById('demo-chat-messages');
        chatInput = document.getElementById('demo-chat-input');
        chatSendBtn = document.getElementById('demo-chat-send-btn');
        chatClose = document.getElementById('demo-chat-close-header');
        chatBackdrop = document.getElementById('demo-chat-backdrop');
        messagesContainer = document.getElementById('demo-chat-messages');

        if (!chatToggle || !chatWindow) return;

        // Toggle chat
        chatToggle.addEventListener('click', function() {
            if (chatOpen) {
                closeChat();
            } else {
                openChat();
            }
        });

        // Close button
        if (chatClose) {
            chatClose.addEventListener('click', closeChat);
        }
        if (chatBackdrop) {
            chatBackdrop.addEventListener('click', closeChat);
        }

        // Send message
        if (chatSendBtn) {
            chatSendBtn.addEventListener('click', sendMessage);
        }

        // Input handling
        if (chatInput) {
            chatInput.addEventListener('keydown', function(e) {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    sendMessage();
                }
            });
            chatInput.addEventListener('input', function() {
                chatSendBtn.disabled = !chatInput.value.trim();
                // Auto-resize
                chatInput.style.height = 'auto';
                chatInput.style.height = Math.min(chatInput.scrollHeight, 120) + 'px';
                // Character count
                var counter = document.getElementById('demo-chat-counter');
                if (counter) {
                    counter.textContent = chatInput.value.length + '/500';
                }
            });
        }

        // Update remaining counter display
        updateRemainingDisplay();
    }

    function openChat() {
        chatOpen = true;
        var isMobile = window.innerWidth < 640;
        chatWindow.classList.remove('pointer-events-none', 'opacity-0');
        chatWindow.classList.add('pointer-events-auto', 'opacity-100');
        if (isMobile) {
            chatWindow.classList.remove('translate-y-full');
            if (chatBackdrop) chatBackdrop.classList.remove('hidden');
        } else {
            chatWindow.classList.remove('sm:scale-0');
            chatWindow.classList.add('sm:scale-100');
            var iconOpen = document.getElementById('demo-chat-icon-open');
            var iconClose = document.getElementById('demo-chat-icon-close');
            if (iconOpen) iconOpen.classList.add('hidden');
            if (iconClose) iconClose.classList.remove('hidden');
        }
        // Focus input
        setTimeout(function() {
            if (chatInput && remainingMessages > 0) chatInput.focus();
        }, 300);
    }

    function closeChat() {
        chatOpen = false;
        var isMobile = window.innerWidth < 640;
        if (isMobile) {
            chatWindow.classList.add('translate-y-full');
            if (chatBackdrop) chatBackdrop.classList.add('hidden');
        } else {
            chatWindow.classList.remove('sm:scale-100');
            chatWindow.classList.add('sm:scale-0');
            var iconOpen = document.getElementById('demo-chat-icon-open');
            var iconClose = document.getElementById('demo-chat-icon-close');
            if (iconOpen) iconOpen.classList.remove('hidden');
            if (iconClose) iconClose.classList.add('hidden');
        }
        setTimeout(function() {
            chatWindow.classList.remove('pointer-events-auto', 'opacity-100');
            chatWindow.classList.add('pointer-events-none', 'opacity-0');
        }, 300);
    }

    function sendMessage() {
        if (isProcessing || !chatInput || !chatInput.value.trim()) return;
        if (remainingMessages <= 0) return;

        var message = chatInput.value.trim();
        if (message.length > 500) return;

        isProcessing = true;
        chatSendBtn.disabled = true;

        // Add user message
        appendMessage('user', message);

        // Clear input (aligned with production chat.js pattern)
        chatInput.value = '';
        chatInput.style.height = 'auto';
        chatSendBtn.disabled = true;
        var counter = document.getElementById('demo-chat-counter');
        if (counter) counter.textContent = '0/500';

        // Show typing indicator
        var typingId = showTypingIndicator();

        // Send to API
        fetch('/api/demo/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: message })
        })
        .then(function(res) {
            if (res.status === 429) {
                remainingMessages = 0;
                showExhaustedState();
                removeTypingIndicator(typingId);
                return null;
            }
            return res.json();
        })
        .then(function(data) {
            if (!data) return;
            removeTypingIndicator(typingId);

            if (data.success && data.data && data.data.reply) {
                appendMessageWithTyping('bot', data.data.reply);
                remainingMessages--;
                updateRemainingDisplay();

                if (remainingMessages <= 0) {
                    setTimeout(function() { showExhaustedState(); }, 1000);
                }
            } else {
                appendMessage('bot', '抱歉，AI 助手暫時無法回覆，請稍後再試。');
            }
        })
        .catch(function() {
            removeTypingIndicator(typingId);
            appendMessage('bot', '網路連線異常，請稍後再試。');
        })
        .finally(function() {
            isProcessing = false;
            if (remainingMessages > 0) {
                chatSendBtn.disabled = false;
            }
        });
    }

    function appendMessage(role, text) {
        if (!messagesContainer) return;

        var div = document.createElement('div');
        div.className = role === 'user'
            ? 'flex justify-end mb-3'
            : 'flex justify-start mb-3';

        var bubble = document.createElement('div');
        bubble.className = role === 'user'
            ? 'max-w-[80%] px-4 py-2.5 bg-primary-600 text-white rounded-2xl rounded-tr-md text-sm'
            : 'max-w-[80%] px-4 py-2.5 bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200 rounded-2xl rounded-tl-md text-sm';

        bubble.textContent = text;
        div.appendChild(bubble);
        messagesContainer.appendChild(div);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    function appendMessageWithTyping(role, text) {
        if (!messagesContainer) return;

        var div = document.createElement('div');
        div.className = 'flex justify-start mb-3';

        var bubble = document.createElement('div');
        bubble.className = 'max-w-[80%] px-4 py-2.5 bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200 rounded-2xl rounded-tl-md text-sm whitespace-pre-wrap';

        div.appendChild(bubble);
        messagesContainer.appendChild(div);

        // Simple typing animation
        var i = 0;
        var interval = setInterval(function() {
            if (i < text.length) {
                bubble.textContent += text.charAt(i);
                i++;
                messagesContainer.scrollTop = messagesContainer.scrollHeight;
            } else {
                clearInterval(interval);
            }
        }, 20);
    }

    function showTypingIndicator() {
        var id = 'typing-' + Date.now();
        var div = document.createElement('div');
        div.id = id;
        div.className = 'flex justify-start mb-3';
        div.innerHTML = '<div class="px-4 py-3 bg-gray-100 dark:bg-gray-700 rounded-2xl rounded-tl-md">' +
            '<div class="flex gap-1"><span class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay:0ms"></span>' +
            '<span class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay:150ms"></span>' +
            '<span class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay:300ms"></span></div></div>';
        if (messagesContainer) {
            messagesContainer.appendChild(div);
            messagesContainer.scrollTop = messagesContainer.scrollHeight;
        }
        return id;
    }

    function removeTypingIndicator(id) {
        var el = document.getElementById(id);
        if (el) el.remove();
    }

    function updateRemainingDisplay() {
        var counter = document.getElementById('demo-chat-remaining');
        if (counter) {
            counter.textContent = '剩餘 ' + remainingMessages + ' 次對話';
            if (remainingMessages <= 1) {
                counter.classList.add('text-red-500');
            }
        }
    }

    function showExhaustedState() {
        // Disable input
        if (chatInput) chatInput.disabled = true;
        if (chatSendBtn) chatSendBtn.disabled = true;

        // Show frosted glass overlay
        var overlay = document.getElementById('demo-chat-overlay');
        if (!overlay) return;

        overlay.classList.remove('hidden');
        overlay.classList.add('flex');
        requestAnimationFrame(function() {
            overlay.classList.remove('opacity-0');
        });

        // Dismiss overlay (input remains disabled)
        var dismiss = document.getElementById('demo-chat-overlay-dismiss');
        if (dismiss) {
            dismiss.addEventListener('click', function() {
                overlay.classList.add('opacity-0');
                setTimeout(function() {
                    overlay.classList.add('hidden');
                    overlay.classList.remove('flex');
                }, 300);
            });
        }
    }

    document.addEventListener('DOMContentLoaded', init);
})();
