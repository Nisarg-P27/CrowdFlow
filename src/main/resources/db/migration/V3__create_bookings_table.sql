CREATE TABLE bookings (
    id UUID PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),

    seat_count INTEGER NOT NULL,

    total_amount NUMERIC(10,2) NOT NULL,

    booking_status VARCHAR(50) NOT NULL,

    booked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    user_id UUID NOT NULL,

    event_id UUID NOT NULL,

    payment_id UUID NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    expires_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bookings_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_bookings_event
        FOREIGN KEY (event_id)
        REFERENCES events(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_bookings_user_id ON bookings(user_id);

CREATE INDEX idx_bookings_event_id ON bookings(event_id);

CREATE INDEX idx_bookings_status ON bookings(booking_status);