import { AdminPlaceholderPage } from './AdminPlaceholderPage.jsx'

export function AdminRidesPage({ user }) {
  return (
    <AdminPlaceholderPage
      user={user}
      title="Админ: Поездки"
      subtitle="Модерация и мониторинг активных/прошедших заказов."
      detail="Пока нет эндпоинтов для списка и действий по поездкам."
    />
  )
}

