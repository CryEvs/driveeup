import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { API_BASE } from '../authContext.jsx'

const initialTaskForm = {
  title: '',
  description: '',
  completionType: 'RIDES',
  requiredRidesCount: 1,
  rewardDriveCoin: 0,
  sortOrder: 0,
  isActive: true,
}

export function AdminTasksPage({ token, user }) {
  const isAdmin = !!user?.isAdmin
  const [tasks, setTasks] = useState([])
  const [form, setForm] = useState(initialTaskForm)
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
      const data = await api('/admin/tasks')
      setTasks(Array.isArray(data) ? data : [])
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
        requiredRidesCount: form.completionType === 'RIDES' ? Number(form.requiredRidesCount) : null,
        rewardDriveCoin: Number(form.rewardDriveCoin),
        sortOrder: Number(form.sortOrder),
        isActive: !!form.isActive,
      }
      if (editingId) await api(`/admin/tasks/${editingId}`, 'PUT', payload)
      else await api('/admin/tasks', 'POST', payload)
      setForm(initialTaskForm)
      setEditingId(null)
      await load()
    } catch (e2) {
      setError(e2.message || 'Ошибка сохранения задания')
    }
  }

  return (
    <section className="content-card">
      <h1>Админ: Задания</h1>
      {error && <p className="error">{error}</p>}
      {loading ? <p>Загрузка...</p> : null}

      <form onSubmit={submit} className="bp-admin-col">
        <label className="bp-field-label">Название</label>
        <input value={form.title} onChange={(e) => setForm((s) => ({ ...s, title: e.target.value }))} required />
        <label className="bp-field-label">Описание</label>
        <textarea value={form.description} onChange={(e) => setForm((s) => ({ ...s, description: e.target.value }))} />
        <label className="bp-field-label">Тип выполнения</label>
        <select value={form.completionType} onChange={(e) => setForm((s) => ({ ...s, completionType: e.target.value }))}>
          <option value="RIDES">За поездки</option>
          <option value="RATING">За полученную оценку рейтинга</option>
          <option value="REFERRAL">За приглашение друга по реферальной ссылке</option>
        </select>
        {form.completionType === 'RIDES' ? (
          <>
            <label className="bp-field-label">За какое количество поездок</label>
            <input
              type="number"
              min="1"
              value={form.requiredRidesCount}
              onChange={(e) => setForm((s) => ({ ...s, requiredRidesCount: e.target.value }))}
              required
            />
          </>
        ) : null}
        <label className="bp-field-label">Награда (DriveCoin)</label>
        <input type="number" min="0" value={form.rewardDriveCoin} onChange={(e) => setForm((s) => ({ ...s, rewardDriveCoin: e.target.value }))} required />
        <label className="bp-field-label">Порядок сортировки</label>
        <input type="number" min="0" value={form.sortOrder} onChange={(e) => setForm((s) => ({ ...s, sortOrder: e.target.value }))} />
        <label className="bp-field-label">
          <input type="checkbox" checked={!!form.isActive} onChange={(e) => setForm((s) => ({ ...s, isActive: e.target.checked }))} /> Активное задание
        </label>
        <button type="submit">{editingId ? 'Сохранить задание' : 'Добавить задание'}</button>
      </form>

      <div className="bp-admin" style={{ marginTop: 14 }}>
        {tasks.map((task) => (
          <div key={task.id} className="bp-admin-season">
            <h3>{task.title}</h3>
            <p>
              Тип: {task.completion_type}
              {task.completion_type === 'RIDES' ? ` (${task.required_rides_count || 1} поездок)` : ''}
              {' | '}Награда: {task.reward_drive_coin} | Active: {String(task.is_active)}
            </p>
            <button
              type="button"
              onClick={() => {
                setEditingId(task.id)
                setForm({
                  title: task.title || '',
                  description: task.description || '',
                  completionType: task.completion_type || 'RIDES',
                  requiredRidesCount: task.required_rides_count || 1,
                  rewardDriveCoin: task.reward_drive_coin || 0,
                  sortOrder: task.sort_order || 0,
                  isActive: !!task.is_active,
                })
              }}
            >
              Редактировать
            </button>
            <button type="button" onClick={() => api(`/admin/tasks/${task.id}`, 'DELETE').then(load)}>Удалить</button>
          </div>
        ))}
      </div>
    </section>
  )
}

