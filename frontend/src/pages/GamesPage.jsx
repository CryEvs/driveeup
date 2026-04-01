import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import {
  formatCooldown,
  getCrossRoadCooldownRemaining,
  startCrossRoadCooldown,
} from '../crossy/crossyCooldown'
import { CrossyGameShell } from '../crossy/CrossyGameShell'
import { useAuth } from '../authContext.jsx'
import '../crossy/crossy.css'

export function GamesPage() {
  const auth = useAuth()
  const [cooldownMs, setCooldownMs] = useState(() => getCrossRoadCooldownRemaining())
  const [gameOpen, setGameOpen] = useState(false)
  const [modalKey, setModalKey] = useState(0)

  useEffect(() => {
    const tick = () => setCooldownMs(getCrossRoadCooldownRemaining())
    tick()
    const id = setInterval(tick, 500)
    return () => clearInterval(id)
  }, [])

  useEffect(() => {
    if (!gameOpen) return
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = prev
    }
  }, [gameOpen])

  const onCooldown = cooldownMs > 0

  function openGame() {
    if (onCooldown) return
    setModalKey((k) => k + 1)
    setGameOpen(true)
  }

  function closeGame() {
    startCrossRoadCooldown()
    try {
      if (typeof window !== 'undefined' && window.AndroidCrossy?.notifyCooldown) {
        window.AndroidCrossy.notifyCooldown()
      }
    } catch {
      /* не WebView */
    }
    setGameOpen(false)
  }

  const modal =
    gameOpen &&
    createPortal(
      <div
        className="game-modal-overlay"
        role="presentation"
        onClick={closeGame}
      >
        <div
          className="game-modal-dialog"
          role="dialog"
          aria-modal="true"
          aria-label="Перебеги дорогу"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="crossy-page crossy-page--modal">
            <CrossyGameShell
              key={modalKey}
              token={auth.token}
              onClaimSuccess={() => {
                auth.fetchMe()
                closeGame()
              }}
              embed={false}
              variant="modal"
              onRequestClose={closeGame}
            />
          </div>
        </div>
      </div>,
      document.body
    )

  return (
    <section className="games-page">
      <article className="game-card game-card--vertical game-card--single">
        <div className="game-card__media">
          <div className="game-card__preview game-card__preview--crossy" aria-hidden>
            <div className="game-card__preview-voxel" />
          </div>
          <div className="game-card__overlay">
            <h2 className="game-card__overlay-title">Перебеги дорогу</h2>
            <div className="game-card__overlay-actions">
              {onCooldown && (
                <p className="game-card__cooldown game-card__cooldown--overlay">
                  Следующая игра через {formatCooldown(cooldownMs)}
                </p>
              )}
              {onCooldown ? (
                <button type="button" className="game-card__play game-card__play--brand game-card__play--on-card" disabled>
                  Играть
                </button>
              ) : (
                <button
                  type="button"
                  className="game-card__play game-card__play--brand game-card__play--on-card"
                  onClick={openGame}
                >
                  Играть
                </button>
              )}
            </div>
          </div>
        </div>
      </article>
      {modal}
    </section>
  )
}
