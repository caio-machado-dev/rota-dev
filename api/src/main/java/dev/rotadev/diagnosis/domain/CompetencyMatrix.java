package dev.rotadev.diagnosis.domain;

import java.util.List;

import dev.rotadev.goals.Level;
import dev.rotadev.goals.Track;

/**
 * Quais competências cada trilha cobra e qual nota cada nível exige (escala de 0 a 5).
 *
 * <p>Fica no código, e não na IA, de propósito: a régua tem que ser a mesma para todo mundo e
 * mudar só por decisão explícita (num PR), não pelo humor de um modelo.
 * Escala: 1 = já usou; 2 = usa com ajuda; 3 = usa sozinho em produção; 4 = decide e ensina; 5 = referência.
 */
public final class CompetencyMatrix {

    private CompetencyMatrix() {
    }

    public static List<Axis> axesFor(Track track, Level level) {
        int base = switch (level) {
            case JUNIOR -> 2;
            case PLENO -> 3;
            case SENIOR -> 4;
            case ESPECIALISTA -> 5;
        };
        int supporting = Math.max(1, base - 1);
        return switch (track) {
            case BACKEND -> List.of(
                    new Axis("language", "Linguagem principal", "Domínio da linguagem e do ecossistema", base),
                    new Axis("apis", "APIs e frameworks", "Construir e evoluir APIs com um framework de mercado", base),
                    new Axis("data", "Banco de dados", "Modelagem, SQL, transações e migrações", base),
                    new Axis("testing", "Testes", "Testes de unidade e de integração, testabilidade", base),
                    new Axis("design", "Arquitetura e design", "Modularização, fronteiras, decisões registradas", supporting),
                    new Axis("delivery", "CI/CD e operação", "Pipeline, deploy, observabilidade", supporting));
            case FRONTEND -> List.of(
                    new Axis("language", "TypeScript/JavaScript", "Linguagem e tipagem", base),
                    new Axis("ui", "Framework de UI", "Componentes, estado, roteamento", base),
                    new Axis("css", "Layout e acessibilidade", "CSS, responsividade, a11y", base),
                    new Axis("testing", "Testes", "Testes de componente e ponta a ponta", base),
                    new Axis("performance", "Performance", "Carregamento, renderização, medição", supporting),
                    new Axis("delivery", "CI/CD", "Build, deploy, qualidade automatizada", supporting));
            case FULLSTACK -> List.of(
                    new Axis("backend", "Backend", "APIs, regras de negócio, autenticação", base),
                    new Axis("frontend", "Frontend", "Interface, estado, acessibilidade", base),
                    new Axis("data", "Banco de dados", "Modelagem e SQL", base),
                    new Axis("testing", "Testes", "Testes nas duas pontas", base),
                    new Axis("design", "Arquitetura", "Fronteiras entre front, API e dados", supporting),
                    new Axis("delivery", "CI/CD e operação", "Pipeline e deploy", supporting));
            case DADOS -> List.of(
                    new Axis("language", "Python/SQL", "Linguagens de dados", base),
                    new Axis("modeling", "Modelagem de dados", "Modelos analíticos e transacionais", base),
                    new Axis("pipelines", "Pipelines", "Ingestão, orquestração, qualidade", base),
                    new Axis("analysis", "Análise e estatística", "Métricas, experimentos, visualização", base),
                    new Axis("platform", "Plataforma", "Warehouse, nuvem, custos", supporting),
                    new Axis("delivery", "Engenharia", "Versionamento, testes, CI", supporting));
            case DEVOPS -> List.of(
                    new Axis("linux", "Linux e redes", "Sistema operacional, rede, segurança básica", base),
                    new Axis("containers", "Containers", "Docker, orquestração", base),
                    new Axis("iac", "Infraestrutura como código", "Terraform ou similar", base),
                    new Axis("cicd", "CI/CD", "Pipelines e estratégias de deploy", base),
                    new Axis("observability", "Observabilidade", "Logs, métricas, alertas", supporting),
                    new Axis("cloud", "Nuvem", "Um provedor a fundo", supporting));
            case MOBILE -> List.of(
                    new Axis("language", "Linguagem mobile", "Kotlin, Swift ou Dart/TS", base),
                    new Axis("ui", "Interface nativa", "Componentes, navegação, acessibilidade", base),
                    new Axis("data", "Dados e offline", "Persistência local, sincronização", base),
                    new Axis("testing", "Testes", "Unidade e interface", base),
                    new Axis("platform", "Plataforma", "Ciclo de vida, permissões, publicação", supporting),
                    new Axis("delivery", "CI/CD", "Builds e distribuição", supporting));
        };
    }
}
