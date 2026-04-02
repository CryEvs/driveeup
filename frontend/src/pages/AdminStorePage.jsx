import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { API_BASE } from '../authContext.jsx'

const initialForm = {
  name: '',
  iconUrl: '',
  shortDescription: '',
  allowedTier: 'ANY',
  description: '',
  usageTerms: '',
  validityText: '',
  priceDriveCoin: 0,
  sortOrder: 0,
  isActive: true,
}

export function AdminStorePage({ token, user }) {
  const isAdmin = !!user?.isAdmin
  const [items, setItems] = useState([])
  const [form, setForm] = useState(initialForm)
  const [editingId, setEditingId] = useState(null)
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
      const data = await api('/admin/store/items')
      setItems(Array.isArray(data) ? data : [])
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

  async function submit(e) {
    e.preventDefault()
    try {
      const payload = {
        ...form,
        priceDriveCoin: Number(form.priceDriveCoin),
        sortOrder: Number(form.sortOrder),
        isActive: !!form.isActive,
      }
      if (editingId) await api(`/admin/store/items/${editingId}`, 'PUT', payload)
      else await api('/admin/store/items', 'POST', payload)
      setForm(initialForm)
      setEditingId(null)
      await load()
    } catch (e2) {
      setError(e2.message || 'Ошибка сохранения товара')
    }
  }

  return (
    <section className="content-card">
      <h1>Админ: Товары за DriveCoin</h1>
      {error && <p className="error">{error}</p>}
      {loading ? <p>Загрузка...</p> : null}

      <form onSubmit={submit} className="bp-admin-col">
        <label className="bp-field-label">Название</label>
        <input value={form.name} onChange={(e) => setForm((s) => ({ ...s, name: e.target.value }))} required />
        <label className="bp-field-label">Иконка (URL)</label>
        <input value={form.iconUrl} onChange={(e) => setForm((s) => ({ ...s, iconUrl: e.target.value }))} />
        <label className="bp-field-label">Краткое описание</label>
        <textarea value={form.shortDescription} onChange={(e) => setForm((s) => ({ ...s, shortDescription: e.target.value }))} />
        <label className="bp-field-label">Для какого статуса доступно</label>
        <select value={form.allowedTier} onChange={(e) => setForm((s) => ({ ...s, allowedTier: e.target.value }))}>
          <option value="ANY">Для любого уровня</option>
          <option value="SILVER">Серебряный и выше</option>
          <option value="GOLD">Только золотой</option>
        </select>
        <label className="bp-field-label">Описание</label>
        <textarea value={form.description} onChange={(e) => setForm((s) => ({ ...s, description: e.target.value }))} />
        <label className="bp-field-label">Условия пользования</label>
        <textarea value={form.usageTerms} onChange={(e) => setForm((s) => ({ ...s, usageTerms: e.target.value }))} />
        <label className="bp-field-label">Срок действия</label>
        <textarea value={form.validityText} onChange={(e) => setForm((s) => ({ ...s, validityText: e.target.value }))} />
        <label className="bp-field-label">Цена (DriveCoin)</label>
        <input type="number" min="0" value={form.priceDriveCoin} onChange={(e) => setForm((s) => ({ ...s, priceDriveCoin: e.target.value }))} required />
        <label className="bp-field-label">Порядок сортировки</label>
        <input type="number" min="0" value={form.sortOrder} onChange={(e) => setForm((s) => ({ ...s, sortOrder: e.target.value }))} />
        <label className="bp-field-label">
          <input type="checkbox" checked={!!form.isActive} onChange={(e) => setForm((s) => ({ ...s, isActive: e.target.checked }))} /> Активный товар
        </label>
        <button type="submit">{editingId ? 'Сохранить товар' : 'Добавить товар'}</button>
      </form>

      <div className="bp-admin" style={{ marginTop: 14 }}>
        {items.map((it) => (
          <div key={it.id} className="bp-admin-season">
            <h3>{it.name}</h3>
            <p>Tier: {it.allowed_tier} | Цена: {it.price_drive_coin} | Active: {String(it.is_active)}</p>
            <button
              type="button"
              onClick={() => {
                setEditingId(it.id)
                setForm({
                  name: it.name || '',
                  iconUrl: it.icon_url || '',
                  shortDescription: it.short_description || '',
                  allowedTier: it.allowed_tier || 'ANY',
                  description: it.description || '',
                  usageTerms: it.usage_terms || '',
                  validityText: it.validity_text || '',
                  priceDriveCoin: it.price_drive_coin || 0,
                  sortOrder: it.sort_order || 0,
                  isActive: !!it.is_active,
                })
              }}
            >
              Редактировать
            </button>
            <button type="button" onClick={() => api(`/admin/store/items/${it.id}`, 'DELETE').then(load)}>Удалить</button>
          </div>
        ))}
      </div>
    </section>
  )
}

