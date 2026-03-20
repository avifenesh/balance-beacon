## 2026-03-20 - Missing focus states on raw buttons
**Learning:** Raw `<button>` tags scattered throughout the app without leveraging the standard `<Button>` component are frequently missing focus-visible classes, making them invisible to keyboard navigation users.
**Action:** When adding or auditing raw button elements, ensure explicit focus styles (e.g., `focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/40 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950`) are applied to maintain keyboard accessibility.
