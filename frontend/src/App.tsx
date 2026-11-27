import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth'
import Layout from './Layout'
import Login from './pages/Login'
import MyWork from './pages/MyWork'
import ChangePassword from './pages/ChangePassword'
import Users from './pages/admin/Users'
import Projects from './pages/admin/Projects'
import Customers from './pages/admin/Customers'
import MachineHours from './pages/admin/MachineHours'
import Materials from './pages/admin/Materials'
import Tools from './pages/admin/Tools'
import Purchases from './pages/admin/Purchases'
import Rejections from './pages/admin/Rejections'
import Reports from './pages/admin/Reports'
import ImportData from './pages/admin/ImportData'
import Suppliers from './pages/admin/Suppliers'
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
        <Route path="/admin/suppliers" element={<RequireAdmin><Suppliers /></RequireAdmin>} />
        <Route path="/admin/materials" element={<RequireAdmin><Materials /></RequireAdmin>} />
        <Route path="/admin/customers" element={<RequireAdmin><Customers /></RequireAdmin>} />
        <Route path="/admin/rejections" element={<RequireAdmin><Rejections /></RequireAdmin>} />
        <Route path="/admin/purchases" element={<RequireAdmin><Purchases /></RequireAdmin>} />
        <Route path="/admin/reports" element={<RequireAdmin><Reports /></RequireAdmin>} />
        <Route path="/admin/import" element={<RequireAdmin><ImportData /></RequireAdmin>} />
        <Route path="/admin/machine-hours" element={<RequireAdmin><MachineHours /></RequireAdmin>} />
        <Route path="/admin/tools" element={<RequireAdmin><Tools /></RequireAdmin>} />
        <Route path="/admin/users" element={<RequireAdmin><Users /></RequireAdmin>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
