import { Navigate } from 'react-router-dom'

export function AdminPlaceholderPage({ user, title, subtitle, detail }) {
  const isAdmin = !!user?.isAdmin
  if (!isAdmin) return <Navigate to="/battle-pass" replace />

  return (
    <section className="content-card">
      <h1>{title}</h1>
      {subtitle ? <p style={{ marginTop: 6, color: '#6c6c6c' }}>{subtitle}</p> : null}
      <div
        style={{
          marginTop: 18,
          border: '1px solid #d7efb5',
          borderRadius: 14,
          padding: 14,
          background: '#fff'
        }}
      >
        <b>В разработке:</b> {detail || 'нет CRUD эндпоинтов для этой сущности'}
      </div>
    </section>
  )
}

