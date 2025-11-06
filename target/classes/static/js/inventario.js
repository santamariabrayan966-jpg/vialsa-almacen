async function buscarCliente() {
    const numero = document.getElementById("documentoCliente").value.trim();
    if (!numero) return alert("Ingrese un número de DNI o RUC");

    try {
        const tipo = numero.length === 8 ? "dni" : "ruc";
        const resp = await fetch(`/api/externo/${tipo}/${numero}`);
        const data = await resp.json();

        if (data.data) {
            const nombre = tipo === "dni"
                ? (data.data.nombre_completo || data.data.nombre || "")
                : (data.data.nombre_o_razon_social || data.data.razon_social || "");
            document.getElementById("nombreCliente").value = nombre.trim();
        } else {
            alert("No se encontraron datos del cliente.");
            document.getElementById("nombreCliente").value = "";
        }
    } catch (err) {
        console.error("❌ Error:", err);
        alert("Error al buscar el cliente.");
    }
}

document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("buscarCliente").addEventListener("click", buscarCliente);
    document.getElementById("documentoCliente").addEventListener("keypress", e => {
        if (e.key === "Enter") {
            e.preventDefault();
            buscarCliente();
        }
    });
});
