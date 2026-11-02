# 0008. Segredos e arquivos em repouso

- **Status:** Aceito
- **Data:** 2026-11-02

## Contexto
Vamos guardar tokens OAuth do GitHub e documentos pessoais (CV, export do LinkedIn). A LGPD exige proteção e exclusão.

## Decisão
- Tokens são cifrados com **AES-GCM** por um `AttributeConverter` JPA. A chave vem da variável `ROTA_ENCRYPTION_KEY`, e o texto cifrado tem um prefixo de versão (`v1:`) para permitir rotação.
- Arquivos enviados ficam atrás da interface `FileStorage`: volume local agora, armazenamento compatível com S3 depois. Nunca são servidos por URL pública: só a API lê, checando o dono.
- Segredos só existem em `.env` (fora do git) ou nos secrets do GitHub Actions.

## Alternativas descartadas
- **pgcrypto no banco:** a chave teria que viajar até o banco nas queries e acabar perto dos dados.
- **Guardar tokens em texto puro:** um dump do banco vazaria acesso às contas do GitHub.

## Consequências
- Rotacionar a chave não exige migração em massa: dados novos usam `v2:`, e os antigos são lidos com `v1` e recifrados aos poucos.
- Perder a chave significa perder os tokens, mas basta o usuário reconectar o GitHub.
