
## 2024-05-19 - Add explicit focus-visible classes and type attribute to raw <button> tags
**Learning:** Raw inline `<button>` tags added ad-hoc often miss standard keyboard accessibility (like visible focus rings) and functional attributes (`type="button"`). This can cause screen reader issues, keyboard navigation dead ends, and unintended form submissions.
**Action:** When creating raw `<button>` elements, always explicitly define `type="button"` and ensure standard tailwind `focus-visible:` accessibility styles (e.g. `focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/40 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950`) are present to match the overall design system focus rings.
