import { useCallback, useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { CrossRoadGame } from '../games/CrossRoadGame'
import {
  formatCooldown,
  getCrossroadCooldownRemaining,
  startCrossroadCooldown
} from '../games/crossroadCooldown'

/**
 * Полноэкранная страница игры для WebView в Android и прямых ссылок.
 */
export function CrossRoadPlayPage() {
  const [searchParams] = useSearchParams()
  const embed = searchParams.get('embed') === '1'

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

  function handleGameOver() {
    startCrossroadCooldown()
    tickCooldown()
    setSession((s) => s + 1)
  }

  const canPlay = cooldownLeft <= 0

  return (
    <div className="crossroad-play-page">
      <header className="crossroad-play-page__bar">
        {!embed && (
          <Link to="/games" className="crossroad-play-page__back">
            ← К играм
          </Link>
        )}
        {embed && <span className="crossroad-play-page__back crossroad-play-page__back--muted">DriveeUP</span>}
        <span className="crossroad-play-page__title">Перебеги дорого</span>
        {!canPlay && (
          <span className="crossroad-play-page__cd">Пауза: {formatCooldown(cooldownLeft)}</span>
        )}
      </header>
      <div className="crossroad-play-page__stage">
        {canPlay ? (
          <CrossRoadGame key={session} embedded onGameOver={handleGameOver} />
        ) : (
          <div className="crossroad-play-page__wait">
            <p>Следующая партия через {formatCooldown(cooldownLeft)}</p>
          </div>
        )}
      </div>
    </div>
  )
}
