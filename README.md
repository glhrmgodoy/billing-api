# Billing API

API REST de cobrança recorrente (assinaturas), inspirada em regras de negócio de plataformas como Netflix/Spotify/SaaS com pagamento mensal. Projeto de portfólio focado em três pilares que costumam faltar em APIs júnior: **autenticação JWT**, **concorrência** (locking otimista) e **idempotência** (webhooks de pagamento).

## Stack

- Java 21
- Spring Boot 4.1.0
- Spring Data JPA + Hibernate
- Spring Security + JWT (jjwt 0.12.6)
- PostgreSQL
- Flyway (migrations versionadas)
- MapStruct (mapeamento DTO ↔ Entity)
- Springdoc OpenAPI (Swagger UI)
- Spring Cloud AWS 4.1.1 (Secrets Manager, Parameter Store, SES, SQS, SNS)
- JUnit 5 + Mockito + AssertJ

## Regras de negócio

- **Ciclo de faturamento**: um job diário (`@Scheduled`) gera a próxima fatura de cada assinatura ativa cujo ciclo venceu, e avança o ciclo automaticamente.
- **Inadimplência**: outro job diário marca faturas vencidas como `OVERDUE` e suspende a assinatura correspondente.
- **Idempotência no pagamento**: o webhook de confirmação de pagamento usa uma `idempotencyKey` única (garantida por constraint no banco) — reenvios do gateway não geram cobrança duplicada.
- **Concorrência na fatura**: a entidade `Invoice` usa locking otimista (`@Version`) para evitar que duas confirmações de pagamento simultâneas corrompam o status uma da outra.
- **Uma assinatura ativa por cliente**: garantido tanto na aplicação quanto por um índice único parcial no Postgres.

## Integração AWS

- **Secrets Manager**: credenciais do banco e segredo do JWT resolvidos via `spring.config.import`, sem segredo em variável de ambiente ou no repositório.
- **Parameter Store**: configuração não sensível que varia por ambiente (nomes das filas, ARN do tópico, remetente de e-mail) vem do path `/billing/<env>/`, também via `spring.config.import`. A aplicação só tem `ssm:GetParametersByPath` no próprio path; o que é política de código (ex.: expiração do JWT) continua versionado no YAML.
- **SES**: e-mails de confirmação e cancelamento de assinatura e de aviso de fatura vencida, disparados por eventos de domínio após o commit (*best-effort*).
- **SNS → SQS (fan-out)**: a confirmação de pagamento publica um evento no tópico `billing-dev-payment-confirmed`, que entrega a duas filas independentes: processamento do pagamento e envio de recibo por e-mail. Cada fila tem DLQ (`maxReceiveCount = 3`).
- **Idempotência por consumidor**: SNS/SQS Standard entregam *at-least-once*; cada consumidor registra `(payment_id, consumer)` em `processed_payment_messages` (padrão *idempotent consumer*), então reentregas não geram recibo em dobro.

## Como rodar

### Pré-requisitos

- Java 21+
- Docker (pra subir o PostgreSQL) — ou uma instância própria do Postgres já rodando
- AWS CLI com o profile `billing-dev` configurado, região `us-east-1`
- Recursos AWS criados:
  - secrets `billing/dev/db-credentials` (JSON com `username`/`password`) e `billing/dev/jwt-secret` (texto puro, mínimo 32 bytes — `openssl rand -base64 32`)
  - identidades verificadas no SES (remetente e destinatários, enquanto a conta estiver em sandbox)
  - tópico SNS `billing-dev-payment-confirmed` e as filas `billing-dev-payment-processing` e `billing-dev-payment-receipt` assinadas nele, cada uma com sua DLQ
  - parâmetros `String` no Parameter Store, sob `/billing/dev/` (o nome é a property com `/` no lugar de `.`):

    | Parâmetro | Exemplo |
    |---|---|
    | `/billing/dev/aws/sns/payment-confirmed-topic-arn` | `arn:aws:sns:us-east-1:<account-id>:billing-dev-payment-confirmed` (ARN, não nome, para a lib não chamar `sns:CreateTopic`) |
    | `/billing/dev/aws/sqs/payment-processing-queue-name` | `billing-dev-payment-processing` |
    | `/billing/dev/aws/sqs/payment-receipt-queue-name` | `billing-dev-payment-receipt` |
    | `/billing/dev/notification/mail/from` | remetente verificado no SES |

