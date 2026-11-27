import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from './auth'

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const isAdmin = user?.role === 'ADMIN'

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">VAHAN <span>OneOps</span></div>
        <nav>
          {isAdmin && <NavLink to="/admin/projects">Projects</NavLink>}
          {isAdmin && <NavLink to="/admin/rejections">Rejections</NavLink>}
          {isAdmin && <NavLink to="/admin/suppliers">Suppliers</NavLink>}
          {isAdmin && <NavLink to="/admin/customers">Customers</NavLink>}
          {isAdmin && <NavLink to="/admin/materials">Materials</NavLink>}
          {isAdmin && <NavLink to="/admin/users">Users</NavLink>}
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
