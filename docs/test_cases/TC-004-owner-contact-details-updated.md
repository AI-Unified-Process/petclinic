# Test Case: Owner Contact Details Updated

## Overview

**ID:** TC-004  
**Goal:** A clinic user finds an owner who has moved and changed number, and updates the owner's address and telephone — verifying the new contact details are what a later search shows.  
**Priority:** Medium  
**Status:** Automated  
**Process:** [customer-record-maintenance.bpmn](../processes/customer-record-maintenance.bpmn) — Owner reports changed details → UC-004 Find Owners by Last Name → UC-005 View Owner Details → What changed? owner contact details → UC-006 Update Owner → Record updated

## Roles

- Clinic User (finds the owner and updates the contact details)

## Preconditions

- The owner "George Franklin", 110 W. Liberty St., Madison, 6085551023, exists and is the only owner whose last name starts with "Franklin" (Flyway test data `V2__seed_reference_data.sql`), so the search lands directly on his details.
- The decision "What changed? owner contact details" is forced by the journey itself: the caller reports a new address and telephone, not a change to a pet.

## Flow

| Step | Name                  | Description                                                                                                         | Test Data                                  | Use Case                                                  |
|------|-----------------------|---------------------------------------------------------------------------------------------------------------------|--------------------------------------------|-----------------------------------------------------------|
| 1    | Find owner            | The Clinic User opens Find Owners and searches for the owner's last name                                            | Franklin                                   | [UC-004](../use_cases/UC-004-find-owners-by-last-name.md) |
| 2    | Review owner details  | The Owner Details view of the owner found in step 1 shows the address and telephone currently on record              | -                                          | [UC-005](../use_cases/UC-005-view-owner-details.md)       |
| 3    | Update owner          | The Clinic User chooses "Edit Owner", replaces address and telephone and submits the form; the other fields stay as they are | 88 Lakeshore Drive, 6085550777       | [UC-006](../use_cases/UC-006-update-owner.md)             |
| 4    | Verify owner updated  | The notification "Owner Values Updated" is shown and the Owner Details view displays the new address and telephone  | -                                          | -                                                         |

## Validation

1. **New details are found**: After the flow, searching Find Owners for "Franklin" leads to George Franklin, 88 Lakeshore Drive, Madison, 6085550777.
2. **Pets untouched**: George Franklin still owns exactly the pet "Leo" (cat, born 2000-09-07), which still has no visits.
3. **Nothing else changed**: Searching Find Owners with an empty last name lists the same owners, with the same name, address, city, telephone and pets, as before the journey — except George Franklin's new address and telephone. No owner and no pet was added.

## Postconditions

- The seeded owner "George Franklin" has address "88 Lakeshore Drive" and telephone "6085550777".
- Cleanup: restore George Franklin's address to "110 W. Liberty St." and telephone to "6085551023" — the seeded record must end as the seed data defines it. Nothing is deleted.
