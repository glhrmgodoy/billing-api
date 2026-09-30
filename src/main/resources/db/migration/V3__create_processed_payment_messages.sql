CREATE TABLE processed_payment_messages (
    payment_id UUID PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL DEFAULT now()
);