### 1. Variáveis de ambiente

Crie um arquivo `.env` na raiz do projeto (esse arquivo **não** deve ser commitado — já está no `.gitignore`). Ele é lido pelo `docker compose` para inicializar o Postgres:

```env
POSTGRES_DB=billing
POSTGRES_USER=postgres
POSTGRES_PASSWORD=sua_senha_local
```

`POSTGRES_USER`/`POSTGRES_PASSWORD` precisam bater com os valores do secret `billing/dev/db-credentials` — a aplicação lê as credenciais do Secrets Manager, não do `.env`.

O `spring-boot:run` **não** lê o `.env`. A única variável que a aplicação exige no shell é `POSTGRES_DB` (nome do banco na URL JDBC, sem valor padrão). O resto da configuração vem do Secrets Manager e do Parameter Store.

### 2. Subir o banco de dados

```bash
docker compose up -d
```

### 3. Rodar a aplicação

```bash
./mvnw spring-boot:run
```

As migrations do Flyway rodam automaticamente na inicialização — não é necessário criar tabelas manualmente.

A API sobe em `http://localhost:8080`. Documentação interativa (Swagger UI) em `http://localhost:8080/swagger-ui.html`.

## Endpoints principais

| Método | Rota | Autenticado? | Descrição |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Não | Cadastro de cliente |
| POST | `/api/v1/auth/login` | Não | Login, retorna JWT |
| GET | `/api/v1/plans` | Não | Lista planos ativos |
| POST | `/api/v1/plans` | Sim | Cria plano |
| GET | `/api/v1/customers/me` | Sim | Perfil do cliente autenticado |
| DELETE | `/api/v1/customers/me` | Sim | Inativa a própria conta |
| POST | `/api/v1/subscriptions` | Sim | Assina um plano |
| GET | `/api/v1/subscriptions/me` | Sim | Assinatura vigente |
| PATCH | `/api/v1/subscriptions/{id}/cancel` | Sim | Cancela (válido até o fim do ciclo pago) |
| GET | `/api/v1/invoices/me` | Sim | Lista faturas do cliente |
| POST | `/api/v1/payments/webhook` | Não* | Confirmação de pagamento (idempotente) |

*Rota do webhook é pública porque quem chama é o gateway de pagamento, não um cliente autenticado.

Autenticação via header: `Authorization: Bearer <token>`.

## Estrutura do projeto

```
src/main/java/com/godoy/billing/
├── config/          # SecurityConfig
├── controller/      # Endpoints REST
├── service/         # Regras de negócio
├── event/           # Eventos de domínio e listeners internos (e-mail)
├── messaging/       # Fronteira com SNS/SQS (publicador e consumidores)
├── security/        # JWT (geração, validação, filtro, UserDetails)
├── repository/      # Interfaces Spring Data JPA
├── domain/
│   ├── entity/      # Plan, Customer, Subscription, Invoice, Payment, ProcessedPaymentMessage
│   └── enums/       # BillingCycle, SubscriptionStatus, InvoiceStatus, PaymentMessageConsumer
├── dto/
│   ├── request/
│   └── response/
├── mapper/          # Interfaces MapStruct
└── exception/       # Exceções customizadas + GlobalExceptionHandler
```

## Status do projeto

- [x] Modelagem de domínio + migrations Flyway
- [x] Autenticação JWT
- [x] Regras de negócio (assinatura, faturamento, pagamento)
- [x] Tratamento global de exceções
- [x] Controllers REST
- [x] Testes unitários
- [x] Integração AWS: Secrets Manager, Parameter Store, SES, SQS, SNS
- [ ] Integração AWS: Parameter Store, CloudWatch