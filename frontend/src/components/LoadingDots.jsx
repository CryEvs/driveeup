/** Три анимированные точки (ожидание загрузки / ответа API) */
export function LoadingDots({ className = '', label = 'Загрузка' }) {
  return (
    <span className={`loading-dots ${className}`.trim()} role="status" aria-label={label}>
      <span className="loading-dots__dot" />
      <span className="loading-dots__dot" />
      <span className="loading-dots__dot" />
    </span>
  )
}
