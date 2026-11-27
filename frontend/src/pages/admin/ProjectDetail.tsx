import { Fragment, useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../../api'
import { ErrorNote, StatusBadge, errorText, formatDate, formatDateTime, formatMoney } from '../../components'
import { SHAPE_LABEL, previewPerPieceKg, sizeText } from '../../weight'
import type { Customer, Material, OutsourceInput, Part, ProjectDetail, RawMaterialInput, Rejection, RejectionInput, RmShape, Stage, StageMaster, Supplier, User } from '../../types'

export default function ProjectDetailPage() {
  const { id } = useParams()
  const projectId = Number(id)
  const [project, setProject] = useState<ProjectDetail | null>(null)
  const [masters, setMasters] = useState<StageMaster[]>([])
  const [users, setUsers] = useState<User[]>([])
  const [suppliers, setSuppliers] = useState<Supplier[]>([])
  const [materials, setMaterials] = useState<Material[]>([])
  const [customers, setCustomers] = useState<Customer[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    Promise.all([api.project(projectId), api.stageMasters(), api.users(), api.suppliers(false), api.materials(true), api.customers(true)])
      .then(([p, m, u, s, mat, c]) => { setProject(p); setMasters(m); setUsers(u.filter(x => x.active)); setSuppliers(s); setMaterials(mat); setCustomers(c) })
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
      <CustomerLine project={project} customers={customers} apply={apply} />
      {project.rawMaterialCost > 0 && (
        <p className="rm-total">Raw material for this project: <strong>{formatMoney(project.rawMaterialCost)}</strong></p>
      )}
      <ErrorNote message={error} />

      <AddPart onAdd={(p) => apply(() => api.addPart(project.id, p))} />

      {project.parts.length === 0 && <div className="empty">Add the child parts of this project above.</div>}
      {project.parts.map(part => (
        <PartCard key={part.id} part={part} masters={masters} users={users} suppliers={suppliers} materials={materials} apply={apply} onStagesAdded={refreshMasters} />
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
  suppliers: Supplier[]
  materials: Material[]
  apply: (action: () => Promise<ProjectDetail>) => Promise<void>
  onStagesAdded: () => void
}

function PartCard({ part, masters, users, suppliers, materials, apply, onStagesAdded }: PartCardProps) {
  const [building, setBuilding] = useState(false)
  const [outsourcing, setOutsourcing] = useState<number | null>(null)   // id of the stage whose supplier form is open
  const [rejecting, setRejecting] = useState<number | null>(null)       // id of the stage whose rejections are open
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

      <RawMaterialSection part={part} materials={materials} apply={apply} />

      {stages.length === 0 && !building && (
        <p className="muted">No stages yet. Choose the stages this part goes through.</p>
      )}

      {stages.length > 0 && (
        <div className="table-wrap">
          <table className="stages">
            <thead><tr><th>#</th><th>Stage</th><th>Owner</th><th>Due</th><th>Done by</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {stages.map(s => (
                <Fragment key={s.id}>
                <tr>
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
                    {s.supplierId !== null ? (
                      <div>
                        <strong>{s.supplierName}</strong>
                        <div className="muted small">
                          {s.jobQty ?? '—'} pcs · {formatMoney(s.ratePerUnit)} each
                          {s.totalCost !== null && <> · <strong>{formatMoney(s.totalCost)}</strong></>}
                        </div>
                        <div className="small">
                          {s.preInspectionDone ? '✓' : '○'} pre-inspection · {s.finalInspectionDone ? '✓' : '○'} final inspection
                        </div>
                        {(s.sentOn || s.receivedOn) && (
                          <div className="small">
                            Sent {s.sentOn ? formatDate(s.sentOn) : '—'} · Received {s.receivedOn ? formatDate(s.receivedOn) : 'not yet'}
                            {s.turnaroundDays !== null && <span className="muted"> ({s.turnaroundDays} days)</span>}
                          </div>
                        )}
                        {s.remarks && <div className="muted small">“{s.remarks}”</div>}
                        {s.rejectedQty > 0 && (
                          <div className="small rm-over">
                            {s.rejectedQty} rejected · debit {formatMoney(s.rejections.reduce((t, r) => t + r.debitAmount, 0))}
                          </div>
                        )}
                      </div>
                    ) : <span className="muted">In-house</span>}
                    <button className="link" onClick={() => { setRejecting(null); setOutsourcing(outsourcing === s.id ? null : s.id) }}>
                      {s.supplierId !== null ? 'Edit' : 'Give to supplier…'}
                    </button>
                    {s.supplierId !== null && (
                      <button className="link" onClick={() => { setOutsourcing(null); setRejecting(rejecting === s.id ? null : s.id) }}>
                        Rejections{s.rejections.length > 0 ? ` (${s.rejections.length})` : ''}
                      </button>
                    )}
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
                {rejecting === s.id && (
                  <tr className="detail-row">
                    <td colSpan={7}>
                      <RejectionPanel stage={s} apply={apply} onClose={() => setRejecting(null)} />
                    </td>
                  </tr>
                )}
                {outsourcing === s.id && (
                  <tr className="detail-row">
                    <td colSpan={7}>
                      <OutsourceForm stage={s} part={part} suppliers={suppliers}
                        onCancel={() => setOutsourcing(null)}
                        onSave={async (o) => {
                          await apply(() => api.updateOutsourcing(s.id, o))
                          setOutsourcing(null)
                        }} />
                    </td>
                  </tr>
                )}
                </Fragment>
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

/** The project's customer: shown, and changeable by the admin. */
function CustomerLine({ project, customers, apply }: {
  project: ProjectDetail
  customers: Customer[]
  apply: (action: () => Promise<ProjectDetail>) => Promise<void>
}) {
  const [editing, setEditing] = useState(false)
  const [choice, setChoice] = useState<number | ''>(project.customerId ?? '')
  const usable = customers.filter(c => c.active || c.id === project.customerId)

  if (!editing) {
    return (
      <p className="small">
        Customer: {project.customerName ? <strong>{project.customerName}</strong> : <span className="rm-over">not set</span>}{' '}
        <button className="link" onClick={() => { setChoice(project.customerId ?? ''); setEditing(true) }}>
          {project.customerName ? 'Change' : 'Set customer…'}
        </button>
      </p>
    )
  }
  return (
    <p className="small">
      Customer:{' '}
      <select value={choice} onChange={e => setChoice(e.target.value ? Number(e.target.value) : '')}>
        <option value="">— choose —</option>
        {usable.map(c => <option key={c.id} value={c.id}>{c.name}{c.active ? '' : ' (deactivated)'}</option>)}
      </select>{' '}
      <button className="primary" disabled={choice === ''}
        onClick={() => { if (choice !== '') apply(() => api.setProjectCustomer(project.id, choice)).then(() => setEditing(false)).catch(() => {}) }}>
        Save
      </button>{' '}
      <button className="link" onClick={() => setEditing(false)}>Cancel</button>
    </p>
  )
}

/**
 * Rejections of one supplier job: parts the supplier spoiled and what is debited to them.
 * Debit = rejected pieces × part cost − machining cost (never below 0), unless an amount is typed in.
 */
function RejectionPanel({ stage, apply, onClose }: {
  stage: Stage
  apply: (action: () => Promise<ProjectDetail>) => Promise<void>
  onClose: () => void
}) {
  const [editing, setEditing] = useState<Rejection | 'new' | null>(stage.rejections.length === 0 ? 'new' : null)
  const [qty, setQty] = useState('')
  const [partCost, setPartCost] = useState('')
  const [machining, setMachining] = useState('')
  const [debit, setDebit] = useState('')
  const [bill, setBill] = useState('')
  const [on, setOn] = useState('')
  const [remarks, setRemarks] = useState('')
  const [saving, setSaving] = useState(false)

  const open = (r: Rejection | 'new') => {
    setEditing(r)
    const t = (n: number | null | undefined) => (n == null ? '' : String(n))
    setQty(r === 'new' ? '' : t(r.rejectedQty))
    setPartCost(r === 'new' ? '' : t(r.partCost))
    setMachining(r === 'new' ? '' : t(r.machiningCost))
    setDebit(r === 'new' || !r.debitManual ? '' : t(r.debitAmount))
    setBill(r === 'new' ? '' : r.billNo ?? '')
    setOn(r === 'new' ? new Date().toISOString().slice(0, 10) : r.rejectedOn ?? '')
    setRemarks(r === 'new' ? '' : r.remarks ?? '')
  }
  const calculated = Math.max(0, (Number(qty) || 0) * (Number(partCost) || 0) - (Number(machining) || 0))

  const submit = async () => {
    const input: RejectionInput = {
      rejectedQty: Number(qty),
      partCost: Number(partCost),
      machiningCost: machining.trim() === '' ? null : Number(machining),
      debitAmount: debit.trim() === '' ? null : Number(debit),
      billNo: bill.trim() || null,
      rejectedOn: on || null,
      remarks: remarks.trim() || null,
    }
    setSaving(true)
    try {
      await apply(() => (editing === 'new' ? api.addRejection(stage.id, input) : api.updateRejection((editing as Rejection).id, input)))
      setEditing(null)
    } catch { /* the page shows the error */ } finally {
      setSaving(false)
    }
  }

  return (
    <div className="outsource">
      <h3>{stage.stageName} at {stage.supplierName}: rejections</h3>
      {stage.rejections.length > 0 && (
        <table>
          <thead><tr><th>Date</th><th>Bill</th><th>Qty</th><th>Part cost</th><th>Machining</th><th>Debit</th><th>Remarks</th><th></th></tr></thead>
          <tbody>
            {stage.rejections.map(r => (
              <tr key={r.id}>
                <td>{formatDate(r.rejectedOn)}</td>
                <td>{r.billNo ?? '—'}</td>
                <td>{r.rejectedQty}</td>
                <td>{formatMoney(r.partCost)}</td>
                <td>{formatMoney(r.machiningCost)}</td>
                <td><strong>{formatMoney(r.debitAmount)}</strong>{r.debitManual && <span className="muted small"> (typed)</span>}</td>
                <td className="muted small">{r.remarks}</td>
                <td className="actions">
                  <button className="link" onClick={() => open(r)}>Edit</button>
                  <button className="link danger" onClick={() => {
                    if (window.confirm('Delete this rejection?')) apply(() => api.deleteRejection(r.id)).catch(() => {})
                  }}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {editing === null ? (
        <div className="builder-actions">
          <button onClick={() => open('new')}>+ Record a rejection</button>
          <span className="spacer" />
          <button className="link" onClick={onClose}>Close</button>
        </div>
      ) : (
        <>
          <div className="grid">
            <label>Rejected pieces<input type="number" min={1} value={qty} onChange={e => setQty(e.target.value)} /></label>
            <label>Part cost per piece (₹)<input type="number" min={0} step="0.01" value={partCost} onChange={e => setPartCost(e.target.value)} /></label>
            <label>Machining cost (₹)<input type="number" min={0} step="0.01" value={machining} onChange={e => setMachining(e.target.value)} placeholder="0" /></label>
            <label>Bill no.<input value={bill} maxLength={40} onChange={e => setBill(e.target.value)} /></label>
            <label>Date<input type="date" value={on} onChange={e => setOn(e.target.value)} /></label>
          </div>
          <p className="rm-preview">
            Calculated debit: <strong>{formatMoney(calculated)}</strong>
            <span className="muted small"> (pieces × part cost − machining cost, minimum 0)</span>
          </p>
          <div className="grid">
            <label>Debit amount (₹), only to override
              <input type="number" min={0} step="0.01" value={debit} onChange={e => setDebit(e.target.value)} placeholder="leave empty to use the calculated one" />
            </label>
          </div>
          <label>Remarks
            <input value={remarks} maxLength={500} onChange={e => setRemarks(e.target.value)} placeholder="e.g. wrong setting done in wire cut" />
          </label>
          <div className="builder-actions">
            <span className="spacer" />
            <button className="link" onClick={() => setEditing(null)}>Cancel</button>
            <button className="primary" disabled={saving || !qty || !partCost} onClick={submit}>{saving ? 'Saving…' : 'Save'}</button>
          </div>
        </>
      )}
    </div>
  )
}

/** Raw material of a part: material, shape and size give the theoretical weight; the weighed weight is entered after. */
function RawMaterialSection({ part, materials, apply }: {
  part: Part
  materials: Material[]
  apply: (action: () => Promise<ProjectDetail>) => Promise<void>
}) {
  const rm = part.rawMaterial
  const [editing, setEditing] = useState(false)

  if (editing) {
    return <RawMaterialForm part={part} materials={materials} onCancel={() => setEditing(false)}
      onSave={async (r) => { await apply(() => api.updateRawMaterial(part.id, r)); setEditing(false) }} />
  }
  if (!rm) {
    return (
      <div className="rm-box">
        <span className="muted">No raw material yet.</span>
        <button className="link" onClick={() => setEditing(true)}>Set raw material…</button>
      </div>
    )
  }
  const kg = (n: number | null) => (n === null ? '—' : `${n.toFixed(3)} kg`)
  return (
    <div className="rm-box">
      <div>
        <strong>{rm.materialName}</strong> · {sizeText(rm.shape, rm.dimA, rm.dimB, rm.dimC, rm.manualWeightKg)}
        <div className="small">
          Theoretical <strong>{kg(rm.theoreticalTotalKg)}</strong>
          {part.quantity > 1 && <span className="muted"> ({kg(rm.theoreticalPerPieceKg)} × {part.quantity})</span>}
          {' · '}Actual <strong>{kg(rm.actualWeightKg)}</strong>
          {rm.differenceKg !== null && (
            <span className={rm.differenceKg > 0 ? 'rm-over' : 'muted'}>
              {' '}({rm.differenceKg > 0 ? '+' : ''}{rm.differenceKg.toFixed(3)} kg{rm.differencePercent !== null && `, ${rm.differencePercent > 0 ? '+' : ''}${rm.differencePercent}%`})
            </span>
          )}
        </div>
        <div className="small">
          {rm.ratePerKg === null ? <span className="muted">No price set for this material</span>
            : <>{formatMoney(rm.ratePerKg)}/kg · cost <strong>{formatMoney(rm.cost)}</strong>
              <span className="muted"> (on {rm.costFromActual ? 'actual' : 'theoretical'} weight)</span></>}
        </div>
      </div>
      <button className="link" onClick={() => setEditing(true)}>Edit</button>
    </div>
  )
}

function RawMaterialForm({ part, materials, onSave, onCancel }: {
  part: Part
  materials: Material[]
  onSave: (r: RawMaterialInput) => Promise<void>
  onCancel: () => void
}) {
  const rm = part.rawMaterial
  const [materialId, setMaterialId] = useState<number | ''>(rm?.materialId ?? '')
  const [shape, setShape] = useState<RmShape>(rm?.shape ?? 'BLOCK')
  const text = (n: number | null | undefined) => (n == null ? '' : String(n))
  const [a, setA] = useState(text(rm?.dimA))
  const [b, setB] = useState(text(rm?.dimB))
  const [c, setC] = useState(text(rm?.dimC))
  const [manual, setManual] = useState(text(rm?.manualWeightKg))
  const [actual, setActual] = useState(text(rm?.actualWeightKg))
  const [saving, setSaving] = useState(false)

  const usable = materials.filter(m => m.active || m.id === rm?.materialId)
  const chosen = materials.find(m => m.id === materialId)
  const perPiece = chosen ? previewPerPieceKg(shape, chosen.density, Number(a), Number(b), Number(c), Number(manual)) : null
  const total = perPiece === null ? null : perPiece * part.quantity
  const labels: Record<RmShape, [string, string, string]> = {
    BLOCK: ['Length (mm)', 'Width (mm)', 'Thickness (mm)'],
    ROUND: ['Diameter (mm)', 'Length (mm)', ''],
    TUBE: ['Outer Ø (mm)', 'Inner Ø (mm)', 'Length (mm)'],
    MANUAL: ['', '', ''],
  }
  const num = (s: string) => (s.trim() === '' ? null : Number(s))

  const save = async (clear: boolean) => {
    setSaving(true)
    try {
      await onSave(clear || materialId === '' ? {
        materialId: null, shape: null, dimA: null, dimB: null, dimC: null, manualWeightKg: null, actualWeightKg: null,
      } : {
        materialId, shape,
        dimA: shape === 'MANUAL' ? null : num(a),
        dimB: shape === 'MANUAL' ? null : num(b),
        dimC: shape === 'BLOCK' || shape === 'TUBE' ? num(c) : null,
        manualWeightKg: shape === 'MANUAL' ? num(manual) : null,
        actualWeightKg: num(actual),
      })
    } catch { /* the page shows the error */ } finally {
      setSaving(false)
    }
  }

  return (
    <div className="outsource">
      <h3>Raw material for {part.drawingNo}</h3>
      <div className="grid">
        <label>Material
          <select value={materialId} onChange={e => setMaterialId(e.target.value ? Number(e.target.value) : '')}>
            <option value="">— none —</option>
            {usable.map(m => <option key={m.id} value={m.id}>{m.name}{m.active ? '' : ' (deactivated)'}</option>)}
          </select>
        </label>
        <label>Shape
          <select value={shape} onChange={e => setShape(e.target.value as RmShape)}>
            {(Object.keys(SHAPE_LABEL) as RmShape[]).map(s => <option key={s} value={s}>{SHAPE_LABEL[s]}</option>)}
          </select>
        </label>
      </div>
      {shape === 'MANUAL' ? (
        <div className="grid">
          <label>Weight per piece (kg)
            <input type="number" min={0} step="0.001" value={manual} onChange={e => setManual(e.target.value)} />
          </label>
        </div>
      ) : (
        <div className="grid">
          <label>{labels[shape][0]}<input type="number" min={0} step="0.01" value={a} onChange={e => setA(e.target.value)} /></label>
          <label>{labels[shape][1]}<input type="number" min={0} step="0.01" value={b} onChange={e => setB(e.target.value)} /></label>
          {labels[shape][2] && (
            <label>{labels[shape][2]}<input type="number" min={0} step="0.01" value={c} onChange={e => setC(e.target.value)} /></label>
          )}
        </div>
      )}
      <p className="rm-preview">
        {chosen === undefined ? <span className="muted">Choose a material to see the weight.</span>
          : total === null ? <span className="muted">Enter the size to see the weight.</span>
          : <>Theoretical weight: <strong>{total.toFixed(3)} kg</strong>
              <span className="muted"> ({perPiece!.toFixed(3)} kg × {part.quantity})</span></>}
      </p>
      <label>Actual weight, whole quantity (kg) — after weighing
        <input type="number" min={0} step="0.001" value={actual} onChange={e => setActual(e.target.value)} placeholder="optional" />
      </label>
      <div className="builder-actions">
        {rm && <button className="link danger" disabled={saving} onClick={() => save(true)}>Remove raw material</button>}
        <span className="spacer" />
        <button className="link" onClick={onCancel}>Cancel</button>
        <button className="primary" disabled={saving || materialId === ''} onClick={() => save(false)}>{saving ? 'Saving…' : 'Save'}</button>
      </div>
    </div>
  )
}

/**
 * Give one stage to a supplier (or take it back in-house) and record the job: pieces sent, rate per piece,
 * inspections and remarks. Suppliers that do this kind of stage are listed first.
 */
function OutsourceForm({ stage, part, suppliers, onSave, onCancel }: {
  stage: Stage
  part: Part
  suppliers: Supplier[]
  onSave: (o: OutsourceInput) => Promise<void>
  onCancel: () => void
}) {
  const [supplierId, setSupplierId] = useState<number | ''>(stage.supplierId ?? '')
  const [qty, setQty] = useState(String(stage.jobQty ?? part.quantity))
  const [rate, setRate] = useState(stage.ratePerUnit !== null ? String(stage.ratePerUnit) : '')
  const [pre, setPre] = useState(stage.preInspectionDone)
  const [fin, setFin] = useState(stage.finalInspectionDone)
  const [remarks, setRemarks] = useState(stage.remarks ?? '')
  const today = new Date().toISOString().slice(0, 10)
  const [sentOn, setSentOn] = useState(stage.sentOn ?? (stage.supplierId === null ? today : ''))
  const [receivedOn, setReceivedOn] = useState(stage.receivedOn ?? '')
  const [saving, setSaving] = useState(false)

  const offering = suppliers.filter(s => s.processes.some(p => p.stageMasterId === stage.stageMasterId))
  const others = suppliers.filter(s => !offering.includes(s))
  const chosen = suppliers.find(s => s.id === supplierId)
  const contact = chosen?.processes.find(p => p.stageMasterId === stage.stageMasterId)
  const option = (s: Supplier) => <option key={s.id} value={s.id}>{s.name}</option>

  const save = async () => {
    setSaving(true)
    try {
      await onSave({
        supplierId: supplierId === '' ? null : supplierId,
        jobQty: supplierId === '' ? null : Number(qty) || null,
        ratePerUnit: supplierId === '' || rate.trim() === '' ? null : Number(rate),
        preInspectionDone: pre,
        finalInspectionDone: fin,
        remarks: remarks.trim() || null,
        sentOn: supplierId === '' ? null : sentOn || null,
        receivedOn: supplierId === '' ? null : receivedOn || null,
      })
    } catch { /* the page shows the error */ } finally {
      setSaving(false)
    }
  }

  return (
    <div className="outsource">
      <h3>{stage.stageName}: who does it?</h3>
      <div className="grid">
        <label>Done by
          <select value={supplierId} onChange={e => setSupplierId(e.target.value ? Number(e.target.value) : '')}>
            <option value="">In-house</option>
            {stage.supplierId !== null && !suppliers.some(s => s.id === stage.supplierId) &&
              <option value={stage.supplierId}>{stage.supplierName} (deactivated)</option>}
            {offering.length > 0 && <optgroup label={`Suppliers that do ${stage.stageName}`}>{offering.map(option)}</optgroup>}
            {others.length > 0 && <optgroup label="Other suppliers">{others.map(option)}</optgroup>}
          </select>
        </label>
        {supplierId !== '' && <>
          <label>Pieces sent
            <input type="number" min={1} value={qty} onChange={e => setQty(e.target.value)} />
          </label>
          <label>Rate per piece (₹)
            <input type="number" min={0} step="0.01" value={rate} onChange={e => setRate(e.target.value)} />
          </label>
          <label>Sent on
            <input type="date" value={sentOn} onChange={e => setSentOn(e.target.value)} />
          </label>
          <label>Received on
            <input type="date" value={receivedOn} min={sentOn || undefined} onChange={e => setReceivedOn(e.target.value)} />
          </label>
        </>}
      </div>
      {contact && (contact.contactPerson || contact.contactPhone) &&
        <p className="muted small">Contact: {contact.contactPerson} {contact.contactPhone}</p>}
      <div className="checks">
        <label className="check"><input type="checkbox" checked={pre} onChange={e => setPre(e.target.checked)} /> Pre-inspection done</label>
        <label className="check"><input type="checkbox" checked={fin} onChange={e => setFin(e.target.checked)} /> Final inspection done</label>
      </div>
      <label>Remarks
        <input value={remarks} maxLength={500} onChange={e => setRemarks(e.target.value)} placeholder="e.g. chamfer rework, 1 rejected" />
      </label>
      <div className="builder-actions">
        <span className="spacer" />
        <button className="link" onClick={onCancel}>Cancel</button>
        <button className="primary" disabled={saving} onClick={save}>{saving ? 'Saving…' : 'Save'}</button>
      </div>
    </div>
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
