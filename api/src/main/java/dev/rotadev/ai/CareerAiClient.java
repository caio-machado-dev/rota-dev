package dev.rotadev.ai;

/**
 * Porta única para a IA (ADR 0007). Os casos de uso do diagnóstico e do plano chamam esta interface e
 * recebem um JSON já validado contra o schema do prompt; não sabem qual provedor nem qual modelo responde.
 */
public interface CareerAiClient {

    /**
     * @throws AiCallFailedException se a IA recusar, cortar a resposta ou devolver JSON inválido
     *                               mesmo depois das tentativas
     */
    AiResult run(AiRequest request);
}
