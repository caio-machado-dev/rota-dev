package dev.rotadev.sources.infrastructure;

import org.springframework.stereotype.Component;

import dev.rotadev.shared.SecretCipher;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Cifra o campo ao gravar e decifra ao ler (ADR 0008). Na entidade basta
 * {@code @Convert(converter = EncryptedStringConverter.class)}: o resto do código lê e escreve a
 * string em texto puro, e só o banco vê o valor cifrado.
 *
 * <p>É um {@code @Component} para receber o {@link SecretCipher} pelo construtor: o Spring Boot
 * configura o Hibernate para pedir conversores ao contêiner do Spring.
 */
@Component
@Converter
class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final SecretCipher cipher;

    EncryptedStringConverter(SecretCipher cipher) {
        this.cipher = cipher;
    }

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        return plaintext == null ? null : cipher.encrypt(plaintext);
    }

    @Override
    public String convertToEntityAttribute(String stored) {
        return stored == null ? null : cipher.decrypt(stored);
    }
}
