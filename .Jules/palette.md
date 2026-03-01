
## 2024-05-18 - Standardize Checkboxes
**Learning:** Native checkboxes stick out visually and lack consistent focus rings across browsers. Using the custom `Checkbox` component standardizes appearance, provides a clear focus ring (`peer-focus-visible:ring-sky-400`), and uses an accessible sr-only input under the hood.
**Action:** Always use the `<Checkbox>` component instead of native `<input type="checkbox">` for better visual consistency and a11y focus states.
