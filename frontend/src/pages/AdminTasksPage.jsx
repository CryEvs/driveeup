import { AdminPlaceholderPage } from './AdminPlaceholderPage.jsx'

export function AdminTasksPage({ user }) {
  return (
    <AdminPlaceholderPage
      user={user}
      title="Админ: Задания"
      subtitle="Создание заданий и наград."
      detail="Пока нет эндпоинтов CRUD для заданий/наград."
    />
  )
}

