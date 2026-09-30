ALTER TABLE processed_payment_messages
ADD COLUMN consumer VARCHAR(40) NOT NULL DEFAULT 'PAYMENT_PROCESSING';

ALTER TABLE processed_payment_messages
ALTER COLUMN consumer DROP DEFAULT;

ALTER TABLE processed_payment_messages
DROP CONSTRAINT processed_payment_messages_pkey,
ADD CONSTRAINT pk_processed_payment_messages PRIMARY KEY (payment_id, consumer);