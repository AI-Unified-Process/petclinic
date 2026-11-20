# Open Questions

Questions about the specification that the specification does not answer yet. A question is `Resolved` only when
the answer is in the specification itself; **Resolved in** names where. Resolved and withdrawn questions stay as
history.

## OQ-001: Which error message does each offending owner field show?

**Element:** UC-003 A1  
**Raised by:** /use-case-implement, 2026-11-20  
**Blocking:** No  
**Status:** Open  
**Assumed:** Each message restates the rule the field broke: "<Field> is required" (BR-001), "<Field> may be at most N characters" (BR-004), "Telephone must be exactly 10 digits" (BR-002).  

A1 step 1 asks for "an error message on each offending field" and fixes the two notifications ("New Owner Created",
"There was an error in creating the owner."), but states no text for the field messages themselves; neither UC-006
(which shares the form) nor any test case does. Readings: (a) restate the broken rule per field, as assumed;
(b) one generic message per kind of error ("must not be blank", "too long", "invalid telephone"); (c) the wording of
the original Spring PetClinic ("must not be blank", "Telephone must be a 10-digit number"). The messages carry
business meaning, so the decision rights reserve them for asking; the code follows the assumed reading until the
specification states them.
