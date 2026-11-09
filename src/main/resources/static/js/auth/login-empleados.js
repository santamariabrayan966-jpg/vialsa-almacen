
document.addEventListener("DOMContentLoaded", () => {

    /* ==============================
       1. LOGIN PRINCIPAL
    ============================== */
    const form = document.getElementById("loginEmpleadoForm");
    const passwordInput = document.getElementById("passwordInput");
    const usernameInput = document.querySelector("input[name='username']");
    const rememberUsername = document.getElementById("rememberUsername");
    const togglePassword = document.getElementById("togglePassword");
    const eyeOpen = document.getElementById("eyeOpen");
    const eyeClosed = document.getElementById("eyeClosed");
    const esModoPreview = document.body?.classList.contains("login-preview-only")
        || new URLSearchParams(window.location.search).get("preview") === "embed";

    if (esModoPreview) {
        document.body.classList.add("login-preview-only");

        document
            .querySelectorAll("input, button, a, select, textarea, [role='button']")
            .forEach(elemento => {
                elemento.setAttribute("aria-disabled", "true");
                elemento.setAttribute("tabindex", "-1");

                if (elemento.matches("input, textarea")) {
                    elemento.setAttribute("readonly", "readonly");
                }
            });

        const bloquearPreview = event => {
            if (
                event.type === "keydown"
                && !["Enter", " ", "Spacebar", "Tab"].includes(event.key)
                && !event.target?.matches?.("input, textarea, select, form")
            ) {
                return;
            }

            event.preventDefault();
            event.stopPropagation();
            event.stopImmediatePropagation();
        };

        ["click", "submit", "keydown", "pointerdown"].forEach(tipo => {
            document.addEventListener(tipo, bloquearPreview, true);
        });

        return;
    }

    const activarCarruselLogin = () => {
        const carrusel = document.querySelector("[data-login-carousel]");
        if (!carrusel) return;

        const slides = Array.from(carrusel.querySelectorAll(".login-carousel-slide"));
        const controles = carrusel.querySelector("[data-login-carousel-controls]");
        const dots = carrusel.querySelector("[data-login-carousel-dots]");
        const anterior = carrusel.querySelector("[data-login-carousel-prev]");
        const siguiente = carrusel.querySelector("[data-login-carousel-next]");

        if (slides.length <= 1) {
            carrusel.classList.add("login-carousel-single");
            return;
        }

        carrusel.classList.add("login-carousel-js");
        let indiceActivo = 0;
        let intervalo = null;

        if (dots && !dots.children.length) {
            slides.forEach((_, indice) => {
                const punto = document.createElement("button");
                punto.type = "button";
                punto.className = "login-carousel-dot";
                punto.setAttribute("aria-label", `Ver imagen ${indice + 1}`);
                punto.addEventListener("click", () => {
                    mostrarSlide(indice);
                    reiniciarAutoplay();
                });
                dots.appendChild(punto);
            });
        }

        const puntos = Array.from(dots?.querySelectorAll(".login-carousel-dot") || []);

        function mostrarSlide(indice) {
            indiceActivo = (indice + slides.length) % slides.length;
            slides.forEach((slide, actual) => {
                slide.classList.toggle("active", actual === indiceActivo);
            });
            puntos.forEach((punto, actual) => {
                punto.classList.toggle("active", actual === indiceActivo);
            });
        }

        function reiniciarAutoplay() {
            if (intervalo) clearInterval(intervalo);
            intervalo = setInterval(() => mostrarSlide(indiceActivo + 1), 6000);
        }

        anterior?.addEventListener("click", () => {
            mostrarSlide(indiceActivo - 1);
            reiniciarAutoplay();
        });

        siguiente?.addEventListener("click", () => {
            mostrarSlide(indiceActivo + 1);
            reiniciarAutoplay();
        });

        controles?.classList.remove("d-none");
        mostrarSlide(0);
        reiniciarAutoplay();
    };

    activarCarruselLogin();


    /* ==============================
       2. CSRF
    ============================== */
    const csrfToken = document.getElementById("csrfToken")?.value;
    const csrfParameter = document.getElementById("csrfParameter")?.value;


    /* ==============================
       3. RECUPERAR CONTRASEÑA
    ============================== */
    const mensaje = document.getElementById("mensajeRecuperacion");
    const correoInput = document.getElementById("correoRecuperacion");
    const codigoInput = document.getElementById("codigoOtp");

    const nuevaPassword = document.getElementById("nuevaPassword");
    const confirmarPassword = document.getElementById("confirmarPassword");

    const passwordHelp = document.getElementById("passwordHelp");
    const passwordMatchHelp = document.getElementById("passwordMatchHelp");

    const btnEnviarCodigo = document.getElementById("btnEnviarCodigo");
    const btnValidarCodigo = document.getElementById("btnValidarCodigo");
    const btnCambiarPassword = document.getElementById("btnCambiarPasswordOtp");

    const otpContainer = document.getElementById("otpContainer");
    const nuevaPasswordContainer = document.getElementById("nuevaPasswordContainer");


    /* ==============================
       4. MENSAJES DEL MODAL
    ============================== */
    function mostrarMensaje(texto, tipo) {
        if (!mensaje) return;

        mensaje.className = `alert alert-${tipo} text-center small`;
        mensaje.textContent = texto;
    }


    /* ==============================
       5. BODY PARA FETCH + CSRF
    ============================== */
    function crearBody(data) {
        const body = new URLSearchParams();

        Object.keys(data).forEach(key => {
            body.append(key, data[key]);
        });

        if (csrfParameter && csrfToken) {
            body.append(csrfParameter, csrfToken);
        }

        return body;
    }


    /* ==============================
       6. VALIDAR CONTRASEÑA SEGURA
    ============================== */
    function passwordSegura(password) {
        return /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&.#_\-]).{8,}$/
            .test(password);
    }


    /* ==============================
       7. VALIDAR CONTRASEÑA EN VIVO
    ============================== */
    function validarPasswordEnVivo() {
        if (!nuevaPassword || !confirmarPassword) return false;

        const password = nuevaPassword.value.trim();
        const confirmar = confirmarPassword.value.trim();

        if (!password) {
            if (passwordHelp) {
                passwordHelp.textContent =
                    "Debe tener mínimo 8 caracteres, una mayúscula, una minúscula, un número y un símbolo.";
                passwordHelp.className = "password-help";
            }

            if (passwordMatchHelp) {
                passwordMatchHelp.textContent = "";
                passwordMatchHelp.className = "password-help";
            }

            return false;
        }

        if (!passwordSegura(password)) {
            if (passwordHelp) {
                passwordHelp.textContent =
                    "La contraseña debe incluir mayúscula, minúscula, número, símbolo y mínimo 8 caracteres.";
                passwordHelp.className = "password-help error";
            }

            return false;
        }

        if (passwordHelp) {
            passwordHelp.textContent = "Contraseña segura.";
            passwordHelp.className = "password-help success";
        }

        if (confirmar && password !== confirmar) {
            if (passwordMatchHelp) {
                passwordMatchHelp.textContent = "Las contraseñas no coinciden.";
                passwordMatchHelp.className = "password-help error";
            }

            return false;
        }

        if (confirmar && password === confirmar) {
            if (passwordMatchHelp) {
                passwordMatchHelp.textContent = "Las contraseñas coinciden.";
                passwordMatchHelp.className = "password-help success";
            }
        }

        return true;
    }


    /* ==============================
       8. OJO LOGIN PRINCIPAL
    ============================== */
    if (togglePassword && passwordInput) {
        togglePassword.addEventListener("click", () => {
            const visible = passwordInput.type === "text";

            passwordInput.type = visible ? "password" : "text";

            if (eyeOpen && eyeClosed) {
                eyeOpen.classList.toggle("d-none", !visible);
                eyeClosed.classList.toggle("d-none", visible);
            }
        });
    }


    /* ==============================
       9. OJOS MODAL RECUPERACIÓN
    ============================== */
    document.querySelectorAll(".password-eye").forEach(btn => {
        btn.addEventListener("click", () => {
            const targetId = btn.getAttribute("data-target");
            const input = document.getElementById(targetId);

            if (!input) return;

            const visible = input.type === "text";

            input.type = visible ? "password" : "text";
            const eyeOpen =
                btn.querySelector("#eyeOpen");

            const eyeClosed =
                btn.querySelector("#eyeClosed");

            if (eyeOpen && eyeClosed) {

                eyeOpen.classList.toggle(
                    "d-none",
                    !visible
                );

                eyeClosed.classList.toggle(
                    "d-none",
                    visible
                );
            }
        });
    });


    /* ==============================
       10. VALIDACIÓN EN VIVO
    ============================== */
    if (nuevaPassword && confirmarPassword) {
        nuevaPassword.addEventListener("input", validarPasswordEnVivo);
        confirmarPassword.addEventListener("input", validarPasswordEnVivo);
    }


    /* ==============================
       11. ENVIAR CÓDIGO
    ============================== */
    if (btnEnviarCodigo) {
        btnEnviarCodigo.addEventListener("click", async () => {
            const correo = correoInput.value.trim();

            if (!correo) {
                mostrarMensaje("Ingresa tu correo de recuperación.", "warning");
                return;
            }

            btnEnviarCodigo.disabled = true;
            btnEnviarCodigo.textContent = "Enviando...";

            try {
                const response = await fetch("/recuperar-password", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/x-www-form-urlencoded"
                    },
                    body: crearBody({ correo })
                });

                const texto = await response.text();

                if (response.ok && texto === "OK") {
                    mostrarMensaje("Código enviado correctamente. Revisa tu correo.", "success");

                    correoInput.disabled = true;
                    btnEnviarCodigo.disabled = true;
                    btnEnviarCodigo.textContent = "Código enviado";

                    otpContainer.classList.remove("d-none");
                } else {
                    mostrarMensaje("No se pudo enviar el código.", "danger");
                    btnEnviarCodigo.disabled = false;
                    btnEnviarCodigo.textContent = "Enviar código";
                }
            } catch (error) {
                mostrarMensaje("Error de conexión con el servidor.", "danger");
                btnEnviarCodigo.disabled = false;
                btnEnviarCodigo.textContent = "Enviar código";
            }
        });
    }


    /* ==============================
       12. VALIDAR CÓDIGO OTP
    ============================== */
    if (btnValidarCodigo) {
        btnValidarCodigo.addEventListener("click", async () => {
            const codigo = codigoInput.value.trim();

            if (!codigo) {
                mostrarMensaje("Ingresa el código recibido.", "warning");
                return;
            }

            btnValidarCodigo.disabled = true;
            btnValidarCodigo.textContent = "Validando...";

            const response = await fetch("/validar-codigo-password", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: crearBody({ codigo })
            });

            const valido = await response.text();

            if (valido === "true") {
                mostrarMensaje("Código validado. Ingresa tu nueva contraseña.", "success");

                codigoInput.disabled = true;
                btnValidarCodigo.disabled = true;
                btnValidarCodigo.textContent = "Código validado";

                nuevaPasswordContainer.classList.remove("d-none");
            } else {
                mostrarMensaje("Código incorrecto o expirado.", "danger");

                btnValidarCodigo.disabled = false;
                btnValidarCodigo.textContent = "Validar código";
            }
        });
    }


    /* ==============================
       13. CAMBIAR CONTRASEÑA
    ============================== */
    if (btnCambiarPassword) {
        btnCambiarPassword.addEventListener("click", async () => {
            const codigo = codigoInput.value.trim();
            const password = nuevaPassword.value.trim();
            const confirmar = confirmarPassword.value.trim();

            if (!password || !confirmar) {
                mostrarMensaje("Completa ambas contraseñas.", "warning");
                return;
            }

            if (!passwordSegura(password)) {
                mostrarMensaje("La contraseña no cumple los requisitos de seguridad.", "danger");
                validarPasswordEnVivo();
                return;
            }

            if (password !== confirmar) {
                mostrarMensaje("Las contraseñas no coinciden.", "danger");
                validarPasswordEnVivo();
                return;
            }

            btnCambiarPassword.disabled = true;
            btnCambiarPassword.textContent = "Actualizando...";

            const response = await fetch("/restablecer-password", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: crearBody({ codigo, password })
            });

            const resultado = await response.text();

            if (resultado === "OK") {
                mostrarMensaje("Contraseña actualizada correctamente.", "success");

                setTimeout(() => {
                    window.location.href = "/interno/login?passwordActualizada";
                }, 1500);
            } else {
                mostrarMensaje("No se pudo cambiar la contraseña.", "danger");

                btnCambiarPassword.disabled = false;
                btnCambiarPassword.textContent = "Cambiar contraseña";
            }
        });
    }

    /* ==============================
       CARGAR USUARIO RECORDADO
    ============================== */

    const savedUsername =
        localStorage.getItem("dentia_username");

    if (savedUsername && usernameInput) {

        usernameInput.value = savedUsername;

        usernameInput.dispatchEvent(new Event("input"));

        if (rememberUsername) {
            rememberUsername.checked = true;
        }
    }


    /* ==============================
       14. SUBMIT LOGIN INTERNO
    ============================== */
    if (form) {
        form.addEventListener("submit", () => {

        if (
            rememberUsername?.checked &&
            usernameInput?.value.trim() !== ""
        ) {

           localStorage.setItem(
               "dentia_username",
               usernameInput.value.trim()
           );

        } else {

            localStorage.removeItem(
                "dentia_username"
            );
        }
            const button = form.querySelector(".btn-dentia");

            if (button) {
                button.disabled = true;
                button.textContent = "Validando acceso...";
            }
        });
    }
});
