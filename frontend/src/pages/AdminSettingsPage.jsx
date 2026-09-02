import React, { useEffect, useState } from 'react'
import client from '../api/client.js'
import { setLogLevel as applyClientLogLevel } from '../logging/logger.js'
import { logger } from '../logging/logger.js'

const LEVELS = ['INFO', 'DEBUG', 'WARNING', 'TRACE']

function formatBytes(bytes) {
  if (bytes === null || bytes === undefined) return '—'
  if (bytes === 0) return '0 Б'
  const units = ['Б', 'КБ', 'МБ', 'ГБ', 'ТБ']
  const i = Math.min(units.length - 1, Math.floor(Math.log(bytes) / Math.log(1024)))
  const value = bytes / Math.pow(1024, i)
  return `${value.toFixed(i === 0 ? 0 : 1)} ${units[i]}`
}

function formatPercent(value) {
  return value === null || value === undefined ? '—' : `${value.toFixed(1)}%`
}

function UsageBar({ used, total }) {
  if (!total) return null
  const pct = Math.min(100, Math.max(0, (used / total) * 100))
  const color = pct > 90 ? 'var(--sibur-orange)' : 'var(--sibur-dna)'
  return (
    <div style={{ background: '#e5e7eb', borderRadius: 6, height: 8, marginTop: 6, overflow: 'hidden' }}>
      <div style={{ width: `${pct}%`, background: color, height: '100%' }} />
    </div>
  )
}

