package dev.rotadev.ai.domain;

/** Tokens de uma chamada, como a API devolve em {@code usage}. */
public record TokenUsage(long inputTokens, long outputTokens, long cacheReadTokens, long cacheWriteTokens) {

    public static final TokenUsage NONE = new TokenUsage(0, 0, 0, 0);
}
