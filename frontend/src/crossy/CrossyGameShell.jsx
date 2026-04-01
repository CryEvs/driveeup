import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { GameCanvas } from './GameCanvas'
import { CrossyUI } from './CrossyUI'
import { getCrossRoadCooldownRemaining, formatCooldown } from './crossyCooldown'
import './crossy.css'

/**
 * Общая разметка игры: кулдаун, сцена, UI. В модалке или на странице (embed).
 */
export function CrossyGameShell({
  token,
  onClaimSuccess,
  embed = false,
  variant = 'page',
  onRequestClose,
}) {
  const [gameReady, setGameReady] = useState(false)
  const [cooldownMs, setCooldownMs] = useState(() => getCrossRoadCooldownRemaining())

  useEffect(() => {
    const tick = () => setCooldownMs(getCrossRoadCooldownRemaining())
    tick()
    const id = setInterval(tick, 500)
    return () => clearInterval(id)
  }, [])

  const blocked = cooldownMs > 0
  const showClose = variant === 'modal' || (variant === 'page' && !embed)
  const webToken =
    token ??
    localStorage.getItem('driveeup_token') ??
    (typeof window !== 'undefined' && window.AndroidAuth && typeof window.AndroidAuth.getToken === 'function'
      ? window.AndroidAuth.getToken()
      : '') ??
    ''

  return (
    <div className="crossy-page__main">
      {blocked ? (
        <div className="crossy-page__cooldown-wrap">
          <div className="crossy-page__cooldown-card">
            <p className="crossy-page__cooldown-title">Перерыв перед следующей игрой</p>
            <p className="crossy-page__cooldown-time">{formatCooldown(cooldownMs)}</p>
            {variant === 'modal' && onRequestClose && (
              <button type="button" className="crossy-page__cooldown-back crossy-page__cooldown-back--btn" onClick={onRequestClose}>
                К играм
              </button>
            )}
            {variant === 'page' && !embed && (
              <Link to="/games" className="crossy-page__cooldown-back">
                К списку игр
              </Link>
            )}
          </div>
        </div>
      ) : (
        <>
          <div className="crossy-page__stage">
            {!gameReady && (
              <div className="crossy-game__loading" aria-busy="true" aria-live="polite">
                <div className="crossy-game__loading-spinner" />
                <span className="crossy-game__loading-text">Загрузка игры…</span>
              </div>
            )}
            {showClose &&
              (variant === 'modal' && onRequestClose ? (
                <button
                  type="button"
                  className="crossy-page__close"
                  aria-label="Закрыть"
                  title="Закрыть"
                  onClick={onRequestClose}
                >
                  <span aria-hidden>×</span>
                </button>
              ) : (
                <Link to="/games" className="crossy-page__close" aria-label="Закрыть" title="Закрыть">
                  <span aria-hidden>×</span>
                </Link>
              ))}
            <GameCanvas onReady={() => setGameReady(true)} />
            <CrossyUI
              token={webToken}
              embed={embed}
              onClaimSuccess={onClaimSuccess}
            />
          </div>
        </>
      )}
    </div>
  )
}
