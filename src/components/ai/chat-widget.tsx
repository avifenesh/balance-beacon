'use client'

import { useState, useRef, useEffect, useMemo, useCallback } from 'react'
import dynamic from 'next/dynamic'
import { Sparkles, X, Send, Plus, Trash2, Pencil, Check, Copy, ClipboardCheck } from 'lucide-react'
import { Currency } from '@prisma/client'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { cn } from '@/utils/cn'
import {
  type Message,
  type ChatSession,
  OPEN_STATE_KEY,
  createTitleFromMessage,
  createSession,
  loadSessionsFromStorage,
  generateMessageId,
  quickPrompts,
} from './chat-utils'

const ChatMarkdown = dynamic(() => import('./chat-markdown').then((m) => ({ default: m.ChatMarkdown })), {
  loading: () => <div className="animate-pulse text-[13px] text-slate-400">Loading...</div>,
})

interface ChatWidgetProps {
  accountId: string
  monthKey: string
  preferredCurrency?: Currency
}

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false)

  const handleCopy = useCallback(async () => {
    try {
      await navigator.clipboard.writeText(text)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      // Clipboard API may not be available in all contexts
    }
  }, [text])

  return (
    <button
      type="button"
      onClick={handleCopy}
      className="absolute right-2 top-2 rounded-md border border-white/10 bg-white/5 p-1 text-slate-400 opacity-0 transition hover:border-white/20 hover:bg-white/10 hover:text-slate-200 group-hover/msg:opacity-100"
      aria-label={copied ? 'Copied' : 'Copy message'}
    >
      {copied ? <ClipboardCheck className="h-3.5 w-3.5 text-emerald-400" /> : <Copy className="h-3.5 w-3.5" />}
    </button>
  )
}

