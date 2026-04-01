import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCrossyStore } from './crossyStore'
import { claimDriveeCoin } from './gameRewardApi'
import { startCrossRoadCooldown } from './crossyCooldown'

export function CrossyUI({ token, embed, onClaimSuccess }) {
  const navigate = useNavigate()
  const status = useCrossyStore((s) => s.status)
  const lanesPassed = useCrossyStore((s) => s.lanesPassed)
  const driveCoinPreview = useCrossyStore((s) => s.driveCoinPreview)

  const [claiming, setClaiming] = useState(false)
  const [claimed, setClaimed] = useState(false)
  const [claimErr, setClaimErr] = useState(null)

  async function handleClaim() {
    if (!token) {
      setClaimErr('Войди в аккаунт, чтобы получить DriveeCoin')
      return
    }
    setClaiming(true)
    setClaimErr(null)
    try {
      await claimDriveeCoin(lanesPassed, token)
      setClaimed(true)
      startCrossRoadCooldown()
      try {
        if (embed && typeof window !== 'undefined' && window.AndroidCrossy?.notifyCooldown) {
          window.AndroidCrossy.notifyCooldown()
        }
      } catch {
        /* WebView без моста */
      }
      onClaimSuccess?.()
      if (!embed) {
        navigate('/games')
      }
    } catch (e) {
      setClaimErr(e.message || 'Ошибка')
    } finally {
      setClaiming(false)
    }
  }

  return (
    <div className="crossy-ui">
      {status === 'running' && (
        <div className="crossy-ui__hud crossy-ui__hud--minimal">
          <span className="crossy-ui__hint">Пробел / тап — вперёд</span>
        </div>
      )}

      {status === 'gameOver' && (
        <div className="crossy-ui__overlay">
          <div className="crossy-ui__panel crossy-ui__panel--end">
            <h2>Игра окончена</h2>
            <p className="crossy-ui__dc">
              Заработано DriveeCoin: <strong>{driveCoinPreview}</strong>
            </p>
            {!embed && (
              <p className="crossy-ui__sub">
                Полос пройдено: {lanesPassed} · начисление 0.5 за полосу
              </p>
            )}
            <button
              type="button"
              className="crossy-ui__claim"
              disabled={claiming || claimed || driveCoinPreview <= 0}
              onClick={handleClaim}
            >
              {claimed ? 'Получено' : claiming ? '…' : 'Получить'}
            </button>
            {claimErr && <p className="crossy-ui__err">{claimErr}</p>}
            {!token && !embed && (
              <p className="crossy-ui__muted">Войди в аккаунт на сайте, затем открой игру снова.</p>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
