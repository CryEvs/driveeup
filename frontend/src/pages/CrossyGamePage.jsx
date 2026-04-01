import { useSearchParams, Navigate } from 'react-router-dom'
import { CrossyGameShell } from '../crossy/CrossyGameShell'

/**
 * Полноэкранная страница только для WebView: ?embed=1.
 * Обычный вход — игра в модальном окне на /games.
 */
export function CrossyGamePage({ token, onClaimSuccess }) {
  const [searchParams] = useSearchParams()
  const embed = searchParams.get('embed') === '1'

  if (!embed) {
    return <Navigate to="/games" replace />
  }

  return (
    <div className="crossy-page">
      <header className="crossy-page__bar">
        <span className="crossy-page__brand">DriveeUP</span>
        <span className="crossy-page__title">Перебеги дорогу</span>
      </header>
      <CrossyGameShell
        token={token}
        onClaimSuccess={onClaimSuccess}
        embed
        variant="page"
      />
    </div>
  )
}
