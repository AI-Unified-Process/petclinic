# Open Questions

Questions about the specification that the specification does not answer yet. A question is `Resolved` only when
the answer is in the specification itself; **Resolved in** names where. Resolved and withdrawn questions stay as
history.

## OQ-001: Can a visit be booked for a date before the pet's birth date?

**Element:** UC-009 BR-004
**Raised by:** /use-case-test, 2026-10-27
**Blocking:** Yes
**Status:** Open

BR-004 accepts any calendar date for a visit, and UC-007 BR-002 keeps the birth date on or before today, but no rule
relates the two. Readings: (a) any date is accepted, as BR-004 says; (b) a date before the birth date is rejected on
the date field. The tests for BR-004 cannot state their expectation until this is decided.

## OQ-002: Do international telephone numbers have to be accepted?

**Element:** UC-003 BR-002
**Raised by:** /use-case-spec, 2026-10-27
**Blocking:** Yes
**Status:** Open

BR-002 requires exactly 10 digits, which only fits North American numbers. The clinic also registers owners from
abroad. Readings: (a) keep 10 digits; (b) allow a leading + and 7 to 15 digits (E.164); (c) no format check, only
not blank.

## OQ-003: How many previous visits does the visit form show?

**Element:** UC-009 step 2
**Raised by:** /use-case-implement, 2026-10-28
**Blocking:** No
**Status:** Open
**Assumed:** All visits of the pet, newest first.

Step 2 shows the pet's previous visits for context but sets no limit. A pet with a long history pushes the form far
down. Readings: (a) all visits; (b) the last five, with a link to the Owner Details view for the rest.

## OQ-004: Does the owner search also match the first name?

**Element:** UC-004 BR-001
**Raised by:** S. Martinelli, 2026-10-28
**Blocking:** No
**Status:** Answered

BR-001 matches the start of the last name only. At the front desk owners are often known by their first name.

**Answer:** No. The search stays on the last name; a search over first and last name is a separate requirement for
the next release.
**Answered by:** Head of Front Desk, 2026-10-30

## OQ-005: May a pet's name be changed to the name of another pet of the same owner?

**Element:** UC-008
**Raised by:** /use-case-spec, 2026-10-24
**Blocking:** No
**Status:** Resolved

UC-007 BR-001 keeps pet names unique per owner on creation; UC-008 did not say whether the rule also holds on update.

**Answer:** Yes, the rule holds on update as well.
**Answered by:** Clinic Manager, 2026-10-25
**Resolved in:** UC-008 BR-001
