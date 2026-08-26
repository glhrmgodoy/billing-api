CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE plans (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       name VARCHAR(100) NOT NULL,
                       price NUMERIC(10, 2) NOT NULL,
                       billing_cycle VARCHAR(20) NOT NULL,
                       active BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMP NOT NULL DEFAULT now(),
                       updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE customers (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           name VARCHAR(150) NOT NULL,
                           email VARCHAR(150) NOT NULL UNIQUE,
                           password VARCHAR(255) NOT NULL,
                           active BOOLEAN NOT NULL DEFAULT TRUE,
                           created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE subscriptions (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               customer_id UUID NOT NULL REFERENCES customers (id),
                               plan_id UUID NOT NULL REFERENCES plans (id),
                               status VARCHAR(20) NOT NULL,
                               current_cycle_start DATE NOT NULL,
                               current_cycle_end DATE NOT NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE invoices (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          subscription_id UUID NOT NULL REFERENCES subscriptions (id),
                          amount NUMERIC(10, 2) NOT NULL,
                          due_date DATE NOT NULL,
                          status VARCHAR(20) NOT NULL,
                          version BIGINT NOT NULL DEFAULT 0,
                          created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE payments (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          invoice_id UUID NOT NULL REFERENCES invoices (id),
                          amount_paid NUMERIC(10, 2) NOT NULL,
                          paid_at TIMESTAMP NOT NULL,
                          idempotency_key VARCHAR(100) NOT NULL UNIQUE
);

CREATE INDEX idx_subscriptions_customer_id ON subscriptions (customer_id);
CREATE INDEX idx_subscriptions_plan_id ON subscriptions (plan_id);
CREATE INDEX idx_invoices_subscription_id ON invoices (subscription_id);
CREATE INDEX idx_payments_invoice_id ON payments (invoice_id);

CREATE UNIQUE INDEX idx_subscriptions_one_active_per_customer
    ON subscriptions (customer_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_subscriptions_status_cycle_end ON subscriptions (status, current_cycle_end);
CREATE INDEX idx_invoices_status_due_date ON invoices (status, due_date);
