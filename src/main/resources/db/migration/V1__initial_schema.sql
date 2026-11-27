-- Initial schema for AIUP PetClinic
-- Matches docs/entity_model.md.

CREATE SEQUENCE owners_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE pets_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE types_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE visits_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE vets_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE specialties_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE types (
    id   INTEGER      DEFAULT nextval('types_seq') PRIMARY KEY,
    name VARCHAR(80)  NOT NULL UNIQUE
);

CREATE TABLE owners (
    id         INTEGER      DEFAULT nextval('owners_seq') PRIMARY KEY,
    first_name VARCHAR(30)  NOT NULL,
    last_name  VARCHAR(30)  NOT NULL,
    address    VARCHAR(255) NOT NULL,
    city       VARCHAR(80)  NOT NULL,
    telephone  VARCHAR(20)  NOT NULL
);

CREATE INDEX owners_last_name_idx ON owners (last_name);

CREATE TABLE pets (
    id         INTEGER     DEFAULT nextval('pets_seq') PRIMARY KEY,
    name       VARCHAR(30) NOT NULL,
    birth_date DATE,
    type_id    INTEGER     NOT NULL REFERENCES types (id),
    owner_id   INTEGER     NOT NULL REFERENCES owners (id),
    -- H2 has no functional indexes (ADR-012): the lower-cased name is a generated
    -- column the unique constraint below can use. Never written by the application.
    name_lower VARCHAR(30) GENERATED ALWAYS AS (LOWER(name))
);

-- UC-007 / UC-008 BR-001: a pet's name is unique per owner, case-insensitively.
-- A UNIQUE constraint on name would compare the stored spelling, so it is on the
-- lower-cased name instead. It leads with owner_id and therefore also serves the
-- lookups by owner.
ALTER TABLE pets ADD CONSTRAINT pets_owner_name_unique UNIQUE (owner_id, name_lower);

CREATE TABLE visits (
    id          INTEGER      DEFAULT nextval('visits_seq') PRIMARY KEY,
    visit_date  DATE         NOT NULL,
    description VARCHAR(255) NOT NULL,
    pet_id      INTEGER      NOT NULL REFERENCES pets (id)
);

CREATE INDEX visits_pet_id_idx ON visits (pet_id);

CREATE TABLE vets (
    id         INTEGER     DEFAULT nextval('vets_seq') PRIMARY KEY,
    first_name VARCHAR(30) NOT NULL,
    last_name  VARCHAR(30) NOT NULL
);

CREATE TABLE specialties (
    id   INTEGER     DEFAULT nextval('specialties_seq') PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE vet_specialties (
    vet_id       INTEGER NOT NULL REFERENCES vets (id),
    specialty_id INTEGER NOT NULL REFERENCES specialties (id),
    PRIMARY KEY (vet_id, specialty_id)
);
