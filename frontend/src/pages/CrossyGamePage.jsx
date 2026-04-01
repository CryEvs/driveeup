import { Link, useSearchParams } from 'react-router-dom'
import { useState, useEffect } from 'react'
import { GameCanvas } from '../crossy/GameCanvas'
import { CrossyUI } from '../crossy/CrossyUI'
import { getCrossRoadCooldownRemaining, formatCooldown } from '../crossy/crossyCooldown'
import '../crossy/crossy.css'

/**
 * «Перебеги дорогу» — аркада, публичный маршрут для WebView.
 */
export function CrossyGamePage({ token, onClaimSuccess }) {
  const [searchParams] = useSearchParams()
  const embed = searchParams.get('embed') === '1'
  const [session, setSession] = useState(0)
  const [cooldownMs, setCooldownMs] = useState(() => getCrossRoadCooldownRemaining())

  useEffect(() => {
    const tick = () => setCooldownMs(getCrossRoadCooldownRemaining())
    tick()
    const id = setInterval(tick, 500)
    return () => clearInterval(id)
  }, [])

  const restart = () => setSession((s) => s + 1)
  const blocked = cooldownMs > 0

  return (
    <div className="crossy-page">
      <header className="crossy-page__bar">
        {embed && <span className="crossy-page__brand">DriveeUP</span>}
        <span className="crossy-page__title">Перебеги дорогу</span>
      </header>

      <div className="crossy-page__main">
        {blocked ? (
          <div className="crossy-page__cooldown-wrap">
            <div className="crossy-page__cooldown-card">
              <p className="crossy-page__cooldown-title">Перерыв перед следующей игрой</p>
              <p className="crossy-page__cooldown-time">{formatCooldown(cooldownMs)}</p>
              {!embed && (
                <Link to="/games" className="crossy-page__cooldown-back">
                  К списку игр
                </Link>
              )}
            </div>
          </div>
        ) : (
          <>
            <div className="crossy-page__stage" key={session}>
              {!embed && (
                <Link to="/games" className="crossy-page__close" aria-label="Закрыть" title="Закрыть">
                  <span aria-hidden>×</span>
                </Link>
              )}
              <GameCanvas />
              <CrossyUI
                token={token ?? localStorage.getItem('driveeup_token') ?? ''}
                embed={embed}
                onClaimSuccess={onClaimSuccess}
              />
            </div>
            <footer className="crossy-page__footer">
              <button type="button" className="crossy-page__restart" onClick={restart}>
                Заново
              </button>
            </footer>
          </>
        )}
      </div>
    </div>
  )
}
