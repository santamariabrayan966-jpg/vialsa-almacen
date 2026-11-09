
/* ==============================
   ABRIR MODAL LOGIN CLIENTES
============================== */

function abrirLogin() {
    const modalEl = document.getElementById("loginModal");

    if (!modalEl) return;

    bootstrap.Modal.getOrCreateInstance(modalEl).show();
}

/* ==============================
   MODAL PRIVACIDAD
============================== */

function abrirPrivacidad(event) {
    event.preventDefault();
    event.stopPropagation();

    const loginModalEl = document.getElementById("loginModal");
    const privacidadModalEl = document.getElementById("privacidadModal");

    if (!loginModalEl || !privacidadModalEl) return;

    const loginModal = bootstrap.Modal.getOrCreateInstance(loginModalEl);
    loginModal.hide();

    setTimeout(() => {
        const privacidadModal = bootstrap.Modal.getOrCreateInstance(privacidadModalEl);
        privacidadModal.show();

        privacidadModalEl.addEventListener("hidden.bs.modal", function () {
            bootstrap.Modal.getOrCreateInstance(loginModalEl).show();
        }, { once: true });

    }, 300);
}