export function ChatWidget({ accountId, monthKey, preferredCurrency }: ChatWidgetProps) {
  const storageKey = useMemo(() => `balance-ai-sessions::${accountId}::${monthKey}`, [accountId, monthKey])
  const activeSessionKey = useMemo(() => `balance-ai-active-session::${accountId}::${monthKey}`, [accountId, monthKey])

  const [isOpen, setIsOpen] = useState(false)
  const [hydrated, setHydrated] = useState(false)

  const loadSessions = useCallback((): ChatSession[] => loadSessionsFromStorage(storageKey), [storageKey])

  const [sessions, setSessions] = useState<ChatSession[]>([])

  const [activeSessionId, setActiveSessionId] = useState('')

  const [renamingSessionId, setRenamingSessionId] = useState<string | null>(null)
  const [renameValue, setRenameValue] = useState('')
  const renameInputRef = useRef<HTMLInputElement>(null)

  const [input, setInput] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const abortRef = useRef<AbortController | null>(null)
  const [lastFailedInput, setLastFailedInput] = useState<string | null>(null)
  const readerRef = useRef<ReadableStreamDefaultReader<Uint8Array> | null>(null)

  // Throttled streaming: track pending content and use RAF to batch updates
  const streamingContentRef = useRef<{ sessionId: string; messageId: string; content: string } | null>(null)
  const rafIdRef = useRef<number | null>(null)

  useEffect(() => {
    if (!isOpen) return

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault()
        setIsOpen(false)
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen])

  useEffect(() => {
    if (!isOpen) return
    const timer = window.requestAnimationFrame(() => inputRef.current?.focus())
    return () => window.cancelAnimationFrame(timer)
  }, [isOpen])

  // Hydrate open state from localStorage on mount, then persist changes
  useEffect(() => {
    if (typeof window === 'undefined') return
    const stored = window.localStorage.getItem(OPEN_STATE_KEY)
    if (stored === 'true') setIsOpen(true)
    setHydrated(true)
  }, [])

  useEffect(() => {
    if (typeof window === 'undefined' || !hydrated) return
    window.localStorage.setItem(OPEN_STATE_KEY, isOpen ? 'true' : 'false')
  }, [isOpen, hydrated])

  // Hydrate sessions on mount to avoid SSR/client mismatch
  useEffect(() => {
    setSessions(loadSessions())
  }, [loadSessions])

  useEffect(() => {
    if (typeof window === 'undefined') return
    const restoredSessions = loadSessions()
    setSessions(restoredSessions)

    const savedActiveId = window.localStorage.getItem(activeSessionKey)
    if (savedActiveId && restoredSessions.some((session) => session.id === savedActiveId)) {
      setActiveSessionId(savedActiveId)
    } else if (restoredSessions[0]?.id) {
      const fallbackId = restoredSessions[0].id
      setActiveSessionId(fallbackId)
      window.localStorage.setItem(activeSessionKey, fallbackId)
    }
  }, [activeSessionKey, loadSessions])

  const setSessionsWithPersist = useCallback(
    (updater: (prev: ChatSession[]) => ChatSession[]) => {
      setSessions((prev) => {
        const next = updater(prev)
        if (typeof window !== 'undefined') {
          window.localStorage.setItem(storageKey, JSON.stringify(next))
        }
        return next
      })
    },
    [storageKey],
  )

  const updateActiveSessionId = useCallback(
    (sessionId: string) => {
      setActiveSessionId(sessionId)
      if (typeof window !== 'undefined') {
        window.localStorage.setItem(activeSessionKey, sessionId)
      }
    },
    [activeSessionKey],
  )

  const currentSession = useMemo(
    () => sessions.find((session) => session.id === activeSessionId) ?? sessions[0],
    [sessions, activeSessionId],
  )

  const messages = useMemo(() => currentSession?.messages ?? [], [currentSession])

  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [])

  useEffect(() => {
    scrollToBottom()
  }, [messages, scrollToBottom])

  const applyMessagesUpdate = useCallback(
    (sessionId: string, transformer: (messages: Message[], session: ChatSession) => Message[]) => {
      setSessionsWithPersist((prev) =>
        prev.map((session) => {
          if (session.id !== sessionId) return session
          const updatedMessages = transformer(session.messages, session)
          const derivedTitle = createTitleFromMessage(updatedMessages.find((message) => message.role === 'user'))
          return {
            ...session,
            title: !session.isCustomTitle && derivedTitle ? derivedTitle : session.title,
            isCustomTitle: session.isCustomTitle || Boolean(derivedTitle && derivedTitle !== session.title),
            messages: updatedMessages,
            updatedAt: new Date().toISOString(),
          }
        }),
      )
    },
    [setSessionsWithPersist],
  )

  // Throttled message content update using requestAnimationFrame
  // Reduces re-renders from ~hundreds per response to ~60fps
  const scheduleStreamingUpdate = useCallback(
    (sessionId: string, messageId: string, content: string) => {
      streamingContentRef.current = { sessionId, messageId, content }

      if (rafIdRef.current !== null) return // Already scheduled

      rafIdRef.current = requestAnimationFrame(() => {
        const pending = streamingContentRef.current
        if (pending) {
          applyMessagesUpdate(pending.sessionId, (existing) =>
            existing.map((message) =>
              message.id === pending.messageId ? { ...message, content: pending.content } : message,
            ),
          )
        }
        rafIdRef.current = null
      })
    },
    [applyMessagesUpdate],
  )

  // Cleanup RAF on unmount
  useEffect(() => {
    return () => {
      if (rafIdRef.current !== null) {
        cancelAnimationFrame(rafIdRef.current)
      }
    }
  }, [])

  const startRename = useCallback((session: ChatSession) => {
    setRenamingSessionId(session.id)
    setRenameValue(session.title)
    queueMicrotask(() => renameInputRef.current?.focus())
  }, [])

  const commitRename = useCallback(() => {
    const trimmed = renameValue.trim()
    if (trimmed && renamingSessionId) {
      setSessionsWithPersist((prev) =>
        prev.map((item) =>
          item.id === renamingSessionId ? { ...item, title: trimmed.slice(0, 64), isCustomTitle: true } : item,
        ),
      )
    }
    setRenamingSessionId(null)
    setRenameValue('')
  }, [renameValue, renamingSessionId, setSessionsWithPersist])

  const cancelRename = useCallback(() => {
    setRenamingSessionId(null)
    setRenameValue('')
  }, [])

  const deleteSession = useCallback(
    (sessionId: string) => {
      setSessionsWithPersist((prev) => {
        if (prev.length <= 1) return prev
        const next = prev.filter((session) => session.id !== sessionId)
        if (!next.length) return prev

        if (sessionId === activeSessionId) {
          const fallbackId = next[0].id
          queueMicrotask(() => updateActiveSessionId(fallbackId))
        }

        return next
      })
    },
    [activeSessionId, setSessionsWithPersist, updateActiveSessionId],
  )

  const createNewSession = useCallback(() => {
    const newSession = createSession(`Conversation ${sessions.length + 1}`)
    setSessionsWithPersist((prev) => [...prev, newSession])
    updateActiveSessionId(newSession.id)
    queueMicrotask(() => inputRef.current?.focus())
  }, [sessions.length, setSessionsWithPersist, updateActiveSessionId])

  const handlePromptClick = (prompt: string) => {
    setInput(prompt)
    inputRef.current?.focus()
  }

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!input.trim() || isLoading || !currentSession) return

    const trimmedInput = input.trim()
    const baseMessages = currentSession.messages
    const sessionId = currentSession.id
    setLastFailedInput(null)

    const userMessage: Message = {
      id: generateMessageId(),
      role: 'user',
      content: trimmedInput,
    }

    applyMessagesUpdate(sessionId, (existing) => [...existing, userMessage])
    setInput('')
    setIsLoading(true)

    // Set up abort controller for cancellable streaming
    const controller = new AbortController()
    abortRef.current = controller

    try {
      const response = await fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          messages: [...baseMessages, userMessage].map((message) => ({
            role: message.role,
            content: message.content,
          })),
          accountId,
          monthKey,
          preferredCurrency,
        }),
        signal: controller.signal,
      })

      if (!response.ok) {
        let errText = ''
        try {
          errText = await response.text()
        } catch {
          // Ignore - errText stays empty
        }
        applyMessagesUpdate(sessionId, (existing) => [
          ...existing,
          {
            id: generateMessageId(),
            role: 'assistant',
            content: `Server error (HTTP ${response.status}). ${errText || 'No error body.'}`,
          },
        ])
        return
      }

      const reader = response.body?.getReader()
      if (!reader) {
        applyMessagesUpdate(sessionId, (existing) => [
          ...existing,
          {
            id: generateMessageId(),
            role: 'assistant',
            content: 'Error: Unable to read response stream',
          },
        ])
        return
      }
      // Store reader ref for cleanup on unmount
      readerRef.current = reader
      const decoder = new TextDecoder()
      let assistantContent = ''

      const assistantMessage: Message = {
        id: generateMessageId(),
        role: 'assistant',
        content: '',
      }

      applyMessagesUpdate(sessionId, (existing) => [...existing, assistantMessage])

      while (reader) {
        const { done, value } = await reader.read()
        if (done) break

        const chunk = decoder.decode(value)
        const lines = chunk.split('\n').filter((line) => line.trim())

        // Surface any non-protocol diagnostics from the server
        const diag = lines.filter((l) => !l.startsWith('0:')).join('\n')

        // Handle AI SDK structured events if present
        let handledStructured = false
        for (const line of lines) {
          if (!line.startsWith('0:')) continue
          try {
            const data = JSON.parse(line.slice(2))
            if (data.type === 'text-delta' && data.textDelta) {
              assistantContent += data.textDelta
              scheduleStreamingUpdate(sessionId, assistantMessage.id, assistantContent)
              handledStructured = true
            }
          } catch {
            // ignore malformed tool chunks
          }
        }

        // Detect Bedrock throttling message and re-run with fallback model
        if (!handledStructured && /Too many tokens|ThrottlingException/i.test(diag)) {
          try {
            reader.cancel()
          } catch {
            // Ignore cancel errors
          }

          const retryResp = await fetch('/api/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              messages: [...baseMessages, userMessage].map((m) => ({ role: m.role, content: m.content })),
              accountId,
              monthKey,
              preferredCurrency,
            }),
            signal: controller.signal,
          })

          const retryReader = retryResp.body?.getReader()
          if (retryReader) {
            while (true) {
              const { done: done2, value: value2 } = await retryReader.read()
              if (done2) break
              const chunk2 = decoder.decode(value2)
              const lines2 = chunk2.split('\n').filter((line) => line.trim())
              for (const line2 of lines2) {
                if (!line2.startsWith('0:')) continue
                try {
                  const data2 = JSON.parse(line2.slice(2))
                  if (data2.type === 'text-delta' && data2.textDelta) {
                    assistantContent += data2.textDelta
                    scheduleStreamingUpdate(sessionId, assistantMessage.id, assistantContent)
                  }
                } catch {
                  // Ignore JSON parse errors for malformed stream lines
                }
              }
            }
          }
          // Stop processing the original stream once fallback is used
          break
        }

        // Fallback: append raw text chunks when no structured parts are present
        if (!handledStructured && diag) {
          assistantContent += diag
          scheduleStreamingUpdate(sessionId, assistantMessage.id, assistantContent)
        }
      }
    } catch (error: unknown) {
      if (error instanceof Error && error.name === 'AbortError') {
        applyMessagesUpdate(sessionId, (existing) => [
          ...existing,
          {
            id: generateMessageId(),
            role: 'assistant',
            content: 'Generation stopped.',
          },
        ])
      } else {
        setLastFailedInput(trimmedInput)
        applyMessagesUpdate(sessionId, (existing) => [
          ...existing,
          {
            id: generateMessageId(),
            role: 'assistant',
            content: 'Sorry, I encountered an error. Please try again.',
          },
        ])
      }
    } finally {
      abortRef.current = null
      readerRef.current = null
      setIsLoading(false)
    }
  }

  const handleStop = useCallback(() => {
    try {
      // Cancel the reader first to stop processing stream data
      readerRef.current?.cancel()
    } catch {
      // Ignore cancel errors
    }
    try {
      abortRef.current?.abort()
    } catch {
      // Ignore abort errors
    }
  }, [])

  // Cleanup on unmount: cancel any in-flight streaming
  useEffect(() => {
    return () => {
      try {
        readerRef.current?.cancel()
      } catch {
        // Ignore cancel errors during unmount
      }
      try {
        abortRef.current?.abort()
      } catch {
        // Ignore abort errors during unmount
      }
    }
  }, [])

  if (!isOpen) {
    return (
      <button
        onClick={() => setIsOpen(true)}
        className="fixed bottom-6 right-6 z-40 inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-4 py-3 text-sm font-medium text-slate-100 shadow-2xl shadow-slate-950/40 backdrop-blur-xl transition hover:border-white/30 hover:bg-white/15 hover:shadow-slate-950/60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-300/60"
        aria-label="Open AI Assistant"
      >
        <Sparkles className="h-5 w-5 text-sky-300" />
        <span className="hidden sm:inline">Ask Balance AI 3.1</span>
      </button>
    )
  }

  return (
    <div className="fixed bottom-6 right-6 z-50 w-[min(360px,calc(100vw-2.5rem))] sm:w-96">
      <div className="flex h-[520px] flex-col overflow-hidden rounded-3xl border border-white/15 bg-slate-950/80 shadow-2xl shadow-slate-950/60 backdrop-blur-xl sm:h-[600px]">
        <div className="flex items-start justify-between border-b border-white/10 bg-gradient-to-r from-sky-500/20 via-slate-950/30 to-transparent px-5 py-4">
          <div className="flex items-center gap-3 text-slate-100">
            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-sky-500/20 text-sky-200 shadow-inner shadow-slate-950/40">
              <Sparkles className="h-5 w-5" />
            </span>
            <div className="flex flex-col">
              <h3 className="text-sm font-semibold text-white">Balance AI 3.1</h3>
              <p className="text-xs text-slate-300">Insights grounded in your accounts and budgets.</p>
            </div>
          </div>
          <button
            onClick={() => setIsOpen(false)}
            className="rounded-full border border-white/10 bg-white/5 p-1.5 text-slate-200 transition hover:border-white/20 hover:bg-white/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-300/50"
            aria-label="Close chat"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="border-b border-white/10 bg-slate-950/60 px-5 py-2">
          <div className="flex items-center gap-3">
            <div className="flex flex-1 items-center gap-2 overflow-x-auto">
              {sessions.map((session) => {
                const isActive = session.id === currentSession?.id
                const isRenaming = renamingSessionId === session.id
                return (
                  <div
                    key={session.id}
                    className={cn(
                      'group flex items-center gap-1 rounded-full border px-3 py-1 text-xs transition',
                      isActive
                        ? 'border-sky-400/40 bg-sky-500/20 text-white shadow-slate-950/40'
                        : 'border-white/10 bg-white/5 text-slate-300 hover:border-white/20 hover:text-white',
                    )}
                  >
                    {isRenaming ? (
                      <div className="flex items-center gap-1">
                        <input
                          ref={renameInputRef}
                          type="text"
                          value={renameValue}
                          onChange={(e) => setRenameValue(e.target.value)}
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') commitRename()
                            if (e.key === 'Escape') cancelRename()
                          }}
                          onBlur={commitRename}
                          className="w-24 bg-transparent text-xs text-white outline-none placeholder:text-slate-500"
                          maxLength={64}
                        />
                        <button
                          type="button"
                          onMouseDown={(e) => e.preventDefault()}
                          onClick={commitRename}
                          className="rounded-full border border-white/10 bg-white/5 p-1 text-emerald-300 hover:border-white/20 hover:bg-white/10"
                          aria-label="Confirm rename"
                        >
                          <Check className="h-3 w-3" />
                        </button>
                        <button
                          type="button"
                          onMouseDown={(e) => e.preventDefault()}
                          onClick={cancelRename}
                          className="rounded-full border border-white/10 bg-white/5 p-1 text-slate-200 hover:border-white/20 hover:bg-white/10"
                          aria-label="Cancel rename"
                        >
                          <X className="h-3 w-3" />
                        </button>
                      </div>
                    ) : (
                      <>
                        <button
                          type="button"
                          onClick={() => {
                            updateActiveSessionId(session.id)
                            queueMicrotask(() => inputRef.current?.focus())
                          }}
                          onDoubleClick={() => startRename(session)}
                          className="max-w-[160px] truncate text-left"
                        >
                          {session.title}
                        </button>
                        <div className="flex items-center gap-1 sm:opacity-0 sm:pointer-events-none sm:group-hover:opacity-100 sm:group-hover:pointer-events-auto sm:group-focus-within:opacity-100 sm:group-focus-within:pointer-events-auto sm:transition-opacity">
                          <button
                            type="button"
                            onClick={() => startRename(session)}
                            className="rounded-full border border-white/10 bg-white/5 p-1 text-slate-200 hover:border-white/20 hover:bg-white/10"
                            aria-label={`Rename ${session.title}`}
                          >
                            <Pencil className="h-3 w-3" />
                          </button>
                          {sessions.length > 1 && (
                            <button
                              type="button"
                              onClick={() => deleteSession(session.id)}
                              className="rounded-full border border-white/10 bg-white/5 p-1 text-slate-200 hover:border-white/20 hover:bg-white/10"
                              aria-label={`Delete ${session.title}`}
                            >
                              <Trash2 className="h-3 w-3" />
                            </button>
                          )}
                        </div>
                      </>
                    )}
                  </div>
                )
              })}
            </div>
            <button
              type="button"
              onClick={createNewSession}
              className="flex items-center gap-1 rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs text-slate-200 transition hover:border-sky-300/40 hover:bg-sky-500/20 hover:text-white"
            >
              <Plus className="h-3.5 w-3.5" />
              New
            </button>
          </div>
        </div>

        <div className="flex-1 space-y-4 overflow-y-auto px-5 py-4 text-slate-100">
          {messages.length === 0 && (
            <div className="mt-6 space-y-6 rounded-2xl border border-white/10 bg-white/5 p-5 text-center text-sm text-slate-300">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-sky-500/20 text-sky-200">
                <Sparkles className="h-6 w-6" />
              </div>
              <div className="space-y-1">
                <p className="text-base font-medium text-white">Ask anything about this workspace.</p>
                <p className="text-xs text-slate-400">
                  Balance AI can summarize trends, explain variances, and draft next steps.
                </p>
              </div>
              <div className="flex flex-wrap justify-center gap-2 text-xs">
                {quickPrompts.map((prompt) => (
                  <button
                    key={prompt.label}
                    onClick={() => handlePromptClick(prompt.text)}
                    className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-slate-200 transition hover:border-sky-300/40 hover:bg-sky-500/10 hover:text-white"
                  >
                    {prompt.label}
                  </button>
                ))}
              </div>
            </div>
          )}

          {messages.map((message) => (
            <div
              key={message.id}
              className={cn('flex w-full', message.role === 'user' ? 'justify-end' : 'justify-start')}
            >
              <div
                className={cn(
                  'relative max-w-[80%] rounded-2xl px-4 py-3 text-sm leading-relaxed shadow shadow-slate-950/30 backdrop-blur',
                  message.role === 'user'
                    ? 'bg-gradient-to-r from-sky-500 to-indigo-500 text-white'
                    : 'group/msg bg-white/8 text-slate-100 ring-1 ring-white/10',
                )}
              >
                {message.role === 'assistant' && <CopyButton text={message.content} />}
                {message.role === 'user' ? (
                  <div className="whitespace-pre-wrap text-[13px]">{message.content}</div>
                ) : (
                  <div className="chat-markdown text-[13px]">
                    <ChatMarkdown content={message.content} />
                  </div>
                )}
              </div>
            </div>
          ))}

          {isLoading && (
            <div className="flex justify-start">
              <div className="flex items-center gap-1 rounded-full border border-white/10 bg-white/5 px-4 py-2">
                <span className="h-2 w-2 animate-bounce rounded-full bg-sky-300" style={{ animationDelay: '0ms' }} />
                <span className="h-2 w-2 animate-bounce rounded-full bg-sky-300" style={{ animationDelay: '120ms' }} />
                <span className="h-2 w-2 animate-bounce rounded-full bg-sky-300" style={{ animationDelay: '240ms' }} />
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {lastFailedInput && !isLoading && (
          <div className="border-t border-white/10 px-5 py-2">
            <button
              type="button"
              onClick={() => {
                setInput(lastFailedInput)
                setLastFailedInput(null)
              }}
              className="w-full rounded-lg bg-sky-500/20 px-3 py-1.5 text-xs text-sky-300 hover:bg-sky-500/30 transition"
            >
              Retry last message
            </button>
          </div>
        )}

        <form onSubmit={handleSubmit} className="border-t border-white/10 bg-slate-950/60 px-5 py-4">
          <div className="flex items-end gap-2">
            <Input
              ref={inputRef}
              value={input}
              onChange={(event) => setInput(event.target.value)}
              placeholder="Ask about your finances..."
              className="flex-1 text-sm"
              disabled={isLoading}
            />
            {isLoading && (
              <Button
                type="button"
                variant="secondary"
                onClick={handleStop}
                className="rounded-xl border border-white/10 bg-white/10 px-3 py-2 text-slate-200 hover:bg-white/15"
                aria-label="Stop generation"
              >
                <X className="h-4 w-4" />
              </Button>
            )}
            <Button
              type="submit"
              variant="secondary"
              className="rounded-xl border border-sky-400/40 bg-sky-500/40 px-4 py-2 text-white shadow-slate-950/40 hover:bg-sky-500/60"
              disabled={isLoading || !input.trim()}
              aria-label="Send message"
            >
              <Send className="h-4 w-4" />
            </Button>
          </div>
          <p className="mt-2 text-[11px] text-slate-500">
            AI answers stay private to your browser session and reference the selected account context.
          </p>
        </form>
      </div>
    </div>
  )
}
