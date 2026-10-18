# Use Case: Find Owners by Last Name

## Overview

**Use Case ID:** UC-004   
**Use Case Name:** Find Owners by Last Name   
**Primary Actor:** Clinic User   
**Goal:** Locate one or more owners by their last name so their details can be reviewed or edited.   
**Trigger:** A customer calls or comes to the clinic, and the Clinic User needs to find the customer's record from their
last name.   
**Status:** Approved   
**Requirements:** [FR-007, FR-008, FR-009, FR-010, NFR-001, NFR-002](../requirements.md)

## Preconditions

## Main Success Scenario

1. Clinic User clicks "Find Owners" in the navigation bar.
2. System displays the Find Owners form with a single "Last name" input field.
3. Clinic User enters all or the beginning of an owner's last name and submits the form.
4. System searches for owners whose last name starts with the entered term (BR-001).
5. System finds more than one matching owner and renders the Owners List, sorted by last name (BR-004) and loaded
   incrementally as the Clinic User scrolls (BR-002), showing, for each owner, their name, address, city, telephone,
   and pets.
6. Clinic User selects an owner from the list to navigate to the Owner Details view (UC-005).

## Alternative Flows

### A1: Empty Last-Name Search

**Trigger:** Clinic User submits the form with an empty last-name field, or one that holds only spaces (step 3).
**Flow:**

1. System treats the empty term as matching every owner (BR-003).
2. Use case continues at step 4.

### A2: Exactly One Match

**Trigger:** The search returns exactly one owner (step 4).
**Flow:**

1. System navigates directly to the Owner Details view for that owner (UC-005).
2. Use case ends.

### A3: No Match

**Trigger:** The search returns no owners (step 4).
**Flow:**

1. System re-renders the Find Owners form, keeping the entered search term.
2. System attaches the error "not found" to the last-name field.
3. Clinic User adjusts the search term.
4. Use case continues at step 3.

### A4: Scroll Through Results

**Trigger:** The result set is larger than what fits on screen (step 5).
**Flow:**

1. Clinic User scrolls toward the bottom of the Owners List.
2. System fetches the next set of owners and appends them to the list (BR-002).
3. Use case continues at step 6.

## Postconditions

### Success Postconditions

- The matching owners are displayed, or the user is viewing the single matching owner's details page.
- No data is modified.

### Failure Postconditions

- No data is modified.

## Business Rules

### BR-001: Prefix Match

Searches use a case-insensitive "starts with" match on last name, so "dav", "Dav", and "DAV" find the same owners;
full-string matches are not required. Leading and trailing spaces in the search term are ignored, so "  Dav" finds
the same owners as "Dav". This deviates from the original Spring PetClinic, whose match is case-sensitive on
PostgreSQL, because a caller's spelling of their name says nothing about its capitalisation
([FR-007](../requirements.md)).

### BR-002: Lazy Loading

The Owners List fetches its rows from the backend as the user scrolls, with no page controls and no fixed page size, and the rows fetched per scroll step do not grow with the number of owners ([NFR-001, NFR-002](../requirements.md)).

### BR-003: Empty Search Returns All

An empty last-name field, or one that holds only spaces, matches every owner rather than raising a validation error;
the number of owners found then decides the outcome as for any other search — the Owners List for more than one, the
Owner Details view for exactly one, and "not found" when no owner is recorded ([FR-009](../requirements.md)).

### BR-004: Owners List Order

The Owners List is sorted by last name, and owners with the same last name by first name, so the order stays the same
while the user scrolls.
