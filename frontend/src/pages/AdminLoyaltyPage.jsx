import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { API_BASE } from '../authContext.jsx'

const TIERS = ['BRONZE', 'SILVER', 'GOLD']

export function AdminLoyaltyPage({ token, user }) {
  const isAdmin = !!user?.isAdmin
  const [benefits, setBenefits] = useState({
    BRONZE: 'Базовые бонусы программы лояльности DriveUP',
    SILVER: 'Расширенные бонусы и приоритет в программе',
    GOLD: 'Максимальные привилегии и приоритет DriveUP',
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  if (!isAdmin) return <Navigate to="/battle-pass" replace />

  async function api(path, method = 'GET', body = null) {
    const res = await fetch(`${API_BASE}${path}`, {
      method,
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: body ? JSON.stringify(body) : undefined,
    })
    const json = await res.json().catch(() => ({}))
    if (!res.ok) throw new Error(json.error || 'Ошибка API')
    return json
  }

  async function load() {
    setLoading(true)
    setError('')
    try {
      const rows = await api('/admin/loyalty/next-ride-benefits')
      const map = { ...benefits }
      ;(rows || []).forEach((r) => {
        if (r?.tier) map[r.tier] = r.benefit_text || ''
      })
      setBenefits(map)
    } catch (e) {
      setError(e.message || 'Ошибка загрузки')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function saveTier(tier) {
    try {
      await api('/admin/loyalty/next-ride-benefits', 'PUT', {
        tier,
        benefitText: benefits[tier] || '',
      })
      await load()
    } catch (e) {
      setError(e.message || 'Ошибка сохранения')
    }
  }

  return (
    <section className="content-card">
      <h1>Админ: Что применится к следующей поездке</h1>
      <p style={{ marginTop: 6, color: '#6c6c6c' }}>
        Редактирование преимуществ по статусу лояльности.
      </p>
      {error && <p className="error">{error}</p>}
      {loading ? <p>Загрузка...</p> : null}

      <div className="bp-admin" style={{ marginTop: 14 }}>
        {TIERS.map((tier) => (
          <div key={tier} className="bp-admin-season">
            <h3>{tier}</h3>
            <textarea
              value={benefits[tier] || ''}
              onChange={(e) => setBenefits((s) => ({ ...s, [tier]: e.target.value }))}
            />
            <button type="button" onClick={() => saveTier(tier)}>Сохранить</button>
          </div>
        ))}
      </div>
    </section>
  )
}

