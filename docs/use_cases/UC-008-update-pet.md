# Use Case: Update Pet

## Overview

**Use Case ID:** UC-008   
**Use Case Name:** Update Pet   
**Primary Actor:** Clinic User   
**Goal:** Modify the details of an existing pet (name, birth date, or type).   
**Status:** Approved   
**Requirements:** [FR-017, FR-015, FR-016, FR-022, FR-023](../requirements.md)

## Preconditions

- The owner and the pet exist.
- The Clinic User has navigated to the Owner Details view for the pet's owner.

## Main Success Scenario

1. Clinic User clicks "Edit Pet" next to the pet on the Owner Details view.
2. System displays the pet edit form pre-filled with the pet's current name, birth date, and type.
3. Clinic User amends one or more fields and submits the form.
4. System validates that:
    - name is not blank,
    - birth date is provided and is not in the future,
    - no other pet belonging to the same owner (with a different id) already has the same name.
5. System stores the updated pet details (name, birth date, type).
6. System returns to the Owner Details view and displays the notification "Pet details has been edited".

## Alternative Flows

### A1: Duplicate Pet Name

**Trigger:** The submitted name matches a different pet already owned by this owner (step 4).
**Flow:**

1. System rejects the `name` field with the error "already exists".
2. System re-renders the pet edit form with the error.
3. Clinic User adjusts the name.
4. Use case continues at step 3.

### A2: Birth Date in the Future

**Trigger:** The submitted birth date is later than today (step 4).
**Flow:**

1. System rejects the `birthDate` field with a type-mismatch error.
2. System re-renders the pet edit form with the error.
3. Clinic User corrects the date.
4. Use case continues at step 3.

### A3: Missing Required Field

**Trigger:** Name or birth date is missing (step 4).
**Flow:**

1. System rejects the affected field with a "required" error.
2. System re-renders the pet edit form with the error messages.
3. Clinic User supplies the missing value(s).
4. Use case continues at step 3.

### A4: Pet Edit Cancelled

**Trigger:** Clinic User cancels instead of submitting the form (step 3).
**Flow:**

1. System discards the amended values and returns to the Owner Details view.
2. Use case ends.

## Postconditions

### Success Postconditions

- The pet record reflects the updated name, birth date, and type.
- The user is viewing the Owner Details view with the updated pet.

### Failure Postconditions

- The pet record is unchanged.

## Business Rules

### BR-001: Unique Pet Name per Owner

UC-007 BR-001 applies on update as well; the pet being edited is not compared against itself ([FR-015](../requirements.md)).

### BR-002: Birth Date Not in Future

UC-007 BR-002 applies on update as well ([FR-016](../requirements.md)).

### BR-003: Pet Type on Update

UC-007 BR-003 is enforced at creation only: on update the type may be left unchanged ([FR-017](../requirements.md)).
