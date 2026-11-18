package org.hermes.shared.domain.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Argon2PasswordEncoderTest {

    private Argon2PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new Argon2PasswordEncoder();
    }

    @Test
    @DisplayName("Should successfully encode password to Argon2id format")
    void shouldEncodePassword() {
        String rawPassword = "SecurePassword123!";
        String encoded = encoder.encode(rawPassword);

        assertNotNull(encoded);
        assertTrue(encoded.startsWith("$argon2id$v=19$m=65536,t=3,p=1$"));
    }

    @Test
    @DisplayName("Should successfully verify correct password match")
    void shouldMatchCorrectPassword() {
        String rawPassword = "SuperSecretPassword#2026";
        String encoded = encoder.encode(rawPassword);

        assertTrue(encoder.matches(rawPassword, encoded));
    }

    @Test
    @DisplayName("Should fail matching incorrect password")
    void shouldFailMatchingIncorrectPassword() {
        String rawPassword = "CorrectPassword123";
        String encoded = encoder.encode(rawPassword);

        assertFalse(encoder.matches("WrongPassword", encoded));
    }

    @Test
    @DisplayName("Should handle null and empty inputs safely")
    void shouldHandleNullAndEmpty() {
        assertThrows(IllegalArgumentException.class, () -> encoder.encode(null));
        assertFalse(encoder.matches(null, "some_hash"));
        assertFalse(encoder.matches("pwd", null));
        assertFalse(encoder.matches("pwd", "invalid_hash_string"));
    }
}
