import React, { useEffect, useState } from 'react'
import client from '../api/client.js'
import { setLogLevel as applyClientLogLevel } from '../logging/logger.js'
import { logger } from '../logging/logger.js'

const LEVELS = ['INFO', 'DEBUG', 'WARNING', 'TRACE']

export default function AdminSettingsPage() {
  const [level, setLevel] = useState('INFO')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    client.get('/system/log-level').then((res) => {
      setLevel(res.data.level)
      applyClientLogLevel(res.data.level)
    })
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
    </div>
  )
}
