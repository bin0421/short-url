import { useState, type FormEvent } from 'react'
import NotFound from './NotFound.tsx'

type ShortLink = {
  code: number
  shortUrl: string
  targetUrl: string
}

const FALLBACK_ERROR = '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.'

// Error responses are RFC 9457 problem details; `detail` holds the human-readable reason.
async function problemDetail(response: Response): Promise<string> {
  try {
    const body = await response.json()
    return typeof body.detail === 'string' ? body.detail : FALLBACK_ERROR
  } catch {
    return FALLBACK_ERROR
  }
}

function MainPage() {
  const [targetUrl, setTargetUrl] = useState('')
  const [shortLink, setShortLink] = useState<ShortLink | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  async function handleCopy(shortUrl: string) {
    await navigator.clipboard.writeText(shortUrl)
    setCopied(true)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    try {
      const response = await fetch('/api/links', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ targetUrl }),
      })
      if (response.ok) {
        setShortLink(await response.json())
        setCopied(false)
        setError(null)
      } else {
        setShortLink(null)
        setError(await problemDetail(response))
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main>
      <h1>short-url</h1>
      <form onSubmit={handleSubmit}>
        <label htmlFor="target-url">줄일 URL</label>
        <input
          id="target-url"
          type="text"
          value={targetUrl}
          onChange={(event) => setTargetUrl(event.target.value)}
          placeholder="https://example.com/very/long/path"
        />
        <button type="submit" disabled={submitting}>
          줄이기
        </button>
      </form>
      {error && <p role="alert">{error}</p>}
      {shortLink && (
        <section aria-label="결과">
          <p className="short-link">
            <a href={shortLink.shortUrl}>{shortLink.shortUrl}</a>
            <button type="button" onClick={() => handleCopy(shortLink.shortUrl)}>
              {copied ? '복사됨' : '복사'}
            </button>
          </p>
          <p>{shortLink.targetUrl}</p>
        </section>
      )}
    </main>
  )
}

// The backend serves this page with 404 for any Code it can't resolve, so every other path is "not found".
function App() {
  return window.location.pathname === '/' ? <MainPage /> : <NotFound />
}

export default App
