console.log("Tienda VIALSA cargada correctamente.");


// =====================================================
// 🔵 ABRIR LOGIN (Modal)
// =====================================================
function abrirLogin() {

    const modalElement = document.getElementById('loginModal');

    if (!modalElement) {
        console.error("❌ Error: No se encontró el modal con id 'loginModal'.");
        return;
    }

    // Obtiene o crea instancia del modal
    const modal = bootstrap.Modal.getOrCreateInstance(modalElement);

    modal.show();
}


// =====================================================
// 🔵 CERRAR LOGIN
// =====================================================
function cerrarLogin() {

    const modalElement = document.getElementById('loginModal');

    if (!modalElement) {
        console.warn("⚠️ cerrarLogin() llamado pero no existe el modal.");
        return;
    }

    const modal = bootstrap.Modal.getInstance(modalElement);

    if (modal) {
        modal.hide();
    } else {
        console.warn("⚠️ El modal no estaba inicializado todavía.");
    }
}



// =====================================================
// 🟡 FUNCIONES TEMPORALES PARA QUE LOS BOTONES
//     NO NAVEGUEN AL LOGIN
// =====================================================

function buscarEnConstruccion() {
    alert("🔍 La búsqueda aún está en desarrollo.\nPronto podrás buscar productos.");
}

function carritoEnConstruccion() {
    alert("🛒 El carrito aún está en desarrollo.\nMás adelante podrás ver tus productos agregados.");
}

function categoriaEnConstruccion() {
    alert("📦 Las categorías aún están en desarrollo.\nPor ahora solo son demostrativas.");
}

function abrirRegistro() {

    const login = bootstrap.Modal.getInstance(document.getElementById('loginModal'));
    if (login) login.hide();

    const registro = new bootstrap.Modal(document.getElementById('registroModal'));
    registro.show();
}
