#!/usr/bin/env python3
"""Publicación autorizada de DentIA. No reinicia Suma, MySQL ni Cloudflare."""
import datetime, gzip, hashlib, http.cookiejar, json, os, pathlib, re, secrets, shlex, shutil, subprocess, sys, time, urllib.parse, urllib.request

job = pathlib.Path(sys.argv[1]).resolve()
assert os.geteuid() == 0
assert str(job).startswith('/home/brayan/.cache/dentia-deploy/session-')
meta = json.loads((job / 'release.json').read_text())
jar = job / 'dentia.jar'
assert hashlib.sha256(jar.read_bytes()).hexdigest() == meta['sha256']
stamp = datetime.datetime.now().strftime('%Y%m%d-%H%M%S')
backup = pathlib.Path('/var/backups/dentia') / ('release-' + stamp)
backup.mkdir(mode=0o700, parents=True)
os.chmod(backup, 0o700)
results = job / 'results'; results.mkdir(exist_ok=True)
report = {'status': 'PREPARING', 'backup': str(backup), 'sha256': meta['sha256']}

def run(args, **kwargs):
    return subprocess.run(args, check=True, text=True, capture_output=True, **kwargs)

def save():
    (results / 'deployment.json').write_text(json.dumps(report, indent=2))
    print(json.dumps(report), flush=True)

def pid(service):
    return run(['systemctl', 'show', service, '-p', 'MainPID', '--value']).stdout.strip()

protected = {s: pid(s) for s in ('suma', 'mysql', 'cloudflared')}
cloud = pathlib.Path('/etc/cloudflared/config.yml')
cloud_hash = hashlib.sha256(cloud.read_bytes()).hexdigest()
envfile = pathlib.Path('/etc/dentia/dentia.env')
current = pathlib.Path('/opt/dentia/current.jar')
assert current.is_file()
shutil.copy2(current, backup / 'previous.jar')
shutil.copy2(envfile, backup / 'previous.env')
env = {}
for line in envfile.read_text().splitlines():
    if not line.strip() or line.lstrip().startswith('#'): continue
    key, value = line.split('=', 1)
    parts = shlex.split(value)
    env[key.strip()] = parts[0] if len(parts) == 1 else value

