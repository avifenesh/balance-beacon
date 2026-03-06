import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  createTitleFromMessage,
  createSession,
  loadSessionsFromStorage,
  generateMessageId,
  quickPrompts,
  type Message,
  type ChatSession,
} from '@/components/ai/chat-utils'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function makeMessage(content: string, role: 'user' | 'assistant' = 'user'): Message {
  return { id: 'msg-1', role, content }
}

function makeSession(overrides: Partial<ChatSession> = {}): ChatSession {
  return {
    id: 'session-1',
    title: 'Test session',
    messages: [],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    isCustomTitle: false,
    ...overrides,
  }
}

// ---------------------------------------------------------------------------
// createTitleFromMessage
// ---------------------------------------------------------------------------

describe('createTitleFromMessage()', () => {
  it('returns null when message is undefined', () => {
    expect(createTitleFromMessage(undefined)).toBeNull()
  })

  it('returns null when message content is empty string', () => {
    expect(createTitleFromMessage(makeMessage(''))).toBeNull()
  })

  it('returns null when message content is whitespace only', () => {
    expect(createTitleFromMessage(makeMessage('   '))).toBeNull()
  })

  it('returns the full content when it is 40 characters or fewer', () => {
    const content = 'Show me my spending' // 19 chars
    expect(createTitleFromMessage(makeMessage(content))).toBe('Show me my spending')
  })

  it('returns exact 40-character content without ellipsis', () => {
    const content = 'A'.repeat(40) // exactly 40 chars
    expect(createTitleFromMessage(makeMessage(content))).toBe('A'.repeat(40))
  })

  it('truncates content longer than 40 characters and appends ellipsis', () => {
    const content = 'A'.repeat(41)
    const result = createTitleFromMessage(makeMessage(content))
    expect(result).toBe('A'.repeat(40) + '…')
  })

  it('truncates at exactly 40 characters before adding ellipsis', () => {
    const content = 'What are my biggest expenses this month?!' // 41 chars
    const result = createTitleFromMessage(makeMessage(content))
    expect(result).toBe('What are my biggest expenses this month?' + '…')
    expect(result!.length).toBe(41) // 40 chars + 1 ellipsis character
  })

  it('trims leading and trailing whitespace before evaluating length', () => {
    const content = '  short  '
    expect(createTitleFromMessage(makeMessage(content))).toBe('short')
  })

  it('trims whitespace before checking truncation threshold', () => {
    // Pad to 50 chars total with spaces on both sides; trimmed is 48 chars > 40
    const inner = 'B'.repeat(48)
    const content = ' ' + inner + ' '
    const result = createTitleFromMessage(makeMessage(content))
    expect(result).toBe('B'.repeat(40) + '…')
  })

  it('works correctly for assistant messages, not just user messages', () => {
    const msg = makeMessage('Assistant reply that is short', 'assistant')
    expect(createTitleFromMessage(msg)).toBe('Assistant reply that is short')
  })

  it('uses the single ellipsis character (U+2026), not three dots', () => {
    const content = 'X'.repeat(50)
    const result = createTitleFromMessage(makeMessage(content))
    expect(result).toContain('…')
    expect(result).not.toMatch(/\.\.\.$/)
  })
})

// ---------------------------------------------------------------------------
// createSession
// ---------------------------------------------------------------------------

describe('createSession()', () => {
  it('returns an object with the given title', () => {
    const session = createSession('My session')
    expect(session.title).toBe('My session')
  })

  it('defaults isCustomTitle to false', () => {
    const session = createSession('Default')
    expect(session.isCustomTitle).toBe(false)
  })

  it('accepts isCustomTitle = true', () => {
    const session = createSession('Custom', true)
    expect(session.isCustomTitle).toBe(true)
  })

  it('sets messages to an empty array', () => {
    const session = createSession('Empty')
    expect(session.messages).toEqual([])
  })

  it('sets a non-empty id', () => {
    const session = createSession('Has ID')
    expect(typeof session.id).toBe('string')
    expect(session.id.length).toBeGreaterThan(0)
  })

  it('generates a unique id on each call', () => {
    const ids = new Set(Array.from({ length: 50 }, () => createSession('x').id))
    expect(ids.size).toBe(50)
  })

  it('sets createdAt to a valid ISO-8601 timestamp', () => {
    const before = Date.now()
    const session = createSession('Time check')
    const after = Date.now()

    const ts = new Date(session.createdAt).getTime()
    expect(ts).toBeGreaterThanOrEqual(before)
    expect(ts).toBeLessThanOrEqual(after)
  })

  it('initializes updatedAt to effectively the same time as createdAt', () => {
    const session = createSession('Timestamps')
    const createdAt = new Date(session.createdAt).getTime()
    const updatedAt = new Date(session.updatedAt).getTime()
    expect(updatedAt).toBeGreaterThanOrEqual(createdAt)
    expect(updatedAt - createdAt).toBeLessThan(1000)
  })

  it('returns an object with all required ChatSession fields', () => {
    const session = createSession('Shape')
    expect(session).toHaveProperty('id')
    expect(session).toHaveProperty('title')
    expect(session).toHaveProperty('messages')
    expect(session).toHaveProperty('createdAt')
    expect(session).toHaveProperty('updatedAt')
    expect(session).toHaveProperty('isCustomTitle')
  })
})

