CREATE TABLE payments (
    id UUID PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),

    amount NUMERIC(10,2) NOT NULL,

    payment_status VARCHAR(50) NOT NULL,

    paid_at TIMESTAMP,

    booking_id UUID NOT NULL UNIQUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    provider VARCHAR(50),

    provider_order_id VARCHAR(100),

    provider_payment_id VARCHAR(100),

    provider_signature VARCHAR(100),

    CONSTRAINT fk_payments_booking
        FOREIGN KEY (booking_id)
        REFERENCES bookings(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_payments_booking_id ON payments(booking_id);

CREATE INDEX idx_payments_transaction_reference
    ON payments(transaction_reference);