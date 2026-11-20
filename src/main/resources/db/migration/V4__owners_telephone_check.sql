-- OWNER.telephone must be exactly 10 digits (docs/entity_model.md, FR-006).
-- V1 created the column without the format constraint the entity model gives it.

ALTER TABLE owners
    -- UC-003 BR-002: An owner's telephone number is exactly 10 digits.
    ADD CONSTRAINT owners_telephone_ck CHECK (telephone ~ '^[0-9]{10}$');
