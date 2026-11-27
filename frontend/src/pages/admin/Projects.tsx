import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../../api'
import { ErrorNote, errorText } from '../../components'
import type { Customer, ProjectSummary } from '../../types'

export default function Projects() {
  const navigate = useNavigate()
  const [projects, setProjects] = useState<ProjectSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [drawingNo, setDrawingNo] = useState('')
  const [name, setName] = useState('')
  const [customers, setCustomers] = useState<Customer[]>([])
  const [customerId, setCustomerId] = useState<number | ''>('')

  useEffect(() => {
    api.projects().then(setProjects).catch(e => setError(errorText(e)))
    api.customers(false).then(setCustomers).catch(e => setError(errorText(e)))
  }, [])

  const create = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    try {
      const p = await api.createProject(drawingNo, name, customerId === '' ? null : customerId)
      navigate(`/admin/projects/${p.id}`)
    } catch (err) {
      setError(errorText(err))
    }
  }

  return (
    <div>
      <h1>Projects</h1>
      <form className="card inline-form" onSubmit={create}>
        <h2>New project</h2>
        <div className="grid">
          <label>Project (drawing) number
            <input value={drawingNo} onChange={e => setDrawingNo(e.target.value)} placeholder="VDE-0324-08-00-00" required />
          </label>
          <label>Project name
            <input value={name} onChange={e => setName(e.target.value)} required />
          </label>
          <label>Customer
            <select value={customerId} onChange={e => setCustomerId(e.target.value ? Number(e.target.value) : '')} required>
              <option value="">— choose —</option>
              {customers.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </label>
        </div>
        <ErrorNote message={error} />
        <button className="primary">Create project</button>
      </form>

      <div className="card table-wrap">
        <table>
          <thead>
            <tr><th>Drawing no.</th><th>Project</th><th>Customer</th><th>Parts</th><th>Progress</th><th>Not allocated</th></tr>
          </thead>
          <tbody>
            {projects?.map(p => {
              const pct = p.stageCount === 0 ? 0 : Math.round((p.stagesDone / p.stageCount) * 100)
              return (
                <tr key={p.id}>
                  <td><Link to={`/admin/projects/${p.id}`}><strong>{p.drawingNo}</strong></Link></td>
                  <td>{p.name}</td>
                  <td>{p.customerName ?? <span className="muted">—</span>}</td>
                  <td>{p.partCount}</td>
                  <td>
                    <div className="bar"><div style={{ width: `${pct}%` }} /></div>
                    <span className="muted small">{p.stagesDone}/{p.stageCount} stages</span>
                  </td>
                  <td>{p.stagesUnassigned > 0 ? <span className="badge warn">{p.stagesUnassigned}</span> : '—'}</td>
                </tr>
              )
            })}
            {projects?.length === 0 && <tr><td colSpan={6} className="empty">No projects yet. Create the first one above.</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  )
}
