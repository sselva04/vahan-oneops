import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api'
import { ErrorNote, errorText, formatDate, formatMoney } from '../../components'
import type { Part, ProjectSummary, RmPurchase, RmPurchaseInput, Supplier } from '../../types'

const today = () => new Date().toISOString().slice(0, 10)
const kg = (n: number) => n.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 3 })

/** Raw material bought for parts: who from, how many kg, at what rate. Weight and rate are filled in from the part. */
export default function Purchases() {
  const [projects, setProjects] = useState<ProjectSummary[]>([])
  const [suppliers, setSuppliers] = useState<Supplier[]>([])
  const [month, setMonth] = useState(today().slice(0, 7))
  const [filterProject, setFilterProject] = useState<number | ''>('')
  const [filterSupplier, setFilterSupplier] = useState<number | ''>('')
  const [data, setData] = useState<{ purchases: RmPurchase[]; totals: { weightKg: number; totalCost: number } } | null>(null)
  const [error, setError] = useState<string | null>(null)

  const [editing, setEditing] = useState<RmPurchase | 'new' | null>(null)
  const [projectId, setProjectId] = useState<number | ''>('')
  const [parts, setParts] = useState<Part[]>([])
  const [partId, setPartId] = useState<number | ''>('')
  const [supplierId, setSupplierId] = useState<number | ''>('')
  const [weight, setWeight] = useState('')
  const [rate, setRate] = useState('')
  const [inDate, setInDate] = useState(today())
  const [outDate, setOutDate] = useState('')
  const [remarks, setRemarks] = useState('')
  const [hint, setHint] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const load = useCallback(() => {
    api.rmPurchases(month || undefined, filterProject === '' ? undefined : filterProject,
      filterSupplier === '' ? undefined : filterSupplier)
      .then(setData).catch(e => setError(errorText(e)))
  }, [month, filterProject, filterSupplier])
  useEffect(load, [load])

  useEffect(() => {
    api.projects().then(setProjects).catch(e => setError(errorText(e)))
    api.suppliers(false).then(setSuppliers).catch(e => setError(errorText(e)))
  }, [])
  useEffect(() => {
    if (projectId === '') { setParts([]); return }
    api.project(projectId).then(p => setParts(p.parts)).catch(e => setError(errorText(e)))
  }, [projectId])

  // choosing a part fills in the weight and price from its raw material (only for a new purchase)
  const pickPart = (id: number | '') => {
    setPartId(id)
    setHint(null)
    if (editing !== 'new' || id === '') return
    const rm = parts.find(p => p.id === id)?.rawMaterial
    if (!rm) { setHint('This part has no raw material set yet, so type the weight and rate.'); return }
    const w = rm.actualWeightKg ?? rm.theoreticalTotalKg
    setWeight(String(w))
    if (rm.ratePerKg !== null) setRate(String(rm.ratePerKg))
    setHint(`${rm.materialName}: ${kg(w)} kg ${rm.actualWeightKg !== null ? '(actual weight)' : '(theoretical weight)'}`
      + (rm.ratePerKg !== null ? ` at ${formatMoney(rm.ratePerKg)}/kg` : ''))
  }

  const open = (p: RmPurchase | 'new') => {
    setEditing(p); setHint(null)
    if (p === 'new') {
      setProjectId(filterProject); setPartId(''); setSupplierId(filterSupplier); setWeight(''); setRate('')
      setInDate(today()); setOutDate(''); setRemarks('')
    } else {
      setProjectId(p.projectId ?? ''); setPartId(p.partId); setSupplierId(p.supplierId); setWeight(String(p.weightKg))
      setRate(String(p.ratePerKg)); setInDate(p.inDate); setOutDate(p.outDate ?? ''); setRemarks(p.remarks ?? '')
    }
  }

  const total = Math.round((Number(weight) || 0) * (Number(rate) || 0) * 100) / 100

  const submit = async (ev: FormEvent) => {
    ev.preventDefault()
    if (partId === '' || supplierId === '') return
    const input: RmPurchaseInput = {
      partId, supplierId, weightKg: Number(weight), ratePerKg: Number(rate),
      inDate, outDate: outDate || null, remarks: remarks.trim() || null,
    }
    setSaving(true); setError(null)
    try {
      if (editing === 'new') await api.addRmPurchase(input)
      else if (editing) await api.updateRmPurchase(editing.id, input)
      setEditing(null)
      load()
    } catch (e) { setError(errorText(e)) } finally { setSaving(false) }
  }

  const remove = async (p: RmPurchase) => {
    if (!window.confirm(`Delete this purchase (${p.partDrawingNo}, ${formatDate(p.inDate)})?`)) return
    setError(null)
    try { await api.deleteRmPurchase(p.id); load() } catch (e) { setError(errorText(e)) }
  }

  return (
    <div>
      <h1>Raw material purchases</h1>
      <p className="muted">What was bought for each part, from whom, and at what cost.</p>
      <ErrorNote message={error} />

      <div className="card filters">
        <label>Month <input type="month" value={month} onChange={e => setMonth(e.target.value)} /></label>
        <select value={filterProject} onChange={e => setFilterProject(e.target.value ? Number(e.target.value) : '')}>
          <option value="">All projects</option>
          {projects.map(p => <option key={p.id} value={p.id}>{p.drawingNo} — {p.name}</option>)}
        </select>
        <select value={filterSupplier} onChange={e => setFilterSupplier(e.target.value ? Number(e.target.value) : '')}>
          <option value="">All suppliers</option>
          {suppliers.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
        </select>
        <button className="link" onClick={() => setMonth('')}>All months</button>
        <span className="spacer" />
        <button className="primary" onClick={() => open('new')}>+ Add purchase</button>
      </div>

      {editing !== null && (
        <form className="card inline-form" onSubmit={submit}>
          <h2>{editing === 'new' ? 'New purchase' : 'Edit purchase'}</h2>
          <div className="grid">
            <label>Project
              <select value={projectId} required onChange={e => { setProjectId(e.target.value ? Number(e.target.value) : ''); setPartId(''); setHint(null) }}>
                <option value="">— choose —</option>
                {projects.map(p => <option key={p.id} value={p.id}>{p.drawingNo} — {p.name}</option>)}
              </select>
            </label>
            <label>Part
              <select value={partId} required disabled={projectId === ''} onChange={e => pickPart(e.target.value ? Number(e.target.value) : '')}>
                <option value="">— choose —</option>
                {parts.map(p => <option key={p.id} value={p.id}>{p.drawingNo} — {p.name}</option>)}
              </select>
            </label>
            <label>Supplier
              <select value={supplierId} required onChange={e => setSupplierId(e.target.value ? Number(e.target.value) : '')}>
                <option value="">— choose —</option>
                {suppliers.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </label>
          </div>
          {hint && <p className="muted small">{hint}</p>}
          <div className="grid">
            <label>Weight (kg)<input type="number" min={0} step="0.001" value={weight} onChange={e => setWeight(e.target.value)} required /></label>
            <label>Rate per kg (₹)<input type="number" min={0} step="0.01" value={rate} onChange={e => setRate(e.target.value)} required /></label>
            <label>In date<input type="date" value={inDate} onChange={e => setInDate(e.target.value)} required /></label>
            <label>Out date<input type="date" value={outDate} min={inDate || undefined} onChange={e => setOutDate(e.target.value)} /></label>
          </div>
          <label>Remarks<input value={remarks} maxLength={500} onChange={e => setRemarks(e.target.value)} /></label>
          <p className="rm-preview">{kg(Number(weight) || 0)} kg × {formatMoney(Number(rate) || 0)} = <strong>{formatMoney(total)}</strong></p>
          <div className="builder-actions">
            <span className="spacer" />
            <button type="button" className="link" onClick={() => setEditing(null)}>Cancel</button>
            <button className="primary" disabled={saving || partId === '' || supplierId === ''}>{saving ? 'Saving…' : 'Save'}</button>
          </div>
        </form>
      )}

      {data && (
        <p className="card filters">
          <span>{data.purchases.length} purchases</span>
          <span><strong>{kg(data.totals.weightKg)} kg</strong></span>
          <span>Total <strong>{formatMoney(data.totals.totalCost)}</strong></span>
        </p>
      )}

      <div className="card table-wrap">
        <table>
          <thead>
            <tr><th>In</th><th>Out</th><th>Supplier</th><th>Project / part</th><th>Material</th><th>Weight</th><th>Rate/kg</th><th>Total</th><th></th></tr>
          </thead>
          <tbody>
            {data?.purchases.map(p => (
              <tr key={p.id}>
                <td>{formatDate(p.inDate)}</td>
                <td>{p.outDate ? formatDate(p.outDate) : <span className="muted">—</span>}</td>
                <td>{p.supplierName}</td>
                <td>
                  {p.partDrawingNo}
                  <div className="muted small">{p.partName}{p.customerName ? ` · ${p.customerName}` : ''}</div>
                  {p.remarks && <div className="muted small">“{p.remarks}”</div>}
                </td>
                <td>{p.materialName ?? <span className="muted">—</span>}</td>
                <td>{kg(p.weightKg)} kg</td>
                <td>{formatMoney(p.ratePerKg)}</td>
                <td><strong>{formatMoney(p.totalCost)}</strong></td>
                <td className="actions">
                  <button className="link" onClick={() => open(p)}>Edit</button>
                  <button className="link danger" onClick={() => remove(p)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {data && data.purchases.length === 0 && <div className="empty">No purchases for this selection.</div>}
      </div>
    </div>
  )
}
