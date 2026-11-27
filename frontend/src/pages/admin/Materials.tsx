import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api'
import { ErrorNote, errorText, formatMoney } from '../../components'
import type { Material } from '../../types'

/** The central list of raw materials: density (for the weight calculation) and price per kg. */
export default function Materials() {
  const [items, setItems] = useState<Material[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [editing, setEditing] = useState<Material | 'new' | null>(null)
  const [showInactive, setShowInactive] = useState(false)

  const load = useCallback(() => {
    api.materials(true).then(setItems).catch(e => setError(errorText(e)))
  }, [])
  useEffect(load, [load])

  const toggle = async (m: Material) => {
    setError(null)
    try {
      await api.setMaterialActive(m.id, !m.active)
      load()
    } catch (e) {
      setError(errorText(e))
    }
  }

  const shown = (items ?? []).filter(m => showInactive || m.active)

  return (
    <div>
      <div className="panel-head">
        <h1>Materials</h1>
        <button className="primary" onClick={() => setEditing('new')}>+ Add material</button>
      </div>
      <p className="muted">
        Price per kg and density for each raw material. A new price is used the next time a part's raw material is saved;
        parts already costed keep the price they were costed at.
      </p>
      <ErrorNote message={error} />

      {editing && (
        <MaterialForm key={editing === 'new' ? 'new' : editing.id} initial={editing === 'new' ? null : editing}
          onCancel={() => setEditing(null)} onSaved={() => { setEditing(null); load() }} />
      )}

      <div className="card filters">
        <label className="check">
          <input type="checkbox" checked={showInactive} onChange={e => setShowInactive(e.target.checked)} />
          Show deactivated
        </label>
      </div>

      <div className="card table-wrap">
        <table>
          <thead><tr><th>Material</th><th>Density (g/cm³)</th><th>Price per kg</th><th>Status</th><th></th></tr></thead>
          <tbody>
            {shown.map(m => (
              <tr key={m.id} className={m.active ? '' : 'inactive'}>
                <td><strong>{m.name}</strong></td>
                <td>{m.density}</td>
                <td>{m.pricePerKg === null ? <span className="muted">not set</span> : formatMoney(m.pricePerKg)}</td>
                <td>{m.active ? 'Active' : 'Deactivated'}</td>
                <td className="actions">
                  <button className="link" onClick={() => setEditing(m)}>Edit</button>
                  <button className="link" onClick={() => toggle(m)}>{m.active ? 'Deactivate' : 'Activate'}</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {items && shown.length === 0 && <div className="empty">No materials.</div>}
      </div>
    </div>
  )
}

function MaterialForm({ initial, onSaved, onCancel }: {
  initial: Material | null
  onSaved: () => void
  onCancel: () => void
}) {
  const [name, setName] = useState(initial?.name ?? '')
  const [density, setDensity] = useState(initial ? String(initial.density) : '7.85')
  const [price, setPrice] = useState(initial?.pricePerKg != null ? String(initial.pricePerKg) : '')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setSaving(true)
    const input = { name: name.trim(), density: Number(density), pricePerKg: price.trim() === '' ? null : Number(price) }
    try {
      if (initial) await api.updateMaterial(initial.id, input)
      else await api.createMaterial(input)
      onSaved()
    } catch (err) {
      setError(errorText(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form className="card inline-form" onSubmit={submit}>
      <h2>{initial ? `Edit ${initial.name}` : 'Add material'}</h2>
      <div className="grid">
        <label>Material name<input value={name} onChange={e => setName(e.target.value)} maxLength={80} required /></label>
        <label>Density (g/cm³)
          <input type="number" step="0.001" min="0.1" value={density} onChange={e => setDensity(e.target.value)} required />
        </label>
        <label>Price per kg (₹)
          <input type="number" step="0.01" min="0" value={price} onChange={e => setPrice(e.target.value)} placeholder="not set" />
        </label>
      </div>
      <p className="muted small">Steels such as OHNS, EN8 and MS are 7.85. Aluminium 2.70, SS 304 7.93, brass 8.50.</p>
      <div className="builder-actions">
        <span className="spacer" />
        <button type="button" className="link" onClick={onCancel}>Cancel</button>
        <button className="primary" disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
      </div>
      <ErrorNote message={error} />
    </form>
  )
}
