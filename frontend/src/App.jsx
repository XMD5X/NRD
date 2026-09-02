import React from 'react'
import { Routes, Route } from 'react-router-dom'
import Layout from './components/Layout.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import LoginPage from './pages/LoginPage.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import TaskDetailPage from './pages/TaskDetailPage.jsx'
import UploadPage from './pages/UploadPage.jsx'
import HistoryPage from './pages/HistoryPage.jsx'
import AdminUsersPage from './pages/AdminUsersPage.jsx'
import AdminScriptsPage from './pages/AdminScriptsPage.jsx'
import AdminSettingsPage from './pages/AdminSettingsPage.jsx'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={
        <ProtectedRoute>
          <Layout><DashboardPage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/tasks/access-rights" element={
        <ProtectedRoute>
          <Layout><TaskDetailPage mode="access-rights" /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/tasks/category/:categoryName" element={
        <ProtectedRoute>
          <Layout><TaskDetailPage mode="category" /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/tasks/script/:scriptId" element={
        <ProtectedRoute>
          <Layout><TaskDetailPage mode="script" /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/uploads" element={
        <ProtectedRoute>
          <Layout><UploadPage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/history" element={
        <ProtectedRoute>
          <Layout><HistoryPage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/admin/users" element={
        <ProtectedRoute adminOnly>
          <Layout><AdminUsersPage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/admin/scripts" element={
        <ProtectedRoute adminOnly>
          <Layout><AdminScriptsPage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/admin/settings" element={
        <ProtectedRoute adminOnly>
          <Layout><AdminSettingsPage /></Layout>
        </ProtectedRoute>
      } />
    </Routes>
  )
}
