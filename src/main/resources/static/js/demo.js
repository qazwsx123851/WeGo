/**
 * WeGo - Demo CTA Interception
 *
 * Single responsibility: intercept clicks on any element with `data-demo-cta`
 * attribute, prevent default, and show the CTA modal with a context-specific
 * message inviting the user to register.
 *
 * Loaded by every demo page. The CTA modal markup itself comes from the
 * fragment: demo/fragments/cta-modal :: cta-modal (must be included in the page).
 *
 * Note: Tab switching, timeline interactions, and chart rendering used to live
 * here for the legacy single-page demo. Each multi-page demo template now
 * handles its own page-specific behaviour inline (see Phase 3-6 templates).
 */
(function () {
    'use strict';

    var CTA_MESSAGES = {
        'add-activity': '想要新增自己的景點？',
        'edit-activity': '想要編輯行程安排？',
        'delete-activity': '想要管理行程安排？',
        'add-expense': '想要記錄旅費？',
        'edit-expense': '想要編輯費用紀錄？',
        'delete-expense': '想要管理費用紀錄？',
        'add-personal-expense': '想要記錄個人花費？',
        'edit-personal-expense': '想要管理個人記帳？',
        'delete-personal-expense': '想要管理個人記帳？',
        'add-todo': '想要新增待辦事項？',
        'edit-todo': '想要管理待辦事項？',
        'delete-todo': '想要管理待辦事項？',
        'toggle-todo': '想要管理待辦事項？',
        'invite-member': '想要邀請朋友一起規劃？',
        'remove-member': '想要管理成員？',
        'upload-document': '想要上傳旅遊文件？',
        'edit-trip': '想要編輯行程資訊？',
        'edit-budget': '想要設定個人預算？',
        'set-budget': '想要設定個人預算？',
        'view-statistics': '想要查看支出統計？',
        'view-settlement': '想要進行帳款結算？',
        'chat-exhausted': '想要無限暢聊 AI 助手？',
        'search-grounding': '想要 AI 用即時搜尋取得最新資訊？',
        'default': '想要開始規劃自己的旅程？'
    };

    function initCtaModal() {
        var backdrop = document.getElementById('cta-modal-backdrop');
        var modal = document.getElementById('cta-modal');
        var dismiss = document.getElementById('cta-dismiss');

        if (!modal) return;

        // Intercept all clicks on elements (or descendants) with data-demo-cta
        document.addEventListener('click', function (e) {
            var ctaBtn = e.target.closest('[data-demo-cta]');
            if (!ctaBtn) return;

            e.preventDefault();
            e.stopPropagation();

            var ctaType = ctaBtn.getAttribute('data-demo-cta');
            var title = CTA_MESSAGES[ctaType] || CTA_MESSAGES['default'];
            showCtaModal(title);
        });

        if (dismiss) dismiss.addEventListener('click', hideCtaModal);
        if (backdrop) backdrop.addEventListener('click', hideCtaModal);

        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') hideCtaModal();
        });
    }

    function showCtaModal(title) {
        var backdrop = document.getElementById('cta-modal-backdrop');
        var modal = document.getElementById('cta-modal');
        var content = document.getElementById('cta-modal-content');
        var titleEl = document.getElementById('cta-title');

        if (titleEl) titleEl.textContent = title;
        if (!backdrop || !modal || !content) return;

        backdrop.classList.remove('hidden');
        modal.classList.remove('hidden');

        requestAnimationFrame(function () {
            backdrop.classList.remove('opacity-0');
            content.classList.remove('scale-95', 'opacity-0');
            content.classList.add('scale-100', 'opacity-100');
        });
    }

    function hideCtaModal() {
        var backdrop = document.getElementById('cta-modal-backdrop');
        var modal = document.getElementById('cta-modal');
        var content = document.getElementById('cta-modal-content');

        if (!backdrop || !modal || !content) return;

        backdrop.classList.add('opacity-0');
        content.classList.remove('scale-100', 'opacity-100');
        content.classList.add('scale-95', 'opacity-0');

        setTimeout(function () {
            backdrop.classList.add('hidden');
            modal.classList.add('hidden');
        }, 300);
    }

    document.addEventListener('DOMContentLoaded', initCtaModal);

})();
