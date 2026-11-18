# Open Questions

Questions about the specification that the specification does not answer yet. A question is `Resolved` only when
the answer is in the specification itself; **Resolved in** names where. Resolved and withdrawn questions stay as
history.

## OQ-001: Which error message does each offending field show when owner registration is rejected?

**Element:** UC-003 A1  
**Raised by:** /use-case-implement, 2026-11-18  
**Blocking:** No  
**Status:** Open  
**Assumed:** "Must not be blank" for a blank field (BR-001), "Must be at most N characters" for an over-long one (BR-004), "Telephone must be exactly 10 digits" for a malformed telephone (BR-002).  

A1 step 1 requires "an error message on each offending field" but gives no text, while it does fix the notification
"There was an error in creating the owner." A user-visible error message is a decision the agent asks about first.
Readings: (a) one message per rule that restates it, as assumed; (b) a message per field naming the field ("First
name is required"); (c) the wording of the classic Spring PetClinic ("must not be blank", "Telephone must be a
10-digit number"). UC-006 A1 has the same gap for the owner edit form, which shares these messages.
