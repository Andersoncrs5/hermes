package org.hermes.modules.apikey.application.service;

import org.hermes.modules.apikey.application.dto.ApiKeyResponseDTO;
import org.hermes.modules.apikey.application.dto.CreateApiKeyDTO;
import org.hermes.modules.apikey.domain.entity.ApiKeyEntity;
import org.hermes.modules.apikey.ports.out.ApiKeyPermissionRepository;
import org.hermes.modules.apikey.ports.out.ApiKeyRepository;
import org.hermes.shared.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateApiKeyUseCaseTest {

    private ApiKeyRepository apiKeyRepository;
    private ApiKeyPermissionRepository apiKeyPermissionRepository;
    private CreateApiKeyUseCase useCase;

    @BeforeEach
    void setUp() {
        apiKeyRepository = mock(ApiKeyRepository.class);
        apiKeyPermissionRepository = mock(ApiKeyPermissionRepository.class);
        useCase = new CreateApiKeyUseCase(apiKeyRepository, apiKeyPermissionRepository);
    }

    @Test
    @DisplayName("Should generate 16-character prefix and raw secret key for TEST environment")
    void shouldGenerateTestApiKey() {
        CreateApiKeyDTO dto = new CreateApiKeyDTO("Test Key", "TEST", List.of(1L, 2L));

        when(apiKeyRepository.insert(any(ApiKeyEntity.class))).thenAnswer(invocation -> {
            ApiKeyEntity entity = invocation.getArgument(0);
            entity.setId(9001L);
            return entity;
        });

        Result<ApiKeyResponseDTO> result = useCase.execute(100L, 200L, dto);

        assertTrue(result.isSuccess());
        ApiKeyResponseDTO response = result.getData();
        assertNotNull(response);
        assertEquals(16, response.keyPrefix().length(), "Prefix must be exactly 16 characters");
        assertTrue(response.keyPrefix().startsWith("hm_test_"));
        assertNotNull(response.secretKey());
        assertTrue(response.secretKey().startsWith(response.keyPrefix() + "_"));
        verify(apiKeyPermissionRepository, times(2)).insert(any());
    }

    @Test
    @DisplayName("Should generate LIVE environment key prefix")
    void shouldGenerateLiveApiKey() {
        CreateApiKeyDTO dto = new CreateApiKeyDTO("Live Key", "LIVE", null);

        when(apiKeyRepository.insert(any(ApiKeyEntity.class))).thenAnswer(invocation -> {
            ApiKeyEntity entity = invocation.getArgument(0);
            entity.setId(9002L);
            return entity;
        });

        Result<ApiKeyResponseDTO> result = useCase.execute(100L, 200L, dto);

        assertTrue(result.isSuccess());
        ApiKeyResponseDTO response = result.getData();
        assertEquals(16, response.keyPrefix().length());
        assertTrue(response.keyPrefix().startsWith("hm_live_"));
        verify(apiKeyPermissionRepository, never()).insert(any());
    }
}
