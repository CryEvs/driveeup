import { AdminPlaceholderPage } from './AdminPlaceholderPage.jsx'

export function AdminStorePage({ user }) {
  return (
    <AdminPlaceholderPage
      user={user}
      title="Админ: Магазин за DriveCoin"
      subtitle="Товары и покупка (списание баланса)."
      detail="Пока нет эндпоинтов для управления товарами/заказами."
    />
  )
}

