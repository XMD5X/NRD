import React, { createContext, useContext, useState } from 'react'
import client from '../api/client.js'
import { logger } from '../logging/logger.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => {
    const token = localStorage.getItem('token')
    const role = localStorage.getItem('role')
    const login = localStorage.getItem('login')
    return token ? { token, role, login } : null
  })

  async function login(loginValue, password) {
    const response = await client.post('/auth/login', { login: loginValue, password })
    const { token, role, login: userLogin } = response.data
    localStorage.setItem('token', token)
    localStorage.setItem('role', role)
    localStorage.setItem('login', userLogin)
    setAuth({ token, role, login: userLogin })
    logger.info('Успешный вход в систему', 'auth')
  }

  function logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('login')
    setAuth(null)
    logger.info('Выход из системы', 'auth')
  }

  return (
    <AuthContext.Provider value={{ auth, login, logout, isAdmin: auth?.role === 'ADMIN' }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
