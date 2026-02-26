'use client'

import { useRef, useEffect, useCallback, useState, RefObject } from 'react'
import { Download, LogOut, Trash2 } from 'lucide-react'

type DashboardSettingsMenuProps = {
  anchorRef: RefObject<HTMLButtonElement | null>
  onClose: () => void
  onExport: () => void
  onLogout: () => void
  onDelete: () => void
  isPendingLogout: boolean
}

export function DashboardSettingsMenu({
  anchorRef,
  onClose,
  onExport,
  onLogout,
  onDelete,
  isPendingLogout,
}: DashboardSettingsMenuProps) {
  const menuRef = useRef<HTMLDivElement>(null)
  const [menuPosition, setMenuPosition] = useState<{ top?: number; bottom?: number }>({})

  // Calculate menu position to keep it in viewport
  const calculateMenuPosition = useCallback(() => {
    if (!anchorRef.current || !menuRef.current) return

    const buttonRect = anchorRef.current.getBoundingClientRect()
    const menuHeight = menuRef.current.offsetHeight
    const viewportHeight = window.innerHeight
    const padding = 8

    // If menu would overflow bottom, position above button
    if (buttonRect.bottom + menuHeight + padding > viewportHeight) {
      setMenuPosition({ bottom: viewportHeight - buttonRect.top + padding })
    } else {
      setMenuPosition({ top: buttonRect.bottom + padding })
    }
  }, [anchorRef])

  // Settings menu keyboard navigation with arrow keys and focus trap
  const handleMenuKeyDown = useCallback(
    (event: KeyboardEvent) => {
      // Get only enabled menu items (skip disabled ones)
      const menuItems = menuRef.current?.querySelectorAll('[role="menuitem"]:not([disabled])')
      if (!menuItems || menuItems.length === 0) return

      const itemCount = menuItems.length

      // Find current index among enabled items
      const currentEnabledIndex = Array.from(menuItems).findIndex((item) => item === document.activeElement)

      switch (event.key) {
        case 'ArrowDown':
          event.preventDefault()
          {
            const nextIndex = currentEnabledIndex < 0 ? 0 : (currentEnabledIndex + 1) % itemCount
            ;(menuItems[nextIndex] as HTMLElement)?.focus()
          }
          break
        case 'ArrowUp':
          event.preventDefault()
          {
            const prevIndex = currentEnabledIndex < 0 ? itemCount - 1 : (currentEnabledIndex - 1 + itemCount) % itemCount
            ;(menuItems[prevIndex] as HTMLElement)?.focus()
          }
          break
        case 'Escape':
          event.preventDefault()
          onClose()
          anchorRef.current?.focus()
          break
        case 'Tab':
          // Close menu on Tab to prevent double focus movement
          event.preventDefault()
          onClose()
          anchorRef.current?.focus()
          break
        case 'Home':
          event.preventDefault()
          ;(menuItems[0] as HTMLElement)?.focus()
          break
        case 'End':
          event.preventDefault()
          ;(menuItems[itemCount - 1] as HTMLElement)?.focus()
          break
      }
    },
    [onClose, anchorRef],
  )

  // Focus management and position calculation when menu opens
  useEffect(() => {
    // Use requestAnimationFrame to ensure the menu is rendered before measuring
    const rafId = requestAnimationFrame(() => {
      calculateMenuPosition()
      // Focus first menu item when menu opens
      const firstItem = menuRef.current?.querySelector('[role="menuitem"]') as HTMLElement | null
      firstItem?.focus()
    })

    document.addEventListener('keydown', handleMenuKeyDown)

    return () => {
      cancelAnimationFrame(rafId)
      document.removeEventListener('keydown', handleMenuKeyDown)
    }
  }, [calculateMenuPosition, handleMenuKeyDown])

  return (
    <>
      <div className="fixed inset-0 z-40" onClick={onClose} aria-hidden="true" />
      <div
        ref={menuRef}
        id="settings-menu"
        role="menu"
        aria-label="Account settings"
        className="fixed right-4 z-50 w-48 rounded-lg border border-white/20 bg-slate-900 py-1 shadow-xl"
        style={menuPosition.bottom ? { bottom: menuPosition.bottom } : { top: menuPosition.top }}
      >
        <button
          type="button"
          role="menuitem"
          tabIndex={-1}
          className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-slate-200 hover:bg-white/10 focus:bg-white/10 focus:outline-none"
          onClick={() => {
            onClose()
            onExport()
          }}
        >
          <Download className="h-4 w-4" />
          Export my data
        </button>
        <button
          type="button"
          role="menuitem"
          tabIndex={-1}
          className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-slate-200 hover:bg-white/10 focus:bg-white/10 focus:outline-none"
          onClick={() => {
            onClose()
            onLogout()
          }}
          disabled={isPendingLogout}
        >
          <LogOut className="h-4 w-4" />
          {isPendingLogout ? 'Signing out...' : 'Sign out'}
        </button>
        <div className="my-1 h-px bg-white/10" role="separator" />
        <button
          type="button"
          role="menuitem"
          tabIndex={-1}
          className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-rose-400 hover:bg-rose-500/10 focus:bg-rose-500/10 focus:outline-none"
          onClick={() => {
            onClose()
            onDelete()
          }}
        >
          <Trash2 className="h-4 w-4" />
          Delete account
        </button>
      </div>
    </>
  )
}
