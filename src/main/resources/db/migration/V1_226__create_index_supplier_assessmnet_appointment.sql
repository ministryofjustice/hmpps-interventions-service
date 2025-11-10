CREATE INDEX IF NOT EXISTS idx_supplier_assessment_appointment_delivery_session__id
    ON supplier_assessment_appointment(supplier_assessment_id);
CREATE INDEX IF NOT EXISTS idx_supplier_assessment_appointment_appointment__id
    ON supplier_assessment_appointment(appointment_id);