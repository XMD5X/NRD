// Файловое логирование фронтенда (INFO/DEBUG/WARNING/TRACE), см. HLD раздел 6.
// Логи накапливаются в буфере и батчами отправляются на backend (/api/logs),
// который дописывает их в файл на диске.

const LEVELS = ['TRACE', 'DEBUG', 'INFO', 'WARNING']
const LEVEL_PRIORITY = { TRACE: 0, DEBUG: 1, INFO: 2, WARNING: 3 }

let currentLevel = 'INFO'
let buffer = []
let flushTimer = null

export function setLogLevel(level) {
  if (LEVELS.includes(level)) {
    currentLevel = level
  }
}

export function getLogLevel() {
  return currentLevel
}

function shouldLog(level) {
  return LEVEL_PRIORITY[level] >= LEVEL_PRIORITY[currentLevel]
}

function push(level, message, context) {
  if (!shouldLog(level)) return
  buffer.push({
    level,
    message,
    context: context || '',
    timestamp: new Date().toISOString(),
  })
  scheduleFlush()
}

function scheduleFlush() {
  if (flushTimer) return
  flushTimer = setTimeout(flush, 3000)
}

async function flush() {
  flushTimer = null
  if (buffer.length === 0) return
  const batch = buffer
  buffer = []
  try {
    const token = localStorage.getItem('token')
    await fetch('/api/logs', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(batch),
    })
  } catch (e) {
    // Если backend недоступен — не блокируем UI, лог просто теряется.
    // eslint-disable-next-line no-console
    console.warn('Не удалось отправить логи на сервер', e)
  }
}

window.addEventListener('beforeunload', () => {
  if (buffer.length > 0) {
    const blob = new Blob([JSON.stringify(buffer)], { type: 'application/json' })
    navigator.sendBeacon('/api/logs', blob)
  }
})

export const logger = {
  trace: (message, context) => push('TRACE', message, context),
  debug: (message, context) => push('DEBUG', message, context),
  info: (message, context) => push('INFO', message, context),
  warning: (message, context) => push('WARNING', message, context),
}
