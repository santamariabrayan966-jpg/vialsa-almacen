console.log("LOGIN.JS cargado correctamente");

function mostrarLogin() {
    const loginView = document.getElementById("loginView");
    const registroView = document.getElementById("registroView");
    if (!loginView || !registroView) return;

    loginView.style.display = "block";
    registroView.style.display = "none";

    const tabs = document.querySelectorAll("#loginTabs .nav-link");
    if (tabs.length === 2) {
        tabs[0].classList.add("active");
        tabs[1].classList.remove("active");
    }
}

function mostrarRegistro() {
    const loginView = document.getElementById("loginView");
    const registroView = document.getElementById("registroView");
    if (!loginView || !registroView) return;

    loginView.style.display = "none";
    registroView.style.display = "block";

    const tabs = document.querySelectorAll("#loginTabs .nav-link");
    if (tabs.length === 2) {
        tabs[1].classList.add("active");
        tabs[0].classList.remove("active");
    }
}

// abrir REGISTRO desde cualquier lado
function abrirRegistroDesdeTienda() {
    const modalEl = document.getElementById('loginModal');
    if (!modalEl) return;

    const modal = new bootstrap.Modal(modalEl);
    modal.show();
    setTimeout(() => mostrarRegistro(), 200);
}

// abrir LOGIN desde cualquier lado
function abrirLogin() {
    const modalEl = document.getElementById("loginModal");
    if (!modalEl) return;

    const modal = new bootstrap.Modal(modalEl);
    modal.show();
    setTimeout(() => mostrarLogin(), 100);
}
