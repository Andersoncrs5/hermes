# Hermes Payment — B2B Multi-tenant Payment Platform

Plataforma B2B multi-tenant de pagamentos, projetada com isolamento lógico rigoroso por organização, autenticação robusta, autorização granular baseada em papéis e permissões (RBAC contextual) e arquitetura hexagonal / Clean Architecture.

---

## 1. Visão Geral e Estrutura do Monorepo

O repositório está organizado segundo a arquitetura do ecossistema Hermes:

```text
/
├── back/                      # Aplicações backend da plataforma
│   └── payment/               # Backend principal Hermes Payment (Quarkus 3.40, JDK 27, jOOQ, Flyway)
├── front/                     # Aplicações Web Frontend
│   └── payment/               # Portal Web / Dashboard administrativo e operacional
├── mobile/                    # Aplicativos Mobile
│   └── paymentUI/             # Interface mobile para POS e terminais de pagamento
├── observabilidade/           # Configurações de telemetria (Prometheus, Loki, Tempo, Alloy, Grafana)
│   ├── prometheus/            # Coleta de métricas e alertas
│   ├── loki/                  # Agregação centralizada de logs
│   ├── tempo/                 # Tracing distribuído (OTLP)
│   ├── alloy/                 # Grafana Alloy (coletor e forwarder de logs/traces)
│   └── grafana/               # Dashboards e data sources provisionados
├── docs/                      # Documentação técnica de arquitetura, segurança e contratos
│   ├── ARCHITECTURE.md        # Detalhes de arquitetura hexagonal, jOOQ e concorrência
│   ├── IAM.md                 # Especificação do IAM, contratos com pagamentos e endpoints
│   └── SECURITY.md            # Práticas de segurança, Argon2id, JWT RS256 e auditoria
├── k8s/                       # Manifestos e configurações Kubernetes
├── cicd/                      # Pipelines de integração e entrega contínua
│   └── jenkins/               # Jenkinsfiles e automações
├── docker-compose.yml         # Orquestração local da infraestrutura e observabilidade
├── .env.example               # Template de variáveis de ambiente
├── .gitignore                 # Regras de exclusão do Git
└── README.md                  # Este documento
```

---

## 2. Tecnologias Principais

- **Linguagem / Runtime**: Java 27 (OpenJDK)
- **Framework Backend**: Quarkus 3.40
- **Persistência & Acesso a Dados**:
  - PostgreSQL 16 (JSONB, índices parciais e B-Tree)
  - Flyway para controle de versão e migrações do banco
  - jOOQ 3.19 (tipagem forte, `DDLDatabase` para geração estática sem banco ativo)
  - *Zero ORM* (sem JPA/Hibernate)
- **Criptografia & Segurança**:
  - Argon2id via Bouncy Castle (64MB, 3 iterações, 4 threads)
  - SmallRye JWT assinado com chaves assimétricas RSA 2048 (RS256)
- **Cache & Idempotência**: Redis 7
- **Observabilidade**: Grafana, Prometheus, Loki, Tempo, Alloy e Exporters

---

## 3. Módulo IAM (Identity and Access Management)

O módulo **IAM** foi implementado com estrita observância das fronteiras de responsabilidade:
- **Zero código de pagamentos**: Não foram criadas tabelas financeiras, integrações com PSPs ou fluxos de pagamento (responsabilidade futura da equipe de pagamentos).
- **Multi-tenancy Nativo**: Isolamento completo por organização através do header `X-Tenant-ID`.
- **Snowflake ID**: Identificadores numéricos `BIGINT` de 64 bits ordenáveis no tempo e distribuídos.
- **RBAC Contextual**: Papéis e permissões granulares avaliados no escopo do tenant do usuário.
- **Autenticação M2M**: Chaves de API (`X-API-Key`) indexadas por prefixo e segredo com hash SHA-256.
- **Bootstrap Automático**: Carga idempotente no startup de permissões fundamentais, papéis do sistema (`master`, `admin`, `user`) e usuário administrador inicial.

