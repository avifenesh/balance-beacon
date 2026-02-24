## 2026-02-24 - Icon-Only Buttons Missing Accessible Names
**Learning:** Dashboard components (`RequestList`, `SharedExpensesList`, `SettlementSummary`) frequently use icon-only buttons with `title` attributes but missing `aria-label`. While `title` provides a tooltip, it is not a reliable accessible name for all screen readers.
**Action:** Always add `aria-label` to icon-only buttons, even if `title` is present. Use descriptive labels that include context (e.g., "Reject request from [Name]" instead of just "Reject").
