## 2025-05-18 - Icon-Only Button Accessibility
**Learning:** Icon-only buttons (like delete 'X' or notification 'Bell') are invisible to screen readers without explicit labels.
**Action:** Always add `aria-label="Action name"` to the button and `aria-hidden="true"` to the icon itself to prevent redundant announcements.
