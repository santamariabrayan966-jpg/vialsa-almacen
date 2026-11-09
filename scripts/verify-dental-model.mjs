import { readFileSync } from 'node:fs';
import assert from 'node:assert/strict';
const path = new URL('../src/main/resources/static/models/dental/dentia-articulado-v2.glb', import.meta.url);
const binary = readFileSync(path);
assert.equal(binary.toString('utf8',0,4), 'glTF'); assert.equal(binary.readUInt32LE(4),2);
assert.equal(binary.readUInt32LE(8),binary.length);
assert.equal(binary.toString('ascii',16,20), 'JSON');
const document = JSON.parse(binary.toString('utf8',20,20+binary.readUInt32LE(12)));
const manifest = JSON.parse(readFileSync(new URL('../assets/dental/articulated-v2/model_manifest.json', import.meta.url), 'utf8'));
const publishedManifest = JSON.parse(readFileSync(new URL('../src/main/resources/static/models/dental/dentia-articulado-v2.json', import.meta.url), 'utf8'));
assert.deepEqual(publishedManifest, manifest, 'El manifiesto publicado difiere del archivo fuente');
assert.equal(manifest.web_bytes, binary.length);
assert.equal(manifest.crown_count, 33);
const expectedFdi = new Set([1,2,3,4].flatMap(quadrant => Array.from({length: 8}, (_, index) => `FDI_${quadrant}${index + 1}`)));
const teeth = document.nodes.filter(n=>/^FDI_\d{2}$/.test(n.name));
assert.deepEqual(new Set(teeth.map(n=>n.name)), expectedFdi, 'Faltan o sobran piezas FDI permanentes');
assert.equal(manifest.teeth.length, 33);
assert.equal(new Set(manifest.teeth.map(t=>t.tooth_id)).size, 33);
for (const tooth of teeth) {
    const entry = manifest.teeth.find(t=>t.tooth_id===tooth.name);
    assert(entry, tooth.name + ' ausente del manifiesto');
    assert.equal(tooth.extras?.fdi, entry.fdi, tooth.name + ' con FDI inconsistente');
    assert.equal(tooth.extras?.arch, entry.arch, tooth.name + ' en arcada inconsistente');
    const parts = tooth.children.map(i=>document.nodes[i]);
    for (const part of ['crown', 'roots']) {
        assert(parts.some(node=>node.extras?.part===part && node.extras?.tooth_id===tooth.name), tooth.name + ' sin ' + part + ' propia');
    }
    assert.equal(parts.find(node=>node.extras?.part==='roots').extras?.root_count, entry.root_count);
}
const additional = document.nodes.filter(n=>n.extras?.additional);
assert.equal(additional.length,1);
assert.equal(additional[0].name, 'EXTRA_L_01');
assert.equal(manifest.teeth.find(t=>t.tooth_id==='EXTRA_L_01')?.fdi, null);
assert.equal(document.nodes.filter(n=>n.extras?.part==='crown').length, manifest.crown_count);
assert.equal(document.nodes.filter(n=>n.extras?.part==='roots').length, manifest.teeth.length);
assert.equal(document.nodes.filter(n=>n.extras?.part==='gingiva').length,2);
const jaw = document.nodes.findIndex(n=>n.name.startsWith('MANDIBULA'));
assert(jaw>=0);
assert(document.animations.some(animation=>animation.channels.some(c=>c.target.node===jaw && c.target.path==='rotation')),'Falta apertura animada');
assert.deepEqual(binary,readFileSync(new URL('../assets/dental/articulated-v2/dentia_articulado.glb',import.meta.url)));
console.log(JSON.stringify({ok:true,teeth:teeth.length,additional:additional.length,crowns:manifest.crown_count,roots:manifest.teeth.length,gums:2,animations:document.animations.map(a=>a.name),bytes:binary.length},null,2));
