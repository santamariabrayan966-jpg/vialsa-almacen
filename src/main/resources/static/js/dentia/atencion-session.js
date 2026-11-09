/* Estado de edición en memoria: nunca persiste información clínica en el navegador. */
(function (scope) {
    'use strict';
    class AtencionSession {
        constructor(pacienteId, version, contenido, createId = () => crypto.randomUUID()) {
            this.pacienteId = Number(pacienteId);
            this.version = Number(version);
            this.saved = JSON.stringify(contenido);
            this.createId = createId;
            this.busy = false;
            this.pending = null;
            this.conflicted = false;
        }
        isDirty(contenido) {
            return this.busy || !!this.pending || this.saved !== JSON.stringify(contenido);
        }
        begin(contenido, finalizar) {
            if (this.busy) throw new Error('Espera a que termine el guardado.');
            if (this.conflicted) throw new Error('Recarga el borrador actualizado antes de continuar.');
            if (Number(contenido.pacienteId) !== this.pacienteId) throw new Error('El paciente del formulario cambió.');
            if (this.pending && (!finalizar || JSON.stringify(contenido) !== JSON.stringify(this.pending.contenido)))
                throw new Error('Reintenta la finalización pendiente antes de modificar esta atención.');
            const snapshot = JSON.parse(JSON.stringify(contenido));
            const request = finalizar
                ? (this.pending || { version: this.version, solicitudId: this.createId(), contenido: snapshot })
                : { pacienteId: this.pacienteId, version: this.version, contenido: snapshot };
            if (finalizar) this.pending = request;
            this.busy = true;
            return JSON.parse(JSON.stringify(request));
        }
        savedDraft(contenido, version) {
            this.saved = JSON.stringify(contenido);
            this.version = Number(version);
            this.busy = false;
        }
        failed(status) {
            this.busy = false;
            if (status === 409) {
                this.pending = null;
                this.conflicted = true;
                return;
            }
            // Una respuesta perdida o un 5xx puede llegar después del commit: conservar la clave.
            if (status >= 400 && status < 500 && status !== 408) this.pending = null;
        }
    }
    if (typeof module !== 'undefined' && module.exports) module.exports = AtencionSession;
    else scope.DentiaAtencionSession = AtencionSession;
})(typeof globalThis !== 'undefined' ? globalThis : this);
