ALTER TABLE appointment
    ADD CONSTRAINT fk_superseded_by_appoinment_id FOREIGN KEY (superseded_by_appointment_id) REFERENCES appointment (id) ON DELETE CASCADE;

ALTER TABLE appointment
    ADD CONSTRAINT superseded_has_superseded_by_appoinment_id CHECK (NOT (superseded_by_appointment_id is null AND superseded = true));