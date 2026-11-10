package dev.rotadev.profile;

/**
 * Quanto confiamos num dado. Regra geral:
 * <ul>
 *   <li>{@code HIGH}: verificável na fonte (código num repositório público, CI configurado);</li>
 *   <li>{@code MEDIUM}: declarado por uma fonte estruturada (cargo no LinkedIn, item do currículo);</li>
 *   <li>{@code LOW}: inferido ou declarado sem prova (habilidade listada sem projeto que mostre).</li>
 * </ul>
 */
public enum Confidence {
    HIGH,
    MEDIUM,
    LOW
}
