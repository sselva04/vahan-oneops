import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api'
import { ErrorNote, errorText } from '../../components'
import { WorkList } from '../MyWork'
import type { Role, User, WorkItem } from '../../types'

export default function Users() {
  const [users, setUsers] = useState<User[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [viewing, setViewing] = useState<{ user: User; items: WorkItem[] } | null>(null)

  const load = useCallback(() => {
    api.users().then(setUsers).catch(e => setError(errorText(e)))
  }, [])
  useEffect(load, [load])

  const run = async (action: () => Promise<unknown>) => {
    setError(null)
    try {
      await action()
      load()
    } catch (e) {
      setError(errorText(e))
    }
  }

  const resetPassword = (u: User) => {
    const pw = window.prompt(`New password for ${u.fullName} (at least 8 characters):`)
    if (pw) run(() => api.resetPassword(u.id, pw))
  }

  const viewWork = async (u: User) => {
    setError(null)
    try {
      setViewing({ user: u, items: await api.userWork(u.id) })
    } catch (e) {
      setError(errorText(e))
    }
  }

  return (
    <div>
      <h1>Users</h1>
      <ErrorNote message={error} />
      <AddUser onCreated={load} />

      <div className="card table-wrap">
        <table>
          <thead>
            <tr><th>Name</th><th>Username</th><th>Role</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            {users?.map(u => (
              <tr key={u.id} className={u.active ? '' : 'inactive'}>
                <td>{u.fullName}</td>
                <td>{u.username}</td>
                <td>{u.role === 'ADMIN' ? 'Admin' : 'Employee'}</td>
                <td>{u.active ? 'Active' : 'Deactivated'}</td>
                <td className="actions">
                  <button className="link" onClick={() => viewWork(u)}>View work</button>
                  <button className="link" onClick={() => resetPassword(u)}>Reset password</button>
                  <button className="link" onClick={() => run(() => api.updateUser(u.id, { active: !u.active }))}>
                    {u.active ? 'Deactivate' : 'Activate'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {viewing && (
        <section className="panel">
          <div className="panel-head">
            <h2>Work allocated to {viewing.user.fullName}</h2>
            <button className="link" onClick={() => setViewing(null)}>Close</button>
          </div>
          <WorkList items={viewing.items} />
        </section>
      )}
    </div>
  )
}

function AddUser({ onCreated }: { onCreated: () => void }) {
  const [username, setUsername] = useState('')
  const [fullName, setFullName] = useState('')
  const [role, setRole] = useState<Role>('EMPLOYEE')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    try {
      await api.createUser({ username, fullName, role, password })
      setUsername(''); setFullName(''); setPassword(''); setRole('EMPLOYEE')
      onCreated()
    } catch (err) {
      setError(errorText(err))
    }
  }

  return (
    <form className="card inline-form" onSubmit={submit}>
      <h2>Add user</h2>
      <div className="grid">
        <label>Full name<input value={fullName} onChange={e => setFullName(e.target.value)} required /></label>
        <label>Username<input value={username} onChange={e => setUsername(e.target.value)} minLength={3} required /></label>
        <label>Role
          <select value={role} onChange={e => setRole(e.target.value as Role)}>
            <option value="EMPLOYEE">Employee</option>
            <option value="ADMIN">Admin</option>
          </select>
        </label>
        <label>Temporary password<input type="text" value={password} onChange={e => setPassword(e.target.value)} minLength={8} required /></label>
      </div>
      <ErrorNote message={error} />
      <button className="primary">Add user</button>
    </form>
  )
}
