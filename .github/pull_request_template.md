## 📌 Descrição do Pull Request

Implementação completa e arquitetura de referência do módulo **IAM (Identity and Access Management)** da plataforma **Hermes Payment** (B2B Multi-tenant).

> [!IMPORTANT]
> **Fronteira Estrita de Responsabilidade**:
> Este PR contempla **exclusivamente o módulo IAM**. Não foram introduzidas quaisquer entidades ou funcionalidades de pagamentos (PIX, boleto, cartão, PSPs, ledger, settlements, payouts ou conciliação financeira), respeitando a segregação com o futuro módulo de pagamentos.

---

### 🚀 Resumo das Alterações por Camada

#### 1. Reorganização Estrutural e Monorepo (Etapa 0)
- Aplicação Quarkus movida para `back/payment/`.
- Estrutura de diretórios criada conforme padrão Janus (`front/payment`, `mobile/paymentUI`, `docs`, `k8s`, `cicd/jenkins`, `observabilidade`) rastreados com `README.md` (sem arquivos `.gitkeep`).
- Remoção de código boilerplate e templates legados.
- Criação de `.env.example` e atualização do pipeline CI.

#### 2. Configurações e Dependências (Etapa 1)
- Atualização do `pom.xml`: Quarkus 3.40, JDK 27 (release 25), jOOQ 3.19, Bouncy Castle (Argon2id), MapStruct 1.6.3, Flyway, Redis, SmallRye JWT.
- Par de chaves assimétricas **RSA 2048** (`privateKey.pem` e `publicKey.pem`) em `src/main/resources/keys/`.
- Configuração de `application.yaml` com profiles e suporte YAML nativo.

#### 3. Domínio Base e Utilitários de Infraestrutura (Etapa 2)
- `BaseEntity`: Entidade base com Snowflake ID (`BIGINT`), controle de concorrência otimista (`version`), e timestamps de auditoria (`created_at`, `updated_at`, `deleted_at`).
- `Page<T>`: Estrutura padrão para consultas paginadas (`content`, `page`, `size`, `totalElements`, `totalPages`).
- `GenericRepository<T, ID>`: Contrato genérico fortemente tipado.
- `SnowflakeIdGenerator`: Gerador thread-safe de IDs de 64 bits distribuídos e ordenáveis no tempo (41 bits timestamp, 10 bits machineId, 12 bits sequence).
- `Argon2PasswordEncoder`: Hashing de senhas via Bouncy Castle seguindo recomendações OWASP (64MB memória, 3 iterações, 4 threads, 32-byte salt).
- `Result<T>`: Encapsulamento tipado para retornos de Use Cases (elimina lançamento desnecessário de exceções para fluxo de negócio).

#### 4. Esquema de Banco de Dados e jOOQ Codegen (Etapa 3)
- Migração Flyway `V1__init_iam_core_schema.sql` com 15 tabelas relacionais altamente indexadas:
  - `users`, `user_passwords`, `tenants`, `user_tenants`, `roles`, `permissions`, `user_tenant_roles`, `role_permissions`, `user_permissions`, `sessions`, `refresh_tokens`, `login_attempts`, `api_keys`, `api_key_permissions`, `audit_logs`.
- Geração estática de código com `jooq-codegen-maven` utilizando `DDLDatabase` sobre os arquivos SQL (zero dependência de banco ativo durante o build).

#### 5. Repositórios e Persistência jOOQ (Etapa 4)
- `GenericJooqRepository<T, R, ID>`: Implementação base com suporte a CRUD, paginação, soft delete e pessimistic locking (`findByIdForUpdate`).
- 15 entidades de domínio puro e 15 repositórios especializados implementando Ports Out.

