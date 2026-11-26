import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../../api'
import { ErrorNote, StatusBadge, errorText, formatDateTime } from '../../components'
import type { Part, ProjectDetail, StageMaster, User } from '../../types'

export default function ProjectDetailPage() {
  const { id } = useParams()
  const projectId = Number(id)
  const [project, setProject] = useState<ProjectDetail | null>(null)
  const [masters, setMasters] = useState<StageMaster[]>([])
  const [users, setUsers] = useState<User[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    Promise.all([api.project(projectId), api.stageMasters(), api.users()])
      .then(([p, m, u]) => { setProject(p); setMasters(m); setUsers(u.filter(x => x.active)) })
      .catch(e => setError(errorText(e)))
  }, [projectId])

  const refreshMasters = useCallback(() => {
    api.stageMasters().then(setMasters).catch(() => {})
  }, [])

  // Every change returns the refreshed project, so we simply replace what is on screen.
  const apply = useCallback(async (action: () => Promise<ProjectDetail>) => {
    setError(null)
    try {
      setProject(await action())
    } catch (e) {
      setError(errorText(e))
      throw e
    }
  }, [])

  if (!project) return <div>{error ? <ErrorNote message={error} /> : <p className="muted">Loading…</p>}</div>

  return (
    <div>
      <p className="crumbs"><Link to="/admin/projects">← Projects</Link></p>
      <h1>{project.drawingNo}</h1>
      <p className="muted">{project.name}</p>
      <ErrorNote message={error} />

      <AddPart onAdd={(p) => apply(() => api.addPart(project.id, p))} />

      {project.parts.length === 0 && <div className="empty">Add the child parts of this project above.</div>}
      {project.parts.map(part => (
        <PartCard key={part.id} part={part} masters={masters} users={users} apply={apply} onStagesAdded={refreshMasters} />
      ))}
    </div>
  )
}

function AddPart({ onAdd }: { onAdd: (p: { drawingNo: string; name: string; quantity: number }) => Promise<void> }) {
  const [drawingNo, setDrawingNo] = useState('')
  const [name, setName] = useState('')
  const [quantity, setQuantity] = useState(1)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    try {
      await onAdd({ drawingNo, name, quantity })
      setDrawingNo(''); setName(''); setQuantity(1)
    } catch { /* the page shows the error */ }
  }

  return (
    <form className="card inline-form" onSubmit={submit}>
      <h2>Add child part</h2>
      <div className="grid">
        <label>Part drawing number
          <input value={drawingNo} onChange={e => setDrawingNo(e.target.value)} placeholder="VDE-0324-08-00-01" required />
        </label>
        <label>Part name
          <input value={name} onChange={e => setName(e.target.value)} required />
        </label>
        <label>Quantity
          <input type="number" min={1} value={quantity} onChange={e => setQuantity(Number(e.target.value))} required />
        </label>
      </div>
      <button className="primary">Add part</button>
    </form>
  )
}

interface PartCardProps {
  part: Part
  masters: StageMaster[]
  users: User[]
  apply: (action: () => Promise<ProjectDetail>) => Promise<void>
  onStagesAdded: () => void
}