---

## 4. Como Executar a Infraestrutura Local

### 4.1 Pré-requisitos
- Docker Engine e Docker Compose instalados
- Java 27 (JDK) configurado no `JAVA_HOME`

### 4.2 Subir os Containers de Infraestrutura e Observabilidade
Execute na raiz do repositório:

```bash
# Copie o arquivo de variáveis de ambiente
cp .env.example .env

# Inicie o PostgreSQL, Redis, Kafka, RabbitMQ e a stack de observabilidade
docker compose up -d
```

Serviços disponibilizados:
- **PostgreSQL**: `localhost:5432` (db: `hermes_db`, user: `hermes_user`, pass: `hermes_pass`)
- **Redis**: `localhost:6379`
- **Kafka**: `localhost:9092`
- **RabbitMQ Management**: `http://localhost:15672`
- **Grafana**: `http://localhost:3000` (user: `admin`, pass: `admin`)
- **Prometheus**: `http://localhost:9090`
- **Jaeger UI**: `http://localhost:16686`

---

## 5. Compilação e Execução do Backend

Entre no diretório da aplicação:

```bash
cd back/payment
```

### 5.1 Executar a Suíte de Testes Unitários
```bash
./mvnw clean test
```

### 5.2 Executar em Modo de Desenvolvimento (Live Reload)
```bash
./mvnw quarkus:dev
```

A API estará disponível em `http://localhost:8080`.

---

## 6. Principais Endpoints da API REST

Todos os endpoints retornam o formato uniforme `ResponseHTTP<T>`:

```json
{
  "success": true,
  "code": 200,
  "message": "Operação realizada com sucesso",
  "data": { ... },
  "errors": [],
  "timestamp": "2026-11-17T21:00:00Z"
}
```

### 6.1 Autenticação e Registro
- `POST /v1/auth/register`: Cadastro de novo usuário
  ```bash
  curl -X POST http://localhost:8080/v1/auth/register \
    -H "Content-Type: application/json" \
    -d '{
      "email": "gestor@empresa.com.br",
      "username": "gestor",
      "password": "SenhaSegura123!",
      "fullName": "Gestor da Empresa"
    }'
  ```

- `POST /v1/auth/login`: Autenticação com e-mail e senha
  ```bash
  curl -X POST http://localhost:8080/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{
      "email": "admin@hermes.payment",
      "password": "HermesMasterPassword2026!",
      "tenantId": null
    }'
  ```

### 6.2 Organizações (Tenants)
- `POST /v1/tenants`: Criação de uma organização (requer `Authorization: Bearer <token>`)
  ```bash
  curl -X POST http://localhost:8080/v1/tenants \
    -H "Authorization: Bearer <TOKEN>" \
    -H "Content-Type: application/json" \
    -d '{
      "name": "Acme Pagamentos Ltda",
      "slug": "acme-pagamentos",
      "countryCode": "BR"
    }'
  ```

- `GET /v1/tenants`: Lista organizações vinculadas ao usuário autenticado

### 6.3 Chaves de API M2M
- `POST /v1/api-keys`: Cria uma chave de API para integrações de backend
  ```bash
  curl -X POST http://localhost:8080/v1/api-keys \
    -H "Authorization: Bearer <TOKEN>" \
    -H "X-Tenant-ID: <TENANT_ID>" \
    -H "Content-Type: application/json" \
    -d '{
      "name": "E-Commerce Checkout Key",
      "expiresInDays": 365,
      "permissionIds": []
    }'
  ```

---

## 7. Próximos Passos (Módulo de Pagamentos)

Com o alicerce do IAM concluído e operacional, os desenvolvedores de pagamentos podem iniciar o desenvolvimento de:
1. Modelo de dados de **Merchants** associados aos tenants do IAM (`tenant_id`).
2. Endpoints de **Payment Intent** e processamento financeiro (PIX, Boleto, Cartão de Crédito).
3. Integração com PSPs e Adquirentes.
4. Regras de liquidação, conciliação e split de recebíveis.
