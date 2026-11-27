import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api'
import { ErrorNote, errorText, formatMoney } from '../../components'
import type { StageMaster, Supplier, SupplierInput } from '../../types'

/** Phone numbers are tappable on a phone: "9944363952, 6374278193" becomes two links. */
function Phones({ value }: { value: string | null }) {
  if (!value) return null
  const numbers = value.split(',').map(n => n.trim()).filter(Boolean)
  return (
    <>
      {numbers.map((n, i) => (
        <span key={n}>{i > 0 && ', '}<a href={`tel:${n.replace(/[^+\d]/g, '')}`}>{n}</a></span>
      ))}
    </>
  )
}

export default function Suppliers() {
  const [all, setAll] = useState<Supplier[] | null>(null)
  const [masters, setMasters] = useState<StageMaster[]>([])
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [process, setProcess] = useState('')
  const [showInactive, setShowInactive] = useState(false)
  const [editing, setEditing] = useState<Supplier | 'new' | null>(null)

  const load = useCallback(() => {
    Promise.all([api.suppliers(true), api.stageMasters()])
      .then(([s, m]) => { setAll(s); setMasters(m) })
      .catch(e => setError(errorText(e)))
  }, [])
  useEffect(load, [load])

  const toggle = async (s: Supplier) => {
    if (s.active && !window.confirm(`Deactivate ${s.name}? It stays on old records but cannot be chosen for new work.`)) return
    setError(null)
    try {
      await api.setSupplierActive(s.id, !s.active)
      load()
    } catch (e) {
      setError(errorText(e))
    }
  }

  const processNames = [...new Set((all ?? []).flatMap(s => s.processes.map(p => p.stageName)))].sort()
  const q = search.trim().toLowerCase()
  const shown = (all ?? []).filter(s =>
    (showInactive || s.active) &&
    (!process || s.processes.some(p => p.stageName === process)) &&
    (!q || [s.name, s.address ?? '', ...s.processes.flatMap(p => [p.stageName, p.contactPerson ?? '', p.contactPhone ?? ''])]
      .some(t => t.toLowerCase().includes(q))))

  return (
    <div>
      <div className="panel-head">
        <h1>Suppliers</h1>
        <button className="primary" onClick={() => setEditing('new')}>+ Add supplier</button>
      </div>
      <p className="muted">Outside workshops, what each one does, and who to call. {all && `${all.filter(s => s.active).length} active.`}</p>
      <ErrorNote message={error} />

      {editing && (
        <SupplierForm key={editing === 'new' ? 'new' : editing.id}
          initial={editing === 'new' ? null : editing} masters={masters}
          onCancel={() => setEditing(null)}
          onSaved={() => { setEditing(null); load() }} />
      )}

      <div className="card filters">
        <input type="search" placeholder="Search name, area, person or phone…" value={search}
          onChange={e => setSearch(e.target.value)} />
        <select value={process} onChange={e => setProcess(e.target.value)}>
          <option value="">All processes</option>
          {processNames.map(n => <option key={n} value={n}>{n}</option>)}
        </select>
        <label className="check">
          <input type="checkbox" checked={showInactive} onChange={e => setShowInactive(e.target.checked)} />
          Show deactivated
        </label>
      </div>

      <div className="card table-wrap">
        <table>
          <thead>
            <tr><th>Supplier</th><th>Processes and contacts</th><th>Track record</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            {shown.map(s => (
              <tr key={s.id} className={s.active ? '' : 'inactive'}>
                <td>
                  <strong>{s.name}</strong>
                  {s.address && <div className="muted small">{s.address}</div>}
                  {s.mapUrl && <a className="small" href={s.mapUrl} target="_blank" rel="noreferrer">Open map ↗</a>}
                </td>
                <td>
                  <ul className="proc-list">
                    {s.processes.map(p => (
                      <li key={p.id}>
                        <span className="chip">{p.stageName}</span>{' '}
                        {p.contactPerson}{p.contactPerson && p.contactPhone && ' · '}<Phones value={p.contactPhone} />
                      </li>
                    ))}
                    {s.processes.length === 0 && <li className="muted">No processes yet</li>}
                  </ul>
                </td>
                <td className="small">
                  {s.jobsReceived > 0
                    ? <>Avg turnaround <strong>{s.avgTurnaroundDays} days</strong> <span className="muted">({s.jobsReceived} jobs)</span></>
                    : <span className="muted">No jobs received yet</span>}
                  <div>
                    {s.debitTotal > 0
                      ? <>Debited <strong>{formatMoney(s.debitTotal)}</strong> <span className="muted">({s.rejectedQty} pcs rejected)</span></>
                      : <span className="muted">No rejections</span>}
                  </div>
                </td>
                <td>{s.active ? 'Active' : 'Deactivated'}</td>
                <td className="actions">
                  <button className="link" onClick={() => setEditing(s)}>Edit</button>
                  <button className="link" onClick={() => toggle(s)}>{s.active ? 'Deactivate' : 'Activate'}</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {all && shown.length === 0 && <div className="empty">No supplier matches.</div>}
      </div>
    </div>
  )
}

interface Row { key: number; stageName: string; contactPerson: string; contactPhone: string }

/** Add or edit one supplier. Saving replaces the supplier's details and its whole list of processes. */
function SupplierForm({ initial, masters, onSaved, onCancel }: {
  initial: Supplier | null
  masters: StageMaster[]
  onSaved: () => void
  onCancel: () => void
}) {
  const [name, setName] = useState(initial?.name ?? '')
  const [address, setAddress] = useState(initial?.address ?? '')
  const [mapUrl, setMapUrl] = useState(initial?.mapUrl ?? '')
  const [rows, setRows] = useState<Row[]>(
    initial && initial.processes.length > 0
      ? initial.processes.map((p, i) => ({ key: i, stageName: p.stageName, contactPerson: p.contactPerson ?? '', contactPhone: p.contactPhone ?? '' }))
      : [{ key: 0, stageName: '', contactPerson: '', contactPhone: '' }])
  const [nextKey, setNextKey] = useState(rows.length)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const change = (key: number, patch: Partial<Row>) =>
    setRows(rs => rs.map(r => (r.key === key ? { ...r, ...patch } : r)))

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setSaving(true)
    const input: SupplierInput = {
      name: name.trim(),
      address: address.trim() || null,
      mapUrl: mapUrl.trim() || null,
      processes: rows.filter(r => r.stageName.trim() !== '').map(r => ({
        stageName: r.stageName.trim(),
        contactPerson: r.contactPerson.trim() || null,
        contactPhone: r.contactPhone.trim() || null,
      })),
    }
    try {
      if (initial) await api.updateSupplier(initial.id, input)
      else await api.createSupplier(input)
      onSaved()
    } catch (err) {
      setError(errorText(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form className="card inline-form" onSubmit={submit}>
      <h2>{initial ? `Edit ${initial.name}` : 'Add supplier'}</h2>
      <div className="grid">
        <label>Supplier name<input value={name} onChange={e => setName(e.target.value)} maxLength={200} required /></label>
        <label>Area / address<input value={address} onChange={e => setAddress(e.target.value)} maxLength={300} /></label>
        <label>Map link (optional)
          <input value={mapUrl} onChange={e => setMapUrl(e.target.value)} maxLength={500} placeholder="https://maps.app.goo.gl/…" />
        </label>
      </div>

      <h3>What they do, and who to call</h3>
      <datalist id="supplier-stage-options">
        {masters.map(m => <option key={m.id} value={m.name} />)}
      </datalist>
      {rows.map(r => (
        <div className="proc-row" key={r.key}>
          <input list="supplier-stage-options" placeholder="Process (type or pick)…" maxLength={80}
            value={r.stageName} onChange={e => change(r.key, { stageName: e.target.value })} />
          <input placeholder="Contact person" maxLength={120}
            value={r.contactPerson} onChange={e => change(r.key, { contactPerson: e.target.value })} />
          <input placeholder="Phone (several: separate with commas)" maxLength={80}
            value={r.contactPhone} onChange={e => change(r.key, { contactPhone: e.target.value })} />
          {rows.length > 1
            ? <button type="button" className="link danger" onClick={() => setRows(rs => rs.filter(x => x.key !== r.key))}>✕</button>
            : <span />}
        </div>
      ))}
      <div className="builder-actions">
        <button type="button" onClick={() => { setRows(rs => [...rs, { key: nextKey, stageName: '', contactPerson: '', contactPhone: '' }]); setNextKey(k => k + 1) }}>
          + Another process
        </button>
        <span className="spacer" />
        <button type="button" className="link" onClick={onCancel}>Cancel</button>
        <button className="primary" disabled={saving}>{saving ? 'Saving…' : 'Save supplier'}</button>
      </div>
      <ErrorNote message={error} />
    </form>
  )
}
