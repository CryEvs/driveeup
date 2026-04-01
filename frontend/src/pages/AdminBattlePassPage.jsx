import { useEffect, useMemo, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { API_BASE } from '../authContext.jsx'

export function AdminBattlePassPage({ token, user }) {
  const [seasons, setSeasons] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [selectedLevel, setSelectedLevel] = useState(null)

  const [seasonForm, setSeasonForm] = useState({ name: '', startsAt: '', endsAt: '' })
  const [levelForm, setLevelForm] = useState({ seasonId: '', role: 'PASSENGER', levelNumber: 1, requiredDriveCoin: 0, iconUrl: '', description: '' })
  const [editingSeasonId, setEditingSeasonId] = useState(null)
  const [editingLevelId, setEditingLevelId] = useState(null)

  const isAdmin = !!user?.isAdmin
  const seasonOptions = useMemo(() => seasons.map((s) => ({ id: s.id, name: s.name })), [seasons])

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
      const data = await api('/admin/battle-pass/seasons')
      setSeasons(Array.isArray(data) ? data : [])
    } catch (e) {
      setError(e.message || 'Ошибка загрузки')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (isAdmin) load()
  }, [isAdmin])

  if (!isAdmin) return <Navigate to="/battle-pass" replace />

  async function submitSeason(e) {
    e.preventDefault()
    try {
      if (editingSeasonId) await api(`/admin/battle-pass/seasons/${editingSeasonId}`, 'PUT', seasonForm)
      else await api('/admin/battle-pass/seasons', 'POST', seasonForm)
      setSeasonForm({ name: '', startsAt: '', endsAt: '' })
      setEditingSeasonId(null)
      await load()
    } catch (e2) {
      setError(e2.message || 'Ошибка сохранения сезона')
    }
  }

  async function submitLevel(e) {
    e.preventDefault()
    const payload = {
      ...levelForm,
      seasonId: Number(levelForm.seasonId),
      levelNumber: Number(levelForm.levelNumber),
      requiredDriveCoin: Number(levelForm.requiredDriveCoin),
    }
    try {
      if (editingLevelId) await api(`/admin/battle-pass/levels/${editingLevelId}`, 'PUT', payload)
      else await api('/admin/battle-pass/levels', 'POST', payload)
      setLevelForm({ seasonId: '', role: 'PASSENGER', levelNumber: 1, requiredDriveCoin: 0, iconUrl: '', description: '' })
      setEditingLevelId(null)
      await load()
    } catch (e2) {
      setError(e2.message || 'Ошибка сохранения уровня')
    }
  }

  return (
    <section className="content-card">
      <h1>Админ-панель Батл-пасса</h1>
      {error && <p className="error">{error}</p>}
      {loading ? <p>Загрузка...</p> : null}

      <div className="bp-admin">
        <form onSubmit={submitSeason}>
          <h3>{editingSeasonId ? 'Редактировать сезон' : 'Создать сезон'}</h3>
          <input placeholder="Название" value={seasonForm.name} onChange={(e) => setSeasonForm((s) => ({ ...s, name: e.target.value }))} required />
          <input type="datetime-local" value={seasonForm.startsAt} onChange={(e) => setSeasonForm((s) => ({ ...s, startsAt: e.target.value }))} required />
          <input type="datetime-local" value={seasonForm.endsAt} onChange={(e) => setSeasonForm((s) => ({ ...s, endsAt: e.target.value }))} required />
          <button type="submit">{editingSeasonId ? 'Сохранить сезон' : 'Добавить сезон'}</button>
        </form>

        <form onSubmit={submitLevel}>
          <h3>{editingLevelId ? 'Редактировать уровень' : 'Добавить уровень'}</h3>
          <select value={levelForm.seasonId} onChange={(e) => setLevelForm((s) => ({ ...s, seasonId: e.target.value }))} required>
            <option value="">Выбери сезон</option>
            {seasonOptions.map((s) => (
              <option key={s.id} value={s.id}>{s.id} - {s.name}</option>
            ))}
          </select>
          <select value={levelForm.role} onChange={(e) => setLevelForm((s) => ({ ...s, role: e.target.value }))}>
            <option value="PASSENGER">Пассажир</option>
            <option value="DRIVER">Водитель</option>
          </select>
          <input type="number" min="1" value={levelForm.levelNumber} onChange={(e) => setLevelForm((s) => ({ ...s, levelNumber: e.target.value }))} required />
          <input type="number" min="0" value={levelForm.requiredDriveCoin} onChange={(e) => setLevelForm((s) => ({ ...s, requiredDriveCoin: e.target.value }))} required />
          <input placeholder="Иконка URL" value={levelForm.iconUrl} onChange={(e) => setLevelForm((s) => ({ ...s, iconUrl: e.target.value }))} />
          <textarea placeholder="Описание" value={levelForm.description} onChange={(e) => setLevelForm((s) => ({ ...s, description: e.target.value }))} />
          <button type="submit">{editingLevelId ? 'Сохранить уровень' : 'Добавить уровень'}</button>
        </form>
      </div>

      {seasons.map((season) => (
        <div key={season.id} className="bp-admin-season">
          <h3>{season.name}</h3>
          <p>{season.starts_at} - {season.ends_at}</p>
          <button type="button" onClick={() => setSeasonForm({ name: season.name, startsAt: season.starts_at?.slice(0, 16) || '', endsAt: season.ends_at?.slice(0, 16) || '' }) || setEditingSeasonId(season.id)}>Редактировать сезон</button>
          <button type="button" onClick={() => api(`/admin/battle-pass/seasons/${season.id}/finish`, 'POST').then(load)}>Завершить досрочно</button>
          <button type="button" onClick={() => api(`/admin/battle-pass/seasons/${season.id}`, 'DELETE').then(load)}>Удалить сезон</button>

          <div className="bp-levels">
            {(season.levels || []).map((lvl) => (
              <button
                type="button"
                key={lvl.id}
                className="bp-level bp-level--open"
                onClick={() => setSelectedLevel(lvl)}
                title="Открыть карточку уровня"
              >
                {lvl.icon_url ? <img src={lvl.icon_url} alt={`Уровень ${lvl.level_number}`} /> : <span>Lv {lvl.level_number}</span>}
                <small>{lvl.required_drive_coin}</small>
              </button>
            ))}
          </div>

          <div className="bp-admin-level-actions">
            {(season.levels || []).map((lvl) => (
              <div key={`actions-${lvl.id}`}>
                <button
                  type="button"
                  onClick={() => {
                    setLevelForm({
                      seasonId: String(season.id),
                      role: lvl.role,
                      levelNumber: lvl.level_number,
                      requiredDriveCoin: lvl.required_drive_coin,
                      iconUrl: lvl.icon_url || '',
                      description: lvl.description || '',
                    })
                    setEditingLevelId(lvl.id)
                  }}
                >
                  Редактировать уровень #{lvl.level_number}
                </button>
                <button type="button" onClick={() => api(`/admin/battle-pass/levels/${lvl.id}`, 'DELETE').then(load)}>Удалить</button>
              </div>
            ))}
          </div>
        </div>
      ))}

      {selectedLevel && (
        <div className="bp-level-modal" onClick={() => setSelectedLevel(null)}>
          <div className="bp-level-modal__card" onClick={(e) => e.stopPropagation()}>
            <h3>Уровень {selectedLevel.level_number}</h3>
            {selectedLevel.icon_url ? <img src={selectedLevel.icon_url} alt="Иконка уровня" /> : null}
            <p>Роль: {selectedLevel.role}</p>
            <p>Нужно DriveCoin за сезон: {selectedLevel.required_drive_coin}</p>
            <p>{selectedLevel.description || 'Без описания'}</p>
            <button type="button" onClick={() => setSelectedLevel(null)}>Закрыть</button>
          </div>
        </div>
      )}
    </section>
  )
}
