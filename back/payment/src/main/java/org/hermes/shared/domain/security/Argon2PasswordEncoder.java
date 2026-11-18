package org.hermes.shared.domain.security;

import jakarta.enterprise.context.ApplicationScoped;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@ApplicationScoped
public class Argon2PasswordEncoder implements PasswordEncoderPort {

    private static final int ITERATIONS = 3;
    private static final int MEMORY = 65536; // 64 MB
    private static final int PARALLELISM = 1;
    private static final int HASH_LENGTH = 32;
    private static final int SALT_LENGTH = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    public String encode(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }

        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);

        byte[] hash = new byte[HASH_LENGTH];

        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(ITERATIONS)
                .withMemoryAsKB(MEMORY)
                .withParallelism(PARALLELISM)
                .withSalt(salt)
                .build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);
        generator.generateBytes(rawPassword.getBytes(StandardCharsets.UTF_8), hash, 0, hash.length);

        Base64.Encoder encoder = Base64.getEncoder().withoutPadding();
        return String.format(
                "$argon2id$v=%d$m=%d,t=%d,p=%d$%s$%s",
                Argon2Parameters.ARGON2_VERSION_13,
                MEMORY,
                ITERATIONS,
                PARALLELISM,
                encoder.encodeToString(salt),
                encoder.encodeToString(hash)
        );
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }

        try {
            String[] parts = encodedPassword.split("\\$");
            // format: ["", "argon2id", "v=19", "m=65536,t=3,p=1", "salt", "hash"]
            if (parts.length != 6 || !"argon2id".equals(parts[1])) {
                return false;
            }

            String[] paramsParts = parts[3].split(",");
            int m = Integer.parseInt(paramsParts[0].substring(2));
            int t = Integer.parseInt(paramsParts[1].substring(2));
            int p = Integer.parseInt(paramsParts[2].substring(2));

            Base64.Decoder decoder = Base64.getDecoder();
            byte[] salt = decoder.decode(parts[4]);
            byte[] expectedHash = decoder.decode(parts[5]);

            byte[] computedHash = new byte[expectedHash.length];

            Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                    .withIterations(t)
                    .withMemoryAsKB(m)
                    .withParallelism(p)
                    .withSalt(salt)
                    .build();

            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(params);
            generator.generateBytes(rawPassword.getBytes(StandardCharsets.UTF_8), computedHash, 0, computedHash.length);

            return MessageDigest.isEqual(expectedHash, computedHash);
        } catch (Exception e) {
            return false;
        }
    }
}
