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

  const [editingScript, setEditingScript] = useState(null)
  const [editingContent, setEditingContent] = useState('')
  const [loadingContent, setLoadingContent] = useState(false)
  const [savingContent, setSavingContent] = useState(false)

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

  async function openEdit(script) {
    setLoadingContent(true)
    try {
      const res = await client.get(`/scripts/${script.id}/content`)
      setEditingContent(res.data.content)
      setEditingScript(script)
    } catch (err) {
      alert(err.response?.data?.error || 'Не удалось открыть скрипт на редактирование')
    } finally {
      setLoadingContent(false)
    }
  }

  function closeEdit() {
    setEditingScript(null)
    setEditingContent('')
  }

  async function saveEdit() {
    setSavingContent(true)
    try {
      await client.put(`/scripts/${editingScript.id}/content`, { content: editingContent })
      logger.info(`Отредактирован скрипт ${editingScript.name}`, 'admin-scripts')
      closeEdit()
    } catch (err) {
      alert(err.response?.data?.error || 'Ошибка сохранения скрипта')
    } finally {
      setSavingContent(false)
    }
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
              <td className="actions">
                <button onClick={() => toggle(s)}>{s.active ? 'Отключить' : 'Включить'}</button>
                <button onClick={() => openEdit(s)} disabled={loadingContent}>Редактировать</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {editingScript && (
        <div className="card">
          <h2>Редактирование: {editingScript.name}</h2>
          <p className="hint">
            Изменения применяются сразу к файлу на диске — следующий запуск задачи
            будет использовать уже отредактированную версию. Предыдущая версия файла
            сохраняется рядом с расширением .bak (только последняя, не полная история).
          </p>
          <textarea
            value={editingContent}
            onChange={(e) => setEditingContent(e.target.value)}
            spellCheck={false}
            style={{ minHeight: 420, fontFamily: 'monospace', fontSize: 13 }}
          />
          <div className="actions" style={{ marginTop: 12 }}>
            <button onClick={saveEdit} disabled={savingContent}>
              {savingContent ? 'Сохраняю…' : 'Сохранить'}
            </button>
            <button onClick={closeEdit} disabled={savingContent}>Отмена</button>
          </div>
        </div>
      )}
    </div>
  )
}
