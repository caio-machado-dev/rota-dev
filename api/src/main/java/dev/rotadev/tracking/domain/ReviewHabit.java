package dev.rotadev.tracking.domain;

import java.time.LocalDate;

/**
 * Protocolo anti vibe coding (glossário): no dia, ao revisar código gerado por IA, a pessoa
 * <ol>
 *   <li>leu o diff inteiro ({@code readDiff});</li>
 *   <li>conseguiu explicar o que ele faz ({@code explained});</li>
 *   <li>escreveu ou rodou um teste que prova o comportamento ({@code tested});</li>
 *   <li>registrou uma nota do que aprendeu ({@code note}).</li>
 * </ol>
 * O dia só conta para o hábito quando os quatro passos estão feitos.
 */
public record ReviewHabit(LocalDate day, boolean readDiff, boolean explained, boolean tested, String note) {

    public boolean complete() {
        return readDiff && explained && tested && note != null && !note.isBlank();
    }

    /** Quantos dos quatro passos foram feitos, para a tela mostrar o progresso. */
    public int stepsDone() {
        return (readDiff ? 1 : 0) + (explained ? 1 : 0) + (tested ? 1 : 0) + (note != null && !note.isBlank() ? 1 : 0);
    }
}
