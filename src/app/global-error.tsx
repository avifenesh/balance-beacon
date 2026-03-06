'use client'

import { useEffect } from 'react'
import { Sentry } from '@/lib/monitoring/sentry-client'

export default function GlobalError({
  error,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  useEffect(() => {
    // Don't report server action mismatch errors - they're expected during deployments
    const isDeploymentError = error.message?.includes('Failed to find Server Action')

    if (!isDeploymentError) {
      Sentry.captureException(error)
    }
  }, [error])

  // Check if this is a deployment-related error
  const isDeploymentError = error.message?.includes('Failed to find Server Action')

  return (
    <html>
      <body>
        <div className="flex min-h-screen items-center justify-center p-4 bg-slate-950">
          <div className="max-w-md rounded-2xl border border-white/15 bg-white/10 p-6 shadow-2xl backdrop-blur-xl">
            {isDeploymentError ? (
              <>
                <h2 className="mb-2 text-lg font-semibold text-white">App Updated</h2>
                <p className="text-sm text-slate-300">A new version has been deployed. Please refresh to continue.</p>
              </>
            ) : (
              <>
                <h2 className="mb-2 text-lg font-semibold text-white">Something went wrong</h2>
                <p className="text-sm text-slate-300">An unexpected error occurred. Our team has been notified.</p>
              </>
            )}
            <button
              onClick={() => window.location.reload()}
              className="mt-4 rounded-lg bg-sky-600 px-4 py-2 text-sm text-white hover:bg-sky-500 transition"
            >
              Refresh page
            </button>
          </div>
        </div>
      </body>
    </html>
  )
}
