import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { useAuth } from './auth'
import { api } from './api'

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const isAdmin = user?.role === 'ADMIN'
  const [importOn, setImportOn] = useState(false)
  // The import page only exists on the dev server; everywhere else this call fails and the link stays hidden.
  useEffect(() => { if (isAdmin) api.importStatus().then(s => setImportOn(s.enabled)).catch(() => setImportOn(false)) }, [isAdmin])

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">VAHAN <span>OneOps</span></div>
        <nav>
          {isAdmin && <NavLink to="/admin/projects">Projects</NavLink>}
          {isAdmin && <NavLink to="/admin/machine-hours">Machine hours</NavLink>}
          {isAdmin && <NavLink to="/admin/tools">Tools</NavLink>}
          {isAdmin && <NavLink to="/admin/rejections">Rejections</NavLink>}
          {isAdmin && <NavLink to="/admin/purchases">Purchases</NavLink>}
          {isAdmin && <NavLink to="/admin/reports">Reports</NavLink>}
          {isAdmin && <NavLink to="/admin/suppliers">Suppliers</NavLink>}
          {isAdmin && <NavLink to="/admin/customers">Customers</NavLink>}
          {isAdmin && <NavLink to="/admin/materials">Materials</NavLink>}
          {isAdmin && <NavLink to="/admin/users">Users</NavLink>}
          {isAdmin && importOn && <NavLink to="/admin/import">Import data</NavLink>}
          <NavLink to="/my-work">My work</NavLink>
        </nav>
        <div className="who">
          <NavLink to="/password" className="who-name">{user?.fullName}</NavLink>
          <button className="link" onClick={() => { logout(); navigate('/login') }}>Log out</button>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
