import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { API_BASE } from '../authContext.jsx'

const initialForm = {
  name: '',
  shortDescription: '',
  allowedTier: 'ANY',
  itemType: 'DISCOUNT',
  discountPercent: 10,
  description: '',
  usageTerms: '',
  validityText: '',
  priceDriveCoin: 0,
  sortOrder: 0,
  isActive: true,
}

function tierPreviewColor(allowedTier) {
  if (allowedTier === 'GOLD') return '#F24B16'
  if (allowedTier === 'SILVER') return '#171918'
  return '#97EA28'
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
        discountPercent: form.itemType === 'DISCOUNT' ? Number(form.discountPercent) : null,
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
        <label className="bp-field-label">Краткое описание</label>
        <textarea value={form.shortDescription} onChange={(e) => setForm((s) => ({ ...s, shortDescription: e.target.value }))} />
        <label className="bp-field-label">Для какого статуса доступно</label>
        <select value={form.allowedTier} onChange={(e) => setForm((s) => ({ ...s, allowedTier: e.target.value }))}>
          <option value="ANY">Для любого уровня</option>
          <option value="SILVER">Серебряный и выше</option>
          <option value="GOLD">Только золотой</option>
        </select>
        <label className="bp-field-label">Тип товара</label>
        <select value={form.itemType} onChange={(e) => setForm((s) => ({ ...s, itemType: e.target.value }))}>
          <option value="DISCOUNT">Скидка</option>
        </select>
        <label className="bp-field-label">Процент скидки</label>
        <input
          type="number"
          min="1"
          max="100"
          value={form.discountPercent}
          onChange={(e) => setForm((s) => ({ ...s, discountPercent: e.target.value }))}
          required
        />
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
            <div
              className="admin-store-tier-preview"
              style={{ backgroundColor: tierPreviewColor(it.allowed_tier || 'ANY') }}
              aria-label={`Превью ранга ${it.allowed_tier || 'ANY'}`}
            >
              <img src="/driveup_design_arrows.png" alt="" aria-hidden="true" />
            </div>
            <p>Tier: {it.allowed_tier} | Тип: {it.item_type} ({it.discount_percent || 0}%) | Цена: {it.price_drive_coin} | Active: {String(it.is_active)}</p>
            <button
              type="button"
              onClick={() => {
                setEditingId(it.id)
                setForm({
                  name: it.name || '',
                  shortDescription: it.short_description || '',
                  allowedTier: it.allowed_tier || 'ANY',
                  itemType: it.item_type || 'DISCOUNT',
                  discountPercent: it.discount_percent || 10,
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

