# Use Case: View Owner Details

## Overview

**Use Case ID:** UC-005  
**Use Case Name:** View Owner Details  
**Primary Actor:** Clinic User  
**Goal:** Inspect an owner's contact information together with the list of their pets and each pet's visit history.  
**Trigger:** The Clinic User needs an owner's contact details, pets, or visit history, for example while answering a
customer's call.  
**Status:** Done  
**Requirements:** [FR-011, FR-012, NFR-003](../requirements.md)

## Preconditions

_None — the owner is resolved in step 2, and A1 handles a missing one._

## Main Success Scenario

1. Clinic User opens the Owner Details view for a given owner id (e.g., via a search result, a newly created owner, or a
   bookmark).
2. System loads the owner together with their pets and each pet's visits.
3. System renders the Owner Details view showing the owner's name, address, city, and telephone, and for each pet
   (BR-002) its name, birth date, type, and visits with date and description (BR-001); a pet without visits shows
   "(no visits yet)" in place of the visit list.
4. System offers action links: "Edit Owner", "Add New Pet", and for each pet "Edit Pet" and "Add Visit".
5. Clinic User optionally follows one of the action links to trigger UC-006, UC-007, UC-008, or UC-009.

## Alternative Flows

### A1: Owner Not Found

**Trigger:** No owner exists with the requested identifier (step 2).  
**Flow:**

1. System cannot resolve the owner and shows the application error view.
2. Use case ends.

## Postconditions

### Success Postconditions

- Owner details, pets, and visits are rendered.
- No data is modified.

### Failure Postconditions

- No data is modified.

## Business Rules

### BR-001: Visit Ordering

A pet's visits are listed chronologically, by ascending visit date; visits on the same date appear in the order in
which they were booked ([NFR-003](../requirements.md)).

### BR-002: Pet Ordering

An owner's pets are listed alphabetically by name ([NFR-003](../requirements.md)).
