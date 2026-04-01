import { useEffect, useMemo, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { API_BASE } from '../authContext.jsx'

export function AdminBattlePassPage({ token, user }) {
  const [seasons, setSeasons] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [selectedLevel, setSelectedLevel] = useState(null)

  const [seasonForm, setSeasonForm] = useState({ name: '', startsAt: '', endsAt: '' })
  const [levelForm, setLevelForm] = useState({
    seasonId: '',
    role: 'PASSENGER',
    levelNumber: 1,
    requiredDriveCoin: 0,
    iconUrl: '',
    description: '',
    giftName: '',
    giftDescription: '',
    giftType: 'DRIVECOIN',
    giftDriveCoin: 0,
    giftText: '',
  })
  const [editingSeasonId, setEditingSeasonId] = useState(null)
  const [editingLevelId, setEditingLevelId] = useState(null)
  const [uploadingIcon, setUploadingIcon] = useState(false)
  const [activeTab, setActiveTab] = useState('seasons')

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
      giftDriveCoin: Number(levelForm.giftDriveCoin),
    }
    try {
      if (editingLevelId) await api(`/admin/battle-pass/levels/${editingLevelId}`, 'PUT', payload)
      else await api('/admin/battle-pass/levels', 'POST', payload)
      setLevelForm({
        seasonId: '',
        role: 'PASSENGER',
        levelNumber: 1,
        requiredDriveCoin: 0,
        iconUrl: '',
        description: '',
        giftName: '',
        giftDescription: '',
        giftType: 'DRIVECOIN',
        giftDriveCoin: 0,
        giftText: '',
      })
      setEditingLevelId(null)
      await load()
    } catch (e2) {
      setError(e2.message || 'Ошибка сохранения уровня')
    }
  }

  async function uploadIcon(file) {
    if (!file) return
    setUploadingIcon(true)
    setError('')
    try {
      const form = new FormData()
      form.append('icon', file)
      const res = await fetch(`${API_BASE}/admin/battle-pass/levels/icon`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
        },
        body: form,
      })
      const json = await res.json().catch(() => ({}))
      if (!res.ok) throw new Error(json.error || 'Не удалось загрузить иконку')
      setLevelForm((s) => ({ ...s, iconUrl: json.iconUrl || '' }))
    } catch (e) {
      setError(e.message || 'Ошибка загрузки иконки')
    } finally {
      setUploadingIcon(false)
    }
  }

  return (
    <section className="content-card">
      <h1>Админ-панель Батл-пасса</h1>
      {error && <p className="error">{error}</p>}
      {loading ? <p>Загрузка...</p> : null}

      <div className="bp-admin-tabs" role="tablist" aria-label="Разделы админ-панели батл-пасса">
        <button
          type="button"
          className={`bp-admin-tab-btn ${activeTab === 'seasons' ? 'bp-admin-tab-btn--active' : ''}`}
          onClick={() => setActiveTab('seasons')}
        >
          Сезоны
        </button>
        <button
          type="button"
          className={`bp-admin-tab-btn ${activeTab === 'levels' ? 'bp-admin-tab-btn--active' : ''}`}
          onClick={() => setActiveTab('levels')}
        >
          Уровни
        </button>
      </div>

      <div className="bp-admin">
        {activeTab === 'seasons' ? (
          <form onSubmit={submitSeason} className="bp-admin-col">
            <h3>{editingSeasonId ? 'Редактировать сезон' : 'Создать сезон'}</h3>
            <label className="bp-field-label">Название сезона</label>
            <input placeholder="Название" value={seasonForm.name} onChange={(e) => setSeasonForm((s) => ({ ...s, name: e.target.value }))} required />
            <label className="bp-field-label">Дата начала</label>
            <input type="datetime-local" value={seasonForm.startsAt} onChange={(e) => setSeasonForm((s) => ({ ...s, startsAt: e.target.value }))} required />
            <label className="bp-field-label">Дата окончания</label>
            <input type="datetime-local" value={seasonForm.endsAt} onChange={(e) => setSeasonForm((s) => ({ ...s, endsAt: e.target.value }))} required />
            <button type="submit">{editingSeasonId ? 'Сохранить сезон' : 'Добавить сезон'}</button>
          </form>
        ) : (
          <form onSubmit={submitLevel} className="bp-admin-col">
            <h3>{editingLevelId ? 'Редактировать уровень' : 'Добавить уровень'}</h3>
            <label className="bp-field-label">Сезон</label>
            <select value={levelForm.seasonId} onChange={(e) => setLevelForm((s) => ({ ...s, seasonId: e.target.value }))} required>
              <option value="">Выбери сезон</option>
              {seasonOptions.map((s) => (
                <option key={s.id} value={s.id}>{s.id} - {s.name}</option>
              ))}
            </select>
            <label className="bp-field-label">Роль</label>
            <select value={levelForm.role} onChange={(e) => setLevelForm((s) => ({ ...s, role: e.target.value }))}>
              <option value="PASSENGER">Пассажир</option>
              <option value="DRIVER">Водитель</option>
            </select>
            <label className="bp-field-label">Номер уровня</label>
            <input type="number" min="1" value={levelForm.levelNumber} onChange={(e) => setLevelForm((s) => ({ ...s, levelNumber: e.target.value }))} required />
            <label className="bp-field-label">Нужно DriveCoin за сезон</label>
            <input type="number" min="0" value={levelForm.requiredDriveCoin} onChange={(e) => setLevelForm((s) => ({ ...s, requiredDriveCoin: e.target.value }))} required />
            <label className="bp-field-label">Иконка уровня (файл)</label>
            <input
              type="file"
              accept="image/*"
              onChange={(e) => uploadIcon(e.target.files?.[0])}
            />
            {uploadingIcon && <p>Загрузка иконки...</p>}
            <label className="bp-field-label">URL иконки (после загрузки подставится автоматически)</label>
            <input placeholder="Иконка URL" value={levelForm.iconUrl} onChange={(e) => setLevelForm((s) => ({ ...s, iconUrl: e.target.value }))} />
            <label className="bp-field-label">Описание уровня</label>
            <textarea placeholder="Описание" value={levelForm.description} onChange={(e) => setLevelForm((s) => ({ ...s, description: e.target.value }))} />
            <label className="bp-field-label">Название подарка</label>
            <input placeholder="Например: Бонус DriveCoin" value={levelForm.giftName} onChange={(e) => setLevelForm((s) => ({ ...s, giftName: e.target.value }))} />
            <label className="bp-field-label">Описание подарка</label>
            <textarea placeholder="Что получает пользователь" value={levelForm.giftDescription} onChange={(e) => setLevelForm((s) => ({ ...s, giftDescription: e.target.value }))} />
            <label className="bp-field-label">Тип подарка</label>
            <select value={levelForm.giftType} onChange={(e) => setLevelForm((s) => ({ ...s, giftType: e.target.value }))}>
              <option value="DRIVECOIN">DriveCoin</option>
              <option value="TEXT">Текст</option>
            </select>
            {levelForm.giftType === 'DRIVECOIN' ? (
              <>
                <label className="bp-field-label">Сколько DriveCoin в подарке</label>
                <input type="number" min="0" value={levelForm.giftDriveCoin} onChange={(e) => setLevelForm((s) => ({ ...s, giftDriveCoin: e.target.value }))} />
              </>
            ) : (
              <>
                <label className="bp-field-label">Текст подарка</label>
                <textarea placeholder="Например: Поздравляем! Вам открыт уникальный титул." value={levelForm.giftText} onChange={(e) => setLevelForm((s) => ({ ...s, giftText: e.target.value }))} />
              </>
            )}
            <button type="submit">{editingLevelId ? 'Сохранить уровень' : 'Добавить уровень'}</button>
          </form>
        )}
      </div>

      {activeTab === 'seasons' && seasons.map((season) => (
        <div key={season.id} className="bp-admin-season">
          <h3>{season.name}</h3>
          <p>{season.starts_at} - {season.ends_at}</p>
          <button type="button" onClick={() => setSeasonForm({ name: season.name, startsAt: season.starts_at?.slice(0, 16) || '', endsAt: season.ends_at?.slice(0, 16) || '' }) || setEditingSeasonId(season.id)}>Редактировать сезон</button>
          <button type="button" onClick={() => api(`/admin/battle-pass/seasons/${season.id}/finish`, 'POST').then(load)}>Завершить досрочно</button>
          <button type="button" onClick={() => api(`/admin/battle-pass/seasons/${season.id}`, 'DELETE').then(load)}>Удалить сезон</button>
        </div>
      ))}

      {activeTab === 'levels' && seasons.map((season) => (
        <div key={`levels-${season.id}`} className="bp-admin-season">
          <h3>{season.name}</h3>
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
                      giftName: lvl.gift_name || '',
                      giftDescription: lvl.gift_description || '',
                      giftType: lvl.gift_type || 'DRIVECOIN',
                      giftDriveCoin: lvl.gift_drive_coin || 0,
                      giftText: lvl.gift_text || '',
                    })
                    setEditingLevelId(lvl.id)
                    setActiveTab('levels')
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
            <p>Подарок: {selectedLevel.gift_name || 'Не задан'}</p>
            <p>Тип подарка: {selectedLevel.gift_type === 'TEXT' ? 'Текст' : 'DriveCoin'}</p>
            {selectedLevel.gift_type === 'TEXT' ? <p>Текст подарка: {selectedLevel.gift_text || 'Не задан'}</p> : <p>DriveCoin в подарке: {selectedLevel.gift_drive_coin || 0}</p>}
            <p>{selectedLevel.gift_description || 'Без описания подарка'}</p>
            <p>{selectedLevel.description || 'Без описания'}</p>
            <button type="button" onClick={() => setSelectedLevel(null)}>Закрыть</button>
          </div>
        </div>
      )}
    </section>
  )
}
