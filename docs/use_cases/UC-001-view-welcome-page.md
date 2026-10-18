# Use Case: View Welcome Page

## Overview

**Use Case ID:** UC-001   
**Use Case Name:** View Welcome Page   
**Primary Actor:** Visitor, Clinic User   
**Goal:** Display the application's home page so the user can orient themselves and navigate to the main functional areas.   
**Trigger:** A Visitor or Clinic User opens the PetClinic application.   
**Status:** Done   
**Requirements:** [FR-001, FR-002, C-010](../requirements.md)

## Preconditions

_None — the welcome page relies on no prior state._

## Main Success Scenario

1. Visitor navigates to the root URL (`/`) of the PetClinic application.
2. System renders the welcome page with the clinic logo, a decorative image, and the main navigation bar.
3. Visitor sees navigation links for Home, Find Owners, Veterinarians, and Error.

## Alternative Flows

_None — the welcome page shows static content without data access, so no step of the main success scenario can fail or branch._

## Postconditions

### Success Postconditions

- Welcome page is rendered in the visitor's browser.
- No application state is changed.

### Failure Postconditions

- No application state is changed.

## Business Rules

### BR-001: Anonymous Access

The welcome page is reachable without authentication; there are no accounts and no login ([C-010](../requirements.md)).
