package org.hermes.shared.infrastructure.bootstrap;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.hermes.modules.authorization.domain.entity.PermissionEntity;
import org.hermes.modules.authorization.domain.entity.RoleEntity;
import org.hermes.modules.authorization.domain.entity.RolePermissionEntity;
import org.hermes.modules.authorization.ports.out.PermissionRepository;
import org.hermes.modules.authorization.ports.out.RolePermissionRepository;
import org.hermes.modules.authorization.ports.out.RoleRepository;
import org.hermes.modules.identity.domain.entity.UserEntity;
import org.hermes.modules.identity.domain.entity.UserPasswordEntity;
import org.hermes.modules.identity.ports.out.UserPasswordRepository;
import org.hermes.modules.identity.ports.out.UserRepository;
import org.hermes.shared.domain.security.PasswordEncoderPort;
import org.hermes.shared.infrastructure.properties.BootstrapProperties;
import org.hermes.shared.persistence.tx.ResultTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class AdminBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapService.class);

    private final BootstrapProperties bootstrapProperties;
    private final UserRepository userRepository;
    private final UserPasswordRepository userPasswordRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PasswordEncoderPort passwordEncoder;

    @Inject
    public AdminBootstrapService(
            BootstrapProperties bootstrapProperties,
            UserRepository userRepository,
            UserPasswordRepository userPasswordRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository,
            PasswordEncoderPort passwordEncoder
    ) {
        this.bootstrapProperties = bootstrapProperties;
        this.userRepository = userRepository;
        this.userPasswordRepository = userPasswordRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    void onStart(@Observes StartupEvent ev) {
        if (!bootstrapProperties.enabled()) {
            log.info("IAM Bootstrap is disabled by configuration");
            return;
        }

        try {
            executeBootstrap();
        } catch (Exception e) {
            log.error("Error during IAM bootstrap execution: {}", e.getMessage(), e);
        }
    }

    @ResultTransaction
    public void executeBootstrap() {
        log.info("Starting IAM Admin and Schema Bootstrap...");

        // 1. Seed Core Permissions
        Map<String, PermissionEntity> permissions = seedPermissions();

        // 2. Seed System Roles
        Map<String, RoleEntity> roles = seedRoles();

        // 3. Link Role Permissions
        linkRolePermissions(roles, permissions);

        // 4. Seed Master Admin User
        seedMasterUser();

        log.info("IAM Admin and Schema Bootstrap completed successfully");
    }

    private Map<String, PermissionEntity> seedPermissions() {
        Map<String, String> defaultPerms = Map.ofEntries(
                Map.entry("iam.user.read", "Read user details and profiles"),
                Map.entry("iam.user.create", "Create user accounts"),
                Map.entry("iam.user.update", "Update user details"),
                Map.entry("iam.user.delete", "Deactivate or delete users"),
                Map.entry("iam.tenant.manage", "Manage tenant settings and members"),
                Map.entry("iam.tenant.invite", "Invite users to tenant"),
                Map.entry("iam.role.read", "Read roles"),
                Map.entry("iam.role.manage", "Create and assign roles"),
                Map.entry("iam.permission.read", "Read permissions"),
                Map.entry("iam.apikey.read", "Read API keys"),
                Map.entry("iam.apikey.create", "Create new API keys"),
                Map.entry("iam.apikey.revoke", "Revoke API keys"),
                Map.entry("iam.apikey.manage", "Full management of API keys"),
                Map.entry("iam.profile.read", "Read own user profile"),
                Map.entry("iam.profile.update", "Update own user profile")
        );

        Map<String, PermissionEntity> result = new HashMap<>();

        for (Map.Entry<String, String> entry : defaultPerms.entrySet()) {
            String slug = entry.getKey();
            String desc = entry.getValue();

            Optional<PermissionEntity> existing = permissionRepository.findBySlug(slug);
            if (existing.isPresent()) {
                result.put(slug, existing.get());
            } else {
                PermissionEntity perm = new PermissionEntity();
                perm.setName(slug);
                perm.setSlug(slug);
                perm.setDescription(desc);
                perm.setModule("iam");
                perm = permissionRepository.insert(perm);
                result.put(slug, perm);
                log.debug("Seeded permission: {}", slug);
            }
        }

        return result;
    }

    private Map<String, RoleEntity> seedRoles() {
        Map<String, String> defaultRoles = Map.of(
                "master", "Global Super Admin with complete platform control",
                "admin", "Organization Admin with tenant management access",
                "user", "Standard platform user"
        );

        Map<String, RoleEntity> result = new HashMap<>();

        for (Map.Entry<String, String> entry : defaultRoles.entrySet()) {
            String slug = entry.getKey();
            String desc = entry.getValue();

            Optional<RoleEntity> existing = roleRepository.findBySlug(slug);
            if (existing.isPresent()) {
                result.put(slug, existing.get());
            } else {
                RoleEntity role = new RoleEntity();
                role.setName(slug.substring(0, 1).toUpperCase() + slug.substring(1));
                role.setSlug(slug);
                role.setDescription(desc);
                role.setIsSystem(true);
                role.setIsActive(true);
                role = roleRepository.insert(role);
                result.put(slug, role);
                log.debug("Seeded role: {}", slug);
            }
        }

        return result;
    }

    private void linkRolePermissions(Map<String, RoleEntity> roles, Map<String, PermissionEntity> permissions) {
        RoleEntity masterRole = roles.get("master");
        RoleEntity adminRole = roles.get("admin");
        RoleEntity userRole = roles.get("user");

        // Master gets all permissions
        if (masterRole != null) {
            for (PermissionEntity p : permissions.values()) {
                assignPermissionIfMissing(masterRole.getId(), p.getId());
            }
        }

        // Admin gets all except master-only ops
        if (adminRole != null) {
            for (Map.Entry<String, PermissionEntity> entry : permissions.entrySet()) {
                assignPermissionIfMissing(adminRole.getId(), entry.getValue().getId());
            }
        }

        // User gets profile permissions
        if (userRole != null) {
            PermissionEntity profileRead = permissions.get("iam.profile.read");
            PermissionEntity profileUpdate = permissions.get("iam.profile.update");
            if (profileRead != null) {
                assignPermissionIfMissing(userRole.getId(), profileRead.getId());
            }
            if (profileUpdate != null) {
                assignPermissionIfMissing(userRole.getId(), profileUpdate.getId());
            }
        }
    }

    private void assignPermissionIfMissing(Long roleId, Long permissionId) {
        Optional<RolePermissionEntity> link = rolePermissionRepository.findByRoleAndPermission(roleId, permissionId);
        if (link.isEmpty()) {
            RolePermissionEntity rp = new RolePermissionEntity();
            rp.setRoleId(roleId);
            rp.setPermissionId(permissionId);
            rolePermissionRepository.insert(rp);
        }
    }

    private void seedMasterUser() {
        var master = bootstrapProperties.master();
        String email = master.email().trim().toLowerCase();
        String username = master.username().trim().toLowerCase();

        Optional<UserEntity> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            log.info("Master user '{}' already exists, skipping bootstrap creation", email);
            return;
        }

        existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            log.info("Master user username '{}' already exists, skipping bootstrap creation", username);
            return;
        }

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setUsername(username);
        user.setFullName(master.fullName());
        user.setIsActive(true);
        user.setIsEmailVerified(true);
        user = userRepository.insert(user);

        String hash = passwordEncoder.encode(master.password());
        UserPasswordEntity password = new UserPasswordEntity();
        password.setUserId(user.getId());
        password.setPasswordHash(hash);
        password.setTypeHash("argon2id");
        password.setShouldUpdate(true);
        userPasswordRepository.insert(password);

        log.info("Master user '{}' successfully created with ID: {}", email, user.getId());
    }
}
