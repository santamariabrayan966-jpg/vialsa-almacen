document.addEventListener("DOMContentLoaded", () => {

    if (window.__dentiaNavbarInicializada) return;
    window.__dentiaNavbarInicializada = true;

    const dropdowns = document.querySelectorAll(".navbar-dentia .dropdown");

    dropdowns.forEach(drop => {

        const toggle = drop.querySelector('[data-bs-toggle="dropdown"]');

        const instance = bootstrap.Dropdown.getOrCreateInstance(toggle);

        let timer;

        // ABRIR
        drop.addEventListener("mouseenter", () => {

            if (window.innerWidth < 1200) return;

            clearTimeout(timer);

            // cerrar otros
            dropdowns.forEach(other => {

                if (other !== drop) {

                    const otherToggle =
                        other.querySelector('[data-bs-toggle="dropdown"]');

                    const otherInstance =
                        bootstrap.Dropdown.getOrCreateInstance(otherToggle);

                    otherInstance.hide();
                }

            });

            instance.show();
        });

        // CERRAR
        drop.addEventListener("mouseleave", () => {

            if (window.innerWidth < 1200) return;

            timer = setTimeout(() => {

                instance.hide();

            }, 120); // MÁS RÁPIDO
        });

        // CLICK DENTRO DEL MENÚ
        drop.querySelector(".dropdown-menu")
            ?.addEventListener("click", event => {

                event.stopPropagation();

            });

    });

    const menuMovil = document.getElementById("menuPrincipal");
    const dropdownsMoviles = Array.from(menuMovil?.querySelectorAll(".nav-item.dropdown") || []);

    dropdownsMoviles.forEach((dropdown, indice) => {
        const toggle = dropdown.querySelector(':scope > [data-bs-toggle="dropdown"]');
        const submenu = dropdown.querySelector(':scope > .dropdown-menu');

        if (!toggle || !submenu) return;

        const idSubmenu = submenu.id || `submenu-panel-${indice + 1}`;
        submenu.id = idSubmenu;
        toggle.setAttribute("aria-controls", idSubmenu);

        dropdown.addEventListener("show.bs.dropdown", () => {
            if (window.innerWidth >= 1200) return;

            dropdownsMoviles.forEach(otro => {
                if (otro === dropdown) return;
                const otroToggle = otro.querySelector(':scope > [data-bs-toggle="dropdown"]');
                if (otroToggle?.classList.contains("show")) {
                    bootstrap.Dropdown.getOrCreateInstance(otroToggle).hide();
                }
            });
        });
    });

    menuMovil?.addEventListener("hidden.bs.offcanvas", () => {
        dropdownsMoviles.forEach(dropdown => {
            const toggle = dropdown.querySelector(':scope > [data-bs-toggle="dropdown"]');
            if (toggle?.classList.contains("show")) {
                bootstrap.Dropdown.getOrCreateInstance(toggle).hide();
            }
        });
    });

    const disparadoresModal = new WeakMap();

    document.addEventListener("show.bs.modal", event => {
        const modal = event.target;
        if (!(modal instanceof HTMLElement)) return;

        const candidato = event.relatedTarget instanceof HTMLElement
            ? event.relatedTarget
            : document.activeElement;

        if (candidato instanceof HTMLElement && !modal.contains(candidato)) {
            disparadoresModal.set(modal, candidato);
        }
    });

    document.addEventListener("hide.bs.modal", event => {
        const modal = event.target;
        const activo = document.activeElement;

        if (modal instanceof HTMLElement && activo instanceof HTMLElement && modal.contains(activo)) {
            activo.blur();
        }
    });

    document.addEventListener("hidden.bs.modal", event => {
        const modal = event.target;
        if (!(modal instanceof HTMLElement)) return;

        const disparador = disparadoresModal.get(modal);
        disparadoresModal.delete(modal);

        const puedeRecibirFoco = disparador instanceof HTMLElement
            && disparador.isConnected
            && !disparador.matches(":disabled, [aria-disabled='true']")
            && disparador.getClientRects().length > 0;

        if (puedeRecibirFoco) {
            disparador.focus({ preventScroll: true });
            return;
        }

        const modalPadre = document.querySelector(".modal.show");
        if (modalPadre instanceof HTMLElement) {
            modalPadre.focus({ preventScroll: true });
        }
    });

    const prepararTablaResponsive = tabla => {
        const encabezados = Array.from(tabla.querySelectorAll("thead th"))
            .map(encabezado => encabezado.textContent.trim());

        tabla.querySelectorAll("tbody tr").forEach(fila => {
            Array.from(fila.children).forEach((celda, indice) => {
                if (celda.tagName === "TD" && !celda.hasAttribute("colspan") && !celda.dataset.label) {
                    celda.dataset.label = encabezados[indice] || "Dato";
                }
            });
        });

        tabla.querySelectorAll("tbody tr > td:last-child").forEach(celdaAcciones => {
            const botones = celdaAcciones.querySelectorAll(".btn");
            if (botones.length < 2) return;

            const contenedor = celdaAcciones.querySelector(":scope > .btn-group, :scope > div");
            (contenedor || celdaAcciones).classList.add("mobile-action-group");
        });
    };

    document.querySelectorAll(".panel-interno .table-responsive table")
        .forEach(prepararTablaResponsive);

    const observarTablasDinamicas = new MutationObserver(cambios => {
        cambios.forEach(cambio => {
            cambio.addedNodes.forEach(nodo => {
                if (!(nodo instanceof Element)) {
                    return;
                }

                const tablaContenedora = nodo.closest(".panel-interno .table-responsive table");
                if (tablaContenedora) {
                    prepararTablaResponsive(tablaContenedora);
                }

                nodo.querySelectorAll?.(".panel-interno .table-responsive table")
                    .forEach(prepararTablaResponsive);
            });
        });
    });

    observarTablasDinamicas.observe(document.body, {
        childList: true,
        subtree: true
    });

});
