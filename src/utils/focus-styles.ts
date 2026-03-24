/**
 * Shared focus-visible styles for interactive text buttons.
 *
 * These classes ensure consistent keyboard navigation and focus indicators
 * across the application.
 */

/**
 * Focus ring styles for keyboard navigation.
 * Use this with interactive elements that need visible focus indicators.
 */
export const focusRingClasses =
  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/40 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950'

/**
 * Complete text button focus styles including padding and rounding.
 * Combines focus ring with base button styling for inline text buttons.
 */
export const textButtonFocusClasses = 'rounded-md px-2 py-1'
