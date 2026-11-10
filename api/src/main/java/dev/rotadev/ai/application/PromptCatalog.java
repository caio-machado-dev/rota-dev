package dev.rotadev.ai.application;

public interface PromptCatalog {

    /** @throws IllegalArgumentException se o prompt ou a versão não existir */
    Prompt get(String name, int version);
}
