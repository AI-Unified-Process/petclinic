# Use Case: Add Pet to Owner

## Overview

**Use Case ID:** UC-007   
**Use Case Name:** Add Pet to Owner   
**Primary Actor:** Clinic User   
**Goal:** Register a new pet under an existing owner so that visits and medical information can subsequently be tracked
for it.   
**Status:** Approved   
**Requirements:** [FR-014, FR-015, FR-016, FR-022, FR-023, C-011](../requirements.md)

## Preconditions

- The owner to which the pet will be added exists.
- At least one pet type (e.g., cat, dog, hamster) is configured in the database.

## Main Success Scenario

1. Clinic User clicks "Add New Pet" on the Owner Details view for the selected owner.
2. System displays the pet creation form, pre-populated with the owner's name and a drop-down of available pet types.
3. Clinic User enters the pet's name, birth date, and selects a type, then submits the form.
4. System validates that:
    - name is not blank,
    - birth date is provided and is not in the future,
    - type is selected,
    - no other pet belonging to the same owner already has the same name.
5. System records the new pet under the owner.
6. System returns to the Owner Details view and displays the notification "New Pet has been Added".

## Alternative Flows

### A1: Duplicate Pet Name for Owner

**Trigger:** The submitted name matches a pet already owned by this owner (step 4).
**Flow:**

1. System rejects the `name` field with the error "already exists".
2. System re-renders the pet form with the error message.
3. Clinic User adjusts the name.
4. Use case continues at step 3.

### A2: Birth Date in the Future

**Trigger:** The submitted birth date is later than today (step 4).
**Flow:**

1. System rejects the `birthDate` field with a type-mismatch error.
2. System re-renders the pet form with the error message.
3. Clinic User corrects the date.
4. Use case continues at step 3.

### A3: Missing Required Field

**Trigger:** Name, birth date, or type is missing (step 4).
**Flow:**

1. System rejects the affected field with a "required" error.
2. System re-renders the pet form with the error messages.
3. Clinic User supplies the missing value(s).
4. Use case continues at step 3.

### A4: Pet Creation Cancelled

**Trigger:** Clinic User cancels instead of submitting the form (step 3).
**Flow:**

1. System discards the entered values and returns to the Owner Details view.
2. Use case ends.

## Postconditions

### Success Postconditions

- A new pet record exists, linked to the owner.
- The user is viewing the Owner Details view showing the new pet.

### Failure Postconditions

- No pet is persisted.

## Business Rules

### BR-001: Unique Pet Name per Owner

One owner cannot have two pets with the same name. Names are compared case-insensitively, so *Leo* and *leo* are the same name; different owners may have pets with the same name ([FR-015](../requirements.md)).

### BR-002: Birth Date Not in Future

A pet's date of birth is always recorded, and it lies on or before today ([FR-016](../requirements.md)).

### BR-003: Pet Type Required on Creation

A pet type must be chosen when a pet is first recorded ([FR-014](../requirements.md)).
