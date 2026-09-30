INSERT INTO plans (id, name, price, billing_cycle, active, created_at, updated_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'Básico', 19.90, 'MONTHLY', true, now(), now()),
    ('22222222-2222-2222-2222-222222222222', 'Pro', 49.90, 'MONTHLY', true, now(), now()),
    ('33333333-3333-3333-3333-333333333333', 'Enterprise Anual', 499.90, 'YEARLY', true, now(), now()),
    ('44444444-4444-4444-4444-444444444444', 'Plano Descontinuado', 9.90, 'MONTHLY', false, now(), now());
