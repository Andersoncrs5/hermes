# Guia de Integração e Arquitetura do IAM (Identity and Access Management)

O módulo **IAM** é a espinha dorsal de segurança, identidade e autorização do **Hermes Payment**. Ele gerencia o ciclo de vida completo de organizações (*Tenants*), usuários (*Users*), credenciais criptográficas, sessões ativas, papéis e permissões granulares (*RBAC*), chaves de API para integrações máquina-a-máquina (*M2M*), e trilha de auditoria.

Este documento destina-se especialmente aos desenvolvedores do **módulo de pagamentos**, explicando detalhadamente como o contexto de identidade deve ser consumido e respeitado em novos fluxos financeiros.

---

## 1. Modelo Conceitual e Hierarquia

O Hermes adota um modelo **B2B Multi-Tenant com isolamento lógico**:

```text
                     ┌───────────────────────┐
                     │     Hermes Payment    │
                     │  (Plataforma Central) │
                     └──────────┬────────────┘
                                │
                 ┌──────────────┴──────────────┐
                 ▼                             ▼
        ┌─────────────────┐           ┌─────────────────┐
        │ Tenant A (B2B)  │           │ Tenant B (B2B)  │
        │ (Ex: Empresa X) │           │ (Ex: Empresa Y) │
        └────────┬────────┘           └────────┬────────┘
                 │                             │
    ┌────────────┼────────────┐                │
    ▼            ▼            ▼                ▼
┌────────┐  ┌─────────┐  ┌─────────┐      ┌────────┐
│ User 1 │  │ User 2  │  │ API Key │      │ User 1 │ (Mesmo usuário físico com
│(Admin) │  │(Auditor)│  │ (M2M)   │      │(Viewer)│  papéis diferentes no Tenant B)
└────────┘  └─────────┘  └─────────┘      └────────┘
```

### 1.1 Distinção Obrigatória de Entidades

| Conceito | Descrição | Escopo / Responsabilidade |
|---|---|---|
| **Tenant** | Organização ou empresa contratante da plataforma (CNPJ / Razão Social). | **IAM**: Cadastro, status, país, ciclo de vida. |
| **User** | Pessoa física que acessa a plataforma (e-mail, nome, senha Argon2id). | **IAM**: Identidade global, credenciais, sessões. |
| **UserTenant** | Vínculo de pertinência entre um usuário e uma organização. | **IAM**: Status do vínculo, associação com roles do tenant. |
| **Role** | Agrupamento de permissões em determinado contexto (`master`, `admin`, `user`). | **IAM**: Definição de perfis de acesso. |
| **Permission** | Permissão granular no formato `modulo:acao` (ex: `users:read`, `payments:create`). | **IAM**: Validação de autorização fina. |
| **ApiKey** | Credencial de autenticação M2M vinculada a um tenant e com permissões restritas. | **IAM**: Autenticação de sistemas externos integrados. |
| **Merchant** | Estabelecimento comercial, recebedor ou conta operacional de pagamento. | **Módulo de Pagamentos**: O IAM **NÃO** gerencia dados bancários, adquirência ou liquidação financeira de merchants. Os merchants criados no módulo de pagamentos devem simplesmente referenciar o `tenant_id` fornecido pelo IAM. |

---

## 2. Tabelas do Esquema IAM (PostgreSQL)

O esquema do banco de dados (gerenciado pela migração `V1__init_iam_core_schema.sql`) é composto por 15 tabelas otimizadas:

1. `users`: Identidade central dos usuários (e-mail único lowercase, username, status, tentativas de login).
2. `user_passwords`: Hashes de senhas (Argon2id, salt único, histórico e flag de expiração `should_update`).
3. `tenants`: Organizações cadastradas (slug único, código ISO 3166-1 alpha-2, status).
4. `user_tenants`: Vínculo n:n entre usuários e tenants.
5. `roles`: Papéis de acesso cadastrados (slug único, escopo do sistema ou por tenant).
6. `permissions`: Lista de permissões atômicas do sistema (ex: `users:write`, `tenants:read`).
7. `user_tenant_roles`: Atribuição de papéis aos usuários dentro de um tenant específico.
8. `role_permissions`: Associação n:n entre papéis e permissões.
9. `user_permissions`: Permissões extraordinárias concedidas diretamente ao usuário no tenant.
10. `sessions`: Registro de sessões ativas com IP, user-agent e revogação dinâmica.
11. `refresh_tokens`: Tokens de longa duração para renovação de acesso, armazenados com hash SHA-256.
12. `login_attempts`: Registro de tentativas de login para proteção contra força bruta.
13. `api_keys`: Credenciais M2M vinculadas a tenants com hash SHA-256 da chave secreta e prefixo para indexação rápida.
14. `api_key_permissions`: Associação de permissões concedidas a cada chave de API.
15. `audit_logs`: Trilha imutável de auditoria com autor, tenant, recurso, ação, IP e payload JSONB de alterações.

