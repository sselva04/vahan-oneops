import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth'
import Layout from './Layout'
import Login from './pages/Login'
import MyWork from './pages/MyWork'
import ChangePassword from './pages/ChangePassword'
import Users from './pages/admin/Users'
import Projects from './pages/admin/Projects'
import ProjectDetailPage from './pages/admin/ProjectDetail'
import type { ReactNode } from 'react'

function RequireAuth({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <p className="center muted">Loading…</p>
  return user ? <>{children}</> : <Navigate to="/login" replace />
}

function RequireAdmin({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  return user?.role === 'ADMIN' ? <>{children}</> : <Navigate to="/my-work" replace />
}

function Home() {
  const { user } = useAuth()
  return <Navigate to={user?.role === 'ADMIN' ? '/admin/projects' : '/my-work'} replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<RequireAuth><Layout /></RequireAuth>}>
        <Route path="/" element={<Home />} />
        <Route path="/my-work" element={<MyWork />} />
        <Route path="/password" element={<ChangePassword />} />
        <Route path="/admin/projects" element={<RequireAdmin><Projects /></RequireAdmin>} />
        <Route path="/admin/projects/:id" element={<RequireAdmin><ProjectDetailPage /></RequireAdmin>} />
        <Route path="/admin/users" element={<RequireAdmin><Users /></RequireAdmin>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
