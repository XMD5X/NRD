import React, { useState } from 'react'
import client from '../api/client.js'
import { logger } from '../logging/logger.js'

// Аналог ExecutionPanel.jsx, но для режима "Все банки" задачи "Выдача прав
// доступа...": вместо одного банка + списка счетов пользователь загружает
// Excel-файл "Счёт / Банк" — backend сам подбирает нужный скрипт под банк
// каждой строки (см. ExecutionController/ExecutionService.executeBatch на бэке).
export default function BulkAccessPanel({ category }) {
  const [userId, setUserId] = useState('')
  const [file, setFile] = useState(null)
  const [execution, setExecution] = useState(null)
  const [running, setRunning] = useState(false)
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')
  const [downloadingTemplate, setDownloadingTemplate] = useState(false)

  async function handleDownloadTemplate() {
    setDownloadingTemplate(true)
    try {
      const response = await client.get('/scripts/execute-batch/template', {
        params: { category },
        responseType: 'blob',
      })
      const disposition = response.headers['content-disposition']
      let filename = 'Шаблон_счета_банки.xlsx'
      if (disposition) {
        const match = disposition.match(/filename\*?=(?:UTF-8'')?"?([^";]+)"?/)
        if (match) filename = decodeURIComponent(match[1])
      }
      const url = window.URL.createObjectURL(new Blob([response.data]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', filename)
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
      logger.info(`Скачан шаблон Excel для роли "${category}"`, 'execution')
    } catch (err) {
      logger.warning(`Ошибка скачивания шаблона Excel: ${err.message}`, 'execution')
      alert('Ошибка скачивания шаблона Excel')
    } finally {
      setDownloadingTemplate(false)
    }
  }

  async function handleRun() {
    if (!file) {
      setError('Выберите файл Excel со счетами и банками')
      return
    }
    setError('')
    setRunning(true)
    setExecution(null)
    const formData = new FormData()
    formData.append('category', category)
    formData.append('userId', userId)
    formData.append('file', file)
    logger.debug(`Запуск массовой выдачи прав доступа ("все банки") для роли ${category}`, 'execution')
    try {
      const res = await client.post('/scripts/execute-batch', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      setExecution(res.data)
      logger.info(`Массовый запуск для роли ${category} выполнен со статусом ${res.data.status}`, 'execution')
    } catch (err) {
      logger.warning(`Ошибка массового запуска для роли ${category}: ${err.message}`, 'execution')
      setError(err.response?.data?.error || 'Ошибка запуска')
    } finally {
      setRunning(false)
    }
  }

  async function handleSend() {
    if (!execution) return
    setSending(true)
    try {
      const res = await client.post(`/executions/${execution.id}/send`)
      setExecution(res.data)
      logger.info(`Результат массового выполнения ${execution.id} отправлен в целевую систему`, 'execution')
    } catch (err) {
      logger.warning(`Ошибка отправки результата: ${err.message}`, 'execution')
      alert(err.response?.data?.error || 'Ошибка отправки')
    } finally {
      setSending(false)
    }
  }

  async function handleDownload() {
    if (!execution) return
    try {
      const response = await client.get(`/executions/${execution.id}/download`, {
        responseType: 'blob',
      })
      const disposition = response.headers['content-disposition']
      let filename = 'result'
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
      logger.info(`Скачан файл результата выполнения ${execution.id}`, 'execution')
    } catch (err) {
      logger.warning(`Ошибка скачивания файла результата: ${err.message}`, 'execution')
      alert('Ошибка скачивания файла результата')
    }
  }

  return (
    <div className="card">
      <h2>Запуск: {category} — все банки</h2>
      <label>
        User ID
        <input value={userId} onChange={(e) => setUserId(e.target.value)} />
      </label>
      <label>
        Excel со счетами и банками
        <input
          type="file"
          accept=".xlsx,.xls"
          onChange={(e) => setFile(e.target.files[0] || null)}
        />
      </label>
      <p className="hint">
        Столбец "Счет" — номер счёта, столбец "Банк" — банк из списка этой роли. Для
        каждой строки будет автоматически выбран нужный скрипт (роль + банк).
      </p>
      <div className="actions">
        <button disabled={running} onClick={handleRun}>
          {running ? 'Выполняется...' : 'Запустить'}
        </button>
        <button disabled={downloadingTemplate} onClick={handleDownloadTemplate}>
          {downloadingTemplate ? 'Скачивание...' : 'Шаблон Excel (скачать)'}
        </button>
      </div>
      {error && <p className="error">{error}</p>}

      {execution && (
        <div className={`status-box status-${execution.status.toLowerCase()}`}>
          <p><strong>Статус:</strong> {statusLabel(execution.status)}</p>
          {execution.stderr && <pre className="log-output">{execution.stderr}</pre>}
          <div className="actions">
            {execution.hasResultFile && (
              <button onClick={handleDownload}>
                {execution.resultFileCount > 1
                  ? `Скачать файлы результата (${execution.resultFileCount}, zip)`
                  : 'Скачать файл результата'}
              </button>
            )}
            {execution.status === 'GENERATED' && (
              <button disabled={sending} onClick={handleSend}>
                {sending ? 'Отправка...' : 'Отправить'}
              </button>
            )}
            {execution.status === 'SENT' && <span className="badge badge-success">Отправлено в целевую систему</span>}
          </div>
        </div>
      )}
    </div>
  )
}

function statusLabel(status) {
  switch (status) {
    case 'RUNNING': return 'Выполняется'
    case 'GENERATED': return 'Успешно (готово к отправке)'
    case 'FAILED': return 'Ошибка'
    case 'SENT': return 'Отправлено'
    default: return status
  }
}