---

## 3. Fluxo de Autenticação e Emissão de Tokens

### 3.1 Login de Usuário (Password Grant com Argon2id)

1. O cliente envia `POST /v1/auth/login` com `{ "email": "...", "password": "...", "tenantId": 1234567890 }`.
2. O `LoginUserUseCase` valida as credenciais:
   - Verifica se a conta está ativa e não bloqueada.
   - Computa e verifica o hash Argon2id da senha via Bouncy Castle.
   - Registra tentativa de login em `login_attempts`.
   - Se o `tenantId` foi informado, valida se o usuário possui vínculo ativo (`UserTenant`) com a organização.
3. Carrega os papéis e permissões ativas do usuário naquele tenant.
4. Gera um par de tokens:
   - **Access Token (JWT)**: Assinado com chave assimétrica **RSA 2048** (algoritmo RS256). Validade de **15 minutos**.
   - **Refresh Token**: String randômica UUID v4 codificada, armazenada no banco com hash SHA-256. Validade de **7 dias**.
5. Retorna `SessionDTO` contendo o token JWT, refresh token, tempo de expiração e dados do usuário.

### 3.2 Estrutura do Access Token (JWT Claims)

```json
{
  "iss": "hermes-iam",
  "sub": "1000000000000001",
  "tenant_id": 2000000000000001,
  "upn": "admin@hermes.payment",
  "email": "admin@hermes.payment",
  "roles": ["admin"],
  "permissions": [
    "users:read",
    "users:write",
    "tenants:read",
    "payments:create",
    "payments:read"
  ],
  "exp": 1700000900,
  "iat": 1700000000
}
```

---

## 4. Como o Módulo de Pagamentos Deve Integrar com o IAM

O desenvolvedor do módulo de pagamentos **não precisa** reescrever autenticação nem checar banco de dados para validar organizações. O Quarkus e o IAM disponibilizam tudo via injeção CDI e anotações declarativas.

### 4.1 Resolução de Tenant e Autenticação

Para qualquer endpoint que exija contexto de organização (ex: criação de pagamento, conciliação, consulta de saldo):

1. **Header HTTP Obrigatório**: O cliente deve enviar o cabeçalho:
   ```http
   X-Tenant-ID: 2000000000000001
   ```
2. **Anotação `@TenantRequired`**:
   O `TenantContextFilter` intercepta a requisição, valida se o `X-Tenant-ID` é válido, confere se o usuário autenticado no JWT pertence ao tenant solicitado e se o vínculo está ativo (`is_active = true`).
3. **Injeção do Contexto no Use Case / Controller**:
   Basta injetar `TenantSecurityContext` ou os produtores de contexto `@TenantId` e `@UserId`:

```java
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.hermes.shared.domain.annotation.TenantId;
import org.hermes.shared.domain.annotation.UserId;
import org.hermes.modules.authentication.adapter.out.token.TenantSecurityContext;

@RequestScoped
public class CreatePaymentUseCase {

    @Inject
    @TenantId
    Long currentTenantId;

    @Inject
    @UserId
    Long currentUserId;

    @Inject
    TenantSecurityContext securityContext;

    public void execute(CreatePaymentCommand cmd) {
        // currentTenantId já está validado e garantido pelo filtro do IAM
        // Garante que o merchant / pagamento seja criado EXCLUSIVAMENTE para o currentTenantId
    }
}
```

### 4.2 Autorização Declarativa de Permissões (`@PermissionsAllowed`)

Para proteger métodos de Use Case ou Controllers com permissões granulares:

