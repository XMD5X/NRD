import React from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

export default function ProtectedRoute({ children, adminOnly = false }) {
  const { auth } = useAuth()
  if (!auth) {
    return <Navigate to="/login" replace />
  }
  if (adminOnly && auth.role !== 'ADMIN') {
    return <Navigate to="/" replace />
  }
  return children
}
