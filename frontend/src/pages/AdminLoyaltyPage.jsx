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
  const [descriptions, setDescriptions] = useState({
    BRONZE: 'Бронзовый уровень: стартовые привилегии и базовые преимущества.',
    SILVER: 'Серебряный уровень: больше бонусов и улучшенные условия.',
    GOLD: 'Золотой уровень: максимальные привилегии и лучшие условия сервиса.',
  })
  const [ridesThresholds, setRidesThresholds] = useState({
    SILVER: 15,
    GOLD: 50,
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
      const descriptionsMap = { ...descriptions }
      const thresholdMap = { ...ridesThresholds }
      ;(rows || []).forEach((r) => {
        if (r?.tier) map[r.tier] = r.benefit_text || ''
        if (r?.tier) descriptionsMap[r.tier] = r.level_description || descriptionsMap[r.tier] || ''
        if (r?.tier === 'SILVER' || r?.tier === 'GOLD') {
          thresholdMap[r.tier] = Number(r.rides_required_total || thresholdMap[r.tier] || 0)
        }
      })
      setBenefits(map)
      setDescriptions(descriptionsMap)
      setRidesThresholds(thresholdMap)
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
        levelDescription: descriptions[tier] || '',
        ridesRequiredTotal: tier === 'SILVER' || tier === 'GOLD' ? Number(ridesThresholds[tier] || 0) : null,
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
        Редактирование преимуществ и описаний уровней по статусу лояльности.
      </p>
      {error && <p className="error">{error}</p>}
      {loading ? <p>Загрузка...</p> : null}

      <div className="bp-admin" style={{ marginTop: 14 }}>
        {TIERS.map((tier) => (
          <div key={tier} className="bp-admin-season">
            <h3>{tier}</h3>
            <label className="bp-field-label">Описание уровня (вкладка уровня лояльности)</label>
            <textarea
              value={descriptions[tier] || ''}
              onChange={(e) => setDescriptions((s) => ({ ...s, [tier]: e.target.value }))}
            />
            {tier === 'SILVER' || tier === 'GOLD' ? (
              <>
                <label className="bp-field-label">Поездок для получения уровня</label>
                <input
                  type="number"
                  min="1"
                  value={ridesThresholds[tier] || 0}
                  onChange={(e) => setRidesThresholds((s) => ({ ...s, [tier]: e.target.value }))}
                />
              </>
            ) : null}
            <label className="bp-field-label">Что применится к следующей поездке</label>
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

