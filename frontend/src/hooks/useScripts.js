import { useEffect, useState } from 'react'
import client from '../api/client.js'
import { logger } from '../logging/logger.js'

// Общая загрузка списка доступных задач (скриптов) — используется и на
// плитках (DashboardPage), и на странице конкретной задачи (TaskDetailPage),
// т.к. это отдельный маршрут и список нужен там заново.
export default function useScripts() {
  const [scripts, setScripts] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  useEffect(() => {
    logger.trace('Загрузка списка доступных задач', 'dashboard')
    client.get('/scripts')
      .then((res) => {
        setScripts(res.data)
        logger.info(`Получено задач: ${res.data.length}`, 'dashboard')
      })
      .catch((err) => {
        // Без .catch() запрос, упавший по любой причине (истёкшая сессия, сеть,
        // ошибка сервера), оставлял бы страницу висеть на "Загрузка..." навсегда.
        // При 401 interceptor в api/client.js сам перенаправит на /login.
        logger.warning(`Ошибка загрузки списка задач: ${err.message}`, 'dashboard')
        setLoadError('Не удалось загрузить список задач. Попробуйте обновить страницу.')
      })
      .finally(() => setLoading(false))
  }, [])

  return { scripts, loading, loadError }
}