```java
import org.hermes.shared.domain.annotation.PermissionsAllowed;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/v1/payments")
public class PaymentController {

    @POST
    @PermissionsAllowed("payments:create")
    public ResponseHTTP<PaymentDTO> createPayment(CreatePaymentRequestDTO request) {
        // Só executa se o usuário ou API Key possuir a permissão "payments:create"
    }
}
```

Se o usuário não tiver a permissão informada no tenant atual, o `PermissionsAllowedInterceptor` interrompe a execução e retorna `HTTP 403 Forbidden` padronizado.

### 4.3 Autenticação de Integrações Externas (API Keys)

Aplicações de terceiros e merchants integrando seus e-commerces com a API do Hermes autenticam-se via chave de API:
- **Header**:
  ```http
  X-API-Key: hermes_live_a1b2c3d4e5f6g7h8_secret...
  ```
- O `ApiKeyAuthenticationFilter` valida a chave instantaneamente:
  1. Extrai o prefixo (16 caracteres) para busca rápida indexada.
  2. Compara o hash SHA-256 do segredo.
  3. Valida se a chave não expirou (`expired_at`) e está ativa.
  4. Carrega o `tenant_id` associado e as permissões atribuídas à chave.
  5. Popula o `TenantSecurityContext` exatamente como no fluxo de usuário.

---

## 5. Garantia de Idempotência (`@Idempotent`)

Operações críticas de pagamento **devem** ser idempotentes para evitar cobranças duplicadas em caso de retry de rede:

1. **Header do Cliente**:
   ```http
   Idempotency-Key: c9b29d47-6b60-498c-8f32-e01c5d0efbc2
   ```
2. O `IdempotencyFilter` valida se o hash da chave já foi processado no Redis. Se a requisição já tiver sido completada anteriormente com sucesso, o payload original em cache é retornado imediatamente com status `HTTP 200/201`, sem reexecutar a lógica de negócio.

---

## 6. Resumo dos Endpoints REST do IAM

Todos os endpoints retornam a estrutura padronizada `ResponseHTTP<T>`.

| Método | Endpoint | Permissão / Requisito | Descrição |
|---|---|---|---|
| `POST` | `/v1/auth/register` | Pública | Cadastro inicial de usuário. |
| `POST` | `/v1/auth/login` | Pública | Login com e-mail e senha, retornando JWT e Refresh Token. |
| `POST` | `/v1/auth/refresh` | Pública | Renovação do JWT a partir do refresh token. |
| `POST` | `/v1/auth/logout` | Autenticado | Invalida a sessão ativa. |
| `POST` | `/v1/auth/logout-all` | Autenticado | Invalida todas as sessões e tokens do usuário. |
| `POST` | `/v1/tenants` | Autenticado | Cria uma nova organização (Tenant) e associa o criador como Admin. |
| `GET` | `/v1/tenants` | Autenticado | Lista todos os tenants dos quais o usuário autenticado participa. |
| `GET` | `/v1/tenants/{tenantId}` | `@TenantRequired` | Detalhes do tenant ativo. |
| `POST` | `/v1/tenants/{tenantId}/invite` | `@TenantRequired`, `users:write` | Convida/vincula um usuário ao tenant. |
| `GET` | `/v1/users/me` | Autenticado | Dados cadastrais do usuário logado. |
| `PUT` | `/v1/users/me` | Autenticado | Atualiza nome completo e preferências do usuário. |
| `GET` | `/v1/users/{userId}` | `users:read` | Consulta dados de usuário por ID. |
| `POST` | `/v1/roles` | `roles:write` | Cria um novo papel customizado. |
| `GET` | `/v1/roles` | `roles:read` | Lista os papéis do sistema ou tenant. |
| `POST` | `/v1/roles/{roleId}/permissions/{permissionId}` | `roles:write` | Concede permissão a um papel. |
| `POST` | `/v1/roles/assign-user-tenant` | `roles:write` | Associa um papel a um usuário em um tenant. |
| `GET` | `/v1/permissions` | `permissions:read` | Lista todas as permissões do sistema. |
| `POST` | `/v1/api-keys` | `@TenantRequired`, `api_keys:write` | Gera uma nova API Key (o segredo só é exibido uma vez). |
| `GET` | `/v1/api-keys` | `@TenantRequired`, `api_keys:read` | Lista chaves de API do tenant ativo. |
| `DELETE`| `/v1/api-keys/{keyId}` | `@TenantRequired`, `api_keys:write` | Revoga imediatamente uma chave de API. |
