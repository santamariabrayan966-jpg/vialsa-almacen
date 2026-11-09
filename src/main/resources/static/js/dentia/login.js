document.addEventListener("DOMContentLoaded", () => {
    const previewMode = document.body.classList.contains("login-preview-only");
    const rememberedUsernameKey = "dentia_staff_user";
    const rememberedChoiceKey = "dentia_staff_remember";
    const username = document.getElementById("username");
    const rememberDevice = document.getElementById("rememberDevice");
    const readingToggle = document.getElementById("dentiaReadingToggle");
    function syncReadingToggle() {
        const large = document.documentElement.dataset.dentiaReadingMode === "large";
        readingToggle?.setAttribute("aria-pressed", String(large));
        readingToggle?.setAttribute("aria-label", large ? "Volver al tamaño normal" : "Activar lectura fácil");
        readingToggle?.setAttribute("title", large ? "Volver al tamaño normal" : "Activar lectura fácil");
    }
    readingToggle?.addEventListener("click", () => {
        const large = document.documentElement.dataset.dentiaReadingMode !== "large";
        if (large) document.documentElement.dataset.dentiaReadingMode = "large";
        else delete document.documentElement.dataset.dentiaReadingMode;
        try { localStorage.setItem("dentia-reading-mode", large ? "large" : "normal"); } catch (_) {}
        syncReadingToggle();
    });
    syncReadingToggle();

    // Un formulario antiguo guardaba la contraseña sin cifrar. Eliminarla en este dispositivo.
    try {
        localStorage.removeItem("dentia_staff_pass");
        if (!previewMode && localStorage.getItem(rememberedChoiceKey) === "true") {
            rememberDevice.checked = true;
            if (!username.value) username.value = localStorage.getItem(rememberedUsernameKey) || "";
        }
    } catch (_) {
        // El navegador puede bloquear el almacenamiento; el acceso sigue funcionando.
    }

    document.querySelectorAll("[data-password-toggle]").forEach((button) => {
        const input = document.getElementById(button.dataset.passwordToggle);
        if (!input) return;

        button.addEventListener("click", () => {
            const showPassword = input.type === "password";
            input.type = showPassword ? "text" : "password";
            button.setAttribute("aria-pressed", String(showPassword));
            button.setAttribute("aria-label", showPassword ? "Ocultar contraseña" : "Mostrar contraseña");
            button.classList.toggle("is-visible", showPassword);
            input.focus({ preventScroll: true });
        });
    });

    document.querySelectorAll("[data-login-form]").forEach((form) => {
        form.addEventListener("submit", (event) => {
            if (previewMode) {
                event.preventDefault();
                return;
            }

            try {
                if (rememberDevice?.checked && username?.value.trim()) {
                    localStorage.setItem(rememberedChoiceKey, "true");
                    localStorage.setItem(rememberedUsernameKey, username.value.trim());
                } else {
                    localStorage.removeItem(rememberedChoiceKey);
                    localStorage.removeItem(rememberedUsernameKey);
                }
            } catch (_) {
                // No impedir el inicio de sesión por restricciones del navegador.
            }

            const button = form.querySelector(".login-submit");
            if (!button || button.disabled) return;
            button.disabled = true;
            button.setAttribute("aria-busy", "true");
            button.querySelector("span:first-child").textContent = "Validando acceso…";
        });
    });

    const resetDialog = document.getElementById("passwordResetDialog");
    const resetMessage = document.getElementById("resetMessage");
    const requestForm = document.getElementById("resetRequestForm");
    const verifyForm = document.getElementById("resetVerifyForm");
    const changeForm = document.getElementById("resetChangeForm");
    const resetIntro = document.getElementById("resetIntro");
    const resetPassword = document.getElementById("resetNewPassword");
    const resetConfirm = document.getElementById("resetConfirmPassword");
    const codeGroup = document.querySelector(".reset-code-digits");
    const codeInputs = Array.from(document.querySelectorAll(".reset-code-digit"));
    let checkingCode = false;
    let resetGeneration = 0;
    function syncCode() { document.getElementById("resetCode").value = codeInputs.map(input => input.value).join(""); }
    function setCodeState(state) { codeGroup.classList.toggle("is-valid", state === "valid"); codeGroup.classList.toggle("is-invalid", state === "invalid"); }
    codeInputs.forEach((input, index) => {
        input.addEventListener("input", () => {
            input.value = input.value.replace(/[^0-9]/g, "").slice(-1);
            setCodeState(""); syncCode();
            if (input.value && index < codeInputs.length - 1) codeInputs[index + 1].focus();
            if (codeInputs.every(field => field.value)) verifyForm.requestSubmit();
        });
        input.addEventListener("keydown", event => {
            if (event.key === "Backspace" && !input.value && index > 0) { codeInputs[index - 1].focus(); codeInputs[index - 1].value = ""; setCodeState(""); syncCode(); event.preventDefault(); }
            if (event.key === "ArrowLeft" && index > 0) { codeInputs[index - 1].focus(); event.preventDefault(); }
            if (event.key === "ArrowRight" && index < codeInputs.length - 1) { codeInputs[index + 1].focus(); event.preventDefault(); }
        });
        input.addEventListener("paste", event => {
            const digits = (event.clipboardData?.getData("text") || "").replace(/[^0-9]/g, "").slice(0, 4);
            if (!digits) return;
            event.preventDefault(); setCodeState("");
            codeInputs.forEach((field, i) => { field.value = digits[i] || ""; }); syncCode();
            codeInputs[Math.min(digits.length, 3)].focus();
            if (digits.length === 4) verifyForm.requestSubmit();
        });
    });
    function setPasswordField(input, feedback, state, message) {
        input.classList.toggle("is-valid", state === "valid");
        input.classList.toggle("is-invalid", state === "invalid");
        if (state) input.setAttribute("aria-invalid", String(state === "invalid")); else input.removeAttribute("aria-invalid");
        feedback.classList.toggle("is-valid", state === "valid");
        feedback.classList.toggle("is-invalid", state === "invalid");
        feedback.textContent = message;
    }
    function updateRequirements() {
        const value = resetPassword.value;
        const rules = { length: value.length >= 8 && value.length <= 80, upper: /[A-Z]/.test(value), lower: /[a-z]/.test(value), number: /[0-9]/.test(value), symbol: /[^\w\s]/.test(value) };
        const labels = { length: "entre 8 y 80 caracteres", upper: "una mayúscula", lower: "una minúscula", number: "un número", symbol: "un símbolo" };
        const missing = Object.keys(rules).filter(key => !rules[key]).map(key => labels[key]);
        document.querySelectorAll("#resetRequirements [data-rule]").forEach(item => { const met = rules[item.dataset.rule]; item.classList.toggle("is-met", met); item.classList.toggle("is-missing", !!value && !met); item.setAttribute("aria-label", item.textContent + (met ? ": cumplido" : ": pendiente")); });
        setPasswordField(resetPassword, document.getElementById("resetPasswordFeedback"), value ? (missing.length ? "invalid" : "valid") : "", value ? (missing.length ? "Falta: " + missing.join(", ") + "." : "La contraseña cumple todos los requisitos.") : "");
        const confirm = resetConfirm.value;
        setPasswordField(resetConfirm, document.getElementById("resetConfirmFeedback"), confirm ? (confirm === value && value ? "valid" : "invalid") : "", confirm ? (confirm === value && value ? "Las contraseñas coinciden." : "Las contraseñas no coinciden.") : "");
    }
    resetPassword?.addEventListener("input", updateRequirements);
    resetConfirm?.addEventListener("input", updateRequirements);
    const csrf = document.querySelector('meta[name="_csrf"]')?.content;

    async function resetPost(url, values) {
        const body = new URLSearchParams(values);
        if (csrf) body.set("_csrf", csrf);
        const response = await fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body
        });
        if (!response.ok) { const error = new Error("Solicitud no disponible"); error.status = response.status; throw error; }
        return (await response.text()).trim();
    }

    document.getElementById("openPasswordReset")?.addEventListener("click", () => {
        if (!previewMode) {
            resetGeneration++; checkingCode = false; codeInputs.forEach(input => { input.disabled = false; }); setCodeState("");
            requestForm.hidden = false; verifyForm.hidden = true; changeForm.hidden = true; resetIntro.hidden = false;
            requestForm.reset(); verifyForm.reset(); changeForm.reset(); resetMessage.textContent = ""; updateRequirements();
            resetDialog?.showModal();
        }
    });
    document.getElementById("closePasswordReset")?.addEventListener("click", () => resetDialog?.close());

    requestForm?.addEventListener("submit", async (event) => {
        event.preventDefault();
        const button = requestForm.querySelector("button[type=submit]");
        button.disabled = true;
        resetMessage.textContent = "Enviando solicitud…";
        try {
            await resetPost("/recuperar-password", { correo: document.getElementById("resetEmail").value.trim() });
            resetMessage.textContent = "Si el correo coincide con el registrado en tu cuenta, recibirás un código. Revisa también spam.";
            requestForm.hidden = true;
            verifyForm.hidden = false;
            codeInputs[0].focus();
        } catch (error) {
            resetMessage.textContent = error.status === 429 ? "Has solicitado varios códigos. Espera hasta una hora desde la primera solicitud e inténtalo de nuevo." : "No se pudo procesar la solicitud. Inténtalo más tarde.";
        } finally {
            button.disabled = false;
        }
    });

    verifyForm?.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (checkingCode || !codeInputs.every(input => /^[0-9]$/.test(input.value))) return;
        checkingCode = true;
        const generation = resetGeneration;
        syncCode();
        codeInputs.forEach(input => { input.disabled = true; });
        resetMessage.textContent = "Verificando código…";
        try {
            const valid = await resetPost("/validar-codigo-password", {
                correo: document.getElementById("resetEmail").value.trim(),
                codigo: document.getElementById("resetCode").value
            });
            if (generation !== resetGeneration || !resetDialog.open) return;
            if (valid !== "true") {
                setCodeState("invalid");
                resetMessage.textContent = "Código inválido, expirado o demasiados intentos.";
                return;
            }
            setCodeState("valid");
            resetMessage.textContent = "Código correcto.";
            await new Promise(resolve => setTimeout(resolve, 650));
            if (generation !== resetGeneration || !resetDialog.open) return;
            verifyForm.hidden = true;
            resetIntro.hidden = true;
            changeForm.hidden = false;
            resetMessage.textContent = "Crea una nueva contraseña.";
            resetPassword.focus();
        } catch (error) {
            if (generation === resetGeneration && resetDialog.open) {
                setCodeState("");
                resetMessage.textContent = "No se pudo verificar el código. Inténtalo de nuevo.";
            }
        } finally {
            if (generation === resetGeneration) { checkingCode = false; codeInputs.forEach(input => { input.disabled = false; }); }
        }
    });

    changeForm?.addEventListener("submit", async (event) => {
        event.preventDefault();
        const password = document.getElementById("resetNewPassword").value;
        if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s]).{8,80}$/.test(password)) {
            resetMessage.textContent = "Completa los requisitos de la contraseña.";
            return;
        }
        if (password !== document.getElementById("resetConfirmPassword").value) {
            resetMessage.textContent = "Las contraseñas no coinciden.";
            return;
        }
        const button = changeForm.querySelector("button[type=submit]");
        button.disabled = true;
        try {
            const result = await resetPost("/restablecer-password", {
                correo: document.getElementById("resetEmail").value.trim(),
                codigo: document.getElementById("resetCode").value.trim(),
                password
            });
            if (result !== "OK") throw new Error("Cambio rechazado");
            changeForm.reset();
            resetDialog.close();
            window.location.assign("/interno/login?passwordActualizada");
        } catch (error) {
            resetMessage.textContent = "No se pudo cambiar la contraseña. Revisa el código y los requisitos.";
            button.disabled = false;
        }
    });

    if (previewMode) {
        document.querySelectorAll("input, button, a").forEach((element) => {
            element.setAttribute("tabindex", "-1");
            element.setAttribute("aria-disabled", "true");
        });
    }
});
