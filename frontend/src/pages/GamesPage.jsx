import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { formatCooldown, getCrossRoadCooldownRemaining } from '../crossy/crossyCooldown'

export function GamesPage() {
  const [cooldownMs, setCooldownMs] = useState(() => getCrossRoadCooldownRemaining())

  useEffect(() => {
    const tick = () => setCooldownMs(getCrossRoadCooldownRemaining())
    tick()
    const id = setInterval(tick, 500)
    return () => clearInterval(id)
  }, [])

  const onCooldown = cooldownMs > 0

  return (
    <section className="games-page content-card">
      <header className="games-page__head">
        <h1>Игры</h1>
        <p className="games-page__lead">
          Бесконечная 3D-аркада: трава, дороги с машинами, ж/д. Зарабатывай DriveeCoin за пройденные полосы.
        </p>
      </header>

      <article className="game-card game-card--single">
        <div className="game-card__preview game-card__preview--crossy" aria-hidden>
          <div className="game-card__preview-voxel" />
          <span className="game-card__preview-badge">3D</span>
        </div>
        <div className="game-card__body">
          <h2 className="game-card__title">Перебеги дорогу</h2>
          <p className="game-card__desc">
            Только вперёд. Старт с одной травы; не больше одной зелёной подряд; подряд полос с машинами 1–4, после
            серии дорог — трава; ж/д и дороги чередуются. На дороге машины в одном направлении.
            После аварии получи DriveeCoin на баланс.
          </p>
          {onCooldown && (
            <p className="game-card__cooldown">Следующая игра через {formatCooldown(cooldownMs)}</p>
          )}
          {onCooldown ? (
            <button type="button" className="game-card__play game-card__play--brand" disabled>
              Играть
            </button>
          ) : (
            <Link
              to="/games/cross-road"
              className="game-card__play game-card__play--link game-card__play--brand"
            >
              Играть
            </Link>
          )}
        </div>
      </article>
    </section>
  )
}
