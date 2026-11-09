(() => {
    if (!document.querySelector('link[data-pwa-install-style]')) {
        const styles = document.createElement("link");
        styles.rel = "stylesheet";
        styles.href = "/css/dentia/pwa-install.css";
        styles.dataset.pwaInstallStyle = "true";
        document.head.appendChild(styles);
    }

    const manifest = document.querySelector('link[rel="manifest"]') || document.createElement("link");
    if (!manifest.parentNode) {
        manifest.rel = "manifest";
        manifest.href = "/app.webmanifest";
        document.head.appendChild(manifest);
    }

    if (!document.querySelector('link[rel="apple-touch-icon"]')) {
        const appleIcon = document.createElement("link");
        appleIcon.rel = "apple-touch-icon";
        appleIcon.href = "/pwa/icon-192.png";
        document.head.appendChild(appleIcon);
    }

    if ("serviceWorker" in navigator && window.isSecureContext) {
        navigator.serviceWorker.register("/service-worker.js", { scope: "/" }).catch(() => {});
    }

    let installEvent = null;
    const isStandalone = () => window.matchMedia("(display-mode: standalone)").matches
        || window.navigator.standalone === true;
    const isIos = /iphone|ipad|ipod/i.test(navigator.userAgent);

    const storage = {
        get() {
            try { return Number(localStorage.getItem("dentia-pwa-remind-after") || 0); }
            catch (_) { return 0; }
        },
        postpone(days = 7) {
            try { localStorage.setItem("dentia-pwa-remind-after", String(Date.now() + days * 86400000)); }
            catch (_) {}
        }
    };

    function createBanner() {
        if (document.getElementById("dentiaInstallBanner")) return document.getElementById("dentiaInstallBanner");
        const appName = document.body.dataset.appName || "DentIA";
        const banner = document.createElement("aside");
        banner.id = "dentiaInstallBanner";
        banner.className = "pwa-install-banner";
        banner.hidden = true;
        banner.setAttribute("role", "status");
        banner.setAttribute("aria-live", "polite");
        banner.innerHTML = `
            <img class="pwa-install-icon" src="/pwa/icon-192.png" alt="" width="56" height="56">
            <div class="pwa-install-copy">
                <strong>Instalar ${escapeHtml(appName)}</strong>
                <span data-pwa-message>Accede más rápido desde la pantalla de inicio.</span>
            </div>
            <div class="pwa-install-actions">
                <button type="button" class="pwa-install-primary" data-pwa-install>Instalar aplicación</button>
                <button type="button" class="pwa-install-dismiss" data-pwa-dismiss aria-label="Cerrar aviso">Ahora no</button>
            </div>`;
        document.body.appendChild(banner);

        banner.querySelector("[data-pwa-dismiss]").addEventListener("click", () => {
            storage.postpone();
            banner.hidden = true;
        });
        banner.querySelector("[data-pwa-install]").addEventListener("click", installApp);
        return banner;
    }

    function showBanner() {
        if (isStandalone() || document.body.classList.contains("login-preview-only") || storage.get() > Date.now()) return;
        const banner = createBanner();
        const message = banner.querySelector("[data-pwa-message]");
        const button = banner.querySelector("[data-pwa-install]");
        if (!installEvent) {
            button.textContent = "Ver cómo instalar";
            message.textContent = isIos
                ? "Toca Compartir y luego “Añadir a pantalla de inicio”."
                : "Abre el menú del navegador y elige “Instalar aplicación” o “Añadir a pantalla de inicio”.";
        }
        banner.hidden = false;
    }

    async function installApp() {
        const banner = createBanner();
        if (!installEvent) {
            banner.querySelector("[data-pwa-message]").textContent = isIos
                ? "En Safari: Compartir → Añadir a pantalla de inicio → Añadir."
                : "En Chrome o Edge: abre el menú ⋮ y elige Instalar aplicación.";
            return;
        }
        installEvent.prompt();
        const choice = await installEvent.userChoice;
        if (choice.outcome === "accepted") banner.hidden = true;
        installEvent = null;
    }

    function escapeHtml(value) {
        const div = document.createElement("div");
        div.textContent = value;
        return div.innerHTML;
    }

    window.addEventListener("beforeinstallprompt", event => {
        event.preventDefault();
        installEvent = event;
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", () => setTimeout(showBanner, 800), { once: true });
        } else {
            setTimeout(showBanner, 800);
        }
    });
    window.addEventListener("appinstalled", () => {
        const banner = document.getElementById("dentiaInstallBanner");
        if (banner) banner.hidden = true;
    });

    document.addEventListener("DOMContentLoaded", () => {
        if (!installEvent && (isIos || !window.isSecureContext)) setTimeout(showBanner, 1200);
    });
})();
