#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
jar=/home/ubuntu/pedidos360-backend-0.0.1-SNAPSHOT.jar
envfile=/etc/pedidos360-public.env
override=/etc/systemd/system/pedidos360.service.d/zz-pedidos360-public.conf
site=https://alvasmash.github.io/CloudNative/
origin=https://alvasmash.github.io

for command in java curl sha256sum python3; do command -v "$command" >/dev/null || { echo "Falta instalar: $command"; exit 1; }; done
[[ -f "$jar" ]] || { echo 'No se encontro el JAR actual; no se modifico el servicio.'; exit 1; }
systemctl show pedidos360 -p ExecStart --value | grep -Fq "$jar" || { echo 'La ruta del servicio es diferente; revisala antes de continuar.'; exit 1; }
sha256sum --check backend.sha256

reuse_env=false
if sudo test -s "$envfile"; then
    reuse_env=true
    echo 'Se conserva la configuracion de Gmail existente en AWS.'
else
read -r -p 'Gmail remitente (la cuenta que ya configuraste en local): ' sender
[[ "$sender" =~ ^[A-Za-z0-9._%+-]+@gmail\.com$ ]] || { echo 'Direccion Gmail no valida.'; exit 1; }
while true; do
    read -r -s -p 'Clave de aplicacion de Google (oculta, 16 caracteres): ' password
    printf '\n'
    password="${password//[[:space:]]/}"
    [[ "$password" =~ ^[A-Za-z0-9]{16}$ ]] && break
    echo 'Copia solo la clave de 16 caracteres generada por Google.'
done
fi

stage=$(mktemp -d "$HOME/pedidos360-release.XXXXXXXX")
chmod 700 "$stage"
if $reuse_env; then
    sudo cat "$envfile" > "$stage/public.env"
else
printf 'SMTP_HOST=smtp.gmail.com\nSMTP_PORT=587\nSMTP_USERNAME=%s\nSMTP_PASSWORD=%s\nSMTP_AUTH=true\nSMTP_STARTTLS=true\nMAIL_FROM=%s\nFRONTEND_URL=%s\nFRONTEND_HASH_ROUTING=true\nAPP_SECURITY_ALLOWED_ORIGINS=%s\n' \
    "$sender" "$password" "$sender" "$site" "$origin" > "$stage/public.env"
unset password
fi

sudo cp -p -- "$jar" "$stage/previous.jar"
had_env=false; had_override=false
if sudo test -f "$envfile"; then sudo cp -p -- "$envfile" "$stage/previous.env"; had_env=true; fi
if sudo test -f "$override"; then sudo cp -p -- "$override" "$stage/previous.conf"; had_override=true; fi

rollback() {
    trap - ERR
    echo 'La actualizacion fallo. Recuperando el backend anterior...'
    sudo systemctl stop pedidos360 || true
    sudo cp -p -- "$stage/previous.jar" "$jar"
    if $had_env; then sudo cp -p -- "$stage/previous.env" "$envfile"; else sudo rm -f -- "$envfile"; fi
    if $had_override; then sudo cp -p -- "$stage/previous.conf" "$override"; else sudo rm -f -- "$override"; fi
    sudo systemctl daemon-reload
    sudo systemctl start pedidos360 || true
    echo "Copia de recuperacion: $stage"
    exit 1
}
trap rollback ERR
sudo install -m 600 -- "$stage/public.env" "$envfile"
sudo mkdir -p -- "$(dirname -- "$override")"
printf '[Service]\nEnvironmentFile=%s\n' "$envfile" | sudo tee "$override" >/dev/null
sudo systemctl stop pedidos360
sudo install -m 644 -- backend.jar "$jar"
sudo systemctl daemon-reload
sudo systemctl start pedidos360

healthy=false
for attempt in $(seq 1 40); do
    if curl --noproxy '*' -fsS --max-time 8 http://127.0.0.1:8080/public/productos >/dev/null 2>&1 \
        && curl --noproxy '*' -fsS --max-time 8 http://127.0.0.1:8080/public/auth/mail-mode | python3 -c 'import json,sys; c=json.load(sys.stdin); sys.exit(0 if c.get("mode")=="SMTP" and c.get("passwordResetAvailable") is True else 1)'; then
        healthy=true; break
    fi
    sleep 3
done
$healthy || { echo 'El backend no supero la comprobacion de arranque.'; false; }
curl --noproxy '*' -fsS --max-time 10 -D "$stage/cors.headers" -o /dev/null \
    -H "Origin: $origin" http://127.0.0.1:8080/public/productos
grep -iq "^access-control-allow-origin: $origin" "$stage/cors.headers"

python3 - <<'PY'
import smtplib, ssl
values = {}
# Read the application environment privately through sudo, never print credentials.
import subprocess
content = subprocess.check_output(['sudo', 'cat', '/etc/pedidos360-public.env'], text=True)
for line in content.splitlines():
    key, _, value = line.partition('=')
    values[key] = value
with smtplib.SMTP('smtp.gmail.com', 587, timeout=20) as smtp:
    smtp.ehlo()
    smtp.starttls(context=ssl.create_default_context())
    smtp.ehlo()
    smtp.login(values['SMTP_USERNAME'], values['SMTP_PASSWORD'])
print('Autenticacion SMTP Gmail: OK')
PY

trap - ERR
echo 'Backend actualizado: API y CORS comprobados; Gmail autentica correctamente.'
echo "Copia de recuperacion: $stage"
echo 'No hace falta abrir los puertos de Kafka o RabbitMQ al publico.'