#### 6. Infraestrutura de Segurança e Tokens (Etapa 5)
- `ITokenService` / `TokenServiceAdapter`: Emissão de JWT (RS256, 15 min de expiração) com claims de `sub`, `tenant_id`, `roles`, `permissions` e refresh tokens (UUIDv4 armazenados com hash SHA-256).
- `TenantSecurityContext`: Bean `@RequestScoped` contendo `userId`, `tenantId`, roles e permissões da requisição ativa.
- `TenantContextFilter` e anotação `@TenantRequired`: Validação do header `X-Tenant-ID` e da associação ativa do usuário com o tenant.
- Produtores CDI `@UserId` e `@TenantId` para injeção direta em qualquer Use Case ou serviço.
- `ApiKeyAuthenticationFilter`: Autenticação M2M via header `X-API-Key` (formato `hermes_live_<prefix_16>_<secret>`, busca indexada por prefixo e validação por hash SHA-256).
- `@PermissionsAllowed` e interceptor ABAC/RBAC.
- `@Idempotent` com `RedisIdempotencyServiceAdapter` (com fallback in-memory) para controle de requisições duplicadas via header `Idempotency-Key`.

#### 7. Camada de Aplicação Hexagonal (Etapa 6)
- DTOs implementados como Java Records imutáveis.
- Mapeamentos automáticos via MapStruct (`CentralMapperConfig`).
- Ports In e Use Cases implementando o padrão Command/Result para Identity, Authentication, Authorization, ApiKey e Audit.

#### 8. Controladores REST Web In (Etapa 7)
- Todos os endpoints padronizados com o envelope `ResponseHTTP<T>`:
  - `AuthController`: `/v1/auth/register`, `/v1/auth/login`, `/v1/auth/refresh`, `/v1/auth/logout`, `/v1/auth/logout-all`.
  - `TenantController`: `/v1/tenants`, `/v1/tenants/{tenantId}`, `/v1/tenants/{tenantId}/invite`.
  - `UserController`: `/v1/users/me`, `/v1/users/{userId}`.
  - `RoleController`: `/v1/roles`, `/v1/roles/{roleId}/permissions/{permissionId}`, `/v1/roles/assign-user-tenant`, `/v1/permissions`.
  - `ApiKeyController`: `/v1/api-keys`, `/v1/api-keys/{keyId}`.

#### 9. Bootstrap Automático do Sistema (Etapa 8)
- `AdminBootstrapService` (`@Startup`): Carga idempotente no boot de:
  - 15 permissões padrão do IAM.
  - 3 roles do sistema (`master`, `admin`, `user`).
  - Superusuário master configurável via variáveis de ambiente (`admin@hermes.payment`).

#### 10. Observabilidade e Orquestração Local (Etapa 9)
- `docker-compose.yml` completo com:
  - **PostgreSQL 16**, **Redis 7**, **Kafka KRaft** (sem ZooKeeper), **RabbitMQ Management** (com métricas Prometheus).
  - Stack de observabilidade: **Grafana 10**, **Prometheus 2.51**, **Loki 3.0**, **Tempo 2.4**, **Jaeger 1.55**, **Grafana Alloy 1.0**.
  - Exporters: `postgres_exporter`, `redis_exporter`, `cadvisor`, `node_exporter`.
- Configurações declarativas sob a pasta `observabilidade/`.

#### 11. Testes e Validação (Etapa 10)
- 16 testes unitários cobrindo Argon2id, Snowflake, TokenService e Use Cases principais.
- Build Maven concluído com **BUILD SUCCESS** (0 falhas, 0 erros, 0 skipped).

---

### 🧪 Como Testar Localmente

```bash
# 1. Iniciar a infraestrutura
docker compose up -d

# 2. Executar suíte de testes
cd back/payment
./mvnw clean test

# 3. Rodar aplicação
./mvnw quarkus:dev
```

---

### 📋 Checklist de Validação
- [x] Arquitetura Hexagonal com Ports e Adapters respeitada.
- [x] Zero código ou dependência de entidades de pagamentos/financeiro.
- [x] jOOQ com tipagem forte e DDLDatabase (sem ORM/JPA).
- [x] Argon2id para senhas e RSA 2048 para JWT.
- [x] Resolução de contexto multi-tenant via `X-Tenant-ID`.
- [x] Suíte de testes unitários passando 100%.
- [x] Documentação técnica completa em `docs/` e `README.md`.
- [x] Nenhum arquivo `.gitkeep` no repositório.
