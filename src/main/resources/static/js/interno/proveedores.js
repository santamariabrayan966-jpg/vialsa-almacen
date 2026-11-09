document.addEventListener("DOMContentLoaded", () => {
    "use strict";

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || "";
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || "X-CSRF-TOKEN";
    const form = document.getElementById("form-proveedor");
    const modalElement = document.getElementById("modalProveedor");
    const drawerElement = document.getElementById("panelDetalleProveedor");
    const workspace = document.getElementById("proveedoresWorkspace");
    const modal = modalElement ? bootstrap.Modal.getOrCreateInstance(modalElement) : null;

    const fields = {
        idProveedor: document.getElementById("prov-idProveedor") || form?.querySelector('[name="idProveedor"]'),
        nroDocumento: document.getElementById("prov-nroDocumento"),
        nombreProveedor: document.getElementById("prov-nombreProveedor"),
        telefono: document.getElementById("prov-telefono"),
        correo: document.getElementById("prov-correo"),
        direccion: document.getElementById("prov-direccion")
    };
    const saveButton = document.getElementById("btn-guardar-proveedor");
    const formGeneralError = document.getElementById("form-error-general");
    const queryButton = document.getElementById("btn-buscar-doc");
    let proveedorActualId = null;
    let ultimoBotonDetalle = null;
    let consultaDocumentoActiva = false;
    let guardando = false;
    let disparadorModalProveedor = null;

    document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(element => {
        bootstrap.Tooltip.getOrCreateInstance(element);
    });

    function normalizeSpaces(value) {
        return String(value || "").trim().replace(/\s+/g, " ");
    }

    function fieldMessage(name, value) {
        const text = String(value || "");
        if (name === "nroDocumento") {
            return /^(?:\d{8}|\d{11})$/.test(text)
                ? "" : "El documento debe tener exactamente 8 dígitos (DNI) u 11 dígitos (RUC).";
        }
        if (name === "nombreProveedor") {
            if (text.length < 3 || text.length > 120) return "El nombre o razón social debe tener entre 3 y 120 caracteres.";
            if (!/^[\p{L}0-9 .,'’&/\-]+$/u.test(text) || !/\p{L}/u.test(text) || /^(\p{L})\1{2,}$/iu.test(text)) {
                return "Ingresa un nombre o razón social real con caracteres válidos.";
            }
        }
        if (name === "telefono" && text) {
            if (!/^\d{7,15}$/.test(text)) return "El teléfono debe contener entre 7 y 15 dígitos.";
            if (text.length === 9 && !text.startsWith("9")) return "Un celular peruano de 9 dígitos debe comenzar con 9.";
        }
        if (name === "correo" && text) {
            if (text.length > 120 || !/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,63}$/i.test(text)) {
                return "Ingresa un correo válido, por ejemplo contacto@empresa.com.";
            }
        }
        if (name === "direccion" && text) {
            if (text.length < 5 || text.length > 180) return "La dirección debe tener entre 5 y 180 caracteres.";
            if (!/^[\p{L}0-9 .,#/()'’&\-]+$/u.test(text) || !/\p{L}/u.test(text)) {
                return "Ingresa una dirección válida que incluya texto.";
            }
        }
        return "";
    }

    function setFieldState(name, message, showValid = true) {
        const input = fields[name];
        const feedback = document.getElementById(`error-${name}`);
        if (!input) return;
        input.classList.toggle("is-invalid", Boolean(message));
        input.classList.toggle("is-valid", !message && showValid && Boolean(input.value));
        if (feedback) feedback.textContent = message || "";
    }

    function validateField(name, showValid = true) {
        const input = fields[name];
        if (!input) return true;
        const message = fieldMessage(name, input.value);
        setFieldState(name, message, showValid);
        return !message;
    }

    function validateForm() {
        return ["nroDocumento", "nombreProveedor", "telefono", "correo", "direccion"]
            .map(name => validateField(name))
            .every(Boolean);
    }

    function clearValidation() {
        Object.values(fields).forEach(input => input?.classList.remove("is-valid", "is-invalid"));
        document.querySelectorAll('[id^="error-"]').forEach(element => { element.textContent = ""; });
        if (formGeneralError) {
            formGeneralError.textContent = "";
            formGeneralError.classList.add("d-none");
        }
    }

    function resetForm() {
        form?.reset();
        if (fields.idProveedor) fields.idProveedor.value = "";
        clearValidation();
        setSaving(false);
    }

    function setSaving(active) {
        guardando = active;
        if (!saveButton) return;
        saveButton.disabled = active;
        saveButton.querySelector(".guardar-normal")?.classList.toggle("d-none", active);
        saveButton.querySelector(".guardar-cargando")?.classList.toggle("d-none", !active);
    }

    function csrfHeaders(extra = {}) {
        const headers = { ...extra };
        if (csrfToken) headers[csrfHeader] = csrfToken;
        return headers;
    }

    async function fetchJson(url, options = {}) {
        let response;
        try {
            response = await fetch(url, options);
        } catch (cause) {
            const error = new Error("No se pudo conectar con el servidor. Revisa tu conexión.");
            error.status = 0;
            error.cause = cause;
            throw error;
        }

        const text = await response.text();
        let data = {};
        if (text) {
            try { data = JSON.parse(text); } catch (_) { data = {}; }
        }
        if (!response.ok) {
            const fallback = response.status === 403 ? "No tienes permiso para realizar esta acción."
                : response.status === 404 ? "No se encontró la información solicitada."
                    : response.status >= 500 ? "El servidor no pudo completar la operación." : "No se pudo completar la operación.";
            const error = new Error(data.mensaje || fallback);
            error.status = response.status;
            error.data = data;
            throw error;
        }
        return data;
    }

    fields.nroDocumento?.addEventListener("input", event => {
        event.target.value = event.target.value.replace(/\D/g, "").slice(0, 11);
        validateField("nroDocumento");
    });
    fields.telefono?.addEventListener("input", event => {
        event.target.value = event.target.value.replace(/\D/g, "").slice(0, 15);
        validateField("telefono");
    });
    fields.correo?.addEventListener("input", event => {
        event.target.value = event.target.value.replace(/\s/g, "").toLowerCase().slice(0, 120);
        validateField("correo");
    });
    fields.nombreProveedor?.addEventListener("input", () => validateField("nombreProveedor"));
    fields.direccion?.addEventListener("input", () => validateField("direccion"));
    fields.nombreProveedor?.addEventListener("blur", event => { event.target.value = normalizeSpaces(event.target.value); validateField("nombreProveedor"); });
    fields.direccion?.addEventListener("blur", event => { event.target.value = normalizeSpaces(event.target.value); validateField("direccion"); });

    form?.addEventListener("submit", async event => {
        event.preventDefault();
        if (guardando || !validateForm()) {
            form.querySelector(".is-invalid")?.focus();
            return;
        }

        fields.nombreProveedor.value = normalizeSpaces(fields.nombreProveedor.value);
        fields.direccion.value = normalizeSpaces(fields.direccion.value);
        setSaving(true);
        try {
            const data = await fetchJson(form.action, {
                method: "POST",
                headers: csrfHeaders({ "Accept": "application/json" }),
                body: new FormData(form)
            });
            await Swal.fire({ title: "Proveedor guardado", text: data.mensaje, icon: "success", confirmButtonColor: "#163a63" });
            window.location.assign(window.location.pathname + window.location.search);
        } catch (error) {
            const errors = error.data?.errores || {};
            Object.entries(errors).forEach(([name, message]) => setFieldState(name, message, false));
            if (formGeneralError) {
                formGeneralError.textContent = error.message;
                formGeneralError.classList.remove("d-none");
            }
            form.querySelector(".is-invalid")?.focus();
        } finally {
            setSaving(false);
        }
    });

    queryButton?.addEventListener("click", async () => {
        if (consultaDocumentoActiva || !validateField("nroDocumento")) return;
        consultaDocumentoActiva = true;
        const original = queryButton.innerHTML;
        queryButton.disabled = true;
        queryButton.innerHTML = '<span class="spinner-border spinner-border-sm" aria-hidden="true"></span> Consultando…';
        const documentNumber = fields.nroDocumento.value;
        const type = documentNumber.length === 8 ? "dni" : "ruc";
        try {
            const result = await fetchJson(`/api/externo/${type}/${documentNumber}`);
            const details = result.data || {};
            if (details.nombre_completo || details.nombres) fields.nombreProveedor.value = normalizeSpaces(details.nombre_completo || details.nombres);
            if (details.direccion) fields.direccion.value = normalizeSpaces(details.direccion);
            validateField("nombreProveedor");
            validateField("direccion");
            await Swal.fire({ title: "Datos encontrados", text: "Puedes revisarlos y completar los campos restantes.", icon: "success", confirmButtonColor: "#163a63" });
        } catch (error) {
            const message = error.status === 404
                ? "No se encontraron datos. Puedes completar el formulario manualmente."
                : `${error.message} Puedes completar el formulario manualmente.`;
            await Swal.fire({ title: "Consulta no disponible", text: message, icon: "info", confirmButtonColor: "#163a63" });
        } finally {
            consultaDocumentoActiva = false;
            queryButton.disabled = false;
            queryButton.innerHTML = original;
        }
    });

    document.getElementById("btn-nuevo-proveedor")?.addEventListener("click", event => {
        disparadorModalProveedor = event.currentTarget;
        resetForm();
        document.getElementById("titulo-modal-proveedor").textContent = "Nuevo proveedor";
        modal?.show();
    });

    modalElement?.addEventListener("hide.bs.modal", () => {
        const activo = document.activeElement;
        if (activo instanceof HTMLElement && modalElement.contains(activo)) {
            activo.blur();
        }
    });

    modalElement?.addEventListener("hidden.bs.modal", () => {
        resetForm();

        const puedeRecibirFoco = disparadorModalProveedor instanceof HTMLElement
            && disparadorModalProveedor.isConnected
            && !disparadorModalProveedor.matches(":disabled, [aria-disabled='true']")
            && disparadorModalProveedor.getClientRects().length > 0;

        if (puedeRecibirFoco) {
            disparadorModalProveedor.focus({ preventScroll: true });
        }

        disparadorModalProveedor = null;
    });

    document.addEventListener("click", async event => {
        const editButton = event.target.closest(".btn-editar-proveedor");
        if (editButton) {
            disparadorModalProveedor = editButton;
            resetForm();
            try {
                const supplier = await fetchJson(`/interno/proveedores/api/${editButton.dataset.id}`);
                fields.idProveedor.value = supplier.idProveedor;
                fields.nroDocumento.value = supplier.nroDocumento || "";
                fields.nombreProveedor.value = supplier.nombreProveedor || "";
                fields.telefono.value = supplier.telefono || "";
                fields.correo.value = supplier.correo || "";
                fields.direccion.value = supplier.direccion || "";
                document.getElementById("titulo-modal-proveedor").textContent = "Editar proveedor";
                modal?.show();
            } catch (error) {
                showError(error.message);
            }
            return;
        }

        const viewButton = event.target.closest(".btn-ver-proveedor");
        if (viewButton) {
            ultimoBotonDetalle = viewButton;
            await openDetail(viewButton.dataset.id);
            return;
        }

        const stateButton = event.target.closest(".btn-estado-proveedor");
        if (stateButton) {
            const nextState = stateButton.dataset.activo !== "true";
            await changeState(stateButton.dataset.id, nextState, stateButton);
            return;
        }

        const deleteButton = event.target.closest(".btn-eliminar-proveedor");
        if (deleteButton) await deleteSupplier(deleteButton.dataset.id, deleteButton.dataset.nombre);
    });

    async function changeState(id, active, button) {
        button?.setAttribute("disabled", "disabled");
        try {
            await fetchJson(`/interno/proveedores/cambiar-estado/${id}?activo=${active}`, {
                method: "POST", headers: csrfHeaders({ "Accept": "application/json" })
            });
            window.location.reload();
        } catch (error) {
            showError(error.message);
            button?.removeAttribute("disabled");
        }
    }

    async function deleteSupplier(id, name) {
        const decision = await Swal.fire({
            title: "¿Eliminar proveedor?",
            text: `${name}. Sus compras y relaciones se conservarán.`,
            icon: "warning",
            showCancelButton: true,
            confirmButtonColor: "#34465a",
            cancelButtonColor: "#6c7a89",
            confirmButtonText: "Sí, eliminar",
            cancelButtonText: "Cancelar"
        });
        if (!decision.isConfirmed) return;
        try {
            const result = await fetchJson(`/interno/proveedores/eliminar/${id}`, {
                method: "POST", headers: csrfHeaders({ "Accept": "application/json" })
            });
            await Swal.fire({ title: "Proveedor eliminado", text: result.mensaje, icon: "success", confirmButtonColor: "#163a63" });
            const card = document.querySelector(".proveedores-card");
            const currentPage = Number(card?.dataset.page || 0);
            const visibleRows = document.querySelectorAll(".proveedor-row").length;
            if (visibleRows === 1 && currentPage > 0) {
                const url = new URL(window.location.href);
                url.searchParams.set("page", String(currentPage - 1));
                window.location.assign(url);
            } else {
                window.location.reload();
            }
        } catch (error) {
            showError(error.message);
        }
    }

    async function openDetail(id) {
        proveedorActualId = Number(id);
        resetDrawer();
        openDetailPanel();
        toggle("detalle-loading", true);
        try {
            const supplier = await fetchJson(`/interno/proveedores/api/${id}`);
            fillDetail(supplier);
        } catch (error) {
            setText("detalle-error", error.message);
            toggle("detalle-error", true);
            toggle("detalle-proveedor-contenido", false);
        } finally {
            toggle("detalle-loading", false);
        }
    }

    function fillDetail(supplier) {
        setText("det-nombre", supplier.nombreProveedor || "Proveedor");
        setText("det-documento", supplier.nroDocumento || "—");
        setText("det-telefono", supplier.telefono || "—");
        setText("det-correo", supplier.correo || "—");
        setText("det-direccion", supplier.direccion || "—");
        setText("det-activo-label", supplier.estado === 1 ? "Activo" : "Inactivo");
        const toggleInput = document.getElementById("det-activo-toggle");
        if (toggleInput) toggleInput.checked = supplier.estado === 1;
        setText("det-total-compras", supplier.totalCompras ?? 0);
        setText("det-total-historico", formatCurrency(supplier.totalHistorico, "PEN"));
        setText("det-ultima-compra", formatDateTime(supplier.ultimaCompra));
        setText("det-sedes", supplier.sedes?.length ? supplier.sedes.join(", ") : "—");
        if (supplier.fechaRegistro) {
            setText("det-fecha-registro", formatDateTime(supplier.fechaRegistro));
            toggle("det-fecha-row", true);
        }
        toggle("detalle-resumen-compras", supplier.puedeVerCompras);
    }

    document.getElementById("det-activo-toggle")?.addEventListener("change", async event => {
        const previous = !event.target.checked;
        event.target.disabled = true;
        try {
            await fetchJson(`/interno/proveedores/cambiar-estado/${proveedorActualId}?activo=${event.target.checked}`, {
                method: "POST", headers: csrfHeaders({ "Accept": "application/json" })
            });
            setText("det-activo-label", event.target.checked ? "Activo" : "Inactivo");
        } catch (error) {
            event.target.checked = previous;
            showError(error.message);
        } finally {
            event.target.disabled = false;
        }
    });

    document.getElementById("btn-ver-compras-proveedor")?.addEventListener("click", () => {
        toggle("proveedor-vista-detalle", false);
        toggle("proveedor-vista-historial", true);
        workspace?.classList.add("is-history-open");
        loadHistory(0, true);
    });
    document.getElementById("btn-volver-detalle")?.addEventListener("click", () => {
        toggle("proveedor-vista-historial", false);
        toggle("proveedor-vista-detalle", true);
        workspace?.classList.remove("is-history-open");
    });
    document.getElementById("filtros-historial")?.addEventListener("submit", event => {
        event.preventDefault();
        loadHistory(0, false);
    });

    async function loadHistory(page, refreshFilters) {
        toggle("historial-loading", true);
        toggle("historial-error", false);
        const params = new URLSearchParams({ pagina: String(page), tamanio: "8" });
        const filterMap = {
            busqueda: document.getElementById("historial-busqueda")?.value,
            idSede: document.getElementById("historial-sede")?.value,
            estado: document.getElementById("historial-estado")?.value,
            fechaDesde: document.getElementById("historial-desde")?.value,
            fechaHasta: document.getElementById("historial-hasta")?.value
        };
        Object.entries(filterMap).forEach(([key, value]) => { if (value) params.set(key, value); });

        try {
            const data = await fetchJson(`/interno/proveedores/api/${proveedorActualId}/compras?${params}`);
            renderHistory(data, refreshFilters);
        } catch (error) {
            setText("historial-error", error.message);
            toggle("historial-error", true);
            toggle("historial-tabla-wrap", false);
            toggle("historial-vacio", false);
        } finally {
            toggle("historial-loading", false);
        }
    }

    function renderHistory(data, refreshFilters) {
        renderHistorySummary(data.resumen);
        if (refreshFilters) renderFilterOptions(data);
        renderSedeSummary(data.comprasPorSede || []);
        const rows = data.compras || [];
        toggle("historial-vacio", data.totalElementos === 0);
        const emptyText = document.querySelector("#historial-vacio p");
        if (emptyText) {
            emptyText.textContent = Number(data.resumen?.totalCompras || 0) === 0
                ? "Aún no se han registrado compras para este proveedor."
                : "No hay compras que coincidan con los filtros seleccionados.";
        }
        toggle("historial-tabla-wrap", data.totalElementos > 0);
        const body = document.getElementById("historial-compras-body");
        if (body) body.innerHTML = rows.map(purchase => `
            <tr>
                <td data-label="Compra">
                    <strong>${escapeHtml(purchase.nroOrdenCompra || `Compra #${purchase.idCompra}`)}</strong>
                    <span>${escapeHtml(purchase.nroComprobante || fullReceipt(purchase) || "Sin comprobante")}</span>
                    <small>${escapeHtml(purchase.tipoComprobante || "Comprobante")} · Registro: ${escapeHtml(formatDateTime(purchase.fechaCompra))}</small>
                    <small>Emisión: ${escapeHtml(formatDate(purchase.fechaEmision))} · Vencimiento: ${escapeHtml(formatDate(purchase.fechaVencimiento))}</small>
                    <small>Proveedor: ${escapeHtml(purchase.nombreProveedor || "—")}</small>
                </td>
                <td data-label="Sede / usuario">
                    <strong>${escapeHtml(purchase.nombreSede || "Sin sede asignada")}</strong>
                    <span>Registró: ${escapeHtml(purchase.nombreUsuario || "—")}</span>
                    <small>Pago: ${escapeHtml(purchase.formaPago ? formatState(purchase.formaPago) : "—")} · Recepción: ${escapeHtml(purchase.tipoRecepcion ? formatState(purchase.tipoRecepcion) : "—")}</small>
                    <small>Guía: ${escapeHtml(purchase.nroGuiaRemision || "—")}</small>
                </td>
                <td data-label="Importes"><strong>${escapeHtml(formatCurrency(purchase.totalCompra, purchase.moneda))}</strong><span>Subtotal ${escapeHtml(formatCurrency(purchase.subtotal, purchase.moneda))}</span><small>IGV ${escapeHtml(formatCurrency(purchase.montoIgv, purchase.moneda))}</small></td>
                <td data-label="Estado"><span class="historial-status">${escapeHtml(formatState(purchase.estado))}</span></td>
                <td><a class="btn-proveedor-icon" href="/interno/compras/detalle/${Number(purchase.idCompra)}" aria-label="Abrir detalle de compra"><i class="bi bi-box-arrow-up-right"></i></a></td>
            </tr>`).join("");
        renderPagination(data.pagina, data.totalPaginas);
    }

    function renderHistorySummary(summary) {
        const container = document.getElementById("historial-resumen");
        if (!container) return;
        container.innerHTML = `
            <article><span>Total de compras</span><strong>${Number(summary?.totalCompras || 0)}</strong></article>
            <article><span>Monto acumulado</span><strong>${escapeHtml(formatCurrency(summary?.totalHistorico, "PEN"))}</strong></article>
            <article><span>Última compra</span><strong>${escapeHtml(formatDateTime(summary?.ultimaCompra))}</strong></article>
            <article><span>Sedes utilizadas</span><strong>${escapeHtml(summary?.sedes?.join(", ") || "—")}</strong></article>`;
    }

    function renderFilterOptions(data) {
        const sedeSelect = document.getElementById("historial-sede");
        const estadoSelect = document.getElementById("historial-estado");
        if (sedeSelect) {
            sedeSelect.innerHTML = '<option value="">Todas las sedes</option>' + (data.comprasPorSede || [])
                .filter(item => item.idSede != null)
                .map(item => `<option value="${Number(item.idSede)}">${escapeHtml(item.nombreSede)}</option>`).join("");
        }
        if (estadoSelect) {
            estadoSelect.innerHTML = '<option value="">Todos los estados</option>' + (data.estados || [])
                .map(state => `<option value="${escapeHtml(state)}">${escapeHtml(formatState(state))}</option>`).join("");
        }
    }

    function renderSedeSummary(items) {
        toggle("compras-sede-section", items.length > 0);
        const container = document.getElementById("compras-sede-list");
        if (!container) return;
        container.innerHTML = items.map(item => `
            <article>
                <div><strong>${escapeHtml(item.nombreSede)}</strong><span>${Number(item.cantidadCompras)} compra(s)</span></div>
                <div><strong>${escapeHtml(formatCurrency(item.totalComprado, "PEN"))}</strong><span>Última: ${escapeHtml(formatDateTime(item.ultimaCompra))}</span></div>
            </article>`).join("");
    }

    function renderPagination(current, total) {
        const container = document.getElementById("historial-paginacion");
        if (!container) return;
        if (total <= 1) { container.innerHTML = ""; return; }
        let html = `<button type="button" ${current === 0 ? "disabled" : ""} data-page="${current - 1}" aria-label="Página anterior"><i class="bi bi-chevron-left"></i></button>`;
        for (let page = 0; page < total; page += 1) {
            if (page === 0 || page === total - 1 || Math.abs(page - current) <= 1) {
                html += `<button type="button" class="${page === current ? "active" : ""}" data-page="${page}">${page + 1}</button>`;
            } else if (!html.endsWith("…</span>")) {
                html += "<span>…</span>";
            }
        }
        html += `<button type="button" ${current >= total - 1 ? "disabled" : ""} data-page="${current + 1}" aria-label="Página siguiente"><i class="bi bi-chevron-right"></i></button>`;
        container.innerHTML = html;
        container.querySelectorAll("button[data-page]").forEach(button => {
            button.addEventListener("click", () => loadHistory(Number(button.dataset.page), false));
        });
    }

    document.getElementById("btn-cerrar-detalle-proveedor")?.addEventListener("click", closeDetailPanel);

    document.addEventListener("keydown", event => {
        if (event.key === "Escape"
                && workspace?.classList.contains("is-detail-open")
                && !document.querySelector(".modal.show")) {
            event.preventDefault();
            closeDetailPanel();
        }
    });

    function openDetailPanel() {
        if (!workspace || !drawerElement) return;
        workspace.classList.add("is-detail-open");
        workspace.classList.remove("is-history-open");
        drawerElement.setAttribute("aria-hidden", "false");
        document.body.classList.add("proveedores-panel-abierto");
        drawerElement.focus({ preventScroll: true });

        if (window.matchMedia("(max-width: 991.98px)").matches) {
            requestAnimationFrame(() => drawerElement.scrollIntoView({ behavior: "smooth", block: "nearest" }));
        }
    }

    function closeDetailPanel() {
        if (!workspace || !drawerElement) return;
        workspace.classList.remove("is-detail-open", "is-history-open");
        drawerElement.setAttribute("aria-hidden", "true");
        document.body.classList.remove("proveedores-panel-abierto");
        proveedorActualId = null;
        resetDrawer();
        ultimoBotonDetalle?.focus({ preventScroll: true });
        ultimoBotonDetalle = null;
    }

    function resetDrawer() {
        ["det-nombre", "det-documento", "det-telefono", "det-correo", "det-direccion", "det-fecha-registro"].forEach(id => setText(id, "—"));
        toggle("detalle-error", false);
        toggle("detalle-proveedor-contenido", true);
        toggle("det-fecha-row", false);
        toggle("proveedor-vista-detalle", true);
        toggle("proveedor-vista-historial", false);
        workspace?.classList.remove("is-history-open");
        ["historial-busqueda", "historial-desde", "historial-hasta"].forEach(id => { const input = document.getElementById(id); if (input) input.value = ""; });
        ["historial-sede", "historial-estado"].forEach(id => { const input = document.getElementById(id); if (input) input.selectedIndex = 0; });
    }

    const searchInput = document.getElementById("buscar-proveedor");
    const sizeSelect = document.getElementById("tamanio-proveedores");
    const loadingList = document.getElementById("proveedores-loading");
    let searchTimer;

    function navigateList(changes) {
        const url = new URL(window.location.href);
        Object.entries(changes).forEach(([name, value]) => {
            if (name === "search" && !String(value).trim()) url.searchParams.delete(name);
            else url.searchParams.set(name, String(value));
        });
        loadingList?.classList.remove("d-none");
        window.location.assign(url);
    }

    searchInput?.addEventListener("input", () => {
        window.clearTimeout(searchTimer);
        searchTimer = window.setTimeout(() => navigateList({
            search: searchInput.value.trim(),
            page: 0
        }), 400);
    });

    searchInput?.addEventListener("keydown", event => {
        if (event.key !== "Enter") return;
        event.preventDefault();
        window.clearTimeout(searchTimer);
        navigateList({ search: searchInput.value.trim(), page: 0 });
    });

    sizeSelect?.addEventListener("change", () => navigateList({ size: sizeSelect.value, page: 0 }));

    document.querySelectorAll(".proveedor-page-link").forEach(link => {
        link.addEventListener("click", event => {
            if (link.closest(".page-item")?.classList.contains("disabled")) {
                event.preventDefault();
                return;
            }
            loadingList?.classList.remove("d-none");
        });
    });

    window.addEventListener("pageshow", () => loadingList?.classList.add("d-none"));

    function fullReceipt(purchase) {
        return [purchase.serie, purchase.numero].filter(Boolean).join("-");
    }

    function formatCurrency(value, currency) {
        const amount = Number(value || 0);
        return new Intl.NumberFormat("es-PE", { style: "currency", currency: String(currency || "PEN").toUpperCase() === "USD" ? "USD" : "PEN" }).format(amount);
    }

    function formatDateTime(value) {
        if (!value) return "—";
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return "—";
        return new Intl.DateTimeFormat("es-PE", { dateStyle: "medium", timeStyle: "short" }).format(date);
    }

    function formatDate(value) {
        if (!value) return "—";
        const date = new Date(`${value}T00:00:00`);
        if (Number.isNaN(date.getTime())) return "—";
        return new Intl.DateTimeFormat("es-PE", { dateStyle: "medium" }).format(date);
    }

    function formatState(value) {
        return String(value || "Sin estado").replaceAll("_", " ").toLocaleLowerCase("es").replace(/^./, letter => letter.toUpperCase());
    }

    function escapeHtml(value) {
        return String(value ?? "").replace(/[&<>'"]/g, character => ({
            "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;"
        })[character]);
    }

    function setText(id, value) {
        const element = document.getElementById(id);
        if (element) element.textContent = value ?? "—";
    }

    function toggle(id, visible) {
        document.getElementById(id)?.classList.toggle("d-none", !visible);
    }

    function showError(message) {
        return Swal.fire({ title: "No se pudo completar la acción", text: message, icon: "error", confirmButtonColor: "#163a63" });
    }
});
