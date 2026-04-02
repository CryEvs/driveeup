import { Link, Navigate } from 'react-router-dom'
import { API_BASE } from '../authContext.jsx'

function AdminCard({ title, to, description, comingSoon = false }) {
  return (
    <div className={`admin-card ${comingSoon ? 'admin-card--disabled' : ''}`}>
      <div className="admin-card__title">{title}</div>
      <div className="admin-card__desc">{description}</div>
      {comingSoon ? (
        <div className="admin-card__soon">в разработке</div>
      ) : (
        <Link className="admin-card__link" to={to}>
          Открыть
        </Link>
      )}
    </div>
  )
}

/**
 * Общая админ-панель.
 * Тут только UI-шелл и навигация по модулям.
 * CRUD будет подключаться по мере появления эндпоинтов/моделей в API.
 */
export function AdminPanelPage({ token, user }) {
  const isAdmin = !!user?.isAdmin

  if (!isAdmin) return <Navigate to="/battle-pass" replace />

  // token/URL сейчас не используются, но оставляем для будущих модулей (общие хелперы).
  void token
  void API_BASE

  return (
    <section className="content-card">
      <h1>Админ-панель</h1>
      <p style={{ marginTop: 6, marginBottom: 0, color: '#6c6c6c' }}>
        Выбирай раздел, чтобы управлять сущностями приложения.
      </p>

      <div className="admin-panel-grid" style={{ marginTop: 18 }}>
        <AdminCard
          title="Батл-Пасс"
          to="/admin/battle-pass"
          description="Сезоны, уровни и подарки."
        />
        <AdminCard
          title="Пользователи"
          to="/admin/users"
          description="Роли, премиум, баланс."
          comingSoon
        />
        <AdminCard
          title="Поездки"
          to="/admin/rides"
          description="Статусы и модерация (мониторинг)."
          comingSoon
        />
        <AdminCard
          title="Лояльность"
          to="/admin/loyalty"
          description="Тиры, пороги, прогресс и привилегии."
        />
        <AdminCard
          title="Магазин за DriveCoin"
          to="/admin/store"
          description="Товары и покупка."
        />
        <AdminCard
          title="Задания"
          to="/admin/tasks"
          description="Порождение заданий и награды."
        />
      </div>
    </section>
  )
}

