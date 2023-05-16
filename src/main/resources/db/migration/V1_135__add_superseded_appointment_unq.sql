ALTER TABLE appointment
    ADD CONSTRAINT superseded_by_appointment_id_unq UNIQUE(superseded_by_appointment_id);