/**
 * DentIA - CyberDock & Dynamic Taskbar
 * Inspirado en Windows 11 Fluent Design y Dock interactivo con carrusel fluido,
 * menú Flyout flotante hacia arriba y paleta de comandos universal (Ctrl + K).
 */
(() => {
    'use strict';

    document.addEventListener('DOMContentLoaded', () => {
        const dock = document.getElementById('dentiaDock');
        const carousel = document.getElementById('dentiaDockCarousel');
        const btnPrev = document.getElementById('dockPrev');
        const btnNext = document.getElementById('dockNext');
        const hubBtn = document.getElementById('dockHubBtn');
        const hubMenu = document.getElementById('dentiaHubMenu');
        const spotlightModalEl = document.getElementById('dentiaSpotlightModal');
        const spotlightInput = document.getElementById('spotlightInput');
        const spotlightResults = document.getElementById('spotlightResults');
        const toggleMinimize = document.getElementById('dockMinimizeBtn');

        // 1. CARROUSEL FLUIDO HORIZONTAL
        if (carousel && btnPrev && btnNext) {
            const scrollAmount = 180;

            function updateArrows() {
                const maxScroll = carousel.scrollWidth - carousel.clientWidth;
                btnPrev.style.opacity = carousel.scrollLeft <= 5 ? '0.3' : '1';
                btnPrev.style.pointerEvents = carousel.scrollLeft <= 5 ? 'none' : 'auto';
                btnNext.style.opacity = carousel.scrollLeft >= maxScroll - 5 ? '0.3' : '1';
                btnNext.style.pointerEvents = carousel.scrollLeft >= maxScroll - 5 ? 'none' : 'auto';
            }

            btnPrev.addEventListener('click', () => {
                carousel.scrollBy({ left: -scrollAmount, behavior: 'smooth' });
            });

            btnNext.addEventListener('click', () => {
                carousel.scrollBy({ left: scrollAmount, behavior: 'smooth' });
            });

            carousel.addEventListener('scroll', updateArrows);
            window.addEventListener('resize', updateArrows);
            setTimeout(updateArrows, 150);

            // Soporte para scroll horizontal con la rueda del mouse sobre el dock
            carousel.addEventListener('wheel', (e) => {
                if (Math.abs(e.deltaX) < Math.abs(e.deltaY)) {
                    e.preventDefault();
                    carousel.scrollBy({ left: e.deltaY * 1.5, behavior: 'auto' });
                }
            }, { passive: false });
        }

        // 2. MENÚ FLYOUT TIPO WINDOWS 11 (CENTRO DE CONTROL DENTIA)
        if (hubBtn && hubMenu) {
            function toggleHub(forceState) {
                const isOpen = forceState !== undefined ? forceState : !hubMenu.classList.contains('active');
                if (isOpen) {
                    hubMenu.classList.add('active');
                    hubBtn.classList.add('active');
                    hubMenu.setAttribute('aria-hidden', 'false');
                    // Focus al input de búsqueda dentro del Hub si existe
                    const hubSearch = document.getElementById('hubSearchInput');
                    if (hubSearch) setTimeout(() => hubSearch.focus(), 150);
                } else {
                    hubMenu.classList.remove('active');
                    hubBtn.classList.remove('active');
                    hubMenu.setAttribute('aria-hidden', 'true');
                }
            }

            hubBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                toggleHub();
            });

            // Cerrar al hacer clic fuera del flyout
            document.addEventListener('click', (e) => {
                if (!hubMenu.contains(e.target) && !hubBtn.contains(e.target)) {
                    toggleHub(false);
                }
            });

            // Cerrar con tecla Escape
            document.addEventListener('keydown', (e) => {
                if (e.key === 'Escape') {
                    toggleHub(false);
                    closeSpotlight();
                }
            });
        }

        // 3. BUSCADOR INTERACTIVO EN FLYOUT HUB
        const hubSearchInput = document.getElementById('hubSearchInput');
        if (hubSearchInput) {
            hubSearchInput.addEventListener('input', () => {
                const term = hubSearchInput.value.toLowerCase().trim();
                const items = document.querySelectorAll('.hub-item-link');
                const categories = document.querySelectorAll('.hub-category-col');

                items.forEach(item => {
                    const text = item.textContent.toLowerCase();
                    const matches = text.includes(term);
                    item.style.display = matches ? 'flex' : 'none';
                });

                // Si una categoría no tiene elementos visibles, ocultarla
                categories.forEach(cat => {
                    const hasVisible = Array.from(cat.querySelectorAll('.hub-item-link')).some(it => it.style.display !== 'none');
                    cat.style.display = hasVisible ? 'block' : 'none';
                });
            });
        }

        // 4. SPOTLIGHT COMMAND PALETTE (CTRL + K O BOTÓN SPOTLIGHT)
        let spotlightModal = null;
        if (spotlightModalEl && typeof bootstrap !== 'undefined') {
            spotlightModal = new bootstrap.Modal(spotlightModalEl);

            window.abrirSpotlight = function() {
                spotlightModal.show();
                setTimeout(() => spotlightInput && spotlightInput.focus(), 200);
            };

            function closeSpotlight() {
                spotlightModal.hide();
            }

            // Atajo de teclado global: Ctrl + K o Cmd + K
            document.addEventListener('keydown', (e) => {
                if ((e.ctrlKey || e.metaKey) && (e.key === 'k' || e.key === 'K')) {
                    e.preventDefault();
                    window.abrirSpotlight();
                }
            });

            // Filtrar resultados en Spotlight
            if (spotlightInput && spotlightResults) {
                spotlightInput.addEventListener('input', () => {
                    const q = spotlightInput.value.toLowerCase().trim();
                    const allLinks = spotlightResults.querySelectorAll('.spotlight-item');
                    let count = 0;
                    allLinks.forEach(link => {
                        const title = link.querySelector('.spotlight-title')?.textContent.toLowerCase() || '';
                        const desc = link.querySelector('.spotlight-desc')?.textContent.toLowerCase() || '';
                        const match = title.includes(q) || desc.includes(q);
                        link.style.display = match ? 'flex' : 'none';
                        if (match) count++;
                    });
                    const noRes = document.getElementById('spotlightNoResults');
                    if (noRes) noRes.hidden = count > 0;
                });
            }
        }

        // 5. DETECCIÓN Y ACTIVACIÓN DE LA PÁGINA ACTUAL EN EL DOCK
        const currentPath = window.location.pathname;
        const currentQuery = window.location.search;
        let activeFound = false;

        document.querySelectorAll('.dentia-dock-item[href]').forEach(link => {
            const href = link.getAttribute('href');
            if (href && href !== '#' && (currentPath.startsWith(href) || (href.includes('?') && currentPath + currentQuery === href))) {
                link.classList.add('active');
                activeFound = true;
                // Scroll suave hasta centrar el ítem activo en el carrusel
                setTimeout(() => {
                    if (carousel) {
                        const offset = link.offsetLeft - (carousel.clientWidth / 2) + (link.clientWidth / 2);
                        carousel.scrollTo({ left: offset, behavior: 'smooth' });
                    }
                }, 200);
            }
        });

        // 6. TOGGLE MINIMIZAR / EXPANDIR DOCK
        if (toggleMinimize && dock) {
            const isMin = localStorage.getItem('dentia_dock_min') === 'true';
            if (isMin) dock.classList.add('minimized');

            toggleMinimize.addEventListener('click', (e) => {
                e.stopPropagation();
                dock.classList.toggle('minimized');
                const nowMin = dock.classList.contains('minimized');
                localStorage.setItem('dentia_dock_min', String(nowMin));
            });
        }
    });
})();
