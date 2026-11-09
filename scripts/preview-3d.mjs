import http from 'node:http';
import { readFile } from 'node:fs/promises';
import { resolve, extname, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../src/main/resources/static/', import.meta.url));
const port = Number(process.env.DENTIA_PREVIEW_PORT || 4173);
// HTML generado por ClinicaTemplateTest; nunca expone una base de pacientes ni endpoints de escritura.
const clinicalPreview = fileURLToPath(new URL('../target/dentia-verification/preview/clinica.html', import.meta.url));
const types = { '.html':'text/html; charset=utf-8', '.js':'text/javascript; charset=utf-8', '.css':'text/css; charset=utf-8', '.json':'application/json', '.glb':'model/gltf-binary', '.png':'image/png' };
http.createServer(async (request, response) => {
    try {
        if (!['GET','HEAD'].includes(request.method)) { response.writeHead(405); response.end(); return; }
        const path = decodeURIComponent(new URL(request.url, 'http://localhost').pathname);
        if (path === '/clinica-preview') {
            const content = await readFile(clinicalPreview);
            response.writeHead(200, { 'Content-Type':'text/html; charset=utf-8', 'Cache-Control':'no-store', 'X-Content-Type-Options':'nosniff' });
            response.end(request.method === 'HEAD' ? undefined : content);
            return;
        }
        const file = resolve(root, '.' + (path === '/' ? '/dental-studio.html' : path));
        if (!file.startsWith(resolve(root) + sep)) { response.writeHead(403); response.end(); return; }
        const content = await readFile(file);
        response.writeHead(200, { 'Content-Type': types[extname(file)] || 'application/octet-stream', 'Cache-Control':'no-store', 'X-Content-Type-Options':'nosniff' });
        response.end(request.method === 'HEAD' ? undefined : content);
    } catch { response.writeHead(404); response.end('No encontrado'); }
}).listen(port,'127.0.0.1',()=>console.log(`DentIA 3D: http://127.0.0.1:${port}/dental-studio.html`));
