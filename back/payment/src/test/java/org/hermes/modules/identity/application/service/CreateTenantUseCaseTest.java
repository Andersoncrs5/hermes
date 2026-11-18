package org.hermes.modules.identity.application.service;

import org.hermes.modules.authorization.domain.entity.RoleEntity;
import org.hermes.modules.authorization.ports.out.RoleRepository;
import org.hermes.modules.authorization.ports.out.UserTenantRoleRepository;
import org.hermes.modules.identity.application.dto.CreateTenantDTO;
import org.hermes.modules.identity.application.dto.TenantDTO;
import org.hermes.modules.identity.application.mapper.TenantMapper;
import org.hermes.modules.identity.domain.entity.TenantEntity;
import org.hermes.modules.identity.ports.out.TenantRepository;
import org.hermes.modules.identity.ports.out.UserTenantRepository;
import org.hermes.shared.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateTenantUseCaseTest {

    private TenantRepository tenantRepository;
    private UserTenantRepository userTenantRepository;
    private RoleRepository roleRepository;
    private UserTenantRoleRepository userTenantRoleRepository;
    private TenantMapper tenantMapper;
    private CreateTenantUseCase useCase;

    @BeforeEach
    void setUp() {
        tenantRepository = mock(TenantRepository.class);
        userTenantRepository = mock(UserTenantRepository.class);
        roleRepository = mock(RoleRepository.class);
        userTenantRoleRepository = mock(UserTenantRoleRepository.class);
        tenantMapper = mock(TenantMapper.class);

        useCase = new CreateTenantUseCase(
                tenantRepository,
                userTenantRepository,
                roleRepository,
                userTenantRoleRepository,
                tenantMapper
        );
    }

    @Test
    @DisplayName("Should successfully create tenant and assign creator as admin")
    void shouldCreateTenantSuccessfully() {
        CreateTenantDTO dto = new CreateTenantDTO("Acme Corp", "acme-corp", "BR");
        Long creatorUserId = 1001L;

        when(tenantRepository.findBySlug("acme-corp")).thenReturn(Optional.empty());

        TenantEntity entity = new TenantEntity();
        entity.setId(5001L);
        entity.setName("Acme Corp");
        entity.setSlug("acme-corp");

        when(tenantMapper.toEntity(dto)).thenReturn(entity);
        when(tenantRepository.insert(any(TenantEntity.class))).thenReturn(entity);
        when(userTenantRepository.insert(any())).thenAnswer(invocation -> {
            org.hermes.modules.identity.domain.entity.UserTenantEntity ut = invocation.getArgument(0);
            ut.setId(8001L);
            return ut;
        });

        RoleEntity adminRole = new RoleEntity();
        adminRole.setId(2001L);
        adminRole.setSlug("admin");
        when(roleRepository.findBySlug("admin")).thenReturn(Optional.of(adminRole));

        TenantDTO expectedDto = new TenantDTO(5001L, "Acme Corp", "acme-corp", "ACTIVE", "BR", null, null);
        when(tenantMapper.toDTO(entity)).thenReturn(expectedDto);

        Result<TenantDTO> result = useCase.execute(dto, creatorUserId);

        assertTrue(result.isSuccess());
        assertEquals("acme-corp", result.getData().slug());
        verify(userTenantRepository, times(1)).insert(any());
        verify(userTenantRoleRepository, times(1)).insert(any());
    }

    @Test
    @DisplayName("Should fail with conflict when slug is already taken")
    void shouldFailWhenSlugTaken() {
        CreateTenantDTO dto = new CreateTenantDTO("Acme Duplicate", "acme-corp", "BR");
        when(tenantRepository.findBySlug("acme-corp")).thenReturn(Optional.of(new TenantEntity()));

        Result<TenantDTO> result = useCase.execute(dto, 1001L);

        assertTrue(result.isFailure());
        assertEquals(409, result.getStatus());
        verify(tenantRepository, never()).insert(any());
    }
}