export default function AdminSettingsPage() {
  const [level, setLevel] = useState('INFO')
  const [saving, setSaving] = useState(false)

  const [metrics, setMetrics] = useState(null)
  const [frontendMetrics, setFrontendMetrics] = useState(null)
  const [metricsLoading, setMetricsLoading] = useState(false)
  const [metricsError, setMetricsError] = useState(null)
  const [downloadingLog, setDownloadingLog] = useState(null)

  useEffect(() => {
    client.get('/system/log-level').then((res) => {
      setLevel(res.data.level)
      applyClientLogLevel(res.data.level)
    })
    loadMetrics()
  }, [])

  async function handleChange(newLevel) {
    setSaving(true)
    try {
      const res = await client.put('/system/log-level', { level: newLevel })
      setLevel(res.data.level)
      applyClientLogLevel(res.data.level)
      logger.info(`Уровень логирования фронтенда изменён на ${res.data.level}`, 'settings')
    } finally {
      setSaving(false)
    }
  }

  function loadMetrics() {
    setMetricsLoading(true)
    setMetricsError(null)
    client.get('/system/metrics')
      .then((res) => setMetrics(res.data))
      .catch(() => setMetricsError('Не удалось получить метрики backend'))
      .finally(() => setMetricsLoading(false))

    // Отдаётся напрямую nginx (не через backend — см. nginx/generate-metrics.sh),
    // поэтому обычный fetch на тот же origin, а не client (baseURL=/api).
    // На уже развёрнутом стенде со старым образом frontend файла ещё нет —
    // это ожидаемо, просто не показываем строку "Фронтенд" в этом случае.
    fetch('/nginx-metrics.json')
      .then((res) => (res.ok ? res.json() : null))
      .then(setFrontendMetrics)
      .catch(() => setFrontendMetrics(null))
  }

  async function downloadLog(path, fallbackName, key) {
    setDownloadingLog(key)
    try {
      const res = await client.get(path, { responseType: 'blob' })
      const disposition = res.headers['content-disposition'] || ''
      const match = disposition.match(/filename\*?=(?:UTF-8'')?"?([^";]+)"?/i)
      const filename = match ? decodeURIComponent(match[1]) : fallbackName
      const url = window.URL.createObjectURL(new Blob([res.data]))
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      document.body.appendChild(a)
      a.click()
      a.remove()
      window.URL.revokeObjectURL(url)
      logger.info(`Выгружены логи: ${filename}`, 'settings')
    } catch (err) {
      alert('Не удалось скачать логи')
    } finally {
      setDownloadingLog(null)
    }
  }

  const b = metrics

  return (
    <div>
      <h1>Настройки системы</h1>

      <div className="card">
        <h2>Уровень файлового логирования фронтенда</h2>
        <p className="hint">
          По умолчанию — INFO. Для отладки переключите на DEBUG/WARNING/TRACE.
          Логи хранятся на сервере 7 дней, затем архивируются (см. HLD.md, раздел 6).
        </p>
        <select value={level} disabled={saving} onChange={(e) => handleChange(e.target.value)}>
          {LEVELS.map((l) => <option key={l} value={l}>{l}</option>)}
        </select>
      </div>

      <div className="card">
        <h2>Системные ресурсы</h2>
        <p className="hint">
          Нагрузка и место на диске backend-процесса. Метрики CPU/RAM контейнера
          nginx (frontend) сюда намеренно не включены — их получение потребовало
          бы доступа backend к Docker API, что после аудита безопасности сочли
          неоправданным риском; вместо этого ниже показан только размер собранной
          статики фронтенда (его считает сам nginx при старте).
        </p>
        {metricsError && <p className="error">{metricsError}</p>}
        {b && (
          <div className="metrics-grid">
            <div className="metric-tile">
              <div className="hint">CPU процесса backend</div>
              <div className="metric-value">{formatPercent(b.processCpuLoadPercent)}</div>
            </div>
            <div className="metric-tile">
              <div className="hint">CPU всего контейнера</div>
              <div className="metric-value">{formatPercent(b.systemCpuLoadPercent)}</div>
              <div className="hint">Ядер доступно: {b.availableProcessors}</div>
            </div>
            <div className="metric-tile">
              <div className="hint">Память (heap JVM)</div>
              <div className="metric-value">{formatBytes(b.heapUsedBytes)} / {formatBytes(b.heapMaxBytes)}</div>
              <UsageBar used={b.heapUsedBytes} total={b.heapMaxBytes} />
            </div>
            <div className="metric-tile">
              <div className="hint">Память контейнера</div>
              <div className="metric-value">{formatBytes(b.memoryUsedBytes)} / {formatBytes(b.memoryTotalBytes)}</div>
              <UsageBar used={b.memoryUsedBytes} total={b.memoryTotalBytes} />
            </div>
            <div className="metric-tile">
              <div className="hint">Диск (том данных)</div>
              <div className="metric-value">
                {formatBytes(b.diskTotalBytes && b.diskUsableBytes ? b.diskTotalBytes - b.diskUsableBytes : null)} / {formatBytes(b.diskTotalBytes)}
              </div>
              <UsageBar
                used={b.diskTotalBytes && b.diskUsableBytes ? b.diskTotalBytes - b.diskUsableBytes : null}
                total={b.diskTotalBytes}
              />
            </div>
            <div className="metric-tile">
              <div className="hint">База данных PostgreSQL</div>
              <div className="metric-value">{formatBytes(b.databaseBytes)}</div>
            </div>
            <div className="metric-tile">
              <div className="hint">Фронтенд (статика nginx)</div>
              <div className="metric-value">{frontendMetrics ? formatBytes(frontendMetrics.staticBytes) : 'н/д'}</div>
            </div>
          </div>
        )}
        {b && b.dataDirs && (
          <table className="table" style={{ marginTop: 16 }}>
            <thead><tr><th>Каталог данных backend</th><th>Путь</th><th>Занято</th></tr></thead>
            <tbody>
              {b.dataDirs.map((d) => (
                <tr key={d.path}>
                  <td>{d.label}</td>
                  <td className="hint">{d.path}</td>
                  <td>{formatBytes(d.bytes)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <button style={{ marginTop: 16 }} onClick={loadMetrics} disabled={metricsLoading}>
          {metricsLoading ? 'Обновляю…' : 'Обновить'}
        </button>
      </div>

      <div className="card">
        <h2>Выгрузка логов</h2>
        <p className="hint">
          Скачивание архивов с файловыми логами backend и фронтенда, а также
          журнала безопасности (входы + действия пользователей) в формате CEF
          для передачи в SIEM.
        </p>
        <div className="actions">
          <button disabled={downloadingLog === 'backend'}
                  onClick={() => downloadLog('/system/logs/backend', 'backend-logs.zip', 'backend')}>
            {downloadingLog === 'backend' ? 'Скачиваю…' : 'Логи backend'}
          </button>
          <button disabled={downloadingLog === 'frontend'}
                  onClick={() => downloadLog('/system/logs/frontend', 'frontend-logs.zip', 'frontend')}>
            {downloadingLog === 'frontend' ? 'Скачиваю…' : 'Логи фронтенда'}
          </button>
          <button disabled={downloadingLog === 'cef'}
                  onClick={() => downloadLog('/system/logs/security-cef', 'security-audit.cef.log', 'cef')}>
            {downloadingLog === 'cef' ? 'Скачиваю…' : 'Журнал безопасности (CEF)'}
          </button>
        </div>
      </div>
    </div>
  )
}