// ---------------------------------------------------------------------------
// loadSessionsFromStorage
// ---------------------------------------------------------------------------

describe('loadSessionsFromStorage()', () => {
  const STORAGE_KEY = 'test-sessions-key'

  // We maintain a fake localStorage object and stub window onto globalThis so
  // the SSR guard (`typeof window === 'undefined'`) evaluates correctly in
  // the Node test environment.

  let store: Record<string, string>
  let mockLocalStorage: Storage

  beforeEach(() => {
    store = {}

    mockLocalStorage = {
      getItem: vi.fn((key: string) => store[key] ?? null),
      setItem: vi.fn((key: string, value: string) => {
        store[key] = value
      }),
      removeItem: vi.fn((key: string) => {
        delete store[key]
      }),
      clear: vi.fn(() => {
        store = {}
      }),
      key: vi.fn(),
      length: 0,
    }

    vi.stubGlobal('window', { localStorage: mockLocalStorage })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.clearAllMocks()
  })

  describe('SSR guard (window undefined)', () => {
    it('returns a default session array when window is not defined', () => {
      vi.unstubAllGlobals() // remove window stub so typeof window === 'undefined'

      const result = loadSessionsFromStorage(STORAGE_KEY)

      expect(Array.isArray(result)).toBe(true)
      expect(result.length).toBe(1)
      expect(result[0].title).toBe('Conversation 1')
    })

    it('default session from SSR guard has correct shape', () => {
      vi.unstubAllGlobals()

      const [session] = loadSessionsFromStorage(STORAGE_KEY)

      expect(session).toHaveProperty('id')
      expect(session).toHaveProperty('messages')
      expect(session.messages).toEqual([])
      expect(session.isCustomTitle).toBe(false)
    })
  })

  describe('missing or empty storage entry', () => {
    it('returns a default session when the key does not exist in localStorage', () => {
      // store is empty, getItem returns null
      const result = loadSessionsFromStorage(STORAGE_KEY)

      expect(result.length).toBe(1)
      expect(result[0].title).toBe('Conversation 1')
    })

    it('returns a default session when the stored value is an empty array', () => {
      store[STORAGE_KEY] = JSON.stringify([])

      const result = loadSessionsFromStorage(STORAGE_KEY)

      expect(result.length).toBe(1)
      expect(result[0].title).toBe('Conversation 1')
    })
  })

  describe('malformed JSON', () => {
    it('returns a default session when the stored value is not valid JSON', () => {
      store[STORAGE_KEY] = 'not-valid-json{{{'

      const result = loadSessionsFromStorage(STORAGE_KEY)

      expect(result.length).toBe(1)
      expect(result[0].title).toBe('Conversation 1')
    })

    it('returns a default session when the stored value is a JSON primitive', () => {
      store[STORAGE_KEY] = '"just a string"'

      const result = loadSessionsFromStorage(STORAGE_KEY)

      expect(result.length).toBe(1)
      expect(result[0].title).toBe('Conversation 1')
    })

    it('returns a default session when the stored value is a JSON object (not array)', () => {
      store[STORAGE_KEY] = JSON.stringify({ id: 'foo' })

      const result = loadSessionsFromStorage(STORAGE_KEY)

      expect(result.length).toBe(1)
      expect(result[0].title).toBe('Conversation 1')
    })

    it('returns a default session when stored value is null literal', () => {
      store[STORAGE_KEY] = 'null'

      const result = loadSessionsFromStorage(STORAGE_KEY)

      expect(result.length).toBe(1)
      expect(result[0].title).toBe('Conversation 1')
    })
  })

  describe('valid stored data', () => {
    it('returns parsed sessions when the stored value is a non-empty array', () => {
      const sessions: ChatSession[] = [
        makeSession({ id: 'a', title: 'Alpha' }),
        makeSession({ id: 'b', title: 'Beta' }),
      ]
      store[STORAGE_KEY] = JSON.stringify(sessions)

      const result = loadSessionsFromStorage(STORAGE_KEY)

      expect(result.length).toBe(2)
      expect(result[0].id).toBe('a')
      expect(result[1].id).toBe('b')
    })

    it('preserves all session fields when loading from storage', () => {
      const session = makeSession({
        id: 'preserved',
        title: 'Preserved title',
        isCustomTitle: true,
        messages: [{ id: 'msg-x', role: 'user', content: 'Hello' }],
      })
      store[STORAGE_KEY] = JSON.stringify([session])

      const [loaded] = loadSessionsFromStorage(STORAGE_KEY)

      expect(loaded.id).toBe('preserved')
      expect(loaded.title).toBe('Preserved title')
      expect(loaded.isCustomTitle).toBe(true)
      expect(loaded.messages).toEqual([{ id: 'msg-x', role: 'user', content: 'Hello' }])
    })

    it('returns a single-element array correctly', () => {
      const sessions: ChatSession[] = [makeSession({ id: 'only', title: 'Only one' })]
      store[STORAGE_KEY] = JSON.stringify(sessions)

      const result = loadSessionsFromStorage(STORAGE_KEY)

      expect(result.length).toBe(1)
      expect(result[0].title).toBe('Only one')
    })

    it('uses the provided storage key to read from localStorage', () => {
      const keyA = 'key-a'
      const keyB = 'key-b'
      const sessionsA: ChatSession[] = [makeSession({ id: 'from-a', title: 'From A' })]
      store[keyA] = JSON.stringify(sessionsA)
      // keyB intentionally absent

      const resultA = loadSessionsFromStorage(keyA)
      const resultB = loadSessionsFromStorage(keyB)

      expect(resultA[0].title).toBe('From A')
      expect(resultB[0].title).toBe('Conversation 1') // default fallback
    })
  })
})

