# Use Case: Update Owner

## Overview

**Use Case ID:** UC-006   
**Use Case Name:** Update Owner   
**Primary Actor:** Clinic User   
**Goal:** Modify an existing owner's contact information.   
**Trigger:** An owner reports changed contact details, for example after moving or getting a new telephone number.   
**Status:** Done   
**Requirements:** [FR-013, FR-005, FR-006, FR-022, FR-023](../requirements.md)

## Preconditions

## Main Success Scenario

1. Clinic User clicks "Edit Owner" on the Owner Details view.
2. System loads the existing owner and displays the owner edit form, pre-filled with the current first name, last name,
   address, city, and telephone.
3. Clinic User amends one or more fields and submits the form.
4. System validates that first name, last name, address, city, and telephone are not blank (BR-001), that none of them
   exceeds its maximum length (BR-003), and that telephone consists of exactly 10 digits.
5. System stores the updated owner.
6. System returns to the Owner Details view and displays the notification "Owner Values Updated".

## Alternative Flows

### A1: Validation Errors

**Trigger:** One or more fields fail validation — a blank field, a value longer than its maximum length, or a telephone
that is not exactly 10 digits (step 4).
**Flow:**

1. System re-renders the owner edit form with an error message on each offending field and keeps every value the
   Clinic User entered (BR-004).
2. System displays the notification "There was an error in updating the owner."
3. Clinic User corrects the input.
4. Use case continues at step 3.

### A2: Edit Cancelled

**Trigger:** Clinic User cancels instead of submitting the form (step 3).
**Flow:**

1. System discards the amended values and returns to the Owner Details view.
2. Use case ends.

### A3: Owner Not Found

**Trigger:** No owner exists for the requested identifier, for example when the edit form is opened from an outdated
bookmark (step 2).
**Flow:**

1. System cannot resolve the owner and shows the application error view (UC-010 A1).
2. Use case ends.

## Postconditions

### Success Postconditions

- The owner record reflects the submitted values.
- The user sees the updated Owner Details view.

### Failure Postconditions

- No owner record is changed.

## Business Rules

### BR-001: Mandatory Fields

UC-003 BR-001 applies on update as well: none of the contact fields may be cleared or left blank ([FR-005](../requirements.md)).

### BR-002: Telephone Format

UC-003 BR-002 applies on update as well ([FR-006](../requirements.md)).

### BR-003: Maximum Field Lengths

UC-003 BR-004 applies on update as well ([FR-023](../requirements.md)).

### BR-004: Entries Preserved on Rejection

UC-003 BR-005 applies on update as well ([FR-023](../requirements.md)).
