import { AdminPlaceholderPage } from './AdminPlaceholderPage.jsx'

export function AdminLoyaltyPage({ user }) {
  return (
    <AdminPlaceholderPage
      user={user}
      title="Админ: Лояльность"
      subtitle="Тиры и прогресс по числу поездок."
      detail="Пока нет API CRUD для лояльности/порогов/привилегий."
    />
  )
}

