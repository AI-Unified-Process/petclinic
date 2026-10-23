# Use Case: Register New Owner

## Overview

**Use Case ID:** UC-003  
**Use Case Name:** Register New Owner  
**Primary Actor:** Clinic User  
**Goal:** Add a new pet owner to the clinic so that their pets and visits can subsequently be tracked.  
**Trigger:** A new customer comes to the clinic and the Clinic User wants to record them as an owner.  
**Status:** Done  
**Requirements:** [FR-004, FR-005, FR-006, FR-022, FR-023](../requirements.md)

## Preconditions

_None — registering an owner relies on no prior state._

## Main Success Scenario

1. Clinic User chooses "Add Owner" from the Find Owners view.
2. System displays an empty owner form with fields for first name, last name, address, city, and telephone.
3. Clinic User fills in all fields and submits the form.
4. System removes leading and trailing spaces from every field (BR-006) and validates that first name, last name,
   address, city, and telephone are not blank, that none of them exceeds its maximum length (BR-004), and that
   telephone consists of exactly 10 digits.
5. System records the new owner and assigns it a new identifier.
6. System navigates to the Owner Details view for the newly created owner and displays the notification "New Owner
   Created".

## Alternative Flows

### A1: Validation Errors

**Trigger:** One or more fields fail validation — a blank field, a value longer than its maximum length, or a telephone
that is not exactly 10 digits (step 4).  
**Flow:**

1. System re-renders the owner creation form with an error message on each offending field and keeps every value the
   Clinic User entered (BR-005).
2. System displays the notification "There was an error in creating the owner."
3. Clinic User corrects the input.
4. Use case continues at step 3.

### A2: Registration Cancelled

**Trigger:** Clinic User cancels instead of submitting the form (step 3).  
**Flow:**

1. System discards the entered values and returns to the Find Owners view.
2. Use case ends.

## Postconditions

### Success Postconditions

- A new owner exists with the submitted first name, last name, address, city, and telephone.
- The user is viewing the new owner's details page.

### Failure Postconditions

- No owner is recorded.

## Business Rules

### BR-001: Mandatory Fields

First name, last name, address, city, and telephone are required; none of them may be blank — a value of only spaces counts as blank — because the clinic must be able to reach an owner about their animal ([FR-005](../requirements.md)).

### BR-002: Telephone Format

An owner's telephone number is exactly 10 digits ([FR-006](../requirements.md)).

### BR-003: System-Assigned Identifier

An owner's identifier is assigned by the system when the owner is recorded; the Clinic User can neither enter nor
change it.

### BR-004: Maximum Field Lengths

First name and last name hold at most 30 characters, address at most 255, and city at most 80. Longer input is rejected
on the offending field like any other validation error, never with the application error view
([FR-023](../requirements.md)).

### BR-005: Entries Preserved on Rejection

When input is rejected, the error is reported on the offending field and every value the Clinic User entered stays in
the form, so only the rejected field has to be corrected ([FR-023](../requirements.md)).

### BR-006: Surrounding Spaces Removed

Leading and trailing spaces are removed from first name, last name, address, city, and telephone before they are
validated and recorded, so " Smith " is recorded as "Smith" and a value of only spaces is blank (BR-001). Recorded
names therefore match a search the way UC-004 BR-001 trims the search term ([FR-005](../requirements.md)).
