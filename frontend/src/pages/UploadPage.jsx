import React, { useEffect, useState } from 'react'
import client from '../api/client.js'
import { logger } from '../logging/logger.js'

const FILE_TYPES = [
  { value: 'CERTIFICATE', label: 'Сертификат' },
  { value: 'ACCOUNT', label: 'Счёт' },
  { value: 'PERMISSION', label: 'Полномочия' },
  { value: 'OTHER', label: 'Другое' },
]

export default function UploadPage() {
  const [file, setFile] = useState(null)
  const [fileType, setFileType] = useState('CERTIFICATE')
  const [uploads, setUploads] = useState([])
  const [message, setMessage] = useState('')

  function loadUploads() {
    client.get('/uploads/mine').then((res) => setUploads(res.data))
  }

  useEffect(loadUploads, [])

  async function handleUpload(e) {
    e.preventDefault()
    if (!file) return
    const formData = new FormData()
    formData.append('file', file)
    formData.append('fileType', fileType)
    try {
      await client.post('/uploads', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
      setMessage('Файл успешно загружен')
      logger.info(`Загружен файл типа ${fileType}: ${file.name}`, 'uploads')
      setFile(null)
      loadUploads()
    } catch (err) {
      logger.warning(`Ошибка загрузки файла: ${err.message}`, 'uploads')
      setMessage('Ошибка загрузки файла')
    }
  }

  return (
    <div>
      <h1>Загрузка сертификатов, счетов, полномочий</h1>
      <form className="card" onSubmit={handleUpload}>
        <label>
          Тип файла
          <select value={fileType} onChange={(e) => setFileType(e.target.value)}>
            {FILE_TYPES.map((t) => (
              <option key={t.value} value={t.value}>{t.label}</option>
            ))}
          </select>
        </label>
        <label>
          Файл
          <input type="file" onChange={(e) => setFile(e.target.files[0])} />
        </label>
        <button type="submit">Загрузить</button>
        {message && <p className="hint">{message}</p>}
      </form>

      <h2>Мои загруженные файлы</h2>
      <table className="table">
        <thead>
          <tr><th>Тип</th><th>Имя файла</th><th>Дата</th></tr>
        </thead>
        <tbody>
          {uploads.map((u) => (
            <tr key={u.id}>
              <td>{u.fileType}</td>
              <td>{u.originalName}</td>
              <td>{new Date(u.uploadedAt).toLocaleString('ru-RU')}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
