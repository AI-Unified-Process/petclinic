# Use Case: Add Pet to Owner

## Overview

**Use Case ID:** UC-007   
**Use Case Name:** Add Pet to Owner   
**Primary Actor:** Clinic User   
**Goal:** Register a new pet under an existing owner so that visits and medical information can subsequently be tracked
for it.   
**Trigger:** An owner brings a pet to the clinic that is not yet recorded, or asks to have a new pet recorded.   
**Status:** Approved   
**Requirements:** [FR-014, FR-015, FR-016, FR-022, FR-023, C-011](../requirements.md)

## Preconditions

- At least one pet type (e.g., cat, dog, hamster) is configured in the database ([C-011](../requirements.md)).

## Main Success Scenario

1. Clinic User clicks "Add New Pet" on the Owner Details view for the selected owner.
2. System displays the pet creation form, showing the owner's name and offering a choice of the available pet types.
3. Clinic User enters the pet's name, birth date, and selects a type, then submits the form.
4. System removes leading and trailing spaces from the name (BR-006) and validates that the name is not blank and does
   not exceed its maximum length (BR-004), that the birth date is provided and not in the future, that a type is
   selected, and that no other pet of the same owner already has the same name (BR-001).
5. System records the new pet under the owner.
6. System returns to the Owner Details view and displays the notification "New Pet has been Added".

## Alternative Flows

### A1: Duplicate Pet Name for Owner

**Trigger:** The submitted name matches a pet already owned by this owner (step 4).
**Flow:**

1. System rejects the name field with the error "already exists".
2. System re-renders the pet form with the error message, keeping every value the Clinic User entered (BR-005).
3. Clinic User adjusts the name.
4. Use case continues at step 3.

### A2: Birth Date in the Future

**Trigger:** The submitted birth date is later than today (step 4).
**Flow:**

1. System rejects the birth date field with the error "invalid date".
2. System re-renders the pet form with the error message, keeping every value the Clinic User entered (BR-005).
3. Clinic User corrects the date.
4. Use case continues at step 3.

### A3: Missing Required Field

**Trigger:** Name, birth date, or type is missing (step 4).
**Flow:**

1. System rejects the affected field with a "required" error.
2. System re-renders the pet form with the error messages, keeping every value the Clinic User entered (BR-005).
3. Clinic User supplies the missing value(s).
4. Use case continues at step 3.

### A4: Pet Creation Cancelled

**Trigger:** Clinic User cancels instead of submitting the form (step 3).
**Flow:**

1. System discards the entered values and returns to the Owner Details view.
2. Use case ends.

### A5: Owner Not Found

**Trigger:** No owner exists for the requested identifier, for example when the pet form is opened from an outdated
bookmark (step 2).
**Flow:**

1. System cannot resolve the owner and shows the application error view (UC-010 A1).
2. Use case ends.

### A6: Pet Name Too Long

**Trigger:** The submitted name exceeds its maximum length (step 4).
**Flow:**

1. System rejects the name field with an error stating the maximum length (BR-004).
2. System re-renders the pet form with the error message, keeping every value the Clinic User entered (BR-005).
3. Clinic User shortens the name.
4. Use case continues at step 3.

### A7: Pet Name Taken Meanwhile

**Trigger:** After the validation of step 4, another Clinic User recorded a pet with the same name for the same owner,
so the name is no longer free when the pet is recorded (step 5).
**Flow:**

1. System records no pet and rejects the name field with the error "already exists" (BR-001).
2. System re-renders the pet form with the error message, keeping every value the Clinic User entered (BR-005).
3. Clinic User adjusts the name.
4. Use case continues at step 3.

## Postconditions

### Success Postconditions

- A new pet record exists, linked to the owner.
- The user is viewing the Owner Details view showing the new pet.

### Failure Postconditions

- No pet is recorded.

## Business Rules

### BR-001: Unique Pet Name per Owner

One owner cannot have two pets with the same name. Names are compared after surrounding spaces are removed (BR-006) and case-insensitively, so *Leo*, *leo*, and *" Leo "* are the same name; different owners may have pets with the same name. The rule holds when two Clinic Users record pets for the same owner at the same moment: the later one is rejected on the name field (A7), never with the application error view ([FR-015](../requirements.md)).

### BR-002: Birth Date Not in Future

A pet's date of birth is always recorded, and it lies on or before today ([FR-016](../requirements.md)).

### BR-003: Pet Type Required on Creation

A pet type must be chosen when a pet is first recorded ([FR-014](../requirements.md)).

### BR-004: Pet Name Length

A pet's name holds at most 30 characters. A longer name is rejected on the name field like any other validation error,
never with the application error view ([FR-023](../requirements.md)).

### BR-005: Entries Preserved on Rejection

UC-003 BR-005 applies to the pet form as well ([FR-023](../requirements.md)).

### BR-006: Surrounding Spaces Removed

UC-003 BR-006 applies to the pet name as well ([FR-015](../requirements.md)).
