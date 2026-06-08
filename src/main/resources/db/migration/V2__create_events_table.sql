CREATE TABLE events (
    id UUID PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),

    title VARCHAR(255) NOT NULL,

    description TEXT,

    venue VARCHAR(255) NOT NULL,

    event_date TIMESTAMP NOT NULL,

    total_seats INTEGER NOT NULL,

    available_seats INTEGER NOT NULL,

    ticket_price NUMERIC(10,2) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_events_event_date ON events(event_date);