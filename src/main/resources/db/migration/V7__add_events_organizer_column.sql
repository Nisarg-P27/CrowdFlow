ALTER TABLE events
ADD COLUMN organizer_id UUID NOT NULL,
ADD CONSTRAINT fk_events_organizer
        FOREIGN KEY (organizer_id)
        REFERENCES users(id);