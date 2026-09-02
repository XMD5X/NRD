import React, { useState } from 'react'
import client from '../api/client.js'
import { logger } from '../logging/logger.js'

export default function ExecutionPanel({ script }) {
  const fields = (() => {
    try {
      return JSON.parse(script.parametersConfig || '[]')
    } catch {
      return []
    }
  })()

  const [values, setValues] = useState({})
  const [execution, setExecution] = useState(null)
  const [running, setRunning] = useState(false)
  const [sending, setSending] = useState(false)

  function updateField(name, value) {
    setValues((prev) => ({ ...prev, [name]: value }))
  }

  async function handleRun() {
    setRunning(true)
    setExecution(null)
    logger.debug(`Запуск скрипта ${script.name}`, 'execution')
    try {
      const res = await client.post(`/scripts/${script.id}/execute`, { parameters: values })
      setExecution(res.data)
      logger.info(`Скрипт ${script.name} выполнен со статусом ${res.data.status}`, 'execution')
    } catch (err) {
      logger.warning(`Ошибка запуска скрипта ${script.name}: ${err.message}`, 'execution')
      alert(err.response?.data?.error || 'Ошибка запуска скрипта')
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
      logger.info(`Результат выполнения ${execution.id} отправлен в целевую систему`, 'execution')
    } catch (err) {
      logger.warning(`Ошибка отправки результата: ${err.message}`, 'execution')
      alert(err.response?.data?.error || 'Ошибка отправки')
    } finally {
      setSending(false)
    }
  }

  async function handleDownload() {
    if (!execution) return
    // Прямая ссылка (window.open) не подходит: авторизация у нас через JWT
    // в заголовке Authorization, а не через cookie, поэтому при обычной
    // навигации браузер не передаёт токен и backend отвечает 403.
    // Поэтому скачиваем файл авторизованным запросом и отдаём как blob.
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
      <h2>Запуск: {script.name}</h2>
      {fields.map((f) => (
        <label key={f.name}>
          {f.label}
          <input value={values[f.name] || ''} onChange={(e) => updateField(f.name, e.target.value)} />
        </label>
      ))}
      <button disabled={running} onClick={handleRun}>
        {running ? 'Выполняется...' : 'Запустить'}
      </button>

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
