/**
 * WeGo Landing Page — Feature Demo Animations
 * Cycles through 4 animated scenes showcasing core features.
 *
 * Depends on: anime.js v4 (global `anime`), WeGo._reducedMotion
 *
 * @module LandingAnimation
 */
(function() {
    'use strict';

    // ── Colors (from Remotion constants) ──────────────────────────
    var C = {
        cream:      '#F0EAE0',
        creamDark:  '#E5DDD2',
        charcoal:   '#2D2A26',
        warmGray:   '#6B6560',
        warmGrayLt: '#9B9590',
        warmBorder: '#D9D0C5',
        white:      '#FFFFFF',
        skyBlue:    '#0EA5E9',
        skyBlueLt:  '#38BDF8',
        orange:     '#F97316',
        green:      '#22C55E',
        purple:     '#A855F7',
        amber:      '#F59E0B',
        red:        '#EF4444'
    };

    var SCENE_DURATION = 6000; // ms per scene
    var FADE_DURATION  = 400;  // ms for scene transitions
    var _timer = null;
    var _currentScene = -1;
    var _stage = null;
    var _intervals = [];  // track setIntervals for cleanup

    // ── Scene Definitions ─────────────────────────────────────────
    var SCENES = [
        { title: 'Shared Itinerary',   build: buildItineraryScene },
        { title: 'Expense Splitting',  build: buildExpenseScene },
        { title: 'AI Assistant',       build: buildAIScene },
        { title: 'Team Collaboration', build: buildCollabScene }
    ];

    // ── Public API ────────────────────────────────────────────────
    function init(stageId) {
        _stage = document.getElementById(stageId);
        if (!_stage) return;
        playScene(0);
    }

    function destroy() {
        clearTimeout(_timer);
        _intervals.forEach(function(id) { clearInterval(id); });
        _intervals = [];
        _currentScene = -1;
    }

    // ── Scene Lifecycle ───────────────────────────────────────────
    function playScene(index) {
        clearTimeout(_timer);
        _intervals.forEach(function(id) { clearInterval(id); });
        _intervals = [];

        _currentScene = index;
        var scene = SCENES[index];

        // Fade out → rebuild → fade in
        _stage.style.opacity = '0';

        setTimeout(function() {
            _stage.innerHTML = '';
            scene.build(_stage);
            _stage.style.opacity = '1';

            // Schedule next scene
            var next = (index + 1) % SCENES.length;
            _timer = setTimeout(function() { playScene(next); }, SCENE_DURATION);
        }, FADE_DURATION);
    }

    // ── Utility: Element Creation ─────────────────────────────────
    function el(tag, attrs, children) {
        var e = document.createElement(tag);
        if (attrs) {
            Object.keys(attrs).forEach(function(k) {
                if (k === 'style' && typeof attrs[k] === 'object') {
                    Object.assign(e.style, attrs[k]);
                } else if (k === 'className') {
                    e.className = attrs[k];
                } else if (k === 'textContent') {
                    e.textContent = attrs[k];
                } else {
                    e.setAttribute(k, attrs[k]);
                }
            });
        }
        if (children) {
            children.forEach(function(child) {
                if (typeof child === 'string') {
                    e.appendChild(document.createTextNode(child));
                } else if (child) {
                    e.appendChild(child);
                }
            });
        }
        return e;
    }

    function svgEl(tag, attrs) {
        var e = document.createElementNS('http://www.w3.org/2000/svg', tag);
        if (attrs) {
            Object.keys(attrs).forEach(function(k) {
                e.setAttribute(k, attrs[k]);
            });
        }
        return e;
    }

    // ── Utility: Typewriter ───────────────────────────────────────
    function typeWriter(element, text, opts) {
        var o = opts || {};
        var mode = o.mode || 'word'; // 'word' or 'char'
        var speed = o.speed || (mode === 'word' ? 200 : 40);
        var delay = o.delay || 0;
        var units = mode === 'word' ? text.split(' ') : text.split('');
        var i = 0;

        element.textContent = '';

        var timeoutId = setTimeout(function() {
            var intervalId = setInterval(function() {
                if (i < units.length) {
                    if (mode === 'word') {
                        element.textContent += (i > 0 ? ' ' : '') + units[i];
                    } else {
                        element.textContent += units[i];
                    }
                    i++;
                } else {
                    clearInterval(intervalId);
                }
            }, speed);
            _intervals.push(intervalId);
        }, delay);
        _intervals.push(timeoutId);
    }

    // ── Utility: Reduced motion check ─────────────────────────────
    function isReduced() {
        return window.WeGo && WeGo._reducedMotion;
    }

    function isDark() {
        return document.documentElement.classList.contains('dark');
    }

    function animateEl(targets, props) {
        if (isReduced() || typeof anime === 'undefined') {
            // Immediately set final state
            var els = typeof targets === 'string'
                ? document.querySelectorAll(targets) : [].concat(targets);
            els.forEach(function(e) {
                if (!e || !e.style) return;
                e.style.opacity = '1';
                e.style.transform = 'none';
            });
            return null;
        }
        return anime.animate(targets, props);
    }

    // ── Narration Header ──────────────────────────────────────────
    function buildNarration(container, text) {
        var narr = el('div', {
            className: 'scene-narration',
            style: {
                fontSize: '18px',
                fontWeight: '700',
                color: isDark() ? C.cream : C.charcoal,
                textAlign: 'center',
                minHeight: '28px',
                marginBottom: '20px',
                lineHeight: '1.4',
                fontFamily: "'Plus Jakarta Sans', 'Noto Sans TC', sans-serif"
            }
        });
        container.appendChild(narr);
        typeWriter(narr, text, { mode: 'word', speed: 180, delay: 300 });
        return narr;
    }

    // ── UI Panel Wrapper ──────────────────────────────────────────
    function buildPanel(opts) {
        var style = {
            background: C.white,
            borderRadius: '16px',
            border: '1px solid ' + C.warmBorder,
            boxShadow: '0 4px 20px rgba(0,0,0,0.06)',
            overflow: 'hidden',
            width: '100%',
            opacity: '0',
            transform: 'translateY(20px)'
        };
        if (opts && opts.padding) style.padding = opts.padding;
        if (opts && opts.display) style.display = opts.display;
        if (opts && opts.gap) style.gap = opts.gap;
        return el('div', { className: 'scene-panel', style: style });
    }

    // ── Scene 1: Itinerary ────────────────────────────────────────
    function buildItineraryScene(container) {
        buildNarration(container, 'One shared itinerary. Everyone sees the plan.');

        var panel = buildPanel();
        container.appendChild(panel);

        // Header
        var header = el('div', { style: {
            display: 'flex', alignItems: 'center', gap: '10px',
            padding: '14px 20px', borderBottom: '1px solid ' + C.warmBorder
        }}, [
            el('div', { style: {
                width: '18px', height: '18px', borderRadius: '3px',
                border: '2px solid ' + C.skyBlue, flexShrink: '0'
            }}),
            el('span', { style: {
                fontSize: '15px', fontWeight: '700', color: C.charcoal,
                fontFamily: "Inter, 'Noto Sans TC', sans-serif"
            }, textContent: 'Tokyo Trip — March 2025' })
        ]);
        panel.appendChild(header);

        var content = el('div', { style: { padding: '16px 20px' } });
        panel.appendChild(content);

        var DAYS = [
            { day: 1, city: 'Tokyo', acts: 'Senso-ji Temple, Shibuya Crossing, Akihabara', color: C.skyBlue },
            { day: 2, city: 'Kyoto', acts: 'Fushimi Inari, Arashiyama, Tea ceremony', color: C.orange },
            { day: 3, city: 'Osaka', acts: 'Dotonbori, Osaka Castle, Street food tour', color: C.purple }
        ];
        var TRANSPORTS = ['🚅 Shinkansen · 2h 15min', '🚅 Shinkansen · 15min'];

        DAYS.forEach(function(day, i) {
            var card = el('div', {
                className: 'scene-day-card',
                style: {
                    borderLeft: '3px solid ' + day.color,
                    borderRadius: '6px',
                    padding: '12px 14px',
                    opacity: '0',
                    transform: 'translateY(15px)'
                }
            }, [
                el('div', { style: { display: 'flex', alignItems: 'baseline', gap: '8px' } }, [
                    el('span', { style: {
                        fontSize: '11px', fontWeight: '700', color: C.warmGrayLt,
                        textTransform: 'uppercase', letterSpacing: '0.5px'
                    }, textContent: 'Day ' + day.day }),
                    el('span', { style: {
                        fontSize: '14px', fontWeight: '700', color: day.color
                    }, textContent: day.city })
                ]),
                el('span', { style: {
                    fontSize: '12px', color: C.warmGray, lineHeight: '1.4', display: 'block', marginTop: '4px'
                }, textContent: day.acts })
            ]);
            content.appendChild(card);

            // Transport connector
            if (i < DAYS.length - 1) {
                var transport = el('div', {
                    className: 'scene-transport',
                    style: {
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        padding: '6px 0', gap: '6px', opacity: '0'
                    }
                }, [
                    el('div', { style: { flex: '1', height: '0', borderTop: '1.5px dashed ' + C.warmBorder } }),
                    el('div', { style: {
                        background: C.cream, border: '1px solid ' + C.warmBorder,
                        borderRadius: '12px', padding: '3px 10px',
                        fontSize: '11px', fontWeight: '600', color: C.warmGray, whiteSpace: 'nowrap'
                    }, textContent: TRANSPORTS[i] }),
                    el('div', { style: { flex: '1', height: '0', borderTop: '1.5px dashed ' + C.warmBorder } })
                ]);
                content.appendChild(transport);
            }
        });

        // Animate
        animateEl(panel, {
            opacity: [0, 1], translateY: [20, 0],
            duration: 500, delay: 600, ease: 'outQuad'
        });
        animateEl('.scene-day-card', {
            opacity: [0, 1], translateY: [15, 0],
            duration: 400, delay: anime.stagger(500, { start: 1400 }), ease: 'outQuad'
        });
        animateEl('.scene-transport', {
            opacity: [0, 1],
            duration: 300, delay: anime.stagger(500, { start: 2200 }), ease: 'outQuad'
        });
    }

    // ── Scene 2: Expense ──────────────────────────────────────────
    function buildExpenseScene(container) {
        buildNarration(container, 'Split bills instantly. Multiple currencies, zero math.');

        var panel = buildPanel({ padding: '20px', display: 'flex', gap: '20px' });
        container.appendChild(panel);

        // Left: Donut chart
        var leftCol = el('div', { style: {
            flex: '1', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center'
        }});
        panel.appendChild(leftCol);

        var CATS = [
            { name: 'Food', pct: 35, color: C.orange },
            { name: 'Transport', pct: 25, color: C.skyBlue },
            { name: 'Tickets', pct: 20, color: C.purple },
            { name: 'Hotel', pct: 15, color: C.green },
            { name: 'Other', pct: 5, color: C.amber }
        ];

        var svgSize = 160;
        var svg = svgEl('svg', { width: svgSize, height: svgSize, viewBox: '0 0 160 160' });
        leftCol.appendChild(svg);

        // Build pie sectors as paths
        var cx = 80, cy = 80, r = 68;
        var cumAngle = -90;
        CATS.forEach(function(cat) {
            var angle = cat.pct * 3.6;
            var startRad = (cumAngle * Math.PI) / 180;
            var endRad = ((cumAngle + angle) * Math.PI) / 180;
            var x1 = cx + r * Math.cos(startRad);
            var y1 = cy + r * Math.sin(startRad);
            var x2 = cx + r * Math.cos(endRad);
            var y2 = cy + r * Math.sin(endRad);
            var largeArc = angle > 180 ? 1 : 0;

            var path = svgEl('path', {
                d: 'M ' + cx + ' ' + cy + ' L ' + x1 + ' ' + y1 +
                   ' A ' + r + ' ' + r + ' 0 ' + largeArc + ' 1 ' + x2 + ' ' + y2 + ' Z',
                fill: cat.color,
                opacity: '0',
                class: 'scene-pie-sector'
            });
            svg.appendChild(path);
            cumAngle += angle;
        });

        // Center hole
        var hole = svgEl('circle', { cx: cx, cy: cy, r: '36', fill: C.white });
        svg.appendChild(hole);
        var totalLabel = svgEl('text', {
            x: cx, y: cy - 4, 'text-anchor': 'middle', fill: C.warmGray,
            'font-size': '10', 'font-weight': '600'
        });
        totalLabel.textContent = 'Total';
        svg.appendChild(totalLabel);
        var totalAmount = svgEl('text', {
            x: cx, y: cy + 12, 'text-anchor': 'middle', fill: C.charcoal,
            'font-size': '12', 'font-weight': '700'
        });
        totalAmount.textContent = '¥128,500';
        svg.appendChild(totalAmount);

        // Legend
        var legend = el('div', { style: {
            display: 'flex', flexWrap: 'wrap', gap: '6px', marginTop: '12px', justifyContent: 'center'
        }});
        CATS.forEach(function(cat) {
            var item = el('div', {
                className: 'scene-legend-item',
                style: {
                    display: 'flex', alignItems: 'center', gap: '4px',
                    fontSize: '10px', color: C.warmGray, opacity: '0'
                }
            }, [
                el('div', { style: {
                    width: '6px', height: '6px', borderRadius: '50%', backgroundColor: cat.color
                }}),
                document.createTextNode(cat.name + ' ' + cat.pct + '%')
            ]);
            legend.appendChild(item);
        });
        leftCol.appendChild(legend);

        // Right: Settlements
        var rightCol = el('div', { style: {
            flex: '1', display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: '10px'
        }});
        panel.appendChild(rightCol);

        var CURRENCIES = [
            { label: 'TWD', color: C.skyBlue },
            { label: 'USD', color: C.green },
            { label: 'JPY', color: C.orange },
            { label: 'EUR', color: C.purple }
        ];
        var badges = el('div', { style: { display: 'flex', gap: '6px', marginBottom: '6px', flexWrap: 'wrap' }});
        CURRENCIES.forEach(function(c) {
            badges.appendChild(el('div', {
                className: 'scene-currency-badge',
                style: {
                    background: C.cream, border: '1px solid ' + C.warmBorder,
                    borderRadius: '10px', padding: '3px 8px',
                    fontSize: '11px', fontWeight: '600', color: C.charcoal,
                    display: 'flex', alignItems: 'center', gap: '4px',
                    opacity: '0', transform: 'scale(0.8)'
                }
            }, [
                el('div', { style: { width: '6px', height: '6px', borderRadius: '50%', backgroundColor: c.color }}),
                document.createTextNode(c.label)
            ]));
        });
        rightCol.appendChild(badges);

        var simplified = el('div', {
            className: 'scene-simplified',
            style: { fontSize: '14px', color: C.charcoal, fontWeight: '600', marginBottom: '4px', opacity: '0' }
        });
        simplified.innerHTML = 'Simplified to <span style="color:' + C.green + '">3 transfers</span>';
        rightCol.appendChild(simplified);

        var SETTLEMENTS = [
            { from: 'Alice', to: 'Bob', amount: '¥3,200', fc: C.orange, tc: C.skyBlue },
            { from: 'Carol', to: 'Alice', amount: '$45.00', fc: C.purple, tc: C.orange },
            { from: 'David', to: 'Carol', amount: '€28.50', fc: C.green, tc: C.purple }
        ];
        SETTLEMENTS.forEach(function(s) {
            var card = el('div', {
                className: 'scene-settle-card',
                style: {
                    backgroundColor: 'rgba(240,234,224,0.5)', borderRadius: '10px',
                    padding: '10px 12px', border: '1px solid ' + C.warmBorder,
                    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                    opacity: '0', transform: 'translateY(12px)'
                }
            }, [
                el('div', { style: { display: 'flex', alignItems: 'center', gap: '6px' } }, [
                    el('div', { style: {
                        width: '26px', height: '26px', borderRadius: '50%',
                        backgroundColor: s.fc, display: 'flex', justifyContent: 'center',
                        alignItems: 'center', fontSize: '11px', color: C.white, fontWeight: '700'
                    }, textContent: s.from[0] }),
                    el('span', { style: { fontSize: '12px', color: C.charcoal, fontWeight: '500' }, textContent: s.from }),
                    el('span', { style: { fontSize: '12px', color: C.warmGrayLt }, textContent: '\u2192' }),
                    el('span', { style: { fontSize: '12px', color: C.charcoal, fontWeight: '500' }, textContent: s.to })
                ]),
                el('span', { style: { fontSize: '13px', fontWeight: '700', color: C.charcoal }, textContent: s.amount })
            ]);
            rightCol.appendChild(card);
        });

        // Animate
        animateEl(panel, {
            opacity: [0, 1], translateY: [20, 0],
            duration: 500, delay: 500, ease: 'outQuad'
        });
        animateEl('.scene-pie-sector', {
            opacity: [0, 0.9],
            duration: 400, delay: anime.stagger(150, { start: 1000 }), ease: 'outQuad'
        });
        animateEl('.scene-legend-item', {
            opacity: [0, 1],
            duration: 300, delay: anime.stagger(100, { start: 2000 }), ease: 'outQuad'
        });
        animateEl('.scene-currency-badge', {
            opacity: [0, 1], scale: [0.8, 1],
            duration: 300, delay: anime.stagger(100, { start: 2200 }), ease: 'outBack'
        });
        animateEl('.scene-simplified', {
            opacity: [0, 1], duration: 300, delay: 2800, ease: 'outQuad'
        });
        animateEl('.scene-settle-card', {
            opacity: [0, 1], translateY: [12, 0],
            duration: 400, delay: anime.stagger(250, { start: 3000 }), ease: 'outQuad'
        });
    }

    // ── Scene 3: AI Chat ──────────────────────────────────────────
    function buildAIScene(container) {
        buildNarration(container, 'Ask anything. Your AI knows your trip.');

        var panel = buildPanel();
        container.appendChild(panel);

        // Header
        var headerBar = el('div', { style: {
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            padding: '10px 16px', borderBottom: '1px solid ' + C.warmBorder
        }}, [
            el('div', { style: { display: 'flex', alignItems: 'center', gap: '6px' } }, [
                el('div', { style: { width: '8px', height: '8px', borderRadius: '50%', backgroundColor: C.green }}),
                el('span', { style: { fontSize: '12px', color: C.warmGray, fontWeight: '500' }, textContent: 'WeGo AI \u2014 Tokyo Trip' })
            ]),
            el('div', { style: {
                backgroundColor: C.purple + '18', borderRadius: '8px', padding: '2px 8px',
                fontSize: '10px', color: C.purple, fontWeight: '600'
            }, textContent: 'Powered by Gemini' })
        ]);
        panel.appendChild(headerBar);

        // Messages area
        var msgs = el('div', { style: { padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px', minHeight: '220px' } });
        panel.appendChild(msgs);

        // User message 1
        var user1 = el('div', {
            className: 'scene-user-msg',
            style: { display: 'flex', justifyContent: 'flex-end', opacity: '0', transform: 'translateY(8px)' }
        }, [
            el('div', { style: {
                backgroundColor: C.skyBlue, borderRadius: '14px 14px 3px 14px',
                padding: '8px 14px', maxWidth: '75%', fontSize: '13px',
                color: C.white, lineHeight: '1.4'
            }, textContent: 'What should we eat near Senso-ji Temple?' })
        ]);
        msgs.appendChild(user1);

        // AI response 1
        var aiRow1 = el('div', {
            className: 'scene-ai-msg',
            style: { display: 'flex', alignItems: 'flex-start', gap: '8px', opacity: '0' }
        });
        var aiAvatar = el('div', { style: {
            width: '24px', height: '24px', borderRadius: '50%',
            background: 'linear-gradient(135deg, ' + C.purple + ', ' + C.skyBlue + ')',
            display: 'flex', justifyContent: 'center', alignItems: 'center', flexShrink: '0',
            fontSize: '10px', color: C.white
        }, textContent: '\u2728' });
        var aiBubble1 = el('div', { style: {
            backgroundColor: C.cream, borderRadius: '14px 14px 14px 3px',
            padding: '8px 14px', maxWidth: '80%', fontSize: '13px',
            color: C.warmGray, lineHeight: '1.4'
        }});
        aiRow1.appendChild(aiAvatar);
        aiRow1.appendChild(aiBubble1);
        msgs.appendChild(aiRow1);

        // User message 2
        var user2 = el('div', {
            className: 'scene-user-msg-2',
            style: { display: 'flex', justifyContent: 'flex-end', opacity: '0', transform: 'translateY(8px)' }
        }, [
            el('div', { style: {
                backgroundColor: C.skyBlue, borderRadius: '14px 14px 3px 14px',
                padding: '8px 14px', maxWidth: '75%', fontSize: '13px',
                color: C.white, lineHeight: '1.4'
            }, textContent: 'How about day passes?' })
        ]);
        msgs.appendChild(user2);

        // AI response 2
        var aiRow2 = el('div', {
            className: 'scene-ai-msg-2',
            style: { display: 'flex', alignItems: 'flex-start', gap: '8px', opacity: '0' }
        });
        var aiAvatar2 = el('div', { style: {
            width: '24px', height: '24px', borderRadius: '50%',
            background: 'linear-gradient(135deg, ' + C.purple + ', ' + C.skyBlue + ')',
            display: 'flex', justifyContent: 'center', alignItems: 'center', flexShrink: '0',
            fontSize: '10px', color: C.white
        }, textContent: '\u2728' });
        var aiBubble2 = el('div', { style: {
            backgroundColor: C.cream, borderRadius: '14px 14px 14px 3px',
            padding: '8px 14px', maxWidth: '80%', fontSize: '13px',
            color: C.warmGray, lineHeight: '1.4'
        }});
        aiRow2.appendChild(aiAvatar2);
        aiRow2.appendChild(aiBubble2);
        msgs.appendChild(aiRow2);

        // Input bar
        panel.appendChild(el('div', { style: {
            padding: '10px 16px', borderTop: '1px solid ' + C.warmBorder,
            display: 'flex', alignItems: 'center', gap: '8px'
        }}, [
            el('div', { style: {
                flex: '1', backgroundColor: C.cream, borderRadius: '10px',
                padding: '8px 12px', fontSize: '12px', color: C.warmGrayLt
            }, textContent: 'Ask about your trip...' }),
            el('div', { style: {
                width: '28px', height: '28px', borderRadius: '50%',
                background: 'linear-gradient(135deg, ' + C.skyBlue + ', ' + C.purple + ')',
                display: 'flex', justifyContent: 'center', alignItems: 'center'
            }, textContent: '\u2191' })
        ]));

        // Animate
        var AI_TEXT_1 = 'Try Asakusa Imahan for sukiyaki \u2014 a local favorite since 1895!';
        var AI_TEXT_2 = 'Get a 72-hour Tokyo Metro pass \u2014 saves ~40%!';

        animateEl(panel, {
            opacity: [0, 1], translateY: [20, 0],
            duration: 500, delay: 500, ease: 'outQuad'
        });
        animateEl('.scene-user-msg', {
            opacity: [0, 1], translateY: [8, 0],
            duration: 400, delay: 1200, ease: 'outQuad'
        });

        // AI typing 1
        var ai1Timer = setTimeout(function() {
            if (!isReduced()) {
                animateEl('.scene-ai-msg', { opacity: [0, 1], duration: 200, ease: 'outQuad' });
            } else {
                aiRow1.style.opacity = '1';
            }
            typeWriter(aiBubble1, AI_TEXT_1, { mode: 'char', speed: 35, delay: 200 });
        }, 1800);
        _intervals.push(ai1Timer);

        // User message 2
        var u2Timer = setTimeout(function() {
            animateEl('.scene-user-msg-2', {
                opacity: [0, 1], translateY: [8, 0],
                duration: 400, ease: 'outQuad'
            });
        }, 4500);
        _intervals.push(u2Timer);

        // AI typing 2
        var ai2Timer = setTimeout(function() {
            if (!isReduced()) {
                animateEl('.scene-ai-msg-2', { opacity: [0, 1], duration: 200, ease: 'outQuad' });
            } else {
                aiRow2.style.opacity = '1';
            }
            typeWriter(aiBubble2, AI_TEXT_2, { mode: 'char', speed: 35, delay: 200 });
        }, 5200);
        _intervals.push(ai2Timer);
    }

    // ── Scene 4: Collaboration ────────────────────────────────────
    function buildCollabScene(container) {
        buildNarration(container, 'Everyone contributes. No one gets left behind.');

        var panel = buildPanel({ padding: '20px', display: 'flex', gap: '16px' });
        container.appendChild(panel);

        // Left: Team Members
        var leftCol = el('div', { style: { flex: '1', display: 'flex', flexDirection: 'column', gap: '8px' } });
        panel.appendChild(leftCol);

        leftCol.appendChild(el('div', { style: {
            fontSize: '14px', fontWeight: '700', color: C.charcoal, marginBottom: '4px'
        }, textContent: 'Team Members' }));

        var MEMBERS = [
            { name: 'Alice', i: 'A', color: C.orange, role: 'Owner', online: true },
            { name: 'Bob',   i: 'B', color: C.skyBlue, role: 'Editor', online: true },
            { name: 'Carol', i: 'C', color: C.purple, role: 'Editor', online: true },
            { name: 'David', i: 'D', color: C.green, role: 'Viewer', online: false }
        ];

        MEMBERS.forEach(function(m) {
            var memberRow = el('div', {
                className: 'scene-member',
                style: {
                    display: 'flex', alignItems: 'center', gap: '8px',
                    padding: '8px 10px', borderRadius: '10px',
                    backgroundColor: 'rgba(240,234,224,0.4)',
                    opacity: '0', transform: 'translateX(-20px)'
                }
            }, [
                el('div', { style: { position: 'relative' } }, [
                    el('div', { style: {
                        width: '28px', height: '28px', borderRadius: '50%',
                        backgroundColor: m.color, display: 'flex', justifyContent: 'center',
                        alignItems: 'center', fontSize: '12px', color: C.white, fontWeight: '700'
                    }, textContent: m.i }),
                    el('div', { style: {
                        position: 'absolute', bottom: '0', right: '0',
                        width: '8px', height: '8px', borderRadius: '50%',
                        backgroundColor: m.online ? C.green : C.warmGrayLt,
                        border: '1.5px solid ' + C.white
                    }})
                ]),
                el('span', { style: { flex: '1', fontSize: '12px', fontWeight: '600', color: C.charcoal }, textContent: m.name }),
                el('div', { style: {
                    fontSize: '10px', fontWeight: '600',
                    color: m.role === 'Owner' ? C.orange : C.warmGray,
                    backgroundColor: m.role === 'Owner' ? C.orange + '15' : C.warmGrayLt + '20',
                    borderRadius: '6px', padding: '2px 8px'
                }, textContent: m.role })
            ]);
            leftCol.appendChild(memberRow);
        });

        // Typing indicator
        var typingEl = el('div', {
            className: 'scene-typing',
            style: {
                fontSize: '11px', color: C.warmGrayLt, fontStyle: 'italic',
                paddingLeft: '10px', marginTop: '4px', opacity: '0'
            },
            textContent: 'Bob is typing'
        });
        leftCol.appendChild(typingEl);

        // Divider
        panel.appendChild(el('div', { style: {
            width: '1px', backgroundColor: C.warmBorder, alignSelf: 'stretch'
        }}));

        // Right: Todo list
        var rightCol = el('div', { style: { flex: '1', display: 'flex', flexDirection: 'column', gap: '8px' } });
        panel.appendChild(rightCol);

        rightCol.appendChild(el('div', { style: {
            fontSize: '14px', fontWeight: '700', color: C.charcoal, marginBottom: '4px'
        }, textContent: 'Trip Checklist' }));

        var TODOS = [
            { text: 'Book flights', done: true, who: 'Alice' },
            { text: 'Reserve hotel', done: true, who: 'Bob' },
            { text: 'Buy pocket WiFi', done: false, who: 'Carol' },
            { text: 'Plan Day 3 activities', done: false, who: 'David' }
        ];

        TODOS.forEach(function(t) {
            var checkboxStyle = {
                width: '16px', height: '16px', borderRadius: '4px',
                border: t.done ? '2px solid ' + C.green : '2px solid ' + C.warmBorder,
                backgroundColor: t.done ? C.green : 'transparent',
                display: 'flex', justifyContent: 'center', alignItems: 'center',
                flexShrink: '0', fontSize: '10px', color: C.white
            };
            var todoRow = el('div', {
                className: 'scene-todo',
                style: {
                    display: 'flex', alignItems: 'center', gap: '8px',
                    padding: '8px 10px', borderRadius: '10px',
                    backgroundColor: 'rgba(240,234,224,0.4)',
                    opacity: '0', transform: 'translateX(20px)'
                }
            }, [
                el('div', { style: checkboxStyle }, t.done ? [document.createTextNode('\u2713')] : []),
                el('span', { style: {
                    flex: '1', fontSize: '12px', fontWeight: '500',
                    color: t.done ? C.warmGrayLt : C.charcoal,
                    textDecoration: t.done ? 'line-through' : 'none'
                }, textContent: t.text }),
                el('span', { style: { fontSize: '10px', color: C.warmGray }, textContent: t.who })
            ]);
            rightCol.appendChild(todoRow);
        });

        // Progress badge
        var progressBadge = el('div', {
            className: 'scene-progress',
            style: {
                display: 'flex', justifyContent: 'flex-end', marginTop: '4px', opacity: '0'
            }
        }, [
            el('div', { style: {
                backgroundColor: C.skyBlue + '15', border: '1px solid ' + C.skyBlue + '33',
                borderRadius: '8px', padding: '3px 10px',
                fontSize: '11px', fontWeight: '600', color: C.skyBlue
            }, textContent: '2/4 done' })
        ]);
        rightCol.appendChild(progressBadge);

        // Animate
        animateEl(panel, {
            opacity: [0, 1], translateY: [20, 0],
            duration: 500, delay: 500, ease: 'outQuad'
        });
        animateEl('.scene-member', {
            opacity: [0, 1], translateX: [-20, 0],
            duration: 400, delay: anime.stagger(200, { start: 1200 }), ease: 'outQuad'
        });
        animateEl('.scene-todo', {
            opacity: [0, 1], translateX: [20, 0],
            duration: 400, delay: anime.stagger(200, { start: 1400 }), ease: 'outQuad'
        });

        // Typing indicator
        var typingTimer = setTimeout(function() {
            animateEl('.scene-typing', { opacity: [0, 1], duration: 300, ease: 'outQuad' });
            // Animate dots
            var dotCount = 0;
            var dotInterval = setInterval(function() {
                dotCount = (dotCount + 1) % 4;
                typingEl.textContent = 'Bob is typing' + '.'.repeat(dotCount);
            }, 500);
            _intervals.push(dotInterval);
        }, 3500);
        _intervals.push(typingTimer);

        animateEl('.scene-progress', {
            opacity: [0, 1], scale: [0.9, 1],
            duration: 300, delay: 3000, ease: 'outBack'
        });
    }

    // ── Export ─────────────────────────────────────────────────────
    window.LandingAnimation = {
        init: init,
        destroy: destroy
    };
})();
