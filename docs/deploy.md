# Deploy na VPS

Como o Rota Dev vai para produção (ADR 0013). **Nada daqui foi executado ainda**: é o roteiro para
a primeira vez e para o dia a dia.

```
GitHub (main) ──release.yml──▶ GHCR (imagens linux/amd64, tag = sha do commit)
                                   │ docker compose pull
VPS ─ Caddy (compose edge, 80/443) ─▶ rota-dev-web:3000 ─▶ api:8080 ─▶ postgres
```

- `infra/prod/compose.yaml`: Postgres, API e web, com limite de memória e sem porta exposta.
- `infra/edge/`: o Caddy compartilhado entre as aplicações da VPS (HTTPS automático).
- `infra/prod/backup.sh`: `pg_dump` diário e cópia dos uploads para o R2 ou B2.
- `.github/workflows/release.yml`: publica as imagens a cada merge na `main`.

## 1. Preparar (uma vez)

1. **DNS:** registro `A` do domínio (ex.: `rota.caiomachadodev.com.br`) apontando para o IP da VPS.
2. **OAuth App de produção** no GitHub, separada da de desenvolvimento, com o callback
   `https://<domínio>/api/login/oauth2/code/github`.
3. **Imagens do GHCR:** os pacotes nascem privados. Ou torne `rota-dev-api` e `rota-dev-web` públicos
   (o repositório já é público), ou faça `docker login ghcr.io` na VPS com um token só de leitura
   (`read:packages`).
4. **Na VPS:**
   ```bash
   docker network create edge
   sudo mkdir -p /opt/rota-dev && sudo chown "$USER" /opt/rota-dev
   # copie infra/prod/compose.yaml, infra/prod/backup.sh e infra/prod/.env.example para /opt/rota-dev
   cp /opt/rota-dev/.env.example /opt/rota-dev/.env && chmod 600 /opt/rota-dev/.env
   # preencha o .env (senhas com: openssl rand -base64 32)
   ```
   Guarde uma cópia da `ROTA_ENCRYPTION_KEY` fora da VPS (gerenciador de senhas): sem ela, os tokens
   do GitHub gravados não abrem mais.

## 2. Migração do Caddy (janela combinada: o Gasto Certo está em produção)

Hoje o Caddy das portas 80/443 é do compose do Gasto Certo. O plano é movê-lo para o compose `edge`,
compartilhado. Faça num horário de pouco uso; a troca derruba o HTTPS por alguns segundos.

1. Copie `infra/edge/` para `/opt/edge` na VPS e defina `ACME_EMAIL` e `ROTA_DEV_DOMAIN` num `/opt/edge/.env`.
2. Escreva `/opt/edge/sites/gastocerto.caddy` com o bloco do site que hoje está no Caddyfile do Gasto
   Certo, trocando o destino do `reverse_proxy` pelo nome do container dele na rede `edge`.
3. Ligue o container do Gasto Certo à rede `edge`: `docker network connect edge <container-da-api-do-gastocerto>`
   (e, depois, declare a rede no compose dele para sobreviver a recriações).
4. **Copie o volume de dados do Caddy antigo** para o `edge_caddy_data`, para não reemitir os certificados.
5. Pare o Caddy antigo (`docker compose stop caddy` no Gasto Certo) e suba o novo (`docker compose up -d` em `/opt/edge`).
6. Teste os dois domínios. **Se algo falhar:** `docker compose down` no edge e `docker compose start caddy`
   no Gasto Certo voltam ao estado anterior.

## 3. Primeiro deploy

```bash
cd /opt/rota-dev
# ROTA_VERSION no .env = sha completo do commit da main (aba Actions → release, ou git rev-parse origin/main)
docker compose pull
docker compose up -d
docker compose logs -f api   # o Flyway aplica as migrações na subida; espere "Started RotaDevApplication"
```

Conferência (a imagem da API não tem curl, então usamos o node do container web):

```bash
docker compose exec web node -e "fetch('http://api:8080/actuator/health').then(r => r.text()).then(console.log)"
curl -sI https://<domínio>/ | head -1                                  # HTTP/2 200
curl -s -o /dev/null -w '%{redirect_url}\n' https://<domínio>/api/oauth2/authorization/github
# o redirect_uri dentro da URL tem que ser https://<domínio>/api/login/oauth2/code/github
docker stats --no-stream | grep rota-dev                               # dentro dos limites de memória
```

## 4. Versão nova

1. PR `develop` → `main`, revisado. O merge dispara o `release.yml`, que publica as imagens com o sha.
2. Na VPS: atualize `ROTA_VERSION` no `.env` e rode `docker compose pull && docker compose up -d`.
3. Faça as mesmas conferências do primeiro deploy.

Migrações do Flyway rodam sozinhas na subida da API. Como migração aplicada é imutável e só cresce,
o rollback de código continua compatível com o banco **desde que nenhuma migração apague ou renomeie
coluna em uso**. Mudanças assim se fazem em dois deploys (primeiro o código para de usar, depois a
migração remove).

## 5. Rollback

```bash
cd /opt/rota-dev
# ROTA_VERSION=<sha anterior> no .env
docker compose up -d
```

## 6. Backup e restauração

Instalação do backup diário: comentário no topo de `infra/prod/backup.sh` (rclone + cron às 04:15 UTC).

**Restaurar um backup** (teste isto antes de precisar, num banco separado):

```bash
rclone copy r2:rota-dev-backups/db/rota-dev-<data>.sql.gz /tmp/
docker compose stop api web
docker compose exec -T postgres psql -U rotadev -d postgres -c "DROP DATABASE rotadev" -c "CREATE DATABASE rotadev"
gunzip -c /tmp/rota-dev-<data>.sql.gz | docker compose exec -T postgres psql -U rotadev -d rotadev
docker compose start api web
```

## Segurança

- Só o Caddy publica portas. Banco e API ficam na rede interna; o web fica nas redes `internal` e `edge`.
- A API confia nos cabeçalhos `X-Forwarded-*` (`forward-headers-strategy: framework`) porque só é
  alcançável por dentro. Conferido com o stack local: com `Host` do domínio e `X-Forwarded-Proto: https`
  (o que o Caddy envia), o callback do OAuth sai `https://<domínio>/...`.
- Cookie de sessão `Secure` em produção (`SESSION_COOKIE_SECURE=true` no compose).
- Segredos só no `/opt/rota-dev/.env` (`chmod 600`), nunca no repositório.