// ---------------------------------------------------------------------------
// generateMessageId
// ---------------------------------------------------------------------------

describe('generateMessageId()', () => {
  it('returns a non-empty string', () => {
    const id = generateMessageId()
    expect(typeof id).toBe('string')
    expect(id.length).toBeGreaterThan(0)
  })

  it('generates unique ids on every call', () => {
    const ids = new Set(Array.from({ length: 100 }, () => generateMessageId()))
    expect(ids.size).toBe(100)
  })

  it('does not return whitespace-only strings', () => {
    for (let i = 0; i < 20; i++) {
      expect(generateMessageId().trim().length).toBeGreaterThan(0)
    }
  })
})

// ---------------------------------------------------------------------------
// quickPrompts
// ---------------------------------------------------------------------------

describe('quickPrompts', () => {
  it('is a non-empty array', () => {
    expect(Array.isArray(quickPrompts)).toBe(true)
    expect(quickPrompts.length).toBeGreaterThan(0)
  })

  it('every entry has a non-empty label string', () => {
    for (const prompt of quickPrompts) {
      expect(typeof prompt.label).toBe('string')
      expect(prompt.label.trim().length).toBeGreaterThan(0)
    }
  })

  it('every entry has a non-empty text string', () => {
    for (const prompt of quickPrompts) {
      expect(typeof prompt.text).toBe('string')
      expect(prompt.text.trim().length).toBeGreaterThan(0)
    }
  })

  it('has no duplicate labels', () => {
    const labels = quickPrompts.map((p) => p.label)
    const uniqueLabels = new Set(labels)
    expect(uniqueLabels.size).toBe(labels.length)
  })

  it('has no duplicate text values', () => {
    const texts = quickPrompts.map((p) => p.text)
    const uniqueTexts = new Set(texts)
    expect(uniqueTexts.size).toBe(texts.length)
  })

  it('contains the expected prompt labels', () => {
    const labels = quickPrompts.map((p) => p.label)
    expect(labels).toContain('Spending summary')
    expect(labels).toContain('Budget status')
    expect(labels).toContain('Top expenses')
    expect(labels).toContain('Income trends')
  })

  it('contains the expected prompt texts', () => {
    const texts = quickPrompts.map((p) => p.text)
    expect(texts).toContain('Show me a summary of my spending this month')
    expect(texts).toContain('How am I doing on my budgets?')
    expect(texts).toContain('What are my biggest expenses?')
    expect(texts).toContain('How has my income changed over the past 3 months?')
  })

  it('each entry has exactly the keys label and text', () => {
    for (const prompt of quickPrompts) {
      const keys = Object.keys(prompt)
      expect(keys).toHaveLength(2)
      expect(keys).toContain('label')
      expect(keys).toContain('text')
    }
  })
})
