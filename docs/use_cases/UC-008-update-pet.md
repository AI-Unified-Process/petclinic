# Use Case: Update Pet

## Overview

**Use Case ID:** UC-008   
**Use Case Name:** Update Pet   
**Primary Actor:** Clinic User   
**Goal:** Modify the details of an existing pet (name, birth date, or type).   
**Trigger:** The Clinic User learns that a pet's recorded name, birth date, or type is wrong, for example when the owner
points out a mistake made at registration.   
**Status:** Done   
**Requirements:** [FR-017, FR-015, FR-016, FR-022, FR-023, C-011](../requirements.md)

## Preconditions

- At least one pet type is configured in the database ([C-011](../requirements.md)).

## Main Success Scenario

1. Clinic User clicks "Edit Pet" next to the pet on the Owner Details view.
2. System displays the pet edit form pre-filled with the pet's current name, birth date, and type.
3. Clinic User amends one or more fields and submits the form.
4. System validates that the name is not blank and does not exceed its maximum length (BR-004), that the birth date is
   provided and not in the future, and that no other pet of the same owner already has the same name.
5. System stores the updated pet details (name, birth date, type).
6. System returns to the Owner Details view and displays the notification "Pet details has been edited".

## Alternative Flows

### A1: Duplicate Pet Name

**Trigger:** The submitted name matches a different pet already owned by this owner (step 4).
**Flow:**

1. System rejects the name field with the error "already exists".
2. System re-renders the pet edit form with the error, keeping every value the Clinic User entered (BR-005).
3. Clinic User adjusts the name.
4. Use case continues at step 3.

### A2: Birth Date in the Future

**Trigger:** The submitted birth date is later than today (step 4).
**Flow:**

1. System rejects the birth date field with the error "invalid date".
2. System re-renders the pet edit form with the error, keeping every value the Clinic User entered (BR-005).
3. Clinic User corrects the date.
4. Use case continues at step 3.

### A3: Missing Required Field

**Trigger:** Name or birth date is missing (step 4).
**Flow:**

1. System rejects the affected field with a "required" error.
2. System re-renders the pet edit form with the error messages, keeping every value the Clinic User entered (BR-005).
3. Clinic User supplies the missing value(s).
4. Use case continues at step 3.

### A4: Pet Edit Cancelled

**Trigger:** Clinic User cancels instead of submitting the form (step 3).
**Flow:**

1. System discards the amended values and returns to the Owner Details view.
2. Use case ends.

### A5: Owner Not Found

**Trigger:** No owner exists for the requested identifier, for example when the edit form is opened from an outdated
bookmark (step 2).
**Flow:**

1. System cannot resolve the owner and shows the application error view (UC-010 A1).
2. Use case ends.

### A6: Pet Not Found for Owner

**Trigger:** The requested pet does not exist or does not belong to the owner (step 2).
**Flow:**

1. System cannot resolve the pet for the given owner and shows the application error view (UC-010 A1).
2. Use case ends.

### A7: Pet Name Too Long

**Trigger:** The submitted name exceeds its maximum length (step 4).
**Flow:**

1. System rejects the name field with an error stating the maximum length (BR-004).
2. System re-renders the pet edit form with the error, keeping every value the Clinic User entered (BR-005).
3. Clinic User shortens the name.
4. Use case continues at step 3.

## Postconditions

### Success Postconditions

- The pet record reflects the updated name, birth date, and type.
- The user is viewing the Owner Details view with the updated pet.

### Failure Postconditions

- No pet record is changed.

## Business Rules

### BR-001: Unique Pet Name per Owner

UC-007 BR-001 applies on update as well; the pet being edited is not compared against itself ([FR-015](../requirements.md)).

### BR-002: Birth Date Not in Future

UC-007 BR-002 applies on update as well ([FR-016](../requirements.md)).

### BR-003: Pet Type on Update

A pet always keeps a type: the edit form offers only the available pet types, the current type stays selected unless
the Clinic User chooses another one, and the type cannot be removed. UC-007 BR-003 therefore needs no separate check
on update ([FR-017](../requirements.md)).

### BR-004: Pet Name Length

UC-007 BR-004 applies on update as well ([FR-023](../requirements.md)).

### BR-005: Entries Preserved on Rejection

UC-003 BR-005 applies to the pet edit form as well ([FR-023](../requirements.md)).
