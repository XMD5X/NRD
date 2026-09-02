import React, { useEffect, useState } from 'react'
import client from '../api/client.js'
import { logger } from '../logging/logger.js'

export default function HistoryPage() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [downloadingId, setDownloadingId] = useState(null)

  useEffect(() => {
    load()
  }, [])

  function load() {
    setLoading(true)
    logger.trace('Загрузка истории выполнений', 'history')
    client.get('/executions')
      .then((res) => {
        setItems(res.data)
        logger.info(`Получено записей истории: ${res.data.length}`, 'history')
      })
      .catch((err) => {
        logger.warning(`Ошибка загрузки истории: ${err.message}`, 'history')
      })
      .finally(() => setLoading(false))
  }

  async function handleDownload(item) {
    // См. ExecutionPanel.jsx: скачивание идёт через авторизованный запрос + blob,
    // а не через прямую ссылку — иначе JWT-заголовок не уйдёт и backend ответит 403.
    setDownloadingId(item.id)
    try {
      const response = await client.get(`/executions/${item.id}/download`, {
        responseType: 'blob',
      })
      const disposition = response.headers['content-disposition']
      let filename = item.resultFileName || 'result'
      if (disposition) {
        const match = disposition.match(/filename="?([^"]+)"?/)
        if (match) filename = match[1]
      }
      const url = window.URL.createObjectURL(new Blob([response.data]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', filename)
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
      logger.info(`Скачан файл результата выполнения ${item.id} из истории`, 'history')
    } catch (err) {
      logger.warning(`Ошибка скачивания файла результата: ${err.message}`, 'history')
      alert('Ошибка скачивания файла результата')
    } finally {
      setDownloadingId(null)
    }
  }

  function formatDate(value) {
    if (!value) return '—'
    return new Date(value).toLocaleString('ru-RU')
  }

  return (
    <div>
      <h1>История генерации файлов</h1>
      <p className="hint">
        Все запуски задач всеми пользователями: кто, когда и что сгенерировал. Файлы результатов
        можно скачать прямо отсюда — это тот же архив, что впоследствии будет отдаваться
        на фронт (см. план по интеграции).
      </p>

      {loading && <p>Загрузка...</p>}
      {!loading && items.length === 0 && <p>Пока нет ни одного выполнения.</p>}

      {!loading && items.length > 0 && (
        <table className="table">
          <thead>
            <tr>
              <th>Когда</th>
              <th>Кто</th>
              <th>Задача</th>
              <th>Категория / банк</th>
              <th>Статус</th>
              <th>Файл</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id}>
                <td>{formatDate(item.startedAt)}</td>
                <td>{item.userLogin || '—'}</td>
                <td>{item.scriptName || '—'}</td>
                <td>
                  {item.category ? item.category : '—'}
                  {item.bankName ? ` — ${item.bankName}` : ''}
                </td>
                <td>{statusLabel(item.status)}</td>
                <td>
                  {item.hasResultFile ? (
                    <button disabled={downloadingId === item.id} onClick={() => handleDownload(item)}>
                      {downloadingId === item.id
                        ? 'Скачивание...'
                        : item.resultFileCount > 1
                          ? `Скачать (${item.resultFileCount}, zip)`
                          : 'Скачать'}
                    </button>
                  ) : (
                    <span className="hint">нет файла</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}

function statusLabel(status) {
  switch (status) {
    case 'RUNNING': return 'Выполняется'
    case 'GENERATED': return 'Готово'
    case 'FAILED': return 'Ошибка'
    case 'SENT': return 'Отправлено'
    default: return status
  }
}
