# Open Questions

Questions about the specification that the specification does not answer yet. A question is `Resolved` only when
the answer is in the specification itself; **Resolved in** names where. Resolved and withdrawn questions stay as
history.

## OQ-001: Is UC-002 Tested, or do its requirements fall back to Open?

**Element:** UC-002
**Raised by:** /use-case-test, 2026-11-11
**Blocking:** Yes
**Status:** Open

The `**Status:**` line of UC-002 reads `Approved`. Its last change on this branch was a reformat only (line breaks
and trailing spaces), but it left the status at `Approved`. An earlier step on the branch had moved the use case to
`Tested`. `docs/requirements.md` still shows the requirement statuses from that `Tested` step: FR-003 `Verified`,
and NFR-001, NFR-002, NFR-003 and C-011 `In Progress`. `RequirementTraceabilityTest.theStatusOfARequirementFollowsItsUseCases`
works out each requirement's status from the statuses of its use cases. With UC-002 at `Approved`, those five
requirements should be `Open`, so the build fails. All 10 tests in `UC002ViewVeterinariansTest` pass. No test
can resolve this, and this skill may not change either document.

Readings: (a) UC-002 goes back to `Tested` after `/use-case-coverage UC-002`, and the requirement statuses stay as
they are; (b) UC-002 stays `Approved`, and `/requirements` sets FR-003, NFR-001, NFR-002, NFR-003 and C-011 back
to `Open`.
