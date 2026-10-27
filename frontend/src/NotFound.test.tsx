import { render, screen } from '@testing-library/react'
import { afterEach, expect, test } from 'vitest'
import App from './App.tsx'

afterEach(() => {
  window.history.replaceState(null, '', '/')
})

test('a path that is not a Short Link shows the not-found page with a way back', () => {
  window.history.replaceState(null, '', '/987654321')

  render(<App />)

  expect(screen.getByRole('heading', { name: '링크를 찾을 수 없습니다' })).toBeInTheDocument()
  expect(screen.getByRole('link', { name: '새 링크 만들기' })).toHaveAttribute('href', '/')
  expect(screen.queryByLabelText('줄일 URL')).not.toBeInTheDocument()
})

test('the root shows the main page', () => {
  render(<App />)

  expect(screen.getByLabelText('줄일 URL')).toBeInTheDocument()
  expect(screen.queryByRole('heading', { name: '링크를 찾을 수 없습니다' })).not.toBeInTheDocument()
})
