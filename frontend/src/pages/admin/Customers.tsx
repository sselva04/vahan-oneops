import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api'
import { ErrorNote, errorText } from '../../components'
import type { Customer } from '../../types'

/** The customers Vahan works for. Each project has one fixed customer. */
export default function Customers() {
  const [items, setItems] = useState<Customer[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [newName, setNewName] = useState('')
  const [editing, setEditing] = useState<number | null>(null)
  const [editName, setEditName] = useState('')
  const [showInactive, setShowInactive] = useState(false)

  const load = useCallback(() => {
    api.customers(true).then(setItems).catch(e => setError(errorText(e)))
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

  const add = (e: FormEvent) => {
    e.preventDefault()
    void run(async () => { await api.createCustomer(newName.trim()); setNewName('') })
  }

  const shown = (items ?? []).filter(c => showInactive || c.active)

  return (
    <div>
      <h1>Customers</h1>
      <p className="muted">Each project has one customer, chosen when the project is created.</p>
      <ErrorNote message={error} />

      <form className="card inline-form" onSubmit={add}>
        <h2>Add customer</h2>
        <div className="grid">
          <label>Customer name<input value={newName} onChange={e => setNewName(e.target.value)} maxLength={120} required /></label>
        </div>
        <button className="primary">Add</button>
      </form>

      <div className="card filters">
        <label className="check">
          <input type="checkbox" checked={showInactive} onChange={e => setShowInactive(e.target.checked)} />
          Show deactivated
        </label>
      </div>

      <div className="card table-wrap">
        <table>
          <thead><tr><th>Customer</th><th>Status</th><th></th></tr></thead>
          <tbody>
            {shown.map(c => (
              <tr key={c.id} className={c.active ? '' : 'inactive'}>
                <td>
                  {editing === c.id
                    ? <input value={editName} maxLength={120} onChange={e => setEditName(e.target.value)} />
                    : <strong>{c.name}</strong>}
                </td>
                <td>{c.active ? 'Active' : 'Deactivated'}</td>
                <td className="actions">
                  {editing === c.id ? <>
                    <button className="link" onClick={() => { void run(async () => { await api.renameCustomer(c.id, editName.trim()); setEditing(null) }) }}>Save</button>
                    <button className="link" onClick={() => setEditing(null)}>Cancel</button>
                  </> : <>
                    <button className="link" onClick={() => { setEditing(c.id); setEditName(c.name) }}>Rename</button>
                    <button className="link" onClick={() => { void run(() => api.setCustomerActive(c.id, !c.active)) }}>
                      {c.active ? 'Deactivate' : 'Activate'}
                    </button>
                  </>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {items && shown.length === 0 && <div className="empty">No customers.</div>}
      </div>
    </div>
  )
}
