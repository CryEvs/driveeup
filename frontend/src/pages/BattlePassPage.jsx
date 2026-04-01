import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { API_BASE } from '../authContext.jsx'

export function BattlePassPage({ token, user }) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedLevel, setSelectedLevel] = useState(null)
  const [claimingLevelId, setClaimingLevelId] = useState(null)
  const [imageErrors, setImageErrors] = useState({})
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
  const threadWidthPx = Math.max(760, levels.length * 132)
  const levelPoints = useMemo(() => {
    return levels.map((lvl) => {
      const required = Number(lvl.requiredDriveCoin || 0)
      const unlocked = seasonDriveCoin >= required
      return {
        ...lvl,
        unlocked,
        canClaimGift: unlocked && !lvl.giftClaimed,
        leftPct: maxTarget > 0 ? Math.min(100, (required / maxTarget) * 100) : 0,
      }
    })
  }, [levels, seasonDriveCoin, maxTarget])

  async function claimGift(levelId) {
    setError('')
    setClaimingLevelId(levelId)
    try {
      const res = await fetch(`${API_BASE}/battle-pass/levels/${levelId}/claim-gift`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
      })
      const json = await res.json().catch(() => ({}))
      if (!res.ok) throw new Error(json.error || 'Не удалось получить подарок')
      await load()
      if (selectedLevel?.id === levelId) {
        setSelectedLevel((prev) => (prev ? { ...prev, giftClaimed: true, giftType: json.giftType || prev.giftType, giftText: json.giftText ?? prev.giftText } : prev))
      }
    } catch (e) {
      setError(e.message || 'Ошибка')
    } finally {
      setClaimingLevelId(null)
    }
  }

  return (
    <section className="content-card content-card--wide">
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
          <div className="bp-thread-scroll">
            <div className="bp-thread-board" style={{ width: `${threadWidthPx}px` }}>
              <div className="bp-progress bp-progress--thread" style={{ '--bp-progress': `${progressPct}%` }}>
                <div className="bp-progress__bar" />
              </div>
              {levelPoints.map((lvl) => (
                <div
                  key={lvl.id}
                  className="bp-level-point"
                  style={{ left: `${lvl.leftPct}%` }}
                >
                <button
                  type="button"
                  className={lvl.unlocked ? 'bp-level bp-level--open' : 'bp-level'}
                  title={lvl.description || ''}
                  onClick={() => setSelectedLevel(lvl)}
                >
                  <small className="bp-level__price">{lvl.requiredDriveCoin} DriveCoin</small>
                  {lvl.iconUrl && !imageErrors[lvl.id] ? (
                    <img
                      src={lvl.iconUrl}
                      alt={`Уровень ${lvl.levelNumber}`}
                      onError={() => setImageErrors((s) => ({ ...s, [lvl.id]: true }))}
                    />
                  ) : (
                    <span>Lv</span>
                  )}
                  <small className="bp-level__number">Уровень {lvl.levelNumber}</small>
                </button>
                {lvl.canClaimGift ? (
                  <button
                    type="button"
                    className="bp-gift-claim"
                    onClick={() => claimGift(lvl.id)}
                    disabled={claimingLevelId === lvl.id}
                  >
                    {claimingLevelId === lvl.id ? 'Выдача...' : 'Забрать подарок'}
                  </button>
                ) : (
                  <p className="bp-gift-status">{lvl.giftClaimed ? 'Подарок получен' : 'Подарок закрыт'}</p>
                )}
                </div>
              ))}
            </div>
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
            {seasonDriveCoin >= Number(selectedLevel.requiredDriveCoin || 0) ? (
              <>
                <p>Подарок: {selectedLevel.giftName || 'Не задан'}</p>
                <p>Тип подарка: {selectedLevel.giftType === 'TEXT' ? 'Текст' : 'DriveCoin'}</p>
                {selectedLevel.giftType === 'TEXT'
                  ? <p>Текст подарка: {selectedLevel.giftText || 'Не задан'}</p>
                  : <p>DriveCoin в подарке: {selectedLevel.giftDriveCoin || 0}</p>}
                <p>{selectedLevel.giftDescription || 'Без описания подарка'}</p>
                {selectedLevel.giftClaimed ? (
                  <p>Статус подарка: получен</p>
                ) : (
                  <button type="button" onClick={() => claimGift(selectedLevel.id)} disabled={claimingLevelId === selectedLevel.id}>
                    {claimingLevelId === selectedLevel.id ? 'Выдача...' : 'Получить подарок'}
                  </button>
                )}
              </>
            ) : (
              <p>Подарок скрыт до достижения уровня.</p>
            )}
            <p>{selectedLevel.description || 'Без описания'}</p>
            <button type="button" onClick={() => setSelectedLevel(null)}>Закрыть</button>
          </div>
        </div>
      )}
    </section>
  )
}
