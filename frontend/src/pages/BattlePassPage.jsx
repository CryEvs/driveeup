import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { API_BASE } from '../authContext.jsx'

export function BattlePassPage({ token, user }) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedLevel, setSelectedLevel] = useState(null)
  const isAdmin = !!user?.isAdmin

  async function load() {
    setLoading(true)
    setError('')
    try {
      const res = await fetch(`${API_BASE}/battle-pass/current`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      const json = await res.json()
      if (!res.ok) throw new Error(json.error || 'Ошибка загрузки батл-пасса')
      setData(json)
    } catch (e) {
      setError(e.message || 'Ошибка')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [token])

  const levels = data?.levels || []
  const seasonDriveCoin = Number(data?.seasonDriveCoin || 0)
  const totalDriveCoin = Number(data?.totalDriveCoin || 0)
  const maxTarget = useMemo(() => {
    if (!levels.length) return 1
    return Math.max(...levels.map((x) => Number(x.requiredDriveCoin || 0)), 1)
  }, [levels])
  const progressPct = Math.min(100, Math.round((seasonDriveCoin / maxTarget) * 100))

  return (
    <section className="content-card">
      <h1>Батл-пасс</h1>
      {loading && <p>Загрузка...</p>}
      {error && <p className="error">{error}</p>}
      {!loading && (
        <>
          {data?.season ? (
            <p>
              Сезон: {data.season.name} ({new Date(data.season.startsAt).toLocaleDateString()} -{' '}
              {new Date(data.season.endsAt).toLocaleDateString()})
            </p>
          ) : (
            <p>Сейчас нет активного сезона.</p>
          )}
          <div className="bp-progress">
            <div className="bp-progress__bar" style={{ width: `${progressPct}%` }} />
          </div>
          <div className="bp-levels">
            {levels.map((lvl) => {
              const unlocked = seasonDriveCoin >= Number(lvl.requiredDriveCoin || 0)
              return (
                <button
                  type="button"
                  key={lvl.id}
                  className={unlocked ? 'bp-level bp-level--open' : 'bp-level'}
                  title={lvl.description || ''}
                  onClick={() => setSelectedLevel(lvl)}
                >
                  {lvl.iconUrl ? <img src={lvl.iconUrl} alt={`Уровень ${lvl.levelNumber}`} /> : <span>Lv {lvl.levelNumber}</span>}
                  <small>{lvl.requiredDriveCoin}</small>
                </button>
              )
            })}
          </div>
          {isAdmin && (
            <p><Link to="/admin/battle-pass">Открыть экран админ-панели Батл-пасса</Link></p>
          )}
        </>
      )}
      {selectedLevel && (
        <div className="bp-level-modal" onClick={() => setSelectedLevel(null)}>
          <div className="bp-level-modal__card" onClick={(e) => e.stopPropagation()}>
            <h3>Уровень {selectedLevel.levelNumber}</h3>
            {selectedLevel.iconUrl ? <img src={selectedLevel.iconUrl} alt="Иконка уровня" /> : null}
            <p>Роль: {selectedLevel.role}</p>
            <p>Нужно DriveCoin за сезон: {selectedLevel.requiredDriveCoin}</p>
            <p>{selectedLevel.description || 'Без описания'}</p>
            <button type="button" onClick={() => setSelectedLevel(null)}>Закрыть</button>
          </div>
        </div>
      )}
    </section>
  )
}
