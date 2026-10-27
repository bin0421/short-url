import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, expect, test, vi } from 'vitest'
import App from './App.tsx'

afterEach(() => {
  vi.unstubAllGlobals()
})

test('submitting a Target URL shows the created Short Link', async () => {
  const fetchMock = vi.fn().mockResolvedValue(
    new Response(
      JSON.stringify({
        code: 42,
        shortUrl: 'https://sho.rt/42',
        targetUrl: 'https://example.com/docs',
      }),
      { status: 201, headers: { 'Content-Type': 'application/json' } },
    ),
  )
  vi.stubGlobal('fetch', fetchMock)
  const user = userEvent.setup()

  render(<App />)
  await user.type(screen.getByLabelText('줄일 URL'), 'https://example.com/docs')
  await user.click(screen.getByRole('button', { name: '줄이기' }))

  expect(await screen.findByRole('link', { name: 'https://sho.rt/42' })).toHaveAttribute(
    'href',
    'https://sho.rt/42',
  )
  expect(screen.getByText('https://example.com/docs')).toBeInTheDocument()
  expect(fetchMock).toHaveBeenCalledWith(
    '/api/links',
    expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ targetUrl: 'https://example.com/docs' }),
    }),
  )
})

function shortLinkResponse(code: number, targetUrl: string, status = 201) {
  return new Response(
    JSON.stringify({ code, shortUrl: `https://sho.rt/${code}`, targetUrl }),
    { status, headers: { 'Content-Type': 'application/json' } },
  )
}

test('hitting the rate limit (429) asks the visitor to try again shortly', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          title: 'Too Many Requests',
          status: 429,
          detail: '요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.',
        }),
        { status: 429, headers: { 'Content-Type': 'application/problem+json' } },
      ),
    ),
  )
  const user = userEvent.setup()

  render(<App />)
  await user.type(screen.getByLabelText('줄일 URL'), 'https://example.com/docs')
  await user.click(screen.getByRole('button', { name: '줄이기' }))

  expect(await screen.findByRole('alert')).toHaveTextContent('잠시 후 다시 시도해 주세요.')
})

test('an existing Short Link (200) is shown the same way as a new one', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(shortLinkResponse(7, 'https://example.com/old', 200)))
  const user = userEvent.setup()

  render(<App />)
  await user.type(screen.getByLabelText('줄일 URL'), 'https://example.com/old')
  await user.click(screen.getByRole('button', { name: '줄이기' }))

  expect(await screen.findByRole('link', { name: 'https://sho.rt/7' })).toBeInTheDocument()
  expect(screen.getByText('https://example.com/old')).toBeInTheDocument()
})

test('the copy button puts the Short Link on the clipboard and confirms it', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(shortLinkResponse(42, 'https://example.com/docs')))
  const user = userEvent.setup()

  render(<App />)
  await user.type(screen.getByLabelText('줄일 URL'), 'https://example.com/docs')
  await user.click(screen.getByRole('button', { name: '줄이기' }))
  await user.click(await screen.findByRole('button', { name: '복사' }))

  expect(await screen.findByRole('button', { name: '복사됨' })).toBeInTheDocument()
  expect(await navigator.clipboard.readText()).toBe('https://sho.rt/42')
})

test('the submit button is disabled while the request is in flight', async () => {
  let respond!: (response: Response) => void
  vi.stubGlobal(
    'fetch',
    vi.fn().mockReturnValue(new Promise<Response>((resolve) => (respond = resolve))),
  )
  const user = userEvent.setup()

  render(<App />)
  await user.type(screen.getByLabelText('줄일 URL'), 'https://example.com/docs')
  await user.click(screen.getByRole('button', { name: '줄이기' }))

  expect(screen.getByRole('button', { name: '줄이기' })).toBeDisabled()

  respond(shortLinkResponse(42, 'https://example.com/docs'))

  expect(await screen.findByRole('link', { name: 'https://sho.rt/42' })).toBeInTheDocument()
  expect(screen.getByRole('button', { name: '줄이기' })).toBeEnabled()
})

test('pressing Enter in the input submits the Target URL', async () => {
  const fetchMock = vi.fn().mockResolvedValue(shortLinkResponse(42, 'https://example.com/docs'))
  vi.stubGlobal('fetch', fetchMock)
  const user = userEvent.setup()

  render(<App />)
  await user.type(screen.getByLabelText('줄일 URL'), 'https://example.com/docs{Enter}')

  expect(await screen.findByRole('link', { name: 'https://sho.rt/42' })).toBeInTheDocument()
  expect(fetchMock).toHaveBeenCalledTimes(1)
})

test('submitting another Target URL replaces the result with the new Short Link', async () => {
  vi.stubGlobal(
    'fetch',
    vi
      .fn()
      .mockResolvedValueOnce(shortLinkResponse(42, 'https://example.com/docs'))
      .mockResolvedValueOnce(shortLinkResponse(43, 'https://example.com/blog')),
  )
  const user = userEvent.setup()

  render(<App />)
  const input = screen.getByLabelText('줄일 URL')
  await user.type(input, 'https://example.com/docs')
  await user.click(screen.getByRole('button', { name: '줄이기' }))
  await user.click(await screen.findByRole('button', { name: '복사' }))
  await screen.findByRole('button', { name: '복사됨' })

  await user.clear(input)
  await user.type(input, 'https://example.com/blog')
  await user.click(screen.getByRole('button', { name: '줄이기' }))

  expect(await screen.findByRole('link', { name: 'https://sho.rt/43' })).toBeInTheDocument()
  expect(screen.getByText('https://example.com/blog')).toBeInTheDocument()
  expect(screen.queryByRole('link', { name: 'https://sho.rt/42' })).not.toBeInTheDocument()
  expect(screen.getByRole('button', { name: '복사' })).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: '복사됨' })).not.toBeInTheDocument()
})

test('a rejected Target URL shows the reason and keeps the input', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          type: 'about:blank',
          title: 'Bad Request',
          status: 400,
          detail: 'http:// 또는 https://로 시작하는 주소만 줄일 수 있습니다.',
        }),
        { status: 400, headers: { 'Content-Type': 'application/problem+json' } },
      ),
    ),
  )
  const user = userEvent.setup()

  render(<App />)
  await user.type(screen.getByLabelText('줄일 URL'), 'ftp://example.com')
  await user.click(screen.getByRole('button', { name: '줄이기' }))

  expect(await screen.findByRole('alert')).toHaveTextContent(
    'http:// 또는 https://로 시작하는 주소만 줄일 수 있습니다.',
  )
  expect(screen.getByLabelText('줄일 URL')).toHaveValue('ftp://example.com')
  expect(screen.queryByRole('link')).not.toBeInTheDocument()
})
