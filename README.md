# Hermes Payment — B2B Multi-tenant Payment Platform

Plataforma B2B multi-tenant de pagamentos, com isolamento rigoroso por organização, autenticação, autorização granular e arquitetura hexagonal / Clean Architecture.

---

## Estrutura do Repositório

```text
/
├── back/                      # Aplicações backend da plataforma
│   └── payment/               # Backend principal Hermes Payment (Quarkus, jOOQ, Flyway, Redis)
├── front/                     # Aplicações Web Frontend
│   └── payment/               # Portal Web / Dashboard administrativo e de pagamentos
├── mobile/                    # Aplicativos Mobile
│   └── paymentUI/             # Interface mobile para merchants e terminais
├── observabilidade/           # Configurações de métricas, logs e tracing (Prometheus, Loki, Tempo, Alloy, Grafana)
├── docs/                      # Documentação técnica de arquitetura, segurança e contratos de API
├── k8s/                       # Manifestos e Helm charts para Kubernetes
├── cicd/                      # Automação de CI/CD
│   └── jenkins/               # Pipelines e configurações Jenkins
├── docker-compose.yml         # Orquestração da infraestrutura local de desenvolvimento
├── .env.example               # Exemplo de variáveis de ambiente
├── .gitignore                 # Regras de exclusão do Git
└── README.md                  # Este documento
```

---

## Módulo IAM (Identity and Access Management)

O módulo IAM é responsável exclusivamente pela gestão de identidades, organizações (tenants), credenciais, sessões, API Keys e controle de acesso baseado em funções e permissões (RBAC com contexto de tenant).

- **Multi-tenancy**: Isolamento completo por tenant com resolução via header `X-Tenant-ID`.
- **Identificadores**: Snowflake BIGINT de 64 bits para entidades.
- **Autenticação**: Suporte nativo a JWT assinado, sessões ativas e rotação de refresh tokens.
- **Criptografia de Senhas**: Argon2id via Bouncy Castle.
- **Persistência**: jOOQ tipado sobre PostgreSQL e migrations versionadas com Flyway.

Para executar o backend:
```bash
cd back/payment
./mvnw quarkus:dev
```
