import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { logger } from '../logging/logger.js'
import SiburLogo from '../components/SiburLogo.jsx'

export default function LoginPage() {
  const [login, setLogin] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const { login: doLogin } = useAuth()
  const navigate = useNavigate()

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    logger.debug(`Попытка входа пользователя ${login}`, 'auth')
    try {
      await doLogin(login, password)
      navigate('/')
    } catch (err) {
      const message = err.response?.data?.error || 'Ошибка входа'
      setError(message)
      logger.warning(`Неудачная попытка входа: ${message}`, 'auth')
    }
  }

  return (
    <div className="centered-form">
      <form className="card" onSubmit={handleSubmit}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
          <SiburLogo size={36} />
          <div>
            <div style={{ fontWeight: 700, fontSize: 18, color: 'var(--sibur-dark-green)' }}>SIBUR</div>
            <div className="hint" style={{ marginTop: -2 }}>Админ-панель поддержки</div>
          </div>
        </div>
        <h1>Вход в систему</h1>
        <p className="hint">
          MVP: вход по логину/паролю (временное решение до перехода на AD).
        </p>
        <label>
          Логин
          <input value={login} onChange={(e) => setLogin(e.target.value)} autoFocus />
        </label>
        <label>
          Пароль
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </label>
        {error && <div className="error">{error}</div>}
        <button type="submit">Войти</button>
        <p className="hint">
          Демо-учётки: <code>admin / admin12345</code> (Администратор),{' '}
          <code>business / business12345</code> (Бизнес-пользователь)
        </p>
      </form>
    </div>
  )
}
