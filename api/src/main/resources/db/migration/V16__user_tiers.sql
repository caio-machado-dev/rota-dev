-- Nível de assinatura do usuário (Fase 4, pronto para SaaS). Define os limites de uso
-- (rota.limits.* no application.yml). Todo mundo começa no FREE.

ALTER TABLE users ADD COLUMN tier text NOT NULL DEFAULT 'FREE' CHECK (tier IN ('FREE', 'PRO'));
