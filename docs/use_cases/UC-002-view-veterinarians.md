# Use Case: View Veterinarians

## Overview

**Use Case ID:** UC-002   
**Use Case Name:** View Veterinarians   
**Primary Actor:** Visitor   
**Goal:** Browse the list of veterinarians employed at the clinic along with their specialties.   
**Status:** Done   
**Requirements:** [FR-003, NFR-001, NFR-002, NFR-003, C-010, C-011](../requirements.md)

## Preconditions

- At least one veterinarian is recorded in the system (see A1 otherwise).

## Main Success Scenario

1. Visitor clicks the "Veterinarians" link in the navigation bar.
2. System retrieves the first set of veterinarians.
3. System renders the veterinarians grid showing, for each vet, the first name, last name, and a comma-separated list of
   specialties (or "none" if no specialties are held).
4. As the Visitor scrolls toward the end of the grid, the system fetches and appends the next chunk of veterinarians
   until all entries have been loaded.

## Alternative Flows

### A1: No Veterinarians Registered

**Trigger:** The clinic has no veterinarians recorded (step 2).
**Flow:**

1. System renders the veterinarians grid with no rows.
2. Use case ends.

## Postconditions

### Success Postconditions

- The veterinarians grid is rendered with all loaded entries.
- No data is modified.

### Failure Postconditions

- On data-access errors, the application error view is shown and no vet list is displayed.

## Business Rules

### BR-001: Lazy Loading

The veterinarians grid fetches its rows from the backend as the user scrolls, with no page controls and no fixed page size, and the rows fetched per scroll step do not grow with the number of vets ([NFR-001, NFR-002](../requirements.md)).

### BR-002: Specialty Ordering

A vet's specialties are listed alphabetically by name ([NFR-003](../requirements.md)).

### BR-003: Anonymous Access

The Veterinarians view is reachable without authentication ([C-010](../requirements.md)).