# El respaldo se verifica restaurándolo en una base exclusivamente temporal.
run(['mysql', '--protocol=socket', '-NBe', 'SELECT 1'])
dump = backup / 'database.sql.gz'
with gzip.open(dump, 'wb') as out:
    proc = subprocess.Popen(['mysqldump', '--protocol=socket', '--single-transaction', '--routines', '--triggers', '--no-tablespaces', 'dentia'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    shutil.copyfileobj(proc.stdout, out)
    err = proc.stderr.read(); assert proc.wait() == 0, 'No se pudo respaldar MySQL'
assert dump.stat().st_size > 100
verify_db = 'dentia_verify_' + stamp.replace('-', '_')
assert re.fullmatch(r'dentia_verify_[0-9_]+', verify_db)
run(['mysql', '--protocol=socket', '-e', 'CREATE DATABASE ' + verify_db])
try:
    with gzip.open(dump, 'rb') as source:
        proc = subprocess.Popen(['mysql', '--protocol=socket', verify_db], stdin=subprocess.PIPE, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
        shutil.copyfileobj(source, proc.stdin); proc.stdin.close()
        err = proc.stderr.read(); assert proc.wait() == 0, 'No se pudo verificar el respaldo'
    def signatures(db):
        return run(['mysql', '--protocol=socket', '-NBe', 'SELECT COUNT(*),COALESCE(SUM(id_paciente),0) FROM ' + db + '.pacientes']).stdout
    assert signatures('dentia') == signatures(verify_db), 'El respaldo no coincide'
finally:
    run(['mysql', '--protocol=socket', '-e', 'DROP DATABASE ' + verify_db])
report['backup_restored'] = True

def users_signature():
    query = "SELECT idUsuario,SHA2(Contrasena,256) FROM dentia.usuarios ORDER BY idUsuario"
    return hashlib.sha256(run(['mysql', '--protocol=socket', '-NBe', query]).stdout.encode()).hexdigest()
users_before = users_signature()

additions = {}
for name in ('DENTIA_PASSWORD_RESET_KEY', 'DENTIA_REMEMBER_ME_KEY'):
    if not env.get(name): additions[name] = secrets.token_hex(32)
if not env.get('DENTIA_DB_HOST'): additions['DENTIA_DB_HOST'] = env.get('DB_HOST', 'localhost')
if not env.get('DENTIA_DB_USERNAME'):
    val = env.get('SPRING_DATASOURCE_USERNAME') or env.get('DB_USERNAME') or env.get('DB_USER')
    assert val, 'Falta identificar la variable de usuario MySQL'
    additions['DENTIA_DB_USERNAME'] = val
if 'DENTIA_DB_PASSWORD' not in env:
    val = env.get('SPRING_DATASOURCE_PASSWORD') or env.get('DB_PASSWORD')
    assert val is not None, 'Falta identificar la variable de contraseña MySQL'
    additions['DENTIA_DB_PASSWORD'] = val
additions['SPRING_PROFILES_ACTIVE'] = 'prod'
# Se inicializan únicamente tablas de gestión aditivas si faltan.
additions['DENTIA_GESTION_INITIALIZE_SCHEMA'] = 'true'
original_env = envfile.read_text()
envfile.write_text(original_env + '\n' + '\n'.join(k + '=' + shlex.quote(v) for k,v in additions.items()) + '\n')
os.chmod(envfile, 0o600)
candidate = pathlib.Path('/opt/dentia/current.jar.next')
shutil.copyfile(jar, candidate); shutil.chown(candidate, user='dentia', group='dentia'); os.chmod(candidate, 0o640)
switched = False
try:
    os.replace(candidate, current); switched = True
    run(['systemctl', 'restart', 'dentia'])
    ready = False
    for _ in range(90):
        try:
            with urllib.request.urlopen('http://127.0.0.1:8080/interno/login', timeout=3) as r:
                html = r.read().decode(); ready = r.status == 200 and 'loginEmpleadoForm' in html
            if ready: break
        except Exception: pass
        time.sleep(2)
    assert ready, 'DentIA no pasó la comprobación de arranque'
    # Prueba HTTPS del error reportado: usuario inexistente no debe producir blanco.
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
    url = 'https://dentia.dodsix.com/interno/login'
    req = urllib.request.Request(url, headers={'User-Agent':'Mozilla/5.0 DentIA-Release-Check'})
    html = opener.open(req, timeout=25).read().decode()
    token = re.search(r'<meta\s+name="_csrf"\s+content="([^"]+)"', html)
    assert token, 'No se recibió el formulario HTTPS'
    data = urllib.parse.urlencode({'username':'dentia_release_check_' + secrets.token_hex(6), 'password':secrets.token_hex(16), '_csrf':token[1]}).encode()
    response = opener.open(urllib.request.Request(url, data=data, headers={'User-Agent':'Mozilla/5.0 DentIA-Release-Check'}), timeout=25)
    body = response.read().decode()
    assert response.status == 200 and 'loginEmpleadoForm' in body and 'incorrectos' in body, 'El acceso inválido no regresó al formulario'
    assert users_signature() == users_before, 'Cambió una credencial de usuario'
    assert {s:pid(s) for s in protected} == protected, 'Cambió un servicio protegido'
    assert hashlib.sha256(cloud.read_bytes()).hexdigest() == cloud_hash
    report.update(status='PASS', public_login=True, invalid_login_visible=True, credentials_preserved=True, protected_services_unchanged=True)
    save()
except Exception:
    if switched:
        shutil.copy2(backup / 'previous.jar', current)
        shutil.copy2(backup / 'previous.env', envfile)
        run(['systemctl', 'restart', 'dentia'])
    report['status'] = 'ROLLED_BACK'; save()
    raise
