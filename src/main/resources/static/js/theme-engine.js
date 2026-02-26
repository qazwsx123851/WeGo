/**
 * WeGo Theme Engine (Context-Aware Dynamic Theming)
 */
class ThemeEngine {
    constructor() {
        this.root = document.documentElement;
        this.init();
    }

    init() {
        document.addEventListener('theme:update', (e) => {
            if (e.detail && e.detail.primaryColor) {
                this.setPrimaryColor(e.detail.primaryColor);
            }
        });

        // Contextual analysis of current view
        setTimeout(() => this.scanContext(), 500);
    }

    setPrimaryColor(rgbValue) {
        this.root.style.setProperty('--color-primary-500', rgbValue);
        this.root.style.setProperty('--color-primary-400', this.adjustLuminance(rgbValue, 0.2));
        this.root.style.setProperty('--color-primary-600', this.adjustLuminance(rgbValue, -0.2));

    }

    adjustLuminance(rgb, percent) {
        let [r, g, b] = rgb.split(' ').map(Number);
        r = Math.min(255, Math.max(0, r + (255 * percent)));
        g = Math.min(255, Math.max(0, g + (255 * percent)));
        b = Math.min(255, Math.max(0, b + (255 * percent)));
        return `${Math.round(r)} ${Math.round(g)} ${Math.round(b)}`;
    }

    scanContext() {
        // AI Vision mock: analyze the first cover image
        const coverImage = document.querySelector('img[src*="cover"]');
        if (coverImage) {
            if (coverImage.alt && coverImage.src.includes('tokyo')) {
                document.dispatchEvent(new CustomEvent('theme:update', {
                    detail: { primaryColor: "236 72 153" } // Pink 500
                }));
            } else if (coverImage.src.includes('nature')) {
                document.dispatchEvent(new CustomEvent('theme:update', {
                    detail: { primaryColor: "34 197 94" } // Green 500
                }));
            } else {
                document.dispatchEvent(new CustomEvent('theme:update', {
                    detail: { primaryColor: "139 92 246" } // Violet 500 for generic trips overlay
                }));
            }
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.wegoThemeEngine = new ThemeEngine();
});