function PartCard({ part, masters, users, apply, onStagesAdded }: PartCardProps) {
  const [building, setBuilding] = useState(false)
  const stages = [...part.stages].sort((a, b) => a.sequenceNo - b.sequenceNo)

  return (
    <section className="card part">
      <div className="part-head">
        <div>
          <h2>{part.drawingNo}</h2>
          <span className="muted">{part.name} · qty {part.quantity}</span>
        </div>
        {!building && <button onClick={() => setBuilding(true)}>+ Add stages</button>}
      </div>

      {stages.length === 0 && !building && (
        <p className="muted">No stages yet. Choose the stages this part goes through.</p>
      )}

      {stages.length > 0 && (
        <div className="table-wrap">
          <table className="stages">
            <thead><tr><th>#</th><th>Stage</th><th>Owner</th><th>Due</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {stages.map(s => (
                <tr key={s.id}>
                  <td>{s.sequenceNo}</td>
                  <td>{s.stageName}</td>
                  <td>
                    <select value={s.assigneeId ?? ''}
                      onChange={e => apply(() => api.updateStage(s.id, e.target.value ? Number(e.target.value) : null, s.dueDate)).catch(() => {})}>
                      <option value="">— not allocated —</option>
                      {users.map(u => <option key={u.id} value={u.id}>{u.fullName}</option>)}
                      {s.assigneeId && !users.some(u => u.id === s.assigneeId) &&
                        <option value={s.assigneeId}>{s.assigneeName} (deactivated)</option>}
                    </select>
                  </td>
                  <td>
                    <input type="date" value={s.dueDate ?? ''}
                      onChange={e => apply(() => api.updateStage(s.id, s.assigneeId, e.target.value || null)).catch(() => {})} />
                  </td>
                  <td>
                    <StatusBadge status={s.status} />
                    {s.completedAt
                      ? <div className="muted small">{formatDateTime(s.completedAt)}</div>
                      : s.startedAt && <div className="muted small">since {formatDateTime(s.startedAt)}</div>}
                  </td>
                  <td>
                    {s.status === 'NOT_STARTED' &&
                      <button className="link danger" onClick={() => apply(() => api.deleteStage(s.id)).catch(() => {})}>Remove</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {building && (
        <RouteBuilder masters={masters} users={users}
          onCancel={() => setBuilding(false)}
          onSave={async (rows) => {
            await apply(() => api.addStages(part.id, rows))
            onStagesAdded()
            setBuilding(false)
          }} />
      )}
    </section>
  )
}

interface Row { key: number; stageName: string; assigneeId: number | ''; dueDate: string }

/**
 * Lets the admin lay out several stages in order (with their owners) and save them in one go.
 * The stage box is a type-in field: start typing to see the existing stages as suggestions,
 * or type any new name. The server matches an existing stage by name or adds the new one.
 */
function RouteBuilder({ masters, users, onCancel, onSave }: {
  masters: StageMaster[]
  users: User[]
  onCancel: () => void
  onSave: (rows: { stageMasterId: number | null; customName: string | null; assigneeId: number | null; dueDate: string | null }[]) => Promise<void>
}) {
  const blank = (key: number): Row => ({ key, stageName: '', assigneeId: '', dueDate: '' })
  const [rows, setRows] = useState<Row[]>([blank(1)])
  const [nextKey, setNextKey] = useState(2)

  const change = (key: number, patch: Partial<Row>) =>
    setRows(rs => rs.map(r => (r.key === key ? { ...r, ...patch } : r)))

  const save = async () => {
    const filled = rows.filter(r => r.stageName.trim() !== '')
    if (filled.length === 0) return
    try {
      await onSave(filled.map(r => ({
        stageMasterId: null,
        customName: r.stageName.trim(),
        assigneeId: r.assigneeId === '' ? null : r.assigneeId,
        dueDate: r.dueDate || null,
      })))
    } catch { /* the page shows the error */ }
  }

  return (
    <div className="builder">
      <h3>Stages, in order</h3>
      <datalist id="stage-options">
        {masters.map(m => <option key={m.id} value={m.name} />)}
      </datalist>
      {rows.map((r, i) => (
        <div className="builder-row" key={r.key}>
          <span className="step">{i + 1}</span>
          <input list="stage-options" className="stage-input" placeholder="Type or pick a stage…" maxLength={80}
            value={r.stageName} onChange={e => change(r.key, { stageName: e.target.value })} />
          <select value={r.assigneeId} onChange={e => change(r.key, { assigneeId: e.target.value ? Number(e.target.value) : '' })}>
            <option value="">Owner (decide later)</option>
            {users.map(u => <option key={u.id} value={u.id}>{u.fullName}</option>)}
          </select>
          <input type="date" value={r.dueDate} onChange={e => change(r.key, { dueDate: e.target.value })} />
          {rows.length > 1 && <button className="link danger" onClick={() => setRows(rs => rs.filter(x => x.key !== r.key))}>✕</button>}
        </div>
      ))}
      <div className="builder-actions">
        <button onClick={() => { setRows(rs => [...rs, blank(nextKey)]); setNextKey(k => k + 1) }}>+ Another stage</button>
        <span className="spacer" />
        <button className="link" onClick={onCancel}>Cancel</button>
        <button className="primary" onClick={save}>Save stages</button>
      </div>
    </div>
  )
}
