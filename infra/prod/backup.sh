#!/usr/bin/env bash
# Backup diário do Postgres de produção (ADR 0013), enviado para fora da VPS com o rclone.
#
# Instalação (uma vez, na VPS):
#   1. rclone config  -> crie o remote do R2 ou B2 (ex.: "r2") e um bucket privado.
#   2. chmod +x /opt/rota-dev/backup.sh
#   3. crontab -e  ->  15 4 * * * /opt/rota-dev/backup.sh >> /var/log/rota-dev-backup.log 2>&1
#
# Restaurar: veja docs/deploy.md ("Restaurar um backup"). Um backup que nunca foi restaurado é só esperança.
set -euo pipefail

cd "$(dirname "$0")"
# shellcheck disable=SC1091
source .env

stamp=$(date -u +%Y-%m-%dT%H%M%SZ)
file="/tmp/rota-dev-${stamp}.sql.gz"

# pg_dump roda dentro do container do banco: a versão do cliente é a mesma do servidor.
docker compose exec -T postgres pg_dump -U "${POSTGRES_USER:-rotadev}" -d "${POSTGRES_DB:-rotadev}" --no-owner \
  | gzip -9 > "$file"

# Um dump vazio ou minúsculo significa erro silencioso: melhor falhar alto.
if [ "$(stat -c %s "$file")" -lt 1024 ]; then
  echo "backup suspeito (menos de 1 KB): $file" >&2
  exit 1
fi

rclone copy "$file" "${BACKUP_REMOTE}/db/" --s3-no-check-bucket
rclone delete "${BACKUP_REMOTE}/db/" --min-age "${BACKUP_RETENTION_DAYS:-30}d"
rm -f "$file"

# Os uploads (currículos, exports) também são dado do usuário: cópia incremental do volume.
uploads=$(docker volume inspect rota-dev_uploads --format '{{ .Mountpoint }}')
rclone sync "$uploads" "${BACKUP_REMOTE}/uploads/"

echo "backup ok: ${stamp}"
