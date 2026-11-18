# Hermes Payment — Arquitetura do Sistema

Este documento descreve as decisões arquiteturais, padrões de design, convenções e estrutura de código adotadas no **Hermes Payment**, com foco específico na fundação estabelecida pelo módulo **IAM (Identity and Access Management)**.

---

## 1. Visão Geral e Princípios Arquiteturais

O Hermes Payment é construído sobre os seguintes princípios arquiteturais:

- **Hexagonal Architecture (Ports & Adapters)**: Desacoplamento estrito entre o domínio de negócio, regras de aplicação e infraestrutura/tecnologias externas (PostgreSQL, Redis, HTTP REST).
- **Clean Architecture & SOLID**:
  - Camada de Domínio pura, sem dependências de frameworks de persistência ou web.
  - Inversão de Controle e Injeção de Dependências gerenciadas pelo Quarkus Arc CDI.
  - Camada de Aplicação contendo Use Cases orquestradores que recebem Commands e retornam `Result<T>`.
- **Zero ORM (No JPA / Hibernate)**: Persistência 100% tipada e de alto desempenho utilizando **jOOQ** com geração de código a partir das migrações SQL geridas pelo **Flyway** (`DDLDatabase`).
- **Multi-Tenancy Contextual**: Cada requisição autenticada é contextualizada a uma organização (*Tenant*), validada por interceptores de segurança e injetada via `@RequestScoped` CDI context.
- **Concorrência e Integridade**:
  - *Optimistic Locking* nativo em todas as entidades via campo `version` (`BIGINT`).
  - *Pessimistic Locking* seguro via `SELECT FOR UPDATE` (`findByIdForUpdate`) para operações críticas.
  - *Soft Delete* padronizado via `deleted_at IS NULL`.

---

## 2. Estrutura do Monorepo

O repositório é organizado em pastas modulares na raiz:

```text
/
├── back/
│   └── payment/               # Backend Java (Quarkus 3.40, JDK 27, jOOQ, Flyway)
│       └── src/main/java/org/hermes/
│           ├── modules/       # Módulos de negócio da aplicação
│           │   ├── apikey/    # Gestão de chaves de API M2M
│           │   ├── audit/     # Trilha de auditoria operacional
│           │   ├── authentication/ # Login, sessões, JWT, Argon2id
│           │   ├── authorization/  # Roles, permissões e RBAC
│           │   └── identity/  # Usuários, tenants e vínculos UserTenant
│           └── shared/        # Utilitários compartilhados, base entities e infraestrutura
├── front/
│   └── payment/               # Portal Web / Dashboard (Frontend)
├── mobile/
│   └── paymentUI/             # Interface Mobile para POS / terminais
├── observabilidade/           # Configurações de observabilidade
│   ├── prometheus/            # Coleta de métricas e scraping
│   ├── loki/                  # Agregação centralizada de logs
│   ├── tempo/                 # Traces distribuídos OTLP
│   ├── alloy/                 # Grafana Alloy pipeline
│   └── grafana/               # Dashboards e data sources provisionados
├── docs/                      # Documentação técnica e especificações
├── k8s/                       # Manifestos Kubernetes / Helm
├── cicd/
│   └── jenkins/               # Pipelines de CI/CD
├── docker-compose.yml         # Orquestração local completa
├── .env.example               # Template de variáveis de ambiente
├── .gitignore
└── README.md
```

---

## 3. Arquitetura Hexagonal no Módulo IAM

Cada submódulo em `org.hermes.modules.*` respeita a segregação estrita:

```text
modules/<modulo>/
├── domain/                    # Entidades puras, Enums, Regras de Negócio e Value Objects
│   └── model/
├── application/               # Portas de Entrada (Ports In) e Casos de Uso
│   ├── port/
│   │   └── in/                # Interfaces de Use Cases (ex: IRegisterUserUseCase)
│   ├── dto/                   # DTOs de Request, Response e Commands (Java Records)
│   ├── mapper/                # Interfaces MapStruct (CentralMapperConfig)
│   └── service/               # Implementações dos Use Cases (ex: RegisterUserUseCase)
└── adapter/                   # Adaptadores de Entrada e Saída
    ├── in/
    │   └── rest/              # JAX-RS Controllers (ResponseHTTP<T>)
    └── out/
        └── persistence/       # Adaptadores de persistência jOOQ implementando Ports Out
            └── jooq/          # Implementações concretas de repositórios
```

### 3.1 Padrão Command e Result

Todos os Use Cases recebem um objeto `Command` (Java Record) e retornam `Result<T>`:

```java
public interface ICreateTenantUseCase {
    Result<TenantDTO> execute(CreateTenantCommand command);
}
```

O `Result<T>` encapsula sucesso ou erro de domínio tipado sem lançar exceções desnecessárias para fluxo de negócio:
- `Result.success(data)` -> Retorno 200/201.
- `Result.failure(error)` -> Mapeado para 400, 404, 409 ou 422 no controller.

---

## 4. Persistência de Dados com jOOQ

### 4.1 GenericRepository e GenericJooqRepository

Para reduzir boilerplate e garantir tipagem forte, a camada de persistência utiliza:
- `GenericRepository<T, ID>`: Porta genérica com operações `save`, `update`, `deleteById` (soft delete), `findById`, `findByIdForUpdate`, `findAll`, `countAll` e paginação.
- `GenericJooqRepository<T, R extends UpdatableRecordImpl<R>, ID>`: Implementação base abstrata sobre as tabelas e records gerados pelo jOOQ.

### 4.2 Geração de Código com DDLDatabase

O jOOQ Code Generator está integrado ao ciclo de compilação do Maven via `jooq-codegen-maven`. Ele utiliza `org.jooq.meta.extensions.ddl.DDLDatabase` para ler diretamente os scripts de migração Flyway em `src/main/resources/db/migrations/`, sem necessidade de banco de dados ativo durante o build:

```xml
<database>
    <name>org.jooq.meta.extensions.ddl.DDLDatabase</name>
    <properties>
        <property>
            <key>scripts</key>
            <value>src/main/resources/db/migrations</value>
        </property>
    </properties>
</database>
```

---

## 5. Estratégia de Identificadores (Snowflake ID)

Todas as entidades persistidas utilizam identificadores numéricos de 64 bits (`BIGINT`) baseados no algoritmo **Twitter Snowflake**:
- **Bit 63**: Reservado (0).
- **Bits 62-22 (41 bits)**: Timestamp em milissegundos a partir do epoch customizado (01/01/2026). Suporta ~69 anos de operação.
- **Bits 21-12 (10 bits)**: Machine / Datacenter ID (suporta até 1.024 instâncias simultâneas sem colisão).
- **Bits 11-0 (12 bits)**: Sequência monotônica por milissegundo (suporta até 4.096 IDs por ms por nó).

**Vantagens**:
1. Indexação B-Tree ultraeficiente no PostgreSQL (menor fragmentação em relação a UUID v4 aleatório).
2. Ordenação temporal nativa por ID.
3. Compatibilidade nativa com tipos primitivos de alta performance em bancos e redes.
