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
- JUnit 5 + Mockito

## Regras de negócio

- **Ciclo de faturamento**: um job diário (`@Scheduled`) gera a próxima fatura de cada assinatura ativa cujo ciclo venceu, e avança o ciclo automaticamente.
- **Inadimplência**: outro job diário marca faturas vencidas como `OVERDUE` e suspende a assinatura correspondente.
- **Idempotência no pagamento**: o webhook de confirmação de pagamento usa uma `idempotencyKey` única (garantida por constraint no banco) — reenvios do gateway não geram cobrança duplicada.
- **Concorrência na fatura**: a entidade `Invoice` usa locking otimista (`@Version`) para evitar que duas confirmações de pagamento simultâneas corrompam o status uma da outra.
- **Uma assinatura ativa por cliente**: garantido tanto na aplicação quanto por um índice único parcial no Postgres.

## Como rodar

### Pré-requisitos

- Java 21+
- Docker (pra subir o PostgreSQL) — ou uma instância própria do Postgres já rodando

### 1. Variáveis de ambiente

Crie um arquivo `.env` na raiz do projeto (esse arquivo **não** deve ser commitado — já está no `.gitignore`):

```env
POSTGRES_DB=billing
POSTGRES_USER=postgres
POSTGRES_PASSWORD=sua_senha_local

JWT_SECRET=gere_um_valor_com_openssl_rand_-base64_32
```

Pra gerar um `JWT_SECRET` válido (mínimo 32 caracteres, exigido pelo HMAC-SHA256):

```bash
openssl rand -base64 32
```

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
├── security/        # JWT (geração, validação, filtro, UserDetails)
├── repository/      # Interfaces Spring Data JPA
├── domain/
│   ├── entity/      # Plan, Customer, Subscription, Invoice, Payment
│   └── enums/       # BillingCycle, SubscriptionStatus, InvoiceStatus
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
- [ ] Testes unitários