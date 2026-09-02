import React from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import SiburLogo from './SiburLogo.jsx'

export default function Layout({ children }) {
  const { auth, logout, isAdmin } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <div className="layout">
      <header className="topbar">
        <div className="brand">
          <span className="brand-mark">
            <SiburLogo height={22} />
          </span>
          <span className="brand-sub">НРД - Бизнес панель</span>
        </div>
        {auth && (
          <nav className="nav">
            <Link to="/">Задачи</Link>
            <Link to="/history">История</Link>
            <Link to="/uploads">Загрузка файлов</Link>
            {isAdmin && <Link to="/admin/users">Пользователи</Link>}
            {isAdmin && <Link to="/admin/scripts">Скрипты</Link>}
            {isAdmin && <Link to="/admin/settings">Настройки</Link>}
            <span className="user-chip">{auth.login} ({auth.role})</span>
            <button onClick={handleLogout}>Выйти</button>
          </nav>
        )}
      </header>
      <main className="content">{children}</main>
    </div>
  )
}
