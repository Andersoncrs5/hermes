package org.hermes.shared.infrastructure.properties;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.List;

@ConfigMapping(prefix = "hermes.bootstrap")
public interface BootstrapProperties {

    @WithDefault("true")
    boolean enabled();

    Master master();

    interface Master {
        @WithDefault("admin")
        String username();

        @WithDefault("admin@hermes.io")
        String email();

        @WithDefault("System Master Admin")
        String fullName();

        @WithDefault("AdminMasterSecret123!")
        String password();

        @WithDefault("master,admin,user")
        List<String> roles();
    }
}
