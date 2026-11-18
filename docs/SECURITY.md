# Hermes Payment — Políticas e Padrões de Segurança

A segurança é o pilar fundamental do **Hermes Payment**. Como uma plataforma B2B de pagamentos, toda a infraestrutura e o código do módulo IAM foram desenhados em conformidade com as melhores práticas de proteção de credenciais, isolamento de dados e auditoria estrita.

---

## 1. Criptografia e Armazenamento de Senhas (Argon2id)

O Hermes utiliza o algoritmo **Argon2id** (vencedor da *Password Hashing Competition* e padrão recomendado pela OWASP) fornecido pela biblioteca **Bouncy Castle**:

- **Variante**: Argon2id (oferece proteção híbrida contra ataques de canal lateral / side-channel e ataques baseados em aceleração por GPU/ASIC).
- **Parâmetros Operacionais**:
  - `iterations` (Tempo): 3
  - `memoryLimit` (Memória): 64 MB (65.536 KB)
  - `parallelism` (Threads): 4
  - `saltLength`: 32 bytes (gerados a partir de `SecureRandom` criptográfico)
  - `hashLength`: 32 bytes
- **Formato Armazenado**:
  `$argon2id$v=19$m=65536,t=3,p=4$<salt_base64>$<hash_base64>`

O campo `should_update` na tabela `user_passwords` permite marcar senhas geradas administrativamente ou senhas legadas para troca obrigatória no próximo login.

---

## 2. Emissão e Validação de Tokens JWT (RS256)

A autenticação de usuários é baseada em tokens JWT assinados digitalmente com criptografia assimétrica **RSA 2048**:

- **Algoritmo**: `RS256` (SHA-256 com RSA PKCS#1 v1.5).
- **Assinatura**: O serviço assina os tokens utilizando a chave privada RSA (`privateKey.pem`).
- **Validação**: Microsserviços e gateways validam o token utilizando exclusivamente a chave pública (`publicKey.pem`). Nenhuma chave simétrica compartilhada (*shared secret*) é necessária entre serviços, garantindo isolamento total.
- **Validade do Access Token**: 15 minutos (tempo de vida curto para minimizar a janela de exposição em caso de vazamento no cliente).
- **Refresh Tokens**:
  - Gerados como strings criptográficas aleatórias de 128 bits.
  - O banco de dados armazena apenas o hash **SHA-256** do refresh token, garantindo que mesmo um dump do banco não permita o sequestro de sessões.
  - Validade de 7 dias com rotação a cada renovação.

---

## 3. Segurança de Chaves de API (M2M)

Para integrações externas via API Keys:
- **Formato da Chave**: `hermes_live_<prefix_16_chars>_<secret_key>`
- **Indexação**: O banco armazena o `prefix` em texto claro para permitir busca $O(1)$ por índice B-Tree sem varrer a tabela.
- **Segredo**: O segredo da chave **nunca** é persistido em texto claro. Ele é armazenado como um hash **SHA-256** (`secret_hash`).
- O valor completo da chave só é retornado uma única vez no momento da criação (`POST /v1/api-keys`).
- As chaves possuem data de expiração configurável e suporte a revogação instantânea via soft delete / status inativo.

---

## 4. Proteção Contra Ataques de Força Bruta

O módulo de autenticação implementa contenção contra ataques de força bruta:
- A tabela `login_attempts` registra cada tentativa com:
  - `ip_address`
  - `user_agent`
  - `is_successful`
  - `failure_reason`
- A tabela `users` mantém o contador acumulado `failed_login_attempts`.
- Ao atingir o limite configurado (padrão: 5 tentativas consecutivas), a conta é automaticamente bloqueada (`blocked_at = NOW()`), impedindo novas autenticações até intervenção de um administrador ou tempo de expiração do bloqueio.

---

## 5. Trilha Imutável de Auditoria (Audit Logs)

Todas as ações sensíveis realizadas na plataforma são auditadas pela tabela `audit_logs`:
- **Campos Registrados**:
  - `tenant_id`: Organização onde o evento ocorreu.
  - `actor_id`: ID Snowflake do usuário ou chave de API responsável.
  - `actor_type`: `USER`, `API_KEY` ou `SYSTEM`.
  - `action`: Ação executada (ex: `USER_INVITED`, `ROLE_ASSIGNED`, `API_KEY_REVOKED`).
  - `resource_type` e `resource_id`: Entidade afetada.
  - `ip_address` e `user_agent`.
  - `changes`: Payload JSONB com o diff dos atributos modificados.

As regras de negócio proíbem alteração ou exclusão de registros da tabela `audit_logs`.

---

## 6. Prevenção de Vazamento de Segredos

- **DTOs Segregados**: Nenhuma entidade de banco é exposta diretamente nos controladores REST. DTOs de resposta omitem explicitamente campos como `password_hash`, `secret_hash`, `type_hash` ou tokens internos.
- **Mapeamentos MapStruct**: A anotação `@Mapping(target = "password", ignore = true)` e a separação estrita de classes DTO impedem a serialização acidental de dados sensíveis.
- **Headers Seguros**: O framework Quarkus adiciona headers de segurança HTTP contra *clickjacking*, *MIME sniffing* e *XSS*.
