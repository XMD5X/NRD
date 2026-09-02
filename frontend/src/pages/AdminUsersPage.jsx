import React, { useEffect, useState } from 'react'
import client from '../api/client.js'
import { logger } from '../logging/logger.js'

export default function AdminUsersPage() {
  const [users, setUsers] = useState([])
  const [newLogin, setNewLogin] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [newRole, setNewRole] = useState('BUSINESS_USER')
  const [history, setHistory] = useState(null)
  const [historyUserLogin, setHistoryUserLogin] = useState('')

  function loadUsers() {
    client.get('/users').then((res) => setUsers(res.data))
  }

  useEffect(loadUsers, [])

  async function handleCreate(e) {
    e.preventDefault()
    try {
      await client.post('/users', { login: newLogin, password: newPassword, role: newRole })
      logger.info(`Создан пользователь ${newLogin} с ролью ${newRole}`, 'admin-users')
      setNewLogin('')
      setNewPassword('')
      loadUsers()
    } catch (err) {
      alert(err.response?.data?.error || 'Ошибка создания пользователя')
    }
  }

  async function handleBlock(user) {
    const reason = window.prompt('Причина блокировки:', '')
    if (reason === null) return
    await client.post(`/users/${user.id}/block`, { reason })
    logger.info(`Заблокирован пользователь ${user.login}`, 'admin-users')
    loadUsers()
  }

  async function handleUnblock(user) {
    await client.post(`/users/${user.id}/unblock`)
    logger.info(`Разблокирован пользователь ${user.login}`, 'admin-users')
    loadUsers()
  }

  async function handleHistory(user) {
    const res = await client.get(`/users/${user.id}/history`)
    setHistory(res.data)
    setHistoryUserLogin(user.login)
  }

  return (
    <div>
      <h1>Управление пользователями</h1>

      <form className="card" onSubmit={handleCreate}>
        <h2>Новый пользователь</h2>
        <label>Логин<input value={newLogin} onChange={(e) => setNewLogin(e.target.value)} /></label>
        <label>Пароль<input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} /></label>
        <label>
          Роль
          <select value={newRole} onChange={(e) => setNewRole(e.target.value)}>
            <option value="BUSINESS_USER">Бизнес-пользователь</option>
            <option value="ADMIN">Администратор</option>
          </select>
        </label>
        <button type="submit">Создать</button>
      </form>

      <table className="table">
        <thead>
          <tr><th>Логин</th><th>Роль</th><th>Статус</th><th>Действия</th></tr>
        </thead>
        <tbody>
          {users.map((u) => (
            <tr key={u.id}>
              <td>{u.login}</td>
              <td>{u.role}</td>
              <td>{u.blocked ? `Заблокирован (${u.blockedReason || ''})` : 'Активен'}</td>
              <td className="actions">
                {u.blocked ? (
                  <button onClick={() => handleUnblock(u)}>Разблокировать</button>
                ) : (
                  <button onClick={() => handleBlock(u)}>Заблокировать</button>
                )}
                <button onClick={() => handleHistory(u)}>История</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {history && (
        <div className="card">
          <h2>История: {historyUserLogin}</h2>
          <h3>Авторизации</h3>
          <table className="table">
            <thead><tr><th>Дата</th><th>Успех</th><th>IP</th></tr></thead>
            <tbody>
              {history.loginHistory.map((h) => (
                <tr key={h.id}>
                  <td>{new Date(h.attemptedAt).toLocaleString('ru-RU')}</td>
                  <td>{h.success ? 'Да' : 'Нет'}</td>
                  <td>{h.ipAddress}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <h3>Действия</h3>
          <table className="table">
            <thead><tr><th>Дата</th><th>Тип</th><th>Детали</th></tr></thead>
            <tbody>
              {history.actionHistory.map((h) => (
                <tr key={h.id}>
                  <td>{new Date(h.createdAt).toLocaleString('ru-RU')}</td>
                  <td>{h.actionType}</td>
                  <td>{h.details}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
