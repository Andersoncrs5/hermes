package org.hermes.shared.infrastructure.properties;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "hermes.security.jwt")
public interface JwtProperties {

    @WithDefault("https://hermes.io")
    String issuer();

    @WithDefault("hermes-api")
    String audience();

    @WithDefault("900")
    long accessTokenExpirationSeconds();

    @WithDefault("604800")
    long refreshTokenExpirationSeconds();

    @WithDefault("86400")
    long sessionInactivitySeconds();
}
