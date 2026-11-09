#!/usr/bin/env python3
"""A personally authenticated SSH connection reused by SFTP and queued DentIA jobs.

No password storage, command-line credentials, host-key autoaccept, sudoers edit,
or automatic reconnect. A single sudo -v authenticates the root job shell in the
same PTY. All subsequent work uses this same SSH transport and shell.
"""
import argparse
import datetime
import getpass
import hashlib
import json
import logging
import os
import pathlib
import re
import shlex
import sys
import tarfile
import time
import uuid

WORKSPACE = pathlib.Path(__file__).resolve().parent.parent
sys.path.insert(0, r'C:/Users/Brayan/Desktop/control de pagos/target/ssh-runner-deps')
import paramiko


def atomic_json(path, value):
    temporary = path.with_suffix('.pending')
    temporary.write_text(json.dumps(value, indent=2), encoding='utf-8')
    os.replace(temporary, path)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--session-directory', required=True, type=pathlib.Path)
    parser.add_argument('--ssh-key', type=pathlib.Path)
    parser.add_argument('--pause-on-exit', action='store_true')
    args = parser.parse_args()
    root = args.session_directory.resolve()
    assert root.is_relative_to(WORKSPACE / 'target'), 'Session must stay in the private workspace target directory'
    root.mkdir(parents=True, exist_ok=True)
    (root / 'queue').mkdir(exist_ok=True)
    state = {'connection': 'awaiting_personal_authentication', 'root_authenticated': False, 'jobs': {}}
    status = root / 'status.json'
    atomic_json(status, state)
    logging.getLogger('paramiko').setLevel(logging.WARNING)
    client = paramiko.SSHClient()
    client.load_host_keys(str(pathlib.Path.home() / '.ssh/known_hosts'))
    client.set_missing_host_key_policy(paramiko.RejectPolicy())
    print('DENTIA: una conexion SSH para transferencias, despliegue y comprobaciones.', flush=True)
    print('Introduce las contrasenas solo aqui: SSH una vez y sudo solo si se solicita. Nunca se guardan.', flush=True)
    password = None if args.ssh_key else getpass.getpass('Contrasena Ubuntu/SSH (no se muestra): ')
    try:
        client.connect('100.90.117.108', username='brayan', password=password,
                       key_filename=str(args.ssh_key.resolve()) if args.ssh_key else None,
                       allow_agent=False, look_for_keys=False, timeout=15,
                       auth_timeout=45, banner_timeout=30)
        password = None
        client.get_transport().set_keepalive(30)
        channel = client.get_transport().open_session()
        channel.get_pty(term='dumb', width=160, height=40)
        channel.exec_command("sudo -p '__DENTIA_SUDO_PASSWORD__\n' -v && printf '\n__DENTIA_SUDO_OK__\n' && sudo -n /bin/bash --noprofile --norc -s")
        auth = b''
        sent = False
        deadline = time.monotonic() + 300
        while time.monotonic() < deadline:
            if channel.recv_ready():
                auth += channel.recv(65536)
                if b'__DENTIA_SUDO_OK__' in auth:
                    break
                if b'__DENTIA_SUDO_PASSWORD__' in auth:
                    if sent:
                        raise RuntimeError('Sudo rejected authentication; personal action is required.')
                    password = getpass.getpass('Autorizacion sudo -v (una vez, no se muestra): ')
                    channel.sendall(password.encode('utf-8') + b'\n')
                    password = None
                    sent = True
                    auth = b''
            if channel.exit_status_ready():
                raise RuntimeError('Sudo no autorizó la sesión. Se agotaron los intentos o se canceló la operación.')
            time.sleep(.1)
        else:
            raise RuntimeError('Personal sudo authentication timed out.')
        password = None
        auth = b''
        channel.sendall(b'stty -echo\nexport LC_ALL=C.UTF-8\numask 077\n')
        sftp = client.open_sftp()
        remote = '/home/brayan/.cache/dentia-deploy/session-' + uuid.uuid4().hex
        sftp.mkdir(remote, mode=0o700)
        state.update(connection='connected', root_authenticated=True, remote_directory=remote)
        atomic_json(status, state)
        print('SSH y sudo listos. Conserva esta ventana abierta; no hace falta copiar comandos ni volver a introducir contrasenas.', flush=True)
        last_work = time.monotonic()
        with (root / 'session.log').open('a', encoding='utf-8', buffering=1) as log:
            while client.get_transport().is_active() and not channel.closed:
                if (root / 'close-session').exists():
                    break
                jobs = sorted((root / 'queue').glob('*.json'))
                waiting = [p for p in jobs if p.stem not in state['jobs']]
                if not waiting:
                    if channel.exit_status_ready():
                        raise RuntimeError('The authorized shell ended; inspect before reconnecting.')
                    if time.monotonic() - last_work > 1800:
                        print('Sesion cerrada tras 30 minutos sin tareas.', flush=True)
                        break
                    time.sleep(.5)
                    continue
                item = waiting[0]
                job_id = item.stem
                assert re.fullmatch(r'[a-zA-Z0-9_-]+', job_id)
                job = json.loads(item.read_text(encoding='utf-8-sig'))
                archive = pathlib.Path(job['archive']).resolve()
                assert archive.is_relative_to(WORKSPACE / 'target') and archive.is_file()
                assert archive.stat().st_size <= 128 * 1024**2
                entry = job['entrypoint']
                assert re.fullmatch(r'[a-z0-9-]+\.sh', entry)
                with tarfile.open(archive, 'r:gz') as bundle:
                    members = bundle.getmembers()
                    assert members and sum(p.size for p in members) <= 256 * 1024**2
                    for member in members:
                        p = pathlib.PurePosixPath(member.name)
                        assert not p.is_absolute() and '..' not in p.parts
                        assert member.isfile() or member.isdir(), 'Links and special archive members are forbidden'
                    assert any(m.name in (entry, './' + entry) for m in members)
                job_dir = remote + '/' + job_id
                sftp.mkdir(job_dir, mode=0o700)
                sftp.put(str(archive), job_dir + '/payload.tar.gz')
                sftp.chmod(job_dir + '/payload.tar.gz', 0o600)
                digest = hashlib.sha256(archive.read_bytes()).hexdigest()
                qdir = shlex.quote(job_dir)
                qarchive = shlex.quote(job_dir + '/payload.tar.gz')
                marker = '__DENTIA_JOB_' + job_id + '_END__'
                command = ("if printf '%s  %s\\n' " + shlex.quote(digest) + ' ' + qarchive
                           + ' | sha256sum --check --status && tar -xzf ' + qarchive + ' -C ' + qdir
                           + ' && /bin/bash ' + shlex.quote(job_dir + '/' + entry) + ' ' + qdir
                           + "; then suma_rc=0; else suma_rc=$?; fi; printf '\\n" + marker + ":%s\\n' \"$suma_rc\"\n")
                state['jobs'][job_id] = {'status': 'running', 'started': datetime.datetime.now(datetime.timezone.utc).isoformat()}
                atomic_json(status, state)
                print('Ejecutando:', job_id, flush=True)
                channel.sendall(command.encode('utf-8'))
                output = ''
                deadline = time.monotonic() + 3600
                with (root / (job_id + '.log')).open('w', encoding='utf-8', buffering=1) as job_log:
                    while time.monotonic() < deadline:
                        if channel.recv_ready():
                            text = channel.recv(65536).decode('utf-8', errors='replace')
                            output += text
                            print(text, end='', flush=True)
                            log.write(text)
                            job_log.write(text)
                            result = re.search(re.escape(marker) + r':([0-9]+)', output)
                            if result:
                                code = int(result.group(1))
                                state['jobs'][job_id].update(status='completed' if code == 0 else 'failed', exit_code=code)
                                break
                            if len(output) > 200000:
                                output = output[-100000:]
                        if channel.closed or not client.get_transport().is_active():
                            raise RuntimeError('Connection lost during a job. Outcome unknown; do not automatically repeat it.')
                        time.sleep(.1)
                    else:
                        raise RuntimeError('Job timeout: outcome requires inspection before retrying.')
                last_work = time.monotonic()
                results = root / 'results' / job_id
                results.mkdir(parents=True, exist_ok=True)
                for filename in ('deployment.json', 'SERVER.md', 'DEPLOYMENT.md', 'BACKUP-RESTORE.md'):
                    try:
                        info = sftp.stat(job_dir + '/results/' + filename)
                        if info.st_size > 1024 * 1024:
                            raise RuntimeError('Unexpected result size')
                        sftp.get(job_dir + '/results/' + filename, str(results / filename))
                    except FileNotFoundError:
                        pass
                atomic_json(status, state)
                print('\nEtapa terminada. La misma conexion queda disponible para las siguientes comprobaciones.', flush=True)
        channel.close()
        state.update(connection='closed', root_authenticated=False)
    except Exception as error:
        # Exception text from authentication libraries may contain server details;
        # never print supplied credentials or private response bodies.
        state.update(connection='attention_required', root_authenticated=False, error_type=type(error).__name__, reason=str(error) if isinstance(error, RuntimeError) else 'Revisar conexión o carpeta de publicación')
        print('Sesion detenida:', str(error) if isinstance(error, RuntimeError) else type(error).__name__, '. No se reintentara automaticamente.', flush=True)
    finally:
        password = None
        client.close()
        atomic_json(status, state)
        print('Estado guardado localmente. No se han almacenado contrasenas.', flush=True)
        if args.pause_on_exit:
            input('Pulsa Enter para cerrar esta ventana. ')


if __name__ == '__main__':
    main()
