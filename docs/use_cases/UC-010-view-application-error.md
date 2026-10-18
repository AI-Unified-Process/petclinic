# Use Case: View Application Error

## Overview

**Use Case ID:** UC-010   
**Use Case Name:** View Application Error   
**Primary Actor:** Visitor (and Clinic User, via any failure flow)   
**Goal:** Present a friendly error page whenever an unexpected failure occurs during navigation, so users are never shown a raw technical error. Also reachable on demand via the "Error" drawer link, which mirrors the Spring PetClinic `/oups` demonstration page.   
**Status:** Approved   
**Requirements:** [FR-024, FR-025, FR-002, NFR-004, NFR-005, C-010](../requirements.md)

## Preconditions

## Main Success Scenario

1. An unexpected failure occurs while the system prepares a view, **or** the Visitor clicks the "Error" link in the navigation drawer.
2. System resolves the failure to the application error view.
3. System renders the application error view inside the application shell, showing a heading "Something happened...", a paragraph with the message for the user (BR-003), and a "Back to Home" link that returns the user to the welcome page (UC-001).
4. Visitor optionally follows the "Back to Home" link to exit the error flow.

## Alternative Flows

### A1: Resource Not Found

**Trigger:** The failure is caused by a resource that cannot be found (step 1) — for example an unknown owner id (UC-005 A1), an owner or pet id that cannot be resolved (UC-006, UC-007, UC-008, UC-009), or a URL that does not match any view.

**Flow:**

1. System resolves the failure to the not-found variant of the error view.
2. System renders the error view with the failure message.
3. Use case ends.

### A2: Unexpected Error

**Trigger:** Any other unexpected failure occurs (step 1), including the one deliberately raised by the `/oups` demonstration route.

**Flow:**

1. System resolves the failure to the generic variant of the error view.
2. System renders the error view with the generic message "An unexpected error occurred. Please try again later." (BR-003), or, for the `/oups` demonstration route, with the demonstration message (BR-004).
3. Use case ends.

## Postconditions

### Success Postconditions

- The application error view is rendered inside the application shell.
- The navigation drawer and header remain available so the user can leave the error view via any nav link.
- No data is modified.

### Failure Postconditions

_None — the error view is itself the terminal state for failed navigations._

## Business Rules

### BR-001: Anonymous Access

The error view is reachable without authentication ([C-010](../requirements.md)).

### BR-002: Navigation Shell Preserved

The error view renders inside the application shell so the drawer and header remain functional. Users are never stranded on a blank page.

### BR-003: Message Only, No Technical Details

The error view shows a failure's own message only when the application wrote that message for the user: a resource that cannot be found (A1) and the `/oups` demonstration (BR-004). Every other failure is shown with the generic message "An unexpected error occurred. Please try again later.", because its own message may carry technical details. The error view never shows technical details such as a stack trace or the original message of an unexpected failure; those remain visible in server logs only.

### BR-004: `/oups` Demonstration Route

A `/oups` route exists purely to demonstrate the error view. Navigating to it always fails with the message `"Expected: controller used to showcase what happens when an exception is thrown"`, matching the Spring PetClinic original.
