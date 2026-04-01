import { useCallback, useEffect, useState } from 'react'
import { CrossRoadGame } from '../games/CrossRoadGame'
import {
  formatCooldown,
  getCrossroadCooldownRemaining,
  startCrossroadCooldown
} from '../games/crossroadCooldown'

export function GamesPage() {
  const [modalOpen, setModalOpen] = useState(false)
  const [session, setSession] = useState(0)
  const [cooldownLeft, setCooldownLeft] = useState(0)

  const tickCooldown = useCallback(() => {
    setCooldownLeft(getCrossroadCooldownRemaining())
  }, [])

  useEffect(() => {
    tickCooldown()
    const id = setInterval(tickCooldown, 500)
    return () => clearInterval(id)
  }, [tickCooldown])

  function openModal() {
    if (getCrossroadCooldownRemaining() > 0) return
    setSession((s) => s + 1)
    setModalOpen(true)
  }

  function closeModal() {
    setModalOpen(false)
  }

  function handleGameOver() {
    startCrossroadCooldown()
    tickCooldown()
  }

  const canPlay = cooldownLeft <= 0

  return (
    <section className="games-page content-card">
      <header className="games-page__head">
        <h1>Игры</h1>
        <p className="games-page__lead">
          Мини-игры DriveeUP. После партии действует пауза 1 минута — отдохни и возвращайся.
        </p>
      </header>

      <article className="game-card">
        <div className="game-card__preview game-card__preview--crossroad" aria-hidden>
          <div className="game-card__preview-road" />
          <div className="game-card__preview-walk" />
          <span className="game-card__preview-badge">3D</span>
        </div>
        <div className="game-card__body">
          <h2 className="game-card__title">Перебеги дорого</h2>
          <p className="game-card__desc">
            Шесть полос, тротуар между третьей и четвёртой. На полосах 1–3 поток влево, на 4–6 —
            вправо. Пробел или тап по центру — шаг вперёд, стрелки или края экрана — смена полосы.
            Доберись до финиша и набери очки. Сбила машина — конец игры.
          </p>
          {!canPlay && (
            <p className="game-card__cooldown">
              Пауза: <strong>{formatCooldown(cooldownLeft)}</strong>
            </p>
          )}
          <button
            type="button"
            className="game-card__play"
            disabled={!canPlay}
            onClick={openModal}
          >
            {canPlay ? 'Играть' : 'Подождите…'}
          </button>
        </div>
      </article>

      {modalOpen && (
        <div className="game-modal" role="dialog" aria-modal="true" aria-labelledby="game-modal-title">
          <div className="game-modal__backdrop" onClick={closeModal} />
          <div className="game-modal__panel">
            <div className="game-modal__bar">
              <h2 id="game-modal-title">Перебеги дорого</h2>
              <button type="button" className="game-modal__close" onClick={closeModal} aria-label="Закрыть">
                ×
              </button>
            </div>
            <div className="game-modal__canvas">
              <CrossRoadGame key={session} onGameOver={handleGameOver} />
            </div>
          </div>
        </div>
      )}
    </section>
  )
}
