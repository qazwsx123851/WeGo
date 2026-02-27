/**
 * WeGo Landing Page — Feature Demo Animations (Dark Theme)
 * Cycles through 4 animated scenes matching production UI.
 *
 * Depends on: anime.js v4 (global `anime`), WeGo._reducedMotion
 *
 * @module LandingAnimation
 */
(function() {
    'use strict';

    // ── Colors (matching production dark theme) ─────────────────
    var C = {
        bgDark:     '#111827',
        cardDark:   '#1F2937',
        cardInner:  '#374151',
        borderDark: '#374151',
        textPrimary:'#F3F4F6',
        textSecond: '#9CA3AF',
        textMuted:  '#6B7280',
        white:      '#FFFFFF',
        skyBlue:    '#0EA5E9',
        skyBlueLt:  '#38BDF8',
        orange:     '#F97316',
        green:      '#22C55E',
        purple:     '#A855F7',
        amber:      '#F59E0B',
        red:        '#EF4444',
        pink:       '#EC4899'
    };

    var SCENE_DURATION    = 6000;
    var FADE_DURATION     = 400;
    var MOBILE_BREAKPOINT = 640;
    var _timer = null;
    var _currentScene = -1;
    var _stage = null;
    var _intervals = [];

    // ── Scene Definitions ───────────────────────────────────────
    var SCENES = [
        { title: '行程景點',    build: buildItineraryScene },
        { title: '分帳記帳',    build: buildExpenseScene },
        { title: 'AI 旅遊助手', build: buildAIScene },
        { title: '待辦事項',    build: buildTodoScene }
    ];

    // ── Public API ──────────────────────────────────────────────
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

    // ── Scene Lifecycle ─────────────────────────────────────────
    function playScene(index) {
        clearTimeout(_timer);
        _intervals.forEach(function(id) { clearInterval(id); });
        _intervals = [];

        _currentScene = index;
        var scene = SCENES[index];

        _stage.style.opacity = '0';

        var fadeId = setTimeout(function() {
            if (!_stage) return;
            _stage.innerHTML = '';
            _stage.style.display = 'flex';
            _stage.style.flexDirection = 'column';
            scene.build(_stage);
            _stage.style.opacity = '1';

            var next = (index + 1) % SCENES.length;
            _timer = setTimeout(function() { playScene(next); }, SCENE_DURATION);
        }, FADE_DURATION);
        _intervals.push(fadeId);
    }

    // ── Utility: Element Creation ───────────────────────────────
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
                } else if (k === 'innerHTML') {
                    e.innerHTML = attrs[k];
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

    // ── Utility: Typewriter ─────────────────────────────────────
    function typeWriter(element, text, opts) {
        var o = opts || {};
        var mode = o.mode || 'char';
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

    // ── Utility: Helpers ────────────────────────────────────────
    function isReduced() {
        return window.WeGo && WeGo._reducedMotion;
    }

    function isMobile() {
        return window.innerWidth < MOBILE_BREAKPOINT;
    }

    function stagger(val, opts) {
        if (typeof anime !== 'undefined' && anime.stagger) {
            return anime.stagger(val, opts);
        }
        return (opts && opts.start) ? opts.start : 0;
    }

    function animateEl(targets, props) {
        if (isReduced() || typeof anime === 'undefined') {
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

    // ── Narration Header ────────────────────────────────────────
    function buildNarration(container, text) {
        var mobile = isMobile();
        var narr = el('div', {
            className: 'scene-narration',
            style: {
                fontSize: mobile ? '15px' : '17px',
                fontWeight: '700',
                color: C.textPrimary,
                textAlign: 'center',
                minHeight: '26px',
                marginBottom: mobile ? '12px' : '18px',
                lineHeight: '1.5',
                fontFamily: "'Plus Jakarta Sans', 'Noto Sans TC', sans-serif"
            }
        });
        container.appendChild(narr);
        typeWriter(narr, text, { mode: 'char', speed: 80, delay: 300 });
        return narr;
    }

    // ── Dark Panel Wrapper ──────────────────────────────────────
    function buildPanel(opts) {
        var style = {
            background: C.cardDark,
            borderRadius: '20px',
            border: '1px solid ' + C.borderDark,
            boxShadow: '0 8px 32px rgba(0,0,0,0.3)',
            overflow: 'hidden',
            width: '100%',
            flex: '1',
            minHeight: '0',
            display: 'flex',
            flexDirection: 'column',
            opacity: '0',
            transform: 'translateY(20px)',
            position: 'relative'
        };
        if (opts && opts.padding) style.padding = opts.padding;
        if (opts && opts.display) style.display = opts.display;
        if (opts && opts.gap) style.gap = opts.gap;
        if (opts && opts.flexDirection) style.flexDirection = opts.flexDirection;
        return el('div', { className: 'scene-panel', style: style });
    }

    // ── Shared: Header Bar ──────────────────────────────────────
    function buildHeaderBar(panel, title, opts) {
        var o = opts || {};
        var header = el('div', { style: {
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            padding: '12px 16px', borderBottom: '1px solid ' + C.borderDark
        }}, [
            el('div', { style: { display: 'flex', alignItems: 'center', gap: '10px' } }, [
                el('span', { style: { fontSize: '16px', color: C.textSecond }, textContent: '\u2039' }),
                el('div', {}, [
                    el('div', { style: {
                        fontSize: '15px', fontWeight: '700', color: C.textPrimary,
                        fontFamily: "'Plus Jakarta Sans', 'Noto Sans TC', sans-serif"
                    }, textContent: title }),
                    o.subtitle ? el('div', { style: {
                        fontSize: '11px', color: C.textMuted, marginTop: '1px'
                    }, textContent: o.subtitle }) : null
                ].filter(Boolean))
            ]),
            o.showAdd ? el('div', { style: {
                width: '32px', height: '32px', borderRadius: '10px',
                backgroundColor: C.orange, display: 'flex',
                justifyContent: 'center', alignItems: 'center',
                fontSize: '18px', color: C.white, fontWeight: '300'
            }, textContent: '+' }) : null
        ].filter(Boolean));
        panel.appendChild(header);
        return header;
    }

    // ── Shared: Bottom Pill Bar ─────────────────────────────────
    var PILL_ICONS = {
        mapPin: function(color) {
            var s = svgEl('svg', { width: '20', height: '20', viewBox: '0 0 24 24', fill: 'none' });
            var p = svgEl('path', {
                d: 'M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5a2.5 2.5 0 110-5 2.5 2.5 0 010 5z',
                fill: color
            });
            s.appendChild(p);
            return s;
        },
        dollar: function(color) {
            var s = svgEl('svg', { width: '20', height: '20', viewBox: '0 0 24 24', fill: 'none' });
            var p = svgEl('path', {
                d: 'M12 2a10 10 0 100 20 10 10 0 000-20zm1 14.93V18h-2v-1.07A4 4 0 018 13h2a2 2 0 002 2 2 2 0 002-2c0-1.1-.9-2-2-2a4 4 0 01-1-7.93V2h2v1.07A4 4 0 0116 7h-2a2 2 0 00-2-2 2 2 0 00-2 2c0 1.1.9 2 2 2a4 4 0 011 7.93z',
                fill: color
            });
            s.appendChild(p);
            return s;
        },
        file: function(color) {
            var s = svgEl('svg', { width: '20', height: '20', viewBox: '0 0 24 24', fill: 'none' });
            var p = svgEl('path', {
                d: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6zm4 18H6V4h7v5h5v11z',
                fill: color
            });
            s.appendChild(p);
            return s;
        },
        check: function(color) {
            var s = svgEl('svg', { width: '20', height: '20', viewBox: '0 0 24 24', fill: 'none' });
            var p = svgEl('path', {
                d: 'M12 2a10 10 0 100 20 10 10 0 000-20zm-1 15l-4-4 1.41-1.41L11 14.17l5.59-5.59L18 10l-7 7z',
                fill: color
            });
            s.appendChild(p);
            return s;
        }
    };

    function buildPillBar(panel, activeIndex) {
        var items = [
            { iconFn: PILL_ICONS.mapPin, label: '景點' },
            { iconFn: PILL_ICONS.dollar, label: '分帳' },
            { iconFn: PILL_ICONS.file,   label: '檔案' },
            { iconFn: PILL_ICONS.check,  label: '代辦' }
        ];
        var bar = el('div', {
            className: 'scene-pill-bar',
            style: {
                display: 'flex', justifyContent: 'space-around', alignItems: 'center',
                padding: '10px 8px 12px',
                backgroundColor: 'rgba(17,24,39,0.92)',
                backdropFilter: 'blur(10px)',
                borderTop: '1px solid ' + C.borderDark,
                opacity: '0', transform: 'translateY(8px)'
            }
        });

        // Push pill bar to bottom of flex panel
        bar.style.marginTop = 'auto';

        items.forEach(function(item, i) {
            var isActive = i === activeIndex;
            var iconColor = isActive ? C.skyBlue : C.textMuted;
            var wrapper = el('div', { style: {
                display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px',
                padding: '6px 14px', borderRadius: '12px',
                backgroundColor: isActive ? 'rgba(14,165,233,0.12)' : 'transparent',
                transition: 'background-color 0.2s ease'
            }}, [
                item.iconFn(iconColor),
                el('span', { style: {
                    fontSize: '11px', fontWeight: isActive ? '700' : '500',
                    color: isActive ? C.skyBlue : C.textMuted,
                    letterSpacing: '0.02em'
                }, textContent: item.label })
            ]);
            bar.appendChild(wrapper);
        });
        panel.appendChild(bar);
        return bar;
    }

    // ── Scene 1: Itinerary ──────────────────────────────────────
    function buildItineraryScene(container) {
        buildNarration(container, '共享行程，所有人同步看到最新計畫');

        var panel = buildPanel();
        container.appendChild(panel);

        buildHeaderBar(panel, '2026 WBC', { subtitle: '03/04 - 03/09' });

        // Day tab bar
        var tabBar = el('div', { style: {
            display: 'flex', gap: '8px', padding: '12px 16px',
            overflowX: 'auto', borderBottom: '1px solid ' + C.borderDark
        }});
        var TABS = [
            { day: '週三', date: '3/4', active: true },
            { day: '週四', date: '3/5', active: false },
            { day: '週五', date: '3/6', active: false },
            { day: '週六', date: '3/7', active: false }
        ];
        TABS.forEach(function(tab) {
            tabBar.appendChild(el('div', {
                className: 'scene-day-tab',
                style: {
                    padding: '6px 14px', borderRadius: '10px',
                    textAlign: 'center', flexShrink: '0',
                    backgroundColor: tab.active ? C.skyBlue : C.cardInner,
                    border: tab.active ? 'none' : '1px solid ' + C.borderDark,
                    opacity: '0', transform: 'translateY(8px)'
                }
            }, [
                el('div', { style: {
                    fontSize: '10px', fontWeight: '500',
                    color: tab.active ? C.white : C.textSecond
                }, textContent: tab.day }),
                el('div', { style: {
                    fontSize: '14px', fontWeight: '700',
                    color: tab.active ? C.white : C.textPrimary
                }, textContent: tab.date })
            ]));
        });
        panel.appendChild(tabBar);

        // Activity content
        var content = el('div', { style: { padding: '12px 16px', display: 'flex', flexDirection: 'column', gap: '4px', flex: '1', minHeight: '0', overflow: 'hidden' } });
        panel.appendChild(content);

        var ACTIVITIES = [
            { icon: '\u21C6', iconBg: C.cardInner, iconColor: C.textSecond,
              name: '機場第二航廈', time: '14:20 - 17:50', dur: '3 小時', note: '華航 CI172 出發' },
            { icon: '\u21C6', iconBg: C.cardInner, iconColor: C.textSecond,
              name: '關西機場', time: '17:50 - 20:20', dur: '2 小時', note: '預估通關 60-70 分鐘' },
            { icon: '\u25CF', iconBg: 'rgba(14,165,233,0.15)', iconColor: C.skyBlue,
              name: '新大阪', time: '19:00 - 19:50', dur: '50 分鐘', note: 'JR特急 Haruka（約50分鐘）' }
        ];
        var TRANSPORTS = [
            { dist: '195 分鐘', badge: '手動', badgeColor: C.amber },
            { dist: '57.4 km · 55 分鐘', badge: '精確', badgeColor: C.green }
        ];

        ACTIVITIES.forEach(function(act, i) {
            var card = el('div', {
                className: 'scene-act-card',
                style: {
                    display: 'flex', alignItems: 'flex-start', gap: '10px',
                    padding: '10px 12px', borderRadius: '12px',
                    backgroundColor: C.cardDark, border: '1px solid ' + C.borderDark,
                    opacity: '0', transform: 'translateY(12px)'
                }
            }, [
                el('div', { style: {
                    width: '36px', height: '36px', borderRadius: '50%',
                    backgroundColor: act.iconBg, display: 'flex',
                    justifyContent: 'center', alignItems: 'center',
                    fontSize: '14px', color: act.iconColor, flexShrink: '0'
                }, textContent: act.icon }),
                el('div', { style: { flex: '1', minWidth: '0' } }, [
                    el('div', { style: {
                        fontSize: '13px', fontWeight: '600', color: C.textPrimary
                    }, textContent: act.name }),
                    el('div', { style: {
                        fontSize: '11px', color: C.textSecond, marginTop: '2px'
                    }, textContent: act.time + '  ' + act.dur }),
                    el('div', { style: {
                        fontSize: '11px', color: C.textMuted, marginTop: '2px'
                    }, textContent: act.note })
                ])
            ]);
            content.appendChild(card);

            if (i < ACTIVITIES.length - 1) {
                var t = TRANSPORTS[i];
                var transport = el('div', {
                    className: 'scene-transport',
                    style: {
                        display: 'flex', alignItems: 'center', gap: '8px',
                        padding: '4px 0 4px 18px', opacity: '0'
                    }
                }, [
                    el('div', { style: {
                        width: '2px', height: '16px', backgroundColor: C.borderDark
                    }}),
                    el('span', { style: {
                        fontSize: '11px', color: C.textSecond
                    }, textContent: t.dist }),
                    el('span', { style: {
                        fontSize: '10px', fontWeight: '600', color: t.badgeColor,
                        backgroundColor: t.badgeColor + '20',
                        borderRadius: '6px', padding: '1px 6px'
                    }, textContent: t.badge })
                ]);
                content.appendChild(transport);
            }
        });

        buildPillBar(panel, 0);

        // Animate
        animateEl(panel, {
            opacity: [0, 1], translateY: [20, 0],
            duration: 500, delay: 500, ease: 'outQuad'
        });
        animateEl('.scene-day-tab', {
            opacity: [0, 1], translateY: [8, 0],
            duration: 300, delay: stagger(100, { start: 1000 }), ease: 'outQuad'
        });
        animateEl('.scene-act-card', {
            opacity: [0, 1], translateY: [12, 0],
            duration: 400, delay: stagger(400, { start: 1500 }), ease: 'outQuad'
        });
        animateEl('.scene-transport', {
            opacity: [0, 1],
            duration: 300, delay: stagger(400, { start: 2100 }), ease: 'outQuad'
        });
        animateEl('.scene-pill-bar', {
            opacity: [0, 1], translateY: [8, 0],
            duration: 300, delay: 3200, ease: 'outQuad'
        });
    }

    // ── Scene 2: Expense ────────────────────────────────────────
    function buildExpenseScene(container) {
        buildNarration(container, '多幣別分帳，一鍵結算不再算數學');

        var mobile = isMobile();
        var panel = buildPanel();
        container.appendChild(panel);

        buildHeaderBar(panel, '分帳', { showAdd: true });

        // Tab switcher
        var tabContainer = el('div', { style: {
            display: 'flex', gap: '4px', margin: '12px 16px',
            padding: '4px', backgroundColor: C.bgDark, borderRadius: '12px'
        }});
        tabContainer.appendChild(el('div', {
            className: 'scene-exp-tab',
            style: {
                flex: '1', padding: '8px', borderRadius: '8px',
                textAlign: 'center', fontSize: '13px', fontWeight: '600',
                backgroundColor: C.cardInner, color: C.textPrimary,
                opacity: '0'
            },
            textContent: '團隊分帳'
        }));
        tabContainer.appendChild(el('div', {
            className: 'scene-exp-tab',
            style: {
                flex: '1', padding: '8px', borderRadius: '8px',
                textAlign: 'center', fontSize: '13px', fontWeight: '500',
                color: C.textMuted, opacity: '0'
            },
            textContent: '個人記帳'
        }));
        panel.appendChild(tabContainer);

        // Overview card
        var overview = el('div', {
            className: 'scene-overview',
            style: {
                margin: '0 16px 12px', padding: mobile ? '14px' : '16px',
                backgroundColor: 'rgba(255,255,255,0.04)',
                border: '1px solid ' + C.borderDark,
                borderRadius: '14px', opacity: '0', transform: 'translateY(10px)'
            }
        });

        // Sub-tabs
        var subTabs = el('div', { style: {
            display: 'flex', gap: '16px', marginBottom: '14px',
            borderBottom: '1px solid ' + C.borderDark, paddingBottom: '10px'
        }});
        ['總覽', '統計', '結算'].forEach(function(t, i) {
            subTabs.appendChild(el('span', { style: {
                fontSize: '13px', fontWeight: i === 0 ? '700' : '500',
                color: i === 0 ? C.textPrimary : C.textMuted
            }, textContent: t }));
        });
        overview.appendChild(subTabs);

        // Metrics
        var metrics = el('div', { style: {
            display: 'flex', gap: mobile ? '10px' : '16px'
        }});

        var metricCard = function(label, value, valueColor) {
            return el('div', { style: {
                flex: '1', padding: '10px', borderRadius: '10px',
                backgroundColor: 'rgba(255,255,255,0.04)'
            }}, [
                el('div', { style: { fontSize: '11px', color: C.textMuted, marginBottom: '4px' }, textContent: label }),
                el('div', { style: {
                    fontSize: mobile ? '16px' : '20px', fontWeight: '700',
                    color: valueColor || C.textPrimary,
                    fontFamily: "'JetBrains Mono', monospace"
                }, textContent: value })
            ]);
        };
        metrics.appendChild(metricCard('總支出', '21,871', null));
        metrics.appendChild(metricCard('您的餘額', '+6,689', C.green));
        overview.appendChild(metrics);

        var perPerson = el('div', { style: {
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            marginTop: '12px', paddingTop: '10px',
            borderTop: '1px solid ' + C.borderDark
        }}, [
            el('span', { style: { fontSize: '12px', color: C.textMuted }, textContent: '人均花費' }),
            el('span', { style: {
                fontSize: '14px', fontWeight: '600', color: C.textPrimary,
                fontFamily: "'JetBrains Mono', monospace"
            }, textContent: '$4,374' })
        ]);
        overview.appendChild(perPerson);
        panel.appendChild(overview);

        // Donut chart section
        var chartSection = el('div', {
            className: 'scene-chart',
            style: {
                display: 'flex', flexDirection: mobile ? 'column' : 'row',
                alignItems: 'center', gap: mobile ? '10px' : '16px',
                padding: '0 16px 12px', opacity: '0'
            }
        });

        var CATS = [
            { name: '娛樂', pct: 50, color: C.orange },
            { name: '餐飲', pct: 34, color: C.skyBlue },
            { name: '交通', pct: 9,  color: C.green },
            { name: '其他', pct: 7,  color: C.purple }
        ];

        var svgSize = mobile ? 100 : 120;
        var svg = svgEl('svg', { width: svgSize, height: svgSize, viewBox: '0 0 120 120' });
        var cx = 60, cy = 60, r = 50;
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
            svg.appendChild(svgEl('path', {
                d: 'M ' + cx + ' ' + cy + ' L ' + x1 + ' ' + y1 +
                   ' A ' + r + ' ' + r + ' 0 ' + largeArc + ' 1 ' + x2 + ' ' + y2 + ' Z',
                fill: cat.color, opacity: '0', class: 'scene-pie-sector'
            }));
            cumAngle += angle;
        });
        svg.appendChild(svgEl('circle', { cx: cx, cy: cy, r: '26', fill: C.cardDark }));
        var centerLabel = svgEl('text', {
            x: cx, y: cy - 2, 'text-anchor': 'middle', fill: C.textMuted, 'font-size': '8'
        });
        centerLabel.textContent = '總計';
        svg.appendChild(centerLabel);
        var centerAmount = svgEl('text', {
            x: cx, y: cy + 10, 'text-anchor': 'middle', fill: C.textPrimary,
            'font-size': '10', 'font-weight': '700'
        });
        centerAmount.textContent = '$21,871';
        svg.appendChild(centerAmount);
        chartSection.appendChild(svg);

        // Legend
        var legend = el('div', { style: {
            display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '6px 16px'
        }});
        CATS.forEach(function(cat) {
            legend.appendChild(el('div', {
                className: 'scene-legend-item',
                style: {
                    display: 'flex', alignItems: 'center', gap: '6px',
                    fontSize: '11px', color: C.textSecond, opacity: '0'
                }
            }, [
                el('div', { style: {
                    width: '8px', height: '8px', borderRadius: '50%', backgroundColor: cat.color
                }}),
                el('span', { textContent: cat.name }),
                el('span', { style: { color: C.textPrimary, fontWeight: '600' }, textContent: cat.pct + '%' })
            ]));
        });
        chartSection.appendChild(legend);
        panel.appendChild(chartSection);

        buildPillBar(panel, 1);

        // Animate
        animateEl(panel, {
            opacity: [0, 1], translateY: [20, 0],
            duration: 500, delay: 500, ease: 'outQuad'
        });
        animateEl('.scene-exp-tab', {
            opacity: [0, 1],
            duration: 300, delay: stagger(150, { start: 1000 }), ease: 'outQuad'
        });
        animateEl('.scene-overview', {
            opacity: [0, 1], translateY: [10, 0],
            duration: 400, delay: 1400, ease: 'outQuad'
        });
        animateEl('.scene-chart', {
            opacity: [0, 1], duration: 400, delay: 2200, ease: 'outQuad'
        });
        animateEl('.scene-pie-sector', {
            opacity: [0, 0.9],
            duration: 300, delay: stagger(120, { start: 2400 }), ease: 'outQuad'
        });
        animateEl('.scene-legend-item', {
            opacity: [0, 1],
            duration: 200, delay: stagger(100, { start: 3000 }), ease: 'outQuad'
        });
        animateEl('.scene-pill-bar', {
            opacity: [0, 1], translateY: [8, 0],
            duration: 300, delay: 3600, ease: 'outQuad'
        });
    }

    // ── Scene 3: AI Chat ────────────────────────────────────────
    function buildAIScene(container) {
        buildNarration(container, 'AI 助手隨問隨答，你的專屬旅遊顧問');

        var panel = buildPanel();
        container.appendChild(panel);

        // Chat header (primary-500 blue)
        var chatHeader = el('div', { style: {
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            padding: '12px 16px', backgroundColor: C.skyBlue
        }}, [
            el('div', { style: { display: 'flex', alignItems: 'center', gap: '10px' } }, [
                el('div', { style: {
                    width: '32px', height: '32px', borderRadius: '50%',
                    backgroundColor: 'rgba(255,255,255,0.2)',
                    display: 'flex', justifyContent: 'center', alignItems: 'center',
                    fontSize: '14px', color: C.white
                }, textContent: '\u2728' }),
                el('div', {}, [
                    el('div', { style: {
                        fontSize: '14px', fontWeight: '700', color: C.white
                    }, textContent: 'WeGo 旅遊助手' }),
                    el('div', { style: {
                        fontSize: '11px', color: 'rgba(255,255,255,0.8)'
                    }, textContent: '旅遊、美食、景點推薦' })
                ])
            ]),
            el('div', { style: {
                fontSize: '16px', color: 'rgba(255,255,255,0.7)', cursor: 'pointer'
            }, textContent: '\u2715' })
        ]);
        panel.appendChild(chatHeader);

        // Messages area
        var msgs = el('div', { style: {
            padding: '14px 16px', display: 'flex', flexDirection: 'column',
            gap: '12px', flex: '1', minHeight: '0', overflow: 'hidden', backgroundColor: C.bgDark
        }});
        panel.appendChild(msgs);

        // AI welcome message
        var aiWelcome = el('div', {
            className: 'scene-ai-msg',
            style: { display: 'flex', alignItems: 'flex-start', gap: '8px', opacity: '0' }
        });
        var welcomeAvatar = el('div', { style: {
            width: '28px', height: '28px', borderRadius: '50%',
            backgroundColor: 'rgba(14,165,233,0.15)',
            display: 'flex', justifyContent: 'center', alignItems: 'center',
            flexShrink: '0', fontSize: '12px', color: C.skyBlue
        }, textContent: '\u2728' });
        var welcomeBubble = el('div', { style: {
            backgroundColor: C.cardInner, borderRadius: '16px 16px 16px 4px',
            padding: '10px 14px', maxWidth: '85%', fontSize: '13px',
            color: C.textPrimary, lineHeight: '1.5'
        }, textContent: '你好！我是 WeGo 旅遊助手，可以幫你推薦餐廳、景點和行程安排。有什麼想問的嗎？' });
        aiWelcome.appendChild(welcomeAvatar);
        aiWelcome.appendChild(welcomeBubble);
        msgs.appendChild(aiWelcome);

        // User message
        var userMsg = el('div', {
            className: 'scene-user-msg',
            style: { display: 'flex', justifyContent: 'flex-end', opacity: '0', transform: 'translateY(8px)' }
        }, [
            el('div', { style: {
                backgroundColor: C.skyBlue, borderRadius: '16px 16px 4px 16px',
                padding: '10px 14px', maxWidth: '75%', fontSize: '13px',
                color: C.white, lineHeight: '1.4'
            }, textContent: '大阪有什麼必吃的美食？' })
        ]);
        msgs.appendChild(userMsg);

        // AI response (typed)
        var aiRow = el('div', {
            className: 'scene-ai-reply',
            style: { display: 'flex', alignItems: 'flex-start', gap: '8px', opacity: '0' }
        });
        var replyAvatar = el('div', { style: {
            width: '28px', height: '28px', borderRadius: '50%',
            backgroundColor: 'rgba(14,165,233,0.15)',
            display: 'flex', justifyContent: 'center', alignItems: 'center',
            flexShrink: '0', fontSize: '12px', color: C.skyBlue
        }, textContent: '\u2728' });
        var replyBubble = el('div', { style: {
            backgroundColor: C.cardInner, borderRadius: '16px 16px 16px 4px',
            padding: '10px 14px', maxWidth: '85%', fontSize: '13px',
            color: C.textPrimary, lineHeight: '1.5'
        }});
        aiRow.appendChild(replyAvatar);
        aiRow.appendChild(replyBubble);
        msgs.appendChild(aiRow);

        // Input bar
        var inputBar = el('div', { style: {
            padding: '10px 16px', backgroundColor: C.bgDark,
            borderTop: '1px solid ' + C.borderDark
        }});
        var inputRow = el('div', { style: {
            display: 'flex', alignItems: 'center', gap: '8px',
            backgroundColor: C.cardInner, borderRadius: '16px',
            border: '1px solid ' + C.borderDark, padding: '6px 8px 6px 12px'
        }}, [
            el('div', { style: {
                width: '28px', height: '28px', borderRadius: '50%',
                border: '1.5px solid ' + C.textMuted,
                display: 'flex', justifyContent: 'center', alignItems: 'center',
                fontSize: '12px', color: C.textMuted, flexShrink: '0'
            }, textContent: '\uD83C\uDF10' }),
            el('span', { style: {
                flex: '1', fontSize: '13px', color: C.textMuted
            }, textContent: '輸入你的問題...' }),
            el('div', { style: {
                width: '32px', height: '32px', borderRadius: '12px',
                backgroundColor: C.skyBlue,
                display: 'flex', justifyContent: 'center', alignItems: 'center',
                fontSize: '14px', color: C.white
            }, textContent: '\u2191' })
        ]);
        inputBar.appendChild(inputRow);

        // Gemini label
        var geminiRow = el('div', { style: {
            display: 'flex', justifyContent: 'space-between',
            marginTop: '6px', padding: '0 4px'
        }}, [
            el('span', { style: { fontSize: '11px', color: C.textMuted }, textContent: 'Gemini 2.5 Flash' }),
            el('span', { style: { fontSize: '11px', color: C.textMuted }, textContent: '0/500' })
        ]);
        inputBar.appendChild(geminiRow);
        panel.appendChild(inputBar);

        // Animate
        var AI_REPLY = '推薦道頓堀的章魚燒和大阪燒！另外黑門市場的海鮮丼也很值得一試 🍣';

        animateEl(panel, {
            opacity: [0, 1], translateY: [20, 0],
            duration: 500, delay: 500, ease: 'outQuad'
        });
        animateEl('.scene-ai-msg', {
            opacity: [0, 1], duration: 300, delay: 1100, ease: 'outQuad'
        });
        animateEl('.scene-user-msg', {
            opacity: [0, 1], translateY: [8, 0],
            duration: 400, delay: 2200, ease: 'outQuad'
        });

        var replyTimer = setTimeout(function() {
            if (!isReduced()) {
                animateEl('.scene-ai-reply', { opacity: [0, 1], duration: 200, ease: 'outQuad' });
            } else {
                aiRow.style.opacity = '1';
            }
            typeWriter(replyBubble, AI_REPLY, { mode: 'char', speed: 50, delay: 200 });
        }, 3000);
        _intervals.push(replyTimer);
    }

    // ── Scene 4: Todos ──────────────────────────────────────────
    function buildTodoScene(container) {
        buildNarration(container, '待辦追蹤，出發前確保萬事俱備');

        var panel = buildPanel();
        container.appendChild(panel);

        // Header with progress badge
        var header = el('div', { style: {
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            padding: '12px 16px', borderBottom: '1px solid ' + C.borderDark
        }}, [
            el('div', { style: { display: 'flex', alignItems: 'center', gap: '10px' } }, [
                el('span', { style: { fontSize: '16px', color: C.textSecond }, textContent: '\u2039' }),
                el('span', { style: {
                    fontSize: '15px', fontWeight: '700', color: C.textPrimary,
                    fontFamily: "'Plus Jakarta Sans', 'Noto Sans TC', sans-serif"
                }, textContent: '待辦事項' }),
                el('div', { style: {
                    backgroundColor: 'rgba(14,165,233,0.15)', borderRadius: '10px',
                    padding: '2px 8px', fontSize: '11px', fontWeight: '600', color: C.skyBlue
                }, textContent: '1/3' })
            ]),
            el('div', { style: {
                width: '32px', height: '32px', borderRadius: '10px',
                backgroundColor: C.orange, display: 'flex',
                justifyContent: 'center', alignItems: 'center',
                fontSize: '18px', color: C.white, fontWeight: '300'
            }, textContent: '+' })
        ]);
        panel.appendChild(header);

        // Filter tabs
        var filterBar = el('div', { style: {
            display: 'flex', gap: '8px', padding: '12px 16px',
            overflowX: 'auto'
        }});
        var FILTERS = [
            { label: '全部', count: 3, active: true },
            { label: '待處理', count: 0, active: false },
            { label: '進行中', count: 2, active: false },
            { label: '已完成', count: 1, active: false }
        ];
        FILTERS.forEach(function(f) {
            filterBar.appendChild(el('div', {
                className: 'scene-filter-tab',
                style: {
                    padding: '6px 14px', borderRadius: '20px',
                    fontSize: '12px', fontWeight: '600', whiteSpace: 'nowrap',
                    backgroundColor: f.active ? C.skyBlue : C.cardInner,
                    color: f.active ? C.white : C.textMuted,
                    opacity: '0', transform: 'scale(0.9)'
                }
            }, [
                document.createTextNode(f.label),
                el('span', { style: {
                    marginLeft: '4px', opacity: '0.7', fontSize: '11px'
                }, textContent: '(' + f.count + ')' })
            ]));
        });
        panel.appendChild(filterBar);

        // Todo cards
        var todoList = el('div', { style: {
            padding: '0 16px 12px', display: 'flex', flexDirection: 'column', gap: '10px',
            flex: '1', minHeight: '0', overflow: 'hidden'
        }});

        var TODOS = [
            { title: '預訂飯店', desc: '確認入住日期', who: '小明', whoColor: C.orange,
              date: '02/11', status: 'done', overdue: false },
            { title: '購買機票', desc: '比價後下單', who: '小華', whoColor: C.skyBlue,
              date: '02/12', status: 'progress', overdue: true },
            { title: '規劃第三天行程', desc: '大阪環球影城', who: '小美', whoColor: C.purple,
              date: '02/13', status: 'progress', overdue: true }
        ];

        TODOS.forEach(function(t) {
            var statusIcon;
            if (t.status === 'done') {
                statusIcon = el('div', { style: {
                    width: '24px', height: '24px', borderRadius: '50%',
                    backgroundColor: C.green, display: 'flex',
                    justifyContent: 'center', alignItems: 'center',
                    fontSize: '12px', color: C.white, flexShrink: '0'
                }, textContent: '\u2713' });
            } else {
                statusIcon = el('div', { style: {
                    width: '24px', height: '24px', borderRadius: '50%',
                    backgroundColor: C.skyBlue, display: 'flex',
                    justifyContent: 'center', alignItems: 'center', flexShrink: '0'
                }}, [
                    el('div', { style: {
                        width: '8px', height: '8px', borderRadius: '50%',
                        backgroundColor: C.white
                    }})
                ]);
            }

            var metaChildren = [
                el('div', { style: { display: 'flex', alignItems: 'center', gap: '4px' } }, [
                    el('div', { style: {
                        width: '18px', height: '18px', borderRadius: '50%',
                        backgroundColor: t.whoColor, display: 'flex',
                        justifyContent: 'center', alignItems: 'center',
                        fontSize: '9px', color: C.white, fontWeight: '700'
                    }, textContent: t.who[0] }),
                    el('span', { style: { fontSize: '11px', color: C.textMuted }, textContent: t.who })
                ]),
                el('span', { style: {
                    fontSize: '11px', color: t.overdue ? C.red : C.textMuted
                }, textContent: '\uD83D\uDCC5 ' + t.date + (t.overdue ? ' 逾期' : '') })
            ];

            var statusBadge = el('span', { style: {
                fontSize: '10px', fontWeight: '600',
                padding: '2px 8px', borderRadius: '6px',
                backgroundColor: t.status === 'done' ? 'rgba(34,197,94,0.15)' : 'rgba(14,165,233,0.15)',
                color: t.status === 'done' ? C.green : C.skyBlue
            }, textContent: t.status === 'done' ? '已完成' : '進行中' });

            var card = el('div', {
                className: 'scene-todo-card',
                style: {
                    display: 'flex', alignItems: 'flex-start', gap: '12px',
                    padding: '12px', borderRadius: '14px',
                    backgroundColor: 'rgba(255,255,255,0.04)',
                    border: '1px solid ' + C.borderDark,
                    opacity: '0', transform: 'translateY(12px)'
                }
            }, [
                statusIcon,
                el('div', { style: { flex: '1', minWidth: '0' } }, [
                    el('div', { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'center' } }, [
                        el('span', { style: {
                            fontSize: '13px', fontWeight: '600',
                            color: t.status === 'done' ? C.textMuted : C.textPrimary,
                            textDecoration: t.status === 'done' ? 'line-through' : 'none'
                        }, textContent: t.title }),
                        statusBadge
                    ]),
                    el('div', { style: {
                        fontSize: '12px', color: C.textMuted, marginTop: '3px'
                    }, textContent: t.desc }),
                    el('div', { style: {
                        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                        marginTop: '8px'
                    }}, metaChildren)
                ])
            ]);
            todoList.appendChild(card);
        });
        panel.appendChild(todoList);

        buildPillBar(panel, 3);

        // Animate
        animateEl(panel, {
            opacity: [0, 1], translateY: [20, 0],
            duration: 500, delay: 500, ease: 'outQuad'
        });
        animateEl('.scene-filter-tab', {
            opacity: [0, 1], scale: [0.9, 1],
            duration: 300, delay: stagger(100, { start: 1000 }), ease: 'outBack'
        });
        animateEl('.scene-todo-card', {
            opacity: [0, 1], translateY: [12, 0],
            duration: 400, delay: stagger(350, { start: 1500 }), ease: 'outQuad'
        });
        animateEl('.scene-pill-bar', {
            opacity: [0, 1], translateY: [8, 0],
            duration: 300, delay: 3200, ease: 'outQuad'
        });
    }

    // ── Export ───────────────────────────────────────────────────
    window.LandingAnimation = {
        init: init,
        destroy: destroy
    };
})();
