import { AdminPlaceholderPage } from './AdminPlaceholderPage.jsx'

export function AdminUsersPage({ user }) {
  return (
    <AdminPlaceholderPage
      user={user}
      title="Админ: Пользователи"
      subtitle="Управление ролями, премиумом и балансом."
      detail="Пока нет API CRUD. Можно подключить после создания эндпоинтов в Laravel."
    />
  )
}

