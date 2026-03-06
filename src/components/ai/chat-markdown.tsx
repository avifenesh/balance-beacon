'use client'

import type { Components } from 'react-markdown'
import ReactMarkdown from 'react-markdown'

const markdownComponents: Components = {
  h1: ({ children }) => <h1 className="mb-2 mt-3 text-base font-semibold text-white first:mt-0">{children}</h1>,
  h2: ({ children }) => <h2 className="mb-2 mt-3 text-sm font-semibold text-white first:mt-0">{children}</h2>,
  h3: ({ children }) => <h3 className="mb-1 mt-2 text-sm font-semibold text-white first:mt-0">{children}</h3>,
  p: ({ children }) => <p className="mb-2 last:mb-0 text-slate-200">{children}</p>,
  strong: ({ children }) => <strong className="font-semibold text-white">{children}</strong>,
  em: ({ children }) => <em className="italic text-slate-300">{children}</em>,
  ul: ({ children }) => <ul className="mb-2 list-disc space-y-1 pl-4 text-slate-200 last:mb-0">{children}</ul>,
  ol: ({ children }) => <ol className="mb-2 list-decimal space-y-1 pl-4 text-slate-200 last:mb-0">{children}</ol>,
  li: ({ children }) => <li className="text-slate-200">{children}</li>,
  a: ({ href, children }) => (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      className="text-sky-400 underline decoration-sky-400/30 hover:text-sky-300 hover:decoration-sky-300/50"
    >
      {children}
    </a>
  ),
  code: ({ className, children }) => {
    const isBlock = className?.includes('language-')
    if (isBlock) {
      return (
        <code className="block overflow-x-auto rounded-lg bg-white/10 p-3 font-mono text-xs text-slate-200">
          {children}
        </code>
      )
    }
    return <code className="rounded bg-white/10 px-1.5 py-0.5 font-mono text-xs text-slate-200">{children}</code>
  },
  pre: ({ children }) => <pre className="mb-2 last:mb-0">{children}</pre>,
  blockquote: ({ children }) => (
    <blockquote className="mb-2 border-l-2 border-sky-400/40 pl-3 text-slate-300 last:mb-0">{children}</blockquote>
  ),
}

export function ChatMarkdown({ content }: { content: string }) {
  return <ReactMarkdown components={markdownComponents}>{content}</ReactMarkdown>
}
