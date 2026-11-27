import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api'
import { ErrorNote, errorText, formatDate, formatMoney } from '../../components'
import type { MachineHourEntry, MachineHourInput, MachineHourList, MachineHourSettings, Part, ProjectSummary } from '../../types'

const today = () => new Date().toISOString().slice(0, 10)
const num = (s: string): number | null => (s.trim() === '' ? null : Number(s))
const hoursText = (h: number) => h.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

/** VMC machine-hour log: billable hours = (design + setting + run minutes) / 60, cost = hours x rate, plus tax. */
export default function MachineHours() {
  const [projects, setProjects] = useState<ProjectSummary[]>([])
  const [settings, setSettings] = useState<MachineHourSettings | null>(null)
  const [rateDraft, setRateDraft] = useState('')
  const [taxDraft, setTaxDraft] = useState('')
  const [month, setMonth] = useState(today().slice(0, 7))
  const [filterProject, setFilterProject] = useState<number | ''>('')
  const [list, setList] = useState<MachineHourList | null>(null)
  const [error, setError] = useState<string | null>(null)

  const [editing, setEditing] = useState<MachineHourEntry | 'new' | null>(null)
  const [date, setDate] = useState(today())
  const [projectId, setProjectId] = useState<number | ''>('')
  const [parts, setParts] = useState<Part[]>([])
  const [partId, setPartId] = useState<number | ''>('')
  const [qty, setQty] = useState('1')
  const [hardened, setHardened] = useState(false)
  const [design, setDesign] = useState('')
  const [setting, setSetting] = useState('')
  const [run, setRun] = useState('')
  const [idle, setIdle] = useState('')
  const [powerCut, setPowerCut] = useState('')
  const [rate, setRate] = useState('')
  const [remarks, setRemarks] = useState('')
  const [saving, setSaving] = useState(false)

  const load = useCallback(() => {
    api.machineHours(month || undefined, filterProject === '' ? undefined : filterProject)
      .then(setList).catch(e => setError(errorText(e)))
  }, [month, filterProject])
  useEffect(load, [load])

  useEffect(() => {
    api.projects().then(setProjects).catch(e => setError(errorText(e)))
    api.machineHourSettings().then(s => { setSettings(s); setRateDraft(String(s.ratePerHour)); setTaxDraft(String(s.taxPercent)) })
      .catch(e => setError(errorText(e)))
  }, [])

  // the parts of the project chosen in the form
  useEffect(() => {
    if (projectId === '') { setParts([]); return }
    api.project(projectId).then(p => setParts(p.parts)).catch(e => setError(errorText(e)))
  }, [projectId])

  const saveSettings = async () => {
    setError(null)
    try {
      const s = await api.saveMachineHourSettings({ ratePerHour: Number(rateDraft), taxPercent: Number(taxDraft) })
      setSettings(s)
    } catch (e) { setError(errorText(e)) }
  }

  const open = (e: MachineHourEntry | 'new') => {
    setEditing(e)
    if (e === 'new') {
      setDate(today()); setProjectId(filterProject); setPartId(''); setQty('1'); setHardened(false)
      setDesign(''); setSetting(''); setRun(''); setIdle(''); setPowerCut(''); setRate(''); setRemarks('')
    } else {
      setDate(e.entryDate); setProjectId(e.projectId); setPartId(e.childPartId ?? ''); setQty(String(e.quantity))
      setHardened(e.hardened); setDesign(String(e.designMinutes)); setSetting(String(e.settingMinutes))
      setRun(String(e.runMinutes)); setIdle(String(e.idleMinutes)); setPowerCut(String(e.powerCutMinutes))
      setRate(String(e.ratePerHour)); setRemarks(e.remarks ?? '')
    }
  }

  const effectiveRate = rate.trim() !== '' ? Number(rate)
    : editing !== null && editing !== 'new' ? editing.ratePerHour : settings?.ratePerHour ?? 0
  const effectiveTax = editing !== null && editing !== 'new' ? editing.taxPercent : settings?.taxPercent ?? 0
  const billableMinutes = (Number(design) || 0) + (Number(setting) || 0) + (Number(run) || 0)
  const previewAmount = Math.round((effectiveRate * billableMinutes) / 60 * 100) / 100
  const previewTax = Math.round(previewAmount * effectiveTax) / 100

  const submit = async (ev: FormEvent) => {
    ev.preventDefault()
    if (projectId === '') return
    const input: MachineHourInput = {
      entryDate: date, projectId, childPartId: partId === '' ? null : partId,
      quantity: Number(qty) || 1, hardened,
      designMinutes: num(design), settingMinutes: num(setting), runMinutes: num(run),
      idleMinutes: num(idle), powerCutMinutes: num(powerCut),
      ratePerHour: num(rate), remarks: remarks.trim() || null,
    }
    setSaving(true); setError(null)
    try {
      if (editing === 'new') await api.addMachineHour(input)
      else if (editing) await api.updateMachineHour(editing.id, input)
      setEditing(null)
      load()
    } catch (e) { setError(errorText(e)) } finally { setSaving(false) }
  }

  const remove = async (e: MachineHourEntry) => {
    if (!window.confirm(`Delete this entry (${e.partDrawingNo ?? e.projectDrawingNo}, ${formatDate(e.entryDate)})?`)) return
    setError(null)
    try { await api.deleteMachineHour(e.id); load() } catch (err) { setError(errorText(err)) }
  }

  return (
    <div>
      <h1>Machine hours</h1>
      <p className="muted">VMC jobs done in-house. Billable time is design + setting + run minutes; idle and power-cut minutes are only recorded.</p>
      <ErrorNote message={error} />

      <div className="card inline-form">
        <h2>Default rate</h2>
        <div className="grid">
          <label>Rate per hour (₹)<input type="number" min={0} step="0.01" value={rateDraft} onChange={e => setRateDraft(e.target.value)} /></label>
          <label>Tax (%)<input type="number" min={0} max={100} step="0.01" value={taxDraft} onChange={e => setTaxDraft(e.target.value)} /></label>
        </div>
        <p className="muted small">New entries use these. Entries already saved keep the rate and tax they were saved with.</p>
        <button disabled={!settings || (Number(rateDraft) === settings.ratePerHour && Number(taxDraft) === settings.taxPercent)}
                onClick={saveSettings}>Save default</button>
      </div>

      <div className="card filters">
        <label>Month <input type="month" value={month} onChange={e => setMonth(e.target.value)} /></label>
        <select value={filterProject} onChange={e => setFilterProject(e.target.value ? Number(e.target.value) : '')}>
          <option value="">All projects</option>
          {projects.map(p => <option key={p.id} value={p.id}>{p.drawingNo} — {p.name}</option>)}
        </select>
        <button className="link" onClick={() => setMonth('')}>All months</button>
        <span className="spacer" />
        <button className="primary" onClick={() => open('new')}>+ Add entry</button>
      </div>

      {editing !== null && (
        <form className="card inline-form" onSubmit={submit}>
          <h2>{editing === 'new' ? 'New machine-hour entry' : 'Edit entry'}</h2>
          <div className="grid">
            <label>Date<input type="date" value={date} onChange={e => setDate(e.target.value)} required /></label>
            <label>Project
              <select value={projectId} required onChange={e => { setProjectId(e.target.value ? Number(e.target.value) : ''); setPartId('') }}>
                <option value="">— choose —</option>
                {projects.map(p => <option key={p.id} value={p.id}>{p.drawingNo} — {p.name}</option>)}
              </select>
            </label>
            <label>Part (optional)
              <select value={partId} onChange={e => setPartId(e.target.value ? Number(e.target.value) : '')} disabled={projectId === ''}>
                <option value="">— whole project —</option>
                {parts.map(p => <option key={p.id} value={p.id}>{p.drawingNo} — {p.name}</option>)}
              </select>
            </label>
            <label>Quantity<input type="number" min={1} value={qty} onChange={e => setQty(e.target.value)} required /></label>
            <label className="check"><input type="checkbox" checked={hardened} onChange={e => setHardened(e.target.checked)} /> Hardened part</label>
          </div>
          <div className="grid">
            <label>Design (min)<input type="number" min={0} value={design} onChange={e => setDesign(e.target.value)} /></label>
            <label>Setting (min)<input type="number" min={0} value={setting} onChange={e => setSetting(e.target.value)} /></label>
            <label>Machine run (min)<input type="number" min={0} value={run} onChange={e => setRun(e.target.value)} /></label>
            <label>Idle (min, not billed)<input type="number" min={0} value={idle} onChange={e => setIdle(e.target.value)} /></label>
            <label>Power cut (min, not billed)<input type="number" min={0} value={powerCut} onChange={e => setPowerCut(e.target.value)} /></label>
            <label>Rate per hour (₹)
              <input type="number" min={0} step="0.01" value={rate} onChange={e => setRate(e.target.value)}
                     placeholder={String(editing === 'new' ? settings?.ratePerHour ?? '' : editing.ratePerHour)} />
            </label>
          </div>
          <label>Remarks<input value={remarks} maxLength={500} onChange={e => setRemarks(e.target.value)} placeholder="e.g. rework, no cutter" /></label>
          <p className="rm-preview">
            {hoursText(billableMinutes / 60)} h × {formatMoney(effectiveRate)} = <strong>{formatMoney(previewAmount)}</strong>
            {' '}+ tax {formatMoney(previewTax)} = <strong>{formatMoney(previewAmount + previewTax)}</strong>
          </p>
          <div className="builder-actions">
            <span className="spacer" />
            <button type="button" className="link" onClick={() => setEditing(null)}>Cancel</button>
            <button className="primary" disabled={saving || projectId === ''}>{saving ? 'Saving…' : 'Save'}</button>
          </div>
        </form>
      )}

      {list && (
        <p className="card filters">
          <span>{list.entries.length} entries · <strong>{hoursText(list.totals.billableHours)} h</strong></span>
          <span>Amount <strong>{formatMoney(list.totals.amount)}</strong></span>
          <span>Tax {formatMoney(list.totals.taxAmount)}</span>
          <span>Total <strong>{formatMoney(list.totals.total)}</strong></span>
        </p>
      )}

      <div className="card table-wrap">
        <table>
          <thead>
            <tr><th>Date</th><th>Customer</th><th>Project / part</th><th>Qty</th><th>Design</th><th>Setting</th><th>Run</th>
              <th>Idle</th><th>Hours</th><th>Rate</th><th>Amount</th><th>Tax</th><th>Total</th><th></th></tr>
          </thead>
          <tbody>
            {list?.entries.map(e => (
              <tr key={e.id}>
                <td>{formatDate(e.entryDate)}</td>
                <td>{e.customerName ?? <span className="muted">—</span>}</td>
                <td>
                  {e.partDrawingNo ?? e.projectDrawingNo}
                  <div className="muted small">{e.partName ?? e.projectName}{e.hardened ? ' · hardened' : ''}</div>
                  {e.remarks && <div className="muted small">“{e.remarks}”</div>}
                </td>
                <td>{e.quantity}</td>
                <td>{e.designMinutes}</td><td>{e.settingMinutes}</td><td>{e.runMinutes}</td>
                <td className="muted">{e.idleMinutes}{e.powerCutMinutes > 0 ? ` (+${e.powerCutMinutes} cut)` : ''}</td>
                <td>{hoursText(e.billableHours)}</td>
                <td>{formatMoney(e.ratePerHour)}</td>
                <td>{formatMoney(e.amount)}</td>
                <td>{formatMoney(e.taxAmount)}</td>
                <td><strong>{formatMoney(e.total)}</strong></td>
                <td className="actions">
                  <button className="link" onClick={() => open(e)}>Edit</button>
                  <button className="link danger" onClick={() => remove(e)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {list && list.entries.length === 0 && <div className="empty">No entries for this selection.</div>}
      </div>
    </div>
  )
}
