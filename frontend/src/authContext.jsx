import { createContext, useContext, useEffect, useState } from 'react'

const API_BASE = '/api'

const AuthContext = createContext(null)

/** WebView Android: токен в SharedPreferences, в localStorage пусто — читаем мост AndroidAuth (см. GamesScreen.kt). */
function readStoredOrAndroidToken() {
  if (typeof window === 'undefined') return ''
  try {
    const fromLs = localStorage.getItem('driveeup_token')
    if (fromLs) return fromLs
  } catch {
    return ''
  }
  try {
    const bridge = window.AndroidAuth
    if (bridge && typeof bridge.getToken === 'function') {
      const t = String(bridge.getToken() || '')
      if (t) {
        try {
          localStorage.setItem('driveeup_token', t)
        } catch {
          /* ignore */
        }
        return t
      }
    }
  } catch {
    /* не WebView */
  }
  return ''
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => readStoredOrAndroidToken())
  const [user, setUser] = useState(null)
  const [userLoading, setUserLoading] = useState(() => !!readStoredOrAndroidToken())
  const [theme, setTheme] = useState(() => localStorage.getItem('driveeup_theme') || 'light')

  useEffect(() => {
    document.body.className = theme === 'dark' ? 'theme-dark' : 'theme-light'
    localStorage.setItem('driveeup_theme', theme)
  }, [theme])

  useEffect(() => {
    let cancelled = false

    async function load() {
      if (!token) {
        setUser(null)
        setUserLoading(false)
        return
      }
      setUserLoading(true)
      try {
        const res = await fetch(`${API_BASE}/auth/me`, {
          headers: { Authorization: `Bearer ${token}` },
        })
        if (cancelled) return
        if (res.ok) {
          setUser(await res.json())
        } else {
          setUser(null)
        }
      } catch {
        if (!cancelled) setUser(null)
      } finally {
        if (!cancelled) setUserLoading(false)
      }
    }

    load()
    return () => {
      cancelled = true
    }
  }, [token])

  function saveToken(nextToken) {
    setToken(nextToken)
    if (nextToken) localStorage.setItem('driveeup_token', nextToken)
    else localStorage.removeItem('driveeup_token')
  }

  async function fetchMe() {
    if (!token) return
    setUserLoading(true)
    try {
      const res = await fetch(`${API_BASE}/auth/me`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (res.ok) {
        setUser(await res.json())
      }
    } catch {
      /* ignore */
    } finally {
      setUserLoading(false)
    }
  }

  const value = { token, user, userLoading, setUser, saveToken, theme, setTheme, fetchMe }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth: нужен AuthProvider')
  return ctx
}

export { API_BASE }
