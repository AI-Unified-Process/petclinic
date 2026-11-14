# Use Case: View Application Error

## Overview

**Use Case ID:** UC-010  
**Use Case Name:** View Application Error  
**Primary Actor:** Visitor, Clinic User  
**Goal:** See a friendly error page with a way back whenever a failure occurs during navigation, so users are never shown a raw technical error or stranded on a blank page. The "Error" drawer link demonstrates this page on demand, mirroring the Spring PetClinic `/oups` demonstration page.  
**Trigger:** A Visitor or Clinic User opens the error demonstration, or a failure occurs while the system prepares a view the user requested.  
**Status:** Done  
**Requirements:** [FR-024, FR-025, FR-002, NFR-004, NFR-005, C-010](../requirements.md)

## Preconditions

_None — the error view relies on no prior state._

## Main Success Scenario

1. Visitor clicks the "Error" link in the navigation drawer.
2. System deliberately fails with the demonstration message (BR-004) and resolves the failure to the application error
   view.
3. System renders the application error view inside the application shell (BR-002), showing a heading "Something
   happened...", a paragraph with the message for the user (BR-003), and a "Back to Home" link.
4. Visitor returns to the welcome page via the "Back to Home" link (UC-001), or leaves the error view through any other
   navigation link.

## Alternative Flows

### A1: Resource Not Found

**Trigger:** Instead of the demonstration, a resource cannot be found while the system prepares a requested view
(step 1) — for example an unknown owner id (UC-005 A1), an owner or pet that cannot be resolved (UC-006 A3, UC-007 A5,
UC-008 A5 and A6, UC-009 A2 and A3), or a URL that does not match any view.  
**Flow:**

1. System resolves the failure to the not-found variant of the error view.
2. System renders the error view as in step 3, with the not-found message for what could not be found (BR-003).
3. Use case continues at step 4.

### A2: Unexpected Error

**Trigger:** Instead of the demonstration, any other failure occurs while the system prepares a requested view
(step 1).  
**Flow:**

1. System resolves the failure to the generic variant of the error view.
2. System renders the error view as in step 3, with the generic message "An unexpected error occurred. Please try
   again later." (BR-003).
3. Use case continues at step 4.

## Postconditions

### Success Postconditions

- The application error view is rendered inside the application shell.
- The navigation drawer and header remain available so the user can leave the error view via any nav link.
- No data is modified.

### Failure Postconditions

- No data is modified.
- No technical details of the failure are shown to the user (BR-003).

## Business Rules

### BR-001: Anonymous Access

UC-001 BR-001 applies to the error view as well ([C-010](../requirements.md)).

### BR-002: Navigation Shell Preserved

The error view renders inside the application shell so the drawer and header remain functional. Users are never stranded on a blank page ([NFR-005](../requirements.md)).

### BR-003: Message Only, No Technical Details

The error view shows a failure's own message only when the application wrote that message for the user: a resource that cannot be found (A1) and the error demonstration (BR-004). The not-found messages are "Owner not found" when no owner exists with the requested identifier, "Pet not found" when the requested pet does not exist or belongs to another owner, and "Page not found" for a URL that matches no view; the message the framework produces for an unknown URL is never shown. Every other failure is shown with the generic message "An unexpected error occurred. Please try again later.", because its own message may carry technical details. The error view never shows technical details such as a stack trace or the original message of an unexpected failure; those remain visible in server logs only ([NFR-004](../requirements.md)).

### BR-004: Error Demonstration

The error demonstration, reached through the "Error" link and at the address `/oups`, exists purely to demonstrate the error view. Opening it always fails with the message `"Expected: controller used to showcase what happens when an exception is thrown"`, matching the Spring PetClinic original ([FR-025](../requirements.md)).
