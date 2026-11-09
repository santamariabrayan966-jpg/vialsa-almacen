(() => {
    const get = id => document.getElementById(id);
    document.querySelectorAll('[data-profile-password-toggle]').forEach(button => button.addEventListener('click', () => {
        const input = get(button.dataset.profilePasswordToggle); const visible = input.type === 'password';
        input.type = visible ? 'text' : 'password'; button.setAttribute('aria-label', visible ? 'Ocultar contraseña' : 'Mostrar contraseña');
        button.setAttribute('aria-pressed', String(visible)); button.querySelector('i').className = visible ? 'bi bi-eye-slash' : 'bi bi-eye';
    }));
    const ruleLabels = { length: 'entre 8 y 80 caracteres', upper: 'una mayúscula', lower: 'una minúscula', number: 'un número', symbol: 'un símbolo' };
    function setState(input, feedback, state, message) {
        input.classList.toggle('profile-valid', state === 'valid');
        input.classList.toggle('profile-invalid', state === 'invalid');
        if (state) input.setAttribute('aria-invalid', String(state === 'invalid')); else input.removeAttribute('aria-invalid');
        feedback.classList.toggle('profile-valid', state === 'valid'); feedback.classList.toggle('profile-invalid', state === 'invalid'); feedback.textContent = message;
    }
    function requirements(inputId, listId, feedbackId, confirmId, confirmFeedbackId) {
        const input = get(inputId), list = get(listId), feedback = get(feedbackId), confirm = confirmId && get(confirmId);
        if (!input || !list || !feedback) return;
        const update = () => {
            const value = input.value;
            const rules = {length: value.length >= 8 && value.length <= 80, upper: /[A-Z]/.test(value), lower: /[a-z]/.test(value), number: /[0-9]/.test(value), symbol: /[^\w\s]/.test(value)};
            const missing = Object.keys(rules).filter(key => !rules[key]).map(key => ruleLabels[key]);
            list.querySelectorAll('[data-rule]').forEach(item => { const met = rules[item.dataset.rule]; item.classList.toggle('is-met', met); item.classList.toggle('is-missing', !!value && !met); });
            setState(input, feedback, value ? (missing.length ? 'invalid' : 'valid') : '', value ? (missing.length ? 'Falta: ' + missing.join(', ') + '.' : 'La contraseña cumple todos los requisitos.') : '');
            if (confirm) { const v=confirm.value; setState(confirm,get(confirmFeedbackId), v ? (v===value && value ? 'valid' : 'invalid') : '', v ? (v===value && value ? 'Las contraseñas coinciden.' : 'Las contraseñas no coinciden.') : ''); }
        };
        input.addEventListener('input', update); confirm?.addEventListener('input', update);
        return update;
    }
    const updateOwnPassword = requirements('perfilNuevaContrasena', 'passwordRequirements', 'profilePasswordFeedback');
    const updateResetPassword = requirements('profileResetPassword', 'resetPasswordRequirements', 'profileResetPasswordFeedback', 'profileResetConfirm', 'profileResetConfirmFeedback');
    get('profilePasswordForm')?.addEventListener('submit', event => {
        const input = get('perfilNuevaContrasena');
        if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s]).{8,80}$/.test(input.value)) { event.preventDefault(); updateOwnPassword(); input.focus(); }
    });

    const root = get('profileReset');
    if (!root) return;
    const email = root.dataset.email || '';
    const status = get('profileResetStatus');
    const csrf = document.querySelector('#profilePasswordForm input[name=\"_csrf\"]')?.value || document.getElementById('dentiaCsrf')?.value;
    async function post(url, values) {
        const body = new URLSearchParams(values);
        if (csrf) body.set('_csrf', csrf);
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body
        });
        if (!response.ok) { const error = new Error('request failed'); error.status = response.status; throw error; }
        return (await response.text()).trim();
    }
    async function run(button, operation) {
        button.disabled = true;
        try { await operation(); }
        catch (error) { status.textContent = error.status === 429 ? 'Has solicitado varios códigos. Espera hasta una hora desde la primera solicitud e inténtalo de nuevo.' : error.status === 403 ? 'La sesión venció. Recarga la página e inténtalo de nuevo.' : error.status === 503 ? 'El servicio de correo no está disponible en este momento.' : 'No se pudo completar la operación. Inténtalo más tarde.'; }
        finally { button.disabled = false; }
    }
    get('profileResetOpen').addEventListener('click', () => {
        get('profileResetSteps').hidden = false;
        status.textContent = email ? '' : 'Agrega un correo a tu perfil y guarda los cambios para recuperar tu contraseña.';
        get('profileResetSend').disabled = !email;
    });
    get('profileResetSend').addEventListener('click', event => run(event.currentTarget, async () => {
        await post('/recuperar-password', { correo: email });
        get('profileResetVerify').hidden = false;
        get('profileResetChange').hidden = true;
        status.textContent = 'Si el correo está habilitado para recuperación, recibirás un código. Revisa también spam.';
    }));
    get('profileResetCheck').addEventListener('click', event => run(event.currentTarget, async () => {
        const codigo = get('profileResetCode').value.trim();
        if (!/^\d{4}$/.test(codigo) || await post('/validar-codigo-password', { correo: email, codigo }) !== 'true') {
            status.textContent = 'El código es inválido o ha vencido.';
            get('profileResetChange').hidden = true;
            return;
        }
        get('profileResetChange').hidden = false;
        status.textContent = 'Código verificado. Crea tu nueva contraseña.';
    }));
    get('profileResetSubmit').addEventListener('click', event => run(event.currentTarget, async () => {
        const password = get('profileResetPassword').value;
        if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s]).{8,80}$/.test(password)) {
            status.textContent = 'Completa todos los requisitos de la contraseña.';
            return;
        }
        if (password !== get('profileResetConfirm').value) {
            status.textContent = 'Las contraseñas no coinciden.';
            return;
        }
        const result = await post('/restablecer-password', { correo: email, codigo: get('profileResetCode').value.trim(), password });
        if (result !== 'OK') throw new Error('rejected');
        get('profileResetPassword').value = '';
        get('profileResetConfirm').value = '';
        updateResetPassword?.();
        get('profileResetChange').hidden = true;
        get('profileResetVerify').hidden = true;
        status.textContent = 'Contraseña actualizada correctamente.';
    }));
})();
