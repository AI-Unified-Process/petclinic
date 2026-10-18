# Use Case: Book Visit for Pet

## Overview

**Use Case ID:** UC-009   
**Use Case Name:** Book Visit for Pet   
**Primary Actor:** Clinic User   
**Goal:** Record a veterinary visit for an existing pet, documenting the date and the reason for the appointment.   
**Trigger:** An owner brings a pet in or asks for an appointment, and the Clinic User wants to record the visit.   
**Status:** Approved   
**Requirements:** [FR-018, FR-019, FR-020, FR-021, FR-022, FR-023](../requirements.md)

## Preconditions

_None — the owner and pet are resolved in step 2, and A2 and A3 handle a missing or foreign one._

## Main Success Scenario

1. Clinic User clicks "Add Visit" next to the pet on the Owner Details view.
2. System loads the owner and pet, then displays the visit form. The date field is pre-populated with today's date; the
   pet's name and previous visits (in the order of UC-005 BR-001) are shown for context.
3. Clinic User optionally adjusts the date, enters a description of the visit, and submits the form.
4. System validates that a date is given and that the description is not blank and does not exceed its maximum length
   (BR-005).
5. System records the visit for the pet.
6. System returns to the Owner Details view and displays the notification "Your visit has been booked".

## Alternative Flows

### A1: Missing Description

**Trigger:** The description field is blank (step 4).
**Flow:**

1. System re-renders the visit form with a "required" error on the description field, keeping the entered date
   (BR-006).
2. Clinic User enters a description.
3. Use case continues at step 3.

### A2: Pet Not Owned by Given Owner

**Trigger:** The requested pet does not exist or does not belong to the owner (step 2).
**Flow:**

1. System cannot resolve the pet for the given owner and shows the application error view (UC-010 A1).
2. Use case ends.

### A3: Owner Not Found

**Trigger:** No owner exists for the requested identifier (step 2).
**Flow:**

1. System cannot resolve the owner and shows the application error view (UC-010 A1).
2. Use case ends.

### A4: Visit Booking Cancelled

**Trigger:** Clinic User cancels instead of submitting the form (step 3).
**Flow:**

1. System discards the entered values and returns to the Owner Details view.
2. Use case ends.

### A5: Description Too Long

**Trigger:** The description exceeds its maximum length (step 4).
**Flow:**

1. System re-renders the visit form with an error on the description field stating the maximum length, keeping every
   value the Clinic User entered (BR-006).
2. Clinic User shortens the description.
3. Use case continues at step 3.

### A6: Missing Visit Date

**Trigger:** The Clinic User cleared the date field (step 4).
**Flow:**

1. System re-renders the visit form with a "required" error on the date field, keeping the entered description
   (BR-006).
2. Clinic User enters a date.
3. Use case continues at step 3.

## Postconditions

### Success Postconditions

- A new visit record exists, linked to the pet.
- The visit appears in the pet's visit history on the Owner Details view.

### Failure Postconditions

- No visit is recorded.

## Business Rules

### BR-001: Description Required

Every visit must have a non-blank description ([FR-020](../requirements.md)).

### BR-002: Default Date

If the user does not change the date field, the visit is recorded with today's date ([FR-019](../requirements.md)).

### BR-003: Owner/Pet Consistency

A visit can only be booked for a pet through the owner who owns it; a request that names a pet of another owner is
rejected.

### BR-004: Visit Date Required, Past or Future

Every visit has a date. Any calendar date is accepted, so a walk-in can be recorded for today, a missed entry for an
earlier day, and an appointment for a later day.

### BR-005: Description Length

A visit description holds at most 255 characters. A longer description is rejected on the description field like any
other validation error, never with the application error view ([FR-023](../requirements.md)).

### BR-006: Entries Preserved on Rejection

UC-003 BR-005 applies to the visit form as well ([FR-023](../requirements.md)).
