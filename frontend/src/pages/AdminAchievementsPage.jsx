import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { API_BASE } from '../authContext.jsx'

const initialForm = {
  title: '',
  description: '',
  awardType: 'INITIAL',
  ridesRequired: 1,
  iconUrl: '',
  sortOrder: 0,
  isActive: true,
}

function awardTypeLabel(type) {
  switch (type) {
    case 'RIDES':
      return 'Проехать количество раз'
    case 'EVERYONE':
      return 'Получено у всех'
    case 'INITIAL':
    default:
      return 'Начальное достижение'
  }
}

export function AdminAchievementsPage({ token, user }) {
  const isAdmin = !!user?.isAdmin
  const [items, setItems] = useState([])
  const [form, setForm] = useState(initialForm)
  const [editingId, setEditingId] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [uploadingIcon, setUploadingIcon] = useState(false)

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
      const data = await api('/admin/achievements')
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
        title: form.title,
        description: form.description,
        awardType: form.awardType,
        ridesRequired: form.awardType === 'RIDES' ? Number(form.ridesRequired) : null,
        iconUrl: form.iconUrl,
        sortOrder: Number(form.sortOrder),
        isActive: !!form.isActive,
      }
      if (editingId) await api(`/admin/achievements/${editingId}`, 'PUT', payload)
      else await api('/admin/achievements', 'POST', payload)
      setForm(initialForm)
      setEditingId(null)
      await load()
    } catch (e2) {
      setError(e2.message || 'Ошибка сохранения')
    }
  }

  async function uploadIcon(file) {
    if (!file) return
    setUploadingIcon(true)
    setError('')
    try {
      const fd = new FormData()
      fd.append('icon', file)
      const res = await fetch(`${API_BASE}/admin/achievements/icon`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: fd,
      })
      const json = await res.json().catch(() => ({}))
      if (!res.ok) throw new Error(json.error || 'Не удалось загрузить иконку')
      setForm((s) => ({ ...s, iconUrl: json.iconUrl || '' }))
    } catch (e) {
      setError(e.message || 'Ошибка загрузки')
    } finally {
      setUploadingIcon(false)
    }
  }

  return (
    <section className="content-card">
      <h1>Админ: Достижения</h1>
      {error && <p className="error">{error}</p>}
      {loading ? <p>Загрузка...</p> : null}

      <form onSubmit={submit} className="bp-admin-col">
        <h3>{editingId ? 'Редактировать достижение' : 'Добавить достижение'}</h3>
        <label className="bp-field-label">Название</label>
        <input value={form.title} onChange={(e) => setForm((s) => ({ ...s, title: e.target.value }))} required />
        <label className="bp-field-label">Описание</label>
        <textarea value={form.description} onChange={(e) => setForm((s) => ({ ...s, description: e.target.value }))} />
        <label className="bp-field-label">Причина выдачи</label>
        <select
          value={form.awardType}
          onChange={(e) => setForm((s) => ({ ...s, awardType: e.target.value }))}
        >
          <option value="RIDES">Проехать количество раз</option>
          <option value="INITIAL">Начальное достижение</option>
          <option value="EVERYONE">Получено у всех</option>
        </select>
        {form.awardType === 'RIDES' ? (
          <>
            <label className="bp-field-label">Сколько раз проехать</label>
            <input
              type="number"
              min="1"
              value={form.ridesRequired}
              onChange={(e) => setForm((s) => ({ ...s, ridesRequired: e.target.value }))}
              required
            />
          </>
        ) : null}
        <label className="bp-field-label">Иконка (файл)</label>
        <input type="file" accept="image/*" onChange={(e) => uploadIcon(e.target.files?.[0])} />
        {uploadingIcon && <p>Загрузка иконки...</p>}
        <label className="bp-field-label">URL иконки</label>
        <input value={form.iconUrl} onChange={(e) => setForm((s) => ({ ...s, iconUrl: e.target.value }))} placeholder="Подставится после загрузки" />
        <label className="bp-field-label">Порядок сортировки</label>
        <input type="number" min="0" value={form.sortOrder} onChange={(e) => setForm((s) => ({ ...s, sortOrder: e.target.value }))} />
        <label className="bp-field-label">
          <input type="checkbox" checked={!!form.isActive} onChange={(e) => setForm((s) => ({ ...s, isActive: e.target.checked }))} /> Активно (показывать в приложении)
        </label>
        <button type="submit">{editingId ? 'Сохранить' : 'Добавить'}</button>
      </form>

      <div className="bp-admin" style={{ marginTop: 18 }}>
        {items.map((row) => (
          <div key={row.id} className="bp-admin-season">
            <h3>{row.title}</h3>
            <p>{row.description || '—'}</p>
            {row.icon_url ? <img src={row.icon_url} alt="" style={{ maxWidth: 80, maxHeight: 80 }} /> : null}
            <p>
              Причина:{' '}
              {row.award_type === 'RIDES' && row.rides_required
                ? `Проехать ${row.rides_required} раз`
                : awardTypeLabel(row.award_type)}
            </p>
            <p>
              Порядок: {row.sort_order} · Активно: {row.is_active ? 'да' : 'нет'}
            </p>
            <button
              type="button"
              onClick={() => {
                setEditingId(row.id)
                setForm({
                  title: row.title || '',
                  description: row.description || '',
                  awardType: row.award_type || 'INITIAL',
                  ridesRequired: row.rides_required ?? 1,
                  iconUrl: row.icon_url || '',
                  sortOrder: row.sort_order ?? 0,
                  isActive: !!row.is_active,
                })
              }}
            >
              Редактировать
            </button>{' '}
            <button type="button" onClick={() => api(`/admin/achievements/${row.id}`, 'DELETE').then(load)}>
              Удалить
            </button>
          </div>
        ))}
      </div>
    </section>
  )
}
