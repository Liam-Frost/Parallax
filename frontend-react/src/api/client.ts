const API_BASE = import.meta.env.VITE_API_BASE ?? 'https://api.example.com/api'

let accessToken: string | null = null

export function setAccessToken(token: string | null) {
  accessToken = token
}

export function getAccessToken() {
  return accessToken
}

function getCookie(name: string) {
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : null
}

async function request(path: string, options: RequestInit = {}) {
  const headers = new Headers(options.headers)
  if (!headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }

  const csrf = getCookie('XSRF-TOKEN')
  if (csrf && options.method && options.method !== 'GET') {
    headers.set('X-CSRF-Token', csrf)
  }

  return fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
    credentials: 'include',
  })
}

export async function apiGet<T>(path: string): Promise<T> {
  const response = await request(path, { method: 'GET' })
  if (!response.ok) throw new Error(`Request failed: ${response.status}`)
  return response.json()
}

export async function apiPost<T>(path: string, body?: unknown): Promise<T> {
  const response = await request(path, {
    method: 'POST',
    body: body ? JSON.stringify(body) : undefined,
  })
  if (!response.ok) throw new Error(`Request failed: ${response.status}`)
  return response.json()
}

export async function apiDelete<T>(path: string, body?: unknown): Promise<T> {
  const response = await request(path, {
    method: 'DELETE',
    body: body ? JSON.stringify(body) : undefined,
  })
  if (!response.ok) throw new Error(`Request failed: ${response.status}`)
  return response.json()
}
