const { test } = require('node:test');
const assert = require('node:assert/strict');
const Session = require('../src/main/resources/static/js/dentia/atencion-session.js');
const content = () => ({ pacienteId: 7, planProcedimiento: 'Control' });
const session = () => new Session(7, 3, content(), () => 'fixed-test-id');

test('guarda con la versión de apertura y actualiza la revisión confirmada', () => {
    const s = session(), data = { ...content(), subjetivo: 'Nota' };
    assert.equal(s.isDirty(content()), false);
    assert.equal(s.isDirty(data), true);
    assert.equal(s.begin(data, false).version, 3);
    s.savedDraft(data, 4);
    assert.equal(s.isDirty(data), false);
    assert.equal(s.begin(data, true).version, 4);
});
test('impide doble clic y guardar mientras finaliza', () => {
    const s = session();
    s.begin(content(), true);
    assert.throws(() => s.begin(content(), true), /Espera/);
    assert.throws(() => s.begin(content(), false), /Espera/);
});
test('reintenta con exactamente la misma clave tras perder la respuesta', () => {
    const s = session(), first = s.begin(content(), true);
    s.failed(undefined);
    assert.deepEqual(s.begin(content(), true), first);
    s.failed(500);
    assert.deepEqual(s.begin(content(), true), first);
});
test('un cierre incierto bloquea cambios y conserva una copia independiente', () => {
    const s = session(), original = content();
    const request = s.begin(original, true);
    original.planProcedimiento = 'Cambio externo';
    request.contenido.planProcedimiento = 'Cambio externo';
    s.failed(500);
    assert.throws(() => s.begin(original, true), /Reintenta/);
    assert.throws(() => s.begin(content(), false), /Reintenta/);
    assert.equal(s.begin(content(), true).contenido.planProcedimiento, 'Control');
});
test('un conflicto conserva la revisión y bloquea nuevos intentos hasta recargar', () => {
    const s = session();
    s.begin(content(), true); s.failed(409);
    assert.equal(s.pending, null);
    assert.equal(s.version, 3);
    assert.equal(s.conflicted, true);
    assert.throws(() => s.begin({ ...content(), subjetivo: 'Nota' }, false), /Recarga/);
});
test('un error de validación libera el intento sin marcar conflicto', () => {
    const s = session();
    s.begin(content(), true); s.failed(400);
    assert.equal(s.pending, null);
    assert.equal(s.conflicted, false);
    assert.equal(s.begin({ ...content(), subjetivo: 'Corregido' }, false).version, 3);
});
test('rechaza cambiar de paciente dentro de una sesión de edición', () => {
    assert.throws(() => session().begin({ ...content(), pacienteId: 8 }, false), /paciente/);
});
