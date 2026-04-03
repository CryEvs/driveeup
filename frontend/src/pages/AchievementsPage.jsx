import { useEffect, useState } from 'react'
import { API_BASE } from '../authContext.jsx'

/** Абсолютный URL иконки: относительные пути, подмена localhost в ссылках с бэкенда. */
function achievementIconSrc(iconUrl) {
  if (!iconUrl || typeof iconUrl !== 'string') return null
  const s = iconUrl.trim()
  if (!s || s === 'null') return null
  if (typeof window !== 'undefined' && s.startsWith('/')) {
    return window.location.origin + s
  }
  if (/^https?:\/\//i.test(s)) {
    try {
      const u = new URL(s)
      const bad = u.hostname === 'localhost' || u.hostname === '127.0.0.1'
      if (bad && (u.pathname.startsWith('/api/') || u.pathname.startsWith('/storage/'))) {
        return window.location.origin + u.pathname + u.search + u.hash
      }
    } catch {
      /* ignore */
    }
    return s
  }
  return s
}

export function AchievementsPage({ token }) {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setError('')
      try {
        const res = await fetch(`${API_BASE}/achievements`, {
          headers: { Authorization: `Bearer ${token}` },
        })
        const json = await res.json()
        if (!res.ok) throw new Error(json.error || 'Ошибка загрузки')
        if (!cancelled) setItems(Array.isArray(json) ? json : [])
      } catch (e) {
        if (!cancelled) setError(e.message || 'Ошибка')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    if (token) load()
    return () => {
      cancelled = true
    }
  }, [token])

  return (
    <section className="content-card">
      <h1>Достижения</h1>
      {loading && <p>Загрузка...</p>}
      {error && <p className="error">{error}</p>}
      {!loading && !error && items.length === 0 && <p>Пока нет достижений.</p>}
      <div className="achievements-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 14, marginTop: 16 }}>
        {items.map((a) => {
          const iconSrc = achievementIconSrc(a.iconUrl)
          return (
            <article
              key={a.id}
              style={{
                border: '1px solid #96ea28',
                borderRadius: 16,
                padding: 14,
                minHeight: 220,
                background: 'linear-gradient(180deg, #fff 0%, #f7fbf2 100%)',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                textAlign: 'center',
              }}
            >
              {iconSrc ? (
                <img src={iconSrc} alt="" style={{ width: 52, height: 52, objectFit: 'contain', marginBottom: 8 }} />
              ) : (
                <div style={{ height: 52 }} />
              )}
              <h3 style={{ margin: '8px 0 6px', fontSize: '1.05rem' }}>{a.title}</h3>
              <p style={{ margin: 0, color: '#555', fontSize: 13, lineHeight: 1.4 }}>{a.description}</p>
            </article>
          )
        })}
      </div>
    </section>
  )
}
