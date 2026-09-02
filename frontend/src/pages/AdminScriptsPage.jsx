import React, { useEffect, useState } from 'react'
import client from '../api/client.js'
import { logger } from '../logging/logger.js'

export default function AdminScriptsPage() {
  const [scripts, setScripts] = useState([])
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [scriptType, setScriptType] = useState('BASH')
  const [parametersConfig, setParametersConfig] = useState('[]')
  const [visibleToRole, setVisibleToRole] = useState('')
  const [file, setFile] = useState(null)

  function load() {
    client.get('/scripts').then((res) => setScripts(res.data))
  }

  useEffect(load, [])

  async function handleUpload(e) {
    e.preventDefault()
    if (!file) return
    const formData = new FormData()
    formData.append('file', file)
    formData.append('name', name)
    formData.append('description', description)
    formData.append('scriptType', scriptType)
    formData.append('parametersConfig', parametersConfig)
    if (visibleToRole) formData.append('visibleToRole', visibleToRole)
    try {
      await client.post('/scripts', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
      logger.info(`Добавлен скрипт ${name}`, 'admin-scripts')
      setName('')
      setDescription('')
      setParametersConfig('[]')
      setFile(null)
      load()
    } catch (err) {
      alert(err.response?.data?.error || 'Ошибка загрузки скрипта')
    }
  }

  async function toggle(script) {
    await client.patch(`/scripts/${script.id}/toggle`)
    logger.info(`Переключена активность скрипта ${script.name}`, 'admin-scripts')
    load()
  }

  return (
    <div>
      <h1>Управление скриптами</h1>

      <form className="card" onSubmit={handleUpload}>
        <h2>Добавить скрипт</h2>
        <p className="hint">
          Скрипт должен принимать параметры как позиционные CLI-аргументы (не через
          интерактивный ввод Read-Host и подобное) — см. BACKEND.md, раздел "Требования к скриптам".
        </p>
        <label>Название<input value={name} onChange={(e) => setName(e.target.value)} /></label>
        <label>Описание<textarea value={description} onChange={(e) => setDescription(e.target.value)} /></label>
        <label>
          Тип скрипта
          <select value={scriptType} onChange={(e) => setScriptType(e.target.value)}>
            <option value="BASH">Bash</option>
            <option value="PYTHON">Python</option>
            <option value="POWERSHELL">PowerShell</option>
          </select>
        </label>
        <label>
          Видимость (роль)
          <select value={visibleToRole} onChange={(e) => setVisibleToRole(e.target.value)}>
            <option value="">Все роли</option>
            <option value="ADMIN">Только администратор</option>
            <option value="BUSINESS_USER">Только бизнес-пользователь</option>
          </select>
        </label>
        <label>
          Конфигурация параметров (JSON, настраивается разработчиком)
          <textarea value={parametersConfig} onChange={(e) => setParametersConfig(e.target.value)} />
        </label>
        <label>Файл скрипта<input type="file" onChange={(e) => setFile(e.target.files[0])} /></label>
        <button type="submit">Загрузить скрипт</button>
      </form>

      <table className="table">
        <thead><tr><th>Название</th><th>Тип</th><th>Статус</th><th>Действия</th></tr></thead>
        <tbody>
          {scripts.map((s) => (
            <tr key={s.id}>
              <td>{s.name}</td>
              <td>{s.scriptType}</td>
              <td>{s.active ? 'Активен' : 'Отключён'}</td>
              <td><button onClick={() => toggle(s)}>{s.active ? 'Отключить' : 'Включить'}</button></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
